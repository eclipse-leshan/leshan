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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.eclipse.leshan.core.node.LwM2mNode;
import org.eclipse.leshan.core.request.DownlinkRequest;
import org.eclipse.leshan.core.response.ErrorCallback;
import org.eclipse.leshan.core.response.LwM2mResponse;
import org.eclipse.leshan.core.response.ResponseCallback;
import org.eclipse.leshan.server.client.Client;
import org.eclipse.leshan.server.client.ClientRegistry;
import org.eclipse.leshan.server.client.ClientRegistryListener;
import org.eclipse.leshan.server.observation.Observation;
import org.eclipse.leshan.server.observation.ObservationRegistry;
import org.eclipse.leshan.server.observation.ObservationRegistryListener;
import org.eclipse.leshan.server.queue.QueueReactor;
import org.eclipse.leshan.server.queue.QueueRequest;
import org.eclipse.leshan.server.queue.QueueRequestFactory;
import org.eclipse.leshan.server.queue.QueueRequestSender;
import org.eclipse.leshan.server.queue.RequestQueue;
import org.eclipse.leshan.server.queue.RequestState;
import org.eclipse.leshan.server.queue.reactor.QueueProcessingTask;
import org.eclipse.leshan.server.queue.reactor.StateTransitionTask;
import org.eclipse.leshan.server.request.LwM2mRequestSender;
import org.eclipse.leshan.util.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This sender is a special implementation of a {@link QueueRequestSender}, which is aware of a LwM2M client's binding
 * mode. If the client supports "Q" (queue) mode, then the request is processed using the internal queue, i.e. the
 * request is enqueued first and then sent if a client is back online. If the client does not support the queue mode in
 * its binding, then the delegate sender is call in order to send the request.
 *
 * <b>Please note:</b> the synchronous {@link #send(Client, DownlinkRequest, Long)} operation is not supported with this
 * sender.
 */
public class QueueRequestSenderImpl implements QueueRequestSender {

    private static final Logger LOG = LoggerFactory.getLogger(QueueRequestSenderImpl.class);
    private final RequestQueue requestQueue;
    private final LwM2mRequestSender delegateSender;
    private final QueueRequestFactory queuedRequestFactory;
    private final QueueReactor queueReactor;
    private final long requestTimeout;

    private long sendExpirationInterval = 86400000000L;  // default send interval of a day in nanos
    private long keepExpirationInterval = 172800000000L; // default keep interval of two days in nanos

    private Lock intervalLock = new ReentrantLock();

    private Map<Long, ResponseContext> responseContext = new ConcurrentHashMap<>();

    private AtomicLong responseIdCounter = new AtomicLong();
    private long queueCleaningPeriod;
    private TimeUnit queueCleaningTimeUnit;

    /**
     * Creates a new QueueRequestSender using given parameters.
     *
     * @param queueReactor queue reactor, which is used for processing and scheduling of queue requests
     * @param requestQueue a request queue, used for storing the queue requests
     * @param delegateSender delegate {@link LwM2mRequestSender}, which is used for sending the requests
     * @param queuedRequestFactory queue request factory is used for creating the requests
     * @param clientRegistry used for client lookup, to continue processing on client updates
     * @param observationRegistry used for observation lookup, to continue processing on notifications
     * @param queueCleaningPeriod period for rescheduling queue's self managing tasks
     * @param queueCleaningTimeUnit time unit for rescheduling queue's self managing tasks
     * @param requestTimeout sender's request timeout
     */
    public QueueRequestSenderImpl(final QueueReactor queueReactor, final RequestQueue requestQueue,
            final LwM2mRequestSender delegateSender, final QueueRequestFactory queuedRequestFactory,
            final ClientRegistry clientRegistry, final ObservationRegistry observationRegistry,
            final long queueCleaningPeriod, final TimeUnit queueCleaningTimeUnit, final Long requestTimeout) {
        Validate.notNull(queueReactor, "queueReactor cannot be null");
        Validate.notNull(requestQueue, "requestQueue cannot be null");
        Validate.notNull(delegateSender, "delegateSender cannot be null");
        Validate.notNull(queuedRequestFactory, "queuedRequestFactory cannot be null");
        Validate.notNull(clientRegistry, "clientRegistry cannot be null");
        Validate.notNull(observationRegistry, "observationRegistry cannot be null");
        Validate.isTrue(queueCleaningPeriod > 0, "queueCleaningPeriod may not be less or equal zero");
        Validate.notNull(requestTimeout, "requestTimeout cannot be null");

        this.queueReactor = queueReactor;
        this.requestQueue = requestQueue;
        this.delegateSender = delegateSender;
        this.queuedRequestFactory = queuedRequestFactory;
        this.queueCleaningPeriod = queueCleaningPeriod;
        this.queueCleaningTimeUnit = queueCleaningTimeUnit;
        this.requestTimeout = requestTimeout;

        clientRegistry.addListener(new ClientRegistryListener() {
            @Override
            public void registered(Client client) {
                queueReactor.addTask(new QueueProcessingTask(delegateSender, responseContext, requestQueue, client,
                        queueReactor, queueCleaningPeriod, queueCleaningTimeUnit, requestTimeout, true));
            }

            @Override
            public void updated(Client clientUpdated) {
                queueReactor.addTask(new QueueProcessingTask(delegateSender, responseContext, requestQueue,
                        clientUpdated, queueReactor, queueCleaningPeriod, queueCleaningTimeUnit, requestTimeout, true));
            }

            @Override
            public void unregistered(Client client) {
            }
        });
        observationRegistry.addListener(new ObservationRegistryListener() {
            @Override
            public void newValue(Observation observation, LwM2mNode value) {
                queueReactor.addTask(new QueueProcessingTask(delegateSender, responseContext, requestQueue, observation
                        .getClient(), queueReactor, queueCleaningPeriod, queueCleaningTimeUnit, requestTimeout, true));
            }

            @Override
            public void cancelled(Observation observation) {
                // not used here
            }

            @Override
            public void newObservation(Observation observation) {
                // not used here
            }
        });
    }

    @Override
    public void setSendExpirationInterval(long sendExpirationInterval, TimeUnit sendExpirationIntervalTimeUnit) {
        if (sendExpirationInterval <= 0) {
            throw new IllegalArgumentException("invalid send expiration interval");
        }
        intervalLock.lock();
        try {
            this.sendExpirationInterval = sendExpirationIntervalTimeUnit.toNanos(sendExpirationInterval);
        } finally {
            intervalLock.unlock();
        }
    }

    @Override
    public void setKeepExpirationInterval(long keepExpirationInterval, TimeUnit keepExpirationIntervalTimeUnit) {
        if (sendExpirationInterval <= 0) {
            throw new IllegalArgumentException("invalid keep expiration interval");
        }
        intervalLock.lock();
        try {
            this.keepExpirationInterval = keepExpirationIntervalTimeUnit.toNanos(keepExpirationInterval);
        } finally {
            intervalLock.unlock();
        }
    }

    @Override
    public <T extends LwM2mResponse> T send(Client destination, DownlinkRequest<T> request, Long requestTimeout) {
        throw new UnsupportedOperationException("synchronous send() with the QueueRequestSender is not supported.");
    }

    @Override
    public <T extends LwM2mResponse> void send(Client destination, DownlinkRequest<T> request,
            final ResponseCallback<T> responseCallback, final ErrorCallback errorCallback) {

        if (destination.getBindingMode().isQueueMode()) {
            final Long responseId = responseIdCounter.incrementAndGet();
            LOG.debug("created response ID {} for request {}", responseId, request);

            long sendExpiration = 0;
            long keepExpiration = 0;

            intervalLock.lock();
            try {
                sendExpiration = sendExpirationInterval;
                keepExpiration = keepExpirationInterval;
            } finally {
                intervalLock.unlock();
            }

            QueueRequest queueRequest = queuedRequestFactory.createQueuedRequest(destination, request,
                    sendExpiration, keepExpiration, responseId);

            responseContext.put(responseId, new ResponseContext() {
                @Override
                public ResponseCallback<? extends LwM2mResponse> getResponseCallback() {
                    return responseCallback;
                }

                @Override
                public ErrorCallback getErrorCallback() {
                    return errorCallback;
                }
            });
            queueReactor.addTask(new StateTransitionTask(queueRequest, requestQueue, RequestState.ENQUEUED));
            queueReactor.addTask(new QueueProcessingTask(delegateSender, responseContext, requestQueue, destination, queueReactor,
                    queueCleaningPeriod, queueCleaningTimeUnit, requestTimeout, true));
        } else {
            // send directly (client is not in queued mode)
            delegateSender.send(destination, request, responseCallback, errorCallback);
        }
    }
}
