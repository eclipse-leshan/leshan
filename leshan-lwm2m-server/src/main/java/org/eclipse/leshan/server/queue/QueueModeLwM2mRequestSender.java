/*******************************************************************************
 * Copyright (c) 2017 RISE SICS AB.
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
 *     RISE SICS AB - initial API and implementation
 *******************************************************************************/
package org.eclipse.leshan.server.queue;

import org.eclipse.leshan.core.request.DownlinkRequest;
import org.eclipse.leshan.core.request.exception.ClientSleepingException;
import org.eclipse.leshan.core.request.exception.TimeoutException;
import org.eclipse.leshan.core.request.exception.UnconnectedPeerException;
import org.eclipse.leshan.core.response.ErrorCallback;
import org.eclipse.leshan.core.response.LwM2mResponse;
import org.eclipse.leshan.core.response.ResponseCallback;
import org.eclipse.leshan.core.util.Validate;
import org.eclipse.leshan.server.registration.Registration;
import org.eclipse.leshan.server.request.DownlinkRequestSender;
import org.eclipse.leshan.server.request.LowerLayerConfig;

/**
 * A {@link DownlinkRequestSender} which supports LWM2M Queue Mode.
 */
public class QueueModeLwM2mRequestSender implements DownlinkRequestSender {

    protected PresenceServiceImpl presenceService;
    protected DownlinkRequestSender delegatedSender;

    /**
     * @param presenceService the presence service object for setting the client into sleepint state when request
     *        Timeout expires and into awake state when a response arrives.
     * @param delegatedSender internal sender that it is used for sending the requests, using delegation.
     */
    public QueueModeLwM2mRequestSender(PresenceServiceImpl presenceService, DownlinkRequestSender delegatedSender) {
        Validate.notNull(presenceService);
        Validate.notNull(delegatedSender);

        this.presenceService = presenceService;
        this.delegatedSender = delegatedSender;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T extends LwM2mResponse> T send(final Registration destination, DownlinkRequest<T> request,
            LowerLayerConfig lowerLayerConfig, long timeout) throws InterruptedException {

        // If the client does not use Q-Mode, just send
        if (!destination.usesQueueMode()) {
            return delegatedSender.send(destination, request, lowerLayerConfig, timeout);
        }

        // If the client uses Q-Mode...

        // If the client is sleeping, warn the user and return
        if (!presenceService.isClientAwake(destination)) {
            throw new ClientSleepingException("The destination client is sleeping, request cannot be sent.");
        }

        // Use delegation to send the request
        try {
            T response = null;
            response = delegatedSender.send(destination, request, lowerLayerConfig, timeout);
            if (response != null) {
                // Set the client awake. This will restart the timer.
                presenceService.setAwake(destination);
            } else {
                // If the timeout expires, this means the client does not respond.
                presenceService.setSleeping(destination);
            }

            // Wait for response, then return it
            return response;
        } catch (UnconnectedPeerException e) {
            // if peer is not connected (No DTLS connection available)
            presenceService.setSleeping(destination);
            throw e;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T extends LwM2mResponse> void send(final Registration destination, DownlinkRequest<T> request,
            LowerLayerConfig lowerLayerConfig, long timeout, final ResponseCallback<T> responseCallback,
            final ErrorCallback errorCallback) {

        // If the client does not use Q-Mode, just send
        if (!destination.usesQueueMode()) {
            delegatedSender.send(destination, request, lowerLayerConfig, timeout, responseCallback, errorCallback);
            return;
        }

        // If the client uses Q-Mode...

        // If the client is sleeping, warn the user and return
        if (!presenceService.isClientAwake(destination)) {
            throw new ClientSleepingException("The destination client is sleeping, request cannot be sent.");
        }

        // Use delegation to send the request, with specific callbacks to perform Queue Mode operation
        delegatedSender.send(destination, request, lowerLayerConfig, timeout, new ResponseCallback<T>() {
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
                    presenceService.setSleeping(destination);
                } else if (e instanceof UnconnectedPeerException) {
                    // if peer is not connected (No DTLS connection available)
                    presenceService.setSleeping(destination);
                }

                // Call the user's callback
                errorCallback.onError(e);
            }
        });
    }

    @Override
    public void cancelOngoingRequests(Registration registration) {
        delegatedSender.cancelOngoingRequests(registration);
    }
}
