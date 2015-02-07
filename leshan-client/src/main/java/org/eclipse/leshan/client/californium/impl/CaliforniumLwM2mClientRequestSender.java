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
import java.util.logging.Logger;

import org.eclipse.californium.core.coap.CoAP.ResponseCode;
import org.eclipse.californium.core.coap.MessageObserverAdapter;
import org.eclipse.californium.core.coap.Request;
import org.eclipse.californium.core.coap.Response;
import org.eclipse.californium.core.network.Endpoint;
import org.eclipse.leshan.LinkObject;
import org.eclipse.leshan.client.request.LwM2mClientRequest;
import org.eclipse.leshan.client.request.LwM2mClientRequestSender;
import org.eclipse.leshan.client.response.OperationResponse;
import org.eclipse.leshan.client.util.ResponseCallback;

public class CaliforniumLwM2mClientRequestSender implements LwM2mClientRequestSender {
    private static final Logger LOG = Logger.getLogger(CaliforniumLwM2mClientRequestSender.class.getCanonicalName());
    private final Endpoint clientEndpoint;
    private final InetSocketAddress serverAddress;
    private final LinkObject[] clientObjectModel;

    public CaliforniumLwM2mClientRequestSender(final Endpoint endpoint, final InetSocketAddress serverAddress,
            final LinkObject... linkObjects) {
        this.clientEndpoint = endpoint;
        this.serverAddress = serverAddress;
        this.clientObjectModel = linkObjects;
    }

    @Override
    public OperationResponse send(final LwM2mClientRequest request) {
        // Create the CoAP request from LwM2m request
        final CoapClientRequestBuilder coapClientRequestBuilder = new CoapClientRequestBuilder(serverAddress,
                clientObjectModel);
        request.accept(coapClientRequestBuilder);
        if (!coapClientRequestBuilder.areParametersValid()) {
            return OperationResponse.failure(ResponseCode.INTERNAL_SERVER_ERROR,
                    "Request has invalid parameters.  Not sending.");
        }
        final Request coapRequest = coapClientRequestBuilder.getRequest();

        // Send CoAP request synchronously
        final SyncRequestObserver syncMessageObserver = new SyncRequestObserver(coapRequest,
                coapClientRequestBuilder.getTimeout()) {
            @Override
            public OperationResponse buildResponse(final Response coapResponse) {
                // Build LwM2m response
                final LwM2mClientResponseBuilder lwm2mResponseBuilder = new LwM2mClientResponseBuilder(coapRequest,
                        coapResponse, CaliforniumLwM2mClientRequestSender.this);
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
    public void send(final LwM2mClientRequest request, final ResponseCallback responseCallback) {
        // Create the CoAP request from LwM2m request
        final CoapClientRequestBuilder coapClientRequestBuilder = new CoapClientRequestBuilder(serverAddress,
                clientObjectModel);
        request.accept(coapClientRequestBuilder);
        if (!coapClientRequestBuilder.areParametersValid()) {
            responseCallback.onFailure(OperationResponse.failure(ResponseCode.INTERNAL_SERVER_ERROR,
                    "Request has invalid parameters.  Not sending."));
            return;
        }
        final Request coapRequest = coapClientRequestBuilder.getRequest();

        // Add CoAP request callback
        coapRequest.addMessageObserver(new AsyncRequestObserver(coapRequest, responseCallback) {

            @Override
            public OperationResponse buildResponse(final Response coapResponse) {
                // Build LwM2m response
                final LwM2mClientResponseBuilder lwm2mResponseBuilder = new LwM2mClientResponseBuilder(coapRequest,
                        coapResponse, CaliforniumLwM2mClientRequestSender.this);
                request.accept(lwm2mResponseBuilder);
                return lwm2mResponseBuilder.getResponse();
            }
        });

        // Send CoAP request asynchronously
        clientEndpoint.sendRequest(coapRequest);
    }

    // ////// Request Observer Class definition/////////////

    private abstract class AbstractRequestObserver extends MessageObserverAdapter {
        protected Request coapRequest;

        public AbstractRequestObserver(final Request coapRequest) {
            this.coapRequest = coapRequest;
        }

        public abstract OperationResponse buildResponse(Response coapResponse);
    }

    private abstract class AsyncRequestObserver extends AbstractRequestObserver {

        protected ResponseCallback responseCallback;

        AsyncRequestObserver(final Request coapRequest, final ResponseCallback responseCallback) {
            super(coapRequest);
            this.responseCallback = responseCallback;
        }

        @Override
        public void onResponse(final Response coapResponse) {
            try {
                final OperationResponse lwM2mResponseT = buildResponse(coapResponse);
                if (lwM2mResponseT != null) {
                    responseCallback.onSuccess(lwM2mResponseT);
                }
            } catch (final Exception e) {
                responseCallback.onFailure(OperationResponse.failure(ResponseCode.INTERNAL_SERVER_ERROR,
                        e.getLocalizedMessage()));
            } finally {
                coapRequest.removeMessageObserver(this);
            }
        }

        @Override
        public void onTimeout() {
            // TODO just have the responseCallback work with just an exception
            responseCallback.onFailure(OperationResponse.failure(ResponseCode.GATEWAY_TIMEOUT, "Request Timed Out."));
        }

        @Override
        public void onCancel() {
            responseCallback.onFailure(OperationResponse.failure(ResponseCode.FORBIDDEN, "Request Cancelled."));
        }

        @Override
        public void onReject() {
            responseCallback.onFailure(OperationResponse.failure(ResponseCode.FORBIDDEN, "Request Rejected."));
        }

    }

    private abstract class SyncRequestObserver extends AbstractRequestObserver {

        protected CountDownLatch latch = new CountDownLatch(1);
        protected AtomicReference<OperationResponse> ref = new AtomicReference<OperationResponse>(null);
        protected AtomicBoolean coapTimeout = new AtomicBoolean(false);
        protected AtomicReference<RuntimeException> exception = new AtomicReference<>();

        protected long timeout;

        public SyncRequestObserver(final Request coapRequest, final long timeout) {
            super(coapRequest);
            this.timeout = timeout;
        }

        @Override
        public void onResponse(final Response coapResponse) {
            LOG.info("Received coap response: " + coapResponse);
            try {
                final OperationResponse lwM2mResponseT = buildResponse(coapResponse);
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

        public OperationResponse waitForResponse() {
            try {
                final boolean latchTimeout = latch.await(timeout, TimeUnit.MILLISECONDS);
                if (!latchTimeout || coapTimeout.get()) {
                    coapRequest.cancel();
                    if (exception.get() != null) {
                        throw exception.get();
                    } else {
                        throw new RuntimeException("Request Timed Out: " + coapRequest.getURI() + " (timeout)");
                    }
                }
            } catch (final InterruptedException e) {
                // no idea why some other thread should have interrupted this thread
                // but anyway, go ahead as if the timeout had been reached
                LOG.info("Caught an unexpected InterruptedException during execution of CoAP request " + e);
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
