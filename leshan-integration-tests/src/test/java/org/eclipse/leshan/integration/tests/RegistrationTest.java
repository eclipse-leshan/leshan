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
import static org.eclipse.leshan.integration.tests.IntegrationTestHelper.LIFETIME;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.californium.core.network.Endpoint;
import org.eclipse.leshan.LinkObject;
import org.eclipse.leshan.client.californium.LeshanClient;
import org.eclipse.leshan.client.californium.impl.CaliforniumLwM2mClientRequestSender;
import org.eclipse.leshan.client.resource.LwM2mInstanceEnabler;
import org.eclipse.leshan.client.resource.LwM2mObjectEnabler;
import org.eclipse.leshan.client.resource.ObjectEnabler;
import org.eclipse.leshan.core.request.ReadRequest;
import org.eclipse.leshan.core.request.RegisterRequest;
import org.eclipse.leshan.core.response.ReadResponse;
import org.eclipse.leshan.server.client.Client;
import org.eclipse.leshan.server.security.NonUniqueSecurityInfoException;
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

    }

    @After
    public void stop() {
        helper.server.stop();
    }

    @Test
    public void register_update_deregister() {
        // Check registration
        assertTrue(helper.server.getClientRegistry().allClients().isEmpty());

        helper.client.start();
        helper.waitForRegistration(1);

        assertEquals(1, helper.server.getClientRegistry().allClients().size());
        Client client = helper.server.getClientRegistry().get(ENDPOINT_IDENTIFIER);
        assertNotNull(client);
        // TODO </0/0> should not be part of the object links
        assertArrayEquals(LinkObject.parse("</>;rt=\"oma.lwm2m\",</1/0>,</2>,</3/0>,</2000/0>".getBytes()),
                client.getObjectLinks());

        // Check for update
        helper.waitForUpdate(LIFETIME);
        assertEquals(1, helper.server.getClientRegistry().allClients().size());

        // Check deregistration
        helper.client.stop(true);
        helper.waitForDeregistration(1);
        assertTrue(helper.server.getClientRegistry().allClients().isEmpty());
    }

    @Test
    public void deregister_cancel_multiple_pending_request() throws InterruptedException {
        // Check registration
        assertTrue(helper.server.getClientRegistry().allClients().isEmpty());

        helper.client.start();
        helper.waitForRegistration(1);

        assertEquals(1, helper.server.getClientRegistry().allClients().size());
        Client client = helper.server.getClientRegistry().get(ENDPOINT_IDENTIFIER);
        assertNotNull(client);
        assertArrayEquals(LinkObject.parse("</>;rt=\"oma.lwm2m\",</1/0>,</2>,</3/0>,</2000/0>".getBytes()),
                client.getObjectLinks());

        // Stop client with out de-registration
        helper.client.stop(false);

        // Send multiple reads which should be retransmitted.
        List<Callback<ReadResponse>> callbacks = new ArrayList<Callback<ReadResponse>>();

        for (int index = 0; index < 4; ++index) {
            Callback<ReadResponse> callback = new Callback<ReadResponse>();
            helper.server.send(client, new ReadRequest(3, 0, 1), callback, callback);
            callbacks.add(callback);
        }

        // Restart client (de-registration/re-registration)
        helper.client.start();

        // Check the request was cancelled.
        int index = 0;
        for (Callback<ReadResponse> callback : callbacks) {
            boolean timedout = !callback.waitForResponse(1000);
            assertFalse("Response or Error expected, no timeout, call " + index, timedout);
            assertTrue("Response or Error expected, call " + index, callback.isCalled().get());
            assertNull("No response expected, call " + index, callback.getResponse());
            assertNotNull("Exception expected, call " + index, callback.getException());
            ++index;
        }
    }

    @Test
    public void register_update_deregister_reregister() throws NonUniqueSecurityInfoException, InterruptedException {
        // Check registration
        assertTrue(helper.server.getClientRegistry().allClients().isEmpty());

        helper.client.start();
        helper.waitForRegistration(1);

        assertEquals(1, helper.server.getClientRegistry().allClients().size());
        assertNotNull(helper.server.getClientRegistry().get(ENDPOINT_IDENTIFIER));

        // Check for update
        helper.waitForUpdate(LIFETIME);
        assertEquals(1, helper.server.getClientRegistry().allClients().size());

        // Check de-registration
        helper.client.stop(true);
        helper.waitForDeregistration(1);
        assertTrue(helper.server.getClientRegistry().allClients().isEmpty());

        // Check new registration
        helper.resetLatch();
        helper.client.start();
        helper.waitForRegistration(1);
        assertEquals(1, helper.server.getClientRegistry().allClients().size());
        assertNotNull(helper.server.getClientRegistry().get(ENDPOINT_IDENTIFIER));
    }

    @Test
    public void register_update_reregister() throws NonUniqueSecurityInfoException, InterruptedException {
        // Check registration
        assertTrue(helper.server.getClientRegistry().allClients().isEmpty());

        helper.client.start();
        helper.waitForRegistration(1);

        assertEquals(1, helper.server.getClientRegistry().allClients().size());
        assertNotNull(helper.server.getClientRegistry().get(ENDPOINT_IDENTIFIER));

        // Check for update
        helper.waitForUpdate(LIFETIME);
        assertEquals(1, helper.server.getClientRegistry().allClients().size());

        // check stop do not de-register
        helper.client.stop(false);
        helper.waitForDeregistration(1);
        assertEquals(1, helper.server.getClientRegistry().allClients().size());

        // check new registration
        helper.resetLatch();
        helper.client.start();
        helper.waitForRegistration(1);
        assertEquals(1, helper.server.getClientRegistry().allClients().size());
        assertNotNull(helper.server.getClientRegistry().get(ENDPOINT_IDENTIFIER));
    }

    // TODO not really a registration test
    @Test(expected = IllegalArgumentException.class)
    public void fail_to_create_client_with_same_object_twice() {
        ObjectEnabler objectEnabler = new ObjectEnabler(1, null, new HashMap<Integer, LwM2mInstanceEnabler>(), null);
        ObjectEnabler objectEnabler2 = new ObjectEnabler(1, null, new HashMap<Integer, LwM2mInstanceEnabler>(), null);
        ArrayList<LwM2mObjectEnabler> objects = new ArrayList<>();
        objects.add(objectEnabler);
        objects.add(objectEnabler2);
        helper.client = new LeshanClient("test", new InetSocketAddress(InetAddress.getLoopbackAddress(), 0),
                new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), objects);
    }

    @Test
    public void register_with_additional_attributes() throws InterruptedException {
        // Check registration
        assertTrue(helper.server.getClientRegistry().allClients().isEmpty());

        // HACK to be able to send a Registration request with additional attributes
        LeshanClient lclient = (LeshanClient) helper.client;
        lclient.getCoapServer().start();
        Endpoint secureEndpoint = lclient.getCoapServer().getEndpoint(lclient.getSecureAddress());
        Endpoint nonSecureEndpoint = lclient.getCoapServer().getEndpoint(lclient.getNonSecureAddress());
        CaliforniumLwM2mClientRequestSender sender = new CaliforniumLwM2mClientRequestSender(secureEndpoint,
                nonSecureEndpoint);

        // Create Request with additional attributes
        Map<String, String> additionalAttributes = new HashMap<>();
        additionalAttributes.put("key1", "value1");
        additionalAttributes.put("imei", "2136872368");
        LinkObject[] linkObjects = LinkObject.parse("</>;rt=\"oma.lwm2m\",</0/0>,</1/0>,</2>,</3/0>".getBytes());
        RegisterRequest registerRequest = new RegisterRequest(ENDPOINT_IDENTIFIER, null, null, null, null, linkObjects,
                additionalAttributes);

        // Send request
        sender.send(helper.server.getNonSecureAddress(), false, registerRequest, 5000l);
        helper.waitForRegistration(1);

        // Check we are registered with the expected attributes
        assertEquals(1, helper.server.getClientRegistry().allClients().size());
        Client client = helper.server.getClientRegistry().get(ENDPOINT_IDENTIFIER);
        assertNotNull(client);
        assertNotNull(helper.last_registration);
        assertEquals(additionalAttributes, helper.last_registration.getAdditionalRegistrationAttributes());
        // TODO </0/0> should not be part of the object links
        assertArrayEquals(LinkObject.parse("</>;rt=\"oma.lwm2m\",</0/0>,</1/0>,</2>,</3/0>".getBytes()),
                client.getObjectLinks());

        lclient.getCoapServer().stop();
    }
}
