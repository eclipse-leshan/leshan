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
 *     Achim Kraus (Bosch Software Innovations GmbH) - add test for deleting a resource
 *     Achim Kraus (Bosch Software Innovations GmbH) - add test for delete security object
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
import org.eclipse.leshan.core.request.CreateRequest;
import org.eclipse.leshan.core.request.DeleteRequest;
import org.eclipse.leshan.core.response.DeleteResponse;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class DeleteTest {

    private final IntegrationTestHelper helper = new IntegrationTestHelper();

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
    public void delete_created_object_instance() throws InterruptedException {
        // create ACL instance
        helper.server.send(
                helper.getCurrentRegistration(),
                new CreateRequest(2, new LwM2mObjectInstance(0, Arrays.asList(new LwM2mResource[] { LwM2mSingleResource
                        .newIntegerResource(0, 123) }))));

        // try to delete this instance
        DeleteResponse response = helper.server.send(helper.getCurrentRegistration(), new DeleteRequest(2, 0));

        // verify result
        assertEquals(ResponseCode.DELETED, response.getCode());
        assertNotNull(response.getCoapResponse());
        assertThat(response.getCoapResponse(), is(instanceOf(Response.class)));
    }

    @Test
    public void cannot_delete_resource_of_created_object_instance() throws InterruptedException {
        // create ACL instance
        helper.server.send(
                helper.getCurrentRegistration(),
                new CreateRequest(2, new LwM2mObjectInstance(0, Arrays.asList(new LwM2mResource[] { LwM2mSingleResource
                        .newIntegerResource(0, 123) }))));

        // try to delete this instance
        DeleteResponse response = helper.server.send(helper.getCurrentRegistration(), new DeleteRequest("/2/0/0"));

        // verify result
        assertEquals(ResponseCode.METHOD_NOT_ALLOWED, response.getCode());
        assertNotNull(response.getCoapResponse());
        assertThat(response.getCoapResponse(), is(instanceOf(Response.class)));
    }

    @Test
    public void cannot_delete_unknown_object_instance() throws InterruptedException {
        // try to create an instance of object 50
        DeleteResponse response = helper.server.send(helper.getCurrentRegistration(), new DeleteRequest(2, 0));

        // verify result
        assertEquals(ResponseCode.NOT_FOUND, response.getCode());
        assertNotNull(response.getCoapResponse());
        assertThat(response.getCoapResponse(), is(instanceOf(Response.class)));
    }

    @Test
    public void cannot_delete_single_manadatory_object_instance() throws InterruptedException {
        // try to create an instance of object 50
        DeleteResponse response = helper.server.send(helper.getCurrentRegistration(), new DeleteRequest(3, 0));

        // verify result
        assertEquals(ResponseCode.METHOD_NOT_ALLOWED, response.getCode());
        assertNotNull(response.getCoapResponse());
        assertThat(response.getCoapResponse(), is(instanceOf(Response.class)));
    }

    @Test
    public void cannot_delete_security_object_instance() throws InterruptedException {
        DeleteResponse response = helper.server.send(helper.getCurrentRegistration(), new DeleteRequest(0, 0));

        // verify result
        assertEquals(ResponseCode.NOT_FOUND, response.getCode());
        assertNotNull(response.getCoapResponse());
        assertThat(response.getCoapResponse(), is(instanceOf(Response.class)));
    }

}
