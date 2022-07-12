/*******************************************************************************
 * Copyright (c) 2021 Sierra Wireless and others.
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
 *     Sierra Wireless - initial API and implementation
 *******************************************************************************/
package org.eclipse.leshan.integration.tests.util;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.eclipse.leshan.core.request.BootstrapDownlinkRequest;
import org.eclipse.leshan.core.request.BootstrapRequest;
import org.eclipse.leshan.core.request.Identity;
import org.eclipse.leshan.core.response.LwM2mResponse;
import org.eclipse.leshan.server.bootstrap.BootstrapFailureCause;
import org.eclipse.leshan.server.bootstrap.BootstrapSession;
import org.eclipse.leshan.server.bootstrap.BootstrapSessionListener;

public class SynchronousBootstrapListener implements BootstrapSessionListener {

    private CountDownLatch finishedLatch = new CountDownLatch(1);
    private CountDownLatch failedLatch = new CountDownLatch(1);
    private BootstrapSession lastSucessfulSession;
    private BootstrapFailureCause lastCause;

    @Override
    public void sessionInitiated(BootstrapRequest request, Identity clientIdentity) {
    }

    @Override
    public void unAuthorized(BootstrapRequest request, Identity clientIdentity) {
    }

    @Override
    public void authorized(BootstrapSession session) {

    }

    @Override
    public void noConfig(BootstrapSession session) {
    }

    @Override
    public void sendRequest(BootstrapSession session, BootstrapDownlinkRequest<? extends LwM2mResponse> request) {
    }

    @Override
    public void onResponseSuccess(BootstrapSession session, BootstrapDownlinkRequest<? extends LwM2mResponse> request,
            LwM2mResponse response) {
    }

    @Override
    public void onResponseError(BootstrapSession session, BootstrapDownlinkRequest<? extends LwM2mResponse> request,
            LwM2mResponse response) {
    }

    @Override
    public void onRequestFailure(BootstrapSession session, BootstrapDownlinkRequest<? extends LwM2mResponse> request,
            Throwable cause) {
    }

    @Override
    public void end(BootstrapSession session) {
        lastSucessfulSession = session;
        finishedLatch.countDown();
    }

    @Override
    public void failed(BootstrapSession session, BootstrapFailureCause cause) {
        lastCause = cause;
        failedLatch.countDown();
    }

    /**
     * Wait until next bootstrap success event.
     *
     * @throws TimeoutException if wait timeouts
     */
    public void waitForSuccess(long timeout, TimeUnit timeUnit) throws InterruptedException, TimeoutException {
        try {
            if (!finishedLatch.await(timeout, timeUnit))
                throw new TimeoutException("wait for bs success timeout");
        } finally {
            finishedLatch = new CountDownLatch(1);
        }
    }

    /**
     * Wait until next bootstrap failure event.
     *
     * @throws TimeoutException if wait timeouts
     */
    public void waitForFailure(long timeout, TimeUnit timeUnit) throws InterruptedException, TimeoutException {
        try {
            if (!failedLatch.await(timeout, timeUnit))
                throw new TimeoutException("wait for bs failure timeout");
        } finally {
            failedLatch = new CountDownLatch(1);
        }
    }

    public BootstrapSession getLastSuccessfulSession() {
        return lastSucessfulSession;
    }

    public BootstrapFailureCause getLastCauseOfFailure() {
        return lastCause;
    }
}
