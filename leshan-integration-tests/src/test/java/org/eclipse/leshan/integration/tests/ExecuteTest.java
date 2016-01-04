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
 *     Kiran Pradeep - add more test cases
 *******************************************************************************/

package org.eclipse.leshan.integration.tests;

import static org.eclipse.leshan.integration.tests.IntegrationTestHelper.ENDPOINT_IDENTIFIER;
import static org.junit.Assert.assertEquals;

import org.eclipse.leshan.ResponseCode;
import org.eclipse.leshan.core.request.ExecuteRequest;
import org.eclipse.leshan.core.request.RegisterRequest;
import org.eclipse.leshan.core.response.ExecuteResponse;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class ExecuteTest {

    private final IntegrationTestHelper helper = new IntegrationTestHelper();

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
    public void cannot_execute_read_only_resource() {
        // client registration
        helper.client.send(new RegisterRequest(ENDPOINT_IDENTIFIER));

        // execute manufacturer resource on device
        ExecuteResponse response = helper.server.send(helper.getClient(), new ExecuteRequest(3, 0, 0));

        // verify result
        assertEquals(ResponseCode.METHOD_NOT_ALLOWED, response.getCode());
    }

    @Test
    public void cannot_execute_read_write_resource() {
        // client registration
        helper.client.send(new RegisterRequest(ENDPOINT_IDENTIFIER));

        // execute current time resource on device
        ExecuteResponse response = helper.server.send(helper.getClient(), new ExecuteRequest(3, 0, 13));

        // verify result
        assertEquals(ResponseCode.METHOD_NOT_ALLOWED, response.getCode());
    }

    @Test
    public void cannot_execute_nonexisting_resource_on_existing_object() {
        // client registration
        helper.client.send(new RegisterRequest(ENDPOINT_IDENTIFIER));

        final int nonExistingResourceId = 9999;
        // execute non existing resource on device
        ExecuteResponse response = helper.server.send(helper.getClient(),
                new ExecuteRequest(3, 0, nonExistingResourceId));

        // verify result
        assertEquals(ResponseCode.NOT_FOUND, response.getCode());
    }

    @Test
    public void cannot_execute_nonexisting_resource_on_non_existing_object() {
        // client registration
        helper.client.send(new RegisterRequest(ENDPOINT_IDENTIFIER));

        final int nonExistingObjectId = 9999;
        ExecuteResponse response = helper.server.send(helper.getClient(),
                new ExecuteRequest(nonExistingObjectId, 0, 0));

        // verify result
        assertEquals(ResponseCode.NOT_FOUND, response.getCode());
    }

    @Test
    public void can_execute_resource() {
        // client registration
        helper.client.send(new RegisterRequest(ENDPOINT_IDENTIFIER));

        // execute reboot resource on device
        ExecuteResponse response = helper.server.send(helper.getClient(), new ExecuteRequest(3, 0, 4));

        // verify result
        assertEquals(ResponseCode.CHANGED, response.getCode());
    }

    @Test
    public void can_execute_resource_with_parameters() {
        // client registration
        helper.client.send(new RegisterRequest(ENDPOINT_IDENTIFIER));

        // execute reboot after 60 seconds on device
        ExecuteResponse response = helper.server.send(helper.getClient(), new ExecuteRequest(3, 0, 4, "60"));

        // verify result
        assertEquals(ResponseCode.CHANGED, response.getCode());
    }

}
