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
 *******************************************************************************/
package org.eclipse.leshan.server.queue;

import java.util.List;

/**
 * Queue Management interface allows users of QueueRequestSender to monitor the requests being queued and to influence
 * them in some way, e.g. drop single request or even a whole queue.
 *
 * @see QueueRequestSender
 */
public interface QueueManagement {

    /**
     * Get all requests for a given client endpoint.
     * 
     * @param endpoint client endpoint
     * @return list of queue requests
     */
    List<QueueRequest> getRequests(String endpoint);

    /**
     * Removes the given request from the queue.
     *
     * @param queueRequest request to remove.
     */
    void dropRequest(QueueRequest queueRequest);

    /**
     * Remove all requests from the queue for a given client endpoint.
     *
     * @param endpoint client endpoint
     */
    void dropAllRequests(String endpoint);
}
