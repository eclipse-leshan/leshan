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
package org.eclipse.leshan.integration.tests.read;

import static org.eclipse.leshan.core.ResponseCode.METHOD_NOT_ALLOWED;
import static org.eclipse.leshan.core.ResponseCode.NOT_FOUND;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.eclipse.californium.core.coap.Response;
import org.eclipse.leshan.core.request.ReadRequest;
import org.eclipse.leshan.core.response.ReadResponse;
import org.eclipse.leshan.integration.tests.util.IntegrationTestHelper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ReadFailedTest {
    public IntegrationTestHelper helper = new IntegrationTestHelper();

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
    public void cannot_read_non_readable_resource() throws InterruptedException {
        // read device reboot resource
        ReadResponse response = helper.server.send(helper.getCurrentRegistration(), new ReadRequest(3, 0, 4));

        // verify result
        assertEquals(METHOD_NOT_ALLOWED, response.getCode());
        assertNotNull(response.getCoapResponse());
        assertThat(response.getCoapResponse(), is(instanceOf(Response.class)));
    }

    @Test
    public void cannot_read_non_existent_object() throws InterruptedException {
        // read object "50"
        ReadResponse response = helper.server.send(helper.getCurrentRegistration(), new ReadRequest(50));

        // verify result
        assertEquals(NOT_FOUND, response.getCode());
        assertNotNull(response.getCoapResponse());
        assertThat(response.getCoapResponse(), is(instanceOf(Response.class)));
    }

    @Test
    public void cannot_read_non_existent_instance() throws InterruptedException {
        // read 2nd Device resource
        ReadResponse response = helper.server.send(helper.getCurrentRegistration(), new ReadRequest(3, 1));

        // verify result
        assertEquals(NOT_FOUND, response.getCode());
        assertNotNull(response.getCoapResponse());
        assertThat(response.getCoapResponse(), is(instanceOf(Response.class)));
    }

    @Test
    public void cannot_read_non_existent_resource() throws InterruptedException {
        // read device 50 resource
        ReadResponse response = helper.server.send(helper.getCurrentRegistration(), new ReadRequest(3, 0, 50));

        // verify result
        assertEquals(NOT_FOUND, response.getCode());
        assertNotNull(response.getCoapResponse());
        assertThat(response.getCoapResponse(), is(instanceOf(Response.class)));
    }

    @Test
    public void cannot_read_security_resource() throws InterruptedException {
        // read device 50 resource
        ReadResponse response = helper.server.send(helper.getCurrentRegistration(), new ReadRequest(0, 0, 0));

        // verify result
        assertEquals(NOT_FOUND, response.getCode());
        assertNotNull(response.getCoapResponse());
        assertThat(response.getCoapResponse(), is(instanceOf(Response.class)));
    }
}
