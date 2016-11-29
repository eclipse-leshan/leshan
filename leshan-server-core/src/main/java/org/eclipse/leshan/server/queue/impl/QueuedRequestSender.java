/*******************************************************************************
 * Copyright (c) 2016 Bosch Software Innovations GmbH and others.
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
 *     Alexander Ellwein, Daniel Maier, Balasubramanian Azhagappan (Bosch Software Innovations GmbH)
 *                                - initial API and implementation
 *******************************************************************************/
package org.eclipse.leshan.server.queue.impl;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.eclipse.leshan.core.observation.Observation;
import org.eclipse.leshan.core.request.DownlinkRequest;
import org.eclipse.leshan.core.request.exception.RequestCanceledException;
import org.eclipse.leshan.core.request.exception.TimeoutException;
import org.eclipse.leshan.core.response.ErrorCallback;
import org.eclipse.leshan.core.response.LwM2mResponse;
import org.eclipse.leshan.core.response.ObserveResponse;
import org.eclipse.leshan.core.response.ResponseCallback;
import org.eclipse.leshan.server.Stoppable;
import org.eclipse.leshan.server.client.Client;
import org.eclipse.leshan.server.client.ClientUpdate;
import org.eclipse.leshan.server.client.RegistrationListener;
import org.eclipse.leshan.server.client.RegistrationService;
import org.eclipse.leshan.server.observation.ObservationRegistry;
import org.eclipse.leshan.server.observation.ObservationRegistryListener;
import org.eclipse.leshan.server.queue.MessageStore;
import org.eclipse.leshan.server.queue.QueuedRequest;
import org.eclipse.leshan.server.request.LwM2mRequestSender;
import org.eclipse.leshan.server.response.ResponseListener;
import org.eclipse.leshan.server.response.ResponseProcessingTask;
import org.eclipse.leshan.util.NamedThreadFactory;
import org.eclipse.leshan.util.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This sender is a special implementation of a {@link LwM2mRequestSender} . This sender assumes the client is using "Q"
 * (queue) mode and processes the request using the internal queue, i.e. the request is enqueued first and then sent if
 * a client is back online.
 */
public class QueuedRequestSender implements LwM2mRequestSender, Stoppable {
    private static final Logger LOG = LoggerFactory.getLogger(QueuedRequestSender.class);
    private final LwM2mRequestSender delegateSender;
    private final ExecutorService processingExecutor = Executors
            .newCachedThreadPool(new NamedThreadFactory("leshan-qmode-processingExecutor-%d"));
    private final MessageStore messageStore;
    private final QueueModeRegistrationListener queueModeRegistrationListener;
    private final QueueModeObservationRegistryListener queueModeObservationRegistryListener;
    private final RegistrationService registrationService;
    private final ObservationRegistry observationRegistry;
    private final ClientStatusTracker clientStatusTracker;
    private final Collection<ResponseListener> responseListeners = new ConcurrentLinkedQueue<>();

    private final ReadWriteLock readWriteLock = new ReentrantReadWriteLock();

    /**
     * Creates a new QueueRequestSender using given builder.
     *
     * @param builder sender builder
     */
    private QueuedRequestSender(Builder builder) {
        this.messageStore = builder.messageStore;
        this.registrationService = builder.registrationService;
        this.observationRegistry = builder.observationRegistry;
        this.delegateSender = builder.delegateSender;

        this.clientStatusTracker = new ClientStatusTracker();

        this.queueModeRegistrationListener = new QueueModeRegistrationListener();
        registrationService.addListener(queueModeRegistrationListener);
        this.queueModeObservationRegistryListener = new QueueModeObservationRegistryListener();
        observationRegistry.addListener(queueModeObservationRegistryListener);
        delegateSender.addResponseListener(createResponseListener());
    }

    /**
     * fluent api builder to construct an instance of {@link QueuedRequestSender}
     * 
     * @return instance of {@link QueuedRequestSender.Builder}
     */
    public static Builder builder() {
        return new Builder();
    }

    @Override
    public <T extends LwM2mResponse> T send(Client destination, DownlinkRequest<T> request, Long requestTimeout)
            throws InterruptedException {
        throw new UnsupportedOperationException("QueueMode doesn't support sending of messages synchronously");
    }

    @Override
    public <T extends LwM2mResponse> void send(Client destination, DownlinkRequest<T> request,
            ResponseCallback<T> responseCallback, ErrorCallback errorCallback) {
        throw new UnsupportedOperationException(
                "QueueMode doesn't support sending of messages with callbacks. Use a request ticket instead and register your response listeners");
    }

    @Override
    public <T extends LwM2mResponse> void send(Client destination, String requestTicket, DownlinkRequest<T> request) {
        LOG.trace("send(requestTicket={})", requestTicket);
        // safe because DownlinkRequest does not use generic itself
        @SuppressWarnings("unchecked")
        DownlinkRequest<LwM2mResponse> castedDownlinkRequest = (DownlinkRequest<LwM2mResponse>) request;
        LOG.trace("Sending request {} with queue mode", castedDownlinkRequest);

        String endpoint = destination.getEndpoint();

        readWriteLock.readLock().lock();
        try {
            // Check whether client is still known to ClientRegistry
            if (registrationService.getByEndpoint(endpoint) != null) {
                QueuedRequest queuedRequest = new QueuedRequestImpl(endpoint, castedDownlinkRequest, requestTicket);
                messageStore.add(queuedRequest);
                // If Client is reachable and this is the first message, we send it
                // immediately.
                if (clientStatusTracker.startClientReceiving(endpoint)) {
                    processingExecutor.execute(newRequestSendingTask(endpoint));
                }
            } else {
                String message = String.format("message received in Queue Mode for the unknown client [%s]", endpoint);
                LOG.warn(message);
                // notify application layer that the message will not be sent for unknown client
                processingExecutor.execute(new ResponseProcessingTask(destination, requestTicket, responseListeners,
                        new RequestCanceledException(message)));
            }
        } finally {
            readWriteLock.readLock().unlock();
        }
    }

    @Override
    public void addResponseListener(ResponseListener listener) {
        responseListeners.add(listener);
    }

    @Override
    public void removeResponseListener(ResponseListener listener) {
        responseListeners.remove(listener);
    }

    @Override
    public void stop() {
        registrationService.removeListener(queueModeRegistrationListener);
        observationRegistry.removeListener(queueModeObservationRegistryListener);
        processingExecutor.shutdown();
        try {
            boolean queueProcessingExecutorTerminated = processingExecutor.awaitTermination(5, TimeUnit.SECONDS);
            if (!(queueProcessingExecutorTerminated)) {
                LOG.debug("Could not stop all executors within timeout. processingExecutor stopped: {}, ",
                        queueProcessingExecutorTerminated);
            }
        } catch (InterruptedException e) {
            LOG.debug("Interrupted while stopping. Abort stopping.");
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public void cancelPendingRequests(Client client) {
        Validate.notNull(client);

        if (LOG.isDebugEnabled()) {
            LOG.debug("Cancelling and removing all pending messages for client {}", client.getEndpoint());
        }

        readWriteLock.writeLock().lock();
        try {
            // Potentially the first message in the queue would have been sent.
            // So try to cancel it now.
            delegateSender.cancelPendingRequests(client);

            // It is better to notify the listener with a RequestCanceledException
            // for each queued message to keep the behavior consistent with CaliforniumLwM2mRequestSender.
            List<QueuedRequest> removedMessages = messageStore.removeAll(client.getEndpoint());
            for (QueuedRequest request : removedMessages) {
                processingExecutor.execute(new ResponseProcessingTask(client, request.getRequestTicket(),
                        responseListeners, new RequestCanceledException("Queued message cancelled")));
            }
        } finally {
            readWriteLock.writeLock().unlock();
        }
    }

    /**
     * creates a response listener for response and error responses from clients using queue mode.
     * 
     * @return instance of {@link ResponseListener}
     */
    private ResponseListener createResponseListener() {
        return new ResponseListener() {

            @Override
            public void onResponse(Client client, String requestTicket, LwM2mResponse response) {
                // process only if the client has used Queue mode.
                if (client != null && client.usesQueueMode()) {
                    LOG.trace("response received in Queue mode successfully: {}", requestTicket);
                    processResponse(client, requestTicket, response);
                }
            }

            @Override
            public void onError(Client client, String requestTicket, Exception exception) {
                // process only if the client has used Queue mode.
                if (client != null && client.usesQueueMode()) {
                    LOG.debug("exception on sending the request: {}", requestTicket, exception);
                    if (exception instanceof TimeoutException) {
                        timeout(client.getEndpoint());
                    } else {
                        processException(client, requestTicket, exception);
                    }
                }
            }
        };
    }

    private void processResponse(Client client, String requestTicket, LwM2mResponse response) {
        LOG.debug("Received Response -> {}", requestTicket);
        messageStore.deleteFirst(client.getEndpoint());
        processingExecutor.execute(new ResponseProcessingTask(client, requestTicket, responseListeners, response));
        processingExecutor.execute(newRequestSendingTask(client.getEndpoint()));
    }

    private void processException(Client client, String requestTicket, Exception exception) {
        LOG.debug("Received error response {}", requestTicket);
        messageStore.deleteFirst(client.getEndpoint());
        processingExecutor.execute(new ResponseProcessingTask(client, requestTicket, responseListeners, exception));
        // If RequestCanceledException is thrown due to cancelPendingMessages call, then there
        // is no use processing next requests which would be removed in the next few moments.
        if (!(exception instanceof RequestCanceledException)) {
            processingExecutor.execute(newRequestSendingTask(client.getEndpoint()));
        }
    }

    private void timeout(String clientEndpoint) {
        LOG.debug("Client {} timed out", clientEndpoint);
        clientStatusTracker.setClientUnreachable(clientEndpoint);
    }

    private RequestSendingTask newRequestSendingTask(String clientEndpoint) {
        return new RequestSendingTask(registrationService, delegateSender, clientStatusTracker, messageStore,
                clientEndpoint);
    }

    private final class QueueModeObservationRegistryListener implements ObservationRegistryListener {
        @Override
        public void newValue(Observation observation, ObserveResponse response) {
            Client client = registrationService.getById(observation.getRegistrationId());
            // if client de-registered already, do nothing.
            if (client == null) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("No registered client found for registrationId: {}. Request is not sent.",
                            observation.getRegistrationId());
                }
                return;
            }
            if (client.usesQueueMode() && clientStatusTracker.setClientReachable(client.getEndpoint())
                    && clientStatusTracker.startClientReceiving(client.getEndpoint())) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Notify from {}. Sending queued requests.", client.getEndpoint());
                }
                processingExecutor.execute(newRequestSendingTask(client.getEndpoint()));
            }
        }

        @Override
        public void cancelled(Observation observation) {
            // not used here
        }

        @Override
        public void newObservation(Observation observation) {
            // not used here
        }
    }

    private final class QueueModeRegistrationListener implements RegistrationListener {
        @Override
        public void registered(Client client) {
            // When client is in QueueMode
            if (client.usesQueueMode()) {
                clientStatusTracker.setClientReachable(client.getEndpoint());
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Client {} registered.", client.getEndpoint());
                }
            }
        }

        @Override
        public void updated(ClientUpdate update, Client clientUpdated) {
            // When client is in QueueMode and was previously in unreachable
            // state and when RECEIVING state could be set
            // i.e:- there is no other message currently being sent
            if (clientUpdated.usesQueueMode() && clientStatusTracker.setClientReachable(clientUpdated.getEndpoint())
                    && clientStatusTracker.startClientReceiving(clientUpdated.getEndpoint())) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Client {} updated. Sending queued request.", clientUpdated.getEndpoint());
                }
                processingExecutor.execute(newRequestSendingTask(clientUpdated.getEndpoint()));
            }
        }

        @Override
        public void unregistered(Client client) {
            if (client.usesQueueMode()) {
                clientStatusTracker.clearClientState(client.getEndpoint());
            }
        }
    }

    /**
     * Builder, which helps to create a QueuedRequestSender instance.
     */
    public static class Builder {

        private MessageStore messageStore;
        private LwM2mRequestSender delegateSender;
        private RegistrationService registrationService;
        private ObservationRegistry observationRegistry;

        public Builder setMessageStore(MessageStore messageStore) {
            this.messageStore = messageStore;
            return this;
        }

        public Builder setRequestSender(LwM2mRequestSender delegateSender) {
            this.delegateSender = delegateSender;
            return this;
        }

        public Builder setRegistrationService(RegistrationService registrationService) {
            this.registrationService = registrationService;
            return this;
        }

        public Builder setObservationRegistry(ObservationRegistry observationRegistry) {
            this.observationRegistry = observationRegistry;
            return this;
        }

        public QueuedRequestSender build() {
            Validate.notNull(messageStore, "messageStore cannot be null");
            Validate.notNull(delegateSender, "delegateSender cannot be null");
            Validate.notNull(registrationService, "registrationService cannot be null");
            Validate.notNull(observationRegistry, "observationRegistry cannot be null");

            return new QueuedRequestSender(this);
        }
    }

    /**
     * An instance of a queued request along with its meta data (endpoint and requestTicket).
     */
    static class QueuedRequestImpl implements QueuedRequest {

        private final DownlinkRequest<LwM2mResponse> downlinkRequest;
        private final String endpoint;
        private final String requestTicket;

        private QueuedRequestImpl(String endpoint, DownlinkRequest<LwM2mResponse> downlinkRequest,
                String requestTicket) {
            Validate.notNull(endpoint, "endpoint may not be null");
            Validate.notNull(downlinkRequest, "request may not be null");
            this.downlinkRequest = downlinkRequest;
            this.endpoint = endpoint;
            this.requestTicket = requestTicket;
        }

        @Override
        public String getEndpoint() {
            return endpoint;
        }

        @Override
        public DownlinkRequest<LwM2mResponse> getDownlinkRequest() {
            return downlinkRequest;
        }

        /*
         * (non-Javadoc)
         * 
         * @see org.eclipse.leshan.server.queue.QueuedRequest#getRequestTicket()
         */
        @Override
        public String getRequestTicket() {
            return this.requestTicket;
        }

        @Override
        public String toString() {
            return new StringBuilder().append("QueuedRequestImpl [requestTicket=").append(requestTicket)
                    .append(", downlinkRequest=").append(downlinkRequest).append(", endpoint=" + endpoint)
                    .append(", requestId=").append("]").toString();
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + (requestTicket.hashCode() ^ (requestTicket.hashCode() >>> 32));
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
            return requestTicket == other.requestTicket;
        }
    }
}
