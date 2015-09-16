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

import org.eclipse.leshan.core.response.ErrorCallback;
import org.eclipse.leshan.core.response.LwM2mResponse;
import org.eclipse.leshan.core.response.ResponseCallback;
import org.eclipse.leshan.server.queue.QueueRequest;
import org.eclipse.leshan.server.queue.QueueRequestSender;
import org.eclipse.leshan.server.queue.QueueTask;
import org.eclipse.leshan.server.queue.impl.ResponseContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A response processing task is responsible for calling back the registered listeners with response or error result.
 *
 * @see QueueRequestSender
 */
public class ResponseProcessingTask implements QueueTask {

    private final Logger LOG = LoggerFactory.getLogger(ResponseProcessingTask.class);

    private Exception exception;
    private boolean hasException;
    private LwM2mResponse response;
    private Map<Long, ResponseContext> responseContext;
    private QueueRequest queueRequest;

    /**
     * Creates a new task for processing response on exception.
     *
     * @param queueRequest request being processed
     * @param responseContext response context map for mapping response ID to the callback
     * @param exception exception to propagate
     */
    public ResponseProcessingTask(QueueRequest queueRequest, Map<Long, ResponseContext> responseContext,
            Exception exception) {
        this.queueRequest = queueRequest;
        this.exception = exception;
        this.hasException = true;
    }

    /**
     * Creates a new task for processing response on an ordinary response result.
     *
     * @param queueRequest request being processed
     * @param responseContext response context map for mapping response ID to the callback
     * @param response response to propagate
     */
    public <T> ResponseProcessingTask(QueueRequest queueRequest, Map<Long, ResponseContext> responseContext,
            LwM2mResponse response) {
        this.queueRequest = queueRequest;
        this.hasException = false;
        this.responseContext = responseContext;
        this.response = response;
    }

    @Override
    public void run() {
        Long responseId = queueRequest.getResponseId();
        LOG.trace("response processing for {}, responseId: {}", queueRequest, responseId);

        ResponseContext context = responseContext.get(responseId);

        if (context != null) {
            try {
                if (hasException) {
                    ErrorCallback errorCallback = context.getErrorCallback();
                    if (errorCallback != null) {
                        LOG.debug("calling response processor for response ID {} (on exception)", responseId);
                        errorCallback.onError(exception);
                    }
                } else {
                    ResponseCallback<LwM2mResponse> responseCallback = (ResponseCallback<LwM2mResponse>) context
                            .getResponseCallback();
                    if (responseCallback != null) {
                        LOG.trace("calling response processor for response ID {} (on response)", responseId);
                        responseCallback.onResponse(response);
                    }
                }
            } finally {
                responseContext.remove(responseId);
            }
        }
    }

    @Override
    public boolean wouldBlock() {
        return true;
    }

}
