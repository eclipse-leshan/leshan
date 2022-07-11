/*******************************************************************************
 * Copyright (c) 2017 Bosch Software Innovations GmbH and others.
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
 *     Bosch Software Innovations GmbH - initial API
 *******************************************************************************/
package org.eclipse.leshan.server.queue;

import static org.junit.Assert.*;

import java.net.Inet4Address;
import java.net.UnknownHostException;
import java.util.EnumSet;

import org.eclipse.leshan.core.request.BindingMode;
import org.eclipse.leshan.core.request.Identity;
import org.eclipse.leshan.server.registration.Registration;
import org.junit.Test;

/**
 * tests the implementation of {@link PresenceService}
 *
 */
public class PresenceServiceTest {
    private ClientAwakeTimeProvider awakeTimeProvider = new StaticClientAwakeTimeProvider();
    private PresenceServiceImpl presenceService = new PresenceServiceImpl(awakeTimeProvider);

    @Test
    public void testSetOnlineForNonQueueMode() throws Exception {
        Registration registration = givenASimpleClient();
        presenceService.addListener(new PresenceListener() {

            @Override
            public void onAwake(Registration registration) {
                fail("No invocation was expected");
            }

            @Override
            public void onSleeping(Registration registration) {
                fail("No invocation was expected");
            }
        });
        presenceService.setAwake(registration);
    }

    @Test
    public void testIsOnline() throws Exception {
        Registration queueModeRegistration = givenASimpleClientWithQueueMode();

        assertTrue(presenceService.isClientAwake(queueModeRegistration));
        presenceService.setSleeping(queueModeRegistration);
        assertFalse(presenceService.isClientAwake(queueModeRegistration));
    }

    private Registration givenASimpleClient() throws UnknownHostException {
        Registration.Builder builder = new Registration.Builder("ID", "urn:client",
                Identity.unsecure(Inet4Address.getLoopbackAddress(), 12354));

        Registration reg = builder.build();
        presenceService.setAwake(reg);
        return reg;
    }

    private Registration givenASimpleClientWithQueueMode() throws UnknownHostException {

        Registration.Builder builder = new Registration.Builder("ID", "urn:client",
                Identity.unsecure(Inet4Address.getLoopbackAddress(), 12354));

        Registration reg = builder.bindingMode(EnumSet.of(BindingMode.U, BindingMode.Q)).build();
        presenceService.setAwake(reg);
        return reg;
    }
}
