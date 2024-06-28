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
 *     Bosch Software Innovations GmbH - extension of ticket based asynchronous call.
 *******************************************************************************/
package org.eclipse.leshan.server.bootstrap.request;

import org.eclipse.leshan.core.node.codec.CodecException;
import org.eclipse.leshan.core.request.BootstrapDownlinkRequest;
import org.eclipse.leshan.core.request.DownlinkRequest;
import org.eclipse.leshan.core.request.exception.ClientSleepingException;
import org.eclipse.leshan.core.request.exception.InvalidResponseException;
import org.eclipse.leshan.core.request.exception.RequestCanceledException;
import org.eclipse.leshan.core.request.exception.RequestRejectedException;
import org.eclipse.leshan.core.request.exception.SendFailedException;
import org.eclipse.leshan.core.request.exception.TimeoutException;
import org.eclipse.leshan.core.request.exception.UnconnectedPeerException;
import org.eclipse.leshan.core.response.ErrorCallback;
import org.eclipse.leshan.core.response.LwM2mResponse;
import org.eclipse.leshan.core.response.ResponseCallback;
import org.eclipse.leshan.server.bootstrap.BootstrapSession;

/**
 * A {@link BootstrapDownlinkRequestSender} is responsible to send LWM2M {@link BootstrapDownlinkRequest} for a given
 * {@link BootstrapSession}.
 */
public interface BootstrapDownlinkRequestSender {

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
     * @param <T> The expected type of the response received.
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
     * @throws ClientSleepingException if client is currently sleeping.
     */
    <T extends LwM2mResponse> T send(BootstrapSession destination, BootstrapDownlinkRequest<T> request,
            long timeoutInMs) throws InterruptedException;

    /**
     * Send a Lightweight M2M {@link DownlinkRequest} asynchronously to a LWM2M client.
     *
     * {@link ResponseCallback} and {@link ErrorCallback} are exclusively called.
     *
     * @param destination The {@link BootstrapSession} associate to the device we want to sent the request.
     * @param request The request to send to the client.
     * @param timeoutInMs The global timeout to wait in milliseconds (see
     *        https://github.com/eclipse/leshan/wiki/Request-Timeout)
     * @param responseCallback a callback called when a response is received (successful or error response). This
     *        callback MUST NOT be null.
     * @param <T> The expected type of the response received.
     * @param errorCallback a callback called when an error or exception occurred when response is received. It can be :
     *        <ul>
     *        <li>{@link RequestRejectedException} if the request is rejected by foreign peer.</li>
     *        <li>{@link RequestCanceledException} if the request is cancelled.</li>
     *        <li>{@link SendFailedException} if the request can not be sent. E.g. error at CoAP or DTLS/UDP layer.</li>
     *        <li>{@link InvalidResponseException} if the response received is malformed.</li>
     *        <li>{@link UnconnectedPeerException} if client is not connected (no dtls connection available).</li>
     *        <li>{@link ClientSleepingException} if client is currently sleeping.</li>
     *        <li>{@link TimeoutException} if the timeout expires (see
     *        https://github.com/eclipse/leshan/wiki/Request-Timeout).</li>
     *        <li>or any other RuntimeException for unexpected issue.
     *        </ul>
     *        This callback MUST NOT be null.
     * @throws CodecException if request payload can not be encoded.
     */
    <T extends LwM2mResponse> void send(BootstrapSession destination, BootstrapDownlinkRequest<T> request,
            long timeoutInMs, ResponseCallback<T> responseCallback, ErrorCallback errorCallback);

    /**
     * cancel all ongoing messages for a LWM2M client identified by the registration identifier. In case a client
     * de-registers, the consumer can use this method to cancel all ongoing messages for the given client.
     *
     * @param session client registration meta data of a LWM2M client.
     */
    void cancelOngoingRequests(BootstrapSession session);
}
