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

import java.util.Calendar;

import org.eclipse.leshan.core.request.DownlinkRequest;
import org.eclipse.leshan.core.response.LwM2mResponse;

/**
 * A queue request represents an element of the request queue, maintained by QueueRequestSender. A queue request
 * contains the data associated with the actual downlink request.
 */
public interface QueueRequest {

    /**
     * @return a request ID assigned to this queue request
     */
    long getRequestId();

    /**
     * @return client's endpoint to send the request to
     */
    String getEndpoint();

    /**
     * @return the actual downlink request which is to be send
     */
    DownlinkRequest<LwM2mResponse> getDownlinkRequest();

    /**
     * @return calendar date, when the time-to-send for the request expires and the sender will not anymore try to send
     *         it.
     */
    Calendar getSendExpiration();

    /**
     * @return true, if the time interval for sending the request passed, i.e. send time is elapsed.
     */
    boolean isSendExpirationReached();

    /**
     * @return the current state of this request
     */
    QueueRequestState getState();
}
