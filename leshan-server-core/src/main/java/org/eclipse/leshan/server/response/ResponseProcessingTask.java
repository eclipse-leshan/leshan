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
package org.eclipse.leshan.server.response;

import java.util.Queue;

import org.eclipse.leshan.core.response.LwM2mResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A response processing task is responsible for calling back the registered listeners with response or error result.
 *
 */
public class ResponseProcessingTask implements Runnable {

    private final Logger LOG = LoggerFactory.getLogger(ResponseProcessingTask.class);

    private final Exception exception;
    private final boolean hasException;
    private final LwM2mResponse response;
    private final Queue<ResponseListener> responseListeners;
    private final String endpoint;
    private final String requestTicket;

    /**
     * Creates a new task for processing response on an ordinary response result.
     *
     * @param clientEndpoint
     * @param request request being processed
     * @param responseContext response context map for mapping response ID to the callback
     * @param response response to propagate
     */
    public ResponseProcessingTask(String clientEndpoint, String requestTicket,
            Queue<ResponseListener> responseListeners, LwM2mResponse response) {
        this.requestTicket = requestTicket;
        this.exception = null;
        this.hasException = false;
        this.responseListeners = responseListeners;
        this.response = response;
        this.endpoint = clientEndpoint;
    }

    /**
     * Creates a new task for processing response on exception.
     *
     * @param clientEndpoint
     * @param request request being processed
     * @param responseListeners response context map for mapping response ID to the callback
     * @param exception exception to propagate
     */
    public ResponseProcessingTask(String clientEndpoint, String requestTicket,
            Queue<ResponseListener> responseListeners, Exception exception) {
        this.requestTicket = requestTicket;
        this.exception = exception;
        this.responseListeners = responseListeners;
        this.hasException = true;
        this.response = null;
        this.endpoint = clientEndpoint;
    }

    @Override
    public void run() {
        try {
            executeAction();
        } catch (Exception e) {
            LOG.info("error while executing runnable", e);
            throw new RuntimeException(e);
        }
    }

    private void executeAction() {
        LOG.trace("response processing for {}", requestTicket);

        for (ResponseListener listener : responseListeners) {
            if (listener != null) {
                if (hasException) {
                    LOG.debug("invoke response listener for requestTicket {} with exception {}", requestTicket,
                            exception);
                    listener.onError(endpoint, requestTicket, exception);
                } else {
                    LOG.trace("invoke response listener for requestTicket {} with response {}", requestTicket,
                            response);
                    listener.onResponse(endpoint, requestTicket, response);
                }
            }
        }
    }
}
