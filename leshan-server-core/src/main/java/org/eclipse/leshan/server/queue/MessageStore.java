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
 *                                  - added removeAll method
 *******************************************************************************/
package org.eclipse.leshan.server.queue;

import java.util.List;

/**
 * a message store provide basic operations to handle storage of requests destined for a client which
 * has connected in Queue Mode. Messages in the message store will be present until one of the following events
 * occur
 *  - Message was sent to the client and was acknowledged
 *  - Client has de-registered
 *  - Client has not sent registration update within the time out period.
 * 
 * @see QueuedRequest
 */
public interface MessageStore {
    /**
     * Adds a new queue request entity to the queue.
     *
     * @param entity queue request to add to the queue
     */
    void add(QueuedRequest entity);

    /**
     * Retrieves first (next, topmost) queue request entity from a given client's queue.
     *
     * @param endpoint client's endpoint
     * @return queue request entity
     */
    QueuedRequest retrieveFirst(String endpoint);

    /**
     * checks whether there are any queued message for the given endpoint
     *
     * @param endpoint client's endpoint
     * @return true if no messages are available, false otherwise.
     */
    boolean isEmpty(String endpoint);

    /**
     * deletes all queued request for a given client endpoint.
     *
     * @param endpoint client's endpoint
     */
    void removeAll(String endpoint);

    /**
     * Deletes the first request from message queue for the given
     * client.
     *
     * @param endpoint client endpoint name
     */
    void deleteFirst(String endpoint);
}
