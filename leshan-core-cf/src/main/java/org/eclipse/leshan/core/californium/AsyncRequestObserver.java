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

import java.util.concurrent.ScheduledExecutorService;

import org.eclipse.californium.core.coap.Request;
import org.eclipse.californium.core.coap.Response;
import org.eclipse.leshan.core.response.ErrorCallback;
import org.eclipse.leshan.core.response.LwM2mResponse;
import org.eclipse.leshan.core.response.ResponseCallback;

/**
 * A dedicated {@link CoapAsyncRequestObserver} for LWM2M.
 * <p>
 * A {@link LwM2mResponse} is created from the CoAP Response received. This behavior is not implemented and should be
 * provided by overriding {@link #buildResponse(Response)}.
 *
 * @param <T> the type of the {@link LwM2mResponse} to build from the CoAP response
 */
public abstract class AsyncRequestObserver<T extends LwM2mResponse> extends CoapAsyncRequestObserver {

    /**
     * A Californium message observer for a CoAP request helping to get results asynchronously dedicated for LWM2M
     * requests.
     * <p>
     * The Californium API does not ensure that message callback are exclusive. E.g. In some race condition, you can get
     * a onReponse call and a onCancel one. The CoapAsyncRequestObserver ensure that you will receive only one event.
     * Meaning, you get either 1 response or 1 error.
     *
     * @param coapRequest The CoAP request to observe.
     * @param responseCallback This is called when a response is received. This MUST NOT be null.
     * @param errorCallback This is called when an error happens. This MUST NOT be null.
     * @param timeoutInMs A response timeout(in millisecond) which is raised if neither a response or error happens (see
     *        https://github.com/eclipse/leshan/wiki/Request-Timeout).
     * @param executor used to scheduled timeout tasks.
     */
    public AsyncRequestObserver(Request coapRequest, final ResponseCallback<T> responseCallback,
            final ErrorCallback errorCallback, long timeoutInMs, ScheduledExecutorService executor,
            ExceptionTranslator exceptionTranslator) {
        super(coapRequest, null, errorCallback, timeoutInMs, executor, exceptionTranslator);
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

    /**
     * Build the {@link LwM2mResponse} from the CoAP {@link Response}.
     *
     * @param coapResponse The CoAP response received.
     * @return the corresponding {@link LwM2mResponse}.
     */
    protected abstract T buildResponse(Response coapResponse);
}
