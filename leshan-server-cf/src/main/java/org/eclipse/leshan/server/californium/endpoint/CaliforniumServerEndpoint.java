/*******************************************************************************
 * Copyright (c) 2022 Sierra Wireless and others.
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
 *******************************************************************************/
package org.eclipse.leshan.server.californium.endpoint;

import java.net.URI;
import java.util.SortedMap;
import java.util.concurrent.ConcurrentNavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicLong;

import org.eclipse.californium.core.coap.MessageObserver;
import org.eclipse.californium.core.coap.MessageObserverAdapter;
import org.eclipse.californium.core.coap.Request;
import org.eclipse.californium.core.coap.Response;
import org.eclipse.californium.core.coap.Token;
import org.eclipse.californium.core.network.CoapEndpoint;
import org.eclipse.leshan.core.californium.AsyncRequestObserver;
import org.eclipse.leshan.core.californium.ExceptionTranslator;
import org.eclipse.leshan.core.californium.SyncRequestObserver;
import org.eclipse.leshan.core.californium.identity.IdentityHandler;
import org.eclipse.leshan.core.endpoint.EndpointUriUtil;
import org.eclipse.leshan.core.endpoint.Protocol;
import org.eclipse.leshan.core.observation.Observation;
import org.eclipse.leshan.core.request.DownlinkRequest;
import org.eclipse.leshan.core.response.ErrorCallback;
import org.eclipse.leshan.core.response.LwM2mResponse;
import org.eclipse.leshan.core.response.ObserveCompositeResponse;
import org.eclipse.leshan.core.response.ObserveResponse;
import org.eclipse.leshan.core.response.ResponseCallback;
import org.eclipse.leshan.core.util.Validate;
import org.eclipse.leshan.server.endpoint.LwM2mServerEndpoint;
import org.eclipse.leshan.server.endpoint.ServerEndpointToolbox;
import org.eclipse.leshan.server.observation.LwM2mNotificationReceiver;
import org.eclipse.leshan.server.profile.ClientProfile;
import org.eclipse.leshan.server.request.LowerLayerConfig;

public class CaliforniumServerEndpoint implements LwM2mServerEndpoint {

    private final Protocol protocol;
    private final ScheduledExecutorService executor;
    private final CoapEndpoint endpoint;
    private final ServerEndpointToolbox toolbox;
    private final ServerCoapMessageTranslator translator;
    private final LwM2mNotificationReceiver notificationReceiver;
    private final IdentityHandler identityHandler;
    private final ExceptionTranslator exceptionTranslator;

    // A map which contains all ongoing CoAP requests
    // This is used to be able to cancel request
    private final ConcurrentNavigableMap<String/* sessionId#requestId */, Request /* ongoing coap Request */> ongoingRequests = new ConcurrentSkipListMap<>();

    public CaliforniumServerEndpoint(Protocol protocol, CoapEndpoint endpoint, ServerCoapMessageTranslator translator,
            ServerEndpointToolbox toolbox, LwM2mNotificationReceiver notificationReceiver,
            IdentityHandler identityHandler, ExceptionTranslator exceptionTranslator,
            ScheduledExecutorService executor) {
        this.protocol = protocol;
        this.translator = translator;
        this.toolbox = toolbox;
        this.endpoint = endpoint;
        this.notificationReceiver = notificationReceiver;
        this.identityHandler = identityHandler;
        this.exceptionTranslator = exceptionTranslator;
        this.executor = executor;
    }

    @Override
    public Protocol getProtocol() {
        return protocol;
    }

    @Override
    public URI getURI() {
        return EndpointUriUtil.createUri(protocol.getUriScheme(), endpoint.getAddress());
    }

    public CoapEndpoint getCoapEndpoint() {
        return endpoint;
    }

    @Override
    public <T extends LwM2mResponse> T send(ClientProfile destination, DownlinkRequest<T> lwm2mRequest,
            LowerLayerConfig lowerLayerConfig, long timeoutInMs) throws InterruptedException {
        // Create the CoAP request from LwM2m request
        final Request coapRequest = translator.createCoapRequest(destination, lwm2mRequest, toolbox, identityHandler);

        // Apply customSetting
        if (lowerLayerConfig != null) {
            lowerLayerConfig.apply(coapRequest);
        }

        // Send CoAP request synchronously
        SyncRequestObserver<T> syncMessageObserver = new SyncRequestObserver<T>(coapRequest, timeoutInMs,
                exceptionTranslator) {
            @Override
            public T buildResponse(Response coapResponse) {
                // Build LwM2m response
                T lwM2mResponse = translator.createLwM2mResponse(destination, lwm2mRequest, coapRequest, coapResponse,
                        toolbox);
                // Handle special observe case
                if (lwM2mResponse != null && lwM2mResponse.isSuccess()) {
                    Observation observation = null;
                    if (lwM2mResponse instanceof ObserveResponse) {
                        observation = ((ObserveResponse) lwM2mResponse).getObservation();
                    } else if (lwM2mResponse instanceof ObserveCompositeResponse) {
                        observation = ((ObserveCompositeResponse) lwM2mResponse).getObservation();
                    }
                    if (observation != null) {
                        notificationReceiver.newObservation(observation, destination.getRegistration());
                    }
                }
                return lwM2mResponse;
            }
        };
        coapRequest.addMessageObserver(syncMessageObserver);

        // Store pending request to be able to cancel it later
        addOngoingRequest(destination.getRegistrationId(), coapRequest);

        // Send CoAP request asynchronously
        endpoint.sendRequest(coapRequest);

        // Wait for response, then return it
        return syncMessageObserver.waitForResponse();
    }

    @Override
    public <T extends LwM2mResponse> void send(ClientProfile destination, DownlinkRequest<T> lwm2mRequest,
            ResponseCallback<T> responseCallback, ErrorCallback errorCallback, LowerLayerConfig lowerLayerConfig,
            long timeoutInMs) {
        Validate.notNull(responseCallback);
        Validate.notNull(errorCallback);

        // Create the CoAP request from LwM2m request
        final Request coapRequest = translator.createCoapRequest(destination, lwm2mRequest, toolbox, identityHandler);

        // Apply customSetting
        if (lowerLayerConfig != null) {
            lowerLayerConfig.apply(coapRequest);
        }

        // Add CoAP request callback
        MessageObserver obs = new AsyncRequestObserver<T>(coapRequest, responseCallback, errorCallback, timeoutInMs,
                executor, exceptionTranslator) {
            @Override
            public T buildResponse(Response coapResponse) {
                // Build LwM2m response
                T lwM2mResponse = translator.createLwM2mResponse(destination, lwm2mRequest, coapRequest, coapResponse,
                        toolbox);
                // Handle special observe case
                if (lwM2mResponse != null && lwM2mResponse.isSuccess()) {
                    Observation observation = null;
                    if (lwM2mResponse instanceof ObserveResponse) {
                        observation = ((ObserveResponse) lwM2mResponse).getObservation();
                    } else if (lwM2mResponse instanceof ObserveCompositeResponse) {
                        observation = ((ObserveCompositeResponse) lwM2mResponse).getObservation();
                    }
                    if (observation != null) {
                        notificationReceiver.newObservation(observation, destination.getRegistration());
                    }
                }
                return lwM2mResponse;
            }
        };
        coapRequest.addMessageObserver(obs);

        // Store pending request to be able to cancel it later
        addOngoingRequest(destination.getRegistrationId(), coapRequest);

        // Send CoAP request asynchronously
        endpoint.sendRequest(coapRequest);
    }

    @Override
    public void cancelObservation(Observation observation) {
        endpoint.cancelObservation(new Token(observation.getId().getBytes()));
    }

    /**
     * Cancel all ongoing requests for the given sessionID.
     *
     * @param sessionID the Id associated to the ongoing requests you want to cancel.
     *
     * @see "All others send methods."
     */
    @Override
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

    private final AtomicLong idGenerator = new AtomicLong(0l);

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
