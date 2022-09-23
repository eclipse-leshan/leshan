/*******************************************************************************
 * Copyright (c) 2019 Sierra Wireless and others.
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
 *     Michał Wadowski (Orange) - Improved compliance with rfc6690
 *     Rikard Höglund (RISE SICS) - Additions to support OSCORE
 *******************************************************************************/
package org.eclipse.leshan.server.californium.request;

import java.util.SortedMap;
import java.util.concurrent.ConcurrentNavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import org.eclipse.californium.core.coap.MessageObserver;
import org.eclipse.californium.core.coap.MessageObserverAdapter;
import org.eclipse.californium.core.coap.Request;
import org.eclipse.californium.core.coap.Response;
import org.eclipse.californium.core.network.Endpoint;
import org.eclipse.californium.elements.EndpointContext;
import org.eclipse.leshan.core.Destroyable;
import org.eclipse.leshan.core.californium.AsyncRequestObserver;
import org.eclipse.leshan.core.californium.CoapAsyncRequestObserver;
import org.eclipse.leshan.core.californium.CoapResponseCallback;
import org.eclipse.leshan.core.californium.CoapSyncRequestObserver;
import org.eclipse.leshan.core.californium.EndpointContextUtil;
import org.eclipse.leshan.core.californium.SyncRequestObserver;
import org.eclipse.leshan.core.californium.TemporaryExceptionTranslator;
import org.eclipse.leshan.core.link.lwm2m.LwM2mLinkParser;
import org.eclipse.leshan.core.model.LwM2mModel;
import org.eclipse.leshan.core.node.LwM2mNode;
import org.eclipse.leshan.core.node.codec.CodecException;
import org.eclipse.leshan.core.node.codec.LwM2mDecoder;
import org.eclipse.leshan.core.node.codec.LwM2mEncoder;
import org.eclipse.leshan.core.request.DownlinkRequest;
import org.eclipse.leshan.core.request.Identity;
import org.eclipse.leshan.core.request.exception.InvalidResponseException;
import org.eclipse.leshan.core.request.exception.RequestCanceledException;
import org.eclipse.leshan.core.request.exception.RequestRejectedException;
import org.eclipse.leshan.core.request.exception.SendFailedException;
import org.eclipse.leshan.core.request.exception.TimeoutException;
import org.eclipse.leshan.core.request.exception.UnconnectedPeerException;
import org.eclipse.leshan.core.response.ErrorCallback;
import org.eclipse.leshan.core.response.LwM2mResponse;
import org.eclipse.leshan.core.response.ResponseCallback;
import org.eclipse.leshan.core.util.NamedThreadFactory;
import org.eclipse.leshan.core.util.Validate;
import org.eclipse.leshan.server.request.LowerLayerConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This sender is able to send LWM2M or CoAP request in a synchronous or asynchronous way.
 * <p>
 * It can also link requests to a kind of "session" and cancel all ongoing requests associated to a given "session".
 */
public class RequestSender implements Destroyable {

    static final Logger LOG = LoggerFactory.getLogger(RequestSender.class);

    private final ScheduledExecutorService executor = Executors.newScheduledThreadPool(1,
            new NamedThreadFactory("Leshan Async Request timeout"));

    private final Endpoint nonSecureEndpoint;
    private final Endpoint secureEndpoint;
    private final LwM2mDecoder decoder;
    private final LwM2mEncoder encoder;
    private final LwM2mLinkParser linkParser;

    // A map which contains all ongoing CoAP requests
    // This is used to be able to cancel request
    private final ConcurrentNavigableMap<String/* sessionId#requestId */, Request /* ongoing coap Request */> ongoingRequests = new ConcurrentSkipListMap<>();

    /**
     * @param secureEndpoint The endpoint used to send coaps request.
     * @param nonSecureEndpoint The endpoint used to send coap request.
     * @param encoder The {@link LwM2mEncoder} used to encode {@link LwM2mNode}.
     * @param decoder The {@link LwM2mDecoder} used to encode {@link LwM2mNode}.
     * @param linkParser a parser {@link LwM2mLinkParser} used to parse a CoRE Link.
     */
    public RequestSender(Endpoint secureEndpoint, Endpoint nonSecureEndpoint, LwM2mEncoder encoder,
            LwM2mDecoder decoder, LwM2mLinkParser linkParser) {
        this.secureEndpoint = secureEndpoint;
        this.nonSecureEndpoint = nonSecureEndpoint;
        this.encoder = encoder;
        this.decoder = decoder;
        this.linkParser = linkParser;
    }

    /**
     * Sends a Lightweight M2M {@link DownlinkRequest} synchronously to a LWM2M client. Will block until a response is
     * received from the remote client.
     * <p>
     * The synchronous way could block a thread during a long time so it is more recommended to use the asynchronous
     * way.
     *
     * @param endpointName the LWM2M client endpoint name.
     * @param destination the LWM2M client {@link Identity}.
     * @param sessionId A session Identifier which could be reused to cancel all ongoing request related to this
     *        sessionId. See {@link #cancelRequests(String)}.
     * @param model The {@link LwM2mModel} used to encode payload in request and decode payload in response.
     * @param rootPath a rootpath to prefix to the LWM2M path to create the CoAP path. (see 8.2.2 Alternate Path in
     *        LWM2M specification)
     * @param request The request to send to the client.
     * @param lowerLayerConfig to tweak lower layer request (e.g. coap request)
     * @param timeoutInMs The response timeout to wait in milliseconds (see
     *        https://github.com/eclipse/leshan/wiki/Request-Timeout)
     * @param allowConnectionInitiation This request can initiate a Handshake if there is no DTLS connection.
     * @return the response or <code>null</code> if the timeout expires (see
     *         https://github.com/eclipse/leshan/wiki/Request-Timeout).
     *
     * @throws CodecException if request payload can not be encoded.
     * @throws InterruptedException if the thread was interrupted.
     * @throws RequestRejectedException if the request is rejected by foreign peer.
     * @throws RequestCanceledException if the request is cancelled.
     * @throws SendFailedException if the request can not be sent. E.g. error at CoAP or DTLS/UDP layer.
     * @throws UnconnectedPeerException if client is not connected (no dtls connection available).
     * @throws InvalidResponseException if the response received is malformed.
     */
    public <T extends LwM2mResponse> T sendLwm2mRequest(final String endpointName, Identity destination,
            String sessionId, final LwM2mModel model, String rootPath, final DownlinkRequest<T> request,
            LowerLayerConfig lowerLayerConfig, long timeoutInMs, boolean allowConnectionInitiation)
            throws InterruptedException {

        // Create the CoAP request from LwM2m request
        CoapRequestBuilder coapClientRequestBuilder = new CoapRequestBuilder(destination, rootPath, sessionId,
                endpointName, model, encoder, allowConnectionInitiation, lowerLayerConfig, null);
        request.accept(coapClientRequestBuilder);
        final Request coapRequest = coapClientRequestBuilder.getRequest();

        // Send CoAP request synchronously
        SyncRequestObserver<T> syncMessageObserver = new SyncRequestObserver<T>(coapRequest, timeoutInMs) {
            @Override
            public T buildResponse(Response coapResponse) {
                // Build LwM2m response
                LwM2mResponseBuilder<T> lwm2mResponseBuilder = new LwM2mResponseBuilder<>(coapRequest, coapResponse,
                        endpointName, model, decoder, linkParser);
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

    /**
     * Send a Lightweight M2M {@link DownlinkRequest} asynchronously to a LWM2M client.
     *
     * The Californium API does not ensure that message callback are exclusive. E.g. In some race condition, you can get
     * a onReponse call and a onCancel one. This method ensures that you will receive only one event. Meaning, you get
     * either 1 response or 1 error.
     *
     * @param endpointName the LWM2M client endpoint name.
     * @param destination the LWM2M client {@link Identity}.
     * @param sessionId A session Identifier which could be reused to cancel all ongoing request related to this
     *        sessionId. See {@link #cancelRequests(String)}.
     * @param model The {@link LwM2mModel} used to encode payload in request and decode payload in response.
     * @param rootPath a rootpath to prefix to the LWM2M path to create the CoAP path. (see 8.2.2 Alternate Path in
     *        LWM2M specification)
     * @param request The request to send to the client.
     * @param lowerLayerConfig to tweak lower layer request (e.g. coap request)
     * @param timeoutInMs The response timeout to wait in milliseconds (see
     *        https://github.com/eclipse/leshan/wiki/Request-Timeout)
     * @param responseCallback a callback called when a response is received (successful or error response). This
     *        callback MUST NOT be null.
     * @param errorCallback a callback called when an error or exception occurred when response is received. It can be :
     *        <ul>
     *        <li>{@link RequestRejectedException} if the request is rejected by foreign peer.</li>
     *        <li>{@link RequestCanceledException} if the request is cancelled.</li>
     *        <li>{@link SendFailedException} if the request can not be sent. E.g. error at CoAP or DTLS/UDP layer.</li>
     *        <li>{@link InvalidResponseException} if the response received is malformed.</li>
     *        <li>{@link UnconnectedPeerException} if client is not connected (no dtls connection available).</li>
     *        <li>{@link TimeoutException} if the timeout expires (see
     *        https://github.com/eclipse/leshan/wiki/Request-Timeout).</li>
     *        <li>or any other RuntimeException for unexpected issue.
     *        </ul>
     *        This callback MUST NOT be null.
     * @param allowConnectionInitiation This request can initiate a Handshake if there is no DTLS connection.
     * @throws CodecException if request payload can not be encoded.
     */

    public <T extends LwM2mResponse> void sendLwm2mRequest(final String endpointName, Identity destination,
            String sessionId, final LwM2mModel model, String rootPath, final DownlinkRequest<T> request,
            LowerLayerConfig lowerLayerConfig, long timeoutInMs, ResponseCallback<T> responseCallback,
            ErrorCallback errorCallback, boolean allowConnectionInitiation) {

        Validate.notNull(responseCallback);
        Validate.notNull(errorCallback);

        // Create the CoAP request from LwM2m request
        CoapRequestBuilder coapClientRequestBuilder = new CoapRequestBuilder(destination, rootPath, sessionId,
                endpointName, model, encoder, allowConnectionInitiation, lowerLayerConfig, null);
        request.accept(coapClientRequestBuilder);
        final Request coapRequest = coapClientRequestBuilder.getRequest();

        // Add CoAP request callback
        MessageObserver obs = new AsyncRequestObserver<T>(coapRequest, responseCallback, errorCallback, timeoutInMs,
                executor) {
            @Override
            public T buildResponse(Response coapResponse) {
                // Build LwM2m response
                LwM2mResponseBuilder<T> lwm2mResponseBuilder = new LwM2mResponseBuilder<>(coapRequest, coapResponse,
                        endpointName, model, decoder, linkParser);
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

    /**
     * Send a CoAP {@link Request} synchronously to a LWM2M client. Will block until a response is received from the
     * remote client.
     * <p>
     * The synchronous way could block a thread during a long time so it is more recommended to use the asynchronous
     * way.
     *
     * @param destination the LWM2M client {@link Identity}.
     * @param sessionId A session Identifier which could be reused to cancel all ongoing request related to this one.
     *        See {@link #cancelRequests(String)}.
     * @param coapRequest The request to send to the client.
     * @param timeoutInMs The response timeout to wait in milliseconds (see
     *        https://github.com/eclipse/leshan/wiki/Request-Timeout)
     * @param allowConnectionInitiation This request can initiate a Handshake if there is no DTLS connection.
     * @return the response or <code>null</code> if the timeout expires (see
     *         https://github.com/eclipse/leshan/wiki/Request-Timeout).
     *
     * @throws InterruptedException if the thread was interrupted.
     * @throws RequestRejectedException if the request is rejected by foreign peer.
     * @throws RequestCanceledException if the request is cancelled.
     * @throws SendFailedException if the request can not be sent. E.g. error at CoAP or DTLS/UDP layer.
     * @throws UnconnectedPeerException if client is not connected (no dtls connection available).
     */
    public Response sendCoapRequest(Identity destination, String sessionId, Request coapRequest, long timeoutInMs,
            boolean allowConnectionInitiation) throws InterruptedException {

        // Define destination
        if (coapRequest.getDestinationContext() == null) {
            EndpointContext context = EndpointContextUtil.extractContext(destination, allowConnectionInitiation);
            coapRequest.setDestinationContext(context);
        } else {
            LOG.warn(
                    "Destination context was not set by Leshan for this request. The context is used to ensure you talk to the right peer. Bad usage could bring to security issue. {}",
                    coapRequest);
        }

        // TODO OSCORE : should we add the OSCORE option automatically here too ?

        // Send CoAP request synchronously
        CoapSyncRequestObserver syncMessageObserver = new CoapSyncRequestObserver(coapRequest, timeoutInMs,
                new TemporaryExceptionTranslator());
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

    /**
     * Sends a CoAP {@link Request} asynchronously to a LWM2M client.
     *
     * The Californium API does not ensure that message callback are exclusive. E.g. In some race condition, you can get
     * a onReponse call and a onCancel one. This method ensures that you will receive only one event. Meaning, you get
     * either 1 response or 1 error.
     *
     * @param destination the LWM2M client {@link Identity}.
     * @param sessionId A session Identifier which could be reused to cancel all ongoing request related to this one.
     *        See {@link #cancelRequests(String)}.
     * @param coapRequest The request to send to the client.
     * @param timeoutInMs The response timeout to wait in milliseconds (see
     *        https://github.com/eclipse/leshan/wiki/Request-Timeout)
     * @param responseCallback a callback called when a response is received (successful or error response). This
     *        callback MUST NOT be null.
     * @param errorCallback a callback called when an error or exception occurred when response is received. It can be :
     *        <ul>
     *        <li>{@link RequestRejectedException} if the request is rejected by foreign peer.</li>
     *        <li>{@link RequestCanceledException} if the request is cancelled.</li>
     *        <li>{@link SendFailedException} if the request can not be sent. E.g. error at CoAP or DTLS/UDP layer.</li>
     *        <li>{@link UnconnectedPeerException} if client is not connected (no dtls connection available).</li>
     *        <li>{@link TimeoutException} if the timeout expires (see
     *        https://github.com/eclipse/leshan/wiki/Request-Timeout).</li>
     *        <li>or any other RuntimeException for unexpected issue.
     *        </ul>
     *        This callback MUST NOT be null.
     * @param allowConnectionInitiation This request can initiate a Handshake if there is no DTLS connection.
     */
    public void sendCoapRequest(Identity destination, String sessionId, Request coapRequest, long timeoutInMs,
            CoapResponseCallback responseCallback, ErrorCallback errorCallback, boolean allowConnectionInitiation) {

        Validate.notNull(responseCallback);
        Validate.notNull(errorCallback);

        // Define destination
        if (coapRequest.getDestinationContext() == null) {
            EndpointContext context = EndpointContextUtil.extractContext(destination, allowConnectionInitiation);
            coapRequest.setDestinationContext(context);
        } else {
            LOG.warn(
                    "Destination context was not set by Leshan for this request. The context is used to ensure you talk to the right peer. Bad usage could bring to security issue.{}",
                    coapRequest);
        }

        // TODO OSCORE : should we add the OSCORE option automatically here too ?

        // Add CoAP request callback
        MessageObserver obs = new CoapAsyncRequestObserver(coapRequest, responseCallback, errorCallback, timeoutInMs,
                executor, new TemporaryExceptionTranslator());
        coapRequest.addMessageObserver(obs);

        // Store pending request to be able to cancel it later
        addOngoingRequest(sessionId, coapRequest);

        // Send CoAP request asynchronously
        if (destination.isSecure())
            secureEndpoint.sendRequest(coapRequest);
        else
            nonSecureEndpoint.sendRequest(coapRequest);
    }

    /**
     * Cancel all ongoing requests for the given sessionID.
     *
     * @param sessionID the Id associated to the ongoing requests you want to cancel.
     *
     * @see "All others send methods."
     */
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

    @Override
    public void destroy() {
        executor.shutdownNow();
        try {
            executor.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            LOG.warn("Destroying RequestSender was interrupted.", e);
        }
    }
}
