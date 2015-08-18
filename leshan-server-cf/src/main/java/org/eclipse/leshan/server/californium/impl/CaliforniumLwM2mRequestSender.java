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
 *     Sierra Wireless - initial API and implementation
 *******************************************************************************/
package org.eclipse.leshan.server.californium.impl;

import java.net.InetSocketAddress;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.californium.core.coap.MessageObserverAdapter;
import org.eclipse.californium.core.coap.Request;
import org.eclipse.californium.core.coap.Response;
import org.eclipse.californium.core.network.Endpoint;
import org.eclipse.leshan.core.model.LwM2mModel;
import org.eclipse.leshan.core.request.DownlinkRequest;
import org.eclipse.leshan.core.request.exception.RequestFailedException;
import org.eclipse.leshan.core.response.ErrorCallback;
import org.eclipse.leshan.core.response.LwM2mResponse;
import org.eclipse.leshan.core.response.ResponseCallback;
import org.eclipse.leshan.server.client.Client;
import org.eclipse.leshan.server.model.LwM2mModelProvider;
import org.eclipse.leshan.server.observation.ObservationRegistry;
import org.eclipse.leshan.server.request.LwM2mRequestSender;
import org.eclipse.leshan.util.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CaliforniumLwM2mRequestSender implements LwM2mRequestSender {

    private static final Logger LOG = LoggerFactory.getLogger(CaliforniumLwM2mRequestSender.class);

    private final Set<Endpoint> endpoints;
    private final ObservationRegistry observationRegistry;
    private final LwM2mModelProvider modelProvider;

    /**
     * @param endpoints the CoAP endpoints to use for sending requests
     * @param observationRegistry the registry for keeping track of observed resources
     * @param modelProvider provides the supported objects definitions
     */
    public CaliforniumLwM2mRequestSender(final Set<Endpoint> endpoints, final ObservationRegistry observationRegistry,
            LwM2mModelProvider modelProvider) {
        Validate.notNull(endpoints);
        Validate.notNull(observationRegistry);
        Validate.notNull(modelProvider);
        this.observationRegistry = observationRegistry;
        this.endpoints = endpoints;
        this.modelProvider = modelProvider;
    }

    @Override
    public <T extends LwM2mResponse> T send(final Client destination, final DownlinkRequest<T> request, Long timeout) {

        // Retrieve the objects definition
        final LwM2mModel model = modelProvider.getObjectModel(destination);

        // Create the CoAP request from LwM2m request
        final CoapRequestBuilder coapRequestBuilder = new CoapRequestBuilder(destination, model);
        request.accept(coapRequestBuilder);
        final Request coapRequest = coapRequestBuilder.getRequest();

        // Send CoAP request synchronously
        final SyncRequestObserver<T> syncMessageObserver = new SyncRequestObserver<T>(coapRequest, destination, timeout) {
            @Override
            public T buildResponse(final Response coapResponse) {
                // Build LwM2m response
                final LwM2mResponseBuilder<T> lwm2mResponseBuilder = new LwM2mResponseBuilder<T>(coapRequest,
                        coapResponse, client, model, observationRegistry);
                request.accept(lwm2mResponseBuilder);
                return lwm2mResponseBuilder.getResponse();
            }
        };
        coapRequest.addMessageObserver(syncMessageObserver);

        // Send CoAP request asynchronously
        final Endpoint endpoint = getEndpointForClient(destination);
        endpoint.sendRequest(coapRequest);

        // Wait for response, then return it
        return syncMessageObserver.waitForResponse();
    }

    @Override
    public <T extends LwM2mResponse> void send(final Client destination, final DownlinkRequest<T> request,
            final ResponseCallback<T> responseCallback, final ErrorCallback errorCallback) {
        // Retrieve the objects definition
        final LwM2mModel model = modelProvider.getObjectModel(destination);

        // Create the CoAP request from LwM2m request
        final CoapRequestBuilder coapRequestBuilder = new CoapRequestBuilder(destination, model);
        request.accept(coapRequestBuilder);
        final Request coapRequest = coapRequestBuilder.getRequest();

        // Add CoAP request callback
        coapRequest.addMessageObserver(new AsyncRequestObserver<T>(coapRequest, destination, responseCallback,
                errorCallback) {
            @Override
            public T buildResponse(final Response coapResponse) {
                // Build LwM2m response
                final LwM2mResponseBuilder<T> lwm2mResponseBuilder = new LwM2mResponseBuilder<T>(coapRequest,
                        coapResponse, client, model, observationRegistry);
                request.accept(lwm2mResponseBuilder);
                return lwm2mResponseBuilder.getResponse();
            }
        });

        // Send CoAP request asynchronously
        final Endpoint endpoint = getEndpointForClient(destination);
        endpoint.sendRequest(coapRequest);
    }

    /**
     * Gets the CoAP endpoint that should be used to communicate with a given client.
     *
     * @param client the client
     * @return the CoAP endpoint bound to the same network address and port that the client connected to during
     *         registration. If no such CoAP endpoint is available, the first CoAP endpoint from the list of registered
     *         endpoints is returned
     */
    private Endpoint getEndpointForClient(final Client client) {
        for (final Endpoint ep : endpoints) {
            final InetSocketAddress endpointAddress = ep.getAddress();
            if (endpointAddress.equals(client.getRegistrationEndpointAddress())) {
                return ep;
            }
        }
        throw new IllegalStateException("can't find the client endpoint for address : "
                + client.getRegistrationEndpointAddress());
    }

    // ////// Request Observer Class definition/////////////
    // TODO leshan-code-cf: All Request Observer should be factorize in a leshan-core-cf project.
    // duplicate from org.eclipse.leshan.client.californium.impl.CaliforniumLwM2mClientRequestSender
    private abstract class AbstractRequestObserver<T extends LwM2mResponse> extends MessageObserverAdapter {
        Request coapRequest;
        Client client;

        public AbstractRequestObserver(final Request coapRequest, final Client client) {
            this.coapRequest = coapRequest;
            this.client = client;
        }

        public abstract T buildResponse(Response coapResponse);
    }

    private abstract class AsyncRequestObserver<T extends LwM2mResponse> extends AbstractRequestObserver<T> {

        ResponseCallback<T> responseCallback;
        ErrorCallback errorCallback;

        AsyncRequestObserver(final Request coapRequest, final Client client,
                final ResponseCallback<T> responseCallback, final ErrorCallback errorCallback) {
            super(coapRequest, client);
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
            errorCallback.onError(new RequestFailedException("Rejected request"));
        }

    }

    private abstract class SyncRequestObserver<T extends LwM2mResponse> extends AbstractRequestObserver<T> {

        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<T> ref = new AtomicReference<T>(null);
        AtomicBoolean coapTimeout = new AtomicBoolean(false);
        AtomicReference<RuntimeException> exception = new AtomicReference<>();
        Long timeout;

        public SyncRequestObserver(final Request coapRequest, final Client client, final Long timeout) {
            super(coapRequest, client);
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

        public T waitForResponse() {
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
            } catch (final InterruptedException e) {
                // no idea why some other thread should have interrupted this thread
                // but anyway, go ahead as if the timeout had been reached
                LOG.debug("Caught an unexpected InterruptedException during execution of CoAP request", e);
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
}
