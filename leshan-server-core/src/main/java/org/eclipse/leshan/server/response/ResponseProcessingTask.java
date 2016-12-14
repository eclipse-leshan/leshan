/*******************************************************************************
 * Copyright (c) 2016 Bosch Software Innovations GmbH and others.
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

import java.util.Collection;

import org.eclipse.leshan.core.response.LwM2mResponse;
import org.eclipse.leshan.server.client.Registration;
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
    private final Collection<ResponseListener> responseListeners;
    private final Registration registration;
    private final String requestTicket;

    /**
     * Creates a new task for processing response on an ordinary response result.
     *
     * @param registration instance of {@link Registration}
     * @param requestTicket unique ticket correlating the response to its request.
     * @param request request being processed
     * @param responseListeners listeners which are notified for response.
     * @param response response to propagate
     */
    public ResponseProcessingTask(Registration registration, String requestTicket, Collection<ResponseListener> responseListeners,
            LwM2mResponse response) {
        this.requestTicket = requestTicket;
        this.exception = null;
        this.hasException = false;
        this.responseListeners = responseListeners;
        this.response = response;
        this.registration = registration;
    }

    /**
     * Creates a new task for processing response on exception.
     *
     * @param registration instance of {@link Registration}
     * @param requestTicket unique ticket correlating the response to its request.
     * @param responseListeners listeners which are notified about the exception.
     * @param exception exception to propagate
     */
    public ResponseProcessingTask(Registration registration, String requestTicket, Collection<ResponseListener> responseListeners,
            Exception exception) {
        this.requestTicket = requestTicket;
        this.exception = exception;
        this.responseListeners = responseListeners;
        this.hasException = true;
        this.response = null;
        this.registration = registration;
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
                    LOG.debug("invoke response listener for requestTicket {} with exception", requestTicket, exception);
                    listener.onError(registration, requestTicket, exception);
                } else {
                    LOG.trace("invoke response listener for requestTicket {} with response {}", requestTicket,
                            response);
                    listener.onResponse(registration, requestTicket, response);
                }
            }
        }
    }
}
