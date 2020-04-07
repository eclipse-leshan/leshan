/*******************************************************************************
 * Copyright (c) 2015 Sierra Wireless and others.
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

public class WriteAttributesResponse extends AbstractLwM2mResponse {

    public WriteAttributesResponse(ResponseCode code, String errorMessage) {
        super(code, errorMessage, null);
    }

    public WriteAttributesResponse(ResponseCode code, String errorMessage, Object coapResponse) {
        super(code, errorMessage, coapResponse);
    }

    @Override
    public boolean isSuccess() {
        return getCode() == ResponseCode.CHANGED;
    }

    @Override
    public boolean isValid() {
        switch (code.getCode()) {
        case ResponseCode.CHANGED_CODE:
        case ResponseCode.BAD_REQUEST_CODE:
        case ResponseCode.UNAUTHORIZED_CODE:
        case ResponseCode.NOT_FOUND_CODE:
        case ResponseCode.METHOD_NOT_ALLOWED_CODE:
        case ResponseCode.INTERNAL_SERVER_ERROR_CODE:
            return true;
        default:
            return false;
        }
    }

    @Override
    public String toString() {
        if (errorMessage != null)
            return String.format("WriteAttributesResponse [code=%s, errormessage=%s]", code, errorMessage);
        else
            return String.format("WriteAttributesResponse [code=%s]", code);
    }

    // Syntactic sugar static constructors :

    public static WriteAttributesResponse success() {
        return new WriteAttributesResponse(ResponseCode.CHANGED, null);
    }

    public static WriteAttributesResponse badRequest(String errorMessage) {
        return new WriteAttributesResponse(ResponseCode.BAD_REQUEST, errorMessage);
    }

    public static WriteAttributesResponse notFound() {
        return new WriteAttributesResponse(ResponseCode.NOT_FOUND, null);
    }

    public static WriteAttributesResponse unauthorized() {
        return new WriteAttributesResponse(ResponseCode.UNAUTHORIZED, null);
    }

    public static WriteAttributesResponse methodNotAllowed() {
        return new WriteAttributesResponse(ResponseCode.METHOD_NOT_ALLOWED, null);
    }

    public static WriteAttributesResponse internalServerError(String errorMessage) {
        return new WriteAttributesResponse(ResponseCode.INTERNAL_SERVER_ERROR, errorMessage);
    }
}
