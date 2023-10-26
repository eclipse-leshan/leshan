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
package org.eclipse.leshan.integration.tests.write;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.eclipse.leshan.core.ResponseCode.CHANGED;
import static org.eclipse.leshan.core.ResponseCode.CONTENT;
import static org.eclipse.leshan.integration.tests.util.LeshanTestClientBuilder.givenClientUsing;
import static org.eclipse.leshan.integration.tests.util.LeshanTestServerBuilder.givenServerUsing;
import static org.eclipse.leshan.integration.tests.util.assertion.Assertions.assertThat;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import org.eclipse.leshan.core.endpoint.Protocol;
import org.eclipse.leshan.core.node.LwM2mMultipleResource;
import org.eclipse.leshan.core.node.LwM2mNode;
import org.eclipse.leshan.core.node.LwM2mObjectInstance;
import org.eclipse.leshan.core.node.LwM2mPath;
import org.eclipse.leshan.core.node.LwM2mResourceInstance;
import org.eclipse.leshan.core.node.LwM2mSingleResource;
import org.eclipse.leshan.core.observation.SingleObservation;
import org.eclipse.leshan.core.request.ContentFormat;
import org.eclipse.leshan.core.request.ObserveRequest;
import org.eclipse.leshan.core.request.ReadRequest;
import org.eclipse.leshan.core.request.WriteCompositeRequest;
import org.eclipse.leshan.core.response.LwM2mResponse;
import org.eclipse.leshan.core.response.ObserveResponse;
import org.eclipse.leshan.core.response.ReadResponse;
import org.eclipse.leshan.core.response.WriteCompositeResponse;
import org.eclipse.leshan.core.util.TestLwM2mId;
import org.eclipse.leshan.integration.tests.util.LeshanTestClient;
import org.eclipse.leshan.integration.tests.util.LeshanTestServer;
import org.eclipse.leshan.integration.tests.util.junit5.extensions.ArgumentsUtil;
import org.eclipse.leshan.integration.tests.util.junit5.extensions.BeforeEachParameterizedResolver;
import org.eclipse.leshan.server.registration.Registration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@ExtendWith(BeforeEachParameterizedResolver.class)
public class WriteCompositeTest {

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
                { Protocol.COAP, "Californium", "Californium" }, //
                { Protocol.COAP, "Californium", "java-coap" }, //
                { Protocol.COAP, "java-coap", "Californium" }, //
                { Protocol.COAP, "java-coap", "java-coap" }, //
                { Protocol.COAP_TCP, "java-coap", "java-coap" } };

        Object[] contentFormats = new Object[] { //
                ContentFormat.SENML_JSON, //
                ContentFormat.SENML_CBOR //
        };

        // for each transport, create 1 test by format.
        return Stream.of(ArgumentsUtil.combine(contentFormats, transports));
    }

    /*---------------------------------/
     *  Set-up and Tear-down Tests
     * -------------------------------*/

    LeshanTestServer server;
    LeshanTestClient client;
    Registration currentRegistration;

    @BeforeEach
    public void start(ContentFormat contentFormat, Protocol givenProtocol, String givenClientEndpointProvider,
            String givenServerEndpointProvider) {
        server = givenServerUsing(givenProtocol).with(givenServerEndpointProvider).build();
        server.start();
        client = givenClientUsing(givenProtocol).with(givenClientEndpointProvider).connectingTo(server).build();
        client.start();
        server.waitForNewRegistrationOf(client);
        client.waitForRegistrationTo(server);

        currentRegistration = server.getRegistrationFor(client);

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
    public void can_write_resources(ContentFormat contentFormat, Protocol givenProtocol,
            String givenClientEndpointProvider, String givenServerEndpointProvider) throws InterruptedException {
        // write device timezone and offset
        Map<String, Object> nodes = new HashMap<>();
        nodes.put("/3/0/14", "+02");
        nodes.put("/1/0/2", 100);

        WriteCompositeResponse response = server.send(currentRegistration,
                new WriteCompositeRequest(contentFormat, nodes));

        // verify result
        assertThat(response) //
                .hasCode(CHANGED) //
                .hasValidUnderlyingResponseFor(givenServerEndpointProvider);

        // read resource to check the value changed
        ReadResponse readResponse = server.send(currentRegistration, new ReadRequest(3, 0, 14));
        assertThat(((LwM2mSingleResource) readResponse.getContent()).getValue()).isEqualTo("+02");
        readResponse = server.send(currentRegistration, new ReadRequest(1, 0, 2));
        assertThat(((LwM2mSingleResource) readResponse.getContent()).getValue()).isEqualTo(100l);
    }

    @TestAllCases
    public void can_write_resource_and_instance(ContentFormat contentFormat, Protocol givenProtocol,
            String givenClientEndpointProvider, String givenServerEndpointProvider) throws InterruptedException {
        // create value
        LwM2mSingleResource utcOffset = LwM2mSingleResource.newStringResource(14, "+02");
        LwM2mPath resourceInstancePath = new LwM2mPath(TestLwM2mId.TEST_OBJECT, 0, TestLwM2mId.MULTIPLE_STRING_VALUE,
                0);
        LwM2mResourceInstance testStringResourceInstance = LwM2mResourceInstance
                .newStringInstance(resourceInstancePath.getResourceInstanceId(), "test_string_instance");

        // add it to the map
        Map<LwM2mPath, LwM2mNode> nodes = new HashMap<>();
        nodes.put(new LwM2mPath("/3/0/14"), utcOffset);
        nodes.put(resourceInstancePath, testStringResourceInstance);

        WriteCompositeResponse response = server.send(currentRegistration,
                new WriteCompositeRequest(contentFormat, nodes, null));

        // verify result
        assertThat(response) //
                .hasCode(CHANGED) //
                .hasValidUnderlyingResponseFor(givenServerEndpointProvider);

        // read resource to check the value changed
        ReadResponse readResponse = server.send(currentRegistration, new ReadRequest(3, 0, 14));
        assertThat(readResponse.getContent()).isEqualTo(utcOffset);

        readResponse = server.send(currentRegistration, new ReadRequest(contentFormat, resourceInstancePath, null));
        assertThat(readResponse.getContent()).isEqualTo(testStringResourceInstance);
    }

    @TestAllCases
    public void can_add_resource_instances(ContentFormat contentFormat, Protocol givenProtocol,
            String givenClientEndpointProvider, String givenServerEndpointProvider) throws InterruptedException {
        // Prepare node
        LwM2mPath resourceInstancePath = new LwM2mPath(TestLwM2mId.TEST_OBJECT, 0, TestLwM2mId.MULTIPLE_STRING_VALUE,
                1);
        LwM2mResourceInstance testStringResourceInstance = LwM2mResourceInstance
                .newStringInstance(resourceInstancePath.getResourceInstanceId(), "test_string_instance");
        Map<LwM2mPath, LwM2mNode> nodes = new HashMap<>();
        nodes.put(resourceInstancePath, testStringResourceInstance);

        // Write it
        WriteCompositeResponse response = server.send(currentRegistration,
                new WriteCompositeRequest(contentFormat, nodes, null));

        // verify result
        assertThat(response) //
                .hasCode(CHANGED) //
                .hasValidUnderlyingResponseFor(givenServerEndpointProvider);

        // read resource to check the value changed
        ReadResponse readResponse = server.send(currentRegistration,
                new ReadRequest(contentFormat, resourceInstancePath.toResourcePath(), null));
        LwM2mMultipleResource multiResource = (LwM2mMultipleResource) readResponse.getContent();

        assertThat(multiResource.getInstances()) //
                .hasSize(2) //
                .containsEntry(resourceInstancePath.getResourceInstanceId(), testStringResourceInstance);
    }

    @TestAllCases
    public void can_observe_instance_with_composite_write(ContentFormat contentFormat, Protocol givenProtocol,
            String givenClientEndpointProvider, String givenServerEndpointProvider) throws InterruptedException {
        // observe device instance
        ObserveResponse observeResponse = server.send(currentRegistration, new ObserveRequest(3, 0));
        assertThat(observeResponse) //
                .hasCode(CONTENT) //
                .hasValidUnderlyingResponseFor(givenServerEndpointProvider);

        // an observation response should have been sent
        SingleObservation observation = observeResponse.getObservation();
        assertThat(observation.getPath()).asString().isEqualTo("/3/0");
        assertThat(observation.getRegistrationId()).isEqualTo(currentRegistration.getId());

        // write device timezone
        Map<String, Object> nodes = new HashMap<>();
        nodes.put("/3/0/14", "+11");
        nodes.put("/3/0/15", "Moon");

        LwM2mResponse writeResponse = server.send(currentRegistration,
                new WriteCompositeRequest(ContentFormat.SENML_CBOR, nodes));
        assertThat(writeResponse) //
                .hasCode(CHANGED) //
                .hasValidUnderlyingResponseFor(givenServerEndpointProvider);

        // verify result both resource must have new value
        server.waitForNewObservation(observation);
        ObserveResponse response = server.waitForNotificationOf(observation);
        assertThat(response).hasValidUnderlyingResponseFor(givenServerEndpointProvider);

        assertThat(response.getContent()).isInstanceOfSatisfying(LwM2mObjectInstance.class, instance -> {
            assertThat(instance.getResources()) //
                    .contains( //
                            entry(14, LwM2mSingleResource.newStringResource(14, "+11")),
                            entry(15, LwM2mSingleResource.newStringResource(15, "Moon")));
        });

        // Ensure we received only one notification.
        server.ensureNoNotification(observation, 1, TimeUnit.SECONDS);
    }
}
