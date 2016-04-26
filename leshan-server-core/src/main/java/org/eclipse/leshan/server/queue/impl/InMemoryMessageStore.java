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
 *     Alexander Ellwein, Daniel Maier (Bosch Software Innovations GmbH)
 *                                - initial API and implementation
 *     Balasubramanian Azhagappan (Bosch Software Innovations GmbH)
 *                              - added isEmpty method
 *******************************************************************************/
package org.eclipse.leshan.server.queue.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.LinkedBlockingQueue;

import org.eclipse.leshan.server.queue.MessageStore;
import org.eclipse.leshan.server.queue.QueuedRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * provides a simple in-memory persistence implementation of the request queue.
 */
public class InMemoryMessageStore implements MessageStore {
    private static Logger LOG = LoggerFactory.getLogger(InMemoryMessageStore.class);
    private final ConcurrentMap<String, BlockingQueue<QueuedRequest>> requestQueueMap = new ConcurrentHashMap<>();

    @Override
    public void add(QueuedRequest entity) {
        LOG.debug("Add entity {}", entity);
        String endpoint = entity.getEndpoint();
        BlockingQueue<QueuedRequest> requestQueue = getMessageQueueForEndpoint(endpoint);
        requestQueue.add(entity);
        requestQueueMap.putIfAbsent(endpoint, requestQueue);
    }

    @Override
    public boolean isEmpty(String endpoint) {
        return getMessageQueueForEndpoint(endpoint).isEmpty();
    }

    @Override
    public QueuedRequest retrieveFirst(String endpoint) {
        LOG.trace("Retrieve first for endpoint {}", endpoint);
        BlockingQueue<QueuedRequest> requests = getMessageQueueForEndpoint(endpoint);
        return requests.peek();
    }

    @Override
    public void removeAll(String endpoint) {
        LOG.debug("Emptying messages for client {}", endpoint);
        // If client has registers and de-registers without any other messages
        // then the queue would not have been initialized.
        if (requestQueueMap.get(endpoint) != null) {
            requestQueueMap.remove(endpoint);
        }
    }

    @Override
    public void deleteFirst(String endpoint) {
        LOG.debug("Delete first entity of endpoint {}", endpoint);
        BlockingQueue<QueuedRequest> requests = requestQueueMap.get(endpoint);
        if (requests != null) {
            requests.poll();
        }
    }

    /**
     * Retrieves a whole queue for a given client's endpoint, in order of processing. Used only for testing purposes.
     *
     * @param endpoint client's endpoint
     * @return list of queue request entities in order of processing.
     */
    public List<QueuedRequest> retrieveAll(String endpoint) {
        LOG.debug("Retrieve all for endpoint {}", endpoint);
        BlockingQueue<QueuedRequest> requests = getMessageQueueForEndpoint(endpoint);
        if (requests.isEmpty()) {
            return Collections.emptyList();
        } else {
            return new ArrayList<>(requests);
        }
    }

    private BlockingQueue<QueuedRequest> getMessageQueueForEndpoint(String endpoint) {
        return requestQueueMap.get(endpoint) != null ? requestQueueMap.get(endpoint)
                : new LinkedBlockingQueue<QueuedRequest>();
    }

}

