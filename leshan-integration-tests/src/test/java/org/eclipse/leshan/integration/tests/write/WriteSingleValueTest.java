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
import static org.eclipse.leshan.core.ResponseCode.CHANGED;
import static org.eclipse.leshan.integration.tests.util.LeshanTestClientBuilder.givenClientUsing;
import static org.eclipse.leshan.integration.tests.util.LeshanTestServerBuilder.givenServerUsing;
import static org.eclipse.leshan.integration.tests.util.assertion.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Date;
import java.util.stream.Stream;

import org.eclipse.leshan.core.LwM2m;
import org.eclipse.leshan.core.endpoint.Protocol;
import org.eclipse.leshan.core.link.Link;
import org.eclipse.leshan.core.link.attributes.QuotedStringAttribute;
import org.eclipse.leshan.core.link.attributes.UnquotedStringAttribute;
import org.eclipse.leshan.core.link.lwm2m.MixedLwM2mLink;
import org.eclipse.leshan.core.link.lwm2m.attributes.LwM2mAttributes;
import org.eclipse.leshan.core.model.ResourceModel.Type;
import org.eclipse.leshan.core.node.LwM2mPath;
import org.eclipse.leshan.core.node.LwM2mResource;
import org.eclipse.leshan.core.node.LwM2mResourceInstance;
import org.eclipse.leshan.core.node.LwM2mSingleResource;
import org.eclipse.leshan.core.node.ObjectLink;
import org.eclipse.leshan.core.node.codec.CodecException;
import org.eclipse.leshan.core.request.ContentFormat;
import org.eclipse.leshan.core.request.ReadRequest;
import org.eclipse.leshan.core.request.WriteRequest;
import org.eclipse.leshan.core.response.ErrorCallback;
import org.eclipse.leshan.core.response.ReadResponse;
import org.eclipse.leshan.core.response.ResponseCallback;
import org.eclipse.leshan.core.response.WriteResponse;
import org.eclipse.leshan.core.util.TestLwM2mId;
import org.eclipse.leshan.core.util.datatype.ULong;
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
public class WriteSingleValueTest {

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
                ContentFormat.TEXT, //
                ContentFormat.CBOR, //
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
    public void write_string_resource(ContentFormat contentFormat, Protocol givenProtocol,
            String givenClientEndpointProvider, String givenServerEndpointProvider) throws InterruptedException {
        // write resource
        String expectedvalue = "stringvalue";
        WriteResponse response = server.send(currentRegistration,
                new WriteRequest(contentFormat, TestLwM2mId.TEST_OBJECT, 0, TestLwM2mId.STRING_VALUE, expectedvalue));

        // verify result
        assertThat(response) //
                .hasCode(CHANGED) //
                .hasValidUnderlyingResponseFor(givenServerEndpointProvider);

        // read resource to check the value changed
        ReadResponse readResponse = server.send(currentRegistration,
                new ReadRequest(contentFormat, TestLwM2mId.TEST_OBJECT, 0, TestLwM2mId.STRING_VALUE));
        LwM2mResource resource = (LwM2mResource) readResponse.getContent();
        assertThat(resource.getValue()).isEqualTo(expectedvalue);
    }

    @TestAllCases
    public void write_boolean_resource(ContentFormat contentFormat, Protocol givenProtocol,
            String givenClientEndpointProvider, String givenServerEndpointProvider) throws InterruptedException {
        // write resource
        boolean expectedvalue = true;
        WriteResponse response = server.send(currentRegistration,
                new WriteRequest(contentFormat, TestLwM2mId.TEST_OBJECT, 0, TestLwM2mId.BOOLEAN_VALUE, expectedvalue));

        // verify result
        assertThat(response) //
                .hasCode(CHANGED) //
                .hasValidUnderlyingResponseFor(givenServerEndpointProvider);

        // read resource to check the value changed
        ReadResponse readResponse = server.send(currentRegistration,
                new ReadRequest(contentFormat, TestLwM2mId.TEST_OBJECT, 0, TestLwM2mId.BOOLEAN_VALUE));
        LwM2mResource resource = (LwM2mResource) readResponse.getContent();
        assertThat(resource.getValue()).isEqualTo(expectedvalue);
    }

    @TestAllCases
    public void write_integer_resource(ContentFormat contentFormat, Protocol givenProtocol,
            String givenClientEndpointProvider, String givenServerEndpointProvider) throws InterruptedException {
        // write resource
        long expectedvalue = -999l;
        WriteResponse response = server.send(currentRegistration,
                new WriteRequest(contentFormat, TestLwM2mId.TEST_OBJECT, 0, TestLwM2mId.INTEGER_VALUE, expectedvalue));

        // verify result
        assertThat(response) //
                .hasCode(CHANGED) //
                .hasValidUnderlyingResponseFor(givenServerEndpointProvider);

        // read resource to check the value changed
        ReadResponse readResponse = server.send(currentRegistration,
                new ReadRequest(contentFormat, TestLwM2mId.TEST_OBJECT, 0, TestLwM2mId.INTEGER_VALUE));
        LwM2mResource resource = (LwM2mResource) readResponse.getContent();
        assertThat(resource.getValue()).isEqualTo(expectedvalue);
    }

    @TestAllCases
    public void can_write_string_resource_instance(ContentFormat contentFormat, Protocol givenProtocol,
            String givenClientEndpointProvider, String givenServerEndpointProvider) throws InterruptedException {

        // read device model number
        int resourceInstance = 0;
        String expectedValue = "newValue";
        WriteResponse response = server.send(currentRegistration,
                new WriteRequest(contentFormat, TestLwM2mId.TEST_OBJECT, 0, TestLwM2mId.MULTIPLE_STRING_VALUE,
                        resourceInstance, expectedValue, Type.STRING));

        // verify result
        assertThat(response) //
                .hasCode(CHANGED) //
                .hasValidUnderlyingResponseFor(givenServerEndpointProvider);

        // read resource to check the value changed
        ReadResponse readResponse = server.send(currentRegistration, new ReadRequest(contentFormat,
                TestLwM2mId.TEST_OBJECT, 0, TestLwM2mId.MULTIPLE_STRING_VALUE, resourceInstance));

        // verify result
        LwM2mResourceInstance resource = (LwM2mResourceInstance) readResponse.getContent();
        assertThat(resource.getValue()).isEqualTo(expectedValue);
    }

    @TestAllCases
    public void write_float_resource(ContentFormat contentFormat, Protocol givenProtocol,
            String givenClientEndpointProvider, String givenServerEndpointProvider) throws InterruptedException {
        // write resource
        double expectedValue = 999.99;
        WriteResponse response = server.send(currentRegistration,
                new WriteRequest(contentFormat, TestLwM2mId.TEST_OBJECT, 0, TestLwM2mId.FLOAT_VALUE, expectedValue));

        // verify result
        assertThat(response) //
                .hasCode(CHANGED) //
                .hasValidUnderlyingResponseFor(givenServerEndpointProvider);

        // read resource to check the value changed
        ReadResponse readResponse = server.send(currentRegistration,
                new ReadRequest(contentFormat, TestLwM2mId.TEST_OBJECT, 0, TestLwM2mId.FLOAT_VALUE));
        LwM2mResource resource = (LwM2mResource) readResponse.getContent();
        assertThat(resource.getValue()).isEqualTo(expectedValue);
    }

    @TestAllCases
    public void write_time_resource(ContentFormat contentFormat, Protocol givenProtocol,
            String givenClientEndpointProvider, String givenServerEndpointProvider) throws InterruptedException {
        // write resource
        Date expectedValue = new Date(946681000l); // second accuracy
        WriteResponse response = server.send(currentRegistration,
                new WriteRequest(contentFormat, TestLwM2mId.TEST_OBJECT, 0, TestLwM2mId.TIME_VALUE, expectedValue));

        // verify result
        assertThat(response) //
                .hasCode(CHANGED) //
                .hasValidUnderlyingResponseFor(givenServerEndpointProvider);

        // read resource to check the value changed
        ReadResponse readResponse = server.send(currentRegistration,
                new ReadRequest(contentFormat, TestLwM2mId.TEST_OBJECT, 0, TestLwM2mId.TIME_VALUE));
        LwM2mResource resource = (LwM2mResource) readResponse.getContent();
        assertThat(resource.getValue()).isEqualTo(expectedValue);
    }

    @TestAllCases
    public void write_corelnk_resource(ContentFormat contentFormat, Protocol givenProtocol,
            String givenClientEndpointProvider, String givenServerEndpointProvider) throws InterruptedException {
        // write resource
        Link[] expectedValue = new Link[3];
        expectedValue[0] = new MixedLwM2mLink(null, new LwM2mPath(3),
                LwM2mAttributes.create(LwM2mAttributes.OBJECT_VERSION, new LwM2m.Version("1.2")));
        expectedValue[1] = new MixedLwM2mLink(null, new LwM2mPath(3, 1));
        expectedValue[2] = new MixedLwM2mLink(null, new LwM2mPath(3, 1, 0),
                new QuotedStringAttribute("attr1", "attr1Value"), new UnquotedStringAttribute("attr2", "attr2Value"));

        WriteResponse response = server.send(currentRegistration,
                new WriteRequest(contentFormat, TestLwM2mId.TEST_OBJECT, 0, TestLwM2mId.CORELNK_VALUE, expectedValue));

        // verify result
        assertThat(response) //
                .hasCode(CHANGED) //
                .hasValidUnderlyingResponseFor(givenServerEndpointProvider);

        // read resource to check the value changed
        ReadResponse readResponse = server.send(currentRegistration,
                new ReadRequest(contentFormat, TestLwM2mId.TEST_OBJECT, 0, TestLwM2mId.CORELNK_VALUE));
        LwM2mResource resource = (LwM2mResource) readResponse.getContent();
        assertThat((Link[]) resource.getValue()).isEqualTo(expectedValue);
    }

    @TestAllCases
    public void write_unsigned_integer_resource(ContentFormat contentFormat, Protocol givenProtocol,
            String givenClientEndpointProvider, String givenServerEndpointProvider) throws InterruptedException {
        // write resource
        ULong expectedValue = ULong.valueOf("18446744073709551615"); // this unsigned integer can not be stored in a

        WriteResponse response = server.send(currentRegistration, new WriteRequest(contentFormat,
                TestLwM2mId.TEST_OBJECT, 0, TestLwM2mId.UNSIGNED_INTEGER_VALUE, expectedValue));

        // verify result
        assertThat(response) //
                .hasCode(CHANGED) //
                .hasValidUnderlyingResponseFor(givenServerEndpointProvider);

        // read resource to check the value changed
        ReadResponse readResponse = server.send(currentRegistration,
                new ReadRequest(TestLwM2mId.TEST_OBJECT, 0, TestLwM2mId.UNSIGNED_INTEGER_VALUE));
        LwM2mResource resource = (LwM2mResource) readResponse.getContent();
        assertThat(resource.getValue()).isEqualTo(expectedValue);
    }

    @TestAllCases
    public void can_write_single_instance_objlnk_resource(ContentFormat contentFormat, Protocol givenProtocol,
            String givenClientEndpointProvider, String givenServerEndpointProvider) throws InterruptedException {

        ObjectLink expectedValue = new ObjectLink(10245, 1);

        // Write objlnk resource
        WriteResponse response = server.send(currentRegistration, new WriteRequest(contentFormat,
                TestLwM2mId.TEST_OBJECT, 0, TestLwM2mId.MULTIPLE_OBJLINK_VALUE, expectedValue));

        // Verify Write result
        assertThat(response) //
                .hasCode(CHANGED) //
                .hasValidUnderlyingResponseFor(givenServerEndpointProvider);

        // Reading back the written OBJLNK value
        ReadResponse readResponse = server.send(currentRegistration,
                new ReadRequest(TestLwM2mId.TEST_OBJECT, 0, TestLwM2mId.MULTIPLE_OBJLINK_VALUE));
        LwM2mSingleResource resource = (LwM2mSingleResource) readResponse.getContent();
        assertThat(resource.getValue()).isEqualTo(expectedValue);
    }

    @TestAllCases
    public void send_writerequest_synchronously_with_bad_payload_raises_codeexception(ContentFormat contentFormat,
            Protocol givenProtocol, String givenClientEndpointProvider, String givenServerEndpointProvider)
            throws InterruptedException {
        assertThrowsExactly(CodecException.class, () -> {
            server.send(currentRegistration, new WriteRequest(contentFormat, 3, 0, 13,
                    "a string instead of timestamp for currenttime resource"));
        });

    }

    @TestAllCases
    public void send_writerequest_asynchronously_with_bad_payload_raises_codeexception(ContentFormat contentFormat,
            Protocol givenProtocol, String givenClientEndpointProvider, String givenServerEndpointProvider)
            throws InterruptedException {
        assertThrowsExactly(CodecException.class, () -> {
            server.send(currentRegistration,
                    new WriteRequest(contentFormat, 3, 0, 13, "a string instead of timestamp for currenttime resource"),
                    new ResponseCallback<WriteResponse>() {
                        @Override
                        public void onResponse(WriteResponse response) {
                        }
                    }, new ErrorCallback() {
                        @Override
                        public void onError(Exception e) {
                        }
                    });
        });
    }
}
