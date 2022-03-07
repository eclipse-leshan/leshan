/*******************************************************************************
 * Copyright (c) 2021 Sierra Wireless and others.
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
package org.eclipse.leshan.integration.tests.send;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.Collection;
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
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class SendTimestampedTest {
    protected IntegrationTestHelper helper = new IntegrationTestHelper() {
        @Override
        protected ObjectsInitializer createObjectsInitializer() {
            return new ObjectsInitializer(new StaticModel(createObjectModels()));
        };
    };

    @Parameters(name = "{0}{1}")
    public static Collection<?> contentFormats() {
        return Arrays.asList(new Object[][] { //
                // {content format}
                { ContentFormat.SENML_JSON }, //
                //                                { ContentFormat.SENML_CBOR }
        });
    }

    private ContentFormat contentformat;

    public SendTimestampedTest(ContentFormat contentformat) {
        this.contentformat = contentformat;
    }

    @Before
    public void start() {

    }

    @After
    public void stop() {
        helper.client.destroy(false);
        helper.server.destroy();
        helper.dispose();
    }

    @Test
    public void server_handle_multiple_timestamped_node() throws InterruptedException, TimeoutException {
        helper.initialize();

        LeshanServerBuilder serverBuilder = helper.createServerBuilder();
        serverBuilder.setDecoder(new LwM2mDecoder() {
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
        });
        helper.server = serverBuilder.build();
        helper.setupServerMonitoring();

        helper.server.start();
        helper.createClient();
        helper.client.start();
        helper.waitForRegistrationAtServerSide(1);

        // Define send listener
        SynchronousSendListener listener = new SynchronousSendListener();
        helper.server.getSendService().addListener(listener);

        // Send Data
        helper.waitForRegistrationAtClientSide(1);
        ServerIdentity server = helper.client.getRegisteredServers().values().iterator().next();
        helper.client.sendData(server, contentformat, Arrays.asList(getExamplePath().toString()), 1000);

        listener.waitForData(1, TimeUnit.SECONDS);
        assertNotNull(listener.getRegistration());
        TimestampedLwM2mNodes data = listener.getData();
        Set<Long> timestamps = data.getTimestamps();

        Map<Long, LwM2mNode> exampleNodes = getExampleTimestampedNodes();
        assertEquals(exampleNodes.keySet(), timestamps);

        for (Long ts : exampleNodes.keySet()) {
            Map<LwM2mPath, LwM2mNode> pathNodeMap = data.getTimestampedPathNodesMap().get(ts);
            assertTrue(pathNodeMap.containsKey(getExamplePath()));

            LwM2mNode node = pathNodeMap.get(getExamplePath());
            LwM2mNode expectedNode = exampleNodes.get(ts);

            assertEquals(node, expectedNode);
        }
    }

    private LwM2mPath getExamplePath() {
        return new LwM2mPath("/2000/2/3");
    }

    private Map<Long, LwM2mNode> getExampleTimestampedNodes() {
        Map<Long, LwM2mNode> timestampedNodes = new HashMap<>();
        timestampedNodes.put(2222L, LwM2mSingleResource.newIntegerResource(3, 12345));
        timestampedNodes.put(4444L, LwM2mSingleResource.newIntegerResource(3, 67890));
        return timestampedNodes;
    }

}
