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
package org.eclipse.leshan.server.californium;

import org.eclipse.californium.core.coap.Request;
import org.eclipse.californium.core.coap.Response;
import org.eclipse.leshan.core.californium.CoapResponseCallback;
import org.eclipse.leshan.core.node.codec.CodecException;
import org.eclipse.leshan.core.response.ErrorCallback;
import org.eclipse.leshan.server.registration.Registration;

public interface CoapRequestSender {

    /**
     * Sends a CoAP request synchronously. Will block until a response is received from the remote client.
     * 
     * @param destination the remote client
     * @param request the request to send to the client
     * @param timeout the request timeout in millisecond
     * @return the response or <code>null</code> if the timeout expires (given parameter or CoAP timeout).
     * 
     * @throws CodecException if request payload can not be encoded.
     * @throws InterruptedException if the thread was interrupted.
     */
    Response sendCoapRequest(final Registration destination, final Request coapRequest, long timeout)
            throws InterruptedException;

    /**
     * Sends a CoAP request asynchronously.
     * 
     * @param destination the remote client
     * @param request the request to send to the client
     * @param timeout the request timeout in millisecond
     * @param responseCallback a callback called when a response is received (successful or error response)
     * @param errorCallback a callback called when an error or exception occurred when response is received
     * 
     * @throws CodecException if request payload can not be encoded.
     */
    void sendCoapRequest(final Registration destination, final Request coapRequest, long timeout,
            CoapResponseCallback responseCallback, ErrorCallback errorCallback);
}