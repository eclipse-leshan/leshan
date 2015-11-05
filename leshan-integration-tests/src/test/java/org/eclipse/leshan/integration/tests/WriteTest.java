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

import static org.eclipse.leshan.integration.tests.IntegrationTestHelper.ENDPOINT_IDENTIFIER;
import static org.junit.Assert.assertEquals;

import org.eclipse.leshan.ResponseCode;
import org.eclipse.leshan.core.node.LwM2mObjectInstance;
import org.eclipse.leshan.core.node.LwM2mResource;
import org.eclipse.leshan.core.node.LwM2mSingleResource;
import org.eclipse.leshan.core.request.ContentFormat;
import org.eclipse.leshan.core.request.ReadRequest;
import org.eclipse.leshan.core.request.RegisterRequest;
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
    }

    @After
    public void stop() {
        helper.server.stop();
        helper.client.stop();
    }

    @Test
    public void can_write_replace_resource() {
        // client registration
        helper.client.send(new RegisterRequest(ENDPOINT_IDENTIFIER));

        // write device timezone
        final String timeZone = "Europe/Paris";
        WriteResponse response = helper.server.send(helper.getClient(), new WriteRequest(Mode.REPLACE, 3, 0, 15,
                timeZone));

        // verify result
        assertEquals(ResponseCode.CHANGED, response.getCode());

        // read the timezone to check the value changed
        ReadResponse readResponse = helper.server.send(helper.getClient(), new ReadRequest(3, 0, 15));
        LwM2mResource resource = (LwM2mResource) readResponse.getContent();
        assertEquals(timeZone, resource.getValue());
    }

    @Test
    public void can_write_replace_resource_in_json() {
        // client registration
        helper.client.send(new RegisterRequest(ENDPOINT_IDENTIFIER));

        // write device timezone
        final String timeZone = "Europe/Paris";
        WriteResponse response = helper.server.send(helper.getClient(), new WriteRequest(Mode.REPLACE, 3, 0, 15,
                timeZone));

        // verify result
        assertEquals(ResponseCode.CHANGED, response.getCode());

        // read the timezone to check the value changed
        ReadResponse readResponse = helper.server.send(helper.getClient(), new ReadRequest(3, 0, 15));
        LwM2mResource resource = (LwM2mResource) readResponse.getContent();
        assertEquals(timeZone, resource.getValue());
    }

    @Test
    public void cannot_write_non_writable_resource() {
        // client registration
        helper.client.send(new RegisterRequest(ENDPOINT_IDENTIFIER));

        // try to write unwritable resource like manufacturer on device
        final String manufacturer = "new manufacturer";
        WriteResponse response = helper.server.send(helper.getClient(), new WriteRequest(Mode.REPLACE, 3, 0, 0,
                manufacturer));

        // verify result
        assertEquals(ResponseCode.METHOD_NOT_ALLOWED, response.getCode());
    }

    @Test
    public void can_write_object_instance() {
        // client registration
        helper.client.send(new RegisterRequest(ENDPOINT_IDENTIFIER));

        // write device timezone and offset
        LwM2mResource utcOffset = LwM2mSingleResource.newStringResource(14, "+02");
        LwM2mResource timeZone = LwM2mSingleResource.newStringResource(15, "Europe/Paris");
        WriteResponse response = helper.server.send(helper.getClient(), new WriteRequest(Mode.REPLACE, 3, 0, utcOffset,
                timeZone));

        // verify result
        assertEquals(ResponseCode.CHANGED, response.getCode());

        // read the timezone to check the value changed
        ReadResponse readResponse = helper.server.send(helper.getClient(), new ReadRequest(3, 0));
        LwM2mObjectInstance instance = (LwM2mObjectInstance) readResponse.getContent();
        assertEquals(utcOffset, instance.getResource(14));
        assertEquals(timeZone, instance.getResource(15));
    }

    @Test
    public void can_write_object_instance_in_json() {
        // client registration
        helper.client.send(new RegisterRequest(ENDPOINT_IDENTIFIER));

        // write device timezone and offset
        LwM2mResource utcOffset = LwM2mSingleResource.newStringResource(14, "+02");
        LwM2mResource timeZone = LwM2mSingleResource.newStringResource(15, "Europe/Paris");
        WriteResponse response = helper.server.send(helper.getClient(), new WriteRequest(Mode.REPLACE,
                ContentFormat.JSON, 3, 0, utcOffset, timeZone));

        // verify result
        assertEquals(ResponseCode.CHANGED, response.getCode());

        // read the timezone to check the value changed
        ReadResponse readResponse = helper.server.send(helper.getClient(), new ReadRequest(3, 0));
        LwM2mObjectInstance instance = (LwM2mObjectInstance) readResponse.getContent();
        assertEquals(utcOffset, instance.getResource(14));
        assertEquals(timeZone, instance.getResource(15));
    }
}
