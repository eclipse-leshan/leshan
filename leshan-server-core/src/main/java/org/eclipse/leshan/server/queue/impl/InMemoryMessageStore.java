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
        BlockingQueue<QueuedRequest> requestQueue = new LinkedBlockingQueue<>();

        // If two Threads want to add a entity at the exact instant, each will
        // retrieve its own instance of
        // Queue and add the entity to the respective instances. But only one of
        // them wins the putIfAbsent method.
        // To not lose messages, this check will prevent.
        BlockingQueue<QueuedRequest> allreadyExisting = requestQueueMap.putIfAbsent(endpoint, requestQueue);
        if (allreadyExisting != null) {
            allreadyExisting.add(entity);
        } else {
            requestQueue.add(entity);
        }
    }

    @Override
    public boolean isEmpty(String endpoint) {
        LOG.trace("Checking for empty Queue {}", endpoint);
        BlockingQueue<QueuedRequest> requests = requestQueueMap.get(endpoint);
        if (requests != null) {
            return requests.isEmpty();
        }
        return true;
    }

    @Override
    public QueuedRequest retrieveFirst(String endpoint) {
        LOG.trace("Retrieve first for endpoint {}", endpoint);
        BlockingQueue<QueuedRequest> requests = requestQueueMap.get(endpoint);
        if (requests != null) {
            return requests.peek();
        }
        return null;
    }

    @Override
    public List<QueuedRequest> removeAll(String endpoint) {
        LOG.debug("Emptying messages for client {}", endpoint);
        Queue<QueuedRequest> requests = requestQueueMap.remove(endpoint);
        if (requests != null) {
            return new ArrayList<>(requests);
        } else {
            return Collections.emptyList();
        }
    }

    @Override
    public void deleteFirst(String endpoint) {
        LOG.debug("Delete first entity of endpoint {}", endpoint);
        Queue<QueuedRequest> requests = requestQueueMap.get(endpoint);
        if (requests != null) {
            requests.poll();
        }
    }

    /**
     * Returns the size of the Queue for given endpoint.
     * 
     * @param endpoint client's endpoint
     * @return list of queue request entities in order of processing.
     */
    public int getQueueSize(String endpoint) {
        BlockingQueue<QueuedRequest> requests = requestQueueMap.get(endpoint);
        if (requests != null) {
            return requests.size();
        }
        return 0;
    }
}
