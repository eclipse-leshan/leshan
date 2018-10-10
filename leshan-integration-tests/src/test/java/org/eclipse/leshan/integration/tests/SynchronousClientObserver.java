/*******************************************************************************
 * Copyright (c) 2018 Sierra Wireless and others.
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
 *     Sierra Wireless - initial API and implementation
 *******************************************************************************/
package org.eclipse.leshan.integration.tests;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

import org.eclipse.leshan.ResponseCode;
import org.eclipse.leshan.client.observer.LwM2mClientObserver;
import org.eclipse.leshan.client.servers.Server;

public class SynchronousClientObserver implements LwM2mClientObserver {

    private CountDownLatch registerLatch = new CountDownLatch(1);
    private AtomicBoolean registerSucceed = new AtomicBoolean(false);
    private AtomicBoolean registerFailed = new AtomicBoolean(false);;

    private CountDownLatch updateLatch = new CountDownLatch(1);
    private AtomicBoolean updateSucceed = new AtomicBoolean(false);
    private AtomicBoolean updateFailed = new AtomicBoolean(false);

    private CountDownLatch deregisterLatch = new CountDownLatch(1);
    private AtomicBoolean deregisterSucceed = new AtomicBoolean(false);
    private AtomicBoolean deregisterFailed = new AtomicBoolean(false);

    private CountDownLatch bootstrapLatch = new CountDownLatch(1);
    private AtomicBoolean bootstrapSucceed = new AtomicBoolean(false);
    private AtomicBoolean bootstrapFailed = new AtomicBoolean(false);

    @Override
    public void onBootstrapSuccess(Server bsserver) {
        bootstrapSucceed.set(true);
        bootstrapLatch.countDown();
    }

    @Override
    public void onBootstrapFailure(Server bsserver, ResponseCode responseCode, String errorMessage) {
        bootstrapFailed.set(true);
        bootstrapLatch.countDown();
    }

    @Override
    public void onBootstrapTimeout(Server bsserver) {
        bootstrapLatch.countDown();
    }

    @Override
    public void onRegistrationSuccess(Server server, String registrationID) {
        registerSucceed.set(true);
        registerLatch.countDown();
    }

    @Override
    public void onRegistrationFailure(Server server, ResponseCode responseCode, String errorMessage) {
        registerFailed.set(true);
        registerLatch.countDown();
    }

    @Override
    public void onRegistrationTimeout(Server server) {
        registerLatch.countDown();
    }

    @Override
    public void onUpdateSuccess(Server server, String registrationID) {
        updateSucceed.set(true);
        updateLatch.countDown();
    }

    @Override
    public void onUpdateFailure(Server server, ResponseCode responseCode, String errorMessage) {
        updateFailed.set(true);
        updateLatch.countDown();
    }

    @Override
    public void onUpdateTimeout(Server server) {
        updateLatch.countDown();
    }

    @Override
    public void onDeregistrationSuccess(Server server, String registrationID) {
        deregisterSucceed.set(true);
        deregisterLatch.countDown();
    }

    @Override
    public void onDeregistrationFailure(Server server, ResponseCode responseCode, String errorMessage) {
        deregisterFailed.set(true);
        deregisterLatch.countDown();
    }

    @Override
    public void onDeregistrationTimeout(Server server) {
        deregisterLatch.countDown();
    }

    /**
     * Wait for registration.
     * 
     * @return true if registration succeed, false if it failed
     * @throws TimeoutException if registration timeout
     */
    public boolean waitForRegistration(long timeout, TimeUnit timeUnit) throws InterruptedException, TimeoutException {
        try {
            if (registerLatch.await(timeout, timeUnit)) {
                if (registerSucceed.get())
                    return true;
                if (registerFailed.get())
                    return false;
                throw new TimeoutException("client registration timeout");
            }
            throw new TimeoutException("client registration latch timeout");
        } finally {
            registerLatch = new CountDownLatch(1);
        }
    }

    /**
     * Wait for registration update.
     * 
     * @return true if registration update succeed, false if it failed
     * @throws TimeoutException if registration update update timeout
     */
    public boolean waitForUpdate(long timeout, TimeUnit timeUnit) throws InterruptedException, TimeoutException {
        try {
            if (updateLatch.await(timeout, timeUnit)) {
                if (updateSucceed.get())
                    return true;
                if (updateFailed.get())
                    return false;
                throw new TimeoutException("client registration update timeout");
            }
            throw new TimeoutException("client registration update latch timeout");
        } finally {
            updateLatch = new CountDownLatch(1);
        }
    }

    /**
     * Wait for deregistration.
     * 
     * @return true if deregistration succeed, false if it failed
     * @throws TimeoutException if deregistration timeout
     */
    public boolean waitForDeregistration(long timeout, TimeUnit timeUnit)
            throws InterruptedException, TimeoutException {
        try {
            if (deregisterLatch.await(timeout, timeUnit)) {
                if (deregisterSucceed.get())
                    return true;
                if (deregisterFailed.get())
                    return false;
                throw new TimeoutException("client deregistration timeout");
            }
            throw new TimeoutException("client deregistration latch timeout");
        } finally {
            deregisterLatch = new CountDownLatch(1);
        }
    }

    /**
     * Wait for bootstrap.
     * 
     * @return true if bootstrap succeed, false if it failed
     * @throws TimeoutException if bootstrap timeout
     */
    public boolean waitForBootstrap(long timeout, TimeUnit timeUnit) throws InterruptedException, TimeoutException {
        try {
            if (bootstrapLatch.await(timeout, timeUnit)) {
                if (bootstrapSucceed.get())
                    return true;
                if (bootstrapFailed.get())
                    return false;
                throw new TimeoutException("client bootstrap timeout");
            }
            throw new TimeoutException("client bootstrap latch timeout");
        } finally {
            bootstrapLatch = new CountDownLatch(1);
        }
    }
}
