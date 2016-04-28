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
import org.eclipse.leshan.core.request.ContentFormat;
import org.eclipse.leshan.core.request.Identity;
import org.eclipse.leshan.core.response.BootstrapDeleteResponse;
import org.eclipse.leshan.core.response.BootstrapFinishResponse;
import org.eclipse.leshan.core.response.BootstrapResponse;
import org.eclipse.leshan.core.response.BootstrapWriteResponse;
import org.eclipse.leshan.core.response.ErrorCallback;
import org.eclipse.leshan.core.response.ResponseCallback;
import org.eclipse.leshan.server.bootstrap.BootstrapConfig.ServerConfig;
import org.eclipse.leshan.server.bootstrap.BootstrapConfig.ServerSecurity;
import org.eclipse.leshan.server.security.BootstrapAuthService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handle the bootstrap logic at Server side. Check if the client is allowed to bootstrap, with the wanted security
 * scheme. Then send delete and write request to bootstrap the client, then close the bootstrap session by sending a
 * bootstrap finished request.
 */
public class BootstrapHandler {

    private static final Logger LOG = LoggerFactory.getLogger(BootstrapHandler.class);

    private final Executor e = Executors.newFixedThreadPool(5);

    private final BootstrapAuthService bsAuthService;
    private final BootstrapStore bsStore;
    private final LwM2mBootstrapRequestSender requestSender;

    public BootstrapHandler(BootstrapStore store, BootstrapAuthService bsAuthService,
            LwM2mBootstrapRequestSender requestSender) {
        this.bsStore = store;
        this.bsAuthService = bsAuthService;
        this.requestSender = requestSender;
    }

    public BootstrapResponse bootstrap(final Identity sender, final BootstrapRequest request) {
        final String endpoint = request.getEndpointName();

        // Check client credentials
        if (!bsAuthService.authenticate(endpoint, sender)) {
            return BootstrapResponse.badRequest("Unauthorized");
        }

        // Get the desired bootstrap config for the endpoint
        final BootstrapConfig cfg = bsStore.getBootstrap(endpoint);
        if (cfg == null) {
            LOG.error("No bootstrap config for {}", endpoint);
            return BootstrapResponse.badRequest("no bootstrap config");
        }

        // Start the boostrap session
        e.execute(new Runnable() {
            @Override
            public void run() {
                sendDelete(endpoint, sender, cfg);
            }
        });

        return BootstrapResponse.success();
    }

    private void sendDelete(final String endpoint, final Identity destination, final BootstrapConfig cfg) {
        requestSender.send(endpoint, destination.getPeerAddress(), destination.isSecure(), new BootstrapDeleteRequest(),
                new ResponseCallback<BootstrapDeleteResponse>() {
                    @Override
                    public void onResponse(BootstrapDeleteResponse response) {
                        LOG.debug("Bootstrap delete {} return code {}", endpoint, response.getCode());
                        List<Integer> toSend = new ArrayList<>(cfg.security.keySet());
                        sendBootstrap(endpoint, destination, cfg, toSend);
                    }
                }, new ErrorCallback() {
                    @Override
                    public void onError(Exception e) {
                        // TODO Handle error on bootstrap
                        LOG.warn(String.format("Error pending bootstrap delete '/' on %s", endpoint), e);
                    }
                });
    }

    private void sendBootstrap(final String endpoint, final Identity destination, final BootstrapConfig cfg,
            final List<Integer> toSend) {
        if (!toSend.isEmpty()) {
            // 1st encode them into a juicy TLV binary
            Integer key = toSend.remove(0);
            ServerSecurity securityConfig = cfg.security.get(key);

            // extract write request parameters
            LwM2mPath path = new LwM2mPath(0, key);
            final LwM2mNode securityInstance = convertToSecurityInstance(key, securityConfig);

            requestSender.send(endpoint, destination.getPeerAddress(), destination.isSecure(),
                    new BootstrapWriteRequest(path, securityInstance, ContentFormat.TLV),
                    new ResponseCallback<BootstrapWriteResponse>() {
                        @Override
                        public void onResponse(BootstrapWriteResponse response) {
                            LOG.debug("Bootstrap write {} return code {}", endpoint, response.getCode());
                            // recursive call until toSend is empty
                            sendBootstrap(endpoint, destination, cfg, toSend);
                        }
                    }, new ErrorCallback() {
                        @Override
                        public void onError(Exception e) {
                            // TODO Handle error on bootstrap
                            LOG.warn(String.format("Error pending bootstrap write of security instance %s on %s",
                                    securityInstance, endpoint), e);
                        }
                    });
        } else {
            // we are done, send the servers
            List<Integer> serversToSend = new ArrayList<>(cfg.servers.keySet());
            sendServers(endpoint, destination, cfg, serversToSend);
        }
    }

    private void sendServers(final String endpoint, final Identity destination, final BootstrapConfig cfg,
            final List<Integer> toSend) {
        if (!toSend.isEmpty()) {
            // get next config
            Integer key = toSend.remove(0);
            ServerConfig serverConfig = cfg.servers.get(key);

            // extract write request parameters
            LwM2mPath path = new LwM2mPath(1, key);
            final LwM2mNode serverInstance = convertToServerInstance(key, serverConfig);

            requestSender.send(endpoint, destination.getPeerAddress(), destination.isSecure(),
                    new BootstrapWriteRequest(path, serverInstance, ContentFormat.TLV),
                    new ResponseCallback<BootstrapWriteResponse>() {
                        @Override
                        public void onResponse(BootstrapWriteResponse response) {
                            LOG.debug("Bootstrap write {} return code {}", endpoint, response.getCode());
                            // recursive call until toSend is empty
                            sendServers(endpoint, destination, cfg, toSend);
                        }
                    }, new ErrorCallback() {
                        @Override
                        public void onError(Exception e) {
                            // TODO Handle error on bootstrap
                            LOG.warn(String.format("Error pending bootstrap write of server instance %s on %s",
                                    serverInstance, endpoint), e);
                        }
                    });
        } else {
            requestSender.send(endpoint, destination.getPeerAddress(), destination.isSecure(),
                    new BootstrapFinishRequest(), new ResponseCallback<BootstrapFinishResponse>() {
                        @Override
                        public void onResponse(BootstrapFinishResponse response) {
                            LOG.debug("Bootstrap Finished {} return code {}", endpoint, response.getCode());
                        }
                    }, new ErrorCallback() {
                        @Override
                        public void onError(Exception e) {
                            // TODO Handle error on bootstrap
                            LOG.warn(String.format("Error pending bootstrap finished on %s", endpoint), e);
                        }
                    });
        }
    }

    private LwM2mObjectInstance convertToSecurityInstance(int instanceId, ServerSecurity securityConfig) {
        Collection<LwM2mResource> resources = new ArrayList<>();

        resources.add(LwM2mSingleResource.newStringResource(0, securityConfig.uri));
        resources.add(LwM2mSingleResource.newBooleanResource(1, securityConfig.bootstrapServer));
        resources.add(LwM2mSingleResource.newIntegerResource(2, securityConfig.securityMode.code));
        resources.add(LwM2mSingleResource.newBinaryResource(3, securityConfig.publicKeyOrId));
        resources.add(LwM2mSingleResource.newBinaryResource(4, securityConfig.serverPublicKeyOrId));
        resources.add(LwM2mSingleResource.newBinaryResource(5, securityConfig.secretKey));
        resources.add(LwM2mSingleResource.newIntegerResource(6, securityConfig.smsSecurityMode.code));
        resources.add(LwM2mSingleResource.newBinaryResource(7, securityConfig.smsBindingKeyParam));
        resources.add(LwM2mSingleResource.newBinaryResource(8, securityConfig.smsBindingKeySecret));
        resources.add(LwM2mSingleResource.newStringResource(9, securityConfig.serverSmsNumber));
        resources.add(LwM2mSingleResource.newIntegerResource(10, securityConfig.serverId));
        resources.add(LwM2mSingleResource.newIntegerResource(11, securityConfig.clientOldOffTime));

        return new LwM2mObjectInstance(instanceId, resources);
    }

    private LwM2mObjectInstance convertToServerInstance(int instanceId, ServerConfig serverConfig) {
        Collection<LwM2mResource> resources = new ArrayList<>();

        resources.add(LwM2mSingleResource.newIntegerResource(0, serverConfig.shortId));
        resources.add(LwM2mSingleResource.newIntegerResource(1, serverConfig.lifetime));
        resources.add(LwM2mSingleResource.newIntegerResource(2, serverConfig.defaultMinPeriod));
        if (serverConfig.defaultMaxPeriod != null)
            resources.add(LwM2mSingleResource.newIntegerResource(3, serverConfig.defaultMaxPeriod));
        if (serverConfig.disableTimeout != null)
            resources.add(LwM2mSingleResource.newIntegerResource(5, serverConfig.disableTimeout));
        resources.add(LwM2mSingleResource.newBooleanResource(6, serverConfig.notifIfDisabled));
        resources.add(LwM2mSingleResource.newStringResource(7, serverConfig.binding.name()));

        return new LwM2mObjectInstance(instanceId, resources);
    }
}
