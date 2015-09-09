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
 * Request state represents the processing state of a queue request.
 *
 * Every created QueueRequest is first in UNKNOWN state, until it is enqueued. After the request has been enqueued, it
 * is promoted to the ENQUEUED state. The request remains in ENQUEUED state, until a LwM2M client sends a registration
 * update (means the client is online now), then the request sender schedules the processing of the request on top,
 * promoting it in the PROCESSING state. A sending can be either successful (request goes to the EXECUTED state) or the
 * server receives a timeout on send, then the request is transferred to the DEFERRED state. A deferred request will be
 * resent next time, when the client is back online, or TTL_ELAPSED, if the time-to-live is elapsed: For all requests in
 * the queue, there is a time-to-live (maximum time until send) and a time-to-keep (maximum time to keep the request in
 * the queue, even after it is sent or deferred).
 */
public enum RequestState {
    /** Unknown (unassigned) state. */
    UNKNOWN,
    /** A request has been initially put into the request queue. */
    ENQUEUED,
    /** A request is currently being processed (sending). */
    PROCESSING,
    /** A request could not be sent and is deferred after client is back with an update. */
    DEFERRED,
    /** A maximum Time-To-Live for the request has been reached and now it is just stored for time-to-keep. */
    TTL_ELAPSED,
    /** A request was executed successfully and now it is stored for time-to-keep. */
    EXECUTED
}
