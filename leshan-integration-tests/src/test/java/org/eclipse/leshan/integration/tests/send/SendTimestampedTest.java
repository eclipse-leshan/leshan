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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.time.Instant;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Stream;

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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

public class SendTimestampedTest {

    @ParameterizedTest(name = "{0}")
    @MethodSource("contentFormats")
    @Retention(RetentionPolicy.RUNTIME)
    private @interface TestAllContentFormat {
    }

    static Stream<ContentFormat> contentFormats() {
        return Stream.of(//
                ContentFormat.SENML_JSON, //
                ContentFormat.SENML_CBOR);
    }

    protected final IntegrationTestHelper helper = new IntegrationTestHelper() {
        @Override
        protected ObjectsInitializer createObjectsInitializer() {
            return new ObjectsInitializer(new StaticModel(createObjectModels()));
        };
    };

    @BeforeEach
    public void start() {
        helper.initialize();
        helper.createServer();
        helper.server.start();
        helper.createClient();
        helper.client.start();
        helper.waitForRegistrationAtServerSide(1);
    }

    @AfterEach
    public void stop() {
        helper.client.destroy(false);
        helper.server.destroy();
        helper.dispose();
    }

    @TestAllContentFormat
    public void server_handle_multiple_timestamped_node(ContentFormat contentFormat)
            throws InterruptedException, TimeoutException {
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
        sender.sendCollectedData(server, contentFormat, 1000, false);
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
