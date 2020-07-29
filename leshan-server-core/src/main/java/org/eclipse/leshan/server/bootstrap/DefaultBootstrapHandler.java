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

import org.eclipse.leshan.core.node.LwM2mNode;
import org.eclipse.leshan.core.node.LwM2mPath;
import org.eclipse.leshan.core.request.BootstrapDeleteRequest;
import org.eclipse.leshan.core.request.BootstrapFinishRequest;
import org.eclipse.leshan.core.request.BootstrapRequest;
import org.eclipse.leshan.core.request.BootstrapWriteRequest;
import org.eclipse.leshan.core.request.DownlinkRequest;
import org.eclipse.leshan.core.request.Identity;
import org.eclipse.leshan.core.response.BootstrapDeleteResponse;
import org.eclipse.leshan.core.response.BootstrapFinishResponse;
import org.eclipse.leshan.core.response.BootstrapResponse;
import org.eclipse.leshan.core.response.BootstrapWriteResponse;
import org.eclipse.leshan.core.response.ErrorCallback;
import org.eclipse.leshan.core.response.LwM2mResponse;
import org.eclipse.leshan.core.response.ResponseCallback;
import org.eclipse.leshan.core.response.SendableResponse;
import org.eclipse.leshan.server.bootstrap.BootstrapConfig.ACLConfig;
import org.eclipse.leshan.server.bootstrap.BootstrapConfig.ServerConfig;
import org.eclipse.leshan.server.bootstrap.BootstrapConfig.ServerSecurity;
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

    protected final BootstrapConfigStore store;

    protected final LwM2mBootstrapRequestSender sender;
    protected final long requestTimeout;

    protected final ConcurrentHashMap<String, BootstrapSession> onGoingSession = new ConcurrentHashMap<>();
    protected final BootstrapSessionManager sessionManager;

    public DefaultBootstrapHandler(BootstrapConfigStore store, LwM2mBootstrapRequestSender sender,
            BootstrapSessionManager sessionManager) {
        this(store, sender, sessionManager, DEFAULT_TIMEOUT);
    }

    public DefaultBootstrapHandler(BootstrapConfigStore store, LwM2mBootstrapRequestSender sender,
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
            final BootstrapConfig cfg = store.get(endpoint, sender, session);
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

    protected void startBootstrap(BootstrapSession session, BootstrapConfig cfg) {
        delete(session, cfg, new ArrayList<>(cfg.toDelete));
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

    protected void delete(final BootstrapSession session, final BootstrapConfig cfg, final List<String> pathToDelete) {
        if (!pathToDelete.isEmpty()) {
            // get next Security configuration
            String path = pathToDelete.get(0);

            final BootstrapDeleteRequest deleteRequest = new BootstrapDeleteRequest(path);
            send(session, deleteRequest, new SafeResponseCallback<BootstrapDeleteResponse>(session) {
                @Override
                public void safeOnResponse(BootstrapDeleteResponse response) {
                    if (response.isSuccess()) {
                        LOG.trace("{} receives {} for {}", session, response, deleteRequest);
                        sessionManager.onResponseSuccess(session, deleteRequest);
                        afterDelete(session, cfg, pathToDelete, BootstrapPolicy.CONTINUE);
                    } else {
                        LOG.debug("{} receives {} for {}", session, response, deleteRequest);
                        BootstrapPolicy policy = sessionManager.onResponseError(session, deleteRequest, response);
                        afterDelete(session, cfg, pathToDelete, policy);
                    }
                }
            }, new SafeErrorCallback(session) {
                @Override
                public void safeOnError(Exception e) {
                    LOG.debug("Error for {} while sending {} ", session, deleteRequest, e);
                    BootstrapPolicy policy = sessionManager.onRequestFailure(session, deleteRequest, e);
                    afterDelete(session, cfg, pathToDelete, policy);
                }
            });
        } else {
            // we are done, write the securities now
            List<Integer> securityInstancesToWrite = new ArrayList<>(cfg.security.keySet());
            writeSecurities(session, cfg, securityInstancesToWrite);
        }
    }

    protected void afterDelete(BootstrapSession session, BootstrapConfig cfg, List<String> pathToDelete,
            BootstrapPolicy policy) {
        if (session.isCancelled()) {
            stopSession(session, CANCELLED);
            return;
        }
        switch (policy) {
        case CONTINUE:
            pathToDelete.remove(0);
            delete(session, cfg, pathToDelete);
            break;
        case RETRY:
            delete(session, cfg, pathToDelete);
            break;
        case RETRYALL:
            startBootstrap(session, cfg);
            break;
        case SEND_FINISHED:
            bootstrapFinished(session, cfg);
            break;
        case STOP:
            stopSession(session, DELETE_FAILED);
            break;
        default:
            throw new IllegalStateException("unknown policy :" + policy);
        }
    }

    protected void writeSecurities(final BootstrapSession session, final BootstrapConfig cfg,
            final List<Integer> securityInstancesToWrite) {
        if (!securityInstancesToWrite.isEmpty()) {
            // get next Security configuration
            Integer key = securityInstancesToWrite.get(0);
            ServerSecurity securityConfig = cfg.security.get(key);

            // create write request from it
            LwM2mPath path = new LwM2mPath(0, key);
            final LwM2mNode securityInstance = BootstrapUtil.convertToSecurityInstance(key, securityConfig);
            final BootstrapWriteRequest writeBootstrapRequest = new BootstrapWriteRequest(path, securityInstance,
                    session.getContentFormat());

            // sent it
            send(session, writeBootstrapRequest, new SafeResponseCallback<BootstrapWriteResponse>(session) {
                @Override
                public void safeOnResponse(BootstrapWriteResponse response) {
                    if (response.isSuccess()) {
                        LOG.trace("{} receives {} for {}", session, response, writeBootstrapRequest);
                        sessionManager.onResponseSuccess(session, writeBootstrapRequest);
                        afterWriteSecurities(session, cfg, securityInstancesToWrite, BootstrapPolicy.CONTINUE);
                    } else {
                        LOG.debug("{} receives {} for {}", session, response, writeBootstrapRequest);
                        BootstrapPolicy policy = sessionManager.onResponseError(session, writeBootstrapRequest,
                                response);
                        afterWriteSecurities(session, cfg, securityInstancesToWrite, policy);
                    }
                }
            }, new SafeErrorCallback(session) {
                @Override
                public void safeOnError(Exception e) {
                    LOG.debug("Error for {} while sending {} ", session, writeBootstrapRequest, e);
                    BootstrapPolicy policy = sessionManager.onRequestFailure(session, writeBootstrapRequest, e);
                    afterWriteSecurities(session, cfg, securityInstancesToWrite, policy);
                }
            });
        } else {
            // we are done, write the servers now
            List<Integer> serverInstancesToWrite = new ArrayList<>(cfg.servers.keySet());
            writeServers(session, cfg, serverInstancesToWrite);
        }
    }

    protected void afterWriteSecurities(BootstrapSession session, BootstrapConfig cfg,
            List<Integer> securityInstancesToWrite, BootstrapPolicy policy) {
        if (session.isCancelled()) {
            stopSession(session, CANCELLED);
            return;
        }
        switch (policy) {
        case CONTINUE:
            securityInstancesToWrite.remove(0);
            writeSecurities(session, cfg, securityInstancesToWrite);
            break;
        case RETRY:
            writeSecurities(session, cfg, securityInstancesToWrite);
            break;
        case RETRYALL:
            startBootstrap(session, cfg);
            break;
        case SEND_FINISHED:
            bootstrapFinished(session, cfg);
            break;
        case STOP:
            stopSession(session, WRITE_SECURITY_FAILED);
            break;
        default:
            throw new IllegalStateException("unknown policy :" + policy);
        }
    }

    protected void writeServers(final BootstrapSession session, final BootstrapConfig cfg,
            final List<Integer> serverInstancesToWrite) {
        if (!serverInstancesToWrite.isEmpty()) {
            // get next Server configuration
            Integer key = serverInstancesToWrite.get(0);
            ServerConfig serverConfig = cfg.servers.get(key);

            // create write request from it
            LwM2mPath path = new LwM2mPath(1, key);
            final LwM2mNode serverInstance = BootstrapUtil.convertToServerInstance(key, serverConfig);
            final BootstrapWriteRequest writeServerRequest = new BootstrapWriteRequest(path, serverInstance,
                    session.getContentFormat());

            // sent it
            send(session, writeServerRequest, new SafeResponseCallback<BootstrapWriteResponse>(session) {
                @Override
                public void safeOnResponse(BootstrapWriteResponse response) {
                    if (response.isSuccess()) {
                        LOG.trace("{} receives {} for {}", session, response, writeServerRequest);
                        sessionManager.onResponseSuccess(session, writeServerRequest);
                        afterWriteServers(session, cfg, serverInstancesToWrite, BootstrapPolicy.CONTINUE);
                    } else {
                        LOG.debug("{} receives {} for {}", session, response, writeServerRequest);
                        BootstrapPolicy policy = sessionManager.onResponseError(session, writeServerRequest, response);
                        afterWriteServers(session, cfg, serverInstancesToWrite, policy);
                    }
                }
            }, new SafeErrorCallback(session) {
                @Override
                public void safeOnError(Exception e) {
                    LOG.debug("Error for {} while sending {} ", session, writeServerRequest, e);
                    BootstrapPolicy policy = sessionManager.onRequestFailure(session, writeServerRequest, e);
                    afterWriteServers(session, cfg, serverInstancesToWrite, policy);
                }
            });
        } else {
            // we are done, write ACLs now
            List<Integer> aclInstancesToWrite = new ArrayList<>(cfg.acls.keySet());
            writedAcls(session, cfg, aclInstancesToWrite);
        }
    }

    protected void afterWriteServers(BootstrapSession session, BootstrapConfig cfg,
            List<Integer> serverInstancesToWrite, BootstrapPolicy policy) {
        if (session.isCancelled()) {
            stopSession(session, CANCELLED);
            return;
        }
        switch (policy) {
        case CONTINUE:
            serverInstancesToWrite.remove(0);
            writeServers(session, cfg, serverInstancesToWrite);
            break;
        case RETRY:
            writeServers(session, cfg, serverInstancesToWrite);
            break;
        case RETRYALL:
            startBootstrap(session, cfg);
            break;
        case SEND_FINISHED:
            bootstrapFinished(session, cfg);
            break;
        case STOP:
            stopSession(session, WRITE_SERVER_FAILED);
            break;
        default:
            throw new IllegalStateException("unknown policy :" + policy);
        }
    }

    protected void writedAcls(final BootstrapSession session, final BootstrapConfig cfg,
            final List<Integer> aclInstancesToWrite) {
        if (!aclInstancesToWrite.isEmpty()) {
            // get next ACL configuration
            Integer key = aclInstancesToWrite.get(0);
            ACLConfig aclConfig = cfg.acls.get(key);

            // create write request from it
            LwM2mPath path = new LwM2mPath(2, key);
            final LwM2mNode aclInstance = BootstrapUtil.convertToAclInstance(key, aclConfig);
            final BootstrapWriteRequest writeACLRequest = new BootstrapWriteRequest(path, aclInstance,
                    session.getContentFormat());

            // sent it
            send(session, writeACLRequest, new SafeResponseCallback<BootstrapWriteResponse>(session) {
                @Override
                public void safeOnResponse(BootstrapWriteResponse response) {
                    if (response.isSuccess()) {
                        LOG.trace("{} receives {} for {}", session, response, writeACLRequest);
                        sessionManager.onResponseSuccess(session, writeACLRequest);
                        afterWritedAcls(session, cfg, aclInstancesToWrite, BootstrapPolicy.CONTINUE);
                    } else {
                        LOG.debug("{} receives {} for {}", session, response, writeACLRequest);
                        BootstrapPolicy policy = sessionManager.onResponseError(session, writeACLRequest, response);
                        afterWritedAcls(session, cfg, aclInstancesToWrite, policy);
                    }
                }
            }, new SafeErrorCallback(session) {
                @Override
                public void safeOnError(Exception e) {
                    LOG.debug("Error for {} while sending {} ", session, writeACLRequest, e);
                    BootstrapPolicy policy = sessionManager.onRequestFailure(session, writeACLRequest, e);
                    afterWritedAcls(session, cfg, aclInstancesToWrite, policy);
                }
            });
        } else {
            // we are done, send bootstrap finished.
            bootstrapFinished(session, cfg);
        }
    }

    protected void afterWritedAcls(BootstrapSession session, BootstrapConfig cfg, List<Integer> aclInstancesToWrite,
            BootstrapPolicy policy) {
        if (session.isCancelled()) {
            stopSession(session, CANCELLED);
            return;
        }
        switch (policy) {
        case CONTINUE:
            aclInstancesToWrite.remove(0);
            writedAcls(session, cfg, aclInstancesToWrite);
            break;
        case RETRY:
            writedAcls(session, cfg, aclInstancesToWrite);
            break;
        case RETRYALL:
            startBootstrap(session, cfg);
            break;
        case SEND_FINISHED:
            bootstrapFinished(session, cfg);
            break;
        case STOP:
            stopSession(session, WRITE_ACL_FAILED);
            break;
        default:
            throw new IllegalStateException("unknown policy :" + policy);
        }
    }

    protected void bootstrapFinished(final BootstrapSession session, final BootstrapConfig cfg) {

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

    protected void afterBootstrapFinished(BootstrapSession session, BootstrapConfig cfg, BootstrapPolicy policy) {
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

    protected <T extends LwM2mResponse> void send(BootstrapSession session, DownlinkRequest<T> request,
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
