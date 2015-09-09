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
 *     Alexander Ellwein (Bosch Software Innovations GmbH)
 *                     - initial API and implementation
 *******************************************************************************/
package org.eclipse.leshan.server.queue.reactor;

import org.eclipse.leshan.server.queue.QueueRequest;
import org.eclipse.leshan.server.queue.QueueTask;
import org.eclipse.leshan.server.queue.RequestQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This queue task can be used to purge the queue for a specified client (endpoint).
 */
public class QueuePurgeTask implements QueueTask {

    private final Logger LOG = LoggerFactory.getLogger(QueuePurgeTask.class);
    private RequestQueue requestQueue;
    private String endpoint;

    /**
     * Creates a new queue purge task.
     *
     * @param endpoint endpoint name of the client to purge the queue for.
     */
    public QueuePurgeTask(RequestQueue requestQueue, String endpoint) {
        this.requestQueue = requestQueue;
        this.endpoint = endpoint;
    }

    @Override
    public void run() {

        for (final QueueRequest queueRequest : requestQueue.getRequests(endpoint)) {
            LOG.trace("purging request {} for client {}", queueRequest.getDownlinkRequest(), queueRequest.getClient()
                    .getEndpoint());
            requestQueue.unqueueRequest(queueRequest);
        }
    }

    @Override
    public boolean wouldBlock() {
        return false;
    }

}
