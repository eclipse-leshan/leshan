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
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import org.eclipse.leshan.core.node.LwM2mNode;
import org.eclipse.leshan.core.node.LwM2mObjectInstance;
import org.eclipse.leshan.core.node.LwM2mPath;
import org.eclipse.leshan.core.node.LwM2mResource;
import org.eclipse.leshan.core.node.LwM2mSingleResource;
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
import org.eclipse.leshan.server.bootstrap.BootstrapConfig.ServerConfig;
import org.eclipse.leshan.server.bootstrap.BootstrapConfig.ServerSecurity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handle the bootstrap logic at Server side. Check if the client is allowed to bootstrap, with the wanted security
 * scheme. Then send delete and write request to bootstrap the client, then close the bootstrap session by sending a
 * bootstrap finished request.
 */
public class BootstrapHandler {

    private static final Logger LOG = LoggerFactory.getLogger(BootstrapHandler.class);

    private final Executor e;

    private final BootstrapStore store;
    private final LwM2mBootstrapRequestSender sender;
    private final BootstrapSessionManager sessionManager;

    public BootstrapHandler(BootstrapStore store, LwM2mBootstrapRequestSender sender,
            BootstrapSessionManager sessionManager) {
        this.store = store;
        this.sender = sender;
        this.sessionManager = sessionManager;
        this.e = Executors.newFixedThreadPool(5);
    }

    protected BootstrapHandler(BootstrapStore store, LwM2mBootstrapRequestSender sender,
            BootstrapSessionManager sessionManager, Executor executor) {
        this.store = store;
        this.sender = sender;
        this.sessionManager = sessionManager;
        this.e = executor;
    }

    public BootstrapResponse bootstrap(Identity sender, BootstrapRequest request) {
        String endpoint = request.getEndpointName();

        // Start session, checking the BS credentials
        final BootstrapSession session = this.sessionManager.begin(endpoint, sender);

        if (!session.isAuthorized()) {
            this.sessionManager.failed(session, UNAUTHORIZED, null);
            return BootstrapResponse.badRequest("Unauthorized");
        }

        // Get the desired bootstrap config for the endpoint
        final BootstrapConfig cfg = store.getBootstrap(endpoint);
        if (cfg == null) {
            LOG.error("No bootstrap config for {}", endpoint);
            this.sessionManager.failed(session, NO_BOOTSTRAP_CONFIG, null);
            return BootstrapResponse.badRequest("no bootstrap config");
        }

        // Start the bootstrap session
        e.execute(new Runnable() {
            @Override
            public void run() {
                sendDelete(session, cfg);
            }
        });

        return BootstrapResponse.success();
    }

    private void sendDelete(final BootstrapSession session, final BootstrapConfig cfg) {

        final BootstrapDeleteRequest deleteRequest = new BootstrapDeleteRequest();
        send(session, deleteRequest, new ResponseCallback<BootstrapDeleteResponse>() {
            @Override
            public void onResponse(BootstrapDeleteResponse response) {
                LOG.debug("Bootstrap delete {} return code {}", session.getEndpoint(), response.getCode());
                List<Integer> toSend = new ArrayList<>(cfg.security.keySet());
                sendBootstrap(session, cfg, toSend);
            }
        }, new ErrorCallback() {
            @Override
            public void onError(Exception e) {
                LOG.warn(String.format("Error during bootstrap delete '/' on %s", session.getEndpoint()), e);
                sessionManager.failed(session, DELETE_FAILED, deleteRequest);
            }
        });
    }

    private void sendBootstrap(final BootstrapSession session, final BootstrapConfig cfg, final List<Integer> toSend) {
        if (!toSend.isEmpty()) {
            // 1st encode them into a juicy TLV binary
            Integer key = toSend.remove(0);
            ServerSecurity securityConfig = cfg.security.get(key);

            // extract write request parameters
            LwM2mPath path = new LwM2mPath(0, key);
            final LwM2mNode securityInstance = convertToSecurityInstance(key, securityConfig);

            final BootstrapWriteRequest writeBootstrapRequest = new BootstrapWriteRequest(path, securityInstance,
                    session.getContentFormat());
            send(session, writeBootstrapRequest, new ResponseCallback<BootstrapWriteResponse>() {
                @Override
                public void onResponse(BootstrapWriteResponse response) {
                    LOG.debug("Bootstrap write {} return code {}", session.getEndpoint(), response.getCode());
                    // recursive call until toSend is empty
                    sendBootstrap(session, cfg, toSend);
                }
            }, new ErrorCallback() {
                @Override
                public void onError(Exception e) {
                    LOG.warn(String.format("Error during bootstrap write of security instance %s on %s",
                            securityInstance, session.getEndpoint()), e);
                    sessionManager.failed(session, WRITE_SECURITY_FAILED, writeBootstrapRequest);
                }
            });
        } else {
            // we are done, send the servers
            List<Integer> serversToSend = new ArrayList<>(cfg.servers.keySet());
            sendServers(session, cfg, serversToSend);
        }
    }

    private void sendServers(final BootstrapSession session, final BootstrapConfig cfg, final List<Integer> toSend) {
        if (!toSend.isEmpty()) {
            // get next config
            Integer key = toSend.remove(0);
            ServerConfig serverConfig = cfg.servers.get(key);

            // extract write request parameters
            LwM2mPath path = new LwM2mPath(1, key);
            final LwM2mNode serverInstance = convertToServerInstance(key, serverConfig);

            final BootstrapWriteRequest writeServerRequest = new BootstrapWriteRequest(path, serverInstance,
                    session.getContentFormat());
            send(session, writeServerRequest, new ResponseCallback<BootstrapWriteResponse>() {
                @Override
                public void onResponse(BootstrapWriteResponse response) {
                    LOG.debug("Bootstrap write {} return code {}", session.getEndpoint(), response.getCode());
                    // recursive call until toSend is empty
                    sendServers(session, cfg, toSend);
                }
            }, new ErrorCallback() {
                @Override
                public void onError(Exception e) {
                    LOG.warn(String.format("Error during bootstrap write of server instance %s on %s", serverInstance,
                            session.getEndpoint()), e);
                    sessionManager.failed(session, WRITE_SERVER_FAILED, writeServerRequest);
                }
            });
        } else {
            final BootstrapFinishRequest finishBootstrapRequest = new BootstrapFinishRequest();
            send(session, finishBootstrapRequest, new ResponseCallback<BootstrapFinishResponse>() {
                @Override
                public void onResponse(BootstrapFinishResponse response) {
                    LOG.debug("Bootstrap Finished {} return code {}", session.getEndpoint(), response.getCode());
                    if (response.isSuccess()) {
                        sessionManager.end(session);
                    } else {
                        sessionManager.failed(session, FINISHED_WITH_ERROR, finishBootstrapRequest);
                    }
                }
            }, new ErrorCallback() {
                @Override
                public void onError(Exception e) {
                    LOG.warn(String.format("Error during bootstrap finished on %s", session.getEndpoint()), e);
                    sessionManager.failed(session, SEND_FINISH_FAILED, finishBootstrapRequest);
                }
            });
        }
    }

    private <T extends LwM2mResponse> void send(BootstrapSession session, DownlinkRequest<T> request,
            ResponseCallback<T> responseCallback, ErrorCallback errorCallback) {
        sender.send(session.getEndpoint(), session.getIdentity().getPeerAddress(), session.getIdentity().isSecure(),
                request, responseCallback, errorCallback);
    }

    private LwM2mObjectInstance convertToSecurityInstance(int instanceId, ServerSecurity securityConfig) {
        Collection<LwM2mResource> resources = new ArrayList<>();

        if (securityConfig.uri != null)
            resources.add(LwM2mSingleResource.newStringResource(0, securityConfig.uri));
        resources.add(LwM2mSingleResource.newBooleanResource(1, securityConfig.bootstrapServer));
        if (securityConfig.securityMode != null)
            resources.add(LwM2mSingleResource.newIntegerResource(2, securityConfig.securityMode.code));
        if (securityConfig.publicKeyOrId != null)
            resources.add(LwM2mSingleResource.newBinaryResource(3, securityConfig.publicKeyOrId));
        if (securityConfig.serverPublicKey != null)
            resources.add(LwM2mSingleResource.newBinaryResource(4, securityConfig.serverPublicKey));
        if (securityConfig.secretKey != null)
            resources.add(LwM2mSingleResource.newBinaryResource(5, securityConfig.secretKey));
        if (securityConfig.smsSecurityMode != null)
            resources.add(LwM2mSingleResource.newIntegerResource(6, securityConfig.smsSecurityMode.code));
        if (securityConfig.smsBindingKeyParam != null)
            resources.add(LwM2mSingleResource.newBinaryResource(7, securityConfig.smsBindingKeyParam));
        if (securityConfig.smsBindingKeySecret != null)
            resources.add(LwM2mSingleResource.newBinaryResource(8, securityConfig.smsBindingKeySecret));
        if (securityConfig.serverSmsNumber != null)
            resources.add(LwM2mSingleResource.newStringResource(9, securityConfig.serverSmsNumber));
        if (securityConfig.serverId != null)
            resources.add(LwM2mSingleResource.newIntegerResource(10, securityConfig.serverId));
        if (securityConfig.clientOldOffTime != null)
            resources.add(LwM2mSingleResource.newIntegerResource(11, securityConfig.clientOldOffTime));
        if (securityConfig.bootstrapServerAccountTimeout != null)
            resources.add(LwM2mSingleResource.newIntegerResource(12, securityConfig.bootstrapServerAccountTimeout));

        return new LwM2mObjectInstance(instanceId, resources);
    }

    private LwM2mObjectInstance convertToServerInstance(int instanceId, ServerConfig serverConfig) {
        Collection<LwM2mResource> resources = new ArrayList<>();

        resources.add(LwM2mSingleResource.newIntegerResource(0, serverConfig.shortId));
        resources.add(LwM2mSingleResource.newIntegerResource(1, serverConfig.lifetime));
        if (serverConfig.defaultMinPeriod != null)
            resources.add(LwM2mSingleResource.newIntegerResource(2, serverConfig.defaultMinPeriod));
        if (serverConfig.defaultMaxPeriod != null)
            resources.add(LwM2mSingleResource.newIntegerResource(3, serverConfig.defaultMaxPeriod));
        if (serverConfig.disableTimeout != null)
            resources.add(LwM2mSingleResource.newIntegerResource(5, serverConfig.disableTimeout));
        resources.add(LwM2mSingleResource.newBooleanResource(6, serverConfig.notifIfDisabled));
        if (serverConfig.binding != null)
            resources.add(LwM2mSingleResource.newStringResource(7, serverConfig.binding.name()));

        return new LwM2mObjectInstance(instanceId, resources);
    }
}
