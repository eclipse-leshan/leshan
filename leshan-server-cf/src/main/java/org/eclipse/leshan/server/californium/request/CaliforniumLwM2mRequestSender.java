/*******************************************************************************
 * Copyright (c) 2013-2015 Sierra Wireless and others.
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
 *     Achim Kraus (Bosch Software Innovations GmbH) - use Identity as destination
 *******************************************************************************/
package org.eclipse.leshan.server.californium.request;

import org.eclipse.californium.core.coap.Request;
import org.eclipse.californium.core.coap.Response;
import org.eclipse.californium.core.network.Endpoint;
import org.eclipse.leshan.core.californium.CoapResponseCallback;
import org.eclipse.leshan.core.model.LwM2mModel;
import org.eclipse.leshan.core.node.LwM2mNode;
import org.eclipse.leshan.core.node.codec.CodecException;
import org.eclipse.leshan.core.node.codec.LwM2mNodeDecoder;
import org.eclipse.leshan.core.node.codec.LwM2mNodeEncoder;
import org.eclipse.leshan.core.request.DownlinkRequest;
import org.eclipse.leshan.core.request.exception.InvalidResponseException;
import org.eclipse.leshan.core.request.exception.RequestCanceledException;
import org.eclipse.leshan.core.request.exception.RequestRejectedException;
import org.eclipse.leshan.core.request.exception.SendFailedException;
import org.eclipse.leshan.core.request.exception.TimeoutException;
import org.eclipse.leshan.core.request.exception.UnconnectedPeerException;
import org.eclipse.leshan.core.response.ErrorCallback;
import org.eclipse.leshan.core.response.LwM2mResponse;
import org.eclipse.leshan.core.response.ObserveResponse;
import org.eclipse.leshan.core.response.ResponseCallback;
import org.eclipse.leshan.core.util.Validate;
import org.eclipse.leshan.server.Destroyable;
import org.eclipse.leshan.server.californium.observation.ObservationServiceImpl;
import org.eclipse.leshan.server.model.LwM2mModelProvider;
import org.eclipse.leshan.server.registration.Registration;
import org.eclipse.leshan.server.request.LwM2mRequestSender;

/**
 * An implementation of {@link LwM2mRequestSender} and {@link CoapRequestSender} based on Californium.
 */
public class CaliforniumLwM2mRequestSender implements LwM2mRequestSender, CoapRequestSender, Destroyable {

    private final ObservationServiceImpl observationService;
    private final LwM2mModelProvider modelProvider;
    private final RequestSender sender;

    /**
     * @param secureEndpoint The endpoint used to send coaps request.
     * @param nonSecureEndpoint The endpoint used to send coap request.
     * @param observationService The service used to store observation.
     * @param modelProvider the {@link LwM2mModelProvider} used retrieve the {@link LwM2mModel} used to encode/decode
     *        {@link LwM2mNode}.
     * @param encoder The {@link LwM2mNodeEncoder} used to encode {@link LwM2mNode}.
     * @param decoder The {@link LwM2mNodeDecoder} used to encode {@link LwM2mNode}.
     */
    public CaliforniumLwM2mRequestSender(Endpoint secureEndpoint, Endpoint nonSecureEndpoint,
            ObservationServiceImpl observationService, LwM2mModelProvider modelProvider, LwM2mNodeEncoder encoder,
            LwM2mNodeDecoder decoder) {
        Validate.notNull(observationService);
        Validate.notNull(modelProvider);
        this.observationService = observationService;
        this.modelProvider = modelProvider;
        this.sender = new RequestSender(secureEndpoint, nonSecureEndpoint, encoder, decoder);
    }

    /**
     * Send a Lightweight M2M request synchronously. Will block until a response is received from the remote server.
     * <p>
     * The synchronous way could block a thread during a long time so it is more recommended to use the asynchronous
     * way.
     * 
     * @param destination The {@link Registration} associate to the device we want to sent the request.
     * @param request The request to send to the client.
     * @param timeoutInMs The global timeout to wait in milliseconds (see
     *        https://github.com/eclipse/leshan/wiki/Request-Timeout)
     * @return the LWM2M response. The response can be <code>null</code> if the timeout expires (see
     *         https://github.com/eclipse/leshan/wiki/Request-Timeout).
     * 
     * @throws CodecException if request payload can not be encoded.
     * @throws InterruptedException if the thread was interrupted.
     * @throws RequestRejectedException if the request is rejected by foreign peer.
     * @throws RequestCanceledException if the request is cancelled.
     * @throws SendFailedException if the request can not be sent. E.g. error at CoAP or DTLS/UDP layer.
     * @throws InvalidResponseException if the response received is malformed.
     * @throws UnconnectedPeerException if client is not connected (no dtls connection available).
     */
    @Override
    public <T extends LwM2mResponse> T send(Registration destination, DownlinkRequest<T> request, long timeoutInMs)
            throws InterruptedException {

        // Retrieve the objects definition
        final LwM2mModel model = modelProvider.getObjectModel(destination);

        // Send requests synchronously
        T response = sender.sendLwm2mRequest(destination.getEndpoint(), destination.getIdentity(), destination.getId(),
                model, destination.getRootPath(), request, timeoutInMs, destination.canInitiateConnection());

        // Handle special observe case
        if (response != null && response.getClass() == ObserveResponse.class && response.isSuccess()) {
            observationService.addObservation(destination, ((ObserveResponse) response).getObservation());
        }
        return response;
    }

    /**
     * Send a Lightweight M2M {@link DownlinkRequest} asynchronously to a LWM2M client.
     * 
     * The Californium API does not ensure that message callback are exclusive. E.g. In some race condition, you can get
     * a onReponse call and a onCancel one. This method ensures that you will receive only one event. Meaning, you get
     * either 1 response or 1 error.
     * 
     * @param destination The {@link Registration} associate to the device we want to sent the request.
     * @param request The request to send to the client.
     * @param timeoutInMs The global timeout to wait in milliseconds (see
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
     * @throws CodecException if request payload can not be encoded.
     */
    @Override
    public <T extends LwM2mResponse> void send(final Registration destination, DownlinkRequest<T> request,
            long timeoutInMs, final ResponseCallback<T> responseCallback, ErrorCallback errorCallback) {
        // Retrieve the objects definition
        final LwM2mModel model = modelProvider.getObjectModel(destination);

        // Send requests asynchronously
        sender.sendLwm2mRequest(destination.getEndpoint(), destination.getIdentity(), destination.getId(), model,
                destination.getRootPath(), request, timeoutInMs, new ResponseCallback<T>() {
                    @Override
                    public void onResponse(T response) {
                        if (response != null && response.getClass() == ObserveResponse.class && response.isSuccess()) {
                            observationService.addObservation(destination,
                                    ((ObserveResponse) response).getObservation());
                        }
                        responseCallback.onResponse(response);
                    }
                }, errorCallback, destination.canInitiateConnection());
    }

    /**
     * Send a CoAP {@link Request} synchronously to a LWM2M client. Will block until a response is received from the
     * remote client.
     * <p>
     * The synchronous way could block a thread during a long time so it is more recommended to use the asynchronous
     * way.
     * 
     * @param destination The {@link Registration} associate to the device we want to sent the request. s
     * @param coapRequest The request to send to the client.
     * @param timeoutInMs The response timeout to wait in milliseconds (see
     *        https://github.com/eclipse/leshan/wiki/Request-Timeout)
     * @return the response or <code>null</code> if the timeout expires (see
     *         https://github.com/eclipse/leshan/wiki/Request-Timeout).
     * 
     * @throws InterruptedException if the thread was interrupted.
     * @throws RequestRejectedException if the request is rejected by foreign peer.
     * @throws RequestCanceledException if the request is cancelled.
     * @throws SendFailedException if the request can not be sent. E.g. error at CoAP or DTLS/UDP layer.
     * @throws UnconnectedPeerException if client is not connected (no dtls connection available).
     */
    @Override
    public Response sendCoapRequest(Registration destination, Request coapRequest, long timeoutInMs)
            throws InterruptedException {
        return sender.sendCoapRequest(destination.getIdentity(), destination.getId(), coapRequest, timeoutInMs,
                destination.canInitiateConnection());
    }

    /**
     * Sends a CoAP {@link Request} asynchronously to a LWM2M client.
     * 
     * The Californium API does not ensure that message callback are exclusive. E.g. In some race condition, you can get
     * a onReponse call and a onCancel one. This method ensures that you will receive only one event. Meaning, you get
     * either 1 response or 1 error.
     * 
     * @param destination The {@link Registration} associate to the device we want to sent the request.
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
     */
    @Override
    public void sendCoapRequest(Registration destination, Request coapRequest, long timeoutInMs,
            CoapResponseCallback responseCallback, ErrorCallback errorCallback) {
        sender.sendCoapRequest(destination.getIdentity(), destination.getId(), coapRequest, timeoutInMs,
                responseCallback, errorCallback, destination.canInitiateConnection());
    }

    /**
     * cancel all ongoing messages for a LWM2M client identified by the registration identifier. In case a client
     * de-registers, the consumer can use this method to cancel all ongoing messages for the given client.
     * 
     * @param registration client registration meta data of a LWM2M client.
     */
    @Override
    public void cancelOngoingRequests(Registration registration) {
        Validate.notNull(registration);
        sender.cancelRequests(registration.getId());
    }

    @Override
    public void destroy() {
        sender.destroy();
    }
}
