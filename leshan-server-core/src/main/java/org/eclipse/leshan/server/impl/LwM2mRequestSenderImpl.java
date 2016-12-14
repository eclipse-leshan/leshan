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
 *     Bosch Software Innovations GmbH 
 *                                - initial implementation
 *******************************************************************************/
package org.eclipse.leshan.server.impl;

import org.eclipse.leshan.core.request.DownlinkRequest;
import org.eclipse.leshan.core.response.ErrorCallback;
import org.eclipse.leshan.core.response.LwM2mResponse;
import org.eclipse.leshan.core.response.ResponseCallback;
import org.eclipse.leshan.server.Stoppable;
import org.eclipse.leshan.server.client.Registration;
import org.eclipse.leshan.server.queue.impl.QueuedRequestSender;
import org.eclipse.leshan.server.request.LwM2mRequestSender;
import org.eclipse.leshan.server.response.ResponseListener;

/**
 * a wrapper delegating the actual invocation to either {@link QueuedRequestSender} or a default
 * {@link LwM2mRequestSender} depending upon whether the client has connected in Queue mode or not.
 */
public class LwM2mRequestSenderImpl implements LwM2mRequestSender, Stoppable {

    private final LwM2mRequestSender defaultRequestSender;
    private final LwM2mRequestSender queuedRequestSender;

    /**
     * default constructor.
     * 
     * @param defaultRequestSender used to send messages to a LWM2M Client using a non-queue mode.
     * @param queuedRequestSender used to send messages to a LWM2M Client using a queue-mode.
     */
    public LwM2mRequestSenderImpl(LwM2mRequestSender defaultRequestSender, LwM2mRequestSender queuedRequestSender) {
        this.defaultRequestSender = defaultRequestSender;
        this.queuedRequestSender = queuedRequestSender;
    }

    @SuppressWarnings("deprecation")
    @Override
    public <T extends LwM2mResponse> T send(Registration destination, DownlinkRequest<T> request, Long timeout)
            throws InterruptedException {
        return defaultRequestSender.send(destination, request, timeout);
    }

    @SuppressWarnings("deprecation")
    @Override
    public <T extends LwM2mResponse> void send(Registration destination, DownlinkRequest<T> request,
            ResponseCallback<T> responseCallback, ErrorCallback errorCallback) {
        if (destination.usesQueueMode()) {
            queuedRequestSender.send(destination, request, responseCallback, errorCallback);
        } else {
            defaultRequestSender.send(destination, request, responseCallback, errorCallback);
        }
    }

    @Override
    public <T extends LwM2mResponse> void send(Registration destination, String requestTicket, DownlinkRequest<T> request) {
        if (destination.usesQueueMode()) {
            queuedRequestSender.send(destination, requestTicket, request);
        } else {
            defaultRequestSender.send(destination, requestTicket, request);
        }
    }

    @Override
    public void addResponseListener(ResponseListener listener) {
        queuedRequestSender.addResponseListener(listener);
        defaultRequestSender.addResponseListener(listener);
    }

    @Override
    public void removeResponseListener(ResponseListener listener) {
        queuedRequestSender.removeResponseListener(listener);
        defaultRequestSender.removeResponseListener(listener);
    }

    @Override
    public void stop() {
        if (queuedRequestSender instanceof Stoppable) {
            ((Stoppable) queuedRequestSender).stop();
        }
        if (defaultRequestSender instanceof Stoppable) {
            ((Stoppable) defaultRequestSender).stop();
        }
    }

    @Override
    public void cancelPendingRequests(Registration registration) {
        // cancel on the both the senders are required for the scenario
        // where the LWM2M client could change binding modes.
        defaultRequestSender.cancelPendingRequests(registration);
        queuedRequestSender.cancelPendingRequests(registration);
    }
}
