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
 *******************************************************************************/

package org.eclipse.leshan.integration.tests;

import static org.eclipse.leshan.ResponseCode.CONTENT;
import static org.eclipse.leshan.ResponseCode.METHOD_NOT_ALLOWED;
import static org.eclipse.leshan.ResponseCode.NOT_FOUND;
import static org.eclipse.leshan.integration.tests.IntegrationTestHelper.ENDPOINT_IDENTIFIER;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.eclipse.leshan.core.node.LwM2mObject;
import org.eclipse.leshan.core.node.LwM2mObjectInstance;
import org.eclipse.leshan.core.node.LwM2mResource;
import org.eclipse.leshan.core.request.ReadRequest;
import org.eclipse.leshan.core.request.RegisterRequest;
import org.eclipse.leshan.core.response.ReadResponse;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

public class ReadTest {

    public IntegrationTestHelper helper = new IntegrationTestHelper();

    @Before
    public void start() {
        helper.createServer();
        helper.server.start();
        helper.createClient();
        helper.client.start();
    }

    @After
    public void stop() {
        helper.server.stop();
        helper.client.stop();
    }

    // TODO we must the object TLV encoding
    @Ignore
    @Test
    public void can_read_empty_object() {
        // client registration
        helper.client.send(new RegisterRequest(ENDPOINT_IDENTIFIER));

        // read ACL object
        ReadResponse response = helper.server.send(helper.getClient(), new ReadRequest(2));

        // verify result
        assertEquals(CONTENT, response.getCode());

        LwM2mObject object = (LwM2mObject) response.getContent();
        assertEquals(2, object.getId());
        assertTrue(object.getInstances().isEmpty());
    }

    // TODO we must the object TLV encoding
    @Ignore
    @Test
    public void can_read_object() {
        // client registration
        helper.client.send(new RegisterRequest(ENDPOINT_IDENTIFIER));

        // read device object
        ReadResponse response = helper.server.send(helper.getClient(), new ReadRequest(3));

        // verify result
        assertEquals(CONTENT, response.getCode());

        LwM2mObject object = (LwM2mObject) response.getContent();
        assertEquals(3, object.getId());

        LwM2mObjectInstance instance = (LwM2mObjectInstance) object.getInstance(0);
        assertEquals(0, instance.getId());
    }

    @Test
    public void can_read_object_instance() {
        // client registration
        helper.client.send(new RegisterRequest(ENDPOINT_IDENTIFIER));

        // read device single instance
        ReadResponse response = helper.server.send(helper.getClient(), new ReadRequest(3, 0));

        // verify result
        assertEquals(CONTENT, response.getCode());

        LwM2mObjectInstance instance = (LwM2mObjectInstance) response.getContent();
        assertEquals(0, instance.getId());
    }

    @Test
    public void can_read_resource() {
        // client registration
        helper.client.send(new RegisterRequest(ENDPOINT_IDENTIFIER));

        // read device model number
        ReadResponse response = helper.server.send(helper.getClient(), new ReadRequest(3, 0, 1));

        // verify result
        assertEquals(CONTENT, response.getCode());

        LwM2mResource resource = (LwM2mResource) response.getContent();
        assertEquals(1, resource.getId());
        assertEquals("Model Number", resource.getValue());
    }

    @Test
    public void cannot_read_non_readable_resource() {
        // client registration
        helper.client.send(new RegisterRequest(ENDPOINT_IDENTIFIER));

        // read device reboot resource
        ReadResponse response = helper.server.send(helper.getClient(), new ReadRequest(3, 0, 4));

        // verify result
        assertEquals(METHOD_NOT_ALLOWED, response.getCode());
    }

    @Test
    public void cannot_read_non_existent_object() {
        // client registration
        helper.client.send(new RegisterRequest(ENDPOINT_IDENTIFIER));

        // read object "50"
        ReadResponse response = helper.server.send(helper.getClient(), new ReadRequest(50));

        // verify result
        assertEquals(NOT_FOUND, response.getCode());
    }

    @Test
    public void cannot_read_non_existent_instance() {
        // client registration
        helper.client.send(new RegisterRequest(ENDPOINT_IDENTIFIER));

        // read 2nd Device resource
        ReadResponse response = helper.server.send(helper.getClient(), new ReadRequest(3, 1));

        // verify result
        assertEquals(NOT_FOUND, response.getCode());
    }

    @Test
    public void cannot_read_non_existent_resource() {
        // client registration
        helper.client.send(new RegisterRequest(ENDPOINT_IDENTIFIER));

        // read device 50 resource
        ReadResponse response = helper.server.send(helper.getClient(), new ReadRequest(3, 0, 50));

        // verify result
        assertEquals(NOT_FOUND, response.getCode());
    }
}
