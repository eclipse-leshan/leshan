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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.eclipse.californium.core.coap.Response;
import org.eclipse.leshan.core.ResponseCode;
import org.eclipse.leshan.core.node.LwM2mResource;
import org.eclipse.leshan.core.node.LwM2mSingleResource;
import org.eclipse.leshan.core.request.WriteRequest;
import org.eclipse.leshan.core.request.WriteRequest.Mode;
import org.eclipse.leshan.core.response.WriteResponse;
import org.eclipse.leshan.integration.tests.util.IntegrationTestHelper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class WriteFailedTest {
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
    public void cannot_write_non_writable_resource() throws InterruptedException {
        // try to write unwritable resource like manufacturer on device
        String manufacturer = "new manufacturer";
        WriteResponse response = helper.server.send(helper.getCurrentRegistration(),
                new WriteRequest(3, 0, 0, manufacturer));

        // verify result
        assertEquals(ResponseCode.METHOD_NOT_ALLOWED, response.getCode());
        assertNotNull(response.getCoapResponse());
        assertThat(response.getCoapResponse(), is(instanceOf(Response.class)));
    }

    @Test
    public void cannot_write_security_resource() throws InterruptedException {
        // try to write unwritable resource like manufacturer on device
        String uri = "new.dest.server";
        WriteResponse response = helper.server.send(helper.getCurrentRegistration(), new WriteRequest(0, 0, 0, uri));

        // verify result
        assertEquals(ResponseCode.NOT_FOUND, response.getCode());
        assertNotNull(response.getCoapResponse());
        assertThat(response.getCoapResponse(), is(instanceOf(Response.class)));
    }

    @Test
    public void cannot_write_replacing_incomplete_object_instance() throws InterruptedException {
        // write server object
        LwM2mResource lifetime = LwM2mSingleResource.newIntegerResource(1, 120);
        LwM2mResource defaultMinPeriod = LwM2mSingleResource.newIntegerResource(2, 10);
        WriteResponse response = helper.server.send(helper.getCurrentRegistration(),
                new WriteRequest(Mode.REPLACE, 1, 0, lifetime, defaultMinPeriod));

        // verify result
        assertEquals(ResponseCode.BAD_REQUEST, response.getCode());
        assertNotNull(response.getCoapResponse());
        assertThat(response.getCoapResponse(), is(instanceOf(Response.class)));
    }
}
