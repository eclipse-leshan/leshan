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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.HashMap;

import org.eclipse.leshan.LinkObject;
import org.eclipse.leshan.client.californium.LeshanClient;
import org.eclipse.leshan.client.resource.LwM2mInstanceEnabler;
import org.eclipse.leshan.client.resource.LwM2mObjectEnabler;
import org.eclipse.leshan.client.resource.ObjectEnabler;
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
        assertArrayEquals(LinkObject.parse("</>;rt=\"oma.lwm2m\",</0/0>,</1/0>,</2>,</3/0>".getBytes()),
                client.getObjectLinks());

        // Check for update
        helper.waitForUpdate(LIFETIME);
        assertEquals(1, helper.server.getClientRegistry().allClients().size());

        // Check deregistration
        helper.client.stop();
        helper.waitForDeregistration(1);
        assertTrue(helper.server.getClientRegistry().allClients().isEmpty());
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
}
