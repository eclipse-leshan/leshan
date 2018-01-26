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

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

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

        // If the client is sleeping, warn the user and return
        if (!presenceService.isClientAwake(destination)) {
            LOG.info("The destination client is sleeping, request couldn't been sent.");
            return null;
        }

        // Create a response callback to use the asynchronous send method in a synchronous way
        ResponseCallbackQueueModeSynchronous<T> synchronousResponseCallback = new ResponseCallbackQueueModeSynchronous<T>(
                presenceService, destination);

        // Use delegation to send the request
        delegatedSender.send(destination, request, timeout, synchronousResponseCallback, synchronousResponseCallback);

        // Wait for response, then return it
        return synchronousResponseCallback.waitForResponse(timeout);
    }

    @Override
    public <T extends LwM2mResponse> void send(final Registration destination, DownlinkRequest<T> request, long timeout,
            final ResponseCallback<T> responseCallback, final ErrorCallback errorCallback) {

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

    /**
     * Callback to use the asynchronous method of {@link CaliforniumLwM2mRequestSender} in a synchronous way
     */
    public class ResponseCallbackQueueModeSynchronous<T extends LwM2mResponse>
            implements ResponseCallback<T>, ErrorCallback {

        /**
         * @param presenceService the presence service object for setting the client into {@link Presence#SLEEPING} when
         *        request Timeout expires and into {@link Presence#Awake} when a response arrives.
         * @param delegatedSender internal sender that it is used for sending the requests, using delegation.
         */
        private PresenceServiceImpl presenceService;
        private Registration destination;
        private final CountDownLatch latch;
        private T response;

        public ResponseCallbackQueueModeSynchronous(PresenceServiceImpl presenceService, Registration destination) {
            this.presenceService = presenceService;
            this.destination = destination;
            this.response = null;
            latch = new CountDownLatch(1);
        }

        @Override
        public void onResponse(T response) {
            presenceService.setAwake(destination);
            this.response = response;
            latch.countDown();
        }

        @Override
        public void onError(Exception e) {
            latch.countDown();
            if (e instanceof TimeoutException) {
                presenceService.clientNotResponding(destination);
            }
        }

        public T waitForResponse(Long timeout) throws InterruptedException {
            if (timeout != null) {
                latch.await(timeout, TimeUnit.MILLISECONDS);
            } else {
                latch.await();
            }
            return this.response;
        }

    }

}
