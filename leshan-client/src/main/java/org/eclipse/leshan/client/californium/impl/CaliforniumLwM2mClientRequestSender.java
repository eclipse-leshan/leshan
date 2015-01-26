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
package org.eclipse.leshan.client.californium.impl;

import java.net.InetSocketAddress;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.californium.core.coap.MessageObserverAdapter;
import org.eclipse.californium.core.coap.Request;
import org.eclipse.californium.core.coap.Response;
import org.eclipse.californium.core.network.Endpoint;
import org.eclipse.leshan.LinkObject;
import org.eclipse.leshan.client.request.LwM2mClientRequestSender;
import org.eclipse.leshan.core.request.UplinkRequest;
import org.eclipse.leshan.core.request.exception.RejectionException;
import org.eclipse.leshan.core.request.exception.RequestTimeoutException;
import org.eclipse.leshan.core.response.ExceptionConsumer;
import org.eclipse.leshan.core.response.LwM2mResponse;
import org.eclipse.leshan.core.response.ResponseConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CaliforniumLwM2mClientRequestSender implements LwM2mClientRequestSender {
    private static final Logger LOG = LoggerFactory.getLogger(CaliforniumLwM2mClientRequestSender.class);
    private static final int COAP_REQUEST_TIMEOUT_MILLIS = 5000;

    private final Endpoint clientEndpoint;
    private final InetSocketAddress serverAddress;
    private final LinkObject[] clientObjectModel;
    private final long timeoutMillis;

    public CaliforniumLwM2mClientRequestSender(final Endpoint endpoint, final InetSocketAddress serverAddress,
            final LinkObject... linkObjects) {
        this.clientEndpoint = endpoint;
        this.serverAddress = serverAddress;
        this.clientObjectModel = linkObjects;
        this.timeoutMillis = COAP_REQUEST_TIMEOUT_MILLIS;
    }

    @Override
    public <T extends LwM2mResponse> T send(final UplinkRequest<T> request) {
        // Create the CoAP request from LwM2m request
        final CoapClientRequestBuilder coapClientRequestBuilder = new CoapClientRequestBuilder(serverAddress,
                clientObjectModel);
        request.accept(coapClientRequestBuilder);
        // TODO manage invalid parameters
        // if (!coapClientRequestBuilder.areParametersValid()) {
        // return OperationResponse.failure(ResponseCode.INTERNAL_SERVER_ERROR,
        // "Request has invalid parameters.  Not sending.");
        // }
        final Request coapRequest = coapClientRequestBuilder.getRequest();

        // Send CoAP request synchronously
        final SyncRequestObserver<T> syncMessageObserver = new SyncRequestObserver<T>(coapRequest, timeoutMillis) {
            @Override
            public T buildResponse(final Response coapResponse) {
                // Build LwM2m response
                final LwM2mClientResponseBuilder<T> lwm2mResponseBuilder = new LwM2mClientResponseBuilder<T>(
                        coapRequest, coapResponse);
                request.accept(lwm2mResponseBuilder);
                return lwm2mResponseBuilder.getResponse();
            }
        };
        coapRequest.addMessageObserver(syncMessageObserver);

        // Send CoAP request asynchronously
        clientEndpoint.sendRequest(coapRequest);

        // Wait for response, then return it
        return syncMessageObserver.waitForResponse();
    }

    @Override
    public <T extends LwM2mResponse> void send(final UplinkRequest<T> request,
            final ResponseConsumer<T> responseCallback, final ExceptionConsumer errorCallback) {
        // Create the CoAP request from LwM2m request
        final CoapClientRequestBuilder coapClientRequestBuilder = new CoapClientRequestBuilder(serverAddress,
                clientObjectModel);
        request.accept(coapClientRequestBuilder);
        // TODO manage invalid parameters
        // if (!coapClientRequestBuilder.areParametersValid()) {
        // responseCallback.onFailure(OperationResponse.failure(ResponseCode.INTERNAL_SERVER_ERROR,
        // "Request has invalid parameters.  Not sending."));
        // return;
        // }
        final Request coapRequest = coapClientRequestBuilder.getRequest();

        // Add CoAP request callback
        coapRequest.addMessageObserver(new AsyncRequestObserver<T>(coapRequest, responseCallback, errorCallback) {

            @Override
            public T buildResponse(final Response coapResponse) {
                // Build LwM2m response
                final LwM2mClientResponseBuilder<T> lwm2mResponseBuilder = new LwM2mClientResponseBuilder<T>(
                        coapRequest, coapResponse);
                request.accept(lwm2mResponseBuilder);
                return lwm2mResponseBuilder.getResponse();
            }
        });

        // Send CoAP request asynchronously
        clientEndpoint.sendRequest(coapRequest);
    }

    // ////// Request Observer Class definition/////////////

    private abstract class AbstractRequestObserver<T extends LwM2mResponse> extends MessageObserverAdapter {
        Request coapRequest;

        public AbstractRequestObserver(final Request coapRequest) {
            this.coapRequest = coapRequest;
        }

        public abstract T buildResponse(Response coapResponse);
    }

    private abstract class AsyncRequestObserver<T extends LwM2mResponse> extends AbstractRequestObserver<T> {

        ResponseConsumer<T> responseCallback;
        ExceptionConsumer errorCallback;

        AsyncRequestObserver(final Request coapRequest, final ResponseConsumer<T> responseCallback,
                final ExceptionConsumer errorCallback) {
            super(coapRequest);
            this.responseCallback = responseCallback;
            this.errorCallback = errorCallback;
        }

        @Override
        public void onResponse(final Response coapResponse) {
            LOG.debug("Received coap response: {}", coapResponse);
            try {
                final T lwM2mResponseT = buildResponse(coapResponse);
                if (lwM2mResponseT != null) {
                    responseCallback.accept(lwM2mResponseT);
                }
            } catch (final Exception e) {
                errorCallback.accept(e);
            } finally {
                coapRequest.removeMessageObserver(this);
            }
        }

        @Override
        public void onTimeout() {
            errorCallback.accept(new TimeoutException());
        }

        @Override
        public void onCancel() {
            errorCallback.accept(new CancellationException());
        }

        @Override
        public void onReject() {
            errorCallback.accept(new RejectionException());
        }

    }

    private abstract class SyncRequestObserver<T extends LwM2mResponse> extends AbstractRequestObserver<T> {

        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<T> ref = new AtomicReference<T>(null);
        AtomicBoolean coapTimeout = new AtomicBoolean(false);
        AtomicReference<RuntimeException> exception = new AtomicReference<>();

        long timeout;

        public SyncRequestObserver(final Request coapRequest, final long timeout) {
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
            latch.countDown();
        }

        @Override
        public void onReject() {
            latch.countDown();
        }

        public T waitForResponse() {
            try {
                final boolean latchTimeout = latch.await(timeout, TimeUnit.MILLISECONDS);
                if (!latchTimeout || coapTimeout.get()) {
                    coapRequest.cancel();
                    if (exception.get() != null) {
                        throw exception.get();
                    } else {
                        throw new RequestTimeoutException(coapRequest.getURI(), timeout);
                    }
                }
            } catch (final InterruptedException e) {
                // no idea why some other thread should have interrupted this thread
                // but anyway, go ahead as if the timeout had been reached
                LOG.debug("Caught an unexpected InterruptedException during execution of CoAP request", e);
            } finally {
                coapRequest.removeMessageObserver(this);
            }

            if (exception.get() != null) {
                throw exception.get();
            }
            return ref.get();
        }
    }
}
