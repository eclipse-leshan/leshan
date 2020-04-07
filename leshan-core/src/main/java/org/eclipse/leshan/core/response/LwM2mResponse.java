/*******************************************************************************
 * Copyright (c) 2013-2015 Sierra Wireless and others.
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
package org.eclipse.leshan.core.response;

import org.eclipse.leshan.core.ResponseCode;

/**
 * A response to Lightweight M2M request.
 */
public interface LwM2mResponse {

    /**
     * Gets the response code.
     *
     * @return the code
     */
    ResponseCode getCode();

    /**
     * Gets the error Message. The message is similar to the Reason-Phrase on an HTTP status line. It is not intended
     * for end users but for software engineers that during debugging need to interpret it.
     *
     * @return the error message
     */
    String getErrorMessage();

    /**
     * Get the underlying CoAP response. The object type depends of the chosen CoAP implementation. (e.g with
     * Californium implementation <code>getCoapResponse()</code> will returns
     * <code>a org.eclipse.californium.core.coap.Response)</code>).
     * 
     * @return the CoAP response
     */
    Object getCoapResponse();

    /**
     * @return true if the request was successfully done.
     */
    boolean isSuccess();

    /**
     * @return true if we get an error or unexpected code.
     */
    boolean isFailure();

    /**
     * @return true if we get a valid response code, a code expected by the LWM2M spec.
     */
    boolean isValid();
}
