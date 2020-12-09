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
import static org.eclipse.leshan.integration.tests.util.IntegrationTestHelper.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.Collection;
import java.util.Date;

import org.eclipse.californium.core.coap.Response;
import org.eclipse.leshan.core.ResponseCode;
import org.eclipse.leshan.core.model.ResourceModel.Type;
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
import org.eclipse.leshan.core.util.datatype.ULong;
import org.eclipse.leshan.integration.tests.util.IntegrationTestHelper;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class WriteSingleValueTest {
    protected IntegrationTestHelper helper = new IntegrationTestHelper();

    @Parameters(name = "{0}")
    public static Collection<?> contentFormats() {
        return Arrays.asList(new Object[][] { //
                                { ContentFormat.TEXT }, //
                                { ContentFormat.TLV }, //
                                { ContentFormat.CBOR }, //
                                { ContentFormat.fromCode(ContentFormat.OLD_TLV_CODE) }, //
                                { ContentFormat.JSON }, //
                                { ContentFormat.fromCode(ContentFormat.OLD_JSON_CODE) }, //
                                { ContentFormat.SENML_JSON }, //
                                { ContentFormat.SENML_CBOR } });
    }

    private ContentFormat contentFormat;

    public WriteSingleValueTest(ContentFormat contentFormat) {
        this.contentFormat = contentFormat;
    }

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
    public void write_string_resource() throws InterruptedException {
        // write resource
        String expectedvalue = "stringvalue";
        WriteResponse response = helper.server.send(helper.getCurrentRegistration(),
                new WriteRequest(contentFormat, TEST_OBJECT_ID, 0, STRING_RESOURCE_ID, expectedvalue));

        // verify result
        assertEquals(ResponseCode.CHANGED, response.getCode());
        assertNotNull(response.getCoapResponse());
        assertThat(response.getCoapResponse(), is(instanceOf(Response.class)));

        // read resource to check the value changed
        ReadResponse readResponse = helper.server.send(helper.getCurrentRegistration(),
                new ReadRequest(contentFormat, TEST_OBJECT_ID, 0, STRING_RESOURCE_ID));
        LwM2mResource resource = (LwM2mResource) readResponse.getContent();
        assertEquals(expectedvalue, resource.getValue());
    }

    @Test
    public void write_boolean_resource() throws InterruptedException {
        // write resource
        boolean expectedvalue = true;
        WriteResponse response = helper.server.send(helper.getCurrentRegistration(),
                new WriteRequest(contentFormat, TEST_OBJECT_ID, 0, BOOLEAN_RESOURCE_ID, expectedvalue));

        // verify result
        assertEquals(ResponseCode.CHANGED, response.getCode());
        assertNotNull(response.getCoapResponse());
        assertThat(response.getCoapResponse(), is(instanceOf(Response.class)));

        // read resource to check the value changed
        ReadResponse readResponse = helper.server.send(helper.getCurrentRegistration(),
                new ReadRequest(contentFormat, TEST_OBJECT_ID, 0, BOOLEAN_RESOURCE_ID));
        LwM2mResource resource = (LwM2mResource) readResponse.getContent();
        assertEquals(expectedvalue, resource.getValue());
    }

    @Test
    public void write_integer_resource() throws InterruptedException {
        // write resource
        long expectedvalue = -999l;
        WriteResponse response = helper.server.send(helper.getCurrentRegistration(),
                new WriteRequest(contentFormat, TEST_OBJECT_ID, 0, INTEGER_RESOURCE_ID, expectedvalue));

        // verify result
        assertEquals(ResponseCode.CHANGED, response.getCode());
        assertNotNull(response.getCoapResponse());
        assertThat(response.getCoapResponse(), is(instanceOf(Response.class)));

        // read resource to check the value changed
        ReadResponse readResponse = helper.server.send(helper.getCurrentRegistration(),
                new ReadRequest(contentFormat, TEST_OBJECT_ID, 0, INTEGER_RESOURCE_ID));
        LwM2mResource resource = (LwM2mResource) readResponse.getContent();
        assertEquals(expectedvalue, resource.getValue());
    }

    @Test
    public void can_write_string_resource_instance() throws InterruptedException {
        write_string_resource_instance(contentFormat, 0);
        write_string_resource_instance(contentFormat, 1);
    }

    private void write_string_resource_instance(ContentFormat format, int resourceInstance)
            throws InterruptedException {
        // read device model number
        String valueToWrite = "newValue";
        WriteResponse response = helper.server.send(helper.getCurrentRegistration(), new WriteRequest(format,
                TEST_OBJECT_ID, 0, STRING_RESOURCE_INSTANCE_ID, resourceInstance, valueToWrite, Type.STRING));

        // verify result
        assertEquals(CHANGED, response.getCode());
        assertNotNull(response.getCoapResponse());
        assertThat(response.getCoapResponse(), is(instanceOf(Response.class)));

        // read resource to check the value changed
        ReadResponse readResponse = helper.server.send(helper.getCurrentRegistration(),
                new ReadRequest(format, TEST_OBJECT_ID, 0, STRING_RESOURCE_INSTANCE_ID, resourceInstance));

        // verify result
        LwM2mResourceInstance resource = (LwM2mResourceInstance) readResponse.getContent();
        assertEquals(valueToWrite, resource.getValue());
    }

    @Test
    public void write_float_resource() throws InterruptedException {
        // write resource
        double expectedvalue = 999.99;
        WriteResponse response = helper.server.send(helper.getCurrentRegistration(),
                new WriteRequest(contentFormat, TEST_OBJECT_ID, 0, FLOAT_RESOURCE_ID, expectedvalue));

        // verify result
        assertEquals(ResponseCode.CHANGED, response.getCode());
        assertNotNull(response.getCoapResponse());
        assertThat(response.getCoapResponse(), is(instanceOf(Response.class)));

        // read resource to check the value changed
        ReadResponse readResponse = helper.server.send(helper.getCurrentRegistration(),
                new ReadRequest(contentFormat, TEST_OBJECT_ID, 0, FLOAT_RESOURCE_ID));
        LwM2mResource resource = (LwM2mResource) readResponse.getContent();
        assertEquals(expectedvalue, resource.getValue());
    }

    @Test
    public void write_time_resource() throws InterruptedException {
        // write resource
        Date expectedvalue = new Date(946681000l); // second accuracy
        WriteResponse response = helper.server.send(helper.getCurrentRegistration(),
                new WriteRequest(contentFormat, TEST_OBJECT_ID, 0, TIME_RESOURCE_ID, expectedvalue), 100000000);

        // verify result
        assertEquals(ResponseCode.CHANGED, response.getCode());
        assertNotNull(response.getCoapResponse());
        assertThat(response.getCoapResponse(), is(instanceOf(Response.class)));

        // read resource to check the value changed
        ReadResponse readResponse = helper.server.send(helper.getCurrentRegistration(),
                new ReadRequest(contentFormat, TEST_OBJECT_ID, 0, TIME_RESOURCE_ID), 100000000);
        LwM2mResource resource = (LwM2mResource) readResponse.getContent();
        assertEquals(expectedvalue, resource.getValue());
    }

    @Test
    public void write_unsigned_integer_resource() throws InterruptedException {
        // write resource
        ULong expectedvalue = ULong.valueOf("18446744073709551615"); // this unsigned integer can not be stored in a

        WriteResponse response = helper.server.send(helper.getCurrentRegistration(),
                new WriteRequest(contentFormat, TEST_OBJECT_ID, 0, UNSIGNED_INTEGER_RESOURCE_ID, expectedvalue));

        // verify result
        assertEquals(ResponseCode.CHANGED, response.getCode());
        assertNotNull(response.getCoapResponse());
        assertThat(response.getCoapResponse(), is(instanceOf(Response.class)));

        // read resource to check the value changed
        ReadResponse readResponse = helper.server.send(helper.getCurrentRegistration(),
                new ReadRequest(TEST_OBJECT_ID, 0, UNSIGNED_INTEGER_RESOURCE_ID));
        LwM2mResource resource = (LwM2mResource) readResponse.getContent();
        assertEquals(expectedvalue, resource.getValue());
    }

    @Test
    public void can_write_single_instance_objlnk_resource() throws InterruptedException {

        ObjectLink data = new ObjectLink(10245, 1);

        // Write objlnk resource
        WriteResponse response = helper.server.send(helper.getCurrentRegistration(),
                new WriteRequest(contentFormat, IntegrationTestHelper.TEST_OBJECT_ID, 0,
                        IntegrationTestHelper.OBJLNK_SINGLE_INSTANCE_RESOURCE_ID, data));

        // Verify Write result
        assertEquals(ResponseCode.CHANGED, response.getCode());
        assertNotNull(response.getCoapResponse());
        assertThat(response.getCoapResponse(), is(instanceOf(Response.class)));

        // Reading back the written OBJLNK value
        ReadResponse readResponse = helper.server.send(helper.getCurrentRegistration(), new ReadRequest(
                IntegrationTestHelper.TEST_OBJECT_ID, 0, IntegrationTestHelper.OBJLNK_SINGLE_INSTANCE_RESOURCE_ID));
        LwM2mSingleResource resource = (LwM2mSingleResource) readResponse.getContent();

        // verify read value
        assertEquals(((ObjectLink) resource.getValue()).getObjectId(), 10245);
        assertEquals(((ObjectLink) resource.getValue()).getObjectInstanceId(), 1);
    }

    @Test(expected = CodecException.class)
    public void send_writerequest_synchronously_with_bad_payload_raises_codeexception() throws InterruptedException {
        helper.server.send(helper.getCurrentRegistration(),
                new WriteRequest(contentFormat, 3, 0, 13, "a string instead of timestamp for currenttime resource"));

    }

    @Test(expected = CodecException.class)
    public void send_writerequest_asynchronously_with_bad_payload_raises_codeexception() throws InterruptedException {
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
    }
}
