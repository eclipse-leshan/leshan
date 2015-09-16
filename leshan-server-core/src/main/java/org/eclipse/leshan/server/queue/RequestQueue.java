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
package org.eclipse.leshan.server.queue;

import java.util.Collection;
import java.util.Set;

/**
 * A request queue represents a queue which is used to queue and process LwM2M downlink requests. It supports a simple
 * state transition operations for queue requests which is used in the queue's implementation, for instance:
 * <ul>
 * <li>A request is initially enqueued;</li>
 * <li>A request is being processed;</li>
 * <li>A request is deferred (because of a timeout);</li>
 * <li>A request's time-to-live is elapsed;</li>
 * <li>A request is executed;</li>
 * <li>A request is unqueued from the queue.</li>
 * </ul>
 *
 * @see SequenceId
 * @see QueueRequest
 * @see RequestState
 */
public interface RequestQueue {

    /**
     * Enqueues a request and returns a unique sequence ID which is associated to this request.
     *
     * @param queueRequest request to enqueue
     * @return sequence ID associated with the request
     */
    SequenceId enqueueRequest(QueueRequest queueRequest);

    /**
     * Enqueues a request which is also associated with an existing sequence ID.
     *
     * @param queueRequest request to enqueue
     * @param existingId sequence ID to add the queue request to
     */
    void enqueueRequest(QueueRequest queueRequest, SequenceId existingId);

    /**
     * Sets the request in a processing state (it is currently being sent).
     *
     * @param queueRequest request which is being processed
     */
    void processingRequest(QueueRequest queueRequest);

    /**
     * Sets the request to a deferred state (i.e. the request could not be sent and is deferred for a next client's
     * update).
     *
     * @param queueRequest queue request to defer
     */
    void deferRequest(QueueRequest queueRequest);

    /**
     * Sets the request in TTL_ELAPSED state (the time-to-live for the request has been reached, it will be not sent
     * anymore, but is kept at least until keep expiration has reached).
     *
     * @param queueRequest queue request to promote to TTL_ELAPSED state
     */
    void ttlElapsedRequest(QueueRequest queueRequest);

    /**
     * Sets the request to executed state, i.e. the request was transmitted successfully.
     *
     * @param queueRequest request to set to the executed state
     */
    void executedRequest(QueueRequest queueRequest);

    /**
     * Remove (unqueue) the request from the queue, for instance, if its keep expiration has been reached.
     *
     * @param queueRequest queue request to unqueue
     */
    void unqueueRequest(QueueRequest queueRequest);

    /**
     * Provides an unmodifiable collection (snapshot) of all requests for a client with the given endpoint ID.
     *
     * @param endpoint endpoint ID of a LwM2M client
     * @return unmodifiable collection (snapshot) of all requests for a given client.
     */
    Collection<QueueRequest> getRequests(String endpoint);

    /**
     * Provides an unmodifiable set (snapshot) of endpoint IDs of clients which have their requests in the queue.
     *
     * @return an unmodifiable set (snapshot) of endpoint IDs of clients which have their requests in the queue.
     */
    Set<String> getEndpoints();
}
