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

import java.util.concurrent.Executor;

/**
 * Request sending task is a Runnable, which is responsible for the actual sending of a queue request. The queue request
 * is sent asynchronously; upon receiving a response or an error the message is removed from queue and
 * a new task is scheduled to process the next message from queue.
 */
class RequestSendingTask implements Runnable {

    private static final Logger LOG = LoggerFactory.getLogger(RequestSendingTask.class);
    private final ClientRegistry clientRegistry;
    private final LwM2mRequestSender requestSender;
    private final Executor queueProcessingExecutor;
    private final Executor responseCallbackExecutor;
    private final TaskFactory taskFactory;
    private final String endpoint;
    private final MessageStore messageStore;
    private final ClientStatusTracker clientStatusTracker;

    /**
     * Creates a new task which is responsible for sending a queue request.
     *
     * @param clientRegistry client registry
     * @param requestSender sender to perform send on (delegate)
     * @param taskFactory factory to create new tasks for response handling
     * @param queueProcessingExecutor executor to schedule subsequent task on
     */
    RequestSendingTask(ClientRegistry clientRegistry,
                       LwM2mRequestSender requestSender,
                       TaskFactory taskFactory,
                       Executor queueProcessingExecutor,
                       Executor responseCallbackExecutor,
                       ClientStatusTracker clientStatusTracker,
                       MessageStore messageStore, final String endpoint) {
        this.clientRegistry = clientRegistry;
        this.requestSender = requestSender;
        this.taskFactory = taskFactory;
        this.queueProcessingExecutor = queueProcessingExecutor;
        this.responseCallbackExecutor = responseCallbackExecutor;
        this.clientStatusTracker = clientStatusTracker;
        this.endpoint = endpoint;
        this.messageStore = messageStore;
    }

    @Override
    public void run() {
        final QueuedRequest firstRequest = messageStore.retrieveFirst(endpoint);

        if (firstRequest != null) {
            try {
                // TODO send with COAP retries ok? See https://github.com/OpenMobileAlliance/OMA-LwM2M-Public-Review/issues/32
                final DownlinkRequest<LwM2mResponse> downlinkRequest = firstRequest.getDownlinkRequest();
                LOG.debug("Sending request: {}", downlinkRequest);
                final Client client = clientRegistry.get(firstRequest.getEndpoint());
                if (client == null) {
                    // client not registered anymore -> ignore this request
                    LOG.debug("Client {} not registered anymore: {}",
                            firstRequest.getEndpoint(),
                            downlinkRequest);
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
        }
    }

    private void processResponse(final QueuedRequest request, final LwM2mResponse response) {
        LOG.debug("Received Response -> {}", request);
        messageStore.delete(request);
        responseCallbackExecutor.execute(taskFactory.newSuccessResponseProcessingTask(request, response));
        queueProcessingExecutor.execute(taskFactory.newRequestSendingTask(request.getEndpoint()));
    }

    private void processException(final QueuedRequest request, final Exception exception) {
        LOG.debug("Received error response {}", request);
        messageStore.delete(request);
        responseCallbackExecutor.execute(taskFactory.newErrorResponseProcessingTask(request, exception));
        queueProcessingExecutor.execute(taskFactory.newRequestSendingTask(request.getEndpoint()));
    }

    private void timeout(final Client client) {
        LOG.debug("Client {} timed out", client.getEndpoint());
        clientStatusTracker.setClientUnreachable(client.getEndpoint());
    }
}
