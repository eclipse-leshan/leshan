/*******************************************************************************
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
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
 *     Balasubramanian Azhagappan (Bosch Software Innovations GmbH)
 *                                  - initial API and implementation
 *******************************************************************************/
package org.eclipse.leshan.server.queue.impl;

import org.eclipse.leshan.LinkObject;
import org.eclipse.leshan.core.request.BindingMode;
import org.eclipse.leshan.server.client.Client;
import org.eclipse.leshan.server.impl.ClientRegistryImpl;
import org.junit.Before;
import org.junit.Test;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Created by Bala Azhagappan on 25.04.2016.
 */
public class ClientStatusTrackerTest {
    public static final String ENDPOINT = "urn:testEndpoint";
    private ClientStatusTracker instanceUnderTest;
    private ClientRegistryImpl registry;
    private Client client;

    @Before
    public void setup() throws  Exception {
        client = createExampleClient();
        registry = new ClientRegistryImpl();
        registry.registerClient(client);
        instanceUnderTest = new ClientStatusTracker(registry);
    }

    @Test
    public void testSetClientUnreachable() throws Exception {
        assertTrue(instanceUnderTest.setClientUnreachable(ENDPOINT));
        assertFalse("Expected client status set to unreachable but received ["
                + instanceUnderTest.isClientReachable(ENDPOINT)
                + "]" , instanceUnderTest.isClientReachable(ENDPOINT));
    }

    @Test
    public void testSetClientReachable() throws Exception {
        assertTrue(instanceUnderTest.setClientReachable(ENDPOINT));
        assertTrue("Expected client status set to reachable but received ["
                + instanceUnderTest.isClientReachable(ENDPOINT)
                + "]" , instanceUnderTest.isClientReachable(ENDPOINT));
    }

    @Test
    public void testIsClientReachable() throws Exception {
        instanceUnderTest.setClientUnreachable(ENDPOINT);
        assertFalse("Expected client status set to unreachable but received ["
                + instanceUnderTest.isClientReachable(ENDPOINT)
                + "]" , instanceUnderTest.isClientReachable(ENDPOINT));
    }

    @Test
    public void testClearClientState() throws Exception {
        instanceUnderTest.setClientUnreachable(ENDPOINT);
        instanceUnderTest.clearClientState(ENDPOINT);
        //Now when client state is not known, it is assummed to be reachable
        assertTrue("Assumed client status to be reachable when unknown but received ["
                + instanceUnderTest.isClientReachable(ENDPOINT)
                + "]" , instanceUnderTest.isClientReachable(ENDPOINT));
    }

    private Client createExampleClient() throws UnknownHostException {
        LinkObject[] objectLinks = LinkObject.parse("</3>".getBytes(org.eclipse.leshan.util.Charsets.UTF_8));

        Client.Builder builder = new Client.Builder("1234", ENDPOINT, InetAddress.getLocalHost(), 21345,
                InetSocketAddress.createUnresolved("localhost", 5683));

       return builder.lifeTimeInSec(10000L).bindingMode(BindingMode.UQ).objectLinks(objectLinks).build();
    }
}