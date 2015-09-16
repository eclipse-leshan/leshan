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

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.eclipse.leshan.server.client.Client;
import org.eclipse.leshan.server.queue.QueueReactor;
import org.eclipse.leshan.server.queue.QueueRequest;
import org.eclipse.leshan.server.queue.QueueTask;
import org.eclipse.leshan.server.queue.RequestQueue;
import org.eclipse.leshan.server.queue.RequestState;
import org.eclipse.leshan.server.queue.impl.ResponseContext;
import org.eclipse.leshan.server.request.LwM2mRequestSender;

/**
 * The queue processing task is intended to check the queue and to promote the requests in their next state, if
 * necessary. This queue processing task has two modes of operation: queue processing only (without sending of pending
 * requests) or full queue processing inclusively sending of the pending requests.
 */
public class QueueProcessingTask implements QueueTask {

    private boolean mayTriggerSend;
    private RequestQueue requestQueue;
    private Client client;
    private QueueReactor queueReactor;
    private long deferPeriod;
    private TimeUnit deferTimeUnit;
    private LwM2mRequestSender requestSender;
    private Long requestTimeout;
    private Map<Long, ResponseContext> responseContext;

    /**
     * Creates a new QueueProcessingTask with given parameters,
     *
     * @param requestSender request sender to use
     * @param responseContext map with response contexts for response mapping
     * @param requestQueue request queue to get requests from
     * @param client LWM2M client
     * @param queueReactor queue reactor for scheduling tasks
     * @param deferPeriod time amount to defer (reschedule the task to process later again)
     * @param deferTimeUnit time unit to defer
     * @param requestTimeout how long for request to time out
     * @param mayTriggerSend if the queue processing task may trigger sending or just tidying up the queue.
     */
    public QueueProcessingTask(final LwM2mRequestSender requestSender,
            final Map<Long, ResponseContext> responseContext, final RequestQueue requestQueue, final Client client,
            final QueueReactor queueReactor, final long deferPeriod, final TimeUnit deferTimeUnit,
            final Long requestTimeout, final boolean mayTriggerSend) {
        this.requestSender = requestSender;
        this.responseContext = responseContext;
        this.requestQueue = requestQueue;
        this.client = client;
        this.queueReactor = queueReactor;
        this.deferPeriod = deferPeriod;
        this.deferTimeUnit = deferTimeUnit;
        this.requestTimeout = requestTimeout;
        this.mayTriggerSend = mayTriggerSend;
    }

    @Override
    public void run() {

        Iterator<QueueRequest> iterator = requestQueue.getRequests(client.getEndpoint()).iterator();

        while (iterator.hasNext()) {

            // we have to find first ENQUEUED or DEFERRED request which is still valid
            boolean shouldProceedToNextRequest = false;

            QueueRequest queueRequest = iterator.next();

            switch (queueRequest.getRequestState()) {
            case ENQUEUED:
                if (!isEnqueuedRequestPromoted(queueRequest)) {
                    if (mayTriggerSend) {
                        queueReactor.addTask(new StateTransitionTask(queueRequest, requestQueue,
                                RequestState.PROCESSING));
                        queueReactor.addTask(new RequestSendingTask(requestSender, responseContext, client,
                                queueRequest, requestQueue, queueReactor, deferPeriod, deferTimeUnit, requestTimeout));
                    }
                }
                break;
            case DEFERRED:
                if (!isDeferredRequestPromoted(queueRequest)) {
                    if (mayTriggerSend) {
                        queueReactor.addTask(new StateTransitionTask(queueRequest, requestQueue,
                                RequestState.PROCESSING));
                        queueReactor.addTask(new RequestSendingTask(requestSender, responseContext, client,
                                queueRequest, requestQueue, queueReactor, deferPeriod, deferTimeUnit, requestTimeout));
                    }
                }
                break;
            case TTL_ELAPSED:
                tryPromoteTtlElapsedRequest(queueRequest);
                shouldProceedToNextRequest = true;
                break;

            case EXECUTED:
                tryPromoteExecutedRequest(queueRequest);
                shouldProceedToNextRequest = true;
                break;

            case PROCESSING:
                // some worker is sending right now, do nothing here
                break;

            default:
                throw new IllegalStateException("request is in unknown or invalid state "
                        + queueRequest.getRequestState());
            }
            // queue is still not empty and we are not know if we will be triggered from client again,
            // so postpone a task for checking the queue later.
            if (!shouldProceedToNextRequest) {
                if (!mayTriggerSend) {
                    queueReactor.addTask(new DeferredTask(deferPeriod, deferTimeUnit, new QueueProcessingTask(
                            requestSender, responseContext, requestQueue, client, queueReactor, deferPeriod,
                            deferTimeUnit, requestTimeout, false)));
                }
                break;
            }
        }
    }

    private void tryPromoteTtlElapsedRequest(QueueRequest queueRequest) {
        if (queueRequest.isKeepExpirationReached()) {
            queueReactor.addTask(new StateTransitionTask(queueRequest, requestQueue, RequestState.UNKNOWN));
        }
    }

    private void tryPromoteExecutedRequest(QueueRequest queueRequest) {
        if (queueRequest.isKeepExpirationReached()) {
            queueReactor.addTask(new StateTransitionTask(queueRequest, requestQueue, RequestState.UNKNOWN));
        }
    }

    private boolean isEnqueuedRequestPromoted(QueueRequest queueRequest) {
        if (queueRequest.isSendExpirationReached()) {
            if (queueRequest.isKeepExpirationReached()) {
                queueReactor.addTask(new StateTransitionTask(queueRequest, requestQueue, RequestState.UNKNOWN));
                return true;
            } else {
                queueReactor.addTask(new StateTransitionTask(queueRequest, requestQueue, RequestState.TTL_ELAPSED));
                return true;
            }
        }
        return false;
    }

    private boolean isDeferredRequestPromoted(QueueRequest queueRequest) {
        if (queueRequest.isSendExpirationReached()) {
            if (queueRequest.isKeepExpirationReached()) {
                queueReactor.addTask(new StateTransitionTask(queueRequest, requestQueue, RequestState.UNKNOWN));
                return true;
            } else {
                queueReactor.addTask(new StateTransitionTask(queueRequest, requestQueue, RequestState.TTL_ELAPSED));
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean wouldBlock() {
        return false;
    }

}
