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
 *     Michał Wadowski (Orange) - Improved compliance with rfc6690
 *******************************************************************************/
package org.eclipse.leshan.server.request;

import org.eclipse.leshan.core.model.LwM2mModel;
import org.eclipse.leshan.core.node.LwM2mNode;
import org.eclipse.leshan.core.node.codec.CodecException;
import org.eclipse.leshan.core.request.DownlinkRequest;
import org.eclipse.leshan.core.request.exception.InvalidResponseException;
import org.eclipse.leshan.core.request.exception.RequestCanceledException;
import org.eclipse.leshan.core.request.exception.RequestRejectedException;
import org.eclipse.leshan.core.request.exception.SendFailedException;
import org.eclipse.leshan.core.request.exception.TimeoutException;
import org.eclipse.leshan.core.request.exception.UnconnectedPeerException;
import org.eclipse.leshan.core.response.ErrorCallback;
import org.eclipse.leshan.core.response.LwM2mResponse;
import org.eclipse.leshan.core.response.ResponseCallback;
import org.eclipse.leshan.core.util.Validate;
import org.eclipse.leshan.server.endpoint.LwM2mServerEndpoint;
import org.eclipse.leshan.server.endpoint.LwM2mServerEndpointsProvider;
import org.eclipse.leshan.server.model.LwM2mModelProvider;
import org.eclipse.leshan.server.profile.ClientProfile;
import org.eclipse.leshan.server.registration.Registration;

/**
 * The default implementation of {@link DownlinkRequestSender}.
 */
public class DefaultDownlinkRequestSender implements DownlinkRequestSender {

    private final LwM2mModelProvider modelProvider;
    private final LwM2mServerEndpointsProvider endpointsProvider;

    /**
     * @param endpointsProvider which provides available {@link LwM2mServerEndpoint}
     * @param modelProvider the {@link LwM2mModelProvider} used retrieve the {@link LwM2mModel} used to encode/decode
     *        {@link LwM2mNode}.
     */
    public DefaultDownlinkRequestSender(LwM2mServerEndpointsProvider endpointsProvider,
            LwM2mModelProvider modelProvider) {
        Validate.notNull(modelProvider);
        this.modelProvider = modelProvider;
        this.endpointsProvider = endpointsProvider;
    }

    /**
     * Send a Lightweight M2M request synchronously. Will block until a response is received from the remote server.
     * <p>
     * The synchronous way could block a thread during a long time so it is more recommended to use the asynchronous
     * way.
     *
     * @param destination The {@link Registration} associate to the device we want to sent the request.
     * @param request The request to send to the client.
     * @param lowerLayerConfig to tweak lower layer request (e.g. coap request)
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
    public <T extends LwM2mResponse> T send(Registration destination, DownlinkRequest<T> request,
            LowerLayerConfig lowerLayerConfig, long timeoutInMs) throws InterruptedException {

        // find endpoint to use
        LwM2mServerEndpoint endpoint = endpointsProvider.getEndpoint(destination.getLastEndpointUsed());

        // Retrieve the objects definition
        final LwM2mModel model = modelProvider.getObjectModel(destination);

        // Send requests synchronously
        T response = endpoint.send(new ClientProfile(destination, model), request, lowerLayerConfig, timeoutInMs);
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
     * @param lowerLayerConfig to tweak lower layer request (e.g. coap request)
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
            LowerLayerConfig lowerLayerConfig, long timeoutInMs, final ResponseCallback<T> responseCallback,
            ErrorCallback errorCallback) {

        // find endpoint to use
        LwM2mServerEndpoint endpoint = endpointsProvider.getEndpoint(destination.getLastEndpointUsed());

        // Retrieve the objects definition
        final LwM2mModel model = modelProvider.getObjectModel(destination);

        // Send requests asynchronously
        endpoint.send(new ClientProfile(destination, model), request, new ResponseCallback<T>() {
            @Override
            public void onResponse(T response) {
                responseCallback.onResponse(response);
            }
        }, errorCallback, lowerLayerConfig, timeoutInMs);
    }

    @Override
    public void cancelOngoingRequests(Registration registration) {
        for (LwM2mServerEndpoint endpoint : endpointsProvider.getEndpoints()) {
            endpoint.cancelRequests(registration.getId());
        }
    }
}
