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
package org.eclipse.leshan.server.queue.reactor;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.eclipse.leshan.core.response.LwM2mResponse;
import org.eclipse.leshan.server.client.Client;
import org.eclipse.leshan.server.queue.QueueReactor;
import org.eclipse.leshan.server.queue.QueueRequest;
import org.eclipse.leshan.server.queue.QueueTask;
import org.eclipse.leshan.server.queue.RequestQueue;
import org.eclipse.leshan.server.queue.RequestState;
import org.eclipse.leshan.server.queue.impl.ResponseContext;
import org.eclipse.leshan.server.request.LwM2mRequestSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This blocking queue task is intended for the actual sending of an outstanding DownlinkRequest. The request timeout
 * exception is handled separately here, because it leads the request to be DEFERRED for later execution.
 */
public class RequestSendingTask implements QueueTask {

    private static final Logger LOG = LoggerFactory.getLogger(RequestSendingTask.class);

    private LwM2mRequestSender requestSender;
    private Client client;
    private QueueRequest queueRequest;
    private QueueReactor queueReactor;
    private Long requestTimeout;
    private RequestQueue requestQueue;
    private long queueCleaningPeriod;
    private TimeUnit queueCleaningTimeUnit;
    private Map<Long, ResponseContext> responseContext;

    /**
     * Creates a new sending task with given parameters.
     *
     * @param requestSender
     * @param responseContext
     * @param client
     * @param queueRequest
     * @param requestQueue
     * @param queueReactor
     * @param queueCleaningPeriod
     * @param queueCleaningTimeUnit
     * @param requestTimeout
     */
    public RequestSendingTask(LwM2mRequestSender requestSender, Map<Long, ResponseContext> responseContext,
            Client client, QueueRequest queueRequest, RequestQueue requestQueue, QueueReactor queueReactor,
            long queueCleaningPeriod, TimeUnit queueCleaningTimeUnit, Long requestTimeout) {

        this.requestSender = requestSender;
        this.responseContext = responseContext;
        this.client = client;
        this.queueRequest = queueRequest;
        this.requestQueue = requestQueue;
        this.queueReactor = queueReactor;
        this.queueCleaningPeriod = queueCleaningPeriod;
        this.queueCleaningTimeUnit = queueCleaningTimeUnit;
        this.requestTimeout = requestTimeout;
    }

    @Override
    public void run() {

        try {
            LOG.debug("sending request: {}", queueRequest.getDownlinkRequest());

            LwM2mResponse response = requestSender.send(client, queueRequest.getDownlinkRequest(), requestTimeout);

            if (response == null) {
                // timeout
                LOG.debug("request timed out: {}", queueRequest.getDownlinkRequest());
                queueReactor.addTask(new StateTransitionTask(queueRequest, requestQueue, RequestState.DEFERRED));
                queueReactor.addTask(new QueueProcessingTask(requestSender, responseContext, requestQueue, client,
                        queueReactor, queueCleaningPeriod, queueCleaningTimeUnit, requestTimeout, false));
            } else {
                LOG.debug("request is sent successfully: {}", queueRequest.getDownlinkRequest());

                queueReactor.addTask(new StateTransitionTask(queueRequest, requestQueue, RequestState.EXECUTED));
                queueReactor.addTask(new ResponseProcessingTask(queueRequest, responseContext, response));
                queueReactor.addTask(new QueueProcessingTask(requestSender, responseContext, requestQueue, client,
                        queueReactor, queueCleaningPeriod, queueCleaningTimeUnit, requestTimeout, true));
            }

        } catch (Exception e) {
            LOG.debug("exception on sending the request: {}", queueRequest.getDownlinkRequest());

            queueReactor.addTask(new StateTransitionTask(queueRequest, requestQueue, RequestState.EXECUTED));
            queueReactor.addTask(new ResponseProcessingTask(queueRequest, responseContext, e));
            queueReactor.addTask(new QueueProcessingTask(requestSender, responseContext, requestQueue, client,
                    queueReactor, queueCleaningPeriod, queueCleaningTimeUnit, requestTimeout, true));
        }
    }

    @Override
    public boolean wouldBlock() {
        return true;
    }

}
