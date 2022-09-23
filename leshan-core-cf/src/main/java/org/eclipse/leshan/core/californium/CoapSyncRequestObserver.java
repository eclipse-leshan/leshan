/*******************************************************************************
 * Copyright (c) 2016 Sierra Wireless and others.
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
import org.eclipse.leshan.core.request.exception.RequestCanceledException;
import org.eclipse.leshan.core.request.exception.RequestRejectedException;
import org.eclipse.leshan.core.request.exception.SendFailedException;
import org.eclipse.leshan.core.request.exception.TimeoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A Californium message observer for a CoAP request.
 * <p>
 * Results are available via synchronous {@link #waitForCoapResponse()} method.
 * <p>
 * This class also provides response timeout facility.
 *
 * @see <a href="https://github.com/eclipse/leshan/wiki/Request-Timeout">Request Timeout Wiki page</a>
 */
public class CoapSyncRequestObserver extends AbstractRequestObserver {

    private static final Logger LOG = LoggerFactory.getLogger(CoapSyncRequestObserver.class);

    private final CountDownLatch latch = new CountDownLatch(1);
    private final AtomicReference<Response> ref = new AtomicReference<>(null);
    private final AtomicBoolean coapTimeout = new AtomicBoolean(false);
    private final AtomicReference<RuntimeException> exception = new AtomicReference<>();
    private final long timeout;
    private final ExceptionTranslator exceptionTranslator;

    /**
     * @param coapRequest The CoAP request to observe.
     * @param timeoutInMs A response timeout(in millisecond) which is raised if neither a response or error happens (see
     *        https://github.com/eclipse/leshan/wiki/Request-Timeout).
     */
    public CoapSyncRequestObserver(Request coapRequest, long timeoutInMs, ExceptionTranslator exceptionTranslator) {
        super(coapRequest);
        this.timeout = timeoutInMs;
        this.exceptionTranslator = exceptionTranslator;
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
        if (!coapTimeout.get()) {
            exception.set(new RequestCanceledException("Request %s canceled", coapRequest.getURI()));
        }
        latch.countDown();
    }

    @Override
    public void onReject() {
        exception.set(new RequestRejectedException("Request %s rejected", coapRequest.getURI()));
        latch.countDown();
    }

    @Override
    public void onSendError(Throwable error) {
        Exception e = exceptionTranslator.translate(coapRequest, error);
        if (e instanceof TimeoutException) {
            coapTimeout.set(true);
        } else if (e instanceof RuntimeException) {
            exception.set((RuntimeException) e);
        } else {
            exception.set(new SendFailedException(e, "Request %s cannot be sent", coapRequest, e.getMessage()));
        }
        latch.countDown();
    }

    /**
     * Wait for the CoAP response.
     *
     * @return the CoAP response. The response can be <code>null</code> if the timeout expires (see
     *         https://github.com/eclipse/leshan/wiki/Request-Timeout).
     *
     * @throws InterruptedException if the thread was interrupted.
     * @throws RequestRejectedException if the request is rejected by foreign peer.
     * @throws RequestCanceledException if the request is cancelled.
     * @throws SendFailedException if the request can not be sent. E.g. error at CoAP or DTLS/UDP layer.
     */
    public Response waitForCoapResponse() throws InterruptedException {
        try {
            boolean timeElapsed = false;
            timeElapsed = !latch.await(timeout, TimeUnit.MILLISECONDS);
            if (timeElapsed || coapTimeout.get()) {
                coapTimeout.set(true);
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
