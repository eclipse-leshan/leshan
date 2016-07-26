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
 *     Alexander Ellwein, Daniel Maier (Bosch Software Innovations GmbH)
 *                                - initial API and implementation
 *     Balasubramanian Azhagappan (Bosch Software Innovations GmbH)
 *                              - added isEmpty method
 *******************************************************************************/
package org.eclipse.leshan.server.queue.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Queue;
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
    private final ConcurrentMap<String, Queue<QueuedRequest>> requestQueueMap = new ConcurrentHashMap<>();

    @Override
    public void add(QueuedRequest entity) {
        LOG.debug("Add entity {}", entity);
        String endpoint = entity.getEndpoint();
        Queue<QueuedRequest> requestQueue = getMessageQueueForEndpoint(endpoint);
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
        Queue<QueuedRequest> requests = getMessageQueueForEndpoint(endpoint);
        return requests.peek();
    }

    @Override
    public void deleteFirst(String endpoint) {
        LOG.debug("Delete first entity of endpoint {}", endpoint);
        Queue<QueuedRequest> requests = requestQueueMap.get(endpoint);
        if (requests != null) {
            requests.poll();
        }
    }

    @Override
    public List<QueuedRequest> removeAll(String endpoint) {
        Queue<QueuedRequest> requests = requestQueueMap.remove(endpoint);
        if (requests != null) {
            return new ArrayList<>(requests);
        } else {
            return Collections.emptyList();
        }
    }

    /**
     * Retrieves a whole queue for a given client's endpoint, in order of processing. Used only for testing purposes.
     *
     * @param endpoint client's endpoint
     * @return list of queue request entities in order of processing.
     */
    public int getQueueSize(String endpoint) {
        Queue<QueuedRequest> requests = getMessageQueueForEndpoint(endpoint);
        return requests.size();
    }

    /*
     * Returns the total number of messages in the Queue for the given endpoint. Used for testing purposes only. +
     */
    public int getPendingMessagesCount(String endpoint) {
        return getMessageQueueForEndpoint(endpoint).size();
    }

    private Queue<QueuedRequest> getMessageQueueForEndpoint(String endpoint) {
        return requestQueueMap.get(endpoint) != null ? requestQueueMap.get(endpoint)
                : new LinkedBlockingQueue<QueuedRequest>();
    }

}
