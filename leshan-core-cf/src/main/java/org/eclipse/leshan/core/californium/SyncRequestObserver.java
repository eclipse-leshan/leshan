/*******************************************************************************
 * Copyright (c) 2018 Sierra Wireless and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * and Eclipse Distribution License v1.0 which accompany this distribution.
 *
 * The Eclipse Public License is available at
 *    http://www.eclipse.org/legal/epl-v20.html
 * and the Eclipse Distribution License is available at
 *    http://www.eclipse.org/org/documents/edl-v10.html.
 *
 * Contributors:
 *     Sierra Wireless - initial API and implementation
 *******************************************************************************/
package org.eclipse.leshan.core.californium;

import org.eclipse.californium.core.coap.Request;
import org.eclipse.californium.core.coap.Response;
import org.eclipse.leshan.core.request.exception.InvalidResponseException;
import org.eclipse.leshan.core.request.exception.RequestCanceledException;
import org.eclipse.leshan.core.request.exception.RequestRejectedException;
import org.eclipse.leshan.core.request.exception.SendFailedException;
import org.eclipse.leshan.core.response.LwM2mResponse;

/**
 * A dedicated {@link SyncRequestObserver} for LWM2M.
 * <p>
 * A {@link LwM2mResponse} is created from the CoAP Response received. This behavior is not implemented and should be
 * provided by overriding {@link #buildResponse(Response)}.
 *
 * @param <T> the type of the {@link LwM2mResponse} to build from the CoAP response
 */
public abstract class SyncRequestObserver<T extends LwM2mResponse> extends CoapSyncRequestObserver {

    public SyncRequestObserver(Request coapRequest, long timeout) {
        this(coapRequest, timeout, new TemporaryExceptionTranslator());
    }

    public SyncRequestObserver(Request coapRequest, long timeout, ExceptionTranslator exceptionTranslator) {
        super(coapRequest, timeout, exceptionTranslator);
    }

    /**
     * Wait for the LWM2M response.
     *
     * @return the LWM2M response. The response can be <code>null</code> if the timeout expires (see
     *         https://github.com/eclipse/leshan/wiki/Request-Timeout).
     *
     * @throws InterruptedException if the thread was interrupted.
     * @throws RequestRejectedException if the request is rejected by foreign peer.
     * @throws RequestCanceledException if the request is cancelled.
     * @throws SendFailedException if the request can not be sent. E.g. error at CoAP or DTLS/UDP layer.
     * @throws InvalidResponseException if the response received is malformed.
     */
    public T waitForResponse() throws InterruptedException {
        Response coapResponse = waitForCoapResponse();
        if (coapResponse != null) {
            return buildResponse(coapResponse);
        } else {
            return null;
        }
    }

    protected abstract T buildResponse(Response coapResponse);
}
