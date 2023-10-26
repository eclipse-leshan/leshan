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
package org.eclipse.leshan.integration.tests.read;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.leshan.core.ResponseCode.CONTENT;
import static org.eclipse.leshan.integration.tests.util.LeshanTestClientBuilder.givenClientUsing;
import static org.eclipse.leshan.integration.tests.util.LeshanTestServerBuilder.givenServerUsing;
import static org.eclipse.leshan.integration.tests.util.assertion.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.leshan.core.endpoint.Protocol;
import org.eclipse.leshan.core.model.ResourceModel.Type;
import org.eclipse.leshan.core.node.LwM2mObject;
import org.eclipse.leshan.core.node.LwM2mPath;
import org.eclipse.leshan.core.node.LwM2mResourceInstance;
import org.eclipse.leshan.core.node.LwM2mRoot;
import org.eclipse.leshan.core.node.LwM2mSingleResource;
import org.eclipse.leshan.core.request.ContentFormat;
import org.eclipse.leshan.core.request.ReadCompositeRequest;
import org.eclipse.leshan.core.request.ReadRequest;
import org.eclipse.leshan.core.response.ReadCompositeResponse;
import org.eclipse.leshan.core.response.ReadResponse;
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
public class ReadCompositeTest {

    /*---------------------------------/
     *  Parameterized Tests
     * -------------------------------*/
    @ParameterizedTest(name = "requested {0} response {1} over {2} - Client using {3} - Server using {4}")
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

        Object[][] contentFormats = new Object[][] { //
                // {request content format, response content format}
                { ContentFormat.SENML_JSON, ContentFormat.SENML_JSON }, //
                { ContentFormat.SENML_CBOR, ContentFormat.SENML_CBOR }, //
                { ContentFormat.SENML_CBOR, ContentFormat.SENML_JSON }, //
                { ContentFormat.SENML_JSON, ContentFormat.SENML_CBOR }, //
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
    public void start(ContentFormat requestContentFormat, ContentFormat responseContentFormat, Protocol givenProtocol,
            String givenClientEndpointProvider, String givenServerEndpointProvider) {
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
    public void can_read_root(ContentFormat requestContentFormat, ContentFormat responseContentFormat,
            Protocol givenProtocol, String givenClientEndpointProvider, String givenServerEndpointProvider)
            throws InterruptedException {
        // read root object
        ReadCompositeResponse response = server.send(currentRegistration,
                new ReadCompositeRequest(requestContentFormat, responseContentFormat, "/"));

        // verify result
        assertThat(response) //
                .hasCode(CONTENT) //
                .hasCode(response.getCode()).hasContentFormat(responseContentFormat, givenServerEndpointProvider);

        assertThat(response.getContent("/")).isInstanceOfSatisfying(LwM2mRoot.class, (root) -> {
            Set<LwM2mPath> availableInstances = currentRegistration.getAvailableInstances();

            // check we get expected object ids
            Set<Integer> expectedObjectIds = availableInstances.stream() //
                    .map(path -> path.getObjectId()) //
                    .collect(Collectors.toSet());
            assertThat(root.getObjects()).containsOnlyKeys(expectedObjectIds);

            // check we get expected instance ids
            for (Integer objectId : expectedObjectIds) {
                Set<Integer> expectedInstanceIds = availableInstances.stream() //
                        .filter(path -> path.getObjectId() == objectId) //
                        .map(path -> path.getObjectInstanceId()) //
                        .collect(Collectors.toSet());

                assertThat(root.getObjects().get(objectId).getInstances()).containsOnlyKeys(expectedInstanceIds);
            }

            // check we right value
            for (LwM2mPath path : availableInstances) {
                // get expected value
                try {
                    ReadResponse readResponse = server.send(currentRegistration,
                            new ReadRequest(ContentFormat.SENML_JSON, path.toString()));
                    assertThat(readResponse).hasCode(CONTENT);

                    // compare it to read composite result
                    assertThat(root.getObjects().get(path.getObjectId()).getInstances().get(path.getObjectInstanceId()))
                            .isEqualTo(readResponse.getContent());
                } catch (InterruptedException e) {
                    fail(e);
                }
            }
        });
    }

    @TestAllCases
    public void can_read_resources(ContentFormat requestContentFormat, ContentFormat responseContentFormat,
            Protocol givenProtocol, String givenClientEndpointProvider, String givenServerEndpointProvider)
            throws InterruptedException {
        // read device model number
        ReadCompositeResponse response = server.send(currentRegistration,
                new ReadCompositeRequest(requestContentFormat, responseContentFormat, "/3/0/0", "/1/0/1"));

        // verify result
        assertThat(response) //
                .hasCode(CONTENT) //
                .hasContentFormat(responseContentFormat, givenServerEndpointProvider);

        LwM2mSingleResource resource = (LwM2mSingleResource) response.getContent("/3/0/0");
        assertThat(resource.getId()).isEqualTo(0);
        assertThat(resource.getType()).isEqualTo(Type.STRING);

        resource = (LwM2mSingleResource) response.getContent("/1/0/1");
        assertThat(resource.getId()).isEqualTo(1);
        assertThat(resource.getType()).isEqualTo(Type.INTEGER);
    }

    @TestAllCases
    public void can_read_resource_instance(ContentFormat requestContentFormat, ContentFormat responseContentFormat,
            Protocol givenProtocol, String givenClientEndpointProvider, String givenServerEndpointProvider)
            throws InterruptedException {
        // read resource instance
        String path = "/" + TestLwM2mId.TEST_OBJECT + "/0/" + TestLwM2mId.MULTIPLE_STRING_VALUE + "/0";
        ReadCompositeResponse response = server.send(currentRegistration,
                new ReadCompositeRequest(requestContentFormat, responseContentFormat, path));

        // verify result
        assertThat(response) //
                .hasCode(CONTENT) //
                .hasContentFormat(responseContentFormat, givenServerEndpointProvider);

        LwM2mResourceInstance resource = (LwM2mResourceInstance) response.getContent(path);
        assertThat(resource.getId()).isEqualTo(0);
        assertThat(resource.getType()).isEqualTo(Type.STRING);

    }

    @TestAllCases
    public void can_read_resource_and_instance(ContentFormat requestContentFormat, ContentFormat responseContentFormat,
            Protocol givenProtocol, String givenClientEndpointProvider, String givenServerEndpointProvider)
            throws InterruptedException {
        // read device model number
        ReadCompositeResponse response = server.send(currentRegistration,
                new ReadCompositeRequest(requestContentFormat, responseContentFormat, "/3/0/0", "/1"));

        // verify result
        assertThat(response) //
                .hasCode(CONTENT) //
                .hasContentFormat(responseContentFormat, givenServerEndpointProvider);

        LwM2mSingleResource resource = (LwM2mSingleResource) response.getContent("/3/0/0");
        assertThat(resource.getId()).isEqualTo(0);
        assertThat(resource.getType()).isEqualTo(Type.STRING);

        LwM2mObject object = (LwM2mObject) response.getContent("/1");
        assertThat(object.getId()).isEqualTo(1);
        assertThat(object.getInstances()).hasSize(1);
    }
}
