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

/**
 * Possible valid states in a lifetime of a queue request.
 *
 * @see QueueRequest
 */
public enum QueueRequestState {
    /** Request was put into the queue. */
    ENQUEUED,

    /** Request is being processed, i.e. sending. */
    PROCESSING,

    /**
     * Request is being processed, but user deleted it. In this state the running request gets executed until the next
     * timeout or success. In case of success the request gets handled as it was not deleted, i.e. response callback
     * gets called. In case of timeout the request gets deleted and the error callback with
     * {@link QueueRequestDeletedException} gets called.
     */
    PROCESSING_DELETED,

    /** Request could not be sent to the client and was deferred. */
    DEFERRED,

    /** Request was sent successfully. */
    EXECUTED,

    /** Request has been elapsed. */
    TTL_ELAPSED
}
