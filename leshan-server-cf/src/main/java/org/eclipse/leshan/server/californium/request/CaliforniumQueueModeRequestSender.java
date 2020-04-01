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
import org.eclipse.leshan.core.request.exception.TimeoutException;
import org.eclipse.leshan.core.response.ErrorCallback;
import org.eclipse.leshan.server.Destroyable;
import org.eclipse.leshan.server.queue.PresenceServiceImpl;
import org.eclipse.leshan.server.queue.QueueModeLwM2mRequestSender;
import org.eclipse.leshan.server.registration.Registration;
import org.eclipse.leshan.server.request.LwM2mRequestSender;

/**
 * A {@link LwM2mRequestSender} and {@link CoapRequestSender} which supports LWM2M Queue Mode.
 */
public class CaliforniumQueueModeRequestSender extends QueueModeLwM2mRequestSender
        implements CoapRequestSender, Destroyable {

    /**
     * @param presenceService the presence service object for setting the client into sleeping state when request
     *        Timeout expires and into awake state when a response arrives.
     * @param delegatedSender internal sender that it is used for sending the requests, using delegation.
     */
    public CaliforniumQueueModeRequestSender(PresenceServiceImpl presenceService, LwM2mRequestSender delegatedSender) {
        super(presenceService, delegatedSender);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Response sendCoapRequest(Registration destination, Request coapRequest, long timeout)
            throws InterruptedException {

        // Ensure that delegated sender is able to send CoAP request
        if (!(delegatedSender instanceof CoapRequestSender)) {
            throw new UnsupportedOperationException("This sender does not support to send CoAP request");
        }
        CoapRequestSender sender = (CoapRequestSender) delegatedSender;

        // If the client does not use Q-Mode, just send
        if (!destination.usesQueueMode()) {
            return sender.sendCoapRequest(destination, coapRequest, timeout);
        }

        // If the client uses Q-Mode...

        // If the client is sleeping, warn the user and return
        if (!presenceService.isClientAwake(destination)) {
            throw new ClientSleepingException("The destination client is sleeping, request cannot be sent.");
        }

        // Use delegation to send the request
        Response response = sender.sendCoapRequest(destination, coapRequest, timeout);
        if (response != null) {
            // Set the client awake. This will restart the timer.
            presenceService.setAwake(destination);
        } else {
            // If the timeout expires, this means the client does not respond.
            presenceService.setSleeping(destination);
        }
        // Wait for response, then return it
        return response;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void sendCoapRequest(final Registration destination, Request coapRequest, long timeout,
            final CoapResponseCallback responseCallback, final ErrorCallback errorCallback) {

        // Ensure that delegated sender is able to send CoAP request
        if (!(delegatedSender instanceof CoapRequestSender)) {
            throw new UnsupportedOperationException("This sender does not support to send CoAP request");
        }
        CoapRequestSender sender = (CoapRequestSender) delegatedSender;

        // If the client does not use Q-Mode, just send
        if (!destination.usesQueueMode()) {
            sender.sendCoapRequest(destination, coapRequest, timeout, responseCallback, errorCallback);
            return;
        }

        // If the client uses Q-Mode...

        // If the client is sleeping, warn the user and return
        if (!presenceService.isClientAwake(destination)) {
            throw new ClientSleepingException("The destination client is sleeping, request cannot be sent.");
        }

        // Use delegation to send the request, with specific callbacks to perform Queue Mode operation
        sender.sendCoapRequest(destination, coapRequest, timeout, new CoapResponseCallback() {
            @Override
            public void onResponse(Response response) {
                // Set the client awake. This will restart the timer.
                presenceService.setAwake(destination);

                // Call the user's callback
                responseCallback.onResponse(response);
            }
        }, new ErrorCallback() {
            @Override
            public void onError(Exception e) {
                if (e instanceof TimeoutException) {
                    // If the timeout expires, this means the client does not respond.
                    presenceService.setSleeping(destination);
                }

                // Call the user's callback
                errorCallback.onError(e);
            }
        });
    }

    @Override
    public void destroy() {
        if (delegatedSender instanceof Destroyable) {
            ((Destroyable) delegatedSender).destroy();
        }
    }
}