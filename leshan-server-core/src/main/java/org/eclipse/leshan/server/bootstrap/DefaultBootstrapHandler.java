/*******************************************************************************
 * Copyright (c) 2016 Sierra Wireless and others.
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v1.0 which accompany this distribution.
 * 
 * The Eclipse Public License is available at
 *    http://www.eclipse.org/legal/epl-v10.html
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
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

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
import org.eclipse.leshan.server.bootstrap.BootstrapConfig.ACLConfig;
import org.eclipse.leshan.server.bootstrap.BootstrapConfig.ServerConfig;
import org.eclipse.leshan.server.bootstrap.BootstrapConfig.ServerSecurity;
import org.eclipse.leshan.server.bootstrap.BootstrapSessionManager.BootstrapPolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handle the bootstrap logic at Server side. Check if the client is allowed to bootstrap, with the wanted security
 * scheme. Then send delete and write request to bootstrap the client, then close the bootstrap session by sending a
 * bootstrap finished request.
 */
public class DefaultBootstrapHandler implements BootstrapHandler {

    private static final Logger LOG = LoggerFactory.getLogger(DefaultBootstrapHandler.class);

    // We choose a default timeout a bit higher to the MAX_TRANSMIT_WAIT(62-93s) which is the time from starting to
    // send a Confirmable message to the time when an acknowledgement is no longer expected.
    protected static final long DEFAULT_TIMEOUT = 2 * 60 * 1000l; // 2min in ms

    protected final Executor e;

    protected final BootstrapConfigStore store;
    protected final LwM2mBootstrapRequestSender sender;
    protected final BootstrapSessionManager sessionManager;
    protected final long requestTimeout;
    protected final ConcurrentHashMap<String, BootstrapSession> onGoingSession = new ConcurrentHashMap<>();

    public DefaultBootstrapHandler(BootstrapConfigStore store, LwM2mBootstrapRequestSender sender,
            BootstrapSessionManager sessionManager) {
        this(store, sender, sessionManager, Executors.newFixedThreadPool(5), DEFAULT_TIMEOUT);
    }

    public DefaultBootstrapHandler(BootstrapConfigStore store, LwM2mBootstrapRequestSender sender,
            BootstrapSessionManager sessionManager, Executor executor) {
        this(store, sender, sessionManager, executor, DEFAULT_TIMEOUT);
    }

    public DefaultBootstrapHandler(BootstrapConfigStore store, LwM2mBootstrapRequestSender sender,
            BootstrapSessionManager sessionManager, Executor executor, long requestTimeout) {
        this.store = store;
        this.sender = sender;
        this.sessionManager = sessionManager;
        this.e = executor;
        this.requestTimeout = requestTimeout;
    }

    @Override
    public BootstrapResponse bootstrap(Identity sender, BootstrapRequest request) {
        String endpoint = request.getEndpointName();

        // Start session, checking the BS credentials
        final BootstrapSession session = this.sessionManager.begin(endpoint, sender);

        if (!session.isAuthorized()) {
            sessionManager.failed(session, UNAUTHORIZED);
            return BootstrapResponse.badRequest("Unauthorized");
        }

        // check if there is not an ongoing session.
        BootstrapSession oldSession = onGoingSession.putIfAbsent(endpoint, session);
        if (oldSession != null) {
            // Do not start the session if there is already a started one
            sessionManager.failed(session, ALREADY_STARTED);
            return BootstrapResponse.badRequest("session already started");
        }

        // Get the desired bootstrap config for the endpoint
        final BootstrapConfig cfg = store.get(endpoint, sender);
        if (cfg == null) {
            LOG.debug("No bootstrap config for {}/{}", endpoint, sender);
            stopSession(session, NO_BOOTSTRAP_CONFIG);
            return BootstrapResponse.badRequest("no bootstrap config");
        }

        // Start the bootstrap session
        e.execute(new Runnable() {
            @Override
            public void run() {
                startBootstrap(session, cfg);
            }
        });

        return BootstrapResponse.success();
    }

    protected void startBootstrap(BootstrapSession session, BootstrapConfig cfg) {
        delete(session, cfg, new ArrayList<>(cfg.toDelete));
    }

    protected void stopSession(BootstrapSession session, BootstrapFailureCause cause) {
        if (!onGoingSession.remove(session.getEndpoint(), session)) {
            LOG.warn("Session for endpoint {} / identity {} was already removed", session.getEndpoint(),
                    session.getIdentity());
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
            send(session, deleteRequest, new ResponseCallback<BootstrapDeleteResponse>() {
                @Override
                public void onResponse(BootstrapDeleteResponse response) {
                    if (response.isSuccess()) {
                        LOG.trace("Bootstrap delete {} return code {}", session.getEndpoint(), response.getCode());
                        sessionManager.onResponseSuccess(deleteRequest);
                        afterDelete(session, cfg, pathToDelete, BootstrapPolicy.CONTINUE);
                    } else {
                        LOG.debug("Bootstrap delete {} return code {}", session.getEndpoint(), response.getCode());
                        BootstrapPolicy policy = sessionManager.onResponseError(deleteRequest, response);
                        afterDelete(session, cfg, pathToDelete, policy);
                    }
                }
            }, new ErrorCallback() {
                @Override
                public void onError(Exception e) {
                    LOG.debug(String.format("Error during bootstrap delete '/' to foreign peer [ep=%s,id=%s]",
                            session.getEndpoint(), session.getIdentity()), e);
                    BootstrapPolicy policy = sessionManager.onRequestFailure(deleteRequest, e);
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
            send(session, writeBootstrapRequest, new ResponseCallback<BootstrapWriteResponse>() {
                @Override
                public void onResponse(BootstrapWriteResponse response) {
                    if (response.isSuccess()) {
                        LOG.trace("Bootstrap write {} return code {}", session.getEndpoint(), response.getCode());
                        sessionManager.onResponseSuccess(writeBootstrapRequest);
                        afterWriteSecurities(session, cfg, securityInstancesToWrite, BootstrapPolicy.CONTINUE);
                    } else {
                        LOG.debug("Bootstrap write {} return code {}", session.getEndpoint(), response.getCode());
                        BootstrapPolicy policy = sessionManager.onResponseError(writeBootstrapRequest, response);
                        afterWriteSecurities(session, cfg, securityInstancesToWrite, policy);
                    }
                }
            }, new ErrorCallback() {
                @Override
                public void onError(Exception e) {
                    LOG.debug(String.format(
                            "Error during bootstrap write of security instance %s to foreign peer [ep=%s,id=%s]",
                            securityInstance, session.getEndpoint(), session.getIdentity()), e);
                    BootstrapPolicy policy = sessionManager.onRequestFailure(writeBootstrapRequest, e);
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
            send(session, writeServerRequest, new ResponseCallback<BootstrapWriteResponse>() {
                @Override
                public void onResponse(BootstrapWriteResponse response) {
                    if (response.isSuccess()) {
                        LOG.trace("Bootstrap write {} return code {}", session.getEndpoint(), response.getCode());
                        sessionManager.onResponseSuccess(writeServerRequest);
                        afterWriteServers(session, cfg, serverInstancesToWrite, BootstrapPolicy.CONTINUE);
                    } else {
                        LOG.debug("Bootstrap write {} return code {}", session.getEndpoint(), response.getCode());
                        BootstrapPolicy policy = sessionManager.onResponseError(writeServerRequest, response);
                        afterWriteServers(session, cfg, serverInstancesToWrite, policy);
                    }
                }
            }, new ErrorCallback() {
                @Override
                public void onError(Exception e) {
                    LOG.debug(String.format(
                            "Error during bootstrap write of server instance %s to foreign peer [ep=%s,id=%s]",
                            serverInstance, session.getEndpoint(), session.getIdentity()), e);
                    BootstrapPolicy policy = sessionManager.onRequestFailure(writeServerRequest, e);
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
            send(session, writeACLRequest, new ResponseCallback<BootstrapWriteResponse>() {
                @Override
                public void onResponse(BootstrapWriteResponse response) {
                    if (response.isSuccess()) {
                        LOG.trace("Bootstrap write {} return code {}", session.getEndpoint(), response.getCode());
                        sessionManager.onResponseSuccess(writeACLRequest);
                        afterWritedAcls(session, cfg, aclInstancesToWrite, BootstrapPolicy.CONTINUE);
                    } else {
                        LOG.debug("Bootstrap write {} return code {}", session.getEndpoint(), response.getCode());
                        BootstrapPolicy policy = sessionManager.onResponseError(writeACLRequest, response);
                        afterWritedAcls(session, cfg, aclInstancesToWrite, policy);
                    }
                }
            }, new ErrorCallback() {
                @Override
                public void onError(Exception e) {
                    LOG.debug(String.format(
                            "Error during bootstrap write of acl instance %s to foreign peer [ep=%s,id=%s]",
                            aclInstance, session.getEndpoint(), session.getIdentity()), e);
                    BootstrapPolicy policy = sessionManager.onRequestFailure(writeACLRequest, e);
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
        send(session, finishBootstrapRequest, new ResponseCallback<BootstrapFinishResponse>() {
            @Override
            public void onResponse(BootstrapFinishResponse response) {
                if (response.isSuccess()) {
                    LOG.trace("Bootstrap Finished {} return code {}", session.getEndpoint(), response.getCode());
                    sessionManager.onResponseSuccess(finishBootstrapRequest);
                    afterBootstrapFinished(session, cfg, BootstrapPolicy.CONTINUE);
                } else {
                    LOG.debug("Bootstrap Finished {} return code {}", session.getEndpoint(), response.getCode());
                    BootstrapPolicy policy = sessionManager.onResponseError(finishBootstrapRequest, response);
                    afterBootstrapFinished(session, cfg, policy);
                }
            }
        }, new ErrorCallback() {
            @Override
            public void onError(Exception e) {
                LOG.debug(String.format("Error during bootstrap finished to foreign peer [ep=%s,id=%s]",
                        session.getEndpoint(), session.getIdentity()), e);
                BootstrapPolicy policy = sessionManager.onRequestFailure(finishBootstrapRequest, e);
                afterBootstrapFinished(session, cfg, policy);
            }
        });
    }

    protected void afterBootstrapFinished(BootstrapSession session, BootstrapConfig cfg, BootstrapPolicy policy) {
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
        sender.send(session.getEndpoint(), session.getIdentity(), request, requestTimeout, responseCallback,
                errorCallback);
    }
}
