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

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.eclipse.leshan.client.resource.ObjectsInitializer;
import org.eclipse.leshan.client.servers.ServerIdentity;
import org.eclipse.leshan.core.model.LwM2mModel;
import org.eclipse.leshan.core.model.StaticModel;
import org.eclipse.leshan.core.node.LwM2mNode;
import org.eclipse.leshan.core.node.LwM2mPath;
import org.eclipse.leshan.core.node.LwM2mSingleResource;
import org.eclipse.leshan.core.node.TimestampedLwM2mNode;
import org.eclipse.leshan.core.node.TimestampedLwM2mNodes;
import org.eclipse.leshan.core.node.TimestampedLwM2mNodesImpl;
import org.eclipse.leshan.core.node.codec.CodecException;
import org.eclipse.leshan.core.node.codec.DefaultLwM2mDecoder;
import org.eclipse.leshan.core.node.codec.LwM2mDecoder;
import org.eclipse.leshan.core.request.ContentFormat;
import org.eclipse.leshan.integration.tests.util.IntegrationTestHelper;
import org.eclipse.leshan.integration.tests.util.SynchronousSendListener;
import org.eclipse.leshan.server.californium.LeshanServerBuilder;
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
            Map<LwM2mPath, LwM2mNode> pathNodeMap = data.getNodesForTimestamp(ts);
            assertTrue(pathNodeMap.containsKey(getExamplePath()));

            LwM2mNode node = pathNodeMap.get(getExamplePath());
            LwM2mNode expectedNode = exampleNodes.get(ts);

            assertEquals(node, expectedNode);
        }
    }

    private static LwM2mPath getExamplePath() {
        return new LwM2mPath("/2000/2/3");
    }

    private static Map<Long, LwM2mNode> getExampleTimestampedNodes() {
        Map<Long, LwM2mNode> timestampedNodes = new HashMap<>();
        timestampedNodes.put(2222L, LwM2mSingleResource.newIntegerResource(3, 12345));
        timestampedNodes.put(4444L, LwM2mSingleResource.newIntegerResource(3, 67890));
        return timestampedNodes;
    }

    private static class TestHelperWithFakeDecoder extends IntegrationTestHelper {
        @Override
        protected ObjectsInitializer createObjectsInitializer() {
            return new ObjectsInitializer(new StaticModel(createObjectModels()));
        }

        @Override
        public void createServer() {
            LeshanServerBuilder serverBuilder = createServerBuilder();
            serverBuilder.setDecoder(new FakeDecoder());
            server = serverBuilder.build();
            setupServerMonitoring();
        }
    };

    private static class FakeDecoder implements LwM2mDecoder {
        private final DefaultLwM2mDecoder decoder = new DefaultLwM2mDecoder(true);

        @Override
        public LwM2mNode decode(byte[] content, ContentFormat format, LwM2mPath path, LwM2mModel model)
                    throws CodecException {
            return decoder.decode(content, format, path, model);
        }

        @Override
        public <T extends LwM2mNode> T decode(byte[] content, ContentFormat format, LwM2mPath path,
                LwM2mModel model, Class<T> nodeClass) throws CodecException {
            return decoder.decode(content, format, path, model, nodeClass);
        }

        @Override
        public Map<LwM2mPath, LwM2mNode> decodeNodes(byte[] content, ContentFormat format, List<LwM2mPath> paths,
                LwM2mModel model) throws CodecException {
            return decoder.decodeNodes(content, format, paths, model);
        }

        @Override
        public TimestampedLwM2mNodes decodeMultiTimestampedNodes(byte[] content, ContentFormat format,
                LwM2mModel model) throws CodecException {
            Map<Long, LwM2mNode> timestampedNodes = getExampleTimestampedNodes();

            TimestampedLwM2mNodesImpl data = new TimestampedLwM2mNodesImpl();
            for (Map.Entry<Long, LwM2mNode> entry : timestampedNodes.entrySet()) {
                data.put(entry.getKey(), getExamplePath(), entry.getValue());
            }

            return data;
        }

        @Override
        public List<TimestampedLwM2mNode> decodeTimestampedData(byte[] content, ContentFormat format,
                LwM2mPath path, LwM2mModel model) throws CodecException {
            return decoder.decodeTimestampedData(content, format, path, model);
        }

        @Override
        public List<LwM2mPath> decodePaths(byte[] content, ContentFormat format) throws CodecException {
            return decoder.decodePaths(content, format);
        }

        @Override
        public boolean isSupported(ContentFormat format) {
            return decoder.isSupported(format);
        }

        @Override
        public Set<ContentFormat> getSupportedContentFormat() {
            return decoder.getSupportedContentFormat();
        }
    }
}
