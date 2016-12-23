/*******************************************************************************
 * Copyright (c) 2016 Bosch Software Innovations GmbH and others.
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
 *     Alexander Ellwein, Daniel Maier (Bosch Software Innovations GmbH)
 *                                - initial API and implementation
 *******************************************************************************/
package org.eclipse.leshan.integration.tests;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.concurrent.CountDownLatch;

import org.eclipse.californium.core.CoapServer;
import org.eclipse.californium.core.network.CoapEndpoint;
import org.eclipse.californium.core.network.Endpoint;
import org.eclipse.californium.core.network.config.NetworkConfig;
import org.eclipse.californium.core.network.config.NetworkConfig.Keys;
import org.eclipse.leshan.LwM2mId;
import org.eclipse.leshan.client.object.Device;
import org.eclipse.leshan.client.object.Security;
import org.eclipse.leshan.client.object.Server;
import org.eclipse.leshan.client.resource.LwM2mObjectEnabler;
import org.eclipse.leshan.client.resource.ObjectsInitializer;
import org.eclipse.leshan.core.node.codec.DefaultLwM2mNodeDecoder;
import org.eclipse.leshan.core.node.codec.DefaultLwM2mNodeEncoder;
import org.eclipse.leshan.core.node.codec.LwM2mNodeDecoder;
import org.eclipse.leshan.core.node.codec.LwM2mNodeEncoder;
import org.eclipse.leshan.core.request.BindingMode;
import org.eclipse.leshan.core.response.ExecuteResponse;
import org.eclipse.leshan.integration.tests.util.QueueModeLeshanServer;
import org.eclipse.leshan.integration.tests.util.QueuedModeLeshanClient;
import org.eclipse.leshan.server.californium.impl.CaliforniumLwM2mRequestSender;
import org.eclipse.leshan.server.californium.impl.InMemoryRegistrationStore;
import org.eclipse.leshan.server.californium.impl.ObservationServiceImpl;
import org.eclipse.leshan.server.californium.impl.RegisterResource;
import org.eclipse.leshan.server.client.Registration;
import org.eclipse.leshan.server.impl.InMemorySecurityStore;
import org.eclipse.leshan.server.impl.LwM2mRequestSenderImpl;
import org.eclipse.leshan.server.impl.RegistrationServiceImpl;
import org.eclipse.leshan.server.model.LwM2mModelProvider;
import org.eclipse.leshan.server.model.StandardModelProvider;
import org.eclipse.leshan.server.queue.impl.InMemoryMessageStore;
import org.eclipse.leshan.server.queue.impl.QueuedRequestSender;
import org.eclipse.leshan.server.registration.RegistrationHandler;
import org.eclipse.leshan.server.request.LwM2mRequestSender;
import org.eclipse.leshan.server.security.EditableSecurityStore;

/**
 * IntegrationTestHelper, which is intended to create a client/server environment for testing the Queue Mode feature.
 */
public class QueueModeIntegrationTestHelper extends IntegrationTestHelper {

    public static final long ACK_TIMEOUT = 700L;
    public static final long CUSTOM_LIFETIME = LIFETIME + 6;
    private Endpoint noSecureEndpoint;
    private Endpoint secureEndpoint;
    QueueModeLeshanServer server;
    private CoapServer coapServer;
    private final NetworkConfig networkConfig;

    public QueueModeIntegrationTestHelper() {
        networkConfig = new NetworkConfig();
        networkConfig.setLong(Keys.ACK_TIMEOUT, ACK_TIMEOUT);
        networkConfig.setInt(Keys.MAX_RETRANSMIT, 0);
    }

    @Override
    public void createServer() {
        // monitor client registration
        super.registerLatch = new CountDownLatch(1);
        super.deregisterLatch = new CountDownLatch(1);
        super.updateLatch = new CountDownLatch(1);

        InMemoryRegistrationStore registrationStore = new InMemoryRegistrationStore();
        RegistrationServiceImpl registrationService = new RegistrationServiceImpl(registrationStore);
        EditableSecurityStore securityStore = new InMemorySecurityStore();

        // coap server
        noSecureEndpoint = new CoapEndpoint(
                new InetSocketAddress(InetAddress.getLoopbackAddress(), networkConfig.getInt(Keys.COAP_PORT)),
                networkConfig, registrationStore);
        secureEndpoint = new CoapEndpoint(
                new InetSocketAddress(InetAddress.getLoopbackAddress(), networkConfig.getInt(Keys.COAP_SECURE_PORT)),
                networkConfig, registrationStore);
        coapServer = new CoapServer(networkConfig);
        coapServer.addEndpoint(noSecureEndpoint);
        coapServer.addEndpoint(secureEndpoint);

        RegisterResource rdResource = new RegisterResource(new RegistrationHandler(registrationService, securityStore));
        coapServer.add(rdResource);

        InMemoryMessageStore inMemoryMessageStore = new InMemoryMessageStore();
        LwM2mModelProvider modelProvider = new StandardModelProvider();
        LwM2mNodeEncoder encoder = new DefaultLwM2mNodeEncoder();
        LwM2mNodeDecoder decoder = new DefaultLwM2mNodeDecoder();
        ObservationServiceImpl observationService = new ObservationServiceImpl(
                registrationStore, modelProvider, decoder);
        observationService.setSecureEndpoint(secureEndpoint);
        secureEndpoint.addNotificationListener(observationService);
        observationService.setNonSecureEndpoint(noSecureEndpoint);
        noSecureEndpoint.addNotificationListener(observationService);
        LwM2mRequestSender delegateSender = new CaliforniumLwM2mRequestSender(new HashSet<>(coapServer.getEndpoints()),
                observationService, modelProvider, encoder, decoder);
        LwM2mRequestSender secondDelegateSender = new CaliforniumLwM2mRequestSender(
                new HashSet<>(coapServer.getEndpoints()), observationService, modelProvider, encoder, decoder);
        QueuedRequestSender queueRequestSender = QueuedRequestSender.builder().setMessageStore(inMemoryMessageStore)
                .setRequestSender(secondDelegateSender).setRegistrationService(registrationService)
                .setObservationService(observationService).build();
        LwM2mRequestSender lwM2mRequestSender = new LwM2mRequestSenderImpl(delegateSender, queueRequestSender);

        server = new QueueModeLeshanServer(coapServer, registrationService, observationService, securityStore,
                modelProvider, lwM2mRequestSender, inMemoryMessageStore);
    }

    @Override
    public void createClient() {
        client = createClient(CUSTOM_LIFETIME);
    }

    public QueuedModeLeshanClient createClient(long lifeTime) {
        ObjectsInitializer initializer = new ObjectsInitializer();
        initializer.setInstancesForObject(LwM2mId.SECURITY,
                Security.noSec("coap://" + noSecureEndpoint.getAddress().getHostString() + ":"
                        + noSecureEndpoint.getAddress().getPort(), 12345));
        if (lifeTime == 0) {
            initializer.setInstancesForObject(LwM2mId.SERVER,
                    new Server(12345, CUSTOM_LIFETIME, BindingMode.UQ, false));
        } else {
            initializer.setInstancesForObject(LwM2mId.SERVER, new Server(12345, lifeTime, BindingMode.UQ, false));
        }
        initializer.setInstancesForObject(LwM2mId.DEVICE, new Device("Eclipse Leshan", MODEL_NUMBER, "12345", "UQ") {
            @Override
            public ExecuteResponse execute(int resourceid, String params) {
                if (resourceid == 4) {
                    return ExecuteResponse.success();
                } else {
                    return super.execute(resourceid, params);
                }
            }
        });
        ArrayList<LwM2mObjectEnabler> enablers = new ArrayList<>();
        enablers.add(initializer.create(0));
        enablers.add(initializer.create(1));
        enablers.add(initializer.create(2));
        enablers.add(initializer.create(3));

        return new QueuedModeLeshanClient(getCurrentEndpoint(), new InetSocketAddress(0), // localAddress
                new InetSocketAddress(0), // localSecureAddress
                enablers);
    }

    @Override
    public Registration getCurrentRegistration() {
        return server.getRegistrationService().getByEndpoint(getCurrentEndpoint());
    }
}
