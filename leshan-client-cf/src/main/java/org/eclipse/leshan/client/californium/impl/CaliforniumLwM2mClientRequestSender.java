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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.californium.core.coap.MessageObserverAdapter;
import org.eclipse.californium.core.coap.Request;
import org.eclipse.californium.core.coap.Response;
import org.eclipse.californium.core.network.Endpoint;
import org.eclipse.leshan.client.request.LwM2mClientRequestSender;
import org.eclipse.leshan.core.request.UplinkRequest;
import org.eclipse.leshan.core.request.exception.RequestFailedException;
import org.eclipse.leshan.core.response.ErrorCallback;
import org.eclipse.leshan.core.response.LwM2mResponse;
import org.eclipse.leshan.core.response.ResponseCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CaliforniumLwM2mClientRequestSender implements LwM2mClientRequestSender {
    private static final Logger LOG = LoggerFactory.getLogger(CaliforniumLwM2mClientRequestSender.class);

    private final Endpoint nonSecureEndpoint;
    private final Endpoint secureEndpoint;

    public CaliforniumLwM2mClientRequestSender(final Endpoint secureEndpoint, final Endpoint nonSecureEndpoint) {
        this.secureEndpoint = secureEndpoint;
        this.nonSecureEndpoint = nonSecureEndpoint;
    }

    @Override
    public <T extends LwM2mResponse> T send(final InetSocketAddress serverAddress, final boolean secure,
            final UplinkRequest<T> request, Long timeout) throws InterruptedException {
        // Create the CoAP request from LwM2m request
        final CoapClientRequestBuilder coapClientRequestBuilder = new CoapClientRequestBuilder(serverAddress);
        request.accept(coapClientRequestBuilder);
        // TODO manage invalid parameters
        // if (!coapClientRequestBuilder.areParametersValid()) {
        // return OperationResponse.failure(ResponseCode.INTERNAL_SERVER_ERROR,
        // "Request has invalid parameters.  Not sending.");
        // }
        final Request coapRequest = coapClientRequestBuilder.getRequest();

        // Send CoAP request synchronously
        final SyncRequestObserver<T> syncMessageObserver = new SyncRequestObserver<T>(coapRequest, timeout) {
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
        if (secure)
            secureEndpoint.sendRequest(coapRequest);
        else
            nonSecureEndpoint.sendRequest(coapRequest);

        // Wait for response, then return it
        return syncMessageObserver.waitForResponse();
    }

    @Override
    public <T extends LwM2mResponse> void send(final InetSocketAddress serverAddress, final boolean secure,
            final UplinkRequest<T> request, final ResponseCallback<T> responseCallback,
            final ErrorCallback errorCallback) {
        // Create the CoAP request from LwM2m request
        final CoapClientRequestBuilder coapClientRequestBuilder = new CoapClientRequestBuilder(serverAddress);
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
        if (secure)
            secureEndpoint.sendRequest(coapRequest);
        else
            nonSecureEndpoint.sendRequest(coapRequest);
    }

    // ////// Request Observer Class definition/////////////
    // TODO leshan-code-cf: All Request Observer should be factorize in a leshan-core-cf project.
    // duplicate from org.eclipse.leshan.server.californium.impl.CaliforniumLwM2mRequestSender
    private abstract class AbstractRequestObserver<T extends LwM2mResponse> extends MessageObserverAdapter {
        Request coapRequest;

        public AbstractRequestObserver(final Request coapRequest) {
            this.coapRequest = coapRequest;
        }

        public abstract T buildResponse(Response coapResponse);
    }

    private abstract class AsyncRequestObserver<T extends LwM2mResponse> extends AbstractRequestObserver<T> {

        ResponseCallback<T> responseCallback;
        ErrorCallback errorCallback;

        AsyncRequestObserver(final Request coapRequest, final ResponseCallback<T> responseCallback,
                final ErrorCallback errorCallback) {
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
                    responseCallback.onResponse(lwM2mResponseT);
                }
            } catch (final Exception e) {
                errorCallback.onError(e);
            } finally {
                coapRequest.removeMessageObserver(this);
            }
        }

        @Override
        public void onTimeout() {
            errorCallback.onError(new org.eclipse.leshan.core.request.exception.TimeoutException());
        }

        @Override
        public void onCancel() {
            errorCallback.onError(new RequestFailedException("Canceled request"));
        }

        @Override
        public void onReject() {
            errorCallback.onError(new RequestFailedException("Reject request"));
        }

    }

    private abstract class SyncRequestObserver<T extends LwM2mResponse> extends AbstractRequestObserver<T> {

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
                throw exception.get();
            }
            return ref.get();
        }
    }
}
