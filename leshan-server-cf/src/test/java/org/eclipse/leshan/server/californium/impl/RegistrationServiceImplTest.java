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
package org.eclipse.leshan.server.californium.impl;

import java.net.InetAddress;
import java.net.InetSocketAddress;

import org.eclipse.leshan.LinkObject;
import org.eclipse.leshan.core.request.BindingMode;
import org.eclipse.leshan.server.client.Registration;
import org.eclipse.leshan.server.client.RegistrationUpdate;
import org.eclipse.leshan.server.impl.RegistrationServiceImpl;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class RegistrationServiceImplTest {

    RegistrationServiceImpl registrationService;
    String ep = "urn:endpoint";
    InetAddress address;
    int port = 23452;
    Long lifetime = 10000L;
    String sms = "0171-32423545";
    BindingMode binding = BindingMode.UQS;
    LinkObject[] objectLinks = LinkObject.parse("</3>".getBytes(org.eclipse.leshan.util.Charsets.UTF_8));
    String registrationId = "4711";
    Registration registration;

    @Before
    public void setUp() throws Exception {
        address = InetAddress.getLocalHost();
        registrationService = new RegistrationServiceImpl(new InMemoryRegistrationStore());
    }

    @Test
    public void update_registration_keeps_properties_unchanged() {
        givenASimpleRegistration(lifetime);
        registrationService.registerClient(registration);

        RegistrationUpdate update = new RegistrationUpdate(registrationId, address, port, null, null, null, null);
        Registration updatedRegistration = registrationService.updateRegistration(update);
        Assert.assertEquals(lifetime, updatedRegistration.getLifeTimeInSec());
        Assert.assertSame(binding, updatedRegistration.getBindingMode());
        Assert.assertEquals(sms, updatedRegistration.getSmsNumber());

        Registration reg = registrationService.getByEndpoint(ep);
        Assert.assertEquals(lifetime, reg.getLifeTimeInSec());
        Assert.assertSame(binding, reg.getBindingMode());
        Assert.assertEquals(sms, reg.getSmsNumber());
    }

    @Test
    public void client_registration_sets_time_to_live() {
        givenASimpleRegistration(lifetime);
        registrationService.registerClient(registration);
        Assert.assertTrue(registration.isAlive());
    }

    @Test
    public void update_registration_to_extend_time_to_live() {
        givenASimpleRegistration(0L);
        registrationService.registerClient(registration);
        Assert.assertFalse(registration.isAlive());

        RegistrationUpdate update = new RegistrationUpdate(registrationId, address, port, lifetime, null, null, null);
        Registration updatedRegistration = registrationService.updateRegistration(update);
        Assert.assertTrue(updatedRegistration.isAlive());

        Registration reg = registrationService.getByEndpoint(ep);
        Assert.assertTrue(reg.isAlive());
    }

    private void givenASimpleRegistration(Long lifetime) {

        Registration.Builder builder = new Registration.Builder(registrationId, ep, address, port,
                InetSocketAddress.createUnresolved("localhost", 5683));

        registration = builder.lifeTimeInSec(lifetime).smsNumber(sms).bindingMode(binding).objectLinks(objectLinks).build();
    }
}
