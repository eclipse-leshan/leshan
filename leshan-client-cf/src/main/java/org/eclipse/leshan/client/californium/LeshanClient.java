/*******************************************************************************
 * Copyright (c) 2013-2015 Sierra Wireless and others.
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
 *     Zebra Technologies - initial API and implementation
 *     Michał Wadowski (Orange) - Improved compliance with rfc6690
 *******************************************************************************/
package org.eclipse.leshan.client.californium;

import java.net.InetSocketAddress;
import java.security.cert.Certificate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ScheduledExecutorService;

import org.eclipse.californium.core.CoapResource;
import org.eclipse.californium.core.CoapServer;
import org.eclipse.californium.core.config.CoapConfig;
import org.eclipse.californium.core.network.CoapEndpoint;
import org.eclipse.californium.core.server.resources.Resource;
import org.eclipse.californium.elements.config.Configuration;
import org.eclipse.californium.elements.util.ExecutorsUtil;
import org.eclipse.californium.elements.util.NamedThreadFactory;
import org.eclipse.californium.scandium.config.DtlsConnectorConfig.Builder;
import org.eclipse.leshan.client.LwM2mClient;
import org.eclipse.leshan.client.RegistrationUpdateHandler;
import org.eclipse.leshan.client.bootstrap.BootstrapConsistencyChecker;
import org.eclipse.leshan.client.bootstrap.BootstrapHandler;
import org.eclipse.leshan.client.californium.bootstrap.BootstrapResource;
import org.eclipse.leshan.client.californium.object.ObjectResource;
import org.eclipse.leshan.client.californium.request.CaliforniumLwM2mRequestSender;
import org.eclipse.leshan.client.engine.RegistrationEngine;
import org.eclipse.leshan.client.engine.RegistrationEngineFactory;
import org.eclipse.leshan.client.observer.LwM2mClientObserver;
import org.eclipse.leshan.client.observer.LwM2mClientObserverAdapter;
import org.eclipse.leshan.client.observer.LwM2mClientObserverDispatcher;
import org.eclipse.leshan.client.request.LwM2mRequestSender;
import org.eclipse.leshan.client.resource.LwM2mObjectEnabler;
import org.eclipse.leshan.client.resource.LwM2mObjectTree;
import org.eclipse.leshan.client.resource.LwM2mRootEnabler;
import org.eclipse.leshan.client.resource.RootEnabler;
import org.eclipse.leshan.client.resource.listener.ObjectListener;
import org.eclipse.leshan.client.resource.listener.ObjectsListenerAdapter;
import org.eclipse.leshan.client.send.DataSender;
import org.eclipse.leshan.client.send.DataSenderManager;
import org.eclipse.leshan.client.send.SendService;
import org.eclipse.leshan.client.servers.ServerIdentity;
import org.eclipse.leshan.core.californium.EndpointFactory;
import org.eclipse.leshan.core.link.LinkSerializer;
import org.eclipse.leshan.core.link.lwm2m.attributes.LwM2mAttributeParser;
import org.eclipse.leshan.core.model.LwM2mModel;
import org.eclipse.leshan.core.node.LwM2mNode;
import org.eclipse.leshan.core.node.LwM2mPath;
import org.eclipse.leshan.core.node.codec.LwM2mDecoder;
import org.eclipse.leshan.core.node.codec.LwM2mEncoder;
import org.eclipse.leshan.core.request.ContentFormat;
import org.eclipse.leshan.core.response.ErrorCallback;
import org.eclipse.leshan.core.response.ResponseCallback;
import org.eclipse.leshan.core.response.SendResponse;
import org.eclipse.leshan.core.util.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A Lightweight M2M client based on Californium (CoAP implementation) and Scandium (DTLS implementation) which supports
 * only 1 LWM2M server.
 */
public class LeshanClient implements LwM2mClient {

    private static final Logger LOG = LoggerFactory.getLogger(LeshanClient.class);

    private final CoapAPI coapApi;
    private final CoapServer coapServer;
    private final CaliforniumLwM2mRequestSender requestSender;
    private final CaliforniumEndpointsManager endpointsManager;

    private LwM2mObjectTree objectTree;
    private final BootstrapHandler bootstrapHandler;
    private final LwM2mRootEnabler rootEnabler;
    private final LwM2mEncoder encoder;
    private final LwM2mDecoder decoder;
    private final RegistrationEngine engine;
    private final LwM2mClientObserverDispatcher observers;
    private final LinkSerializer linkSerializer;
    private final DataSenderManager dataSenderManager;

    public LeshanClient(String endpoint, InetSocketAddress localAddress,
            List<? extends LwM2mObjectEnabler> objectEnablers, List<DataSender> dataSenders, Configuration coapConfig,
            Builder dtlsConfigBuilder, List<Certificate> trustStore, EndpointFactory endpointFactory,
            RegistrationEngineFactory engineFactory, BootstrapConsistencyChecker checker,
            Map<String, String> additionalAttributes, Map<String, String> bsAdditionalAttributes, LwM2mEncoder encoder,
            LwM2mDecoder decoder, ScheduledExecutorService sharedExecutor, LinkSerializer linkSerializer,
            LwM2mAttributeParser attributeParser) {

        Validate.notNull(endpoint);
        Validate.notEmpty(objectEnablers);
        Validate.notNull(coapConfig);

        objectTree = createObjectTree(objectEnablers);
        rootEnabler = createRootEnabler(objectTree);
        this.decoder = decoder;
        this.encoder = encoder;
        this.linkSerializer = linkSerializer;
        observers = createClientObserverDispatcher();
        bootstrapHandler = createBootstrapHandler(objectTree, checker);
        endpointsManager = createEndpointsManager(localAddress, coapConfig, dtlsConfigBuilder, trustStore,
                endpointFactory);
        requestSender = createRequestSender(endpointsManager, sharedExecutor, encoder, objectTree.getModel(),
                linkSerializer);
        dataSenderManager = createDataSenderManager(dataSenders, rootEnabler, requestSender);
        engine = engineFactory.createRegistratioEngine(endpoint, objectTree, endpointsManager, requestSender,
                bootstrapHandler, observers, additionalAttributes, bsAdditionalAttributes,
                getSupportedContentFormat(decoder, encoder), sharedExecutor);

        coapServer = createCoapServer(coapConfig, sharedExecutor);
        coapServer.add(createBootstrapResource(engine, endpointsManager, bootstrapHandler));
        endpointsManager.setCoapServer(coapServer);
        linkObjectTreeToCoapServer(coapServer, engine, endpointsManager, objectTree, encoder, decoder, linkSerializer,
                attributeParser);
        createRegistrationUpdateHandler(engine, endpointsManager, bootstrapHandler, objectTree);

        coapApi = new CoapAPI();
    }

    protected LwM2mRootEnabler createRootEnabler(LwM2mObjectTree tree) {
        return new RootEnabler(tree);
    }

    protected LwM2mObjectTree createObjectTree(List<? extends LwM2mObjectEnabler> objectEnablers) {
        return new LwM2mObjectTree(this, objectEnablers);
    }

    protected DataSenderManager createDataSenderManager(List<DataSender> dataSenders, LwM2mRootEnabler rootEnabler,
            LwM2mRequestSender requestSender) {
        Map<String, DataSender> dataSenderMap = new HashMap<>();
        for (DataSender dataSender : dataSenders) {
            dataSenderMap.put(dataSender.getName(), dataSender);
        }
        return new DataSenderManager(dataSenderMap, rootEnabler, requestSender);
    }

    protected LwM2mClientObserverDispatcher createClientObserverDispatcher() {
        LwM2mClientObserverDispatcher observer = new LwM2mClientObserverDispatcher();
        observer.addObserver(new LwM2mClientObserverAdapter() {
            @Override
            public void onUnexpectedError(Throwable unexpectedError) {
                LeshanClient.this.destroy(false);
            }
        });
        return observer;
    }

    protected BootstrapHandler createBootstrapHandler(LwM2mObjectTree objectTree, BootstrapConsistencyChecker checker) {
        return new BootstrapHandler(objectTree.getObjectEnablers(), checker);
    }

    protected CoapServer createCoapServer(Configuration coapConfig, ScheduledExecutorService sharedExecutor) {
        // create coap server
        CoapServer coapServer = new CoapServer(coapConfig) {
            @Override
            protected Resource createRoot() {
                // Use to handle Delete on "/"
                return new org.eclipse.leshan.client.californium.RootResource(engine, endpointsManager,
                        bootstrapHandler, this, rootEnabler, encoder, decoder, linkSerializer);
            }
        };

        // configure executors
        if (sharedExecutor != null) {
            coapServer.setExecutors(sharedExecutor, sharedExecutor, true);
        } else {
            // use same executor as main and secondary one.
            ScheduledExecutorService executor = ExecutorsUtil.newScheduledThreadPool(
                    coapConfig.get(CoapConfig.PROTOCOL_STAGE_THREAD_COUNT),
                    new NamedThreadFactory("CoapServer(main)#"));
            coapServer.setExecutors(executor, executor, false);
        }
        return coapServer;
    }

    protected void linkObjectTreeToCoapServer(final CoapServer coapServer, final RegistrationEngine registrationEngine,
            final CaliforniumEndpointsManager endpointsManager, LwM2mObjectTree objectTree, final LwM2mEncoder encoder,
            final LwM2mDecoder decoder, LinkSerializer linkSerializer, LwM2mAttributeParser attributeParser) {

        // Create CoAP resources for each lwm2m Objects.
        for (LwM2mObjectEnabler enabler : objectTree.getObjectEnablers().values()) {
            CoapResource clientObject = createObjectResource(enabler, registrationEngine, endpointsManager, encoder,
                    decoder, linkSerializer, attributeParser);
            coapServer.add(clientObject);
        }

        // listen object tree
        objectTree.addListener(new ObjectsListenerAdapter() {
            @Override
            public void objectAdded(LwM2mObjectEnabler object) {
                CoapResource clientObject = createObjectResource(object, registrationEngine, endpointsManager, encoder,
                        decoder, linkSerializer, attributeParser);
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

    protected CoapResource createObjectResource(LwM2mObjectEnabler enabler, RegistrationEngine registrationEngine,
            CaliforniumEndpointsManager endpointsManager, LwM2mEncoder encoder, LwM2mDecoder decoder,
            LinkSerializer linkSerializer, LwM2mAttributeParser attributeParser) {
        return new ObjectResource(enabler, registrationEngine, endpointsManager, encoder, decoder, linkSerializer,
                attributeParser);
    }

    protected CoapResource createBootstrapResource(RegistrationEngine registrationEngine,
            CaliforniumEndpointsManager endpointsManager, BootstrapHandler bootstrapHandler) {
        return new BootstrapResource(registrationEngine, endpointsManager, bootstrapHandler);
    }

    protected CaliforniumEndpointsManager createEndpointsManager(InetSocketAddress localAddress,
            Configuration coapConfig, Builder dtlsConfigBuilder, List<Certificate> trustStore,
            EndpointFactory endpointFactory) {
        return new CaliforniumEndpointsManager(localAddress, coapConfig, dtlsConfigBuilder, trustStore,
                endpointFactory);
    }

    protected CaliforniumLwM2mRequestSender createRequestSender(CaliforniumEndpointsManager endpointsManager,
            ScheduledExecutorService executor, LwM2mEncoder encoder, LwM2mModel model, LinkSerializer linkSerializer) {
        return new CaliforniumLwM2mRequestSender(endpointsManager, executor, encoder, model, linkSerializer);
    }

    protected RegistrationUpdateHandler createRegistrationUpdateHandler(RegistrationEngine engine,
            CaliforniumEndpointsManager endpointsManager, BootstrapHandler bootstrapHandler,
            LwM2mObjectTree objectTree) {
        RegistrationUpdateHandler registrationUpdateHandler = new RegistrationUpdateHandler(engine, bootstrapHandler);
        registrationUpdateHandler.listen(objectTree);
        return registrationUpdateHandler;
    }

    protected Set<ContentFormat> getSupportedContentFormat(LwM2mDecoder decoder, LwM2mEncoder encoder) {
        Set<ContentFormat> supportedContentFormat = new TreeSet<>();
        supportedContentFormat.addAll(decoder.getSupportedContentFormat());
        supportedContentFormat.addAll(encoder.getSupportedContentFormat());
        return supportedContentFormat;
    }

    @Override
    public void start() {
        LOG.info("Starting Leshan client ...");
        endpointsManager.start();
        engine.start();
        objectTree.start();
        dataSenderManager.start();

        if (LOG.isInfoEnabled()) {
            LOG.info("Leshan client[endpoint:{}] started.", engine.getEndpoint());
        }
    }

    @Override
    public void stop(boolean deregister) {
        LOG.info("Stopping Leshan Client ...");
        dataSenderManager.stop();
        engine.stop(deregister);
        endpointsManager.stop();
        objectTree.stop();

        LOG.info("Leshan client stopped.");
    }

    @Override
    public void destroy(boolean deregister) {
        LOG.info("Destroying Leshan client ...");
        dataSenderManager.destroy();
        engine.destroy(deregister);
        endpointsManager.destroy();
        requestSender.destroy();
        objectTree.destroy();

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

    @Override
    public void triggerRegistrationUpdate(ServerIdentity server) {
        engine.triggerRegistrationUpdate(server);
    }

    @Override
    public boolean triggerClientInitiatedBootstrap(boolean deregister) {
        return engine.triggerClientInitiatedBootstrap(deregister);
    }

    @Override
    public SendService getSendService() {
        return new SendService() {

            @Override
            public SendResponse sendData(ServerIdentity server, ContentFormat format, List<String> paths,
                    long timeoutInMs) throws InterruptedException {
                Validate.notNull(server);
                Validate.notEmpty(paths);

                Map<LwM2mPath, LwM2mNode> collectedData = dataSenderManager.getCurrentValues(server,
                        LwM2mPath.getLwM2mPathList(paths));
                return dataSenderManager.sendData(server, format, collectedData, timeoutInMs);
            }

            @Override
            public void sendData(ServerIdentity server, ContentFormat format, List<String> paths, long timeoutInMs,
                    ResponseCallback<SendResponse> onResponse, ErrorCallback onError) {
                Validate.notNull(server);
                Validate.notEmpty(paths);
                Validate.notNull(onResponse);
                Validate.notNull(onError);

                Map<LwM2mPath, LwM2mNode> collectedData = dataSenderManager.getCurrentValues(server,
                        LwM2mPath.getLwM2mPathList(paths));
                dataSenderManager.sendData(server, format, collectedData, onResponse, onError, timeoutInMs);

            }

            @Override
            public DataSender getDataSender(String senderName) {
                return dataSenderManager.getDataSender(senderName);
            }

            @Override
            public <T extends DataSender> T getDataSender(String senderName, Class<T> senderSubType) {
                return dataSenderManager.getDataSender(senderName, senderSubType);
            }
        };
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
         * Returns the current {@link CoapEndpoint} used to communicate with the given server.
         *
         * @return the {@link CoapEndpoint} used to communicate to LWM2M server.
         */
        public CoapEndpoint getEndpoint(ServerIdentity server) {
            return (CoapEndpoint) endpointsManager.getEndpoint(server);
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
     * Returns the registration Id for the given server.
     *
     * @return the client registration Id or <code>null</code> if the client is not registered
     */
    public String getRegistrationId(ServerIdentity server) {
        return engine.getRegistrationId(server);
    }

    /**
     * @return All the registered Server indexed by the corresponding registration id;
     */
    public Map<String, ServerIdentity> getRegisteredServers() {
        return engine.getRegisteredServers();
    }

    /**
     * Returns the current {@link InetSocketAddress} use to communicate with the given server.
     *
     * @return the address used to connect to the server or <code>null</code> if the client is not started.
     */
    public InetSocketAddress getAddress(ServerIdentity server) {
        return endpointsManager.getEndpoint(server).getAddress();
    }
}
