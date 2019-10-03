/*******************************************************************************
 * Copyright (c) 2018 Sierra Wireless and others.
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
import org.eclipse.leshan.core.response.ErrorCallback;
import org.eclipse.leshan.core.response.LwM2mResponse;
import org.eclipse.leshan.core.response.ResponseCallback;

public abstract class AsyncRequestObserver<T extends LwM2mResponse> extends CoapAsyncRequestObserver {

    public AsyncRequestObserver(Request coapRequest, final ResponseCallback<T> responseCallback,
            final ErrorCallback errorCallback, long timeoutInMs) {
        super(coapRequest, null, errorCallback, timeoutInMs);
        this.responseCallback = new CoapResponseCallback() {

            @Override
            public void onResponse(Response coapResponse) {
                T lwM2mResponseT = null;
                try {
                    lwM2mResponseT = buildResponse(coapResponse);
                } catch (Exception e) {
                    errorCallback.onError(e);
                }
                if (lwM2mResponseT != null) {
                    responseCallback.onResponse(lwM2mResponseT);
                }
            }
        };
    }

    protected abstract T buildResponse(Response coapResponse);
}
