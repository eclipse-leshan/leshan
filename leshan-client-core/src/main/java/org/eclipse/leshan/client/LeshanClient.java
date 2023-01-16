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
package org.eclipse.leshan.client;

import java.security.cert.Certificate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ScheduledExecutorService;

import org.eclipse.leshan.client.bootstrap.BootstrapConsistencyChecker;
import org.eclipse.leshan.client.bootstrap.BootstrapHandler;
import org.eclipse.leshan.client.endpoint.ClientEndpointToolbox;
import org.eclipse.leshan.client.endpoint.DefaultEndpointsManager;
import org.eclipse.leshan.client.endpoint.LwM2mClientEndpoint;
import org.eclipse.leshan.client.endpoint.LwM2mClientEndpointsProvider;
import org.eclipse.leshan.client.engine.RegistrationEngine;
import org.eclipse.leshan.client.engine.RegistrationEngineFactory;
import org.eclipse.leshan.client.observer.LwM2mClientObserver;
import org.eclipse.leshan.client.observer.LwM2mClientObserverAdapter;
import org.eclipse.leshan.client.observer.LwM2mClientObserverDispatcher;
import org.eclipse.leshan.client.request.DefaultDownlinkReceiver;
import org.eclipse.leshan.client.request.DefaultUplinkRequestSender;
import org.eclipse.leshan.client.request.DownlinkRequestReceiver;
import org.eclipse.leshan.client.request.UplinkRequestSender;
import org.eclipse.leshan.client.resource.LwM2mObjectEnabler;
import org.eclipse.leshan.client.resource.LwM2mObjectTree;
import org.eclipse.leshan.client.resource.LwM2mRootEnabler;
import org.eclipse.leshan.client.resource.RootEnabler;
import org.eclipse.leshan.client.send.DataSender;
import org.eclipse.leshan.client.send.DataSenderManager;
import org.eclipse.leshan.client.send.SendService;
import org.eclipse.leshan.client.servers.ServerIdentity;
import org.eclipse.leshan.core.link.LinkSerializer;
import org.eclipse.leshan.core.link.lwm2m.attributes.LwM2mAttributeParser;
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

    private final LwM2mClientEndpointsProvider endpointsProvider;
    private final EndpointsManager endpointsManager;
    private final UplinkRequestSender requestSender;

    private final LwM2mObjectTree objectTree;
    private final BootstrapHandler bootstrapHandler;
    private final LwM2mRootEnabler rootEnabler;
    private final RegistrationEngine engine;
    private final LwM2mClientObserverDispatcher observers;
    private final DataSenderManager dataSenderManager;

    public LeshanClient(String endpoint, List<? extends LwM2mObjectEnabler> objectEnablers,
            List<DataSender> dataSenders, List<Certificate> trustStore, RegistrationEngineFactory engineFactory,
            BootstrapConsistencyChecker checker, Map<String, String> additionalAttributes,
            Map<String, String> bsAdditionalAttributes, LwM2mEncoder encoder, LwM2mDecoder decoder,
            ScheduledExecutorService sharedExecutor, LinkSerializer linkSerializer,
            LwM2mAttributeParser attributeParser, LwM2mClientEndpointsProvider endpointsProvider) {

        Validate.notNull(endpoint);
        Validate.notEmpty(objectEnablers);
        Validate.notNull(checker);

        objectTree = createObjectTree(objectEnablers);
        List<String> errors = checker.checkconfig(objectTree.getObjectEnablers());
        if (errors != null) {
            throw new IllegalArgumentException(
                    String.format("Invalid 'ObjectEnabler' Setting : \n - %s", String.join("\n - ", errors)));
        }

        this.endpointsProvider = endpointsProvider;
        rootEnabler = createRootEnabler(objectTree);
        observers = createClientObserverDispatcher();
        bootstrapHandler = createBoostrapHandler(objectTree, checker);

        ClientEndpointToolbox toolbox = new ClientEndpointToolbox(decoder, encoder, linkSerializer,
                objectTree.getModel(), attributeParser);
        endpointsManager = createEndpointsManager(this.endpointsProvider, toolbox, trustStore);
        requestSender = createRequestSender(this.endpointsProvider);
        dataSenderManager = createDataSenderManager(dataSenders, rootEnabler, requestSender);

        engine = engineFactory.createRegistratioEngine(endpoint, objectTree, endpointsManager, requestSender,
                bootstrapHandler, observers, additionalAttributes, bsAdditionalAttributes,
                getSupportedContentFormat(decoder, encoder), sharedExecutor);

        DownlinkRequestReceiver requestReceiver = createRequestReceiver(bootstrapHandler, rootEnabler, objectTree,
                engine);
        createRegistrationUpdateHandler(engine, endpointsManager, bootstrapHandler, objectTree);

        endpointsProvider.init(objectTree, requestReceiver, toolbox);
    }

    protected LwM2mRootEnabler createRootEnabler(LwM2mObjectTree tree) {
        return new RootEnabler(tree);
    }

    protected LwM2mObjectTree createObjectTree(List<? extends LwM2mObjectEnabler> objectEnablers) {
        return new LwM2mObjectTree(this, objectEnablers);
    }

    protected DataSenderManager createDataSenderManager(List<DataSender> dataSenders, LwM2mRootEnabler rootEnabler,
            UplinkRequestSender requestSender) {
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

    protected BootstrapHandler createBoostrapHandler(LwM2mObjectTree objectTree, BootstrapConsistencyChecker checker) {
        return new BootstrapHandler(objectTree.getObjectEnablers(), checker);
    }

    protected DownlinkRequestReceiver createRequestReceiver(BootstrapHandler bootstrapHandler,
            LwM2mRootEnabler rootEnabler, LwM2mObjectTree objectTree, RegistrationEngine registrationEngine) {
        return new DefaultDownlinkReceiver(bootstrapHandler, rootEnabler, objectTree, registrationEngine);
    }

    protected EndpointsManager createEndpointsManager(LwM2mClientEndpointsProvider endpointProvider,
            ClientEndpointToolbox toolbox, List<Certificate> trustStore) {
        return new DefaultEndpointsManager(endpointProvider, toolbox, trustStore);
    }

    protected UplinkRequestSender createRequestSender(LwM2mClientEndpointsProvider endpointsProvider) {
        return new DefaultUplinkRequestSender(endpointsProvider);
    }

    protected RegistrationUpdateHandler createRegistrationUpdateHandler(RegistrationEngine engine,
            EndpointsManager endpointsManager, BootstrapHandler bootstrapHandler, LwM2mObjectTree objectTree) {
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
        endpointsProvider.destroy();
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

    public LwM2mClientEndpoint getEndpoint(ServerIdentity server) {
        return endpointsProvider.getEndpoint(server);
    }
}
