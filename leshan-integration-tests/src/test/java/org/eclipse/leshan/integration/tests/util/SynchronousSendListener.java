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

import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.eclipse.leshan.core.node.LwM2mNode;
import org.eclipse.leshan.core.node.LwM2mPath;
import org.eclipse.leshan.core.node.TimestampedLwM2mNodes;
import org.eclipse.leshan.core.request.SendRequest;
import org.eclipse.leshan.server.registration.Registration;
import org.eclipse.leshan.server.send.SendListener;

public class SynchronousSendListener implements SendListener {
    private CountDownLatch dataLatch = new CountDownLatch(1);
    private volatile TimestampedLwM2mNodes data;

    private CountDownLatch errorLatch = new CountDownLatch(1);
    private volatile Exception error;

    private volatile Registration registration;

    @Override
    public void dataReceived(Registration registration, TimestampedLwM2mNodes data, SendRequest request) {
        this.data = data;
        this.registration = registration;
        dataLatch.countDown();
    }

    @Override
    public void onError(Registration registration, Exception error) {
        this.error = error;
        this.registration = registration;
        errorLatch.countDown();
    }

    public TimestampedLwM2mNodes getData() {
        return data;
    }

    public Map<LwM2mPath, LwM2mNode> getNodes() {
        return data.getNodes();
    }

    public Registration getRegistration() {
        return registration;
    }

    public Exception getError() {
        return error;
    }

    public void waitForData(int timeout, TimeUnit unit) throws TimeoutException, InterruptedException {
        if (!dataLatch.await(timeout, unit))
            throw new TimeoutException("wait for data timeout");
    }

    public void waitForError(int timeout, TimeUnit unit) throws TimeoutException, InterruptedException {
        if (!errorLatch.await(timeout, unit))
            throw new TimeoutException("wait for error timeout");
    }
}
