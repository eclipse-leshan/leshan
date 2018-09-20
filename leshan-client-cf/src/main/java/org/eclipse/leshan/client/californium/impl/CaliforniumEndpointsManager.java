/*******************************************************************************
 * Copyright (c) 2017 Sierra Wireless and others.
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
package org.eclipse.leshan.client.californium.impl;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.Collection;

import org.eclipse.californium.core.CoapServer;
import org.eclipse.californium.core.network.CoapEndpoint;
import org.eclipse.californium.core.network.Endpoint;
import org.eclipse.californium.core.network.config.NetworkConfig;
import org.eclipse.californium.elements.auth.RawPublicKeyIdentity;
import org.eclipse.californium.scandium.DTLSConnector;
import org.eclipse.californium.scandium.config.DtlsConnectorConfig.Builder;
import org.eclipse.californium.scandium.dtls.pskstore.StaticPskStore;
import org.eclipse.californium.scandium.dtls.rpkstore.TrustedRpkStore;
import org.eclipse.leshan.SecurityMode;
import org.eclipse.leshan.client.servers.EndpointsManager;
import org.eclipse.leshan.client.servers.Server;
import org.eclipse.leshan.client.servers.ServerInfo;
import org.eclipse.leshan.core.californium.EndpointFactory;
import org.eclipse.leshan.core.request.Identity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CaliforniumEndpointsManager implements EndpointsManager {

    private static final Logger LOG = LoggerFactory.getLogger(CaliforniumEndpointsManager.class);

    private boolean started = false;

    private Endpoint currentEndpoint;
    private Builder dtlsConfigbuilder;
    private NetworkConfig coapConfig;
    private InetSocketAddress localAddress;
    private CoapServer coapServer;
    private EndpointFactory endpointFactory;

    public CaliforniumEndpointsManager(CoapServer coapServer, InetSocketAddress localAddress, NetworkConfig coapConfig,
            Builder dtlsConfigBuilder, EndpointFactory endpointFactory) {
        this.coapServer = coapServer;
        this.localAddress = localAddress;
        this.coapConfig = coapConfig;
        this.dtlsConfigbuilder = dtlsConfigBuilder;
        this.endpointFactory = endpointFactory;
    }

    @Override
    public synchronized Identity createEndpoint(ServerInfo serverInfo) {
        // Clear previous endpoint
        if (currentEndpoint != null) {
            coapServer.getEndpoints().remove(currentEndpoint);
            currentEndpoint.destroy();
        }

        // Create new endpoint
        Identity server;
        if (serverInfo.isSecure()) {
            Builder newBuilder = new Builder(dtlsConfigbuilder.getIncompleteConfig());

            // Support PSK
            if (serverInfo.secureMode == SecurityMode.PSK) {
                StaticPskStore staticPskStore = new StaticPskStore(serverInfo.pskId, serverInfo.pskKey);
                newBuilder.setPskStore(staticPskStore);
            } else if (serverInfo.secureMode == SecurityMode.RPK) {
                // set identity
                newBuilder.setIdentity(serverInfo.privateKey, serverInfo.publicKey);
                // set RPK truststore
                final PublicKey expectedKey = serverInfo.serverPublicKey;
                newBuilder.setRpkTrustStore(new TrustedRpkStore() {
                    @Override
                    public boolean isTrusted(RawPublicKeyIdentity id) {
                        PublicKey receivedKey = id.getKey();
                        if (receivedKey == null) {
                            LOG.warn("The server public key is null {}", id);
                            return false;
                        }
                        if (!receivedKey.equals(expectedKey)) {
                            LOG.debug(
                                    "Server public key received does match with the expected one.\nReceived: {}\nExpected: {}",
                                    receivedKey, expectedKey);
                            return false;
                        }
                        return true;
                    }
                });
            }
            // TODO add support X509
            if (endpointFactory != null) {
                currentEndpoint = endpointFactory.createSecuredEndpoint(newBuilder.build(), coapConfig, null);
            } else {
                CoapEndpoint.CoapEndpointBuilder builder = new CoapEndpoint.CoapEndpointBuilder();
                builder.setConnector(new DTLSConnector(newBuilder.build()));
                builder.setNetworkConfig(coapConfig);
                currentEndpoint = builder.build();
            }
            server = Identity.psk(serverInfo.getAddress(), serverInfo.pskId);
        } else {
            if (endpointFactory != null) {
                currentEndpoint = endpointFactory.createUnsecuredEndpoint(localAddress, coapConfig, null);
            } else {
                CoapEndpoint.CoapEndpointBuilder builder = new CoapEndpoint.CoapEndpointBuilder();
                builder.setInetSocketAddress(localAddress);
                builder.setNetworkConfig(coapConfig);
                currentEndpoint = builder.build();
            }
            server = Identity.unsecure(serverInfo.getAddress());
        }

        // Add new endpoint
        coapServer.addEndpoint(currentEndpoint);

        // Start endpoint if needed
        if (started) {
            coapServer.start();
            try {
                currentEndpoint.start();
                LOG.info("New endpoint created for server {} at {}", serverInfo.serverUri, currentEndpoint.getUri());
            } catch (IOException e) {
                throw new RuntimeException("Unable to start endpoint", e);
            }
        }
        return server;
    }

    @Override
    public synchronized Collection<Server> createEndpoints(Collection<? extends ServerInfo> serverInfo) {
        if (serverInfo == null || serverInfo.isEmpty())
            return null;
        else {
            // TODO support multi server;
            ServerInfo firstServer = serverInfo.iterator().next();
            Identity identity = createEndpoint(firstServer);
            Collection<Server> servers = new ArrayList<>(1);
            servers.add(new Server(identity, firstServer.serverId));
            return servers;
        }
    }

    public synchronized Endpoint getEndpoint(Identity server) {
        // TODO support multi server;
        if (currentEndpoint.isStarted())
            return currentEndpoint;
        return null;
    }

    @Override
    public synchronized void start() {
        if (started)
            return;
        started = true;

        // we don't have any endpoint so nothing to start
        if (currentEndpoint == null)
            return;

        coapServer.start();
    }

    @Override
    public synchronized void stop() {
        if (!started)
            return;
        started = false;

        // If we have no endpoint this means that we never start coap server
        if (currentEndpoint == null)
            return;

        coapServer.stop();
    }

    @Override
    public synchronized void destroy() {
        started = false;
        coapServer.destroy();
    }
}
