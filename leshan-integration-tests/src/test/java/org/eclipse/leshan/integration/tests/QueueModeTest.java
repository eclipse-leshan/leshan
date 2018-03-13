/*******************************************************************************
 * Copyright (c) 2017 RISE SICS AB.
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
 *     RISE SICS AB - initial API and implementation
 *******************************************************************************/

package org.eclipse.leshan.integration.tests;

import static org.junit.Assert.assertArrayEquals;

import org.eclipse.leshan.Link;
import org.eclipse.leshan.core.request.ReadRequest;
import org.eclipse.leshan.core.response.ReadResponse;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class QueueModeTest {

    protected QueueModeIntegrationTestHelper queueModeHelper = new QueueModeIntegrationTestHelper();
    private final long awaketime = 1; // seconds

    @Before
    public void start() {
        queueModeHelper.initialize();
        queueModeHelper.createServer((int) awaketime * 1000);
        queueModeHelper.server.start();
        queueModeHelper.createClient();
    }

    @After
    public void stop() throws InterruptedException {
        queueModeHelper.client.destroy(true);
        queueModeHelper.server.destroy();
        queueModeHelper.dispose();
    }

    @Test
    public void awake_sleeping_awake_sleeping() {
        // Check client is not registered
        queueModeHelper.assertClientNotRegisterered();

        // Start it and wait for registration
        queueModeHelper.client.start();
        queueModeHelper.waitForRegistration(1);

        // Check client is well registered
        queueModeHelper.assertClientRegisterered();
        assertArrayEquals(Link.parse("</>;rt=\"oma.lwm2m\",</1/0>,</2>,</3/0>,</2000/0>".getBytes()),
                queueModeHelper.getCurrentRegistration().getObjectLinks());

        // Check that client is awake
        queueModeHelper.ensureClientAwake();

        // Wait for client awake time expiration (1% margin)
        queueModeHelper.waitForAwakeTime(awaketime * 1010);

        // Check that client is sleeping
        queueModeHelper.ensureClientSleeping();

        // Trigger update manually for waking up
        queueModeHelper.client.triggerRegistrationUpdate();
        queueModeHelper.waitForUpdate(1);

        // Check that client is awake
        queueModeHelper.ensureClientAwake();
        queueModeHelper.resetAwakeLatch();

        // Wait for client awake time expiration (1% margin)
        queueModeHelper.waitForAwakeTime(awaketime * 1010);

        // Check that client is sleeping
        queueModeHelper.ensureClientSleeping();

        // Stop client with out de-registration
        queueModeHelper.client.stop(false);
    }

    @Test
    public void one_awake_notification() {

        // Check client is not registered
        queueModeHelper.assertClientNotRegisterered();

        // Start it and wait for registration
        queueModeHelper.client.start();
        queueModeHelper.waitForRegistration(1);

        // Check client is well registered
        queueModeHelper.assertClientRegisterered();

        // Check that client is awake and only one awake notification
        queueModeHelper.ensureClientAwake();
        queueModeHelper.ensureOneAwakeNotification();

        // Triggers one update
        queueModeHelper.client.triggerRegistrationUpdate();
        queueModeHelper.waitForUpdate(1);
        queueModeHelper.resetLatch();

        // Check only one notification
        queueModeHelper.ensureOneAwakeNotification();

        // Wait for client awake time expiration (1% margin)
        queueModeHelper.waitForAwakeTime(awaketime * 1010);

        // Check that client is sleeping
        queueModeHelper.ensureClientSleeping();

        // Trigger update manually for waking up
        queueModeHelper.client.triggerRegistrationUpdate();
        queueModeHelper.waitForUpdate(1);
        queueModeHelper.resetLatch();

        // Check that client is awake
        queueModeHelper.ensureClientAwake();
        queueModeHelper.resetAwakeLatch();

        // Triggers two updates
        queueModeHelper.client.triggerRegistrationUpdate();
        queueModeHelper.waitForUpdate(1);
        queueModeHelper.resetLatch();
        queueModeHelper.client.triggerRegistrationUpdate();
        queueModeHelper.waitForUpdate(1);
        queueModeHelper.resetLatch();

        // Check only one notification
        queueModeHelper.ensureOneAwakeNotification();

    }

    @Test
    public void sleeping_if_timeout() throws InterruptedException {

        // Check client is not registered
        queueModeHelper.assertClientNotRegisterered();

        // Start it and wait for registration
        queueModeHelper.client.start();
        queueModeHelper.waitForRegistration(1);

        // Check client is well registered and awake
        queueModeHelper.assertClientRegisterered();
        queueModeHelper.ensureClientAwake();

        // Stop the client to ensure that TimeOut exception is thrown
        queueModeHelper.client.stop(false);
        // Send a response with very short timeout
        ReadResponse response = queueModeHelper.server.send(queueModeHelper.getCurrentRegistration(),
                new ReadRequest(3, 0, 1), 1);

        // Check that a timeout occurs
        queueModeHelper.ensureTimeoutException(response);

        // Check that the client is sleeping
        queueModeHelper.ensureClientSleeping();
    }

    @Test
    public void correct_sending_when_awake() throws InterruptedException {
        ReadResponse response;

        // Check client is not registered
        queueModeHelper.assertClientNotRegisterered();

        // Start it and wait for registration
        queueModeHelper.client.start();
        queueModeHelper.waitForRegistration(1);

        // Check client is well registered and awake
        queueModeHelper.assertClientRegisterered();
        queueModeHelper.ensureClientAwake();

        // Send a response a check that it is received correctly
        response = queueModeHelper.server.send(queueModeHelper.getCurrentRegistration(), new ReadRequest(3, 0, 1));
        queueModeHelper.ensureReceivedRequest(response);

        // Wait for client awake time expiration (1% margin)
        queueModeHelper.waitForAwakeTime(awaketime * 1010);

        // Check that client is sleeping
        queueModeHelper.ensureClientSleeping();

        // Trigger update manually for waking up
        queueModeHelper.client.triggerRegistrationUpdate();
        queueModeHelper.waitForUpdate(1);

        // Check that client is awake
        queueModeHelper.ensureClientAwake();

        // Send a response a check that it is received correctly
        response = queueModeHelper.server.send(queueModeHelper.getCurrentRegistration(), new ReadRequest(3, 0, 1));
        queueModeHelper.ensureReceivedRequest(response);
    }

}
