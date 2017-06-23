/*******************************************************************************
 * Copyright (c) 2013-2015 Sierra Wireless and others.
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
 *     Zebra Technologies - initial API and implementation
 *******************************************************************************/
package org.eclipse.leshan.client.californium;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.californium.core.CoapServer;
import org.eclipse.californium.core.network.CoapEndpoint;
import org.eclipse.californium.core.network.config.NetworkConfig;
import org.eclipse.californium.core.network.config.NetworkConfig.Keys;
import org.eclipse.californium.core.server.resources.Resource;
import org.eclipse.californium.scandium.DTLSConnector;
import org.eclipse.californium.scandium.config.DtlsConnectorConfig;
import org.eclipse.californium.scandium.config.DtlsConnectorConfig.Builder;
import org.eclipse.leshan.LwM2mId;
import org.eclipse.leshan.client.LwM2mClient;
import org.eclipse.leshan.client.californium.impl.BootstrapResource;
import org.eclipse.leshan.client.californium.impl.CaliforniumLwM2mRequestSender;
import org.eclipse.leshan.client.californium.impl.ObjectResource;
import org.eclipse.leshan.client.californium.impl.RootResource;
import org.eclipse.leshan.client.californium.impl.SecurityObjectPskStore;
import org.eclipse.leshan.client.observer.LwM2mClientObserver;
import org.eclipse.leshan.client.observer.LwM2mClientObserverAdapter;
import org.eclipse.leshan.client.observer.LwM2mClientObserverDispatcher;
import org.eclipse.leshan.client.resource.LwM2mObjectEnabler;
import org.eclipse.leshan.client.servers.BootstrapHandler;
import org.eclipse.leshan.client.servers.DmServerInfo;
import org.eclipse.leshan.client.servers.RegistrationEngine;
import org.eclipse.leshan.client.servers.ServerInfo;
import org.eclipse.leshan.core.node.codec.DefaultLwM2mNodeDecoder;
import org.eclipse.leshan.core.node.codec.DefaultLwM2mNodeEncoder;
import org.eclipse.leshan.util.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A Lightweight M2M client.
 */
public class LeshanClient implements LwM2mClient {

    private static final Logger LOG = LoggerFactory.getLogger(LeshanClient.class);

    private final ConcurrentHashMap<Integer, LwM2mObjectEnabler> objectEnablers;

    private final CoapServer clientSideServer;
    private final CaliforniumLwM2mRequestSender requestSender;
    private final RegistrationEngine engine;
    private final BootstrapHandler bootstrapHandler;
    private final LwM2mClientObserverDispatcher observers;

    private CoapEndpoint secureEndpoint;

    private CoapEndpoint nonSecureEndpoint;

    public LeshanClient(String endpoint, InetSocketAddress localAddress, InetSocketAddress localSecureAddress,
            List<? extends LwM2mObjectEnabler> objectEnablers, NetworkConfig networkConfig) {

        Validate.notNull(endpoint);
        Validate.notNull(localAddress);
        Validate.notNull(localSecureAddress);
        Validate.notEmpty(objectEnablers);
        Validate.notNull(networkConfig);

        // Create Object enablers
        this.objectEnablers = new ConcurrentHashMap<>();
        for (LwM2mObjectEnabler enabler : objectEnablers) {
            if (this.objectEnablers.containsKey(enabler.getId())) {
                throw new IllegalArgumentException(
                        String.format("There is several objectEnablers with the same id %d.", enabler.getId()));
            }
            this.objectEnablers.put(enabler.getId(), enabler);
        }

        // Create CoAP non secure endpoint
        nonSecureEndpoint = new CoapEndpoint(localAddress, networkConfig);

        // Create CoAP secure endpoint
        LwM2mObjectEnabler securityEnabler = this.objectEnablers.get(LwM2mId.SECURITY);
        if (securityEnabler == null) {
            throw new IllegalArgumentException("Security object is mandatory");
        }

        Builder builder = new DtlsConnectorConfig.Builder().setAddress(localSecureAddress);
        builder.setPskStore(new SecurityObjectPskStore(securityEnabler));
        builder.setMaxConnections(networkConfig.getInt(Keys.MAX_ACTIVE_PEERS));
        builder.setStaleConnectionThreshold(networkConfig.getLong(Keys.MAX_PEER_INACTIVITY_PERIOD));
        final DTLSConnector dtlsConnector = new DTLSConnector(builder.build());
        secureEndpoint = new CoapEndpoint(dtlsConnector, networkConfig);

        // Create sender
        requestSender = new CaliforniumLwM2mRequestSender(secureEndpoint, nonSecureEndpoint);

        // Create Client Observers
        observers = new LwM2mClientObserverDispatcher();
        observers.addObserver(new LwM2mClientObserverAdapter() {
            @Override
            public void onBootstrapSuccess(ServerInfo bsserver) {
                dtlsConnector.clearConnectionState();
            }

            @Override
            public void onBootstrapTimeout(ServerInfo bsserver) {
                dtlsConnector.clearConnectionState();
            }

            @Override
            public void onRegistrationTimeout(DmServerInfo server) {
                dtlsConnector.clearConnectionState();
            }

            @Override
            public void onUpdateTimeout(DmServerInfo server) {
                dtlsConnector.clearConnectionState();
            }
        });

        // Create registration engine
        bootstrapHandler = new BootstrapHandler(this.objectEnablers);
        engine = new RegistrationEngine(endpoint, this.objectEnablers, requestSender, bootstrapHandler, observers);

        // Create CoAP Server
        clientSideServer = new CoapServer(networkConfig) {
            @Override
            protected Resource createRoot() {
                // Use to handle Delete on "/"
                return new RootResource(bootstrapHandler);
            }
        };
        clientSideServer.addEndpoint(secureEndpoint);
        clientSideServer.addEndpoint(nonSecureEndpoint);

        // Create CoAP resources for each lwm2m Objects.
        for (LwM2mObjectEnabler enabler : objectEnablers) {
            ObjectResource clientObject = new ObjectResource(enabler, bootstrapHandler, new DefaultLwM2mNodeEncoder(),
                    new DefaultLwM2mNodeDecoder());
            clientSideServer.add(clientObject);
        }

        // Create CoAP resources needed for the bootstrap sequence
        clientSideServer.add(new BootstrapResource(bootstrapHandler));
    }

    @Override
    public void start() {
        LOG.info("Starting Leshan client ...");
        clientSideServer.start();
        engine.start();
        LOG.info("Leshan client started [endpoint:{}].", engine.getEndpoint());
    }

    @Override
    public void stop(boolean deregister) {
        LOG.info("Stopping Leshan Client ...");
        engine.stop(deregister);
        clientSideServer.stop();
        LOG.info("Leshan client stopped.");
    }

    @Override
    public void destroy(boolean deregister) {
        LOG.info("Destroying Leshan client ...");
        engine.destroy(deregister);
        clientSideServer.destroy();
        LOG.info("Leshan client destroyed.");
    }

    @Override
    public Collection<LwM2mObjectEnabler> getObjectEnablers() {
        return Collections.unmodifiableCollection(new ArrayList<>(objectEnablers.values()));
    }

    public CoapServer getCoapServer() {
        return clientSideServer;
    }

    public InetSocketAddress getNonSecureAddress() {
        return nonSecureEndpoint.getAddress();
    }

    public InetSocketAddress getSecureAddress() {
        return secureEndpoint.getAddress();
    }

    public void addObserver(LwM2mClientObserver observer) {
        observers.addObserver(observer);
    }

    public void removeObserver(LwM2mClientObserver observer) {
        observers.removeObserver(observer);
    }

    /**
     * Returns the current registration Id (meaningful only because this client implementation supports only one LWM2M
     * server).
     * 
     * @return the client registration Id or <code>null</code> if the client is not registered
     */
    public String getRegistrationId() {
        return engine.getRegistrationId();
    }
}
