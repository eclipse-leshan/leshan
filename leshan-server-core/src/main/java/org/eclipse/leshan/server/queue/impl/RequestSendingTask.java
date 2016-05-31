/*******************************************************************************
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
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
import org.eclipse.leshan.core.request.exception.TimeoutException;
import org.eclipse.leshan.core.response.ErrorCallback;
import org.eclipse.leshan.core.response.LwM2mResponse;
import org.eclipse.leshan.core.response.ResponseCallback;
import org.eclipse.leshan.server.client.Client;
import org.eclipse.leshan.server.client.ClientRegistry;
import org.eclipse.leshan.server.queue.MessageStore;
import org.eclipse.leshan.server.queue.QueuedRequest;
import org.eclipse.leshan.server.request.LwM2mRequestSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.Executor;

/**
 * Request sending task is a Runnable, which is responsible for the actual sending of a queue request. The queue request
 * is sent asynchronously; upon receiving a response or an error the message is removed from queue and a new task is
 * scheduled to process the next message from queue.
 */
class RequestSendingTask implements Runnable {
    private static final Logger LOG = LoggerFactory.getLogger(RequestSendingTask.class);
    private final ClientRegistry clientRegistry;
    private final LwM2mRequestSender requestSender;
    private final Executor processingExecutor;
    private final String endpoint;
    private final MessageStore messageStore;
    private final ClientStatusTracker clientStatusTracker;
    private Map<Long, ResponseContext> responseContextHolder;

    /**
     * Creates a new task which is responsible for sending a queue request.
     *
     * @param clientRegistry client registry
     * @param requestSender sender to perform send on (delegate)
     * @param queueProcessingExecutor executor to schedule subsequent task on
     * @param clientStatusTracker tracks the status of the client
     * @param messageStore holds queued messages for the client
     * @param endpoint clients endpoint identifier
     * @param responseContextHolder holds the callback instances of the users for processing any response or error from
     *        client.
     */
    RequestSendingTask(ClientRegistry clientRegistry, LwM2mRequestSender requestSender,
            Executor queueProcessingExecutor, ClientStatusTracker clientStatusTracker, MessageStore messageStore,
            final String endpoint, Map<Long, ResponseContext> responseContextHolder) {
        this.clientRegistry = clientRegistry;
        this.requestSender = requestSender;
        this.processingExecutor = queueProcessingExecutor;
        this.clientStatusTracker = clientStatusTracker;
        this.endpoint = endpoint;
        this.messageStore = messageStore;
        this.responseContextHolder = responseContextHolder;
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
        final QueuedRequest firstRequest = messageStore.retrieveFirst(endpoint);

        if (firstRequest != null) {
            try {
                final DownlinkRequest<LwM2mResponse> downlinkRequest = firstRequest.getDownlinkRequest();
                LOG.debug("Sending request: {}", downlinkRequest);
                final Client client = clientRegistry.get(firstRequest.getEndpoint());
                if (client == null) {
                    // client not registered anymore -> ignore this request
                    LOG.debug("Client {} not registered anymore: {}", endpoint, downlinkRequest);
                } else {
                    requestSender.send(client, downlinkRequest, new ResponseCallback<LwM2mResponse>() {
                        @Override
                        public void onResponse(LwM2mResponse response) {
                            LOG.debug("request sent successfully: {}", downlinkRequest);
                            processResponse(firstRequest, response);
                        }
                    }, new ErrorCallback() {
                        @Override
                        public void onError(Exception e) {
                            LOG.debug("exception on sending the request: {}", downlinkRequest, e);
                            if (e instanceof TimeoutException) {
                                timeout(client);
                            } else {
                                processException(firstRequest, e);
                            }
                        }
                    });
                }
            } catch (RuntimeException e) {
                processException(firstRequest, e);
            }
        } else {
            LOG.debug("No more requests to send to client {}", endpoint);
            clientStatusTracker.stopClientReceiving(endpoint);
        }
    }

    private void processResponse(final QueuedRequest request, final LwM2mResponse response) {
        LOG.debug("Received Response -> {}", request);
        messageStore.deleteFirst(endpoint);
        processingExecutor.execute(new ResponseProcessingTask(request, responseContextHolder, response));
        processingExecutor.execute(new RequestSendingTask(clientRegistry, requestSender, processingExecutor,
                clientStatusTracker, messageStore, endpoint, responseContextHolder));
    }

    private void processException(final QueuedRequest request, final Exception exception) {
        LOG.debug("Received error response {}", request);
        messageStore.deleteFirst(endpoint);
        processingExecutor.execute(new ResponseProcessingTask(request, responseContextHolder, exception));
        processingExecutor.execute(new RequestSendingTask(clientRegistry, requestSender, processingExecutor,
                clientStatusTracker, messageStore, endpoint, responseContextHolder));
    }

    private void timeout(final Client client) {
        if(LOG.isDebugEnabled()) {
            LOG.debug("Client {} timed out", client.getEndpoint());
        }
        clientStatusTracker.setClientUnreachable(client.getEndpoint());
    }
}
