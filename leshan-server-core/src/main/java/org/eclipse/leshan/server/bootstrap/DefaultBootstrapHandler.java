/*******************************************************************************
 * Copyright (c) 2016 Sierra Wireless and others.
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * and Eclipse Distribution License v1.0 which accompany this distribution.
 * 
 * The Eclipse Public License is available at
 *    http://www.eclipse.org/legal/epl-v20.html
 * and the Eclipse Distribution License is available at
 *    http://www.eclipse.org/org/documents/edl-v10.html.
 * 
 * Contributors:
 *     Sierra Wireless - initial API and implementation
 *******************************************************************************/
package org.eclipse.leshan.server.bootstrap;

import static org.eclipse.leshan.server.bootstrap.BootstrapFailureCause.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.leshan.core.request.BootstrapDownlinkRequest;
import org.eclipse.leshan.core.request.BootstrapFinishRequest;
import org.eclipse.leshan.core.request.BootstrapRequest;
import org.eclipse.leshan.core.request.Identity;
import org.eclipse.leshan.core.response.BootstrapFinishResponse;
import org.eclipse.leshan.core.response.BootstrapResponse;
import org.eclipse.leshan.core.response.ErrorCallback;
import org.eclipse.leshan.core.response.LwM2mResponse;
import org.eclipse.leshan.core.response.ResponseCallback;
import org.eclipse.leshan.core.response.SendableResponse;
import org.eclipse.leshan.server.bootstrap.BootstrapSessionManager.BootstrapPolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A default implementation for {@link BootstrapHandler}.
 */
public class DefaultBootstrapHandler implements BootstrapHandler {

    private static final Logger LOG = LoggerFactory.getLogger(DefaultBootstrapHandler.class);

    // We choose a default timeout a bit higher to the MAX_TRANSMIT_WAIT(62-93s) which is the time from starting to
    // send a Confirmable message to the time when an acknowledgement is no longer expected.
    public static final long DEFAULT_TIMEOUT = 2 * 60 * 1000l; // 2min in ms

    protected final BootstrapConfigurationStore store;

    protected final LwM2mBootstrapRequestSender sender;
    protected final long requestTimeout;

    protected final ConcurrentHashMap<String, BootstrapSession> onGoingSession = new ConcurrentHashMap<>();
    protected final BootstrapSessionManager sessionManager;

    @Deprecated
    public DefaultBootstrapHandler(BootstrapConfigStore store, LwM2mBootstrapRequestSender sender,
            BootstrapSessionManager sessionManager) {
        this(new BootstrapConfigurationStoreAdapter(store), sender, sessionManager, DEFAULT_TIMEOUT);
    }

    @Deprecated
    public DefaultBootstrapHandler(BootstrapConfigStore store, LwM2mBootstrapRequestSender sender,
            BootstrapSessionManager sessionManager, long requestTimeout) {
        this(new BootstrapConfigurationStoreAdapter(store), sender, sessionManager, requestTimeout);
    }

    public DefaultBootstrapHandler(BootstrapConfigurationStore store, LwM2mBootstrapRequestSender sender,
            BootstrapSessionManager sessionManager) {
        this(store, sender, sessionManager, DEFAULT_TIMEOUT);
    }

    public DefaultBootstrapHandler(BootstrapConfigurationStore store, LwM2mBootstrapRequestSender sender,
            BootstrapSessionManager sessionManager, long requestTimeout) {
        this.store = store;
        this.sender = sender;
        this.sessionManager = sessionManager;
        this.requestTimeout = requestTimeout;
    }

    @Override
    public SendableResponse<BootstrapResponse> bootstrap(Identity sender, BootstrapRequest request) {
        String endpoint = request.getEndpointName();

        // Start session, checking the BS credentials
        final BootstrapSession session;
        session = sessionManager.begin(request, sender);

        if (!session.isAuthorized()) {
            sessionManager.failed(session, UNAUTHORIZED);
            return new SendableResponse<>(BootstrapResponse.badRequest("Unauthorized"));
        }

        // check if there is not an ongoing session.
        BootstrapSession oldSession = onGoingSession.put(endpoint, session);
        if (oldSession != null) {
            // stop previous ongoing session.
            synchronized (oldSession) {
                oldSession.cancel();
                this.sender.cancelOngoingRequests(oldSession);
            }
        }

        try {
            // Get the desired bootstrap config for the endpoint
            final BootstrapConfiguration cfg = store.get(endpoint, sender, session);
            if (cfg == null) {
                LOG.debug("No bootstrap config for {}", session);
                stopSession(session, NO_BOOTSTRAP_CONFIG);
                return new SendableResponse<>(BootstrapResponse.badRequest("no bootstrap config"));
            }

            // Start bootstrap once response is sent.
            Runnable onSent = new Runnable() {
                @Override
                public void run() {
                    startBootstrap(session, cfg);
                }
            };
            return new SendableResponse<>(BootstrapResponse.success(), onSent);

        } catch (RuntimeException e) {
            LOG.warn("Unexpected error at bootstrap start-up for {}", session, e);
            stopSession(session, INTERNAL_SERVER_ERROR);
            return new SendableResponse<>(BootstrapResponse.internalServerError(e.getMessage()));
        }
    }

    protected void startBootstrap(BootstrapSession session, BootstrapConfiguration cfg) {
        sendRequest(session, cfg, new ArrayList<>(cfg.getRequests()));
    }

    protected void stopSession(BootstrapSession session, BootstrapFailureCause cause) {
        if (!onGoingSession.remove(session.getEndpoint(), session)) {
            if (!session.isCancelled()) {
                LOG.warn("{} was already removed", session);
            }
        }
        // if there is no cause of failure, this is a success
        if (cause == null) {
            sessionManager.end(session);
        } else {
            sessionManager.failed(session, cause);
        }
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    protected void sendRequest(final BootstrapSession session, final BootstrapConfiguration cfg,
            final List<BootstrapDownlinkRequest<? extends LwM2mResponse>> requestToSend) {
        if (!requestToSend.isEmpty()) {
            // get next request
            final BootstrapDownlinkRequest request = requestToSend.get(0);

            send(session, request, new SafeResponseCallback(session) {
                @Override
                public void safeOnResponse(LwM2mResponse response) {
                    if (response.isSuccess()) {
                        LOG.trace("{} receives {} for {}", session, response, request);
                        sessionManager.onResponseSuccess(session, request);
                        afterRequest(session, cfg, requestToSend, BootstrapPolicy.CONTINUE);
                    } else {
                        LOG.debug("{} receives {} for {}", session, response, request);
                        BootstrapPolicy policy = sessionManager.onResponseError(session, request, response);
                        afterRequest(session, cfg, requestToSend, policy);
                    }
                }
            }, new SafeErrorCallback(session) {
                @Override
                public void safeOnError(Exception e) {
                    LOG.debug("Error for {} while sending {} ", session, request, e);
                    BootstrapPolicy policy = sessionManager.onRequestFailure(session, request, e);
                    afterRequest(session, cfg, requestToSend, policy);
                }
            });
        } else {
            // we are done, send bootstrap finished.
            bootstrapFinished(session, cfg);
        }
    }

    protected void afterRequest(BootstrapSession session, BootstrapConfiguration cfg,
            final List<BootstrapDownlinkRequest<? extends LwM2mResponse>> requestToSend, BootstrapPolicy policy) {
        if (session.isCancelled()) {
            stopSession(session, CANCELLED);
            return;
        }
        switch (policy) {
        case CONTINUE:
            requestToSend.remove(0);
            sendRequest(session, cfg, requestToSend);
            break;
        case RETRY:
            sendRequest(session, cfg, requestToSend);
            break;
        case RETRYALL:
            startBootstrap(session, cfg);
            break;
        case SEND_FINISHED:
            bootstrapFinished(session, cfg);
            break;
        case STOP:
            stopSession(session, REQUEST_FAILED);
            break;
        default:
            throw new IllegalStateException("unknown policy :" + policy);
        }
    }

    protected void bootstrapFinished(final BootstrapSession session, final BootstrapConfiguration cfg) {

        final BootstrapFinishRequest finishBootstrapRequest = new BootstrapFinishRequest();
        send(session, finishBootstrapRequest, new SafeResponseCallback<BootstrapFinishResponse>(session) {
            @Override
            public void safeOnResponse(BootstrapFinishResponse response) {
                if (response.isSuccess()) {
                    LOG.trace("{} receives {} for {}", session, response, finishBootstrapRequest);
                    sessionManager.onResponseSuccess(session, finishBootstrapRequest);
                    afterBootstrapFinished(session, cfg, BootstrapPolicy.CONTINUE);
                } else {
                    LOG.debug("{} receives {} for {}", session, response, finishBootstrapRequest);
                    BootstrapPolicy policy = sessionManager.onResponseError(session, finishBootstrapRequest, response);
                    afterBootstrapFinished(session, cfg, policy);
                }
            }
        }, new SafeErrorCallback(session) {
            @Override
            public void safeOnError(Exception e) {
                LOG.debug("Error for {} while sending {} ", session, finishBootstrapRequest, e);
                BootstrapPolicy policy = sessionManager.onRequestFailure(session, finishBootstrapRequest, e);
                afterBootstrapFinished(session, cfg, policy);
            }
        });
    }

    protected void afterBootstrapFinished(BootstrapSession session, BootstrapConfiguration cfg,
            BootstrapPolicy policy) {
        if (session.isCancelled()) {
            stopSession(session, CANCELLED);
            return;
        }
        switch (policy) {
        case CONTINUE:
            stopSession(session, null);
            break;
        case RETRY:
            bootstrapFinished(session, cfg);
            break;
        case RETRYALL:
            startBootstrap(session, cfg);
            break;
        case SEND_FINISHED:
            bootstrapFinished(session, cfg);
            break;
        case STOP:
            stopSession(session, FINISH_FAILED);
            break;
        default:
            throw new IllegalStateException("unknown policy :" + policy);
        }
    }

    protected <T extends LwM2mResponse> void send(BootstrapSession session, BootstrapDownlinkRequest<T> request,
            ResponseCallback<T> responseCallback, ErrorCallback errorCallback) {
        synchronized (session) {
            if (!session.isCancelled()) {
                sender.send(session, request, requestTimeout, responseCallback, errorCallback);
            } else {
                stopSession(session, CANCELLED);
            }
        }
    }

    protected abstract class SafeResponseCallback<T extends LwM2mResponse> implements ResponseCallback<T> {

        private BootstrapSession session;

        public SafeResponseCallback(BootstrapSession session) {
            this.session = session;
        }

        @Override
        public void onResponse(T response) {
            try {
                safeOnResponse(response);
            } catch (RuntimeException e) {
                LOG.warn("Unexpected error on response callback for {}", session, e);
                stopSession(session, INTERNAL_SERVER_ERROR);
            }
        }

        public abstract void safeOnResponse(T response);
    }

    protected abstract class SafeErrorCallback implements ErrorCallback {

        private BootstrapSession session;

        public SafeErrorCallback(BootstrapSession session) {
            this.session = session;
        }

        @Override
        public void onError(Exception error) {
            try {
                safeOnError(error);
            } catch (RuntimeException e) {
                LOG.warn("Unexpected error on error callback for {}", session, e);
                stopSession(session, INTERNAL_SERVER_ERROR);
            }
        }

        public abstract void safeOnError(Exception e);
    }
}
