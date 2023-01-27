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

import static org.eclipse.leshan.core.ResponseCode.CHANGED;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Date;
import java.util.stream.Stream;

import org.eclipse.californium.core.coap.Response;
import org.eclipse.leshan.core.LwM2m;
import org.eclipse.leshan.core.ResponseCode;
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
import org.eclipse.leshan.integration.tests.util.IntegrationTestHelper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

public class WriteSingleValueTest {

    @ParameterizedTest(name = "{0}")
    @MethodSource("contentFormats")
    @Retention(RetentionPolicy.RUNTIME)
    private @interface TestAllContentFormat {
    }

    static Stream<ContentFormat> contentFormats() {
        return Stream.of(//
                ContentFormat.TEXT, //
                ContentFormat.CBOR, //
                ContentFormat.TLV, //
                ContentFormat.fromCode(ContentFormat.OLD_TLV_CODE), //
                ContentFormat.fromCode(ContentFormat.OLD_JSON_CODE), //
                ContentFormat.JSON, //
                ContentFormat.SENML_JSON, //
                ContentFormat.SENML_CBOR);
    }

    protected IntegrationTestHelper helper = new IntegrationTestHelper();

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
    public void write_string_resource(ContentFormat contentFormat) throws InterruptedException {
        // write resource
        String expectedvalue = "stringvalue";
        WriteResponse response = helper.server.send(helper.getCurrentRegistration(),
                new WriteRequest(contentFormat, TestLwM2mId.TEST_OBJECT, 0, TestLwM2mId.STRING_VALUE, expectedvalue));

        // verify result
        assertEquals(ResponseCode.CHANGED, response.getCode());
        assertNotNull(response.getCoapResponse());
        assertThat(response.getCoapResponse(), is(instanceOf(Response.class)));

        // read resource to check the value changed
        ReadResponse readResponse = helper.server.send(helper.getCurrentRegistration(),
                new ReadRequest(contentFormat, TestLwM2mId.TEST_OBJECT, 0, TestLwM2mId.STRING_VALUE));
        LwM2mResource resource = (LwM2mResource) readResponse.getContent();
        assertEquals(expectedvalue, resource.getValue());
    }

    @TestAllContentFormat
    public void write_boolean_resource(ContentFormat contentFormat) throws InterruptedException {
        // write resource
        boolean expectedvalue = true;
        WriteResponse response = helper.server.send(helper.getCurrentRegistration(),
                new WriteRequest(contentFormat, TestLwM2mId.TEST_OBJECT, 0, TestLwM2mId.BOOLEAN_VALUE, expectedvalue));

        // verify result
        assertEquals(ResponseCode.CHANGED, response.getCode());
        assertNotNull(response.getCoapResponse());
        assertThat(response.getCoapResponse(), is(instanceOf(Response.class)));

        // read resource to check the value changed
        ReadResponse readResponse = helper.server.send(helper.getCurrentRegistration(),
                new ReadRequest(contentFormat, TestLwM2mId.TEST_OBJECT, 0, TestLwM2mId.BOOLEAN_VALUE));
        LwM2mResource resource = (LwM2mResource) readResponse.getContent();
        assertEquals(expectedvalue, resource.getValue());
    }

    @TestAllContentFormat
    public void write_integer_resource(ContentFormat contentFormat) throws InterruptedException {
        // write resource
        long expectedvalue = -999l;
        WriteResponse response = helper.server.send(helper.getCurrentRegistration(),
                new WriteRequest(contentFormat, TestLwM2mId.TEST_OBJECT, 0, TestLwM2mId.INTEGER_VALUE, expectedvalue));

        // verify result
        assertEquals(ResponseCode.CHANGED, response.getCode());
        assertNotNull(response.getCoapResponse());
        assertThat(response.getCoapResponse(), is(instanceOf(Response.class)));

        // read resource to check the value changed
        ReadResponse readResponse = helper.server.send(helper.getCurrentRegistration(),
                new ReadRequest(contentFormat, TestLwM2mId.TEST_OBJECT, 0, TestLwM2mId.INTEGER_VALUE));
        LwM2mResource resource = (LwM2mResource) readResponse.getContent();
        assertEquals(expectedvalue, resource.getValue());
    }

    @TestAllContentFormat
    public void can_write_string_resource_instance(ContentFormat contentFormat) throws InterruptedException {
        write_string_resource_instance(contentFormat, 0);
    }

    private void write_string_resource_instance(ContentFormat format, int resourceInstance)
            throws InterruptedException {
        // read device model number
        String valueToWrite = "newValue";
        WriteResponse response = helper.server.send(helper.getCurrentRegistration(),
                new WriteRequest(format, TestLwM2mId.TEST_OBJECT, 0, TestLwM2mId.MULTIPLE_STRING_VALUE,
                        resourceInstance, valueToWrite, Type.STRING));

        // verify result
        assertEquals(CHANGED, response.getCode());
        assertNotNull(response.getCoapResponse());
        assertThat(response.getCoapResponse(), is(instanceOf(Response.class)));

        // read resource to check the value changed
        ReadResponse readResponse = helper.server.send(helper.getCurrentRegistration(), new ReadRequest(format,
                TestLwM2mId.TEST_OBJECT, 0, TestLwM2mId.MULTIPLE_STRING_VALUE, resourceInstance));

        // verify result
        LwM2mResourceInstance resource = (LwM2mResourceInstance) readResponse.getContent();
        assertEquals(valueToWrite, resource.getValue());
    }

    @TestAllContentFormat
    public void write_float_resource(ContentFormat contentFormat) throws InterruptedException {
        // write resource
        double expectedvalue = 999.99;
        WriteResponse response = helper.server.send(helper.getCurrentRegistration(),
                new WriteRequest(contentFormat, TestLwM2mId.TEST_OBJECT, 0, TestLwM2mId.FLOAT_VALUE, expectedvalue));

        // verify result
        assertEquals(ResponseCode.CHANGED, response.getCode());
        assertNotNull(response.getCoapResponse());
        assertThat(response.getCoapResponse(), is(instanceOf(Response.class)));

        // read resource to check the value changed
        ReadResponse readResponse = helper.server.send(helper.getCurrentRegistration(),
                new ReadRequest(contentFormat, TestLwM2mId.TEST_OBJECT, 0, TestLwM2mId.FLOAT_VALUE));
        LwM2mResource resource = (LwM2mResource) readResponse.getContent();
        assertEquals(expectedvalue, resource.getValue());
    }

    @TestAllContentFormat
    public void write_time_resource(ContentFormat contentFormat) throws InterruptedException {
        // write resource
        Date expectedvalue = new Date(946681000l); // second accuracy
        WriteResponse response = helper.server.send(helper.getCurrentRegistration(),
                new WriteRequest(contentFormat, TestLwM2mId.TEST_OBJECT, 0, TestLwM2mId.TIME_VALUE, expectedvalue));

        // verify result
        assertEquals(ResponseCode.CHANGED, response.getCode());
        assertNotNull(response.getCoapResponse());
        assertThat(response.getCoapResponse(), is(instanceOf(Response.class)));

        // read resource to check the value changed
        ReadResponse readResponse = helper.server.send(helper.getCurrentRegistration(),
                new ReadRequest(contentFormat, TestLwM2mId.TEST_OBJECT, 0, TestLwM2mId.TIME_VALUE));
        LwM2mResource resource = (LwM2mResource) readResponse.getContent();
        assertEquals(expectedvalue, resource.getValue());
    }

    @TestAllContentFormat
    public void write_corelnk_resource(ContentFormat contentFormat) throws InterruptedException {
        // write resource
        Link[] expectedvalue = new Link[3];
        expectedvalue[0] = new MixedLwM2mLink(null, new LwM2mPath(3),
                LwM2mAttributes.create(LwM2mAttributes.OBJECT_VERSION, new LwM2m.Version("1.2")));
        expectedvalue[1] = new MixedLwM2mLink(null, new LwM2mPath(3, 1));
        expectedvalue[2] = new MixedLwM2mLink(null, new LwM2mPath(3, 1, 0),
                new QuotedStringAttribute("attr1", "attr1Value"), new UnquotedStringAttribute("attr2", "attr2Value"));

        WriteResponse response = helper.server.send(helper.getCurrentRegistration(),
                new WriteRequest(contentFormat, TestLwM2mId.TEST_OBJECT, 0, TestLwM2mId.CORELNK_VALUE, expectedvalue));

        // verify result
        assertEquals(ResponseCode.CHANGED, response.getCode());
        assertNotNull(response.getCoapResponse());
        assertThat(response.getCoapResponse(), is(instanceOf(Response.class)));

        // read resource to check the value changed
        ReadResponse readResponse = helper.server.send(helper.getCurrentRegistration(),
                new ReadRequest(contentFormat, TestLwM2mId.TEST_OBJECT, 0, TestLwM2mId.CORELNK_VALUE));
        LwM2mResource resource = (LwM2mResource) readResponse.getContent();
        assertArrayEquals(expectedvalue, (Link[]) resource.getValue());
    }

    @TestAllContentFormat
    public void write_unsigned_integer_resource(ContentFormat contentFormat) throws InterruptedException {
        // write resource
        ULong expectedvalue = ULong.valueOf("18446744073709551615"); // this unsigned integer can not be stored in a

        WriteResponse response = helper.server.send(helper.getCurrentRegistration(), new WriteRequest(contentFormat,
                TestLwM2mId.TEST_OBJECT, 0, TestLwM2mId.UNSIGNED_INTEGER_VALUE, expectedvalue));

        // verify result
        assertEquals(ResponseCode.CHANGED, response.getCode());
        assertNotNull(response.getCoapResponse());
        assertThat(response.getCoapResponse(), is(instanceOf(Response.class)));

        // read resource to check the value changed
        ReadResponse readResponse = helper.server.send(helper.getCurrentRegistration(),
                new ReadRequest(TestLwM2mId.TEST_OBJECT, 0, TestLwM2mId.UNSIGNED_INTEGER_VALUE));
        LwM2mResource resource = (LwM2mResource) readResponse.getContent();
        assertEquals(expectedvalue, resource.getValue());
    }

    @TestAllContentFormat
    public void can_write_single_instance_objlnk_resource(ContentFormat contentFormat) throws InterruptedException {

        ObjectLink data = new ObjectLink(10245, 1);

        // Write objlnk resource
        WriteResponse response = helper.server.send(helper.getCurrentRegistration(),
                new WriteRequest(contentFormat, TestLwM2mId.TEST_OBJECT, 0, TestLwM2mId.MULTIPLE_OBJLINK_VALUE, data));

        // Verify Write result
        assertEquals(ResponseCode.CHANGED, response.getCode());
        assertNotNull(response.getCoapResponse());
        assertThat(response.getCoapResponse(), is(instanceOf(Response.class)));

        // Reading back the written OBJLNK value
        ReadResponse readResponse = helper.server.send(helper.getCurrentRegistration(),
                new ReadRequest(TestLwM2mId.TEST_OBJECT, 0, TestLwM2mId.MULTIPLE_OBJLINK_VALUE));
        LwM2mSingleResource resource = (LwM2mSingleResource) readResponse.getContent();

        // verify read value
        assertEquals(((ObjectLink) resource.getValue()).getObjectId(), 10245);
        assertEquals(((ObjectLink) resource.getValue()).getObjectInstanceId(), 1);
    }

    @TestAllContentFormat
    public void send_writerequest_synchronously_with_bad_payload_raises_codeexception(ContentFormat contentFormat)
            throws InterruptedException {
        assertThrowsExactly(CodecException.class, () -> {
            helper.server.send(helper.getCurrentRegistration(), new WriteRequest(contentFormat, 3, 0, 13,
                    "a string instead of timestamp for currenttime resource"));
        });

    }

    @TestAllContentFormat
    public void send_writerequest_asynchronously_with_bad_payload_raises_codeexception(ContentFormat contentFormat)
            throws InterruptedException {
        assertThrowsExactly(CodecException.class, () -> {
            helper.server.send(helper.getCurrentRegistration(),
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
