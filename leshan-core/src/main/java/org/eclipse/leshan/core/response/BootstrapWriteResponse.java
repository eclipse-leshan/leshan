/*******************************************************************************
 * Copyright (c) 2015 Sierra Wireless and others.
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
package org.eclipse.leshan.core.response;

import org.eclipse.leshan.ResponseCode;

/**
 * Response to a write request from the bootstrap server.
 */
public class BootstrapWriteResponse extends AbstractLwM2mResponse {

    public BootstrapWriteResponse(ResponseCode code, String errorMessage) {
        this(code, errorMessage, null);
    }

    public BootstrapWriteResponse(ResponseCode code, String errorMessage, Object coapResponse) {
        super(code, errorMessage, coapResponse);
    }

    @Override
    public boolean isSuccess() {
        return getCode() == ResponseCode.CHANGED;
    }

    @Override
    public String toString() {
        if (errorMessage != null)
            return String.format("BootstrapWriteResponse [code=%s, errormessage=%s]", code, errorMessage);
        else
            return String.format("BootstrapWriteResponse [code=%s]", code);
    }

    // Syntactic sugar static constructors :

    public static BootstrapWriteResponse success() {
        return new BootstrapWriteResponse(ResponseCode.CHANGED, null);
    }

    public static BootstrapWriteResponse unsupportedContentFormat() {
        return new BootstrapWriteResponse(ResponseCode.UNSUPPORTED_CONTENT_FORMAT, null);
    }

    public static BootstrapWriteResponse badRequest(String errorMessage) {
        return new BootstrapWriteResponse(ResponseCode.BAD_REQUEST, errorMessage);
    }

    public static BootstrapWriteResponse internalServerError(String errorMessage) {
        return new BootstrapWriteResponse(ResponseCode.INTERNAL_SERVER_ERROR, errorMessage);
    }

}
