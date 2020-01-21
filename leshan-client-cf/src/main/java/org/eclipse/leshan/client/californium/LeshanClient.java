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
import java.util.List;
import java.util.Map;

import org.eclipse.californium.core.CoapServer;
import org.eclipse.californium.core.network.config.NetworkConfig;
import org.eclipse.californium.core.server.resources.Resource;
import org.eclipse.californium.scandium.config.DtlsConnectorConfig.Builder;
import org.eclipse.leshan.client.LwM2mClient;
import org.eclipse.leshan.client.RegistrationEngine;
import org.eclipse.leshan.client.RegistrationUpdateHandler;
import org.eclipse.leshan.client.bootstrap.BootstrapHandler;
import org.eclipse.leshan.client.californium.bootstrap.BootstrapResource;
import org.eclipse.leshan.client.californium.object.ObjectResource;
import org.eclipse.leshan.client.californium.request.CaliforniumLwM2mRequestSender;
import org.eclipse.leshan.client.observer.LwM2mClientObserver;
import org.eclipse.leshan.client.observer.LwM2mClientObserverDispatcher;
import org.eclipse.leshan.client.resource.LwM2mObjectEnabler;
import org.eclipse.leshan.client.resource.LwM2mObjectTree;
import org.eclipse.leshan.client.resource.listener.ObjectsListenerAdapter;
import org.eclipse.leshan.core.californium.EndpointFactory;
import org.eclipse.leshan.core.node.codec.LwM2mNodeDecoder;
import org.eclipse.leshan.core.node.codec.LwM2mNodeEncoder;
import org.eclipse.leshan.util.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A Lightweight M2M client based on Californium (CoAP implementation).
 */
public class LeshanClient implements LwM2mClient {

    private static final Logger LOG = LoggerFactory.getLogger(LeshanClient.class);

    private final CoapServer clientSideServer;
    private final CaliforniumLwM2mRequestSender requestSender;
    private final RegistrationEngine engine;
    private final BootstrapHandler bootstrapHandler;
    private final LwM2mClientObserverDispatcher observers;
    private LwM2mObjectTree objectTree;

    private final CaliforniumEndpointsManager endpointsManager;

    public LeshanClient(String endpoint, InetSocketAddress localAddress,
            List<? extends LwM2mObjectEnabler> objectEnablers, NetworkConfig coapConfig, Builder dtlsConfigBuilder,
            EndpointFactory endpointFactory, Map<String, String> additionalAttributes, final LwM2mNodeEncoder encoder,
            final LwM2mNodeDecoder decoder) {

        Validate.notNull(endpoint);
        Validate.notEmpty(objectEnablers);
        Validate.notNull(coapConfig);

        // create ObjectTree
        objectTree = new LwM2mObjectTree(this, objectEnablers);

        // Create Client Observers
        observers = new LwM2mClientObserverDispatcher();

        bootstrapHandler = new BootstrapHandler(objectTree.getObjectEnablers());

        // Create CoAP Server
        clientSideServer = new CoapServer(coapConfig) {
            @Override
            protected Resource createRoot() {
                // Use to handle Delete on "/"
                return new org.eclipse.leshan.client.californium.RootResource(bootstrapHandler, this);
            }
        };

        // Create CoAP resources for each lwm2m Objects.
        for (LwM2mObjectEnabler enabler : objectEnablers) {
            ObjectResource clientObject = new ObjectResource(enabler, bootstrapHandler, encoder, decoder);
            clientSideServer.add(clientObject);
        }
        objectTree.addListener(new ObjectsListenerAdapter() {
            @Override
            public void objectAdded(LwM2mObjectEnabler object) {
                ObjectResource clientObject = new ObjectResource(object, bootstrapHandler, encoder, decoder);
                clientSideServer.add(clientObject);
            }

            @Override
            public void objectRemoved(LwM2mObjectEnabler object) {
                Resource resource = clientSideServer.getRoot().getChild(Integer.toString(object.getId()));
                clientSideServer.remove(resource);
            }
        });

        // Create CoAP resources needed for the bootstrap sequence
        clientSideServer.add(new BootstrapResource(bootstrapHandler));

        // Create EndpointHandler
        endpointsManager = new CaliforniumEndpointsManager(clientSideServer, localAddress, coapConfig,
                dtlsConfigBuilder, endpointFactory);

        // Create sender
        requestSender = new CaliforniumLwM2mRequestSender(endpointsManager);

        // Create registration engine
        engine = new RegistrationEngine(endpoint, objectTree.getObjectEnablers(), endpointsManager, requestSender,
                bootstrapHandler, observers, additionalAttributes);

        RegistrationUpdateHandler registrationUpdateHandler = new RegistrationUpdateHandler(engine, bootstrapHandler);
        registrationUpdateHandler.listen(objectTree);
    }

    @Override
    public void start() {
        LOG.info("Starting Leshan client ...");
        endpointsManager.start();
        engine.start();
        if (LOG.isInfoEnabled()) {
            LOG.info("Leshan client[endpoint:{}] started.", engine.getEndpoint());
        }
    }

    @Override
    public void stop(boolean deregister) {
        LOG.info("Stopping Leshan Client ...");
        engine.stop(deregister);
        endpointsManager.stop();
        LOG.info("Leshan client stopped.");
    }

    @Override
    public void destroy(boolean deregister) {
        LOG.info("Destroying Leshan client ...");
        engine.destroy(deregister);
        endpointsManager.destroy();
        LOG.info("Leshan client destroyed.");
    }

    @Override
    public void triggerRegistrationUpdate() {
        engine.triggerRegistrationUpdate();
    }

    @Override
    public LwM2mObjectTree getObjectTree() {
        return objectTree;
    }

    public CoapServer getCoapServer() {
        return clientSideServer;
    }

    public InetSocketAddress getAddress() {
        return endpointsManager.getEndpoint(null).getAddress();
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
