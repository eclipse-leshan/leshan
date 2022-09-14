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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.time.Instant;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.eclipse.leshan.client.resource.ObjectsInitializer;
import org.eclipse.leshan.client.send.ManualDataSender;
import org.eclipse.leshan.client.servers.ServerIdentity;
import org.eclipse.leshan.core.model.ResourceModel.Type;
import org.eclipse.leshan.core.model.StaticModel;
import org.eclipse.leshan.core.node.LwM2mNode;
import org.eclipse.leshan.core.node.LwM2mPath;
import org.eclipse.leshan.core.node.LwM2mSingleResource;
import org.eclipse.leshan.core.node.TimestampedLwM2mNodes;
import org.eclipse.leshan.core.request.ContentFormat;
import org.eclipse.leshan.core.util.TestLwM2mId;
import org.eclipse.leshan.integration.tests.util.IntegrationTestHelper;
import org.eclipse.leshan.integration.tests.util.SynchronousSendListener;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class SendTimestampedTest {

    protected final IntegrationTestHelper helper = new IntegrationTestHelper() {
        @Override
        protected ObjectsInitializer createObjectsInitializer() {
            return new ObjectsInitializer(new StaticModel(createObjectModels()));
        };
    };

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
        ManualDataSender sender = helper.client.getSendService().getDataSender(ManualDataSender.DEFAULT_NAME,
                ManualDataSender.class);
        sender.collectData(Arrays.asList(getExamplePath()));
        Thread.sleep(1000);
        sender.collectData(Arrays.asList(getExamplePath()));
        sender.sendCollectedData(server, ContentFormat.SENML_JSON, 1000, false);
        listener.waitForData(1, TimeUnit.SECONDS);

        // Verify SendListener data received
        assertNotNull(listener.getRegistration());
        TimestampedLwM2mNodes data = listener.getData();
        assertEquals(2, data.getTimestamps().size());

        for (Instant ts : data.getTimestamps()) {
            assertNotNull(ts);
            Map<LwM2mPath, LwM2mNode> pathNodeMap = data.getNodesAt(ts);
            assertTrue(pathNodeMap.containsKey(getExamplePath()));

            LwM2mNode node = pathNodeMap.get(getExamplePath());
            assertEquals(TestLwM2mId.FLOAT_VALUE, node.getId());
            assertTrue(node instanceof LwM2mSingleResource);
            assertTrue(((LwM2mSingleResource) node).getType() == Type.FLOAT);
        }
    }

    private static LwM2mPath getExamplePath() {
        return new LwM2mPath(TestLwM2mId.TEST_OBJECT, 0, TestLwM2mId.FLOAT_VALUE);
    }
}
