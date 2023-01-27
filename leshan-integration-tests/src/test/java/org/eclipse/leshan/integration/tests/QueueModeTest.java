/*******************************************************************************
 * Copyright (c) 2017 RISE SICS AB.
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
 *     RISE SICS AB - initial API and implementation
 *     Michał Wadowski (Orange) - Improved compliance with rfc6690
 *******************************************************************************/

package org.eclipse.leshan.integration.tests;

import static org.eclipse.leshan.integration.tests.util.IntegrationTestHelper.linkParser;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.eclipse.leshan.core.link.LinkParseException;
import org.eclipse.leshan.core.request.ReadRequest;
import org.eclipse.leshan.core.response.ReadResponse;
import org.eclipse.leshan.integration.tests.util.QueueModeIntegrationTestHelper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class QueueModeTest {

    protected QueueModeIntegrationTestHelper queueModeHelper = new QueueModeIntegrationTestHelper();
    private final long awaketime = 1; // seconds

    @BeforeEach
    public void start() {
        queueModeHelper.initialize();
        queueModeHelper.createServer((int) awaketime * 1000);
        queueModeHelper.server.start();
        queueModeHelper.createClient();
    }

    @AfterEach
    public void stop() throws InterruptedException {
        queueModeHelper.client.destroy(true);
        queueModeHelper.server.destroy();
        queueModeHelper.dispose();
    }

    @Test
    public void awake_sleeping_awake_sleeping() throws LinkParseException {
        // Check client is not registered
        queueModeHelper.assertClientNotRegisterered();

        // Start it and wait for registration
        queueModeHelper.client.start();

        // Check that client is awake
        queueModeHelper.waitToGetAwake(1000);
        queueModeHelper.ensureClientAwake();

        // Check client is well registered
        queueModeHelper.assertClientRegisterered();
        assertArrayEquals(linkParser.parseCoreLinkFormat(
                "</>;rt=\"oma.lwm2m\";ct=\"60 110 112 11542 11543\",</1>;ver=1.1,</1/0>,</2>,</3>;ver=1.1,</3/0>,</3442/0>"
                        .getBytes()),
                queueModeHelper.getCurrentRegistration().getObjectLinks());

        // Wait for client awake time expiration (20% margin)
        queueModeHelper.ensureAwakeFor(awaketime, 200);

        // Check that client is sleeping
        queueModeHelper.ensureClientSleeping();

        // Trigger update manually for waking up
        queueModeHelper.waitForRegistrationAtClientSide(1);
        queueModeHelper.client.triggerRegistrationUpdate();

        // Check that client is awake
        queueModeHelper.waitToGetAwake(1000);
        queueModeHelper.ensureClientAwake();

        // Wait for client awake time expiration (20% margin)
        queueModeHelper.ensureAwakeFor(awaketime, 200);

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

        // Check that client is awake and only one awake notification
        queueModeHelper.waitToGetAwake(1000);
        queueModeHelper.ensureClientAwake();
        assertEquals(1, queueModeHelper.presenceCounter.getNbAwake(), "Only one awake event should be received");

        // Check client is well registered
        queueModeHelper.assertClientRegisterered();

        // Triggers one update
        queueModeHelper.waitForRegistrationAtClientSide(1);
        queueModeHelper.client.triggerRegistrationUpdate();
        queueModeHelper.waitForUpdateAtClientSide(1);

        // Check only one notification
        assertEquals(1, queueModeHelper.presenceCounter.getNbAwake(), "Only one awake event should be received");

        // Wait for client awake time expiration (20% margin)
        queueModeHelper.ensureAwakeFor(awaketime, 200);

        // Check that client is sleeping
        queueModeHelper.ensureClientSleeping();

        // Trigger update manually for waking up
        queueModeHelper.presenceCounter.resetCounter();
        queueModeHelper.client.triggerRegistrationUpdate();
        queueModeHelper.waitForUpdateAtClientSide(1);

        // Check that client is awake
        queueModeHelper.waitToGetAwake(500);
        queueModeHelper.ensureClientAwake();

        // Triggers two updates
        queueModeHelper.client.triggerRegistrationUpdate();
        queueModeHelper.waitForUpdateAtClientSide(1);
        queueModeHelper.client.triggerRegistrationUpdate();
        queueModeHelper.waitForUpdateAtClientSide(1);

        // Check only one notification
        assertEquals(1, queueModeHelper.presenceCounter.getNbAwake(), "Only one awake event should be received");

    }

    @Test
    public void sleeping_if_timeout() throws InterruptedException {
        // Check client is not registered
        queueModeHelper.assertClientNotRegisterered();

        // Start it and wait for registration
        queueModeHelper.client.start();

        // Check client is well registered and awake
        queueModeHelper.waitToGetAwake(1000);
        queueModeHelper.ensureClientAwake();
        queueModeHelper.assertClientRegisterered();

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

        // Check client is well registered and awake
        queueModeHelper.waitToGetAwake(1000);
        queueModeHelper.ensureClientAwake();
        queueModeHelper.assertClientRegisterered();

        // Send a response a check that it is received correctly
        response = queueModeHelper.server.send(queueModeHelper.getCurrentRegistration(), new ReadRequest(3, 0, 1));
        queueModeHelper.ensureReceivedRequest(response);

        // Wait for client awake time expiration (20% margin)
        queueModeHelper.ensureAwakeFor(awaketime, 200);

        // Check that client is sleeping
        queueModeHelper.ensureClientSleeping();

        // Trigger update manually for waking up
        queueModeHelper.client.triggerRegistrationUpdate();
        queueModeHelper.waitForUpdateAtClientSide(1);

        // Check that client is awake
        queueModeHelper.waitToGetAwake(500);
        queueModeHelper.ensureClientAwake();

        // Send request and check that it is received correctly
        response = queueModeHelper.server.send(queueModeHelper.getCurrentRegistration(), new ReadRequest(3, 0, 1));
        queueModeHelper.ensureReceivedRequest(response);
    }
}
