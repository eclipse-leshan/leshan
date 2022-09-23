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
 *     Sierra Wireless - initial API and implementation
 *     Michał Wadowski (Orange) - Add Observe-Composite feature.
 *     Michał Wadowski (Orange) - Improved compliance with rfc6690.
 *******************************************************************************/
package org.eclipse.leshan.server.registration;

import static org.junit.Assert.assertEquals;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.EnumSet;

import org.eclipse.leshan.core.link.Link;
import org.eclipse.leshan.core.request.BindingMode;
import org.eclipse.leshan.core.request.Identity;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class InMemoryRegistrationStoreTest {

    private final String ep = "urn:endpoint";
    private final int port = 23452;
    private final Long lifetime = 10000L;
    private final String sms = "0171-32423545";
    private final EnumSet<BindingMode> binding = EnumSet.of(BindingMode.U, BindingMode.Q, BindingMode.S);
    private final Link[] objectLinks = new Link[] { new Link("/3") };
    private final String registrationId = "4711";

    RegistrationStore store;
    InetAddress address;
    Registration registration;

    @Before
    public void setUp() throws UnknownHostException {
        address = InetAddress.getLocalHost();
        store = new InMemoryRegistrationStore();
    }

    @Test
    public void update_registration_keeps_properties_unchanged() {
        givenASimpleRegistration(lifetime);
        store.addRegistration(registration);

        RegistrationUpdate update = new RegistrationUpdate(registrationId, Identity.unsecure(address, port), null, null,
                null, null, null);
        UpdatedRegistration updatedRegistration = store.updateRegistration(update);
        assertEquals(lifetime, updatedRegistration.getUpdatedRegistration().getLifeTimeInSec());
        Assert.assertSame(binding, updatedRegistration.getUpdatedRegistration().getBindingMode());
        assertEquals(sms, updatedRegistration.getUpdatedRegistration().getSmsNumber());

        assertEquals(registration, updatedRegistration.getPreviousRegistration());

        Registration reg = store.getRegistrationByEndpoint(ep);
        assertEquals(lifetime, reg.getLifeTimeInSec());
        Assert.assertSame(binding, reg.getBindingMode());
        assertEquals(sms, reg.getSmsNumber());
    }

    @Test
    public void client_registration_sets_time_to_live() {
        givenASimpleRegistration(lifetime);
        store.addRegistration(registration);
        Assert.assertTrue(registration.isAlive());
    }

    @Test
    public void update_registration_to_extend_time_to_live() {
        givenASimpleRegistration(0L);
        store.addRegistration(registration);
        Assert.assertFalse(registration.isAlive());

        RegistrationUpdate update = new RegistrationUpdate(registrationId, Identity.unsecure(address, port), lifetime,
                null, null, null, null);
        UpdatedRegistration updatedRegistration = store.updateRegistration(update);
        Assert.assertTrue(updatedRegistration.getUpdatedRegistration().isAlive());

        Registration reg = store.getRegistrationByEndpoint(ep);
        Assert.assertTrue(reg.isAlive());
    }

    private void givenASimpleRegistration(Long lifetime) {

        Registration.Builder builder = new Registration.Builder(registrationId, ep, Identity.unsecure(address, port));

        registration = builder.lifeTimeInSec(lifetime).smsNumber(sms).bindingMode(binding).objectLinks(objectLinks)
                .build();
    }
}
