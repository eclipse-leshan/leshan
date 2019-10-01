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
 *     Bosch Software Innovations GmbH - extension of ticket based asynchronous call.
 *******************************************************************************/
package org.eclipse.leshan.server.request;

import org.eclipse.leshan.core.node.codec.CodecException;
import org.eclipse.leshan.core.request.DownlinkRequest;
import org.eclipse.leshan.core.response.ErrorCallback;
import org.eclipse.leshan.core.response.LwM2mResponse;
import org.eclipse.leshan.core.response.ResponseCallback;
import org.eclipse.leshan.server.registration.Registration;

public interface LwM2mRequestSender {

    /**
     * Sends a Lightweight M2M request synchronously. Will block until a response is received from the remote client.
     * 
     * @param destination the remote client
     * @param request the request to send to the client
     * @param timeout the request timeout in millisecond
     * @return the response or <code>null</code> if the timeout expires (given parameter or CoAP timeout).
     * 
     * @throws CodecException if request payload can not be encoded.
     * @throws InterruptedException if the thread was interrupted.
     */
    <T extends LwM2mResponse> T send(Registration destination, DownlinkRequest<T> request, long timeout)
            throws InterruptedException;

    /**
     * Sends a Lightweight M2M request asynchronously.
     * 
     * @param destination the remote client
     * @param request the request to send to the client
     * @param timeout the request timeout in millisecond
     * @param responseCallback a callback called when a response is received (successful or error response)
     * @param errorCallback a callback called when an error or exception occurred when response is received
     * 
     * @throws CodecException if request payload can not be encoded.
     */
    <T extends LwM2mResponse> void send(Registration destination, DownlinkRequest<T> request, long timeout,
            ResponseCallback<T> responseCallback, ErrorCallback errorCallback);

    /**
     * cancel all ongoing messages for a LWM2M client identified by the registration identifier. In case a client
     * de-registers, the consumer can use this method to cancel all ongoing messages for the given client.
     * 
     * @param registration client registration meta data of a LWM2M client.
     */
    void cancelOngoingRequests(Registration registration);
}
