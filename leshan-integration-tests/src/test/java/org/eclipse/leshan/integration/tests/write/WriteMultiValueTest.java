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

import static org.eclipse.leshan.core.ResponseCode.CONTENT;
import static org.eclipse.leshan.integration.tests.util.TestUtil.assertContentFormat;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.californium.core.coap.Response;
import org.eclipse.leshan.core.ResponseCode;
import org.eclipse.leshan.core.model.ResourceModel.Type;
import org.eclipse.leshan.core.node.LwM2mMultipleResource;
import org.eclipse.leshan.core.node.LwM2mObjectInstance;
import org.eclipse.leshan.core.node.LwM2mResource;
import org.eclipse.leshan.core.node.LwM2mSingleResource;
import org.eclipse.leshan.core.node.ObjectLink;
import org.eclipse.leshan.core.request.ContentFormat;
import org.eclipse.leshan.core.request.ReadRequest;
import org.eclipse.leshan.core.request.WriteRequest;
import org.eclipse.leshan.core.request.WriteRequest.Mode;
import org.eclipse.leshan.core.response.ReadResponse;
import org.eclipse.leshan.core.response.WriteResponse;
import org.eclipse.leshan.core.util.TestLwM2mId;
import org.eclipse.leshan.integration.tests.util.IntegrationTestHelper;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class WriteMultiValueTest {
    protected IntegrationTestHelper helper = new IntegrationTestHelper();

    @Parameters(name = "{0}")
    public static Collection<?> contentFormats() {
        return Arrays.asList(new Object[][] { //
                { ContentFormat.TLV }, //
                { ContentFormat.fromCode(ContentFormat.OLD_TLV_CODE) }, //
                { ContentFormat.JSON }, //
                { ContentFormat.fromCode(ContentFormat.OLD_JSON_CODE) }, //
                { ContentFormat.SENML_JSON }, //
                { ContentFormat.SENML_CBOR } });
    }

    private ContentFormat contentFormat;

    public WriteMultiValueTest(ContentFormat contentFormat) {
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
    public void can_write_object_instance() throws InterruptedException {
        // write device timezone and offset
        LwM2mResource utcOffset = LwM2mSingleResource.newStringResource(14, "+02");
        LwM2mResource timeZone = LwM2mSingleResource.newStringResource(15, "Europe/Paris");
        WriteResponse response = helper.server.send(helper.getCurrentRegistration(),
                new WriteRequest(Mode.REPLACE, contentFormat, 3, 0, utcOffset, timeZone));

        // verify result
        assertEquals(ResponseCode.CHANGED, response.getCode());
        assertNotNull(response.getCoapResponse());
        assertThat(response.getCoapResponse(), is(instanceOf(Response.class)));

        // read the timezone to check the value changed
        ReadResponse readResponse = helper.server.send(helper.getCurrentRegistration(), new ReadRequest(3, 0));
        LwM2mObjectInstance instance = (LwM2mObjectInstance) readResponse.getContent();
        assertEquals(utcOffset, instance.getResource(14));
        assertEquals(timeZone, instance.getResource(15));
    }

    @Test
    public void can_write_replacing_object_instance() throws InterruptedException {
        // setup server object
        WriteResponse response = helper.server.send(helper.getCurrentRegistration(),
                new WriteRequest(contentFormat, 1, 0, 3, 60));

        // verify result
        assertEquals(ResponseCode.CHANGED, response.getCode());
        assertNotNull(response.getCoapResponse());
        assertThat(response.getCoapResponse(), is(instanceOf(Response.class)));

        // write server object
        LwM2mResource lifetime = LwM2mSingleResource.newIntegerResource(1, 120);
        LwM2mResource defaultMinPeriod = LwM2mSingleResource.newIntegerResource(2, 10);
        LwM2mResource notificationStoring = LwM2mSingleResource.newBooleanResource(6, false);
        LwM2mResource binding = LwM2mSingleResource.newStringResource(7, "U");
        response = helper.server.send(helper.getCurrentRegistration(), new WriteRequest(Mode.REPLACE, contentFormat, 1,
                0, lifetime, defaultMinPeriod, notificationStoring, binding));

        // verify result
        assertEquals(ResponseCode.CHANGED, response.getCode());
        assertNotNull(response.getCoapResponse());
        assertThat(response.getCoapResponse(), is(instanceOf(Response.class)));

        // read the values to check the value changed
        ReadResponse readResponse = helper.server.send(helper.getCurrentRegistration(), new ReadRequest(1, 0));
        LwM2mObjectInstance instance = (LwM2mObjectInstance) readResponse.getContent();
        assertEquals(lifetime, instance.getResource(1));
        assertEquals(defaultMinPeriod, instance.getResource(2));
        assertEquals(notificationStoring, instance.getResource(6));
        assertEquals(binding, instance.getResource(7));
        assertNull(instance.getResource(3)); // removed not contained optional writable resource
    }

    @Test
    public void can_write_updating_object_instance() throws InterruptedException {
        // setup server object
        WriteResponse response = helper.server.send(helper.getCurrentRegistration(),
                new WriteRequest(contentFormat, 1, 0, 3, 60));

        // verify result
        assertEquals(ResponseCode.CHANGED, response.getCode());
        assertNotNull(response.getCoapResponse());
        assertThat(response.getCoapResponse(), is(instanceOf(Response.class)));
        // write server object
        LwM2mResource lifetime = LwM2mSingleResource.newIntegerResource(1, 120);
        LwM2mResource defaultMinPeriod = LwM2mSingleResource.newIntegerResource(2, 10);
        response = helper.server.send(helper.getCurrentRegistration(),
                new WriteRequest(Mode.UPDATE, contentFormat, 1, 0, lifetime, defaultMinPeriod));

        // verify result
        assertEquals(ResponseCode.CHANGED, response.getCode());
        assertNotNull(response.getCoapResponse());
        assertThat(response.getCoapResponse(), is(instanceOf(Response.class)));

        // read the values to check the value changed
        ReadResponse readResponse = helper.server.send(helper.getCurrentRegistration(), new ReadRequest(1, 0));
        LwM2mObjectInstance instance = (LwM2mObjectInstance) readResponse.getContent();
        assertEquals(lifetime, instance.getResource(1));
        assertEquals(defaultMinPeriod, instance.getResource(2));
        // no resources are removed when updating
        assertNotNull(instance.getResource(3));
        assertNotNull(instance.getResource(6));
        assertNotNull(instance.getResource(7));
    }

    @Test
    public void can_write_multi_instance_objlnk_resource_in_tlv() throws InterruptedException {
        // object link not yet implemented for some content format.
        switch (contentFormat.getCode()) {
        case ContentFormat.JSON_CODE:
        case ContentFormat.OLD_JSON_CODE:
            return;
        }

        Map<Integer, ObjectLink> neighbourCellReport = new HashMap<>();
        neighbourCellReport.put(0, new ObjectLink(10245, 1));
        neighbourCellReport.put(1, new ObjectLink(10242, 2));
        neighbourCellReport.put(2, new ObjectLink(10244, 3));

        // Write objlnk resource in TLV format
        WriteResponse response = helper.server.send(helper.getCurrentRegistration(), new WriteRequest(contentFormat,
                TestLwM2mId.TEST_OBJECT, 0, TestLwM2mId.MULTIPLE_OBJLINK_VALUE, neighbourCellReport, Type.OBJLNK));

        // Verify Write result
        assertEquals(ResponseCode.CHANGED, response.getCode());
        assertNotNull(response.getCoapResponse());
        assertThat(response.getCoapResponse(), is(instanceOf(Response.class)));

        // Reading back the written OBJLNK value
        ReadResponse readResponse = helper.server.send(helper.getCurrentRegistration(),
                new ReadRequest(TestLwM2mId.TEST_OBJECT, 0, TestLwM2mId.MULTIPLE_OBJLINK_VALUE));
        LwM2mMultipleResource resource = (LwM2mMultipleResource) readResponse.getContent();

        // verify read value
        assertEquals(((ObjectLink) resource.getValue(0)).getObjectId(), 10245);
        assertEquals(((ObjectLink) resource.getValue(0)).getObjectInstanceId(), 1);
        assertEquals(((ObjectLink) resource.getValue(1)).getObjectId(), 10242);
        assertEquals(((ObjectLink) resource.getValue(1)).getObjectInstanceId(), 2);
        assertEquals(((ObjectLink) resource.getValue(2)).getObjectId(), 10244);
        assertEquals(((ObjectLink) resource.getValue(2)).getObjectInstanceId(), 3);
    }

    @Test
    public void can_write_object_resource_instance() throws InterruptedException {

        // --------------------------------
        // write (replace) multi instance
        // --------------------------------
        Map<Integer, String> values = new HashMap<>();
        values.put(10, "value10");
        values.put(20, "value20");

        WriteResponse response = helper.server.send(helper.getCurrentRegistration(), new WriteRequest(Mode.REPLACE,
                contentFormat, TestLwM2mId.TEST_OBJECT, 0, TestLwM2mId.MULTIPLE_STRING_VALUE, values, Type.STRING));

        // Verify Write result
        assertEquals(ResponseCode.CHANGED, response.getCode());
        assertNotNull(response.getCoapResponse());
        assertThat(response.getCoapResponse(), is(instanceOf(Response.class)));

        // read multi instance
        ReadResponse readResponse = helper.server.send(helper.getCurrentRegistration(),
                new ReadRequest(contentFormat, TestLwM2mId.TEST_OBJECT, 0, TestLwM2mId.MULTIPLE_STRING_VALUE));

        // verify result
        assertEquals(CONTENT, readResponse.getCode());
        assertContentFormat(contentFormat, readResponse);

        LwM2mMultipleResource resourceInstance = (LwM2mMultipleResource) readResponse.getContent();
        assertEquals("value10", resourceInstance.getInstance(10).getValue());
        assertEquals("value20", resourceInstance.getInstance(20).getValue());

        // --------------------------------
        // write (update) multi instance
        // --------------------------------
        Map<Integer, String> newValues = new HashMap<>();
        newValues.put(20, "value200");
        newValues.put(30, "value30");
        response = helper.server.send(helper.getCurrentRegistration(), new WriteRequest(Mode.UPDATE, contentFormat,
                TestLwM2mId.TEST_OBJECT, 0, TestLwM2mId.MULTIPLE_STRING_VALUE, newValues, Type.STRING));

        // Verify Write result
        assertEquals(ResponseCode.CHANGED, response.getCode());
        assertNotNull(response.getCoapResponse());
        assertThat(response.getCoapResponse(), is(instanceOf(Response.class)));

        // read multi instance
        readResponse = helper.server.send(helper.getCurrentRegistration(),
                new ReadRequest(contentFormat, TestLwM2mId.TEST_OBJECT, 0, TestLwM2mId.MULTIPLE_STRING_VALUE));

        // verify result
        assertEquals(CONTENT, readResponse.getCode());
        assertContentFormat(contentFormat, readResponse);

        resourceInstance = (LwM2mMultipleResource) readResponse.getContent();
        assertEquals("value200", resourceInstance.getInstance(20).getValue());
        assertEquals("value30", resourceInstance.getInstance(30).getValue());
        assertEquals("value10", resourceInstance.getInstance(10).getValue());

        // --------------------------------
        // write (replace) multi instance
        // --------------------------------
        newValues = new HashMap<>();
        newValues.put(1, "value1");
        response = helper.server.send(helper.getCurrentRegistration(), new WriteRequest(Mode.REPLACE, contentFormat,
                TestLwM2mId.TEST_OBJECT, 0, TestLwM2mId.MULTIPLE_STRING_VALUE, newValues, Type.STRING));

        // Verify Write result
        assertEquals(ResponseCode.CHANGED, response.getCode());
        assertNotNull(response.getCoapResponse());
        assertThat(response.getCoapResponse(), is(instanceOf(Response.class)));

        // read multi instance
        readResponse = helper.server.send(helper.getCurrentRegistration(),
                new ReadRequest(contentFormat, TestLwM2mId.TEST_OBJECT, 0, TestLwM2mId.MULTIPLE_STRING_VALUE));

        // verify result
        assertEquals(CONTENT, readResponse.getCode());
        assertContentFormat(contentFormat, readResponse);

        resourceInstance = (LwM2mMultipleResource) readResponse.getContent();
        assertEquals(1, resourceInstance.getInstances().size());
        assertEquals("value1", resourceInstance.getInstance(1).getValue());
    }
}
