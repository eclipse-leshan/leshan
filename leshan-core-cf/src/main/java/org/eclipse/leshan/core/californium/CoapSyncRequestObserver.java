/*******************************************************************************
 * Copyright (c) 2016 Sierra Wireless and others.
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
 *     Achim Kraus (Bosch Software Innovations GmbH) - set exception in onSendError
 *     Simon Bernard                                 - use specific exception for onSendError  
 *******************************************************************************/
package org.eclipse.leshan.core.californium;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.californium.core.coap.Request;
import org.eclipse.californium.core.coap.Response;
import org.eclipse.leshan.core.request.exception.RequestRejectedException;
import org.eclipse.leshan.core.request.exception.SendFailedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CoapSyncRequestObserver extends AbstractRequestObserver {

    private static final Logger LOG = LoggerFactory.getLogger(CoapSyncRequestObserver.class);

    private CountDownLatch latch = new CountDownLatch(1);
    private AtomicReference<Response> ref = new AtomicReference<>(null);
    private AtomicBoolean coapTimeout = new AtomicBoolean(false);
    private AtomicReference<RuntimeException> exception = new AtomicReference<>();
    private long timeout;

    public CoapSyncRequestObserver(Request coapRequest, long timeout) {
        super(coapRequest);
        this.timeout = timeout;
    }

    @Override
    public void onResponse(Response coapResponse) {
        LOG.debug("Received coap response: {}", coapResponse);
        try {
            ref.set(coapResponse);
        } catch (RuntimeException e) {
            exception.set(e);
        } finally {
            latch.countDown();
        }
    }

    @Override
    public void onTimeout() {
        coapTimeout.set(true);
        latch.countDown();
    }

    @Override
    public void onCancel() {
        LOG.debug(String.format("Synchronous request cancelled %s", coapRequest));
        latch.countDown();
    }

    @Override
    public void onReject() {
        exception.set(new RequestRejectedException("Request %s rejected", coapRequest.getURI()));
        latch.countDown();
    }

    @Override
    public void onSendError(Throwable error) {
        exception.set(new SendFailedException(error, "Request %s cannot be sent", coapRequest, error.getMessage()));
        latch.countDown();
    }

    public Response waitForCoapResponse() throws InterruptedException {
        try {
            boolean timeElapsed = false;
            timeElapsed = !latch.await(timeout, TimeUnit.MILLISECONDS);
            if (timeElapsed || coapTimeout.get()) {
                coapRequest.cancel();
            }
        } finally {
            coapRequest.removeMessageObserver(this);
        }

        if (exception.get() != null) {
            coapRequest.cancel();
            throw exception.get();
        }
        return ref.get();
    }
}