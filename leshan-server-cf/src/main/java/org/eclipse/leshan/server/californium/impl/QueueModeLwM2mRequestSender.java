/*******************************************************************************
 * Copyright (c) 2017 RISE SICS AB.
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
 *     RISE SICS AB - initial API and implementation
 *******************************************************************************/
package org.eclipse.leshan.server.californium.impl;

import org.eclipse.leshan.core.request.DownlinkRequest;
import org.eclipse.leshan.core.request.exception.TimeoutException;
import org.eclipse.leshan.core.response.ErrorCallback;
import org.eclipse.leshan.core.response.LwM2mResponse;
import org.eclipse.leshan.core.response.ResponseCallback;
import org.eclipse.leshan.server.queue.Presence;
import org.eclipse.leshan.server.queue.PresenceServiceImpl;
import org.eclipse.leshan.server.registration.Registration;
import org.eclipse.leshan.server.request.LwM2mRequestSender;
import org.eclipse.leshan.util.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class QueueModeLwM2mRequestSender implements LwM2mRequestSender {

    private static final Logger LOG = LoggerFactory.getLogger(QueueModeLwM2mRequestSender.class);

    private PresenceServiceImpl presenceService;
    private LwM2mRequestSender delegatedSender;

    /**
     * @param presenceService the presence service object for setting the client into {@link Presence#SLEEPING} when
     *        request Timeout expires and into {@link Presence#Awake} when a response arrives.
     * @param delegatedSender internal sender that it is used for sending the requests, using delegation.
     */
    public QueueModeLwM2mRequestSender(PresenceServiceImpl presenceService, LwM2mRequestSender delegatedSender) {
        Validate.notNull(presenceService);
        Validate.notNull(delegatedSender);

        this.presenceService = presenceService;
        this.delegatedSender = delegatedSender;
    }

    @Override
    public <T extends LwM2mResponse> T send(final Registration destination, DownlinkRequest<T> request, long timeout)
            throws InterruptedException {

        // If the client does not use Q-Mode, just send
        if (!destination.usesQueueMode()) {
            return delegatedSender.send(destination, request, timeout);
        }

        // If the client uses Q-Mode...

        // If the client is sleeping, warn the user and return
        if (!presenceService.isClientAwake(destination)) {
            LOG.info("The destination client is sleeping, request couldn't been sent.");
            return null;
        }

        // Use delegation to send the request
        T response = delegatedSender.send(destination, request, timeout);
        if (response != null) {

            // Set the client awake. This will restart the timer.
            presenceService.setAwake(destination);
        } else {

            // If the timeout expires, this means the client does not respond.
            presenceService.clientNotResponding(destination);
        }
        // Wait for response, then return it
        return response;
    }

    @Override
    public <T extends LwM2mResponse> void send(final Registration destination, DownlinkRequest<T> request, long timeout,
            final ResponseCallback<T> responseCallback, final ErrorCallback errorCallback) {

        // If the client does not use Q-Mode, just send
        if (!destination.usesQueueMode()) {
            delegatedSender.send(destination, request, timeout, responseCallback, errorCallback);
            return;
        }

        // If the client uses Q-Mode...

        // If the client is sleeping, warn the user and return
        if (!presenceService.isClientAwake(destination)) {
            LOG.info("The destination client is sleeping, request couldn't been sent.");
            return;
        }

        // Use delegation to send the request, with specific callbacks to perform Queue Mode operation
        delegatedSender.send(destination, request, timeout, new ResponseCallback<T>() {
            @Override
            public void onResponse(T response) {
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
                    presenceService.clientNotResponding(destination);
                }

                // Call the user's callback
                errorCallback.onError(e);
            }
        });

    }

    @Override
    public void cancelPendingRequests(Registration registration) {
        delegatedSender.cancelPendingRequests(registration);
    }
}
