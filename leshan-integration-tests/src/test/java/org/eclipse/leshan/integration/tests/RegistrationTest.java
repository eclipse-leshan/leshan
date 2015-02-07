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

import static com.jayway.awaitility.Awaitility.await;
import static org.eclipse.leshan.integration.tests.IntegrationTestHelper.ENDPOINT_IDENTIFIER;
import static org.junit.Assert.*;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.leshan.client.californium.LeshanClient;
import org.eclipse.leshan.client.request.AbstractLwM2mClientRequest;
import org.eclipse.leshan.client.request.RegisterRequest;
import org.eclipse.leshan.client.request.identifier.ClientIdentifier;
import org.eclipse.leshan.client.resource.LwM2mClientObjectDefinition;
import org.eclipse.leshan.client.response.OperationResponse;
import org.eclipse.leshan.client.util.ResponseCallback;
import org.eclipse.leshan.server.client.Client;
import org.junit.After;
import org.junit.Test;

public class RegistrationTest {

    private final IntegrationTestHelper helper = new IntegrationTestHelper();

    @After
    public void stop() {
        helper.stop();
    }

    @Test
    public void registered_device_exists() {
        final OperationResponse register = helper.register();

        assertTrue(register.isSuccess());
        assertNotNull(helper.getClient());
    }

    @Test(expected = IllegalArgumentException.class)
    public void fail_to_create_client_with_null() {
        helper.client = new LeshanClient(helper.clientAddress, helper.serverAddress,
                (LwM2mClientObjectDefinition[]) null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void fail_to_create_client_with_same_object_twice() {
        final LwM2mClientObjectDefinition objectOne = new LwM2mClientObjectDefinition(1, false, false);
        helper.client = new LeshanClient(helper.clientAddress, helper.serverAddress, objectOne, objectOne);
    }

    @Test
    public void registered_device_exists_async() {
        final ResponseCallback callback = registerDeviceAsynch();

        assertTrue(callback.isSuccess());
        assertNotNull(helper.getClient());
    }

    @Test
    public void wont_send_synchronous_if_not_started() {
        final AbstractLwM2mClientRequest registerRequest = new RegisterRequest(ENDPOINT_IDENTIFIER,
                helper.clientParameters);

        final OperationResponse response = helper.client.send(registerRequest);

        assertFalse(response.isSuccess());
    }

    @Test
    public void wont_send_asynchronous_if_not_started() {
        final AbstractLwM2mClientRequest registerRequest = new RegisterRequest(ENDPOINT_IDENTIFIER,
                helper.clientParameters);

        final ResponseCallback callback = new ResponseCallback();
        helper.client.send(registerRequest, callback);

        assertTrue(callback.isCalled().get());
        assertFalse(callback.isSuccess());
    }

    @Test
    public void registered_device_updated() {
        final OperationResponse register = helper.register();

        final ClientIdentifier clientIdentifier = register.getClientIdentifier();

        final Map<String, String> updatedParameters = new HashMap<>();
        final int updatedLifetime = 1337;
        updatedParameters.put("lt", Integer.toString(updatedLifetime));

        final OperationResponse update = helper.update(clientIdentifier, updatedParameters);
        final Client client = helper.getClient();

        assertTrue(update.isSuccess());
        assertEquals(updatedLifetime, client.getLifeTimeInSec());
        assertNotNull(client);
    }

    @Test
    public void deregister_registered_device_then_reregister_async() {
        ResponseCallback registerCallback = registerDeviceAsynch();

        final ClientIdentifier clientIdentifier = registerCallback.getResponse().getClientIdentifier();

        final ResponseCallback deregisterCallback = new ResponseCallback();

        helper.deregister(clientIdentifier, deregisterCallback);

        await().untilTrue(deregisterCallback.isCalled());

        assertTrue(deregisterCallback.isSuccess());
        assertNull(helper.getClient());

        registerCallback = registerDeviceAsynch();

        assertTrue(registerCallback.isSuccess());
        assertNotNull(helper.getClient());
    }

    private ResponseCallback registerDeviceAsynch() {
        final ResponseCallback registerCallback = new ResponseCallback();

        helper.register(registerCallback);

        await().untilTrue(registerCallback.isCalled());

        return registerCallback;
    }

}
