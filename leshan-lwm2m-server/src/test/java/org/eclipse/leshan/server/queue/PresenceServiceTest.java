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

import static org.eclipse.leshan.core.util.TestToolBox.uriHandler;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.net.Inet4Address;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.EnumSet;

import org.eclipse.leshan.core.peer.IpPeer;
import org.eclipse.leshan.core.request.BindingMode;
import org.eclipse.leshan.server.registration.IRegistration;
import org.eclipse.leshan.server.registration.Registration;
import org.junit.jupiter.api.Test;

/**
 * tests the implementation of {@link PresenceService}
 *
 */
public class PresenceServiceTest {

    private final ClientAwakeTimeProvider awakeTimeProvider = new StaticClientAwakeTimeProvider();
    private final PresenceServiceImpl presenceService = new PresenceServiceImpl(awakeTimeProvider);

    @Test
    public void testSetOnlineForNonQueueMode() throws Exception {
        IRegistration registration = givenASimpleClient();
        presenceService.addListener(new PresenceListener() {

            @Override
            public void onAwake(IRegistration registration) {
                fail("No invocation was expected");
            }

            @Override
            public void onSleeping(IRegistration registration) {
                fail("No invocation was expected");
            }
        });
        presenceService.setAwake(registration);
    }

    @Test
    public void testIsOnline() throws Exception {
        IRegistration queueModeRegistration = givenASimpleClientWithQueueMode();

        assertTrue(presenceService.isClientAwake(queueModeRegistration));
        presenceService.setSleeping(queueModeRegistration);
        assertFalse(presenceService.isClientAwake(queueModeRegistration));
    }

    private IRegistration givenASimpleClient() throws UnknownHostException {
        Registration.Builder builder = new Registration.Builder("ID", "urn:client",
                new IpPeer(new InetSocketAddress(Inet4Address.getLoopbackAddress(), 12354)),
                uriHandler.createUri("coap://localhost:5683"));

        IRegistration reg = builder.build();
        presenceService.setAwake(reg);
        return reg;
    }

    private IRegistration givenASimpleClientWithQueueMode() throws UnknownHostException {

        Registration.Builder builder = new Registration.Builder("ID", "urn:client",
                new IpPeer(new InetSocketAddress(Inet4Address.getLoopbackAddress(), 12354)),
                uriHandler.createUri("coap://localhost:5683"));

        IRegistration reg = builder.bindingMode(EnumSet.of(BindingMode.U, BindingMode.Q)).build();
        presenceService.setAwake(reg);
        return reg;
    }
}
