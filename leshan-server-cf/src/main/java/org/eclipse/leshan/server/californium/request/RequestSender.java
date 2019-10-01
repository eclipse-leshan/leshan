/*******************************************************************************
 * Copyright (c) 2019 Sierra Wireless and others.
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
package org.eclipse.leshan.server.californium.request;

import java.util.SortedMap;
import java.util.concurrent.ConcurrentNavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.atomic.AtomicLong;

import org.eclipse.californium.core.coap.MessageObserver;
import org.eclipse.californium.core.coap.MessageObserverAdapter;
import org.eclipse.californium.core.coap.Request;
import org.eclipse.californium.core.coap.Response;
import org.eclipse.californium.core.network.Endpoint;
import org.eclipse.californium.elements.EndpointContext;
import org.eclipse.leshan.core.californium.AsyncRequestObserver;
import org.eclipse.leshan.core.californium.CoapAsyncRequestObserver;
import org.eclipse.leshan.core.californium.CoapResponseCallback;
import org.eclipse.leshan.core.californium.CoapSyncRequestObserver;
import org.eclipse.leshan.core.californium.EndpointContextUtil;
import org.eclipse.leshan.core.californium.SyncRequestObserver;
import org.eclipse.leshan.core.model.LwM2mModel;
import org.eclipse.leshan.core.node.codec.LwM2mNodeDecoder;
import org.eclipse.leshan.core.node.codec.LwM2mNodeEncoder;
import org.eclipse.leshan.core.request.DownlinkRequest;
import org.eclipse.leshan.core.request.Identity;
import org.eclipse.leshan.core.response.ErrorCallback;
import org.eclipse.leshan.core.response.LwM2mResponse;
import org.eclipse.leshan.core.response.ResponseCallback;
import org.eclipse.leshan.server.californium.bootstrap.CaliforniumLwM2mBootstrapRequestSender;
import org.eclipse.leshan.util.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This sender is able to send LWM2M or CoAP request in a synchronous or asynchronous way.
 * <p>
 * It can also link requests to a kind of "session" and cancel all ongoing requests associated to a given "session".
 *
 */
public class RequestSender {
    static final Logger LOG = LoggerFactory.getLogger(CaliforniumLwM2mBootstrapRequestSender.class);

    private final Endpoint nonSecureEndpoint;
    private final Endpoint secureEndpoint;
    private final LwM2mNodeDecoder decoder;
    private final LwM2mNodeEncoder encoder;

    // A map which contains all ongoing CoAP requests
    // This is used to be able to cancel request
    private final ConcurrentNavigableMap<String/* sessionId#requestId */, Request /* ongoing coap Request */> ongoingRequests = new ConcurrentSkipListMap<>();

    public RequestSender(Endpoint secureEndpoint, Endpoint nonSecureEndpoint, LwM2mNodeEncoder encoder,
            LwM2mNodeDecoder decoder) {
        this.secureEndpoint = secureEndpoint;
        this.nonSecureEndpoint = nonSecureEndpoint;
        this.encoder = encoder;
        this.decoder = decoder;
    }

    public <T extends LwM2mResponse> T sendLwm2mRequest(final String endpointName, Identity destination,
            String sessionId, final LwM2mModel model, String rootPath, final DownlinkRequest<T> request, long timeout)
            throws InterruptedException {

        // Create the CoAP request from LwM2m request
        CoapRequestBuilder coapClientRequestBuilder = new CoapRequestBuilder(destination, rootPath, sessionId,
                endpointName, model, encoder);
        request.accept(coapClientRequestBuilder);
        final Request coapRequest = coapClientRequestBuilder.getRequest();

        // Send CoAP request synchronously
        SyncRequestObserver<T> syncMessageObserver = new SyncRequestObserver<T>(coapRequest, timeout) {
            @Override
            public T buildResponse(Response coapResponse) {
                // Build LwM2m response
                LwM2mResponseBuilder<T> lwm2mResponseBuilder = new LwM2mResponseBuilder<>(coapRequest, coapResponse,
                        endpointName, model, decoder);
                request.accept(lwm2mResponseBuilder);
                return lwm2mResponseBuilder.getResponse();
            }
        };
        coapRequest.addMessageObserver(syncMessageObserver);

        // Store pending request to be able to cancel it later
        addOngoingRequest(sessionId, coapRequest);

        // Send CoAP request asynchronously
        if (destination.isSecure())
            secureEndpoint.sendRequest(coapRequest);
        else
            nonSecureEndpoint.sendRequest(coapRequest);

        // Wait for response, then return it
        return syncMessageObserver.waitForResponse();
    }

    public <T extends LwM2mResponse> void sendLwm2mRequest(final String endpointName, Identity destination,
            String sessionId, final LwM2mModel model, String rootPath, final DownlinkRequest<T> request, long timeout,
            ResponseCallback<T> responseCallback, ErrorCallback errorCallback) {
        // Create the CoAP request from LwM2m request
        CoapRequestBuilder coapClientRequestBuilder = new CoapRequestBuilder(destination, rootPath, sessionId,
                endpointName, model, encoder);
        request.accept(coapClientRequestBuilder);
        final Request coapRequest = coapClientRequestBuilder.getRequest();

        // Add CoAP request callback
        MessageObserver obs = new AsyncRequestObserver<T>(coapRequest, responseCallback, errorCallback, timeout) {
            @Override
            public T buildResponse(Response coapResponse) {
                // Build LwM2m response
                LwM2mResponseBuilder<T> lwm2mResponseBuilder = new LwM2mResponseBuilder<>(coapRequest, coapResponse,
                        endpointName, model, decoder);
                request.accept(lwm2mResponseBuilder);
                return lwm2mResponseBuilder.getResponse();
            }
        };
        coapRequest.addMessageObserver(obs);

        // Store pending request to be able to cancel it later
        addOngoingRequest(sessionId, coapRequest);

        // Send CoAP request asynchronously
        if (destination.isSecure())
            secureEndpoint.sendRequest(coapRequest);
        else
            nonSecureEndpoint.sendRequest(coapRequest);
    }

    public Response sendCoapRequest(Identity destination, String sessionId, Request coapRequest, long timeout)
            throws InterruptedException {

        // Define destination
        EndpointContext context = EndpointContextUtil.extractContext(destination);
        coapRequest.setDestinationContext(context);

        // Send CoAP request synchronously
        CoapSyncRequestObserver syncMessageObserver = new CoapSyncRequestObserver(coapRequest, timeout);
        coapRequest.addMessageObserver(syncMessageObserver);

        // Store pending request to be able to cancel it later
        addOngoingRequest(sessionId, coapRequest);

        // Send CoAP request asynchronously
        if (destination.isSecure())
            secureEndpoint.sendRequest(coapRequest);
        else
            nonSecureEndpoint.sendRequest(coapRequest);

        // Wait for response, then return it
        return syncMessageObserver.waitForCoapResponse();
    }

    public void sendCoapRequest(Identity destination, String sessionId, Request coapRequest, long timeout,
            CoapResponseCallback responseCallback, ErrorCallback errorCallback) {

        // Define destination
        EndpointContext context = EndpointContextUtil.extractContext(destination);
        coapRequest.setDestinationContext(context);

        // Add CoAP request callback
        MessageObserver obs = new CoapAsyncRequestObserver(coapRequest, responseCallback, errorCallback, timeout);
        coapRequest.addMessageObserver(obs);

        // Store pending request to be able to cancel it later
        addOngoingRequest(sessionId, coapRequest);

        // Send CoAP request asynchronously
        if (destination.isSecure())
            secureEndpoint.sendRequest(coapRequest);
        else
            nonSecureEndpoint.sendRequest(coapRequest);
    }

    public void cancelRequests(String sessionID) {
        Validate.notNull(sessionID);
        SortedMap<String, Request> requests = ongoingRequests.subMap(getFloorKey(sessionID), getCeilingKey(sessionID));
        for (Request coapRequest : requests.values()) {
            coapRequest.cancel();
        }
        requests.clear();
    }

    private static String getFloorKey(String sessionID) {
        // The key format is sessionid#long, So we need a key which is always before this pattern (in natural order).
        return sessionID + '#';
    }

    private static String getCeilingKey(String sessionID) {
        // The key format is sessionid#long, So we need a key which is always after this pattern (in natural order).
        return sessionID + "#A";
    }

    private static String getKey(String sessionID, long requestId) {
        return sessionID + '#' + requestId;
    }

    private void addOngoingRequest(String sessionID, Request coapRequest) {
        if (sessionID != null) {
            CleanerMessageObserver observer = new CleanerMessageObserver(sessionID, coapRequest);
            coapRequest.addMessageObserver(observer);
            ongoingRequests.put(observer.getRequestKey(), coapRequest);
        }
    }

    private void removeOngoingRequest(String key, Request coapRequest) {
        Validate.notNull(key);
        ongoingRequests.remove(key, coapRequest);
    }

    private AtomicLong idGenerator = new AtomicLong(0l);

    private class CleanerMessageObserver extends MessageObserverAdapter {

        private final String requestKey;
        private final Request coapRequest;

        public CleanerMessageObserver(String sessionID, Request coapRequest) {
            super();
            requestKey = getKey(sessionID, idGenerator.incrementAndGet());
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
            removeOngoingRequest(requestKey, coapRequest);
        }

        @Override
        public void onAcknowledgement() {
        }

        @Override
        protected void failed() {
            removeOngoingRequest(requestKey, coapRequest);
        }

        @Override
        public void onCancel() {
            removeOngoingRequest(requestKey, coapRequest);
        }
    }
}
