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
 *     Achim Kraus (Bosch Software Innovations GmbH) - add test for create security object
 *******************************************************************************/

package org.eclipse.leshan.integration.tests;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.junit.Assert.*;

import java.util.Arrays;

import org.eclipse.californium.core.coap.Response;
import org.eclipse.leshan.ResponseCode;
import org.eclipse.leshan.core.node.LwM2mObjectInstance;
import org.eclipse.leshan.core.node.LwM2mResource;
import org.eclipse.leshan.core.node.LwM2mSingleResource;
import org.eclipse.leshan.core.request.ContentFormat;
import org.eclipse.leshan.core.request.CreateRequest;
import org.eclipse.leshan.core.request.ReadRequest;
import org.eclipse.leshan.core.response.CreateResponse;
import org.eclipse.leshan.core.response.ReadResponse;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

public class CreateTest {

    IntegrationTestHelper helper = new IntegrationTestHelper();

    @Before
    public void start() {
        helper.initialize();
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
        helper.dispose();
    }

    @Test
    public void can_create_instance_of_object_without_instance_id() throws InterruptedException {
        // create ACL instance
        CreateResponse response = helper.server.send(helper.getCurrentRegistration(), new CreateRequest(2,
                new LwM2mResource[] { LwM2mSingleResource.newIntegerResource(0, 123) }));

        // verify result
        assertEquals(ResponseCode.CREATED, response.getCode());
        assertEquals("2/0", response.getLocation());
        assertNotNull(response.getCoapResponse());
        assertThat(response.getCoapResponse(), is(instanceOf(Response.class)));

        // create a second ACL instance
        response = helper.server.send(helper.getCurrentRegistration(), new CreateRequest(2,
                new LwM2mResource[] { LwM2mSingleResource.newIntegerResource(0, 123) }));

        // verify result
        assertEquals(ResponseCode.CREATED, response.getCode());
        assertEquals("2/1", response.getLocation());
        assertNotNull(response.getCoapResponse());
        assertThat(response.getCoapResponse(), is(instanceOf(Response.class)));

    }

    @Test
    public void can_create_specific_instance_of_object() throws InterruptedException {
        // create ACL instance
        LwM2mObjectInstance instance = new LwM2mObjectInstance(12,
                Arrays.<LwM2mResource> asList(LwM2mSingleResource.newIntegerResource(3, 123)));
        CreateResponse response = helper.server.send(helper.getCurrentRegistration(), new CreateRequest(2, instance));

        // verify result
        assertEquals(ResponseCode.CREATED, response.getCode());
        assertEquals("2/12", response.getLocation());
        assertNotNull(response.getCoapResponse());
        assertThat(response.getCoapResponse(), is(instanceOf(Response.class)));
    }

    @Test
    public void can_create_specific_instance_of_object_with_json() throws InterruptedException {
        // create ACL instance
        LwM2mObjectInstance instance = new LwM2mObjectInstance(12,
                Arrays.<LwM2mResource> asList(LwM2mSingleResource.newIntegerResource(3, 123)));
        CreateResponse response = helper.server.send(helper.getCurrentRegistration(),
                new CreateRequest(ContentFormat.JSON, 2, instance));

        // verify result
        assertEquals(ResponseCode.CREATED, response.getCode());
        assertEquals("2/12", response.getLocation());
        assertNotNull(response.getCoapResponse());
        assertThat(response.getCoapResponse(), is(instanceOf(Response.class)));
    }

    @Test
    public void cannot_create_instance_of_object() throws InterruptedException {
        // try to create an instance of object 50
        CreateResponse response = helper.server.send(helper.getCurrentRegistration(),
                new CreateRequest(50, new LwM2mResource[0]));

        // verify result
        assertEquals(ResponseCode.NOT_FOUND, response.getCode());
        assertNotNull(response.getCoapResponse());
        assertThat(response.getCoapResponse(), is(instanceOf(Response.class)));
    }

    // TODO not sure all the writable mandatory resource should be present
    // E.g. for softwareUpdate (object 9) packageURI and package are writable resource mandatory
    // but you will not make a create with this two resource.
    @Ignore
    @Test
    public void cannot_create_instance_without_all_required_resources() throws InterruptedException {
        // create ACL instance
        CreateResponse response = helper.server.send(helper.getCurrentRegistration(),
                new CreateRequest(2, new LwM2mResource[0]));

        // verify result
        assertEquals(ResponseCode.BAD_REQUEST, response.getCode());
        assertNotNull(response.getCoapResponse());
        assertThat(response.getCoapResponse(), is(instanceOf(Response.class)));

        // try to read to check if the instance is not created
        // client registration
        ReadResponse readResponse = helper.server.send(helper.getCurrentRegistration(), new ReadRequest(2, 0));
        assertEquals(ResponseCode.NOT_FOUND, readResponse.getCode());
        assertNotNull(response.getCoapResponse());
        assertThat(response.getCoapResponse(), is(instanceOf(Response.class)));
    }

    // TODO we must probably implement this.
    @Ignore
    @Test
    public void cannot_create_instance_with_extraneous_resources() throws InterruptedException {
        // create ACL instance
        LwM2mObjectInstance instance = new LwM2mObjectInstance(0, Arrays.<LwM2mResource> asList(
                LwM2mSingleResource.newIntegerResource(3, 123), LwM2mSingleResource.newIntegerResource(50, 123)));
        CreateRequest request = new CreateRequest(2, instance);
        CreateResponse response = helper.server.send(helper.getCurrentRegistration(), request);

        // verify result
        assertEquals(ResponseCode.BAD_REQUEST, response.getCode());
        assertNotNull(response.getCoapResponse());
        assertThat(response.getCoapResponse(), is(instanceOf(Response.class)));

        // try to read to check if the instance is not created
        // client registration
        ReadResponse readResponse = helper.server.send(helper.getCurrentRegistration(), new ReadRequest(2, 0));
        assertEquals(ResponseCode.NOT_FOUND, readResponse.getCode());
        assertNotNull(response.getCoapResponse());
        assertThat(response.getCoapResponse(), is(instanceOf(Response.class)));
    }

    // TODO I'm not sure we can do use only writable resource on create
    // see https://github.com/OpenMobileAlliance/OMA-LwM2M-Public-Review/issues/30
    @Ignore
    @Test
    public void cannot_create_instance_with_non_writable_resource() {
    }

    @Test
    public void cannot_create_mandatory_single_object() throws InterruptedException {
        // try to create another instance of device object
        CreateResponse response = helper.server.send(helper.getCurrentRegistration(),
                new CreateRequest(3, new LwM2mResource[] { LwM2mSingleResource.newIntegerResource(3, 123) }));

        // verify result
        assertEquals(ResponseCode.METHOD_NOT_ALLOWED, response.getCode());
        assertNotNull(response.getCoapResponse());
        assertThat(response.getCoapResponse(), is(instanceOf(Response.class)));
    }

    @Test
    public void cannot_create_instance_of_security_object() throws InterruptedException {
        CreateResponse response = helper.server.send(helper.getCurrentRegistration(),
                new CreateRequest(0, new LwM2mResource[] { LwM2mSingleResource.newStringResource(0, "new.dest") }));

        // verify result
        assertEquals(ResponseCode.NOT_FOUND, response.getCode());
        assertNotNull(response.getCoapResponse());
        assertThat(response.getCoapResponse(), is(instanceOf(Response.class)));
    }

}
