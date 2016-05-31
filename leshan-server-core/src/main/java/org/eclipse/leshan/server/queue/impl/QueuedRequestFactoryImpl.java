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
package org.eclipse.leshan.server.queue.impl;

import org.eclipse.leshan.core.request.DownlinkRequest;
import org.eclipse.leshan.core.response.LwM2mResponse;
import org.eclipse.leshan.server.queue.QueuedRequest;
import org.eclipse.leshan.server.queue.QueuedRequestFactory;
import org.eclipse.leshan.util.Validate;

import java.util.concurrent.atomic.AtomicLong;

/**
 * This class provides a queue request factory for use in simple in-memory persistence.
 * 
 * @see InMemoryMessageStore
 */
public class QueuedRequestFactoryImpl implements QueuedRequestFactory {
    private final AtomicLong idCounter = new AtomicLong();

    /**
     * An instance of a queued request along with its current state.
     */
    static class QueuedRequestImpl implements QueuedRequest {

        private final DownlinkRequest<LwM2mResponse> downlinkRequest;
        private final String endpoint;
        private final long requestId;

        private QueuedRequestImpl(long requestId, String endpoint, DownlinkRequest<LwM2mResponse> downlinkRequest) {
            Validate.notNull(endpoint, "endpoint may not be null");
            Validate.notNull(downlinkRequest, "request may not be null");
            this.requestId = requestId;
            this.downlinkRequest = downlinkRequest;
            this.endpoint = endpoint;
        }

        @Override
        public String getEndpoint() {
            return endpoint;
        }

        @Override
        public DownlinkRequest<LwM2mResponse> getDownlinkRequest() {
            return downlinkRequest;
        }

        @Override
        public long getRequestId() {
            return requestId;
        }

        @Override
        public String toString() {
            return "QueuedRequestImpl [downlinkRequest=" + downlinkRequest + ", endpoint=" + endpoint + ", requestId="
                    + requestId + "]";
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + (int) (requestId ^ (requestId >>> 32));
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            QueuedRequestImpl other = (QueuedRequestImpl) obj;
            return requestId == other.requestId;
        }
    }

    @Override
    public QueuedRequest newQueueRequestEntity(String endpoint, DownlinkRequest<LwM2mResponse> request) {
        return new QueuedRequestImpl(idCounter.getAndIncrement(), endpoint, request);
    }
}
