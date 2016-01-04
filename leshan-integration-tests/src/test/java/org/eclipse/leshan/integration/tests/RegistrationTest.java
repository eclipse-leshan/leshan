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
 *     Kai Hudalla (Bosch Software Innovations GmbH) - move LeshanClient constructor tests
 *                                                     to separate (unit) test class
 *******************************************************************************/

package org.eclipse.leshan.integration.tests;

import static org.eclipse.leshan.integration.tests.IntegrationTestHelper.ENDPOINT_IDENTIFIER;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.eclipse.leshan.LinkObject;
import org.eclipse.leshan.ResponseCode;
import org.eclipse.leshan.core.request.DeregisterRequest;
import org.eclipse.leshan.core.request.RegisterRequest;
import org.eclipse.leshan.core.request.UpdateRequest;
import org.eclipse.leshan.core.response.DeregisterResponse;
import org.eclipse.leshan.core.response.RegisterResponse;
import org.eclipse.leshan.core.response.UpdateResponse;
import org.eclipse.leshan.server.client.Client;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class RegistrationTest {

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
        helper.client.stop();
        helper.server.stop();
    }

    // TODO we must fix the API of registered response
    @Test
    public void registered_device_exists() {
        // check there are no client registered
        assertTrue(helper.server.getClientRegistry().allClients().isEmpty());

        // client registration
        RegisterResponse response = helper.client.send(new RegisterRequest(ENDPOINT_IDENTIFIER));

        // verify result
        assertEquals(ResponseCode.CREATED, response.getCode());
        assertEquals(1, helper.server.getClientRegistry().allClients().size());
        Client client = helper.server.getClientRegistry().get(ENDPOINT_IDENTIFIER);
        // TODO we must fix the API of registered response
        // assertEquals(response.getRegistrationID(), client.getRegistrationId());
        assertArrayEquals(LinkObject.parse("</>;rt=\"oma.lwm2m\",</2>,</3/0>".getBytes()), client.getObjectLinks());
    }

    @Test
    public void registered_device_exists_async() throws InterruptedException {
        final Callback<RegisterResponse> callback = new Callback<>();

        // client registration
        helper.client.send(new RegisterRequest(ENDPOINT_IDENTIFIER), callback, callback);

        // verify result
        callback.waitForResponse(2000);
        assertTrue(callback.isCalled().get());
        RegisterResponse response = callback.getResponse();
        assertEquals(ResponseCode.CREATED, response.getCode());
        assertEquals(1, helper.server.getClientRegistry().allClients().size());
        Client client = helper.server.getClientRegistry().get(ENDPOINT_IDENTIFIER);
        // TODO we must fix the API of registered response
        // assertEquals(response.getRegistrationID(), client.getRegistrationId());
        assertArrayEquals(LinkObject.parse("</>;rt=\"oma.lwm2m\",</2>,</3/0>".getBytes()), client.getObjectLinks());
    }

    @Test(expected = RuntimeException.class)
    public void wont_send_synchronous_if_not_started() {
        helper.client.stop();
        final RegisterRequest registerRequest = new RegisterRequest(ENDPOINT_IDENTIFIER);
        helper.client.send(registerRequest);
    }

    @Test(expected = RuntimeException.class)
    public void wont_send_asynchronous_if_not_started() {
        helper.client.stop();
        final RegisterRequest registerRequest = new RegisterRequest(ENDPOINT_IDENTIFIER);
        final Callback<RegisterResponse> callback = new Callback<RegisterResponse>();
        helper.client.send(registerRequest, callback, callback);
    }

    @Test
    public void registered_device_updated() {
        // client registration
        RegisterResponse response = helper.client.send(new RegisterRequest(ENDPOINT_IDENTIFIER));

        // do an update
        final Long updatedLifetime = 1337l;
        final UpdateResponse updateResponse = helper.client
                .send(new UpdateRequest(response.getRegistrationID(), updatedLifetime, null, null, null));

        // verify result
        final Client client = helper.server.getClientRegistry().get(ENDPOINT_IDENTIFIER);
        assertNotNull(client);
        assertEquals(ResponseCode.CHANGED, updateResponse.getCode());
        assertEquals(updatedLifetime, client.getLifeTimeInSec());
    }

    @Test
    public void registered_device_deregistered() {
        // client registration
        RegisterResponse response = helper.client.send(new RegisterRequest(ENDPOINT_IDENTIFIER));

        // do an update
        final DeregisterResponse deregisteredResponse = helper.client
                .send(new DeregisterRequest(response.getRegistrationID()));

        // verify result
        assertEquals(ResponseCode.DELETED, deregisteredResponse.getCode());
        assertTrue(helper.server.getClientRegistry().allClients().isEmpty());

    }
}
