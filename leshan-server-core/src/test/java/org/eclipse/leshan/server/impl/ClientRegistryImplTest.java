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
 *     Sierra Wireless - initial API and implementation
 *******************************************************************************/
package org.eclipse.leshan.server.impl;

import java.net.InetAddress;
import java.net.InetSocketAddress;

import org.eclipse.leshan.LinkObject;
import org.eclipse.leshan.core.request.BindingMode;
import org.eclipse.leshan.server.client.Client;
import org.eclipse.leshan.server.client.ClientUpdate;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class ClientRegistryImplTest {

    ClientRegistryImpl registry;
    String ep = "urn:endpoint";
    InetAddress address;
    int port = 23452;
    Long lifetime = 10000L;
    String sms = "0171-32423545";
    BindingMode binding = BindingMode.UQS;
    LinkObject[] objectLinks = LinkObject.parse("</3>".getBytes(org.eclipse.leshan.util.Charsets.UTF_8));
    String registrationId = "4711";
    Client client;

    @Before
    public void setUp() throws Exception {
        address = InetAddress.getLocalHost();
        registry = new ClientRegistryImpl();
    }

    @Test
    public void update_registration_keeps_properties_unchanged() {
        givenASimpleClient(lifetime);
        registry.registerClient(client);

        ClientUpdate update = new ClientUpdate(registrationId, address, port, null, null, null, null);
        Client updatedClient = registry.updateClient(update);
        Assert.assertEquals(lifetime, updatedClient.getLifeTimeInSec());
        Assert.assertSame(binding, updatedClient.getBindingMode());
        Assert.assertEquals(sms, updatedClient.getSmsNumber());

        Client registeredClient = registry.get(ep);
        Assert.assertEquals(lifetime, registeredClient.getLifeTimeInSec());
        Assert.assertSame(binding, registeredClient.getBindingMode());
        Assert.assertEquals(sms, registeredClient.getSmsNumber());
    }

    @Test
    public void client_registration_sets_time_to_live() {
        givenASimpleClient(lifetime);
        registry.registerClient(client);
        Assert.assertTrue(client.isAlive());
    }

    @Test
    public void update_registration_to_extend_time_to_live() {
        givenASimpleClient(0L);
        registry.registerClient(client);
        Assert.assertFalse(client.isAlive());

        ClientUpdate update = new ClientUpdate(registrationId, address, port, lifetime, null, null, null);
        Client updatedClient = registry.updateClient(update);
        Assert.assertTrue(updatedClient.isAlive());

        Client registeredClient = registry.get(ep);
        Assert.assertTrue(registeredClient.isAlive());
    }

    private void givenASimpleClient(Long lifetime) {
        client = new Client(registrationId, ep, address, port, null, lifetime, sms, binding, objectLinks,
                InetSocketAddress.createUnresolved("localhost", 5683));
    }
}
