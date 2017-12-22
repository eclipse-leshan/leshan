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

import java.util.Set;
import java.util.SortedMap;
import java.util.concurrent.ConcurrentNavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;

import org.eclipse.californium.core.coap.MessageObserverAdapter;
import org.eclipse.californium.core.coap.Request;
import org.eclipse.californium.core.coap.Response;
import org.eclipse.californium.core.network.Endpoint;
import org.eclipse.leshan.core.node.codec.LwM2mNodeDecoder;
import org.eclipse.leshan.core.node.codec.LwM2mNodeEncoder;
import org.eclipse.leshan.server.model.LwM2mModelProvider;
import org.eclipse.leshan.server.registration.Registration;
import org.eclipse.leshan.util.Validate;

/**
 * A sender which stores request cancel to be able to cancel it if needed. This is generally used to avoid
 * retransmission after de-registration.
 */
public class CaliforniumLwM2mRequestSender extends SimpleLwM2mRequestSender {

    // A map which contains all pending CoAP requests
    // This is mainly used to cancel request and avoid retransmission on de-registration
    private final ConcurrentNavigableMap<String/* registrationId#requestId */, Request /* pending coap Request */> pendingRequests = new ConcurrentSkipListMap<>();

    public CaliforniumLwM2mRequestSender(Set<Endpoint> endpoints, LwM2mModelProvider modelProvider,
            LwM2mNodeEncoder encoder, LwM2mNodeDecoder decoder) {
        super(endpoints, modelProvider, encoder, decoder);
    }

    @Override
    protected void addPendingRequest(String registrationId, Request coapRequest) {
        Validate.notNull(registrationId);
        CleanerMessageObserver observer = new CleanerMessageObserver(registrationId, coapRequest);
        coapRequest.addMessageObserver(observer);
        pendingRequests.put(observer.getRequestKey(), coapRequest);
    }

    @Override
    public void cancelPendingRequests(Registration registration) {
        Validate.notNull(registration);
        String registrationId = registration.getId();
        SortedMap<String, Request> requests = pendingRequests.subMap(getFloorKey(registrationId),
                getCeilingKey(registrationId));
        for (Request coapRequest : requests.values()) {
            coapRequest.cancel();
        }
        requests.clear();
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

    private void removePendingRequest(String key, Request coapRequest) {
        Validate.notNull(key);
        pendingRequests.remove(key, coapRequest);
    }

    private class CleanerMessageObserver extends MessageObserverAdapter {

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
        protected void failed() {
            removePendingRequest(requestKey, coapRequest);
        }

        @Override
        public void onCancel() {
            removePendingRequest(requestKey, coapRequest);
        }
    }
}
