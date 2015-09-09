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

import org.eclipse.leshan.core.request.DownlinkRequest;
import org.eclipse.leshan.server.client.Client;

/**
 * A QueueRequest is an element of the request queue, containing the actual downlink request, its send expiration (a
 * time until the sender will retry the send), the keep expiration (time until a deferred or an executed request is kept
 * in the queue), the state of processing for this particular queue request, the sequence ID (means the queue request is
 * a single request or it is bound into a sequence with queue requests of same sequence ID) and a response ID, which can
 * be used to associate a response from the client with this particular queue request in ResponseProcessor.
 *
 * @see RequestState
 * @see SequenceId
 * @see ResponseProcessor
 */
public interface QueueRequest {

    /**
     * @return a sequence ID assigned to this queue request
     */
    SequenceId getSequenceId();

    /**
     * @return a response ID assigned to this queue request
     */
    Long getResponseId();

    /**
     * @return LwM2M client to send the request to
     */
    Client getClient();

    /**
     * @return the actual downlink request which is to be send
     */
    DownlinkRequest<?> getDownlinkRequest();

    /**
     * @return timestamp of the date, when the time-to-send for the request expires and the sender will not anymore try to send it.
     */
    long getSendExpiration();

    /**
     * @return true, if the time interval for sending the request passed, i.e. send time is elapsed.
     */
    boolean isSendExpirationReached();

    /**
     * @return timestamp of the date, when the time-to-keep for the request expires and the request may be removed from the queue.
     */
    long getKeepExpiration();

    /**
     * @return true, if the time interval to keep the request in queue passed, i.e. the keep time is elapsed.
     */
    boolean isKeepExpirationReached();

    /**
     * @return the current state of this request
     */
    RequestState getRequestState();

}
