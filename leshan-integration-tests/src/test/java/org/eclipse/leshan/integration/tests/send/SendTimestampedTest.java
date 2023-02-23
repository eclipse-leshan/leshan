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

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.leshan.integration.tests.util.LeshanTestClientBuilder.givenClientUsing;
import static org.eclipse.leshan.integration.tests.util.LeshanTestServerBuilder.givenServerUsing;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.time.Instant;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Stream;

import org.eclipse.leshan.client.send.ManualDataSender;
import org.eclipse.leshan.client.servers.ServerIdentity;
import org.eclipse.leshan.core.endpoint.Protocol;
import org.eclipse.leshan.core.model.ResourceModel.Type;
import org.eclipse.leshan.core.node.LwM2mNode;
import org.eclipse.leshan.core.node.LwM2mPath;
import org.eclipse.leshan.core.node.LwM2mSingleResource;
import org.eclipse.leshan.core.node.TimestampedLwM2mNodes;
import org.eclipse.leshan.core.request.ContentFormat;
import org.eclipse.leshan.core.util.TestLwM2mId;
import org.eclipse.leshan.integration.tests.util.LeshanTestClient;
import org.eclipse.leshan.integration.tests.util.LeshanTestServer;
import org.eclipse.leshan.integration.tests.util.junit5.extensions.ArgumentsUtil;
import org.eclipse.leshan.integration.tests.util.junit5.extensions.BeforeEachParameterizedResolver;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@ExtendWith(BeforeEachParameterizedResolver.class)
public class SendTimestampedTest {

    /*---------------------------------/
     *  Parameterized Tests
     * -------------------------------*/
    @ParameterizedTest(name = "{0} over {1} - Client using {2} - Server using {3}")
    @MethodSource("transports")
    @Retention(RetentionPolicy.RUNTIME)
    private @interface TestAllCases {
    }

    static Stream<Arguments> transports() {

        Object[][] transports = new Object[][] {
                // ProtocolUsed - Client Endpoint Provider - Server Endpoint Provider
                { Protocol.COAP, "Californium", "Californium" } };

        Object[] contentFormats = new Object[] { //
                ContentFormat.SENML_JSON, //
                ContentFormat.SENML_CBOR };

        // for each transport, create 1 test by format.
        return Stream.of(ArgumentsUtil.combine(contentFormats, transports));
    }

    /*---------------------------------/
     *  Set-up and Tear-down Tests
     * -------------------------------*/

    LeshanTestServer server;
    LeshanTestClient client;

    @BeforeEach
    public void start(ContentFormat format, Protocol givenProtocol, String givenClientEndpointProvider,
            String givenServerEndpointProvider) {
        server = givenServerUsing(givenProtocol).with(givenServerEndpointProvider).build();
        server.start();
        client = givenClientUsing(givenProtocol).with(givenClientEndpointProvider).connectingTo(server).build();
        client.start();
        server.waitForNewRegistrationOf(client);
        client.waitForRegistrationTo(server);
    }

    @AfterEach
    public void stop() throws InterruptedException {
        if (client != null)
            client.destroy(false);
        if (server != null)
            server.destroy();
    }

    /*---------------------------------/
     *  Tests
     * -------------------------------*/
    @TestAllCases
    public void server_handle_multiple_timestamped_node(ContentFormat contentFormat, Protocol givenProtocol,
            String givenClientEndpointProvider, String givenServerEndpointProvider)
            throws InterruptedException, TimeoutException {
        // Send Data
        ServerIdentity serverIdentity = client.getRegisteredServers().values().iterator().next();
        ManualDataSender sender = client.getSendService().getDataSender(ManualDataSender.DEFAULT_NAME,
                ManualDataSender.class);
        sender.collectData(Arrays.asList(getExamplePath()));
        Thread.sleep(1000);
        sender.collectData(Arrays.asList(getExamplePath()));
        sender.sendCollectedData(serverIdentity, contentFormat, 1000, false);
        TimestampedLwM2mNodes data = server.waitForData(client.getEndpointName(), 1, TimeUnit.SECONDS);

        // Verify SendListener data received
        assertThat(data.getTimestamps()).hasSize(2);

        for (Instant ts : data.getTimestamps()) {
            assertNotNull(ts);
            Map<LwM2mPath, LwM2mNode> pathNodeMap = data.getNodesAt(ts);
            assertThat(pathNodeMap).containsKey(getExamplePath());

            LwM2mNode node = pathNodeMap.get(getExamplePath());
            assertThat(node.getId()).isEqualTo(TestLwM2mId.FLOAT_VALUE);
            assertThat(node).isInstanceOfSatisfying(LwM2mSingleResource.class,
                    n -> assertThat(n.getType()).isEqualTo(Type.FLOAT));
        }
    }

    private static LwM2mPath getExamplePath() {
        return new LwM2mPath(TestLwM2mId.TEST_OBJECT, 0, TestLwM2mId.FLOAT_VALUE);
    }
}
