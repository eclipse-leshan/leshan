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

import static org.eclipse.leshan.server.bootstrap.BootstrapFailureCause.CANCELLED;
import static org.eclipse.leshan.server.bootstrap.BootstrapFailureCause.FINISH_FAILED;
import static org.eclipse.leshan.server.bootstrap.BootstrapFailureCause.INTERNAL_SERVER_ERROR;
import static org.eclipse.leshan.server.bootstrap.BootstrapFailureCause.NO_BOOTSTRAP_CONFIG;
import static org.eclipse.leshan.server.bootstrap.BootstrapFailureCause.REQUEST_FAILED;
import static org.eclipse.leshan.server.bootstrap.BootstrapFailureCause.UNAUTHORIZED;

import java.net.URI;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.leshan.core.request.BootstrapDownlinkRequest;
import org.eclipse.leshan.core.request.BootstrapFinishRequest;
import org.eclipse.leshan.core.request.BootstrapRequest;
import org.eclipse.leshan.core.request.Identity;
import org.eclipse.leshan.core.response.BootstrapResponse;
import org.eclipse.leshan.core.response.ErrorCallback;
import org.eclipse.leshan.core.response.LwM2mResponse;
import org.eclipse.leshan.core.response.ResponseCallback;
import org.eclipse.leshan.core.response.SendableResponse;
import org.eclipse.leshan.core.util.Validate;
import org.eclipse.leshan.server.bootstrap.BootstrapSessionManager.BootstrapPolicy;
import org.eclipse.leshan.server.bootstrap.request.BootstrapDownlinkRequestSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A default implementation for {@link BootstrapHandler}.
 * <p>
 * It ensure there isn't 2 bootstrap session at the same time for a given client. If this happens the old one was stop
 * and ongoing request are cancelled.
 * <p>
 * It also ensure that we send only one request at a time for a given client.
 * <p>
 * All the logic for a given session is delegate to a the {@link BootstrapSessionManager}.
 */
public class DefaultBootstrapHandler implements BootstrapHandler {

    private static final Logger LOG = LoggerFactory.getLogger(DefaultBootstrapHandler.class);

    // We choose a default timeout a bit higher to the MAX_TRANSMIT_WAIT(62-93s) which is the time from starting to
    // send a Confirmable message to the time when an acknowledgement is no longer expected.
    public static final long DEFAULT_TIMEOUT = 2 * 60 * 1000l; // 2min in ms

    protected final BootstrapDownlinkRequestSender sender;
    protected final long requestTimeout;

    protected final ConcurrentHashMap<String, BootstrapSession> onGoingSession = new ConcurrentHashMap<>();
    protected final BootstrapSessionManager sessionManager;
    protected final BootstrapSessionListener listener;

    public DefaultBootstrapHandler(BootstrapDownlinkRequestSender sender, BootstrapSessionManager sessionManager,
            BootstrapSessionListener listener) {
        this(sender, sessionManager, listener, DEFAULT_TIMEOUT);
    }

    public DefaultBootstrapHandler(BootstrapDownlinkRequestSender sender, BootstrapSessionManager sessionManager,
            BootstrapSessionListener listener, long requestTimeout) {
        Validate.notNull(sender);
        Validate.notNull(sessionManager);
        Validate.notNull(listener);
        this.sender = sender;
        this.sessionManager = sessionManager;
        this.listener = listener;
        this.requestTimeout = requestTimeout;
    }

    @Override
    public SendableResponse<BootstrapResponse> bootstrap(Identity sender, BootstrapRequest request, URI endpointUsed) {
        String endpoint = request.getEndpointName();

        // Start session, checking the BS credentials
        final BootstrapSession session;
        session = sessionManager.begin(request, sender, endpointUsed);
        listener.sessionInitiated(request, sender);

        if (!session.isAuthorized()) {
            sessionManager.failed(session, UNAUTHORIZED);
            listener.unAuthorized(request, sender);
            return new SendableResponse<>(BootstrapResponse.badRequest("Unauthorized"));
        }
        listener.authorized(session);

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
            // check if there is a configuration to apply for this device
            if (!sessionManager.hasConfigFor(session)) {
                LOG.debug("No bootstrap config for {}", session);
                listener.noConfig(session);
                stopSession(session, NO_BOOTSTRAP_CONFIG);
                return new SendableResponse<>(BootstrapResponse.badRequest("no bootstrap config"));
            }

            // Start bootstrap once response is sent.
            Runnable onSent = new Runnable() {
                @Override
                public void run() {
                    startBootstrap(session);
                }
            };
            return new SendableResponse<>(BootstrapResponse.success(), onSent);

        } catch (RuntimeException e) {
            LOG.warn("Unexpected error at bootstrap start-up for {}", session, e);
            stopSession(session, INTERNAL_SERVER_ERROR);
            return new SendableResponse<>(BootstrapResponse.internalServerError(e.getMessage()));
        }
    }

    protected void startBootstrap(BootstrapSession session) {
        sendRequest(session, sessionManager.getFirstRequest(session));
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
            listener.end(session);
        } else {
            sessionManager.failed(session, cause);
            listener.failed(session, cause);
        }
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    protected void sendRequest(final BootstrapSession session,
            final BootstrapDownlinkRequest<? extends LwM2mResponse> requestToSend) {

        listener.sendRequest(session, requestToSend);
        send(session, requestToSend, new SafeResponseCallback(session) {
            @Override
            public void safeOnResponse(LwM2mResponse response) {
                if (response.isSuccess()) {
                    LOG.trace("{} receives {} for {}", session, response, requestToSend);
                    BootstrapPolicy policy = sessionManager.onResponseSuccess(session, requestToSend, response);
                    listener.onResponseSuccess(session, requestToSend, response);
                    afterRequest(session, policy, requestToSend);
                } else {
                    LOG.debug("{} receives {} for {}", session, response, requestToSend);
                    BootstrapPolicy policy = sessionManager.onResponseError(session, requestToSend, response);
                    listener.onResponseError(session, requestToSend, response);
                    afterRequest(session, policy, requestToSend);
                }
            }
        }, new SafeErrorCallback(session) {
            @Override
            public void safeOnError(Exception e) {
                LOG.debug("Error for {} while sending {} ", session, requestToSend, e);
                BootstrapPolicy policy = sessionManager.onRequestFailure(session, requestToSend, e);
                listener.onRequestFailure(session, requestToSend, e);
                afterRequest(session, policy, requestToSend);
            }
        });
    }

    protected void afterRequest(BootstrapSession session, BootstrapPolicy policy,
            BootstrapDownlinkRequest<? extends LwM2mResponse> requestSent) {
        if (session.isCancelled()) {
            stopSession(session, CANCELLED);
            return;
        }
        if (policy.shouldContinue()) {
            sendRequest(session, policy.nextRequest());
        } else if (policy.shouldfail()) {
            if (requestSent instanceof BootstrapFinishRequest) {
                stopSession(session, FINISH_FAILED);
            } else {
                stopSession(session, REQUEST_FAILED);
            }
        } else if (policy.shouldFinish()) {
            stopSession(session, null);
        } else {
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

        private final BootstrapSession session;

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

        private final BootstrapSession session;

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
