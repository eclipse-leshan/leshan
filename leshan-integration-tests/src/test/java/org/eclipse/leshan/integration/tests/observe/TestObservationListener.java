/*******************************************************************************
 * Copyright (c) 2022    Sierra Wireless and others.
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
 *******************************************************************************/
package org.eclipse.leshan.integration.tests.observe;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.eclipse.leshan.core.observation.CompositeObservation;
import org.eclipse.leshan.core.observation.Observation;
import org.eclipse.leshan.core.observation.SingleObservation;
import org.eclipse.leshan.core.response.ObserveCompositeResponse;
import org.eclipse.leshan.core.response.ObserveResponse;
import org.eclipse.leshan.server.observation.ObservationListener;
import org.eclipse.leshan.server.registration.Registration;

public class TestObservationListener implements ObservationListener {

    private CountDownLatch latch = new CountDownLatch(1);
    private final AtomicBoolean receivedNotify = new AtomicBoolean();
    private AtomicInteger counter = new AtomicInteger(0);
    private ObserveResponse observeResponse;
    private ObserveCompositeResponse observeCompositeResponse;
    private Exception error;

    @Override
    public void onResponse(SingleObservation observation, Registration registration, ObserveResponse response) {
        receivedNotify.set(true);
        this.observeResponse = response;
        this.error = null;
        this.counter.incrementAndGet();
        latch.countDown();
    }

    @Override
    public void onResponse(CompositeObservation observation, Registration registration,
            ObserveCompositeResponse response) {
        receivedNotify.set(true);
        this.observeCompositeResponse = response;
        this.error = null;
        this.counter.incrementAndGet();
        latch.countDown();
    }

    @Override
    public void onError(Observation observation, Registration registration, Exception error) {
        receivedNotify.set(true);
        this.observeResponse = null;
        this.observeCompositeResponse = null;
        this.error = error;
        latch.countDown();
    }

    @Override
    public void cancelled(Observation observation) {
        latch.countDown();
    }

    @Override
    public void newObservation(Observation observation, Registration registration) {
    }

    public AtomicBoolean receivedNotify() {
        return receivedNotify;
    }

    public ObserveResponse getObserveResponse() {
        return observeResponse;
    }

    public ObserveCompositeResponse getObserveCompositeResponse() {
        return observeCompositeResponse;
    }

    public Exception getError() {
        return error;
    }

    public void waitForNotification(long timeout) throws InterruptedException {
        latch.await(timeout, TimeUnit.MILLISECONDS);
    }

    public int getNotificationCount() {
        return counter.get();
    }

    public void reset() {
        latch = new CountDownLatch(1);
        receivedNotify.set(false);
        this.observeResponse = null;
        this.observeCompositeResponse = null;
        error = null;
        this.counter = new AtomicInteger(0);
    }
}
