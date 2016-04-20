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
 *     Alexander Ellwein, Daniel Maier (Bosch Software Innovations GmbH)
 *                                - initial API and implementation
 *******************************************************************************/
package org.eclipse.leshan.server.queue.impl;

import org.eclipse.leshan.core.response.ErrorCallback;
import org.eclipse.leshan.core.response.LwM2mResponse;
import org.eclipse.leshan.core.response.ResponseCallback;
import org.eclipse.leshan.server.queue.QueuedRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * A response processing task is responsible for calling back the registered listeners with response or error result.
 *
 */
class ResponseProcessingTask implements Runnable {

    private final Logger LOG = LoggerFactory.getLogger(ResponseProcessingTask.class);

    private final Exception exception;
    private final boolean hasException;
    private final LwM2mResponse response;
    private final Map<Long, ResponseContext> responseContext;

    private final QueuedRequest request;

    /**
     * Creates a new task for processing response on exception.
     *
     * @param request request being processed
     * @param responseContext response context map for mapping response ID to the callback
     * @param exception exception to propagate
     */
    ResponseProcessingTask(QueuedRequest request, Map<Long, ResponseContext> responseContext,
                           Exception exception) {
        this.request = request;
        this.exception = exception;
        this.responseContext = responseContext;
        this.hasException = true;
        this.response = null;
    }

    /**
     * Creates a new task for processing response on an ordinary response result.
     *
     * @param request request being processed
     * @param responseContext response context map for mapping response ID to the callback
     * @param response response to propagate
     */
    ResponseProcessingTask(QueuedRequest request, Map<Long, ResponseContext> responseContext,
                           LwM2mResponse response) {
        this.request = request;
        this.exception = null;
        this.hasException = false;
        this.responseContext = responseContext;
        this.response = response;
    }

    @Override
    public void run() {
        long requestId = request.getRequestId();
        LOG.trace("response processing for {}, requestId: {}", request, requestId);

        ResponseContext context = responseContext.get(requestId);

        if (context != null) {
            try {
                if (hasException) {
                    ErrorCallback errorCallback = context.getErrorCallback();
                    if (errorCallback != null) {
                        LOG.debug("invoke callback for requestId {} with exception {}", requestId, exception);
                        errorCallback.onError(exception);
                    }
                } else {
                    ResponseCallback<LwM2mResponse> responseCallback = context.getResponseCallback();
                    if (responseCallback != null) {
                        LOG.trace("invoke callback for requestId {} with response {}", requestId, response);
                        responseCallback.onResponse(response);
                    }
                }
            } finally {
                responseContext.remove(requestId);
            }
        }
    }
}
