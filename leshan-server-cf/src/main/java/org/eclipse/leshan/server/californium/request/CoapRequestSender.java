/*******************************************************************************
 * Copyright (c) 2018 Sierra Wireless and others.
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
package org.eclipse.leshan.server.californium.request;

import org.eclipse.californium.core.coap.Request;
import org.eclipse.californium.core.coap.Response;
import org.eclipse.leshan.core.californium.CoapResponseCallback;
import org.eclipse.leshan.core.request.exception.ClientSleepingException;
import org.eclipse.leshan.core.request.exception.RequestCanceledException;
import org.eclipse.leshan.core.request.exception.RequestRejectedException;
import org.eclipse.leshan.core.request.exception.SendFailedException;
import org.eclipse.leshan.core.request.exception.TimeoutException;
import org.eclipse.leshan.core.request.exception.UnconnectedPeerException;
import org.eclipse.leshan.core.response.ErrorCallback;
import org.eclipse.leshan.core.response.ResponseCallback;
import org.eclipse.leshan.server.registration.Registration;

/**
 * A {@link CoapRequestSender} is responsible to send CoAP {@link Request} for a given {@link Registration}.
 */
public interface CoapRequestSender {

    /**
     * Send a CoAP {@link Request} synchronously to a LWM2M client. Will block until a response is received from the
     * remote client.
     * <p>
     * The synchronous way could block a thread during a long time so it is more recommended to use the asynchronous
     * way.
     * 
     * @param destination The registration linked to the LWM2M client to which the request must be sent.
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
     * @throws ClientSleepingException if client is currently sleeping.
     */
    Response sendCoapRequest(final Registration destination, final Request coapRequest, long timeoutInMs)
            throws InterruptedException;

    /**
     * Sends a CoAP {@link Request} asynchronously to a LWM2M client.
     * 
     * {@link ResponseCallback} and {@link ErrorCallback} are exclusively called.
     * 
     * @param destination The registration linked to the LWM2M client to which the request must be sent.
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
     *        <li>{@link ClientSleepingException} if client is currently sleeping.</li>
     *        <li>{@link UnconnectedPeerException} if client is not connected (no dtls connection available).</li>
     *        <li>{@link TimeoutException} if the timeout expires (see
     *        https://github.com/eclipse/leshan/wiki/Request-Timeout).</li>
     *        <li>or any other RuntimeException for unexpected issue.
     *        </ul>
     *        This callback MUST NOT be null.
     */
    void sendCoapRequest(final Registration destination, final Request coapRequest, long timeoutInMs,
            CoapResponseCallback responseCallback, ErrorCallback errorCallback);
}