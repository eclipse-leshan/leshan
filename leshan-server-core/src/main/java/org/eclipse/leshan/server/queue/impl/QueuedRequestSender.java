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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.eclipse.leshan.core.node.LwM2mNode;
import org.eclipse.leshan.core.observation.Observation;
import org.eclipse.leshan.core.request.BindingMode;
import org.eclipse.leshan.core.request.DownlinkRequest;
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
import org.eclipse.leshan.util.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This sender is a special implementation of a {@link LwM2mRequestSender}, which is aware of a LwM2M client's binding
 * mode. If the client supports "Q" (queue) mode, then the request is processed using the internal queue, i.e. the
 * request is enqueued first and then sent if a client is back online. If the client does not support the queue mode in
 * its binding, then the delegate sender is called in order to send the request.
 */
public class QueuedRequestSender implements LwM2mRequestSender, Stoppable {
    private static final Logger LOG = LoggerFactory.getLogger(QueuedRequestSender.class);
    private final LwM2mRequestSender delegateSender;
    private final ExecutorService processingExecutor = new ExceptionAwareExecutorService(
            Executors.newCachedThreadPool(new NamedThreadFactory("leshan-qmode-processingExecutor-%d")));
    private final Map<Long, ResponseContext> responseContextHolder = new ConcurrentHashMap<>();
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
    private QueuedRequestSender(Builder builder) {
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

    @Override
    public <T extends LwM2mResponse> T send(Client destination, DownlinkRequest<T> request, Long requestTimeout)
            throws InterruptedException {
        // Send without callbacks are intended to be synchronous calls and will not be queued.
        // using the delegate sender to send it immediately.
        return delegateSender.send(destination, request, requestTimeout);
    }

    @Override
    public <T extends LwM2mResponse> void send(Client destination, DownlinkRequest<T> request,
            final ResponseCallback<T> responseCallback, final ErrorCallback errorCallback) {

        LOG.trace("send()");
        // safe because DownlinkRequest does not use generics itself
        @SuppressWarnings("unchecked")
        final DownlinkRequest<LwM2mResponse> castedDownlinkRequest = (DownlinkRequest<LwM2mResponse>) request;
        if (isQueueMode(destination.getBindingMode())) {
            LOG.debug("Sending request {} with queue mode", castedDownlinkRequest);

            final String endpoint = destination.getEndpoint();

            // Accept messages only when the client is already known to ClientRegistry
            if (clientRegistry.get(endpoint) != null) {
                final QueuedRequest queuedRequest = queuedRequestFactory.newQueueRequestEntity(endpoint,
                        castedDownlinkRequest);
                // prepare error and response context
                responseContextHolder.put(queuedRequest.getRequestId(), new ResponseContext() {
                    @Override
                    public ResponseCallback<LwM2mResponse> getResponseCallback() {
                        // we rely that we get called with correct response
                        @SuppressWarnings("unchecked")
                        ResponseCallback<LwM2mResponse> castedResponseCallback = (ResponseCallback<LwM2mResponse>) responseCallback;
                        return castedResponseCallback;
                    }

                    @Override
                    public ErrorCallback getErrorCallback() {
                        return errorCallback;
                    }
                });

                messageStore.add(queuedRequest);
                // If Client is reachable and this is the first message, we send it immediately.
                if (clientStatusTracker.startClientReceiving(endpoint)) {
                    processingExecutor.execute(newRequestSendingTask(endpoint));
                }
            } else {
                LOG.warn("Ignoring a message received in Queue Mode for the unknown client [{}]", endpoint);
            }
        } else {
            // send directly (client is not in queued mode)
            delegateSender.send(destination, request, responseCallback, errorCallback);
        }
    }

    @Override
    public void stop() {
        clientRegistry.removeListener(queueModeClientRegistryListener);
        observationRegistry.removeListener(queueModeObservationRegistryListener);
        processingExecutor.shutdown();
        try {
            boolean queueProcessingExecutorTerminated = processingExecutor.awaitTermination(5, TimeUnit.SECONDS);
            if (!(queueProcessingExecutorTerminated)) {
                LOG.debug(
                        "Could not stop all executors within timeout. processingExecutor stopped: {}, ",
                        queueProcessingExecutorTerminated);
            }
        } catch (InterruptedException e) {
            LOG.debug("Interrupted while stopping. Abort stopping.");
            Thread.currentThread().interrupt();
        }
    }

    private RequestSendingTask newRequestSendingTask(final String endpoint) {
        return new RequestSendingTask(clientRegistry, delegateSender, processingExecutor,
                clientStatusTracker, messageStore, endpoint, responseContextHolder);
    }

    private boolean isQueueMode(BindingMode bindingMode) {
        return bindingMode.equals(BindingMode.UQ);
    }

    private final class QueueModeObservationRegistryListener implements ObservationRegistryListener {
        @Override
        public void newValue(Observation observation, LwM2mNode value) {
            Client client = clientRegistry.findByRegistrationId(observation.getRegistrationId());
            if (isQueueMode(client.getBindingMode())
                    && clientStatusTracker.setClientReachable(client.getEndpoint())
                    && clientStatusTracker.startClientReceiving(client.getEndpoint())) {
                LOG.debug("Notify from {}. Sending queued requests.", client.getEndpoint());
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

    private final class QueueModeClientRegistryListener implements ClientRegistryListener {
        @Override
        public void registered(final Client client) {
            // When client is in QueueMode
            if (isQueueMode(client.getBindingMode())) {
                clientStatusTracker.setClientReachable(client.getEndpoint());
                LOG.debug("Client {} registered.", client.getEndpoint());
            }
        }

        @Override
        public void updated(ClientUpdate update, Client clientUpdated) {
            // When client is in QueueMode and was previously in unreachable state and when
            // RECEIVING state could be set:i.e:- there is no other message currently being sent)
            if (isQueueMode(clientUpdated.getBindingMode())
                    && clientStatusTracker.setClientReachable(clientUpdated.getEndpoint())
                    && clientStatusTracker.startClientReceiving(clientUpdated.getEndpoint())) {
                LOG.debug("Client {} updated. Sending queued request.", clientUpdated.getEndpoint());
                processingExecutor.execute(newRequestSendingTask(clientUpdated.getEndpoint()));
            }
        }

        @Override
        public void unregistered(final Client client) {
            if (isQueueMode(client.getBindingMode())) {
                clientStatusTracker.clearClientState(client.getEndpoint());
                LOG.debug("Client {} de-registered. Removing all queued requests.", client.getEndpoint());
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
        private int responseCallbackWorkers;

        public Builder setMessageStore(MessageStore messageStore) {
            this.messageStore = messageStore;
            return this;
        }

        public Builder setQueuedRequestFactory(QueuedRequestFactory queuedRequestFactory) {
            this.queuedRequestFactory = queuedRequestFactory;
            return this;
        }

        public Builder setRequestSender(LwM2mRequestSender delegateSender) {
            this.delegateSender = delegateSender;
            return this;
        }

        public Builder setClientRegistry(ClientRegistry clientRegistry) {
            this.clientRegistry = clientRegistry;
            return this;
        }

        public Builder setObservationRegistry(ObservationRegistry observationRegistry) {
            this.observationRegistry = observationRegistry;
            return this;
        }

        public Builder setResponseCallbackWorkers(int responseCallbackWorkers) {
            this.responseCallbackWorkers = responseCallbackWorkers;
            return this;
        }

        public QueuedRequestSender build() {
            Validate.notNull(messageStore, "messageStore cannot be null");
            Validate.notNull(queuedRequestFactory, "queuedRequestFactory cannot be null");
            Validate.notNull(delegateSender, "delegateSender cannot be null");
            Validate.notNull(clientRegistry, "clientRegistry cannot be null");
            Validate.notNull(observationRegistry, "observationRegistry cannot be null");
            Validate.isTrue(responseCallbackWorkers > 0, "responseCallbackWorkers must be greater than zero");

            return new QueuedRequestSender(this);
        }
    }
}
