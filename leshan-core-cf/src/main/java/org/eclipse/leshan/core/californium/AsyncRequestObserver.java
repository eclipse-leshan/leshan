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
 *******************************************************************************/
package org.eclipse.leshan.core.californium;

import org.eclipse.californium.core.coap.Request;
import org.eclipse.californium.core.coap.Response;
import org.eclipse.leshan.core.request.exception.RequestCanceledException;
import org.eclipse.leshan.core.request.exception.RequestRejectedException;
import org.eclipse.leshan.core.response.ErrorCallback;
import org.eclipse.leshan.core.response.LwM2mResponse;
import org.eclipse.leshan.core.response.ResponseCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AsyncRequestObserver<T extends LwM2mResponse> extends AbstractRequestObserver<T> {
    private static final Logger LOG = LoggerFactory.getLogger(AsyncRequestObserver.class);

    private ResponseCallback<T> responseCallback;
    private ErrorCallback errorCallback;

    public AsyncRequestObserver(Request coapRequest, ResponseCallback<T> responseCallback,
            ErrorCallback errorCallback) {
        super(coapRequest);
        this.responseCallback = responseCallback;
        this.errorCallback = errorCallback;
    }

    @Override
    public void onResponse(Response coapResponse) {
        LOG.debug("Received coap response: {}", coapResponse);
        try {
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
    public void onTimeout() {
        errorCallback.onError(new org.eclipse.leshan.core.request.exception.TimeoutException("Request %s timed out",
                coapRequest.getURI()));
    }

    @Override
    public void onCancel() {
        errorCallback.onError(new RequestCanceledException("Request %s cancelled", coapRequest.getURI()));
    }

    @Override
    public void onReject() {
        errorCallback.onError(new RequestRejectedException("Request %s rejected", coapRequest.getURI()));
    }

}