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
import java.util.concurrent.ScheduledExecutorService;

import org.eclipse.californium.core.CoapResource;
import org.eclipse.californium.core.CoapServer;
import org.eclipse.californium.core.network.CoapEndpoint;
import org.eclipse.californium.core.network.config.NetworkConfig;
import org.eclipse.californium.core.server.resources.Resource;
import org.eclipse.californium.elements.util.ExecutorsUtil;
import org.eclipse.californium.elements.util.NamedThreadFactory;
import org.eclipse.californium.scandium.config.DtlsConnectorConfig.Builder;
import org.eclipse.leshan.client.LwM2mClient;
import org.eclipse.leshan.client.RegistrationUpdateHandler;
import org.eclipse.leshan.client.bootstrap.BootstrapHandler;
import org.eclipse.leshan.client.californium.bootstrap.BootstrapResource;
import org.eclipse.leshan.client.californium.object.ObjectResource;
import org.eclipse.leshan.client.californium.request.CaliforniumLwM2mRequestSender;
import org.eclipse.leshan.client.engine.RegistrationEngine;
import org.eclipse.leshan.client.engine.RegistrationEngineFactory;
import org.eclipse.leshan.client.observer.LwM2mClientObserver;
import org.eclipse.leshan.client.observer.LwM2mClientObserverDispatcher;
import org.eclipse.leshan.client.resource.LwM2mObjectEnabler;
import org.eclipse.leshan.client.resource.LwM2mObjectTree;
import org.eclipse.leshan.client.resource.listener.ObjectListener;
import org.eclipse.leshan.client.resource.listener.ObjectsListenerAdapter;
import org.eclipse.leshan.core.californium.EndpointFactory;
import org.eclipse.leshan.core.node.codec.LwM2mNodeDecoder;
import org.eclipse.leshan.core.node.codec.LwM2mNodeEncoder;
import org.eclipse.leshan.util.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A Lightweight M2M client based on Californium (CoAP implementation) which supports only 1 LWM2M server.
 */
public class LeshanClient implements LwM2mClient {

    private static final Logger LOG = LoggerFactory.getLogger(LeshanClient.class);

    private final CoapAPI coapApi;
    private final CoapServer coapServer;
    private final CaliforniumLwM2mRequestSender requestSender;
    private final CaliforniumEndpointsManager endpointsManager;

    private LwM2mObjectTree objectTree;
    private final BootstrapHandler bootstrapHandler;
    private final RegistrationEngine engine;
    private final LwM2mClientObserverDispatcher observers;

    public LeshanClient(String endpoint, InetSocketAddress localAddress,
            List<? extends LwM2mObjectEnabler> objectEnablers, NetworkConfig coapConfig, Builder dtlsConfigBuilder,
            EndpointFactory endpointFactory, RegistrationEngineFactory engineFactory,
            Map<String, String> additionalAttributes, LwM2mNodeEncoder encoder, LwM2mNodeDecoder decoder,
            ScheduledExecutorService sharedExecutor) {

        Validate.notNull(endpoint);
        Validate.notEmpty(objectEnablers);
        Validate.notNull(coapConfig);

        objectTree = createObjectTree(objectEnablers);
        observers = createClientObserverDispatcher();
        bootstrapHandler = createBoostrapHandler(objectTree);
        coapServer = createCoapServer(coapConfig, sharedExecutor);
        linkObjectTreeToCoapServer(coapServer, bootstrapHandler, objectTree, encoder, decoder);
        coapServer.add(createBootstrapResource(bootstrapHandler));
        endpointsManager = createEndpointsManager(coapServer, localAddress, coapConfig, dtlsConfigBuilder,
                endpointFactory);
        requestSender = createRequestSender(endpointsManager, sharedExecutor);
        engine = engineFactory.createRegistratioEngine(endpoint, objectTree, endpointsManager, requestSender,
                bootstrapHandler, observers, additionalAttributes, sharedExecutor);
        createRegistrationUpdateHandler(engine, endpointsManager, bootstrapHandler, objectTree);

        coapApi = new CoapAPI();
    }

    protected LwM2mObjectTree createObjectTree(List<? extends LwM2mObjectEnabler> objectEnablers) {
        return new LwM2mObjectTree(this, objectEnablers);
    }

    protected LwM2mClientObserverDispatcher createClientObserverDispatcher() {
        return new LwM2mClientObserverDispatcher();
    }

    protected BootstrapHandler createBoostrapHandler(LwM2mObjectTree objectTree) {
        return new BootstrapHandler(objectTree.getObjectEnablers());
    }

    protected CoapServer createCoapServer(NetworkConfig coapConfig, ScheduledExecutorService sharedExecutor) {
        // create coap server
        CoapServer coapServer = new CoapServer(coapConfig) {
            @Override
            protected Resource createRoot() {
                // Use to handle Delete on "/"
                return new org.eclipse.leshan.client.californium.RootResource(bootstrapHandler, this);
            }
        };

        // configure executors
        if (sharedExecutor != null) {
            coapServer.setExecutors(sharedExecutor, sharedExecutor, true);
        } else {
            // use same executor as main and secondary one.
            ScheduledExecutorService executor = ExecutorsUtil.newScheduledThreadPool(
                    coapConfig.getInt(NetworkConfig.Keys.PROTOCOL_STAGE_THREAD_COUNT),
                    new NamedThreadFactory("CoapServer(main)#"));
            coapServer.setExecutors(executor, executor, false);
        }
        return coapServer;
    }

    protected void linkObjectTreeToCoapServer(final CoapServer coapServer, final BootstrapHandler bootstrapHandler,
            LwM2mObjectTree objectTree, final LwM2mNodeEncoder encoder, final LwM2mNodeDecoder decoder) {

        // Create CoAP resources for each lwm2m Objects.
        for (LwM2mObjectEnabler enabler : objectTree.getObjectEnablers().values()) {
            CoapResource clientObject = createObjectResource(enabler, bootstrapHandler, encoder, decoder);
            coapServer.add(clientObject);
        }

        // listen object tree
        objectTree.addListener(new ObjectsListenerAdapter() {
            @Override
            public void objectAdded(LwM2mObjectEnabler object) {
                CoapResource clientObject = createObjectResource(object, bootstrapHandler, encoder, decoder);
                coapServer.add(clientObject);
            }

            @Override
            public void objectRemoved(LwM2mObjectEnabler object) {
                Resource resource = coapServer.getRoot().getChild(Integer.toString(object.getId()));
                if (resource instanceof ObjectListener) {
                    object.removeListener((ObjectListener) (resource));
                }
                coapServer.remove(resource);
            }
        });
    }

    protected CoapResource createObjectResource(LwM2mObjectEnabler enabler, BootstrapHandler bootstrapHandler,
            LwM2mNodeEncoder encoder, LwM2mNodeDecoder decoder) {
        return new ObjectResource(enabler, bootstrapHandler, encoder, decoder);
    }

    protected CoapResource createBootstrapResource(BootstrapHandler bootstrapHandler) {
        return new BootstrapResource(bootstrapHandler);
    }

    protected CaliforniumEndpointsManager createEndpointsManager(CoapServer coapServer, InetSocketAddress localAddress,
            NetworkConfig coapConfig, Builder dtlsConfigBuilder, EndpointFactory endpointFactory) {
        return new CaliforniumEndpointsManager(coapServer, localAddress, coapConfig, dtlsConfigBuilder,
                endpointFactory);
    }

    protected CaliforniumLwM2mRequestSender createRequestSender(CaliforniumEndpointsManager endpointsManager,
            ScheduledExecutorService executor) {
        return new CaliforniumLwM2mRequestSender(endpointsManager, executor);
    }

    protected RegistrationUpdateHandler createRegistrationUpdateHandler(RegistrationEngine engine,
            CaliforniumEndpointsManager endpointsManager, BootstrapHandler bootstrapHandler,
            LwM2mObjectTree objectTree) {
        RegistrationUpdateHandler registrationUpdateHandler = new RegistrationUpdateHandler(engine, bootstrapHandler);
        registrationUpdateHandler.listen(objectTree);
        return registrationUpdateHandler;
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
        requestSender.destroy();
        LOG.info("Leshan client destroyed.");
    }

    @Override
    public LwM2mObjectTree getObjectTree() {
        return objectTree;
    }

    @Override
    public void triggerRegistrationUpdate() {
        engine.triggerRegistrationUpdate();
    }

    /**
     * A CoAP API, generally needed if you want to access to underlying CoAP layer.
     */
    public CoapAPI coap() {
        return coapApi;
    }

    public class CoapAPI {
        /**
         * @return the underlying {@link CoapServer}
         */
        public CoapServer getServer() {
            return coapServer;
        }

        /**
         * Returns the current {@link CoapEndpoint} used to communicate with the server.
         * <p>
         * A different endpoint address should be used by connected server, so this method only make sense as this
         * current implementation supports only one LWM2M server.
         * 
         * @return the {@link CoapEndpoint} used to communicate to LWM2M server.
         */
        public CoapEndpoint getEndpoint() {
            return (CoapEndpoint) endpointsManager.getEndpoint(null);
        }
    }

    /**
     * Add listener to observe client lifecycle (bootstrap, register, update, deregister).
     */
    public void addObserver(LwM2mClientObserver observer) {
        observers.addObserver(observer);
    }

    /**
     * Remove the given {@link LwM2mClientObserver}.
     */
    public void removeObserver(LwM2mClientObserver observer) {
        observers.removeObserver(observer);
    }

    /**
     * Returns the current registration Id.
     * <p>
     * Client should have 1 registration Id by connected server, so this method only make sense current implementation
     * supports only one LWM2M server.
     * 
     * @return the client registration Id or <code>null</code> if the client is not registered
     */
    public String getRegistrationId() {
        return engine.getRegistrationId();
    }

    /**
     * Returns the current {@link InetSocketAddress} use to communicate with the server.
     * <p>
     * A different endpoint/socket address should be used by connected server, so this method only make sense as this
     * current implementation supports only one LWM2M server.
     * 
     * @return the address used to connect to the server or <code>null</code> if the client is not started.
     */
    public InetSocketAddress getAddress() {
        return endpointsManager.getEndpoint(null).getAddress();
    }
}
