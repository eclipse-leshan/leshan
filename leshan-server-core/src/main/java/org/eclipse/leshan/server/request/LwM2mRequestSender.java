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
import org.eclipse.leshan.server.response.ResponseListener;

public interface LwM2mRequestSender {

    /**
     * @Deprecated Synchronous send of a message will not be supported in the future. It is replaced by
     *             {@link #send(Registration, String, DownlinkRequest)}
     */
    @Deprecated
    <T extends LwM2mResponse> T send(Registration destination, DownlinkRequest<T> request, Long timeout)
            throws InterruptedException;

    /**
     * @Deprecated Asynchronous send of a message with a callback will not be supported in the future. It is replaced by
     *             {@link #send(Registration, String, DownlinkRequest)}
     */
    @Deprecated
    <T extends LwM2mResponse> void send(Registration destination, DownlinkRequest<T> request,
            ResponseCallback<T> responseCallback, ErrorCallback errorCallback);

    /**
     * sends a Lightweight M2M request asynchronously and uses the requestTicket to correlate the response from a LWM2M
     * Client.
     *
     * @param destination registration meta data of a LWM2M client.
     * @param requestTicket a globally unique identifier for correlating the response
     * @param request an instance of downlink request.
     * @param <T> instance of LwM2mResponse
     */
    <T extends LwM2mResponse> void send(Registration destination, String requestTicket, DownlinkRequest<T> request);

    /**
     * adds the listener for the given LWM2M client. This method shall be used to re-register a listener for already
     * sent messages or pending messages.
     *
     * @param listener global listener for handling the responses from a LWM2M client.
     */
    void addResponseListener(ResponseListener listener);

    /**
     * removes the given instance of response listener from LWM2M Sender's list of response listeners.
     * 
     * @param listener target listener to be removed.
     */
    void removeResponseListener(ResponseListener listener);

    /**
     * cancel all pending messages for a LWM2M client identified by the registration identifier. In case a client
     * de-registers, the consumer can use this method to cancel all messages pending for the given client.
     * 
     * @param registration client registration meta data of a LWM2M client.
     * @throws RequestCanceledException when a request is already being sent in CoAP, then the exception is thrown.
     */
    void cancelPendingRequests(Registration registration);
}
