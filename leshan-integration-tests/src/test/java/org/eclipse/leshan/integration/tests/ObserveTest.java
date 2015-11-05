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
 *     Zebra Technologies - initial API and implementation
 *******************************************************************************/

package org.eclipse.leshan.integration.tests;

import static org.eclipse.leshan.integration.tests.IntegrationTestHelper.ENDPOINT_IDENTIFIER;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.eclipse.leshan.ResponseCode;
import org.eclipse.leshan.core.node.LwM2mNode;
import org.eclipse.leshan.core.node.LwM2mObject;
import org.eclipse.leshan.core.node.LwM2mObjectInstance;
import org.eclipse.leshan.core.node.LwM2mSingleResource;
import org.eclipse.leshan.core.observation.Observation;
import org.eclipse.leshan.core.request.ObserveRequest;
import org.eclipse.leshan.core.request.ReadRequest;
import org.eclipse.leshan.core.request.RegisterRequest;
import org.eclipse.leshan.core.request.WriteRequest;
import org.eclipse.leshan.core.request.WriteRequest.Mode;
import org.eclipse.leshan.core.response.LwM2mResponse;
import org.eclipse.leshan.core.response.ObserveResponse;
import org.eclipse.leshan.core.response.ReadResponse;
import org.eclipse.leshan.server.observation.ObservationRegistryListener;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class ObserveTest {

    private IntegrationTestHelper helper = new IntegrationTestHelper();

    @Before
    public void start() {
        helper.createServer();
        helper.server.start();
        helper.createClient();
        helper.client.start();
    }

    @After
    public void stop() {
        helper.server.stop();
        helper.client.stop();
    }

    @Test
    public void can_observe_resource() throws InterruptedException {
        // client registration
        helper.client.send(new RegisterRequest(ENDPOINT_IDENTIFIER));

        TestObservationListener listener = new TestObservationListener();
        helper.server.getObservationRegistry().addListener(listener);

        // observe device timezone
        ObserveResponse observeResponse = helper.server.send(helper.getClient(), new ObserveRequest(3, 0, 15));
        assertEquals(ResponseCode.CONTENT, observeResponse.getCode());

        // an observation response should have been sent
        Observation observation = observeResponse.getObservation();
        assertEquals("/3/0/15", observation.getPath().toString());
        assertEquals(helper.getClient().getRegistrationId(), observation.getRegistrationId());

        // write device timezone
        LwM2mResponse writeResponse = helper.server.send(helper.getClient(), new WriteRequest(Mode.REPLACE, 3, 0, 15,
                "Europe/Paris"));

        // verify result
        listener.waitForNotification(2000);
        assertEquals(ResponseCode.CHANGED, writeResponse.getCode());
        assertTrue(listener.receievedNotify().get());
        assertEquals(LwM2mSingleResource.newStringResource(15, "Europe/Paris"), listener.getContent());
    }

    @Test
    public void can_observe_instance() throws InterruptedException {
        // client registration
        helper.client.send(new RegisterRequest(ENDPOINT_IDENTIFIER));

        TestObservationListener listener = new TestObservationListener();
        helper.server.getObservationRegistry().addListener(listener);

        // observe device timezone
        ObserveResponse observeResponse = helper.server.send(helper.getClient(), new ObserveRequest(3, 0));
        assertEquals(ResponseCode.CONTENT, observeResponse.getCode());

        // an observation response should have been sent
        Observation observation = observeResponse.getObservation();
        assertEquals("/3/0", observation.getPath().toString());
        assertEquals(helper.getClient().getRegistrationId(), observation.getRegistrationId());

        // write device timezone
        LwM2mResponse writeResponse = helper.server.send(helper.getClient(), new WriteRequest(Mode.REPLACE, 3, 0, 15,
                "Europe/Paris"));

        // verify result
        listener.waitForNotification(2000);
        assertEquals(ResponseCode.CHANGED, writeResponse.getCode());
        assertTrue(listener.receievedNotify().get());
        assertTrue(listener.getContent() instanceof LwM2mObjectInstance);

        // try to read the object instance for comparing
        ReadResponse readResp = helper.server.send(helper.getClient(), new ReadRequest(3, 0));

        assertEquals(readResp.getContent(), listener.getContent());
    }

    @Test
    public void can_observe_object() throws InterruptedException {
        // client registration
        helper.client.send(new RegisterRequest(ENDPOINT_IDENTIFIER));

        TestObservationListener listener = new TestObservationListener();
        helper.server.getObservationRegistry().addListener(listener);

        // observe device timezone
        ObserveResponse observeResponse = helper.server.send(helper.getClient(), new ObserveRequest(3));
        assertEquals(ResponseCode.CONTENT, observeResponse.getCode());

        // an observation response should have been sent
        Observation observation = observeResponse.getObservation();
        assertEquals("/3", observation.getPath().toString());
        assertEquals(helper.getClient().getRegistrationId(), observation.getRegistrationId());

        // write device timezone
        LwM2mResponse writeResponse = helper.server.send(helper.getClient(), new WriteRequest(Mode.REPLACE, 3, 0, 15,
                "Europe/Paris"));

        // verify result
        listener.waitForNotification(2000);
        assertEquals(ResponseCode.CHANGED, writeResponse.getCode());
        assertTrue(listener.receievedNotify().get());
        assertTrue(listener.getContent() instanceof LwM2mObject);

        // try to read the object for comparing
        ReadResponse readResp = helper.server.send(helper.getClient(), new ReadRequest(3));

        assertEquals(readResp.getContent(), listener.getContent());
    }

    private final class TestObservationListener implements ObservationRegistryListener {

        private final CountDownLatch latch = new CountDownLatch(1);
        private final AtomicBoolean receivedNotify = new AtomicBoolean();
        private LwM2mNode content;

        @Override
        public void newValue(final Observation observation, final LwM2mNode value) {
            receivedNotify.set(true);
            content = value;
            latch.countDown();
        }

        @Override
        public void cancelled(final Observation observation) {
            latch.countDown();
        }

        @Override
        public void newObservation(final Observation observation) {
        }

        public AtomicBoolean receievedNotify() {
            return receivedNotify;
        }

        public LwM2mNode getContent() {
            return content;
        }

        public void waitForNotification(long timeout) throws InterruptedException {
            latch.await(timeout, TimeUnit.MILLISECONDS);
        }
    }
}
