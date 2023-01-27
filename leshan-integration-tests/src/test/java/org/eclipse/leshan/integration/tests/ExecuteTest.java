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
 *     Kiran Pradeep - add more test cases
 *     Achim Kraus (Bosch Software Innovations GmbH) - add test for execute security object
 *     Achim Kraus (Bosch Software Innovations GmbH) - replace close() with destroy()
 *******************************************************************************/

package org.eclipse.leshan.integration.tests;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.eclipse.californium.core.coap.Response;
import org.eclipse.leshan.core.ResponseCode;
import org.eclipse.leshan.core.request.ExecuteRequest;
import org.eclipse.leshan.core.response.ExecuteResponse;
import org.eclipse.leshan.integration.tests.util.IntegrationTestHelper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ExecuteTest {

    private final IntegrationTestHelper helper = new IntegrationTestHelper();

    @BeforeEach
    public void start() {
        helper.initialize();
        helper.createServer();
        helper.server.start();
        helper.createClient();
        helper.client.start();
        helper.waitForRegistrationAtServerSide(1);
    }

    @AfterEach
    public void stop() {
        helper.client.destroy(false);
        helper.server.destroy();
        helper.dispose();
    }

    @Test
    public void cannot_execute_read_only_resource() throws InterruptedException {
        // execute manufacturer resource on device
        ExecuteResponse response = helper.server.send(helper.getCurrentRegistration(), new ExecuteRequest(3, 0, 0));

        // verify result
        assertEquals(ResponseCode.METHOD_NOT_ALLOWED, response.getCode());
        assertNotNull(response.getCoapResponse());
        assertThat(response.getCoapResponse(), is(instanceOf(Response.class)));
    }

    @Test
    public void cannot_execute_read_write_resource() throws InterruptedException {
        // execute current time resource on device
        ExecuteResponse response = helper.server.send(helper.getCurrentRegistration(), new ExecuteRequest(3, 0, 13));

        // verify result
        assertEquals(ResponseCode.METHOD_NOT_ALLOWED, response.getCode());
        assertNotNull(response.getCoapResponse());
        assertThat(response.getCoapResponse(), is(instanceOf(Response.class)));
    }

    @Test
    public void cannot_execute_nonexisting_resource_on_existing_object() throws InterruptedException {
        int nonExistingResourceId = 9999;
        // execute non existing resource on device
        ExecuteResponse response = helper.server.send(helper.getCurrentRegistration(),
                new ExecuteRequest(3, 0, nonExistingResourceId));

        // verify result
        assertEquals(ResponseCode.NOT_FOUND, response.getCode());
        assertNotNull(response.getCoapResponse());
        assertThat(response.getCoapResponse(), is(instanceOf(Response.class)));
    }

    @Test
    public void cannot_execute_nonexisting_resource_on_non_existing_object() throws InterruptedException {
        int nonExistingObjectId = 9999;
        ExecuteResponse response = helper.server.send(helper.getCurrentRegistration(),
                new ExecuteRequest(nonExistingObjectId, 0, 0));

        // verify result
        assertEquals(ResponseCode.NOT_FOUND, response.getCode());
        assertNotNull(response.getCoapResponse());
        assertThat(response.getCoapResponse(), is(instanceOf(Response.class)));
    }

    @Test
    public void cannot_execute_security_object() throws InterruptedException {
        ExecuteResponse response = helper.server.send(helper.getCurrentRegistration(), new ExecuteRequest(0, 0, 0));

        // verify result
        assertEquals(ResponseCode.NOT_FOUND, response.getCode());
        assertNotNull(response.getCoapResponse());
        assertThat(response.getCoapResponse(), is(instanceOf(Response.class)));
    }

    @Test
    public void can_execute_resource() throws InterruptedException {
        // execute reboot resource on device
        ExecuteResponse response = helper.server.send(helper.getCurrentRegistration(), new ExecuteRequest(3, 0, 4));

        // verify result
        assertEquals(ResponseCode.CHANGED, response.getCode());
        assertNotNull(response.getCoapResponse());
        assertThat(response.getCoapResponse(), is(instanceOf(Response.class)));
    }

    @Test
    public void can_execute_resource_with_parameters() throws InterruptedException {
        // execute reboot after 60 seconds on device
        ExecuteResponse response = helper.server.send(helper.getCurrentRegistration(),
                new ExecuteRequest(3, 0, 4, "6"));

        // verify result
        assertEquals(ResponseCode.CHANGED, response.getCode());
        assertNotNull(response.getCoapResponse());
        assertThat(response.getCoapResponse(), is(instanceOf(Response.class)));
    }
}
