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
 *     Bosch Software Innovations GmbH 
 *                                - initial implementation
 *******************************************************************************/
package org.eclipse.leshan.server.impl;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.eclipse.leshan.core.request.BindingMode;
import org.eclipse.leshan.core.request.DownlinkRequest;
import org.eclipse.leshan.core.request.exception.TimeoutException;
import org.eclipse.leshan.core.response.ErrorCallback;
import org.eclipse.leshan.core.response.LwM2mResponse;
import org.eclipse.leshan.core.response.ResponseCallback;
import org.eclipse.leshan.server.Stoppable;
import org.eclipse.leshan.server.client.Client;
import org.eclipse.leshan.server.client.ClientRegistry;
import org.eclipse.leshan.server.queue.impl.QueuedRequestSender;
import org.eclipse.leshan.server.request.LwM2mRequestSender;
import org.eclipse.leshan.server.response.ResponseListener;
import org.eclipse.leshan.server.response.ResponseProcessingTask;
import org.eclipse.leshan.util.NamedThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Client binding mode aware request sender. i.e: provides a wrapper delegating the actual invocation to either
 * {@link QueuedRequestSender} or a default {@link LwM2mRequestSender} depending upon whether the client has connected
 * in Queue mode or not.
 */
public class LwM2mRequestSenderImpl implements LwM2mRequestSender, Stoppable {

    private static final Logger LOG = LoggerFactory.getLogger(LwM2mRequestSenderImpl.class);
    private final ExecutorService processingExecutor = Executors
            .newCachedThreadPool(new NamedThreadFactory("leshan-response-processingExecutor-%d"));
    private final Queue<ResponseListener> responseListeners = new ConcurrentLinkedQueue<>();

    private final LwM2mRequestSender defaultRequestSender;
    private final QueuedRequestSender queuedRequestSender;
    private final ResponseListener queueModeResponseListener;
    private final ResponseListener defaultResponseListener;
    private final ClientRegistry clientRegistry;

    /**
     * default constructor.
     * 
     * @param defaultRequestSender used to send messages to a LWM2M Client using a non-queue mode.
     * @param queuedRequestSender used to send messages to a LWM2M Client using a queue-mode.
     */
    public LwM2mRequestSenderImpl(final LwM2mRequestSender defaultRequestSender,
            final QueuedRequestSender queuedRequestSender, final ClientRegistry clientRegistry) {
        this.defaultRequestSender = defaultRequestSender;
        this.queuedRequestSender = queuedRequestSender;
        this.clientRegistry = clientRegistry;
        this.queueModeResponseListener = queuedRequestSender.createResponseListener();
        this.defaultResponseListener = createBindingAwareResponseListener();
        this.defaultRequestSender.addResponseListener(defaultResponseListener);
    }

    @SuppressWarnings("deprecation")
    @Override
    public <T extends LwM2mResponse> T send(final Client destination, final DownlinkRequest<T> request,
            final Long timeout) throws InterruptedException {
        return defaultRequestSender.send(destination, request, timeout);
    }

    @SuppressWarnings("deprecation")
    @Override
    public <T extends LwM2mResponse> void send(final Client destination, final DownlinkRequest<T> request,
            final ResponseCallback<T> responseCallback, final ErrorCallback errorCallback) {
        if (destination.getBindingMode().equals(BindingMode.UQ)) {
            queuedRequestSender.send(destination, request, responseCallback, errorCallback);
        } else {
            defaultRequestSender.send(destination, request, responseCallback, errorCallback);
        }
    }

    @Override
    public <T extends LwM2mResponse> void send(final Client registrationInfo, final String requestTicket,
            final DownlinkRequest<T> request) {
        if (registrationInfo.getBindingMode().equals(BindingMode.UQ)) {
            queuedRequestSender.send(registrationInfo, requestTicket, request);
        } else {
            defaultRequestSender.send(registrationInfo, requestTicket, request);
        }
    }

    @Override
    public void addResponseListener(final ResponseListener listener) {
        responseListeners.add(listener);
    }

    @Override
    public void removeResponseListener(final ResponseListener listener) {
        responseListeners.remove(listener);
    }

    @Override
    public void stop() {
        defaultRequestSender.removeResponseListener(this.defaultResponseListener);
        processingExecutor.shutdown();
    }

    private ResponseListener createBindingAwareResponseListener() {
        return new ResponseListener() {

            @Override
            public void onResponse(final String clientEndpoint, final String requestTicket,
                    final LwM2mResponse response) {
                LOG.debug("Response received for request: {}", requestTicket);
                final Client registrationInfo = clientRegistry.get(clientEndpoint);
                // process only when client is still known
                if (registrationInfo != null) {
                    // only if the client has used Queue mode notify its
                    // listener
                    if (registrationInfo.usesQueueMode()) {
                        LOG.debug("Notifying QueueModeResponseListener.onResponse for ticket: {}", requestTicket);
                        queueModeResponseListener.onResponse(clientEndpoint, requestTicket, response);
                    }
                    LOG.debug("Notifying All ResponseListeners.onResponse for ticket: {}", requestTicket);
                    processingExecutor.execute(
                            new ResponseProcessingTask(clientEndpoint, requestTicket, responseListeners, response));
                }
            }

            @Override
            public void onError(final String clientEndpoint, final String requestTicket, final Exception exception) {
                LOG.info("Exception on sending the request: {}", requestTicket, exception);
                final Client registrationInfo = clientRegistry.get(clientEndpoint);
                // process only when client is still known
                if (registrationInfo != null) {
                    // only if the client uses Queue mode notify its listener
                    if (registrationInfo.usesQueueMode()) {
                        LOG.debug("Notifying QueueModeResponseListener.onError for ticket: {}", requestTicket);
                        queueModeResponseListener.onError(clientEndpoint, requestTicket, exception);
                    }

                    // When client uses no queue mode and no timeout exception,
                    // notify all listeners.
                    // Otherwise queue mode will do retry sometime later when
                    // client connects again.
                    if (!(exception instanceof TimeoutException) && !(registrationInfo.usesQueueMode())) {
                        LOG.debug("Notifying All ResponseListeners.onError for ticket: {}", requestTicket);
                        processingExecutor.execute(new ResponseProcessingTask(clientEndpoint, requestTicket,
                                responseListeners, exception));
                    }
                }
            }
        };

    }
}
