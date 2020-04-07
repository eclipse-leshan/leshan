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

public class DeleteResponse extends AbstractLwM2mResponse {

    public DeleteResponse(ResponseCode code, String errorMessage) {
        this(code, errorMessage, null);
    }

    public DeleteResponse(ResponseCode code, String errorMessage, Object coapResponse) {
        super(code, errorMessage, coapResponse);
    }

    @Override
    public boolean isSuccess() {
        return getCode() == ResponseCode.DELETED;
    }

    @Override
    public boolean isValid() {
        switch (code.getCode()) {
        case ResponseCode.DELETED_CODE:
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
            return String.format("DeleteResponse [code=%s, errormessage=%s]", code, errorMessage);
        else
            return String.format("DeleteResponse [code=%s]", code);
    }

    // Syntactic sugar static constructors :

    public static DeleteResponse success() {
        return new DeleteResponse(ResponseCode.DELETED, null);
    }

    public static DeleteResponse badRequest(String errorMessage) {
        return new DeleteResponse(ResponseCode.BAD_REQUEST, errorMessage);
    }

    public static DeleteResponse notFound() {
        return new DeleteResponse(ResponseCode.NOT_FOUND, null);
    }

    public static DeleteResponse unauthorized() {
        return new DeleteResponse(ResponseCode.UNAUTHORIZED, null);
    }

    public static DeleteResponse methodNotAllowed() {
        return new DeleteResponse(ResponseCode.METHOD_NOT_ALLOWED, null);
    }

    public static DeleteResponse internalServerError(String errorMessage) {
        return new DeleteResponse(ResponseCode.INTERNAL_SERVER_ERROR, errorMessage);
    }
}
