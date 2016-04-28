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
 *******************************************************************************/
package org.eclipse.leshan.server.californium.impl;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.californium.core.coap.Request;
import org.eclipse.californium.core.coap.Response;
import org.eclipse.leshan.core.request.exception.RequestFailedException;
import org.eclipse.leshan.core.response.LwM2mResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

//////// Request Observer Class definition/////////////
// TODO leshan-code-cf: All Request Observer should be factorize in a leshan-core-cf project.
// duplicate from org.eclipse.leshan.client.californium.impl.CaliforniumLwM2mClientRequestSender
public abstract class SyncRequestObserver<T extends LwM2mResponse> extends AbstractRequestObserver<T> {

    private static final Logger LOG = LoggerFactory.getLogger(SyncRequestObserver.class);

    CountDownLatch latch = new CountDownLatch(1);
    AtomicReference<T> ref = new AtomicReference<T>(null);
    AtomicBoolean coapTimeout = new AtomicBoolean(false);
    AtomicReference<RuntimeException> exception = new AtomicReference<>();
    Long timeout;

    public SyncRequestObserver(final Request coapRequest, final Long timeout) {
        super(coapRequest);
        this.timeout = timeout;
    }

    @Override
    public void onResponse(final Response coapResponse) {
        LOG.debug("Received coap response: {}", coapResponse);
        try {
            final T lwM2mResponseT = buildResponse(coapResponse);
            if (lwM2mResponseT != null) {
                ref.set(lwM2mResponseT);
            }
        } catch (final RuntimeException e) {
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
        exception.set(new RequestFailedException("Rejected request"));
        latch.countDown();
    }

    public T waitForResponse() throws InterruptedException {
        try {
            boolean timeElapsed = false;
            if (timeout != null) {
                timeElapsed = !latch.await(timeout, TimeUnit.MILLISECONDS);
            } else {
                latch.await();
            }
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