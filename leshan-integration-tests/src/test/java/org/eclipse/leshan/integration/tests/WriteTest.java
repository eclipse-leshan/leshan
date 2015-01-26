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
import org.eclipse.leshan.core.node.Value;
import org.eclipse.leshan.core.request.ReadRequest;
import org.eclipse.leshan.core.request.RegisterRequest;
import org.eclipse.leshan.core.request.WriteRequest;
import org.eclipse.leshan.core.response.LwM2mResponse;
import org.eclipse.leshan.core.response.ValueResponse;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class WriteTest {
    private IntegrationTestHelper helper = new IntegrationTestHelper();

    @Before
    public void start() {
        helper.start();
    }

    @After
    public void stop() {
        helper.stop();
    }

    @Test
    public void can_write_replace_to_resource() {
        // client registration
        helper.client.send(new RegisterRequest(ENDPOINT_IDENTIFIER));

        // write device timezone
        final String timeZone = "Europe/Paris";
        LwM2mResource newValue = new LwM2mResource(15, Value.newStringValue(timeZone));
        LwM2mResponse response = helper.server.send(helper.getClient(),
                new WriteRequest(3, 0, 15, newValue, null, true));

        // verify result
        assertEquals(ResponseCode.CHANGED, response.getCode());

        // read the timezone to check the value changed
        ValueResponse readResponse = helper.server.send(helper.getClient(), new ReadRequest(3, 0, 15));
        LwM2mResource resource = (LwM2mResource) readResponse.getContent();
        assertEquals(timeZone, resource.getValue().value);
    }

    @Test
    public void cannot_write_to_non_writable_resource() {
        // client registration
        helper.client.send(new RegisterRequest(ENDPOINT_IDENTIFIER));

        // try to write unwritable resource like manufacturer on device
        final String manufacturer = "new manufacturer";
        LwM2mResource newValue = new LwM2mResource(15, Value.newStringValue(manufacturer));
        LwM2mResponse response = helper.server
                .send(helper.getClient(), new WriteRequest(3, 0, 0, newValue, null, true));

        // verify result
        assertEquals(ResponseCode.METHOD_NOT_ALLOWED, response.getCode());
    }

    @Test
    public void can_write_to_writable_multiple_resource() {
        // client registration
        helper.client.send(new RegisterRequest(ENDPOINT_IDENTIFIER));

        // write device timezone and offset
        LwM2mResource utcOffset = new LwM2mResource(14, Value.newStringValue("+02"));
        LwM2mResource timeZone = new LwM2mResource(15, Value.newStringValue("Europe/Paris"));
        LwM2mObjectInstance newValue = new LwM2mObjectInstance(0, new LwM2mResource[] { utcOffset, timeZone });
        LwM2mResponse response = helper.server.send(helper.getClient(), new WriteRequest("/3/0", newValue, null, true));

        // verify result
        assertEquals(ResponseCode.CHANGED, response.getCode());

        // read the timezone to check the value changed
        ValueResponse readResponse = helper.server.send(helper.getClient(), new ReadRequest(3, 0));
        LwM2mObjectInstance instance = (LwM2mObjectInstance) readResponse.getContent();
        assertEquals(utcOffset, instance.getResources().get(14));
        assertEquals(timeZone, instance.getResources().get(15));
    }
}
