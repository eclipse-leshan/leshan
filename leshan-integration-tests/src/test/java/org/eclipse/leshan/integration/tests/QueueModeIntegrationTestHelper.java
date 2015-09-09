/*******************************************************************************
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
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
 *     Alexander Ellwein (Bosch Software Innovations GmbH)
 *                     - initial API and implementation
 *******************************************************************************/
package org.eclipse.leshan.integration.tests;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.concurrent.TimeUnit;

import org.eclipse.californium.core.CoapServer;
import org.eclipse.californium.core.network.CoAPEndpoint;
import org.eclipse.californium.core.network.Endpoint;
import org.eclipse.leshan.client.resource.LwM2mObjectEnabler;
import org.eclipse.leshan.client.resource.ObjectsInitializer;
import org.eclipse.leshan.integration.tests.util.QueuedModeLeshanClient;
import org.eclipse.leshan.server.californium.LeshanServerBuilder;
import org.eclipse.leshan.server.californium.impl.CaliforniumLwM2mRequestSender;
import org.eclipse.leshan.server.californium.impl.LeshanServer;
import org.eclipse.leshan.server.californium.impl.RegisterResource;
import org.eclipse.leshan.server.californium.impl.SecureEndpoint;
import org.eclipse.leshan.server.client.ClientRegistry;
import org.eclipse.leshan.server.impl.ClientRegistryImpl;
import org.eclipse.leshan.server.impl.ObservationRegistryImpl;
import org.eclipse.leshan.server.impl.SecurityRegistryImpl;
import org.eclipse.leshan.server.model.LwM2mModelProvider;
import org.eclipse.leshan.server.model.StandardModelProvider;
import org.eclipse.leshan.server.observation.ObservationRegistry;
import org.eclipse.leshan.server.queue.QueueReactor;
import org.eclipse.leshan.server.queue.QueueRequestFactory;
import org.eclipse.leshan.server.queue.QueueRequestSender;
import org.eclipse.leshan.server.queue.RequestQueue;
import org.eclipse.leshan.server.queue.impl.QueueRequestFactoryImpl;
import org.eclipse.leshan.server.queue.impl.QueueRequestSenderImpl;
import org.eclipse.leshan.server.queue.impl.RequestQueueImpl;
import org.eclipse.leshan.server.queue.reactor.QueueReactorImpl;
import org.eclipse.leshan.server.registration.RegistrationHandler;
import org.eclipse.leshan.server.request.LwM2mRequestSender;
import org.eclipse.leshan.server.security.SecurityRegistry;
import org.eclipse.leshan.server.security.SecurityStore;

/**
 * IntegrationTestHelper, which is intended to create a client/server environment for testing the Queue Mode feature.
 */
public class QueueModeIntegrationTestHelper extends IntegrationTestHelper {

    private RequestQueue requestQueue;
    private QueueReactor queueReactor;
    private QueueRequestSenderImpl requestSender;

    private CoapServer createCoapServer(final InetSocketAddress localAddress, final ClientRegistry clientRegistry,
            final SecurityStore securityStore) {

        CoapServer coapServer = new CoapServer();
        final Endpoint endpoint = new CoAPEndpoint(localAddress);
        coapServer.addEndpoint(endpoint);

        final RegisterResource rdResource = new RegisterResource(new RegistrationHandler(clientRegistry, securityStore));
        coapServer.add(rdResource);

        return coapServer;
    }

    @Override
    public void createServer() {
        LeshanServerBuilder serverBuilder = new LeshanServerBuilder();

        // create and wire the dependencies

        InetSocketAddress localAddress = new InetSocketAddress(InetAddress.getLoopbackAddress(), 0);
        QueueRequestFactory requestFactory = new QueueRequestFactoryImpl();
        queueReactor = new QueueReactorImpl(0);
        requestQueue = new RequestQueueImpl(requestFactory, queueReactor);
        ClientRegistry clientRegistry = new ClientRegistryImpl();
        SecurityRegistry securityRegistry = new SecurityRegistryImpl();
        CoapServer coapServer = createCoapServer(localAddress, clientRegistry, securityRegistry);
        ObservationRegistry observationRegistry = new ObservationRegistryImpl();
        LwM2mModelProvider modelProvider = new StandardModelProvider();

        LwM2mRequestSender delegateSender = new CaliforniumLwM2mRequestSender(new HashSet<>(coapServer.getEndpoints()),
                observationRegistry, modelProvider);

        requestSender = new QueueRequestSenderImpl(queueReactor, requestQueue, delegateSender, requestFactory,
                clientRegistry, observationRegistry, 10, TimeUnit.MINUTES, 700L);

        requestSender.setSendExpirationInterval(24, TimeUnit.HOURS); // send within 24 hours
        requestSender.setKeepExpirationInterval(5, TimeUnit.DAYS); // keep at most 5 days

        serverBuilder.setClientRegistry(clientRegistry);
        serverBuilder.setCoapServer(coapServer);
        serverBuilder.setObjectModelProvider(modelProvider);
        serverBuilder.setObservationRegistry(observationRegistry);
        serverBuilder.setSecurityRegistry(securityRegistry);
        serverBuilder.setLocalAddress(localAddress);
        serverBuilder.setRequestSender(requestSender);

        server = serverBuilder.build();
    }

    @Override
    public void createClient() {
        ObjectsInitializer initializer = new ObjectsInitializer();
        ArrayList<LwM2mObjectEnabler> enablers = new ArrayList<>();
        enablers.add(initializer.create(3));

        client = new QueuedModeLeshanClient(new InetSocketAddress("0", 0), getServerAddress(), enablers);
    }

    protected InetSocketAddress getServerAddress() {
        for (Endpoint endpoint : ((LeshanServer) server).getCoapServer().getEndpoints()) {
            if (!(endpoint instanceof SecureEndpoint))
                return endpoint.getAddress();
        }
        return null;
    }

    protected QueueRequestSender getQueueRequestSender() {
        return requestSender;
    }

    protected RequestQueue getRequestQueue() {
        return requestQueue;
    }

    protected QueueReactor getQueueReactor() {
        return queueReactor;
    }
}
