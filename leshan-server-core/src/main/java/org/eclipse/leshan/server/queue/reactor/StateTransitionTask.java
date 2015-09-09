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

import org.eclipse.leshan.server.queue.QueueRequest;
import org.eclipse.leshan.server.queue.QueueTask;
import org.eclipse.leshan.server.queue.RequestQueue;
import org.eclipse.leshan.server.queue.RequestState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * State transition task used for synchronization of transition operations for all queue requests.
 */
public class StateTransitionTask implements QueueTask {
    private final static Logger LOG = LoggerFactory.getLogger(StateTransitionTask.class);

    private RequestState newState;
    private QueueRequest queueRequest;
    private RequestQueue requestQueue;

    /**
     * Creates a new state transition task to apply the given new state to the given request.
     *
     * @param queueRequest queue request to transform
     * @param requestQueue request queue
     * @param newState new state to apply
     */
    public StateTransitionTask(QueueRequest queueRequest, RequestQueue requestQueue, RequestState newState) {
        this.queueRequest = queueRequest;
        this.requestQueue = requestQueue;
        this.newState = newState;
    }

    @Override
    public boolean wouldBlock() {
        return false;
    }

    @Override
    public void run() {

        switch (newState) {
        case ENQUEUED:
            LOG.debug("{} -> ENQUEUED", queueRequest.getDownlinkRequest());
            requestQueue.enqueueRequest(queueRequest);
            break;
        case DEFERRED:
            LOG.debug("{} -> DEFERRED", queueRequest.getDownlinkRequest());
            requestQueue.deferRequest(queueRequest);
            break;
        case PROCESSING:
            LOG.debug("{} -> PROCESSING", queueRequest.getDownlinkRequest());
            requestQueue.processingRequest(queueRequest);
            break;
        case TTL_ELAPSED:
            LOG.debug("{} -> TTL_ELAPSED", queueRequest.getDownlinkRequest());
            requestQueue.ttlElapsedRequest(queueRequest);
            break;
        case EXECUTED:
            LOG.debug("{} -> EXECUTED", queueRequest.getDownlinkRequest());
            requestQueue.executedRequest(queueRequest);
            break;
        case UNKNOWN:
            LOG.debug("{} -> UNKNOWN (unqueue)", queueRequest.getDownlinkRequest());
            requestQueue.unqueueRequest(queueRequest);
            break;
        default:
            throw new IllegalStateException("attempted a transition to an invalid state: " + newState);
        }
    }
}
