/*******************************************************************************
 * Copyright (c) 2021 Orange.
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
 *     Orange - Send with multiple-timestamped values
 *******************************************************************************/
package org.eclipse.leshan.integration.tests.send;

import static org.junit.Assert.*;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.eclipse.leshan.client.californium.LeshanClientBuilder;
import org.eclipse.leshan.client.object.Security;
import org.eclipse.leshan.client.object.Server;
import org.eclipse.leshan.client.resource.DummyInstanceEnabler;
import org.eclipse.leshan.client.resource.LwM2mObjectEnabler;
import org.eclipse.leshan.client.resource.ObjectsInitializer;
import org.eclipse.leshan.client.resource.SimpleInstanceEnabler;
import org.eclipse.leshan.client.servers.ServerIdentity;
import org.eclipse.leshan.core.LwM2mId;
import org.eclipse.leshan.core.model.LwM2mModel;
import org.eclipse.leshan.core.model.StaticModel;
import org.eclipse.leshan.core.node.LwM2mNode;
import org.eclipse.leshan.core.node.LwM2mPath;
import org.eclipse.leshan.core.node.LwM2mSingleResource;
import org.eclipse.leshan.core.node.TimestampedLwM2mNodes;
import org.eclipse.leshan.core.node.codec.DefaultLwM2mDecoder;
import org.eclipse.leshan.core.node.codec.DefaultLwM2mEncoder;
import org.eclipse.leshan.core.request.ContentFormat;
import org.eclipse.leshan.integration.tests.util.IntegrationTestHelper;
import org.eclipse.leshan.integration.tests.util.SynchronousSendListener;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class SendTimestampedTest {

    protected final IntegrationTestHelper helper = new TestHelperWithFakeDecoder();

    @Before
    public void start() {
        helper.initialize();
        helper.createServer();
        helper.server.start();
        helper.createClient();
        helper.client.start();
        helper.waitForRegistrationAtServerSide(1);
    }

    @After
    public void stop() {
        helper.client.destroy(false);
        helper.server.destroy();
        helper.dispose();
    }

    @Test
    public void server_handle_multiple_timestamped_node() throws InterruptedException, TimeoutException {
        // Define send listener
        SynchronousSendListener listener = new SynchronousSendListener();
        helper.server.getSendService().addListener(listener);

        // Send Data
        helper.waitForRegistrationAtClientSide(1);
        ServerIdentity server = helper.client.getRegisteredServers().values().iterator().next();
        helper.client.sendData(server, ContentFormat.SENML_JSON, Arrays.asList(getExamplePath().toString()), 1000);
        listener.waitForData(1, TimeUnit.SECONDS);

        // Verify SendListener data received
        assertNotNull(listener.getRegistration());
        TimestampedLwM2mNodes data = listener.getData();

        Map<Long, LwM2mNode> exampleNodes = getExampleTimestampedNodes();
        assertEquals(exampleNodes.keySet(), data.getTimestamps());

        for (Long ts : exampleNodes.keySet()) {
            Map<LwM2mPath, LwM2mNode> pathNodeMap = data.getNodesAt(ts);
            assertTrue(pathNodeMap.containsKey(getExamplePath()));

            LwM2mNode node = pathNodeMap.get(getExamplePath());
            LwM2mNode expectedNode = exampleNodes.get(ts);

            assertEquals(node, expectedNode);
        }
    }

    private static LwM2mPath getExamplePath() {
        return new LwM2mPath("/2000/1/3");
    }

    private static Map<Long, LwM2mNode> getExampleTimestampedNodes() {
        Map<Long, LwM2mNode> timestampedNodes = new HashMap<>();
        timestampedNodes.put(268435456L, LwM2mSingleResource.newFloatResource(3, 12345));
        timestampedNodes.put(268435457L, LwM2mSingleResource.newFloatResource(3, 67890));
        return timestampedNodes;
    }

    private static class TestHelperWithFakeDecoder extends IntegrationTestHelper {
        @Override
        protected ObjectsInitializer createObjectsInitializer() {
            return new ObjectsInitializer(new StaticModel(createObjectModels()));
        }

        @Override
        public void createClient(Map<String, String> additionalAttributes) {
            // Create objects Enabler
            ObjectsInitializer initializer = createObjectsInitializer();
            initializer.setInstancesForObject(LwM2mId.SECURITY,
                    Security.noSec("coap://" + server.getUnsecuredAddress().getHostString() + ":"
                            + server.getUnsecuredAddress().getPort(), 12345));
            initializer.setInstancesForObject(LwM2mId.SERVER, new Server(12345, LIFETIME));
            initializer.setInstancesForObject(LwM2mId.DEVICE, new TestDevice("Eclipse Leshan", MODEL_NUMBER, "12345"));
            initializer.setClassForObject(LwM2mId.ACCESS_CONTROL, DummyInstanceEnabler.class);
            initializer.setInstancesForObject(TEST_OBJECT_ID, new DummyInstanceEnabler(0),
                    new SimpleInstanceEnabler(1, FLOAT_RESOURCE_ID, 12345d));
            List<LwM2mObjectEnabler> objects = initializer.createAll();

            // Build Client
            LeshanClientBuilder builder = new LeshanClientBuilder(currentEndpointIdentifier.get());
            builder.setDecoder(new DefaultLwM2mDecoder(true));
            builder.setEncoder(new FakeEncoder());
            builder.setAdditionalAttributes(additionalAttributes);
            builder.setObjects(objects);
            client = builder.build();
            setupClientMonitoring();
        }
    }

    private static class FakeEncoder extends DefaultLwM2mEncoder {
        public FakeEncoder() {
            super(true);
        }

        @Override
        public byte[] encodeTimestampedNodes(TimestampedLwM2mNodes timestampedNodes, ContentFormat format,
                LwM2mModel model) {
            return ("[{\"bn\":\"/2000/1/\",\"n\":\"3\",\"v\":12345,\"t\":268435456},"
                    + "{\"n\":\"3\",\"v\":67890,\"t\":268435457}]").getBytes(StandardCharsets.UTF_8);
        }
    }
}
