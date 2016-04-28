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
import java.util.SortedMap;
import java.util.concurrent.ConcurrentNavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;

import org.eclipse.californium.core.coap.MessageObserver;
import org.eclipse.californium.core.coap.Request;
import org.eclipse.californium.core.coap.Response;
import org.eclipse.californium.core.network.Endpoint;
import org.eclipse.leshan.core.model.LwM2mModel;
import org.eclipse.leshan.core.request.DownlinkRequest;
import org.eclipse.leshan.core.response.ErrorCallback;
import org.eclipse.leshan.core.response.LwM2mResponse;
import org.eclipse.leshan.core.response.ResponseCallback;
import org.eclipse.leshan.server.client.Client;
import org.eclipse.leshan.server.model.LwM2mModelProvider;
import org.eclipse.leshan.server.observation.ObservationRegistry;
import org.eclipse.leshan.server.request.LwM2mRequestSender;
import org.eclipse.leshan.util.Validate;

public class CaliforniumLwM2mRequestSender implements LwM2mRequestSender {

    private final Set<Endpoint> endpoints;
    private final ObservationRegistry observationRegistry;
    private final LwM2mModelProvider modelProvider;
    // A map which contains all pending CoAP requests
    // This is mainly used to cancel request and avoid retransmission on de-registration
    private final ConcurrentNavigableMap<String/* registrationId#requestId */, Request /* pending coap Request */> pendingRequests = new ConcurrentSkipListMap<>();

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
    public <T extends LwM2mResponse> T send(final Client destination, final DownlinkRequest<T> request, Long timeout)
            throws InterruptedException {

        // Retrieve the objects definition
        final LwM2mModel model = modelProvider.getObjectModel(destination);

        // Create the CoAP request from LwM2m request
        final CoapRequestBuilder coapRequestBuilder = new CoapRequestBuilder(
                new InetSocketAddress(destination.getAddress(), destination.getPort()), destination.getRootPath(),
                model);
        request.accept(coapRequestBuilder);
        final Request coapRequest = coapRequestBuilder.getRequest();

        // Send CoAP request synchronously
        final SyncRequestObserver<T> syncMessageObserver = new SyncRequestObserver<T>(coapRequest, timeout) {
            @Override
            public T buildResponse(final Response coapResponse) {
                // Build LwM2m response
                final LwM2mResponseBuilder<T> lwm2mResponseBuilder = new LwM2mResponseBuilder<T>(coapRequest,
                        coapResponse, destination, model, observationRegistry);
                request.accept(lwm2mResponseBuilder);
                return lwm2mResponseBuilder.getResponse();
            }
        };
        coapRequest.addMessageObserver(syncMessageObserver);

        // Store pending request to cancel it on deregistration
        addPendingRequest(destination.getRegistrationId(), coapRequest);

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
        final CoapRequestBuilder coapRequestBuilder = new CoapRequestBuilder(
                new InetSocketAddress(destination.getAddress(), destination.getPort()), destination.getRootPath(),
                model);
        request.accept(coapRequestBuilder);
        final Request coapRequest = coapRequestBuilder.getRequest();

        // Add CoAP request callback
        coapRequest.addMessageObserver(new AsyncRequestObserver<T>(coapRequest, responseCallback, errorCallback) {
            @Override
            public T buildResponse(final Response coapResponse) {
                // Build LwM2m response
                final LwM2mResponseBuilder<T> lwm2mResponseBuilder = new LwM2mResponseBuilder<T>(coapRequest,
                        coapResponse, destination, model, observationRegistry);
                request.accept(lwm2mResponseBuilder);
                return lwm2mResponseBuilder.getResponse();
            }
        });

        // Store pending request to cancel it on deregistration
        addPendingRequest(destination.getRegistrationId(), coapRequest);

        // Send CoAP request asynchronously
        final Endpoint endpoint = getEndpointForClient(destination);
        endpoint.sendRequest(coapRequest);
    }

    private String getFloorKey(String registrationId) {
        // The key format is regid#int, So we need a key which is always before this pattern (in natural order).
        return registrationId + '#';
    }

    private String getCeilingKey(String registrationId) {
        // The key format is regid#int, So we need a key which is always after this pattern (in natural order).
        return registrationId + "#A";
    }

    private String getKey(String registrationId, int requestId) {
        return registrationId + '#' + requestId;
    }

    public void cancelPendingRequests(String registrationId) {
        Validate.notNull(registrationId);
        SortedMap<String, Request> requests = pendingRequests.subMap(getFloorKey(registrationId),
                getCeilingKey(registrationId));
        for (Request coapRequest : requests.values()) {
            coapRequest.cancel();
        }
        requests.clear();
    }

    private void addPendingRequest(String registrationId, Request coapRequest) {
        Validate.notNull(registrationId);
        CleanerMessageObserver observer = new CleanerMessageObserver(registrationId, coapRequest);
        coapRequest.addMessageObserver(observer);
        pendingRequests.put(observer.getRequestKey(), coapRequest);
    }

    private void removePendingRequest(String key, Request coapRequest) {
        Validate.notNull(key);
        pendingRequests.remove(key, coapRequest);
    }

    private class CleanerMessageObserver implements MessageObserver {

        private final String requestKey;
        private final Request coapRequest;

        public CleanerMessageObserver(String registrationId, Request coapRequest) {
            super();
            requestKey = getKey(registrationId, hashCode());
            this.coapRequest = coapRequest;
        }

        public String getRequestKey() {
            return requestKey;
        }

        @Override
        public void onRetransmission() {
        }

        @Override
        public void onResponse(Response response) {
            removePendingRequest(requestKey, coapRequest);
        }

        @Override
        public void onAcknowledgement() {
            // we can remove the request on acknowledgement as we only want to avoid CoAP retransmission.
            removePendingRequest(requestKey, coapRequest);
        }

        @Override
        public void onReject() {
            removePendingRequest(requestKey, coapRequest);
        }

        @Override
        public void onTimeout() {
            removePendingRequest(requestKey, coapRequest);
        }

        @Override
        public void onCancel() {
            removePendingRequest(requestKey, coapRequest);
        }
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
        throw new IllegalStateException(
                "can't find the client endpoint for address : " + client.getRegistrationEndpointAddress());
    }
}
