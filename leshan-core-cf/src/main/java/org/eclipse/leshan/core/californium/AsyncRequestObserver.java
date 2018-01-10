/*******************************************************************************
 * Copyright (c) 2016 Sierra Wireless and others.
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
 *     Sierra Wireless - initial API and implementation
 *     Achim Kraus (Bosch Software Innovations GmbH) - redirect onSendError
 *                                                     to error callback.
 *     Simon Bernard                                 - use specific exception for onSendError                                                 
 *******************************************************************************/
package org.eclipse.leshan.core.californium;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.eclipse.californium.core.coap.Request;
import org.eclipse.californium.core.coap.Response;
import org.eclipse.leshan.core.request.exception.RequestCanceledException;
import org.eclipse.leshan.core.request.exception.RequestRejectedException;
import org.eclipse.leshan.core.request.exception.SendFailedException;
import org.eclipse.leshan.core.response.ErrorCallback;
import org.eclipse.leshan.core.response.LwM2mResponse;
import org.eclipse.leshan.core.response.ResponseCallback;
import org.eclipse.leshan.util.NamedThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AsyncRequestObserver<T extends LwM2mResponse> extends AbstractRequestObserver<T> {

    private static final Logger LOG = LoggerFactory.getLogger(AsyncRequestObserver.class);
    private static volatile ScheduledExecutorService executor;

    private final ResponseCallback<T> responseCallback;
    private final ErrorCallback errorCallback;
    private final long timeoutInMs;
    private ScheduledFuture<?> cleaningTask;
    private boolean cancelled = false;

    private final AtomicBoolean responseTimedOut = new AtomicBoolean(false);

    public AsyncRequestObserver(Request coapRequest, ResponseCallback<T> responseCallback, ErrorCallback errorCallback,
            long timeoutInMs) {
        super(coapRequest);
        this.responseCallback = responseCallback;
        this.errorCallback = errorCallback;
        this.timeoutInMs = timeoutInMs;
    }

    @Override
    public void onResponse(Response coapResponse) {
        LOG.debug("Received coap response: {}", coapResponse);
        try {
            cleaningTask.cancel(false);
            T lwM2mResponseT = buildResponse(coapResponse);
            if (lwM2mResponseT != null) {
                responseCallback.onResponse(lwM2mResponseT);
            }
        } catch (Exception e) {
            errorCallback.onError(e);
        } finally {
            coapRequest.removeMessageObserver(this);
        }
    }

    @Override
    public void onReadyToSend() {
        scheduleCleaningTask();
    }

    @Override
    public void onTimeout() {
        cancelCleaningTask();
        errorCallback.onError(new org.eclipse.leshan.core.request.exception.TimeoutException("Request %s timed out",
                coapRequest.getURI()));
    }

    @Override
    public void onCancel() {
        cancelCleaningTask();
        if (responseTimedOut.get())
            errorCallback.onError(new org.eclipse.leshan.core.request.exception.TimeoutException("Request %s timed out",
                    coapRequest.getURI()));
        else
            errorCallback.onError(new RequestCanceledException("Request %s cancelled", coapRequest.getURI()));
    }

    @Override
    public void onReject() {
        cancelCleaningTask();
        errorCallback.onError(new RequestRejectedException("Request %s rejected", coapRequest.getURI()));
    }

    @Override
    public void onSendError(Throwable error) {
        cancelCleaningTask();
        errorCallback.onError(new SendFailedException(error, "Unable to send request %s", coapRequest.getURI()));
    }

    private synchronized void scheduleCleaningTask() {
        if (!cancelled)
            cleaningTask = getExecutor().schedule(new Runnable() {
                @Override
                public void run() {
                    responseTimedOut.set(true);
                    coapRequest.cancel();
                }
            }, timeoutInMs, TimeUnit.MILLISECONDS);
    }

    private synchronized void cancelCleaningTask() {
        if (cleaningTask != null) {
            cleaningTask.cancel(false);
        }
        cancelled = true;
    }

    private ScheduledExecutorService getExecutor() {
        if (executor == null) {
            synchronized (this.getClass()) {
                if (executor == null) {
                    executor = Executors.newScheduledThreadPool(1,
                            new NamedThreadFactory("Leshan Async Request timeout"));
                }
            }
        }
        return executor;
    }
}