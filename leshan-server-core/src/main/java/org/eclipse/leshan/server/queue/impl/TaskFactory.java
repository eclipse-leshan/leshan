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

import org.eclipse.leshan.core.response.LwM2mResponse;
import org.eclipse.leshan.server.client.ClientRegistry;
import org.eclipse.leshan.server.queue.MessageStore;
import org.eclipse.leshan.server.queue.QueuedRequest;
import org.eclipse.leshan.server.request.LwM2mRequestSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.Executor;

/**
 * Task factory helps to create different tasks, which can be scheduled for the queue mode processing.
 */
class TaskFactory {
    private static final Logger LOG = LoggerFactory.getLogger(TaskFactory.class);
    private final ClientRegistry clientRegistry;
    private final LwM2mRequestSender requestSender;
    private final Executor queueProcessingExecutor;
    private final Executor responseCallbackExecutor;
    private final MessageStore messageStore;
    private final ClientStatusTracker clientStatusTracker;
    private final Map<Long, ResponseContext> responseContextHolder;

    TaskFactory(ClientRegistry clientRegistry,
                LwM2mRequestSender requestSender,
                Executor queueProcessingExecutor,
                Executor responseCallbackExecutor,
                MessageStore messageStore,
                Map<Long, ResponseContext> responseContextHolder,
                ClientStatusTracker clientStatusTracker) {
        this.clientRegistry = clientRegistry;
        this.requestSender = requestSender;
        this.queueProcessingExecutor = queueProcessingExecutor;
        this.responseCallbackExecutor = responseCallbackExecutor;
        this.messageStore = messageStore;
        this.responseContextHolder = responseContextHolder;
        this.clientStatusTracker = clientStatusTracker;
    }

    Runnable newRequestSendingTask(String endpoint) {
        return new RequestSendingTask(clientRegistry, requestSender, this, queueProcessingExecutor,
                responseCallbackExecutor, clientStatusTracker, messageStore, endpoint);
    }

    Runnable newSuccessResponseProcessingTask(final QueuedRequest request, final LwM2mResponse response) {
        return new ResponseProcessingTask(request, responseContextHolder, response);
    }

    Runnable newErrorResponseProcessingTask(final QueuedRequest request, final Exception exception) {
        return new ResponseProcessingTask(request, responseContextHolder, exception);
    }
}
