/*******************************************************************************
 * Copyright (c) 2023 Sierra Wireless and others.
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
 *     Sierra Wireless - initial API and implementation
 *******************************************************************************/
package org.eclipse.leshan.integration.tests.util;

import java.net.URI;
import java.security.cert.Certificate;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ScheduledExecutorService;

import org.eclipse.leshan.client.LeshanClientBuilder;
import org.eclipse.leshan.client.bootstrap.BootstrapConsistencyChecker;
import org.eclipse.leshan.client.californium.endpoint.CaliforniumClientEndpointsProvider;
import org.eclipse.leshan.client.endpoint.LwM2mClientEndpointsProvider;
import org.eclipse.leshan.client.engine.RegistrationEngineFactory;
import org.eclipse.leshan.client.object.LwM2mTestObject;
import org.eclipse.leshan.client.object.Security;
import org.eclipse.leshan.client.object.Server;
import org.eclipse.leshan.client.resource.DummyInstanceEnabler;
import org.eclipse.leshan.client.resource.LwM2mObjectEnabler;
import org.eclipse.leshan.client.resource.ObjectsInitializer;
import org.eclipse.leshan.client.send.DataSender;
import org.eclipse.leshan.client.send.ManualDataSender;
import org.eclipse.leshan.core.LwM2mId;
import org.eclipse.leshan.core.endpoint.Protocol;
import org.eclipse.leshan.core.link.LinkSerializer;
import org.eclipse.leshan.core.link.lwm2m.attributes.LwM2mAttributeParser;
import org.eclipse.leshan.core.model.StaticModel;
import org.eclipse.leshan.core.node.codec.DefaultLwM2mDecoder;
import org.eclipse.leshan.core.node.codec.DefaultLwM2mEncoder;
import org.eclipse.leshan.core.node.codec.LwM2mDecoder;
import org.eclipse.leshan.core.node.codec.LwM2mEncoder;
import org.eclipse.leshan.core.util.TestLwM2mId;
import org.eclipse.leshan.integration.tests.util.IntegrationTestHelper.TestDevice;
import org.eclipse.leshan.server.LeshanServer;
import org.eclipse.leshan.server.endpoint.LwM2mServerEndpoint;

public class LeshanTestClientBuilder extends LeshanClientBuilder {

    public static final String MODEL_NUMBER = "IT-TEST-123";
    public static final long LIFETIME = 2;

    private static final Random r = new Random();

    private final Protocol protocolToUse;
    ObjectsInitializer initializer;

    // server.getEndpoint(Protocol.COAP).getURI().toString()
    public LeshanTestClientBuilder(Protocol protocolToUse) {
        super("leshan_test_client_" + r.nextInt());
        this.protocolToUse = protocolToUse;

        // Create objects Enabler
        initializer = new ObjectsInitializer(new StaticModel(TestObjectLoader.loadDefaultObject()));
        initializer.setInstancesForObject(LwM2mId.DEVICE, new TestDevice("Eclipse Leshan", MODEL_NUMBER, "12345"));
        initializer.setClassForObject(LwM2mId.ACCESS_CONTROL, DummyInstanceEnabler.class);
        initializer.setInstancesForObject(TestLwM2mId.TEST_OBJECT, new LwM2mTestObject());

        // Build Client
        this.setDecoder(new DefaultLwM2mDecoder(true));
        this.setEncoder(new DefaultLwM2mEncoder(true));
        this.setDataSenders(new ManualDataSender());

    }

    @Override
    public LeshanTestClient build() {
        List<LwM2mObjectEnabler> objects = initializer.createAll();
        this.setObjects(objects);
        return (LeshanTestClient) super.build();
    }

    @Override
    protected LeshanTestClient createLeshanClient(String endpoint, List<? extends LwM2mObjectEnabler> objectEnablers,
            List<DataSender> dataSenders, List<Certificate> trustStore, RegistrationEngineFactory engineFactory,
            BootstrapConsistencyChecker checker, Map<String, String> additionalAttributes,
            Map<String, String> bsAdditionalAttributes, LwM2mEncoder encoder, LwM2mDecoder decoder,
            ScheduledExecutorService sharedExecutor, LinkSerializer linkSerializer,
            LwM2mAttributeParser attributeParser, LwM2mClientEndpointsProvider endpointsProvider) {
        return new LeshanTestClient(endpoint, objectEnablers, dataSenders, trustStore, engineFactory, checker,
                additionalAttributes, bsAdditionalAttributes, encoder, decoder, sharedExecutor, linkSerializer,
                attributeParser, endpointsProvider);
    }

    public LeshanTestClientBuilder with(String endpointProvider) {
        if (endpointProvider.equals("Californium")) {
            this.setEndpointsProvider(new CaliforniumClientEndpointsProvider());
        }
        return this;
    }

    public LeshanTestClientBuilder connectingTo(LeshanServer server) {
        LwM2mServerEndpoint endpoint = server.getEndpoint(protocolToUse);
        URI uri = endpoint.getURI();
        initializer.setInstancesForObject(LwM2mId.SECURITY, Security.noSec(uri.toString(), 12345));
        initializer.setInstancesForObject(LwM2mId.SERVER, new Server(12345, LIFETIME));
        return this;
    }

    public static LeshanTestClientBuilder givenClientUsing(Protocol protocol) {
        return new LeshanTestClientBuilder(protocol);
    }
}
