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

import org.eclipse.leshan.core.request.DownlinkRequest;
import org.eclipse.leshan.core.response.LwM2mResponse;
import org.eclipse.leshan.server.queue.QueueRequestState;

/**
 * QueueRequestEntity represents a queue request in a persistable form.
 */
public interface QueueRequestEntity {

    /**
     * a request ID is used to keep the natural ascending order of queue requests in the queue. In a specific
     * persistence implementation, it can be, for instance, a record ID field of the particular queue request.
     * 
     * @return ID
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
     * @return the current state of this request
     */
    QueueRequestState getRequestState();
}
