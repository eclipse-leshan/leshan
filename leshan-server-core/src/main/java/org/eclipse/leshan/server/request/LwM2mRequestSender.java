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

import org.eclipse.leshan.core.request.DownlinkRequest;
import org.eclipse.leshan.core.request.exception.RequestCanceledException;
import org.eclipse.leshan.core.response.ErrorCallback;
import org.eclipse.leshan.core.response.LwM2mResponse;
import org.eclipse.leshan.core.response.ResponseCallback;
import org.eclipse.leshan.server.client.Registration;

public interface LwM2mRequestSender {

    /**
     * Sends a Lightweight M2M request synchronously. Will block until a response is received from the remote client.
     * 
     * @param destination the remote client
     * @param request the request to the client
     * @return the response or <code>null</code> if the CoAP timeout expires ( see
     *         http://tools.ietf.org/html/rfc7252#section-4.2 ).
     */
    <T extends LwM2mResponse> T send(Registration destination, DownlinkRequest<T> request, Long timeout)
            throws InterruptedException;
    
    /**
     * Sends a Lightweight M2M request synchronously. Will block until a response is received from the remote client.
     * 
     * @param destination the remote client
     * @param request the request to the client
     * @param timeout the request timeout in millisecond
     * @return the response or <code>null</code> if the timeout expires (given parameter or CoAP timeout).
     */
    <T extends LwM2mResponse> void send(Registration destination, DownlinkRequest<T> request,
            ResponseCallback<T> responseCallback, ErrorCallback errorCallback);

    /**
     * cancel all pending messages for a LWM2M client identified by the registration identifier. In case a client
     * de-registers, the consumer can use this method to cancel all messages pending for the given client.
     * 
     * @param registration client registration meta data of a LWM2M client.
     * @throws RequestCanceledException when a request is already being sent in CoAP, then the exception is thrown.
     */
    void cancelPendingRequests(Registration registration);
}
