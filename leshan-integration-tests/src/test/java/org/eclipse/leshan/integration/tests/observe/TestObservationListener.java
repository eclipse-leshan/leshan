package org.eclipse.leshan.integration.tests.observe;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.eclipse.leshan.core.observation.Observation;
import org.eclipse.leshan.core.response.ObserveResponse;
import org.eclipse.leshan.server.observation.ObservationListener;
import org.eclipse.leshan.server.registration.Registration;

public class TestObservationListener implements ObservationListener {

    private CountDownLatch latch = new CountDownLatch(1);
    private final AtomicBoolean receivedNotify = new AtomicBoolean();
    private AtomicInteger counter = new AtomicInteger(0);
    private ObserveResponse response;
    private Exception error;

    @Override
    public void onResponse(Observation observation, Registration registration, ObserveResponse response) {
        receivedNotify.set(true);
        this.response = response;
        this.error = null;
        this.counter.incrementAndGet();
        latch.countDown();
    }

    @Override
    public void onError(Observation observation, Registration registration, Exception error) {
        receivedNotify.set(true);
        this.response = null;
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

    public ObserveResponse getResponse() {
        return response;
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
        response = null;
        error = null;
        this.counter = new AtomicInteger(0);
    }
}