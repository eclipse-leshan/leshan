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

public class WriteResponse extends AbstractLwM2mResponse {

    public WriteResponse(ResponseCode code, String errorMessage) {
        super(code, errorMessage, null);
    }

    public WriteResponse(ResponseCode code, String errorMessage, Object coapResponse) {
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
        case ResponseCode.REQUEST_ENTITY_INCOMPLETE_CODE:
        case ResponseCode.REQUEST_ENTITY_TOO_LARGE_CODE:
        case ResponseCode.UNSUPPORTED_CONTENT_FORMAT_CODE:
        case ResponseCode.INTERNAL_SERVER_ERROR_CODE:
            return true;
        default:
            return false;
        }
    }

    @Override
    public String toString() {
        if (errorMessage != null)
            return String.format("WriteResponse [code=%s, errormessage=%s]", code, errorMessage);
        else
            return String.format("WriteResponse [code=%s]", code);
    }

    // Syntactic sugar static constructors :

    public static WriteResponse success() {
        return new WriteResponse(ResponseCode.CHANGED, null);
    }

    public static WriteResponse badRequest(String errorMessage) {
        return new WriteResponse(ResponseCode.BAD_REQUEST, errorMessage);
    }

    public static WriteResponse notFound() {
        return new WriteResponse(ResponseCode.NOT_FOUND, null);
    }

    public static WriteResponse unauthorized() {
        return new WriteResponse(ResponseCode.UNAUTHORIZED, null);
    }

    public static WriteResponse methodNotAllowed() {
        return new WriteResponse(ResponseCode.METHOD_NOT_ALLOWED, null);
    }

    public static WriteResponse unsupportedContentFormat() {
        return new WriteResponse(ResponseCode.UNSUPPORTED_CONTENT_FORMAT, null);
    }

    public static WriteResponse internalServerError(String errorMessage) {
        return new WriteResponse(ResponseCode.INTERNAL_SERVER_ERROR, errorMessage);
    }
}
