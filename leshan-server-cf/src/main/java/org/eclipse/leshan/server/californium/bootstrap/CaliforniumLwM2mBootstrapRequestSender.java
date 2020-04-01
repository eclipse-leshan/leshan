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
 *     Zebra Technologies - initial API and implementation
 *******************************************************************************/
package org.eclipse.leshan.server.californium.bootstrap;

import org.eclipse.californium.core.network.Endpoint;
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
import org.eclipse.leshan.core.response.ErrorCallback;
import org.eclipse.leshan.core.response.LwM2mResponse;
import org.eclipse.leshan.core.response.ResponseCallback;
import org.eclipse.leshan.server.Destroyable;
import org.eclipse.leshan.server.bootstrap.BootstrapSession;
import org.eclipse.leshan.server.bootstrap.LwM2mBootstrapRequestSender;
import org.eclipse.leshan.server.californium.request.RequestSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An implementation of {@link LwM2mBootstrapRequestSender} based on Californium.
 */
public class CaliforniumLwM2mBootstrapRequestSender implements LwM2mBootstrapRequestSender, Destroyable {

    static final Logger LOG = LoggerFactory.getLogger(CaliforniumLwM2mBootstrapRequestSender.class);

    private final LwM2mModel model;
    private final RequestSender sender;

    /**
     * @param secureEndpoint The endpoint used to send coaps request.
     * @param nonSecureEndpoint The endpoint used to send coap request.
     * @param model the {@link LwM2mModel} used to encode/decode {@link LwM2mNode}.
     * @param encoder The {@link LwM2mNodeEncoder} used to encode {@link LwM2mNode}.
     * @param decoder The {@link LwM2mNodeDecoder} used to encode {@link LwM2mNode}.
     */
    public CaliforniumLwM2mBootstrapRequestSender(Endpoint secureEndpoint, Endpoint nonSecureEndpoint, LwM2mModel model,
            LwM2mNodeEncoder encoder, LwM2mNodeDecoder decoder) {
        this.model = model;
        this.sender = new RequestSender(secureEndpoint, nonSecureEndpoint, encoder, decoder);
    }

    /**
     * Send a Lightweight M2M request synchronously. Will block until a response is received from the remote server.
     * <p>
     * The synchronous way could block a thread during a long time so it is more recommended to use the asynchronous
     * way.
     * 
     * @param destination The {@link BootstrapSession} associate to the device we want to sent the request.
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
     */
    @Override
    public <T extends LwM2mResponse> T send(BootstrapSession destination, DownlinkRequest<T> request, long timeoutInMs)
            throws InterruptedException {
        return sender.sendLwm2mRequest(destination.getEndpoint(), destination.getIdentity(), destination.getId(), model,
                null, request, timeoutInMs, false);
    }

    /**
     * Send a Lightweight M2M {@link DownlinkRequest} asynchronously to a LWM2M client.
     * 
     * The Californium API does not ensure that message callback are exclusive. E.g. In some race condition, you can get
     * a onReponse call and a onCancel one. This method ensures that you will receive only one event. Meaning, you get
     * either 1 response or 1 error.
     * 
     * @param destination The {@link BootstrapSession} associate to the device we want to sent the request.
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
     *        <li>{@link TimeoutException} if the timeout expires (see
     *        https://github.com/eclipse/leshan/wiki/Request-Timeout).</li>
     *        <li>or any other RuntimeException for unexpected issue.
     *        </ul>
     *        This callback MUST NOT be null.
     * @throws CodecException if request payload can not be encoded.
     */
    @Override
    public <T extends LwM2mResponse> void send(BootstrapSession destination, DownlinkRequest<T> request,
            long timeoutInMs, ResponseCallback<T> responseCallback, ErrorCallback errorCallback) {
        sender.sendLwm2mRequest(destination.getEndpoint(), destination.getIdentity(), destination.getId(), model, null,
                request, timeoutInMs, responseCallback, errorCallback, false);
    }

    /**
     * Cancel all ongoing requests for a given bootstrap session.
     * 
     * @param session the bootstrap session for which we need to cancel requests.
     */
    @Override
    public void cancelOngoingRequests(BootstrapSession session) {
        sender.cancelRequests(session.getId());
    }

    @Override
    public void destroy() {
        sender.destroy();
    }
}