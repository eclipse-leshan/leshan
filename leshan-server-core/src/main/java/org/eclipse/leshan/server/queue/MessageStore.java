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
 * a message store provide basic operations to handle storage of requests destined for a client which has connected in
 * Queue Mode. Messages in the message store will be present until one of the following events occur
 * <p>
 * - Message was sent to the client and was acknowledged either with a response or an error. <br>
 * - Client has de-registered <br>
 * - Client has not sent registration update within the time out period. <br>
 * </p>
 * When the last two events occur, the queue is emptied for the client.
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
     * @return queue request entity for the given endpoint or null, if no QueuedRequest exists.
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
     * Deletes the first request from message queue for the given client.
     *
     * @param endpoint client endpoint name
     */
    void deleteFirst(String endpoint);

    /**
     * removes all the requests from queue and returns the list of all the requests that got removed.
     * 
     * @param endpoint client's endpoint.
     * @return the list of all the requests that got removed.
     */
    List<QueuedRequest> removeAll(String endpoint);
}
