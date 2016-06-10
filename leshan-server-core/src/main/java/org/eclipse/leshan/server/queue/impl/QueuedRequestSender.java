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
 *     Alexander Ellwein, Daniel Maier, Balasubramanian Azhagappan (Bosch Software Innovations GmbH)
 *                                - initial API and implementation
 *******************************************************************************/
package org.eclipse.leshan.server.queue.impl;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.eclipse.leshan.core.node.LwM2mNode;
import org.eclipse.leshan.core.observation.Observation;
import org.eclipse.leshan.core.request.BindingMode;
import org.eclipse.leshan.core.request.DownlinkRequest;
import org.eclipse.leshan.core.request.exception.TimeoutException;
import org.eclipse.leshan.core.response.ErrorCallback;
import org.eclipse.leshan.core.response.LwM2mResponse;
import org.eclipse.leshan.core.response.ResponseCallback;
import org.eclipse.leshan.server.Stoppable;
import org.eclipse.leshan.server.client.Client;
import org.eclipse.leshan.server.client.ClientRegistry;
import org.eclipse.leshan.server.client.ClientRegistryListener;
import org.eclipse.leshan.server.client.ClientUpdate;
import org.eclipse.leshan.server.observation.ObservationRegistry;
import org.eclipse.leshan.server.observation.ObservationRegistryListener;
import org.eclipse.leshan.server.queue.MessageStore;
import org.eclipse.leshan.server.queue.QueuedRequest;
import org.eclipse.leshan.server.queue.QueuedRequestFactory;
import org.eclipse.leshan.server.request.LwM2mRequestSender;
import org.eclipse.leshan.server.response.ResponseListener;
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
    private final QueuedRequestFactory queuedRequestFactory;
    private final QueueModeClientRegistryListener queueModeClientRegistryListener;
    private final QueueModeObservationRegistryListener queueModeObservationRegistryListener;
    private final ClientRegistry clientRegistry;
    private final ObservationRegistry observationRegistry;
    private final ClientStatusTracker clientStatusTracker;

    /**
     * Creates a new QueueRequestSender using given builder.
     *
     * @param builder sender builder
     */
    private QueuedRequestSender(final Builder builder) {
        this.messageStore = builder.messageStore;
        this.queuedRequestFactory = builder.queuedRequestFactory;
        this.clientRegistry = builder.clientRegistry;
        this.observationRegistry = builder.observationRegistry;
        this.delegateSender = builder.delegateSender;

        this.clientStatusTracker = new ClientStatusTracker();

        this.queueModeClientRegistryListener = new QueueModeClientRegistryListener();
        clientRegistry.addListener(queueModeClientRegistryListener);
        this.queueModeObservationRegistryListener = new QueueModeObservationRegistryListener();
        observationRegistry.addListener(queueModeObservationRegistryListener);
    }

    public static Builder builder() {
        return new Builder();
    }

    @SuppressWarnings("deprecation")
    @Override
    public <T extends LwM2mResponse> T send(final Client registrationInfo, final DownlinkRequest<T> request,
            final Long requestTimeout) throws InterruptedException {
        throw new UnsupportedOperationException("QueueMode doesn't support sending sending of messages synchronously");
    }

    @SuppressWarnings("deprecation")
    @Override
    public <T extends LwM2mResponse> void send(final Client registrationInfo, final DownlinkRequest<T> request,
            final ResponseCallback<T> responseCallback, final ErrorCallback errorCallback) {
        throw new UnsupportedOperationException(
                "QueueMode doesn't support sending of messages with callbacks. Use a request ticket instead and register your response listeners");
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.leshan.server.request.LwM2mRequestSender#send(org.eclipse. leshan.server.client.Client,
     * java.lang.String, org.eclipse.leshan.core.request.DownlinkRequest)
     */
    @Override
    public <T extends LwM2mResponse> void send(final Client registrationInfo, final String requestTicket,
            final DownlinkRequest<T> request) {
        LOG.trace("send(requestTicket={})", requestTicket);
        // safe because DownlinkRequest does not use generic itself
        @SuppressWarnings("unchecked")
        final DownlinkRequest<LwM2mResponse> castedDownlinkRequest = (DownlinkRequest<LwM2mResponse>) request;
        LOG.debug("Sending request {} with queue mode", castedDownlinkRequest);

        final String endpoint = registrationInfo.getEndpoint();

        // Accept messages only when the client is already known to
        // ClientRegistry
        if (clientRegistry.get(endpoint) != null) {
            final QueuedRequest queuedRequest = queuedRequestFactory.newQueueRequestEntity(endpoint,
                    castedDownlinkRequest, requestTicket);
            messageStore.add(queuedRequest);
            // If Client is reachable and this is the first message, we send it
            // immediately.
            if (clientStatusTracker.startClientReceiving(endpoint)) {
                processingExecutor.execute(newRequestSendingTask(endpoint));
            }
        } else {
            LOG.warn("Ignoring a message received in Queue Mode for the unknown client [{}]", endpoint);
        }
    }

    @Override
    public void addResponseListener(final ResponseListener listener) {
        // Noop.
    }

    @Override
    public void removeResponseListener(final ResponseListener listener) {
        // Noop.
    }

    @Override
    public void stop() {
        clientRegistry.removeListener(queueModeClientRegistryListener);
        observationRegistry.removeListener(queueModeObservationRegistryListener);
        processingExecutor.shutdown();
        try {
            final boolean queueProcessingExecutorTerminated = processingExecutor.awaitTermination(5, TimeUnit.SECONDS);
            if (!(queueProcessingExecutorTerminated)) {
                LOG.debug("Could not stop all executors within timeout. processingExecutor stopped: {}, ",
                        queueProcessingExecutorTerminated);
            }
        } catch (final InterruptedException e) {
            LOG.debug("Interrupted while stopping. Abort stopping.");
            Thread.currentThread().interrupt();
        }
    }

    /**
     * creates a response listener for response and error responses from clients using queue mode.
     * 
     * @return instance of {@link ResponseListener}
     */
    public ResponseListener createResponseListener() {
        return new ResponseListener() {

            @Override
            public void onResponse(final String clientEndpoint, final String requestTicket,
                    final LwM2mResponse response) {
                final Client registrationInfo = clientRegistry.get(clientEndpoint);
                // process only if the client has used Queue mode.
                if (registrationInfo != null && isQueueMode(registrationInfo.getBindingMode())) {
                    LOG.debug("response received in Queue mode successfully: {}", requestTicket);
                    processResponse(clientEndpoint, requestTicket, response);
                }
            }

            @Override
            public void onError(final String clientEndpoint, final String requestTicket, final Exception exception) {
                final Client registrationInfo = clientRegistry.get(clientEndpoint);
                // process only if the client has used Queue mode.
                if (registrationInfo != null && isQueueMode(registrationInfo.getBindingMode())) {
                    LOG.debug("exception on sending the request: {}", requestTicket, exception);
                    if (exception instanceof TimeoutException) {
                        timeout(clientEndpoint);
                    } else {
                        processException(clientEndpoint, requestTicket, exception);
                    }
                }
            }
        };
    }

    private void processResponse(final String clientEndpoint, final String requestTicket,
            final LwM2mResponse response) {
        LOG.debug("Received Response -> {}", requestTicket);
        messageStore.deleteFirst(clientEndpoint);
        processingExecutor.execute(newRequestSendingTask(clientEndpoint));
    }

    private void processException(final String clientEndpoint, final String requestTicket, final Exception exception) {
        LOG.debug("Received error response {}", requestTicket);
        messageStore.deleteFirst(clientEndpoint);
        processingExecutor.execute(newRequestSendingTask(clientEndpoint));
    }

    private void timeout(final String clientEndpoint) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Client {} timed out", clientEndpoint);
        }
        clientStatusTracker.setClientUnreachable(clientEndpoint);
    }

    private RequestSendingTask newRequestSendingTask(final String endpoint) {
        return new RequestSendingTask(clientRegistry, delegateSender, clientStatusTracker, messageStore, endpoint);
    }

    private boolean isQueueMode(final BindingMode bindingMode) {
        return bindingMode.equals(BindingMode.UQ);
    }

    private final class QueueModeObservationRegistryListener implements ObservationRegistryListener {
        @Override
        public void newValue(final Observation observation, final LwM2mNode value) {
            final Client client = clientRegistry.findByRegistrationId(observation.getRegistrationId());
            if (isQueueMode(client.getBindingMode()) && clientStatusTracker.setClientReachable(client.getEndpoint())
                    && clientStatusTracker.startClientReceiving(client.getEndpoint())) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Notify from {}. Sending queued requests.", client.getEndpoint());
                }
                processingExecutor.execute(newRequestSendingTask(client.getEndpoint()));
            }
        }

        @Override
        public void cancelled(final Observation observation) {
            // not used here
        }

        @Override
        public void newObservation(final Observation observation) {
            // not used here
        }
    }

    private final class QueueModeClientRegistryListener implements ClientRegistryListener {
        @Override
        public void registered(final Client client) {
            // When client is in QueueMode
            if (isQueueMode(client.getBindingMode())) {
                clientStatusTracker.setClientReachable(client.getEndpoint());
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Client {} registered.", client.getEndpoint());
                }
            }
        }

        @Override
        public void updated(final ClientUpdate update, final Client clientUpdated) {
            // When client is in QueueMode and was previously in unreachable
            // state and when RECEIVING state could be set
            // i.e:- there is no other message currently being sent
            if (isQueueMode(clientUpdated.getBindingMode())
                    && clientStatusTracker.setClientReachable(clientUpdated.getEndpoint())
                    && clientStatusTracker.startClientReceiving(clientUpdated.getEndpoint())) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Client {} updated. Sending queued request.", clientUpdated.getEndpoint());
                }
                processingExecutor.execute(newRequestSendingTask(clientUpdated.getEndpoint()));
            }
        }

        @Override
        public void unregistered(final Client client) {
            if (isQueueMode(client.getBindingMode())) {
                clientStatusTracker.clearClientState(client.getEndpoint());
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Client {} de-registered. Removing all queued requests.", client.getEndpoint());
                }
                messageStore.removeAll(client.getEndpoint());
                // TODO: Also cancel any CoAP retries?
            }
        }
    }

    /**
     * Builder, which helps to create a QueuedRequestSender instance.
     */
    public static class Builder {

        private MessageStore messageStore;
        private QueuedRequestFactory queuedRequestFactory;
        private LwM2mRequestSender delegateSender;
        private ClientRegistry clientRegistry;
        private ObservationRegistry observationRegistry;

        public Builder setMessageStore(final MessageStore messageStore) {
            this.messageStore = messageStore;
            return this;
        }

        public Builder setQueuedRequestFactory(final QueuedRequestFactory queuedRequestFactory) {
            this.queuedRequestFactory = queuedRequestFactory;
            return this;
        }

        public Builder setRequestSender(final LwM2mRequestSender delegateSender) {
            this.delegateSender = delegateSender;
            return this;
        }

        public Builder setClientRegistry(final ClientRegistry clientRegistry) {
            this.clientRegistry = clientRegistry;
            return this;
        }

        public Builder setObservationRegistry(final ObservationRegistry observationRegistry) {
            this.observationRegistry = observationRegistry;
            return this;
        }

        public QueuedRequestSender build() {
            Validate.notNull(messageStore, "messageStore cannot be null");
            Validate.notNull(queuedRequestFactory, "queuedRequestFactory cannot be null");
            Validate.notNull(delegateSender, "delegateSender cannot be null");
            Validate.notNull(clientRegistry, "clientRegistry cannot be null");
            Validate.notNull(observationRegistry, "observationRegistry cannot be null");

            return new QueuedRequestSender(this);
        }
    }
}
