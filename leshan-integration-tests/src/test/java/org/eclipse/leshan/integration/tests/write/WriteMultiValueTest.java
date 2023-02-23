/*******************************************************************************
 * Copyright (c) 2020 Sierra Wireless and others.
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
import java.util.stream.Stream;

import org.eclipse.leshan.core.endpoint.Protocol;
import org.eclipse.leshan.core.model.ResourceModel.Type;
import org.eclipse.leshan.core.node.LwM2mMultipleResource;
import org.eclipse.leshan.core.node.LwM2mObjectInstance;
import org.eclipse.leshan.core.node.LwM2mResource;
import org.eclipse.leshan.core.node.LwM2mResourceInstance;
import org.eclipse.leshan.core.node.LwM2mSingleResource;
import org.eclipse.leshan.core.node.ObjectLink;
import org.eclipse.leshan.core.request.ContentFormat;
import org.eclipse.leshan.core.request.ReadRequest;
import org.eclipse.leshan.core.request.WriteRequest;
import org.eclipse.leshan.core.request.WriteRequest.Mode;
import org.eclipse.leshan.core.response.ReadResponse;
import org.eclipse.leshan.core.response.WriteResponse;
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
public class WriteMultiValueTest {

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
                ContentFormat.fromCode(ContentFormat.OLD_TLV_CODE), //
                ContentFormat.fromCode(ContentFormat.OLD_JSON_CODE), //
                ContentFormat.JSON, //
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
    public void can_write_object_instance(ContentFormat contentFormat, Protocol givenProtocol,
            String givenClientEndpointProvider, String givenServerEndpointProvider) throws InterruptedException {
        // write device timezone and offset
        LwM2mResource utcOffset = LwM2mSingleResource.newStringResource(14, "+02");
        LwM2mResource timeZone = LwM2mSingleResource.newStringResource(15, "Europe/Paris");
        WriteResponse response = server.send(currentRegistration,
                new WriteRequest(Mode.REPLACE, contentFormat, 3, 0, utcOffset, timeZone));

        // verify result
        assertThat(response) //
                .hasCode(CHANGED) //
                .hasValidUnderlyingResponseFor(givenServerEndpointProvider);

        // read the timezone to check the value changed
        ReadResponse readResponse = server.send(currentRegistration, new ReadRequest(3, 0));
        assertThat(readResponse.getContent()).isInstanceOfSatisfying(LwM2mObjectInstance.class, instance -> {
            assertThat(instance.getResources()).contains( //
                    entry(14, utcOffset), //
                    entry(15, timeZone));
        });
    }

    @TestAllCases
    public void can_write_replacing_object_instance(ContentFormat contentFormat, Protocol givenProtocol,
            String givenClientEndpointProvider, String givenServerEndpointProvider) throws InterruptedException {
        // setup server object
        WriteResponse response = server.send(currentRegistration, new WriteRequest(contentFormat, 1, 0, 3, 60));

        // verify result
        assertThat(response) //
                .hasCode(CHANGED) //
                .hasValidUnderlyingResponseFor(givenServerEndpointProvider);

        // write server object
        LwM2mResource lifetime = LwM2mSingleResource.newIntegerResource(1, 120);
        LwM2mResource defaultMinPeriod = LwM2mSingleResource.newIntegerResource(2, 10);
        LwM2mResource notificationStoring = LwM2mSingleResource.newBooleanResource(6, false);
        LwM2mResource binding = LwM2mSingleResource.newStringResource(7, "U");
        response = server.send(currentRegistration, new WriteRequest(Mode.REPLACE, contentFormat, 1, 0, lifetime,
                defaultMinPeriod, notificationStoring, binding));

        // verify result
        assertThat(response) //
                .hasCode(CHANGED) //
                .hasValidUnderlyingResponseFor(givenServerEndpointProvider);

        // read the values to check the value changed
        ReadResponse readResponse = server.send(currentRegistration, new ReadRequest(1, 0));
        assertThat(readResponse.getContent()).isInstanceOfSatisfying(LwM2mObjectInstance.class, instance -> {
            assertThat(instance.getResources()) //
                    .contains( //
                            entry(1, lifetime), //
                            entry(2, defaultMinPeriod), //
                            entry(6, notificationStoring), //
                            entry(7, binding)) //
                    .doesNotContainKey(3); // removed not contained optional writable resource
        });
    }

    @TestAllCases
    public void can_write_updating_object_instance(ContentFormat contentFormat, Protocol givenProtocol,
            String givenClientEndpointProvider, String givenServerEndpointProvider) throws InterruptedException {
        // setup server object
        WriteResponse response = server.send(currentRegistration, new WriteRequest(contentFormat, 1, 0, 3, 60));

        // verify result
        assertThat(response) //
                .hasCode(CHANGED) //
                .hasValidUnderlyingResponseFor(givenServerEndpointProvider);

        // write server object
        LwM2mResource lifetime = LwM2mSingleResource.newIntegerResource(1, 120);
        LwM2mResource defaultMinPeriod = LwM2mSingleResource.newIntegerResource(2, 10);
        response = server.send(currentRegistration,
                new WriteRequest(Mode.UPDATE, contentFormat, 1, 0, lifetime, defaultMinPeriod));

        // verify result
        assertThat(response) //
                .hasCode(CHANGED) //
                .hasValidUnderlyingResponseFor(givenServerEndpointProvider);

        // read the values to check the value changed
        ReadResponse readResponse = server.send(currentRegistration, new ReadRequest(1, 0));
        assertThat(readResponse.getContent()).isInstanceOfSatisfying(LwM2mObjectInstance.class, instance -> {
            assertThat(instance.getResources()) //
                    .contains( //
                            entry(1, lifetime), //
                            entry(2, defaultMinPeriod)) //
                    .containsKeys(3, 6, 7); // no resources are removed when updating
        });
    }

    @TestAllCases
    public void can_write_multi_instance_objlnk_resource(ContentFormat contentFormat, Protocol givenProtocol,
            String givenClientEndpointProvider, String givenServerEndpointProvider) throws InterruptedException {

        Map<Integer, ObjectLink> neighbourCellReport = new HashMap<>();
        neighbourCellReport.put(0, new ObjectLink(10245, 1));
        neighbourCellReport.put(1, new ObjectLink(10242, 2));
        neighbourCellReport.put(2, new ObjectLink(10244, 3));

        // Write objlnk resource in TLV format
        WriteResponse response = server.send(currentRegistration, new WriteRequest(contentFormat,
                TestLwM2mId.TEST_OBJECT, 0, TestLwM2mId.MULTIPLE_OBJLINK_VALUE, neighbourCellReport, Type.OBJLNK));

        // Verify Write result
        assertThat(response) //
                .hasCode(CHANGED) //
                .hasValidUnderlyingResponseFor(givenServerEndpointProvider);

        // Reading back the written OBJLNK value
        ReadResponse readResponse = server.send(currentRegistration,
                new ReadRequest(TestLwM2mId.TEST_OBJECT, 0, TestLwM2mId.MULTIPLE_OBJLINK_VALUE));

        assertThat(readResponse.getContent()).isInstanceOfSatisfying(LwM2mMultipleResource.class, instance -> {
            assertThat(instance.getInstances()) //
                    .contains( //
                            entry(0, LwM2mResourceInstance.newObjectLinkInstance(0, new ObjectLink(10245, 1))), //
                            entry(1, LwM2mResourceInstance.newObjectLinkInstance(1, new ObjectLink(10242, 2))), //
                            entry(2, LwM2mResourceInstance.newObjectLinkInstance(2, new ObjectLink(10244, 3))));
        });
    }

    @TestAllCases
    public void can_write_object_instance_with_empty_multi_resource(ContentFormat contentFormat, Protocol givenProtocol,
            String givenClientEndpointProvider, String givenServerEndpointProvider) throws InterruptedException {

        // =============== Try to write ==================
        // /3442/0/1110 : { 0 = "string1", 1 = "string2" }
        // /3442/0/1120 : { 1= 11, 1 = 22 }

        // create initial value
        Map<Integer, String> strings = new HashMap<>();
        strings.put(0, "string1");
        strings.put(1, "string2");
        LwM2mMultipleResource stringMultiResource = LwM2mMultipleResource
                .newStringResource(TestLwM2mId.MULTIPLE_STRING_VALUE, strings);

        Map<Integer, Long> ints = new HashMap<>();
        ints.put(0, 11l);
        ints.put(1, 22l);
        LwM2mMultipleResource intMultiResource = LwM2mMultipleResource
                .newIntegerResource(TestLwM2mId.MULTIPLE_INTEGER_VALUE, ints);

        // Write new instance
        WriteResponse response = server.send(currentRegistration, new WriteRequest(Mode.REPLACE, contentFormat,
                TestLwM2mId.TEST_OBJECT, 0, stringMultiResource, intMultiResource));

        // Verify Write result
        assertThat(response) //
                .hasCode(CHANGED) //
                .hasValidUnderlyingResponseFor(givenServerEndpointProvider);

        // Reading back the instance
        ReadResponse readResponse = server.send(currentRegistration, new ReadRequest(TestLwM2mId.TEST_OBJECT, 0));
        // verify read value
        assertThat(readResponse.getContent()).isInstanceOfSatisfying(LwM2mObjectInstance.class, instance -> {
            assertThat(instance.getResources()) //
                    .contains( //
                            entry(TestLwM2mId.MULTIPLE_STRING_VALUE, stringMultiResource), //
                            entry(TestLwM2mId.MULTIPLE_INTEGER_VALUE, intMultiResource));
        });

        // =============== Now try to replace with ==================
        // /3441/0/1110 : { 3 = "newString"}

        // create initial value
        strings = new HashMap<>();
        strings.put(3, "newString");
        LwM2mMultipleResource stringMultiResource2 = LwM2mMultipleResource
                .newStringResource(TestLwM2mId.MULTIPLE_STRING_VALUE, strings);

        // Write new instance
        response = server.send(currentRegistration,
                new WriteRequest(Mode.REPLACE, contentFormat, TestLwM2mId.TEST_OBJECT, 0, stringMultiResource2));

        // Verify Write result
        assertThat(response) //
                .hasCode(CHANGED) //
                .hasValidUnderlyingResponseFor(givenServerEndpointProvider);

        // Reading back the instance
        readResponse = server.send(currentRegistration, new ReadRequest(TestLwM2mId.TEST_OBJECT, 0));
        assertThat(readResponse.getContent()).isInstanceOfSatisfying(LwM2mObjectInstance.class, instance -> {
            assertThat(instance.getResources()) //
                    .contains(entry(TestLwM2mId.MULTIPLE_STRING_VALUE, stringMultiResource2)) //
                    .doesNotContainKey(TestLwM2mId.MULTIPLE_INTEGER_VALUE);
        });
    }

    @TestAllCases
    public void can_write_object_resource_instance(ContentFormat contentFormat, Protocol givenProtocol,
            String givenClientEndpointProvider, String givenServerEndpointProvider) throws InterruptedException {

        // --------------------------------
        // write (replace) multi instance
        // --------------------------------
        Map<Integer, String> values = new HashMap<>();
        values.put(10, "value10");
        values.put(20, "value20");

        WriteResponse response = server.send(currentRegistration, new WriteRequest(Mode.REPLACE, contentFormat,
                TestLwM2mId.TEST_OBJECT, 0, TestLwM2mId.MULTIPLE_STRING_VALUE, values, Type.STRING));

        // Verify Write result
        assertThat(response) //
                .hasCode(CHANGED) //
                .hasValidUnderlyingResponseFor(givenServerEndpointProvider);

        // read multi instance
        ReadResponse readResponse = server.send(currentRegistration,
                new ReadRequest(contentFormat, TestLwM2mId.TEST_OBJECT, 0, TestLwM2mId.MULTIPLE_STRING_VALUE));

        // verify result
        assertThat(readResponse) //
                .hasCode(CONTENT) //
                .hasContentFormat(contentFormat, givenServerEndpointProvider);

        assertThat(readResponse.getContent()).isInstanceOfSatisfying(LwM2mMultipleResource.class, instance -> {
            assertThat(instance.getInstances()) //
                    .contains( //
                            entry(10, LwM2mResourceInstance.newStringInstance(10, "value10")), //
                            entry(20, LwM2mResourceInstance.newStringInstance(20, "value20")));
        });

        // --------------------------------
        // write (update) multi instance
        // --------------------------------
        Map<Integer, String> newValues = new HashMap<>();
        newValues.put(20, "value200");
        newValues.put(30, "value30");
        response = server.send(currentRegistration, new WriteRequest(Mode.UPDATE, contentFormat,
                TestLwM2mId.TEST_OBJECT, 0, TestLwM2mId.MULTIPLE_STRING_VALUE, newValues, Type.STRING));

        // Verify Write result
        assertThat(response) //
                .hasCode(CHANGED) //
                .hasValidUnderlyingResponseFor(givenServerEndpointProvider);

        // read multi instance
        readResponse = server.send(currentRegistration,
                new ReadRequest(contentFormat, TestLwM2mId.TEST_OBJECT, 0, TestLwM2mId.MULTIPLE_STRING_VALUE));

        // verify result
        assertThat(readResponse) //
                .hasCode(CONTENT) //
                .hasContentFormat(contentFormat, givenServerEndpointProvider);

        assertThat(readResponse.getContent()).isInstanceOfSatisfying(LwM2mMultipleResource.class, instance -> {
            assertThat(instance.getInstances()) //
                    .contains( //
                            entry(20, LwM2mResourceInstance.newStringInstance(20, "value200")), //
                            entry(30, LwM2mResourceInstance.newStringInstance(30, "value30")), //
                            entry(10, LwM2mResourceInstance.newStringInstance(10, "value10")));
        });

        // --------------------------------
        // write (replace) multi instance
        // --------------------------------
        newValues = new HashMap<>();
        newValues.put(1, "value1");
        response = server.send(currentRegistration, new WriteRequest(Mode.REPLACE, contentFormat,
                TestLwM2mId.TEST_OBJECT, 0, TestLwM2mId.MULTIPLE_STRING_VALUE, newValues, Type.STRING));

        // Verify Write result
        assertThat(response) //
                .hasCode(CHANGED) //
                .hasValidUnderlyingResponseFor(givenServerEndpointProvider);

        // read multi instance
        readResponse = server.send(currentRegistration,
                new ReadRequest(contentFormat, TestLwM2mId.TEST_OBJECT, 0, TestLwM2mId.MULTIPLE_STRING_VALUE));

        // verify result
        assertThat(readResponse) //
                .hasCode(CONTENT) //
                .hasContentFormat(contentFormat, givenServerEndpointProvider);

        assertThat(readResponse.getContent()).isInstanceOfSatisfying(LwM2mMultipleResource.class, instance -> {
            assertThat(instance.getInstances()) //
                    .containsExactly( //
                            entry(1, LwM2mResourceInstance.newStringInstance(1, "value1")));
        });
    }
}
