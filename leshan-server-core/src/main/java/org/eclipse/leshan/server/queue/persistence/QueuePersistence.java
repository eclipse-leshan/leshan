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
package org.eclipse.leshan.server.queue.persistence;

import java.util.List;

import org.eclipse.leshan.server.queue.QueueRequestState;

/**
 * Queue persistence implementations provide basic operations to handle persistence of queue requests, which are passed
 * around as entities (QueueRequestEntity object instances).
 * 
 * @see QueueRequestEntity
 */
public interface QueuePersistence {
    /**
     * Adds a new queue request entity to the queue.
     *
     * @param entity queue request to add to the queue
     */
    void add(QueueRequestEntity entity);

    /**
     * Retrieves first (next, topmost) queue request entity from a given client's queue.
     *
     * @param endpoint client's endpoint
     * @return queue request entity
     */
    QueueRequestEntity retrieveFirst(String endpoint);

    /**
     * Retrieves a whole queue for a given client's endpoint, in order of processing.
     *
     * @param endpoint client's endpoint
     * @return list of queue request entities in order of processing.
     */
    List<QueueRequestEntity> retrieveAll(String endpoint);

    /**
     * Retrieves a particular queue request entity for given client's endpoint and queue request ID.
     * 
     * @param endpoint client's endpoint
     * @param requestId request ID
     * @return queue request entity
     */
    QueueRequestEntity retrieveByRequestId(String endpoint, long requestId);

    /**
     * Updates the queue request entity in the persistence.
     *
     * @param oldEntity old entity to update
     * @param newEntity new entity to replace with
     * @return new entity
     */
    QueueRequestEntity update(QueueRequestEntity oldEntity, QueueRequestEntity newEntity);

    /**
     * Deletes a given queue request entity.
     * 
     * @param entity entity to delete
     */
    void delete(QueueRequestEntity entity);

    /**
     * Updates the state of the given queue request to the given new state.
     * 
     * @param entity entity to update state
     * @param newState new state value
     * @return updated queue request entity
     */
    QueueRequestEntity updateState(QueueRequestEntity entity, QueueRequestState newState);
}
