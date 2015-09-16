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

/**
 * Queue management interface allows simple ordering/prioritizing of single queue requests or request sequences.
 */
public interface QueueManagement {

    /**
     * Moves the given queue request to the top of the queue, so it will be processed first.
     *
     * @param queueRequest queue request to move
     */
    void moveTop(QueueRequest queueRequest);

    /**
     * Moves a sequence with the given sequence ID to the top of the queue, so it will processed first.
     *
     * @param endpoint endpoint ID of the client which has the sequence ID assigned
     * @param sequenceId sequence ID of the sequence to move
     */
    void moveSequenceTop(String endpoint, SequenceId sequenceId);

    /**
     * Moves the given queue request to the bottom of the queue, so it will be processed last.
     *
     * @param queueRequest queue request to move
     */
    void moveBottom(QueueRequest queueRequest);

    /**
     * Moves a sequence with the given sequence ID to the bottom of the queue, so it will processed last.
     *
     * @param endpoint endpoint ID of the client which has the sequence ID assigned
     * @param sequenceId sequence ID of the sequence to move
     */
    void moveSequenceBottom(String endpoint, SequenceId sequenceId);

    /**
     * Moves a given request one position up the queue, over a single request or sequence.
     *
     * @param queueRequest queue request to move
     */
    void moveUp(QueueRequest queueRequest);

    /**
     * Moves a sequence with the given ID one position up the queue, over a single request or sequence.
     *
     * @param endpoint endpoint ID of the client which has the sequence ID assigned
     * @param sequenceId sequence ID of the sequence to move
     */
    void moveSequenceUp(String endpoint, SequenceId sequenceId);

    /**
     * Moves a given request one position down the queue, over a single request or sequence.
     *
     * @param queueRequest queue request to move
     */
    void moveDown(QueueRequest queueRequest);

    /**
     * Moves a sequence with the given ID one position down the queue, over a single request or sequence.
     *
     * @param endpoint endpoint ID of the client which has the sequence ID assigned
     * @param sequenceId sequence ID of the sequence to move
     */
    void moveSequenceDown(String endpoint, SequenceId sequenceId);

    /**
     * Removes the given request from the queue.
     *
     * @param queueRequest request to remove.
     */
    void dropRequest(QueueRequest queueRequest);

    /**
     * Removes a sequence of requests for a given client from the queue.
     *
     * @param endpoint endpoint ID of the client which has the sequence ID assigned
     * @param sequenceId sequence ID of the sequence to remove.
     */
    void dropSequence(String endpoint, SequenceId sequenceId);
}
