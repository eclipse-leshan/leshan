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

import static org.eclipse.leshan.integration.tests.util.IntegrationTestHelper.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.Collection;

import org.eclipse.californium.core.coap.Response;
import org.eclipse.leshan.core.ResponseCode;
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
import org.eclipse.leshan.integration.tests.util.IntegrationTestHelper;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class CreateTest {
    protected IntegrationTestHelper helper = new IntegrationTestHelper();

    @Parameters(name = "{0}")
    public static Collection<?> contentFormats() {
        return Arrays.asList(new Object[][] { //
                                { ContentFormat.TLV }, //
                                { ContentFormat.JSON }, //
                                { ContentFormat.SENML_JSON }, //
                                { ContentFormat.SENML_CBOR } });
    }

    private ContentFormat contentFormat;

    public CreateTest(ContentFormat contentFormat) {
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
    public void can_create_instance_without_instance_id() throws InterruptedException {
        try {
            // create ACL instance
            CreateResponse response = helper.server.send(helper.getCurrentRegistration(),
                    new CreateRequest(contentFormat, 2, LwM2mSingleResource.newIntegerResource(3, 33)));

            // verify result
            assertEquals(ResponseCode.CREATED, response.getCode());
            assertEquals("2/0", response.getLocation());
            assertNotNull(response.getCoapResponse());
            assertThat(response.getCoapResponse(), is(instanceOf(Response.class)));

            // create a second ACL instance
            response = helper.server.send(helper.getCurrentRegistration(),
                    new CreateRequest(contentFormat, 2, LwM2mSingleResource.newIntegerResource(3, 34)));

            // verify result
            assertEquals(ResponseCode.CREATED, response.getCode());
            assertEquals("2/1", response.getLocation());
            assertNotNull(response.getCoapResponse());
            assertThat(response.getCoapResponse(), is(instanceOf(Response.class)));

            // read object 2
            ReadResponse readResponse = helper.server.send(helper.getCurrentRegistration(), new ReadRequest(2));
            assertEquals(ResponseCode.CONTENT, readResponse.getCode());
            LwM2mObject object = (LwM2mObject) readResponse.getContent();
            assertEquals(33l, object.getInstance(0).getResource(3).getValue());
            assertEquals(34l, object.getInstance(1).getResource(3).getValue());
        } catch (InvalidRequestException e) {
            // only TLV support create instance without instance id
            assertNotEquals(ContentFormat.TLV, contentFormat);
        }
    }

    @Test
    public void can_create_instance_with_id() throws InterruptedException {
        // create ACL instance
        LwM2mObjectInstance instance = new LwM2mObjectInstance(12, LwM2mSingleResource.newIntegerResource(3, 123));
        CreateResponse response = helper.server.send(helper.getCurrentRegistration(),
                new CreateRequest(contentFormat, 2, instance));

        // verify result
        assertEquals(ResponseCode.CREATED, response.getCode());
        assertEquals(null, response.getLocation());
        assertNotNull(response.getCoapResponse());
        assertThat(response.getCoapResponse(), is(instanceOf(Response.class)));

        // read object 2
        ReadResponse readResponse = helper.server.send(helper.getCurrentRegistration(), new ReadRequest(2));
        assertEquals(ResponseCode.CONTENT, readResponse.getCode());
        LwM2mObject object = (LwM2mObject) readResponse.getContent();
        assertEquals(object.getInstance(12).getResource(3).getValue(), 123l);
    }

    @Test
    public void can_create_2_instances_of_object() throws InterruptedException {
        // create ACL instance
        LwM2mObjectInstance instance1 = new LwM2mObjectInstance(12, LwM2mSingleResource.newIntegerResource(3, 123));
        LwM2mObjectInstance instance2 = new LwM2mObjectInstance(13, LwM2mSingleResource.newIntegerResource(3, 124));
        CreateResponse response = helper.server.send(helper.getCurrentRegistration(),
                new CreateRequest(contentFormat, 2, instance1, instance2));

        // verify result
        assertEquals(ResponseCode.CREATED, response.getCode());
        assertEquals(null, response.getLocation());
        assertNotNull(response.getCoapResponse());
        assertThat(response.getCoapResponse(), is(instanceOf(Response.class)));

        // read object 2
        ReadResponse readResponse = helper.server.send(helper.getCurrentRegistration(), new ReadRequest(2));
        assertEquals(ResponseCode.CONTENT, readResponse.getCode());
        LwM2mObject object = (LwM2mObject) readResponse.getContent();
        assertEquals(object.getInstance(12).getResource(3).getValue(), 123l);
        assertEquals(object.getInstance(13).getResource(3).getValue(), 124l);
    }

    @Test
    public void cannot_create_instance_without_all_required_resources() throws InterruptedException {
        // create ACL instance without any resources
        CreateResponse response = helper.server.send(helper.getCurrentRegistration(),
                new CreateRequest(contentFormat, TEST_OBJECT_ID, new LwM2mObjectInstance(0, new LwM2mResource[0])));

        // verify result
        assertEquals(ResponseCode.BAD_REQUEST, response.getCode());
        assertNotNull(response.getCoapResponse());
        assertThat(response.getCoapResponse(), is(instanceOf(Response.class)));

        // create ACL instance with only 1 mandatory resources (1 missing)
        CreateResponse response2 = helper.server.send(helper.getCurrentRegistration(), new CreateRequest(contentFormat,
                TEST_OBJECT_ID,
                new LwM2mObjectInstance(0, LwM2mSingleResource.newIntegerResource(INTEGER_MANDATORY_RESOURCE_ID, 12))));

        // verify result
        assertEquals(ResponseCode.BAD_REQUEST, response2.getCode());

        // create ACL instance
        LwM2mObjectInstance instance0 = new LwM2mObjectInstance(0,
                LwM2mSingleResource.newIntegerResource(INTEGER_MANDATORY_RESOURCE_ID, 22),
                LwM2mSingleResource.newStringResource(STRING_MANDATORY_RESOURCE_ID, "string"));
        LwM2mObjectInstance instance1 = new LwM2mObjectInstance(1,
                LwM2mSingleResource.newIntegerResource(INTEGER_MANDATORY_RESOURCE_ID, 22));

        CreateResponse response3 = helper.server.send(helper.getCurrentRegistration(),
                new CreateRequest(contentFormat, TEST_OBJECT_ID, instance0, instance1));

        // verify result
        assertEquals(ResponseCode.BAD_REQUEST, response3.getCode());

        // try to read to check if the instance is not created
        // client registration
        ReadResponse readResponse = helper.server.send(helper.getCurrentRegistration(), new ReadRequest(2, 0));
        assertEquals(ResponseCode.NOT_FOUND, readResponse.getCode());
        assertNotNull(response.getCoapResponse());
        assertThat(response.getCoapResponse(), is(instanceOf(Response.class)));
    }
}
