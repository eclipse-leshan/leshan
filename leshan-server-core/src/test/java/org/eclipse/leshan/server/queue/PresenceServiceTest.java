/*******************************************************************************
 * Copyright (c) 2017 Bosch Software Innovations GmbH and others.
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
 *     Bosch Software Innovations GmbH - initial API
 *******************************************************************************/
package org.eclipse.leshan.server.queue;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;

import org.eclipse.leshan.core.request.BindingMode;
import org.eclipse.leshan.server.registration.Registration;
import org.junit.Test;

/**
 * tests the implementation of {@link PresenceService}
 *
 */
public class PresenceServiceTest {

    private PresenceServiceImpl instance = new PresenceServiceImpl();

    @Test
    public void testSetOnlineForNonQueueMode() throws Exception {
        Registration registration = givenASimpleClient();
        instance.addListener(new PresenceListener() {

            @Override
            public void onOnline(Registration registration) {
                fail("No invocation was expected");
            }

            @Override
            public void onOffline(Registration registration) {
                fail("No invocation was expected");
            }
        });
        instance.setOnline(registration);
    }

    /**
     * Test method for
     * {@link org.eclipse.leshan.server.queue.PresenceService#setOffline(org.eclipse.leshan.server.client.Registration)}.
     * 
     * @throws Exception
     */
    @Test
    public void testSetOnlineAndOffline() throws Exception {
        Registration queueModeRegistration = givenASimpleClientWithQueueMode();
        instance.addListener(new PresenceListener() {
            int onlineStatusCountDown = 0;
            int offlineStatusCountDown = 0;

            @Override
            public void onOnline(Registration registration) {
                onlineStatusCountDown++;
                assertTrue("Expected online state to be invoked only once but invoked" + onlineStatusCountDown,
                        onlineStatusCountDown == 1);
            }

            @Override
            public void onOffline(Registration registration) {
                offlineStatusCountDown++;
                assertTrue("Expected offline state to be invoked only once but invoked" + offlineStatusCountDown,
                        offlineStatusCountDown == 1);
            }
        });
        // invoke setOnline multiple times
        instance.setOnline(queueModeRegistration);
        instance.setOnline(queueModeRegistration);
        // invoke setOffline multiple times
        instance.setOffline(queueModeRegistration);
        instance.setOffline(queueModeRegistration);
    }

    @Test
    public void testIsOnline() throws Exception {
        Registration queueModeRegistration = givenASimpleClientWithQueueMode();

        assertFalse(instance.isOnline(queueModeRegistration));
        instance.setOnline(queueModeRegistration);
        assertTrue(instance.isOnline(queueModeRegistration));
    }

    private Registration givenASimpleClient() throws UnknownHostException {
        InetSocketAddress address = InetSocketAddress.createUnresolved("localhost", 5683);

        Registration.Builder builder = new Registration.Builder("ID", "urn:client", InetAddress.getLocalHost(), 10000,
                address);

        return builder.build();
    }

    private Registration givenASimpleClientWithQueueMode() throws UnknownHostException {
        InetSocketAddress address = InetSocketAddress.createUnresolved("localhost", 5683);

        Registration.Builder builder = new Registration.Builder("ID", "urn:client", InetAddress.getLocalHost(), 10000,
                address);

        return builder.bindingMode(BindingMode.UQ).build();
    }
}
