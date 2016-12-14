/*******************************************************************************
 * Copyright (c) 2016 Bosch Software Innovations GmbH and others.
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
 *     Alexander Ellwein, Daniel Maier, Balasubramanian Azhagappan (Bosch Software Innovations GmbH)
 *                                - initial API and implementation
 *******************************************************************************/
package org.eclipse.leshan.server.queue.impl;

import org.eclipse.leshan.core.request.DownlinkRequest;
import org.eclipse.leshan.core.response.LwM2mResponse;
import org.eclipse.leshan.server.client.Registration;
import org.eclipse.leshan.server.client.RegistrationService;
import org.eclipse.leshan.server.queue.MessageStore;
import org.eclipse.leshan.server.queue.QueuedRequest;
import org.eclipse.leshan.server.request.LwM2mRequestSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Request sending task is a Runnable, which is responsible for the actual sending of a queue request. The queue request
 * is sent asynchronously; upon receiving a response or an error the message is removed from queue and a new task is
 * scheduled to process the next message from queue.
 */
class RequestSendingTask implements Runnable {
    private static final Logger LOG = LoggerFactory.getLogger(RequestSendingTask.class);
    private final RegistrationService registrationService;
    private final LwM2mRequestSender requestSender;
    private final String endpoint;
    private final MessageStore messageStore;
    private final ClientStatusTracker clientStatusTracker;

    /**
     * Creates a new task which is responsible for sending a queue request.
     *
     * @param registrationService registration service
     * @param delegateSender sender to perform send on (delegate)
     * @param clientStatusTracker tracks the status of the client
     * @param messageStore holds queued messages for the client
     * @param endpoint clients endpoint identifier
     */
    public RequestSendingTask(RegistrationService registrationService, LwM2mRequestSender delegateSender,
            ClientStatusTracker clientStatusTracker, MessageStore messageStore, String endpoint) {
        this.registrationService = registrationService;
        this.requestSender = delegateSender;
        this.clientStatusTracker = clientStatusTracker;
        this.endpoint = endpoint;
        this.messageStore = messageStore;
    }

    @Override
    public void run() {
        try {
            executeAction();
        } catch (Exception e) {
            LOG.info("error while executing runnable", e);
            throw new RuntimeException(e);
        }
    }

    private void executeAction() {
        QueuedRequest firstRequest = messageStore.retrieveFirst(endpoint);

        if (firstRequest != null) {
            DownlinkRequest<LwM2mResponse> downlinkRequest = firstRequest.getDownlinkRequest();
            LOG.debug("Sending request: {}", downlinkRequest);
            Registration registration = registrationService.getByEndpoint(firstRequest.getEndpoint());
            if (registration == null) {
                // client not registered anymore -> don't send this request
                LOG.debug("Client {} not registered anymore: {}", endpoint, downlinkRequest);
            } else {
                requestSender.send(registration, firstRequest.getRequestTicket(), downlinkRequest);
            }
        } else {
            LOG.debug("No more requests to send to client {}", endpoint);
            clientStatusTracker.stopClientReceiving(endpoint);
        }
    }
}
