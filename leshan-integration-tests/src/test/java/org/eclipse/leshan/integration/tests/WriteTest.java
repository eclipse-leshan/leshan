/*******************************************************************************
 * Copyright (c) 2013-2015 Sierra Wireless and others.
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v1.0 which accompany this distribution.
 * 
 * The Eclipse Public License is available at
 *    http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 *    http://www.eclipse.org/org/documents/edl-v10.html.
 * 
 * Contributors:
 *     Zebra Technologies - initial API and implementation
 *     Achim Kraus (Bosch Software Innovations GmbH) - add test for write security object
 *     Achim Kraus (Bosch Software Innovations GmbH) - add test for update and replace instances
 *******************************************************************************/

package org.eclipse.leshan.integration.tests;

import static org.eclipse.leshan.integration.tests.IntegrationTestHelper.BOOLEAN_RESOURCE_ID;
import static org.eclipse.leshan.integration.tests.IntegrationTestHelper.FLOAT_RESOURCE_ID;
import static org.eclipse.leshan.integration.tests.IntegrationTestHelper.INTEGER_RESOURCE_ID;
import static org.eclipse.leshan.integration.tests.IntegrationTestHelper.OPAQUE_RESOURCE_ID;
import static org.eclipse.leshan.integration.tests.IntegrationTestHelper.STRING_RESOURCE_ID;
import static org.eclipse.leshan.integration.tests.IntegrationTestHelper.TEST_OBJECT_ID;
import static org.eclipse.leshan.integration.tests.IntegrationTestHelper.TIME_RESOURCE_ID;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.Date;

import org.eclipse.leshan.ResponseCode;
import org.eclipse.leshan.core.node.LwM2mObjectInstance;
import org.eclipse.leshan.core.node.LwM2mResource;
import org.eclipse.leshan.core.node.LwM2mSingleResource;
import org.eclipse.leshan.core.request.ContentFormat;
import org.eclipse.leshan.core.request.ReadRequest;
import org.eclipse.leshan.core.request.WriteRequest;
import org.eclipse.leshan.core.request.WriteRequest.Mode;
import org.eclipse.leshan.core.response.ReadResponse;
import org.eclipse.leshan.core.response.WriteResponse;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class WriteTest {
    private IntegrationTestHelper helper = new IntegrationTestHelper();

    @Before
    public void start() {
        helper.createServer();
        helper.server.start();
        helper.createClient();
        helper.client.start();
        helper.waitForRegistration(1);
    }

    @After
    public void stop() {
        helper.client.stop(false);
        helper.server.stop();
    }

    @Test
    public void can_write_string_resource_in_text() throws InterruptedException {
        write_string_resource(ContentFormat.TEXT);
    }

    @Test
    public void can_write_string_resource_in_tlv() throws InterruptedException {
        write_string_resource(ContentFormat.TLV);
    }

    @Test
    // TODO fix json format
    public void can_write_string_resource_in_json() throws InterruptedException {
        write_string_resource(ContentFormat.JSON);
    }

    private void write_string_resource(ContentFormat format) throws InterruptedException {
        // write resource
        final String expectedvalue = "stringvalue";
        WriteResponse response = helper.server.send(helper.getClient(),
                new WriteRequest(format, TEST_OBJECT_ID, 0, STRING_RESOURCE_ID, expectedvalue));

        // verify result
        assertEquals(ResponseCode.CHANGED, response.getCode());

        // read resource to check the value changed
        ReadResponse readResponse = helper.server.send(helper.getClient(),
                new ReadRequest(TEST_OBJECT_ID, 0, STRING_RESOURCE_ID));
        LwM2mResource resource = (LwM2mResource) readResponse.getContent();
        assertEquals(expectedvalue, resource.getValue());
    }

    @Test
    public void can_write_boolean_resource_in_text() throws InterruptedException {
        write_boolean_resource(ContentFormat.TEXT);
    }

    @Test
    public void can_write_boolean_resource_in_tlv() throws InterruptedException {
        write_boolean_resource(ContentFormat.TLV);
    }

    @Test
    public void can_write_boolean_resource_in_json() throws InterruptedException {
        write_boolean_resource(ContentFormat.JSON);
    }

    private void write_boolean_resource(ContentFormat format) throws InterruptedException {
        // write resource
        final boolean expectedvalue = true;
        WriteResponse response = helper.server.send(helper.getClient(),
                new WriteRequest(format, TEST_OBJECT_ID, 0, BOOLEAN_RESOURCE_ID, expectedvalue));

        // verify result
        assertEquals(ResponseCode.CHANGED, response.getCode());

        // read resource to check the value changed
        ReadResponse readResponse = helper.server.send(helper.getClient(),
                new ReadRequest(TEST_OBJECT_ID, 0, BOOLEAN_RESOURCE_ID));
        LwM2mResource resource = (LwM2mResource) readResponse.getContent();
        assertEquals(expectedvalue, resource.getValue());
    }

    @Test
    public void can_write_integer_resource_in_text() throws InterruptedException {
        write_integer_resource(ContentFormat.TEXT);
    }

    @Test
    public void can_write_integer_resource_in_tlv() throws InterruptedException {
        write_integer_resource(ContentFormat.TLV);
    }

    @Test
    public void can_write_integer_resource_in_json() throws InterruptedException {
        write_integer_resource(ContentFormat.JSON);
    }

    private void write_integer_resource(ContentFormat format) throws InterruptedException {
        // write resource
        final long expectedvalue = 999l;
        WriteResponse response = helper.server.send(helper.getClient(),
                new WriteRequest(format, TEST_OBJECT_ID, 0, INTEGER_RESOURCE_ID, expectedvalue));

        // verify result
        assertEquals(ResponseCode.CHANGED, response.getCode());

        // read resource to check the value changed
        ReadResponse readResponse = helper.server.send(helper.getClient(),
                new ReadRequest(TEST_OBJECT_ID, 0, INTEGER_RESOURCE_ID));
        LwM2mResource resource = (LwM2mResource) readResponse.getContent();
        assertEquals(expectedvalue, resource.getValue());
    }

    @Test
    public void can_write_float_resource_in_text() throws InterruptedException {
        write_float_resource(ContentFormat.TEXT);
    }

    @Test
    public void can_write_float_resource_in_tlv() throws InterruptedException {
        write_float_resource(ContentFormat.TLV);
    }

    @Test
    public void can_write_float_resource_in_json() throws InterruptedException {
        write_float_resource(ContentFormat.JSON);
    }

    private void write_float_resource(ContentFormat format) throws InterruptedException {
        // write resource
        final double expectedvalue = 999.99;
        WriteResponse response = helper.server.send(helper.getClient(),
                new WriteRequest(format, TEST_OBJECT_ID, 0, FLOAT_RESOURCE_ID, expectedvalue));

        // verify result
        assertEquals(ResponseCode.CHANGED, response.getCode());

        // read resource to check the value changed
        ReadResponse readResponse = helper.server.send(helper.getClient(),
                new ReadRequest(TEST_OBJECT_ID, 0, FLOAT_RESOURCE_ID));
        LwM2mResource resource = (LwM2mResource) readResponse.getContent();
        assertEquals(expectedvalue, resource.getValue());
    }

    @Test
    public void can_write_time_resource_in_text() throws InterruptedException {
        write_time_resource(ContentFormat.TEXT);
    }

    @Test
    public void can_write_time_resource_in_tlv() throws InterruptedException {
        write_time_resource(ContentFormat.TLV);
    }

    @Test
    public void can_write_time_resource_in_json() throws InterruptedException {
        write_time_resource(ContentFormat.JSON);
    }

    private void write_time_resource(ContentFormat format) throws InterruptedException {
        // write resource
        final Date expectedvalue = new Date(946681000l); // second accuracy
        WriteResponse response = helper.server.send(helper.getClient(),
                new WriteRequest(format, TEST_OBJECT_ID, 0, TIME_RESOURCE_ID, expectedvalue));

        // verify result
        assertEquals(ResponseCode.CHANGED, response.getCode());

        // read resource to check the value changed
        ReadResponse readResponse = helper.server.send(helper.getClient(),
                new ReadRequest(TEST_OBJECT_ID, 0, TIME_RESOURCE_ID));
        LwM2mResource resource = (LwM2mResource) readResponse.getContent();
        assertEquals(expectedvalue, resource.getValue());
    }

    @Test
    public void can_write_opaque_resource_in_opaque() throws InterruptedException {
        write_opaque_resource(ContentFormat.OPAQUE);
    }

    @Test
    public void can_write_opaque_resource_in_tlv() throws InterruptedException {
        write_opaque_resource(ContentFormat.TLV);
    }

    @Test
    public void can_write_opaque_resource_in_json() throws InterruptedException {
        write_opaque_resource(ContentFormat.JSON);
    }

    private void write_opaque_resource(ContentFormat format) throws InterruptedException {
        // write resource
        final byte[] expectedvalue = new byte[] { 1, 2, 3 };
        WriteResponse response = helper.server.send(helper.getClient(),
                new WriteRequest(format, TEST_OBJECT_ID, 0, OPAQUE_RESOURCE_ID, expectedvalue));

        // verify result
        assertEquals(ResponseCode.CHANGED, response.getCode());

        // read resource to check the value changed
        ReadResponse readResponse = helper.server.send(helper.getClient(),
                new ReadRequest(TEST_OBJECT_ID, 0, OPAQUE_RESOURCE_ID));
        LwM2mResource resource = (LwM2mResource) readResponse.getContent();
        assertArrayEquals(expectedvalue, (byte[]) resource.getValue());
    }

    @Test
    public void cannot_write_non_writable_resource() throws InterruptedException {
        // try to write unwritable resource like manufacturer on device
        final String manufacturer = "new manufacturer";
        WriteResponse response = helper.server.send(helper.getClient(), new WriteRequest(3, 0, 0, manufacturer));

        // verify result
        assertEquals(ResponseCode.METHOD_NOT_ALLOWED, response.getCode());
    }

    @Test
    public void cannot_write_security_resource() throws InterruptedException {
        // try to write unwritable resource like manufacturer on device
        final String uri = "new.dest.server";
        WriteResponse response = helper.server.send(helper.getClient(), new WriteRequest(0, 0, 0, uri));

        // verify result
        assertEquals(ResponseCode.NOT_FOUND, response.getCode());
    }

    @Test
    public void can_write_object_instance() throws InterruptedException {
        // write device timezone and offset
        LwM2mResource utcOffset = LwM2mSingleResource.newStringResource(14, "+02");
        LwM2mResource timeZone = LwM2mSingleResource.newStringResource(15, "Europe/Paris");
        WriteResponse response = helper.server.send(helper.getClient(),
                new WriteRequest(Mode.REPLACE, 3, 0, utcOffset, timeZone));

        // verify result
        assertEquals(ResponseCode.CHANGED, response.getCode());

        // read the timezone to check the value changed
        ReadResponse readResponse = helper.server.send(helper.getClient(), new ReadRequest(3, 0));
        LwM2mObjectInstance instance = (LwM2mObjectInstance) readResponse.getContent();
        assertEquals(utcOffset, instance.getResource(14));
        assertEquals(timeZone, instance.getResource(15));
    }

    @Test
    public void can_write_replacing_object_instance() throws InterruptedException {
        // setup server object
        WriteResponse response = helper.server.send(helper.getClient(), new WriteRequest(1, 0, 3, 60));

        // verify result
        assertEquals(ResponseCode.CHANGED, response.getCode());

        // write server object
        LwM2mResource lifetime = LwM2mSingleResource.newIntegerResource(1, 120);
        LwM2mResource defaultMinPeriod = LwM2mSingleResource.newIntegerResource(2, 10);
        LwM2mResource notificationStoring = LwM2mSingleResource.newBooleanResource(6, false);
        LwM2mResource binding = LwM2mSingleResource.newStringResource(7, "U");
        response = helper.server.send(helper.getClient(),
                new WriteRequest(Mode.REPLACE, 1, 0, lifetime, defaultMinPeriod, notificationStoring, binding));

        // verify result
        assertEquals(ResponseCode.CHANGED, response.getCode());

        // read the values to check the value changed
        ReadResponse readResponse = helper.server.send(helper.getClient(), new ReadRequest(1, 0));
        LwM2mObjectInstance instance = (LwM2mObjectInstance) readResponse.getContent();
        assertEquals(lifetime, instance.getResource(1));
        assertEquals(defaultMinPeriod, instance.getResource(2));
        assertEquals(notificationStoring, instance.getResource(6));
        assertEquals(binding, instance.getResource(7));
        assertNull(instance.getResource(3)); // removed not contained optional writable resource
    }

    @Test
    public void cannot_write_replacing_incomplete_object_instance() throws InterruptedException {
        // write server object
        LwM2mResource lifetime = LwM2mSingleResource.newIntegerResource(1, 120);
        LwM2mResource defaultMinPeriod = LwM2mSingleResource.newIntegerResource(2, 10);
        WriteResponse response = helper.server.send(helper.getClient(),
                new WriteRequest(Mode.REPLACE, 1, 0, lifetime, defaultMinPeriod));

        // verify result
        assertEquals(ResponseCode.BAD_REQUEST, response.getCode());
    }

    @Test
    public void can_write_updating_object_instance() throws InterruptedException {
        // setup server object
        WriteResponse response = helper.server.send(helper.getClient(), new WriteRequest(1, 0, 3, 60));

        // verify result
        assertEquals(ResponseCode.CHANGED, response.getCode());
        // write server object
        LwM2mResource lifetime = LwM2mSingleResource.newIntegerResource(1, 120);
        LwM2mResource defaultMinPeriod = LwM2mSingleResource.newIntegerResource(2, 10);
        response = helper.server.send(helper.getClient(),
                new WriteRequest(Mode.UPDATE, 1, 0, lifetime, defaultMinPeriod));

        // verify result
        assertEquals(ResponseCode.CHANGED, response.getCode());

        // read the values to check the value changed
        ReadResponse readResponse = helper.server.send(helper.getClient(), new ReadRequest(1, 0));
        LwM2mObjectInstance instance = (LwM2mObjectInstance) readResponse.getContent();
        assertEquals(lifetime, instance.getResource(1));
        assertEquals(defaultMinPeriod, instance.getResource(2));
        // no resources are removed when updating
        assertNotNull(instance.getResource(3));
        assertNotNull(instance.getResource(6));
        assertNotNull(instance.getResource(7));
    }

    @Test
    public void can_write_object_instance_in_json() throws InterruptedException {
        // write device timezone and offset
        LwM2mResource utcOffset = LwM2mSingleResource.newStringResource(14, "+02");
        LwM2mResource timeZone = LwM2mSingleResource.newStringResource(15, "Europe/Paris");
        WriteResponse response = helper.server.send(helper.getClient(),
                new WriteRequest(Mode.REPLACE, ContentFormat.JSON, 3, 0, utcOffset, timeZone));

        // verify result
        assertEquals(ResponseCode.CHANGED, response.getCode());

        // read the timezone to check the value changed
        ReadResponse readResponse = helper.server.send(helper.getClient(), new ReadRequest(3, 0));
        LwM2mObjectInstance instance = (LwM2mObjectInstance) readResponse.getContent();
        assertEquals(utcOffset, instance.getResource(14));
        assertEquals(timeZone, instance.getResource(15));
    }
}
