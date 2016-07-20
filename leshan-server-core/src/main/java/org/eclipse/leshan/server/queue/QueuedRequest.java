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

import org.eclipse.leshan.core.request.DownlinkRequest;
import org.eclipse.leshan.core.response.LwM2mResponse;

/**
 * QueuedRequest is wrapper around {@link DownlinkRequest} along with the current state of the request.
 */
public interface QueuedRequest {

    /**
     * requestTicket represents the correlation identifier used to correlate any response for this request from LWM2M
     * Client.
     * 
     * @return ID.
     */
    String getRequestTicket();

    /**
     * @return client's endpoint to send the request to
     */
    String getEndpoint();

    /**
     * @return the actual downlink request which is to be send
     */
    DownlinkRequest<LwM2mResponse> getDownlinkRequest();
}
