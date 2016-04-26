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
 * Queue request factory is used for creation of the queue request entities (persistable queue requests).
 */
public interface QueuedRequestFactory {

    /**
     * Creates new queue request entity.
     *
     * @param endpoint client's endpoint name
     * @param request request
     *
     * @return new QueuedRequest
     */
    QueuedRequest newQueueRequestEntity(String endpoint, DownlinkRequest<LwM2mResponse> request);
}
