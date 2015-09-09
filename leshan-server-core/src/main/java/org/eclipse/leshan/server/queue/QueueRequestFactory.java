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
 * Queue request factory is used for creation or transition of the queue requests.
 */
public interface QueueRequestFactory {

    /**
     * Creates a new (unqueued) queue request.
     *
     * @param client client to send the request to
     * @param request request to send
     * @param sendExpiration amount of time in nanoseconds, after that the request expires (will not be send anymore)
     * @param keepExpiration amount of time in nanoseconds, after that the request may be removed from the queue
     * @param responseId response ID associated with this request, may be used in ResponseProcessor
     * @return new unqueued queue request
     */
    public QueueRequest createQueuedRequest(Client client, DownlinkRequest<?> request, long sendExpiration,
            long keepExpiration, long responseId);

    /**
     * Transforms the queue request to a new state.
     *
     * @param request request to transform
     * @param sequenceIdToSet new sequence ID to be set
     * @param newState new state to set
     * @return transformed queue request
     */
    public QueueRequest transformRequest(QueueRequest request, SequenceId sequenceIdToSet, RequestState newState);
}
