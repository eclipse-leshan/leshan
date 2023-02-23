/*******************************************************************************
 * Copyright (c) 2013-2015 Sierra Wireless and others.
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
 *     Zebra Technologies - initial API and implementation
 *     Achim Kraus (Bosch Software Innovations GmbH) - add test for create security object
 *     Achim Kraus (Bosch Software Innovations GmbH) - replace close() with destroy()
 *******************************************************************************/

package org.eclipse.leshan.integration.tests.create;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.leshan.core.ResponseCode.BAD_REQUEST;
import static org.eclipse.leshan.core.ResponseCode.CONTENT;
import static org.eclipse.leshan.core.ResponseCode.CREATED;
import static org.eclipse.leshan.core.ResponseCode.NOT_FOUND;
import static org.eclipse.leshan.integration.tests.util.LeshanTestClientBuilder.givenClientUsing;
import static org.eclipse.leshan.integration.tests.util.LeshanTestServerBuilder.givenServerUsing;
import static org.eclipse.leshan.integration.tests.util.assertion.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.stream.Stream;

import org.eclipse.leshan.core.LwM2mId;
import org.eclipse.leshan.core.endpoint.Protocol;
import org.eclipse.leshan.core.node.LwM2mObject;
import org.eclipse.leshan.core.node.LwM2mObjectInstance;
import org.eclipse.leshan.core.node.LwM2mResource;
import org.eclipse.leshan.core.node.LwM2mSingleResource;
import org.eclipse.leshan.core.request.ContentFormat;
import org.eclipse.leshan.core.request.CreateRequest;
import org.eclipse.leshan.core.request.ReadRequest;
import org.eclipse.leshan.core.request.exception.InvalidRequestException;
import org.eclipse.leshan.core.response.CreateResponse;
import org.eclipse.leshan.core.response.ReadResponse;
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
public class CreateTest {

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
                ContentFormat.TLV, //
                ContentFormat.JSON, //
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
    Registration currentRegistration;

    @BeforeEach
    public void start(ContentFormat format, Protocol givenProtocol, String givenClientEndpointProvider,
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
    public void can_create_instance_without_instance_id(ContentFormat contentFormat, Protocol givenProtocol,
            String givenClientEndpointProvider, String givenServerEndpointProvider) throws InterruptedException {
        try {
            // create ACL instance
            CreateResponse response = server.send(currentRegistration,
                    new CreateRequest(contentFormat, 2, LwM2mSingleResource.newIntegerResource(3, 33)));

            // verify result
            assertThat(response) //
                    .hasCode(CREATED) //
                    .hasValidUnderlyingResponseFor(givenServerEndpointProvider);
            assertThat(response.getLocation()).isEqualTo("2/0");

            // create a second ACL instance
            response = server.send(currentRegistration,
                    new CreateRequest(contentFormat, 2, LwM2mSingleResource.newIntegerResource(3, 34)));

            // verify result
            assertThat(response) //
                    .hasCode(CREATED) //
                    .hasValidUnderlyingResponseFor(givenServerEndpointProvider);
            assertThat(response.getLocation()).isEqualTo("2/1");

            // read object 2
            ReadResponse readResponse = server.send(currentRegistration, new ReadRequest(2));
            assertThat(readResponse).hasCode(CONTENT);
            LwM2mObject object = (LwM2mObject) readResponse.getContent();
            assertThat(object.getInstance(0).getResource(3).getValue()).isEqualTo(33l);
            assertThat(object.getInstance(1).getResource(3).getValue()).isEqualTo(34l);
        } catch (InvalidRequestException e) {
            // only TLV support create instance without instance id
            assertNotEquals(ContentFormat.TLV, contentFormat);
        }
    }

    @TestAllCases
    public void can_create_instance_with_id(ContentFormat contentFormat, Protocol givenProtocol,
            String givenClientEndpointProvider, String givenServerEndpointProvider) throws InterruptedException {
        // create ACL instance
        LwM2mObjectInstance instance = new LwM2mObjectInstance(12, LwM2mSingleResource.newIntegerResource(3, 123));
        CreateResponse response = server.send(currentRegistration, new CreateRequest(contentFormat, 2, instance));

        // verify result
        assertThat(response) //
                .hasCode(CREATED) //
                .hasValidUnderlyingResponseFor(givenServerEndpointProvider);
        assertThat(response.getLocation()).isNull();

        // read object 2
        ReadResponse readResponse = server.send(currentRegistration, new ReadRequest(2));
        assertThat(readResponse).hasCode(CONTENT);
        LwM2mObject object = (LwM2mObject) readResponse.getContent();
        assertThat(object.getInstance(12).getResource(3).getValue()).isEqualTo(123l);
    }

    @TestAllCases
    public void can_create_2_instances_of_object(ContentFormat contentFormat, Protocol givenProtocol,
            String givenClientEndpointProvider, String givenServerEndpointProvider) throws InterruptedException {
        // create ACL instance
        LwM2mObjectInstance instance1 = new LwM2mObjectInstance(12, LwM2mSingleResource.newIntegerResource(3, 123));
        LwM2mObjectInstance instance2 = new LwM2mObjectInstance(13, LwM2mSingleResource.newIntegerResource(3, 124));
        CreateResponse response = server.send(currentRegistration,
                new CreateRequest(contentFormat, 2, instance1, instance2));

        // verify result
        assertThat(response) //
                .hasCode(CREATED) //
                .hasValidUnderlyingResponseFor(givenServerEndpointProvider);
        assertThat(response.getLocation()).isNull();

        // read object 2
        ReadResponse readResponse = server.send(currentRegistration, new ReadRequest(2));
        assertThat(readResponse).hasCode(CONTENT);
        LwM2mObject object = (LwM2mObject) readResponse.getContent();
        assertThat(object.getInstance(12).getResource(3).getValue()).isEqualTo(123l);
        assertThat(object.getInstance(13).getResource(3).getValue()).isEqualTo(124l);
    }

    @TestAllCases
    public void cannot_create_instance_without_all_required_resources(ContentFormat contentFormat,
            Protocol givenProtocol, String givenClientEndpointProvider, String givenServerEndpointProvider)
            throws InterruptedException {
        // create ACL instance without any resources
        CreateResponse response = server.send(currentRegistration, new CreateRequest(contentFormat,
                LwM2mId.ACCESS_CONTROL, new LwM2mObjectInstance(0, new LwM2mResource[0])));

        // verify result
        assertThat(response) //
                .hasCode(BAD_REQUEST) //
                .hasValidUnderlyingResponseFor(givenServerEndpointProvider);

        // create ACL instance with only 1 mandatory resources (2 missing)
        CreateResponse response2 = server.send(currentRegistration,
                new CreateRequest(contentFormat, LwM2mId.ACCESS_CONTROL,
                        new LwM2mObjectInstance(0, LwM2mSingleResource.newIntegerResource(LwM2mId.ACL_OBJECT_ID, 12))));

        // verify result
        assertThat(response2) //
                .hasCode(BAD_REQUEST) //
                .hasValidUnderlyingResponseFor(givenServerEndpointProvider);

        // create 2 ACL instances, one with missing mandatory resource and the other with all mandatory resources
        LwM2mObjectInstance instance0 = new LwM2mObjectInstance(0,
                LwM2mSingleResource.newIntegerResource(LwM2mId.ACL_OBJECT_ID, 22),
                LwM2mSingleResource.newIntegerResource(LwM2mId.ACL_OBJECT_INSTANCE_ID, 22),
                LwM2mSingleResource.newIntegerResource(LwM2mId.ACL_ACCESS_CONTROL_OWNER, 11));
        LwM2mObjectInstance instance1 = new LwM2mObjectInstance(1,
                LwM2mSingleResource.newIntegerResource(LwM2mId.ACL_OBJECT_ID, 22));

        CreateResponse response3 = server.send(currentRegistration,
                new CreateRequest(contentFormat, LwM2mId.ACCESS_CONTROL, instance0, instance1));

        // verify result
        assertThat(response3) //
                .hasCode(BAD_REQUEST) //
                .hasValidUnderlyingResponseFor(givenServerEndpointProvider);

        // try to read to check if the instance is not created
        // client registration
        ReadResponse readResponse = server.send(currentRegistration, new ReadRequest(2, 0));
        assertThat(readResponse) //
                .hasCode(NOT_FOUND) //
                .hasValidUnderlyingResponseFor(givenServerEndpointProvider);
    }
}
