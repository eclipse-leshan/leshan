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

import static org.junit.Assert.*;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.eclipse.leshan.LwM2mId;
import org.eclipse.leshan.client.californium.LeshanClientBuilder;
import org.eclipse.leshan.client.object.Device;
import org.eclipse.leshan.client.object.Security;
import org.eclipse.leshan.client.object.Server;
import org.eclipse.leshan.client.resource.LwM2mObjectEnabler;
import org.eclipse.leshan.client.resource.ObjectsInitializer;
import org.eclipse.leshan.core.model.LwM2mModel;
import org.eclipse.leshan.core.request.BindingMode;
import org.eclipse.leshan.core.response.ExecuteResponse;
import org.eclipse.leshan.core.response.LwM2mResponse;
import org.eclipse.leshan.server.californium.LeshanServerBuilder;
import org.eclipse.leshan.server.queue.PresenceListener;
import org.eclipse.leshan.server.queue.StaticClientAwakeTimeProvider;
import org.eclipse.leshan.server.registration.Registration;

/**
 * Helper for running a server and executing a client against it.
 * 
 */
public class QueueModeIntegrationTestHelper extends IntegrationTestHelper {
    public static final long LIFETIME = 3600; // Updates are manually triggered with a timer
    private CountDownLatch awakeLatch;
    private AtomicInteger awakeNotifications = new AtomicInteger();

    @Override
    public void createClient() {
        // Create objects Enabler
        ObjectsInitializer initializer = new ObjectsInitializer(new LwM2mModel(createObjectModels()));
        initializer.setInstancesForObject(LwM2mId.SECURITY, Security.noSec(
                "coap://" + server.getUnsecuredAddress().getHostString() + ":" + server.getUnsecuredAddress().getPort(),
                12345));
        initializer.setInstancesForObject(LwM2mId.SERVER, new Server(12345, LIFETIME, BindingMode.UQ, false));
        initializer.setInstancesForObject(LwM2mId.DEVICE, new Device("Eclipse Leshan", MODEL_NUMBER, "12345", "UQ") {
            @Override
            public ExecuteResponse execute(int resourceid, String params) {
                if (resourceid == 4) {
                    return ExecuteResponse.success();
                } else {
                    return super.execute(resourceid, params);
                }
            }
        });
        List<LwM2mObjectEnabler> objects = initializer.createMandatory();
        objects.addAll(initializer.create(2, 2000));

        // Build Client
        LeshanClientBuilder builder = new LeshanClientBuilder(currentEndpointIdentifier.get());
        builder.setObjects(objects);
        client = builder.build();
    }

    public void createServer(int clientAwakeTime) {
        server = createServerBuilder(clientAwakeTime).build();
        awakeNotifications.set(0);
        server.getPresenceService().addListener(new PresenceListener() {

            @Override
            public void onAwake(Registration registration) {
                if (registration.getEndpoint().equals(currentEndpointIdentifier.get())) {
                    awakeNotifications.addAndGet(1);
                }

            }

            @Override
            public void onSleeping(Registration registration) {
                awakeNotifications.set(0);
                awakeLatch.countDown();
            }

        });
        // monitor client registration
        setupRegistrationMonitoring();
    }

    protected LeshanServerBuilder createServerBuilder(int clientAwakeTime) {
        LeshanServerBuilder builder = super.createServerBuilder();
        builder.setClientAwakeTimeProvider(new StaticClientAwakeTimeProvider(clientAwakeTime));
        return builder;
    }

    @Override
    public void resetLatch() {
        registerLatch = new CountDownLatch(1);
        deregisterLatch = new CountDownLatch(1);
        updateLatch = new CountDownLatch(1);
        awakeLatch = new CountDownLatch(1);
    }

    public void resetAwakeLatch() {
        awakeLatch = new CountDownLatch(1);
    }

    public void waitForAwakeTime(long timeInMilliseconds) {
        try {
            assertTrue(awakeLatch.await(timeInMilliseconds, TimeUnit.MILLISECONDS));
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public void ensureOneAwakeNotification() {
        assertEquals(1, awakeNotifications.intValue());
    }

    public void ensureClientAwake() {
        assertTrue(server.getPresenceService().isClientAwake(getCurrentRegistration()));
    }

    public void ensureClientSleeping() {
        assertFalse(server.getPresenceService().isClientAwake(getCurrentRegistration()));
    }

    public void ensureReceivedRequest(LwM2mResponse response) {
        assertNotNull(response);
    }

    public void ensureTimeoutException(LwM2mResponse response) {
        assertNull(response);
    }

}
