/*******************************************************************************
 * Copyright (c) 2020 Sierra Wireless and others.
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
package org.eclipse.leshan.client;

import java.util.List;

import org.eclipse.leshan.client.resource.LwM2mObjectTree;
import org.eclipse.leshan.client.send.NoDataException;
import org.eclipse.leshan.client.servers.ServerIdentity;
import org.eclipse.leshan.core.node.codec.CodecException;
import org.eclipse.leshan.core.request.ContentFormat;
import org.eclipse.leshan.core.request.exception.InvalidRequestException;
import org.eclipse.leshan.core.request.exception.InvalidResponseException;
import org.eclipse.leshan.core.request.exception.RequestCanceledException;
import org.eclipse.leshan.core.request.exception.RequestRejectedException;
import org.eclipse.leshan.core.request.exception.SendFailedException;
import org.eclipse.leshan.core.request.exception.TimeoutException;
import org.eclipse.leshan.core.request.exception.UnconnectedPeerException;
import org.eclipse.leshan.core.response.ErrorCallback;
import org.eclipse.leshan.core.response.ResponseCallback;
import org.eclipse.leshan.core.response.SendResponse;

/**
 * A Lightweight M2M client.
 */
public interface LwM2mClient {

    /**
     * Starts the client.
     */
    void start();

    /**
     * Stop the client and can be restarted later using {@link #start()}.
     * 
     * @param deregister True if client should deregister itself before to stop.
     */
    void stop(boolean deregister);

    /**
     * Destroy the client, frees all system resources. Client can not be restarted.
     * 
     * @param deregister True if client should deregister itself before to be destroyed.
     */
    void destroy(boolean deregister);

    /**
     * Trigger a registration update to all registered Server.
     */
    void triggerRegistrationUpdate();

    /**
     * Trigger a registration update to the given server.
     */
    void triggerRegistrationUpdate(ServerIdentity server);

    /**
     * Send Data synchronously to a LWM2M Server.
     * <p>
     * The "Send" operation is used by the LwM2M Client to send data to the LwM2M Server without explicit request by
     * that Server.
     * <p>
     * If some data can not be collected before to send, this will be silently ignored.<br>
     * If there is not data to send at all, {@link NoDataException} is raised.
     * 
     * @param server to which data must be send
     * @param format {@link ContentFormat} to use. It MUST be {@link ContentFormat#SENML_CBOR} or
     *        {@link ContentFormat#SENML_JSON}
     * @param paths the list of LWM2M node path to send.
     * @param timeoutInMs The global timeout to wait in milliseconds (see
     *        https://github.com/eclipse/leshan/wiki/Request-Timeout)
     * @return the LWM2M response. The response can be <code>null</code> if the timeout expires (see
     *         https://github.com/eclipse/leshan/wiki/Request-Timeout).
     * 
     * @throws InterruptedException if the thread was interrupted.
     * @throws InvalidRequestException if send request can not be created.
     * @throws CodecException if request payload can not be encoded.
     * @throws NoDataException if we can not collect data for given list of path.
     * @throws RequestRejectedException if the request is rejected by foreign peer.
     * @throws RequestCanceledException if the request is cancelled.
     * @throws SendFailedException if the request can not be sent. E.g. error at CoAP or DTLS/UDP layer.
     * @throws InvalidResponseException if the response received is malformed.
     * @throws UnconnectedPeerException if client is not connected (no dtls connection available).
     */
    SendResponse sendData(ServerIdentity server, ContentFormat format, List<String> paths, long timeoutInMs)
            throws InterruptedException;

    /**
     * Send Data asynchronously to a LWM2M Server.
     * <p>
     * The "Send" operation is used by the LwM2M Client to send data to the LwM2M Server without explicit request by
     * that Server.
     * <p>
     * If some data can not be collected before to send, this will be silently ignored.<br>
     * If there is not data to send at all, {@link NoDataException} is raised.
     * <p>
     * {@link ResponseCallback} and {@link ErrorCallback} are exclusively called.
     * 
     * @param server to which data must be send
     * @param format {@link ContentFormat} to use. It MUST be {@link ContentFormat#SENML_CBOR} or
     *        {@link ContentFormat#SENML_JSON}
     * @param paths the list of LWM2M node path to send.
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
     * @throws NoDataException if we can not collect data for given list of path.
     * @throws InvalidRequestException if send request can not be created.
     */
    void sendData(ServerIdentity server, ContentFormat format, List<String> paths, long timeoutInMs,
            ResponseCallback<SendResponse> responseCallback, ErrorCallback errorCallback);

    /**
     * @return the {@link LwM2mObjectTree} containing all the object implemented by this client.
     */
    LwM2mObjectTree getObjectTree();
}
