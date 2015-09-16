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
package org.eclipse.leshan.server.queue.impl;

import org.eclipse.leshan.core.request.DownlinkRequest;
import org.eclipse.leshan.server.client.Client;
import org.eclipse.leshan.server.queue.QueueRequest;
import org.eclipse.leshan.server.queue.QueueRequestFactory;
import org.eclipse.leshan.server.queue.RequestState;
import org.eclipse.leshan.server.queue.SequenceId;
import org.eclipse.leshan.util.Validate;

/**
 * The queue request factory is responsible to create queue requests and do state transitions on them. In this
 * particular implementation, the queue requests are mutable and only this class is in charge of manipulation on queue
 * requests.
 *
 * @see QueueRequest
 * @see RequestState
 */
public class QueueRequestFactoryImpl implements QueueRequestFactory {

    /**
     * A simple mutable implementation of a queue request.
     */
    public static class QueueRequestImpl implements QueueRequest {

        private final Client client;
        private final DownlinkRequest<?> downlinkRequest;
        private SequenceId sequenceId;
        private RequestState requestState;
        private final long sendExpiration;
        private final long keepExpiration;
        private final Long responseId;

        private QueueRequestImpl(Client client, DownlinkRequest<?> downlinkRequest, long sendExpiration,
                long keepExpiration, long responseId) {
            Validate.notNull(client, "client cannot be null");
            Validate.notNull(downlinkRequest, "request cannot be null");
            Validate.isTrue(keepExpiration >= sendExpiration, "keep expiration date must be after the send expiration");
            this.client = client;
            this.downlinkRequest = downlinkRequest;
            this.sequenceId = SequenceId.NONE;
            this.requestState = RequestState.UNKNOWN;

            this.sendExpiration = System.nanoTime() + sendExpiration;
            this.keepExpiration = System.nanoTime() + keepExpiration;
            this.responseId = responseId;
        }

        @Override
        public SequenceId getSequenceId() {
            return sequenceId;
        }

        @Override
        public Client getClient() {
            return client;
        }

        @Override
        public DownlinkRequest<?> getDownlinkRequest() {
            return downlinkRequest;
        }

        @Override
        public long getSendExpiration() {
            return sendExpiration;
        }

        @Override
        public long getKeepExpiration() {
            return keepExpiration;
        }

        @Override
        public RequestState getRequestState() {
            return requestState;
        }

        private void setRequestState(RequestState state) {
            this.requestState = state;
        }

        private void setSequenceId(SequenceId newSeqId) {
            this.sequenceId = newSeqId;
        }

        @Override
        public Long getResponseId() {
            return responseId;
        }

        @Override
        public boolean isSendExpirationReached() {
            return System.nanoTime() >= sendExpiration;
        }

        @Override
        public boolean isKeepExpirationReached() {
            return System.nanoTime() >= keepExpiration;
        }

        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder();
            builder.append("QueueRequestImpl [client=");
            builder.append(client);
            builder.append(", downlinkRequest=");
            builder.append(downlinkRequest);
            builder.append(", sequenceId=");
            builder.append(sequenceId);
            builder.append(", requestState=");
            builder.append(requestState);
            builder.append(", sendExpiration=");
            builder.append(sendExpiration);
            builder.append(", keepExpiration=");
            builder.append(keepExpiration);
            builder.append(", responseId=");
            builder.append(responseId);
            builder.append("]");
            return builder.toString();
        }
    }

    @Override
    public QueueRequest createQueuedRequest(Client client, DownlinkRequest<?> request, long sendExpiration,
            long keepExpiration, long responseId) {
        return new QueueRequestImpl(client, request, sendExpiration, keepExpiration, responseId);
    }

    @Override
    public QueueRequest transformRequest(QueueRequest request, SequenceId sequenceIdToSet, RequestState newState) {
        ((QueueRequestImpl) request).setRequestState(newState);
        ((QueueRequestImpl) request).setSequenceId(sequenceIdToSet);
        return request;
    }
}
