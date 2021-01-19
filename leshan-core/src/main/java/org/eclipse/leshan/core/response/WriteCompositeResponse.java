/*******************************************************************************
 * Copyright (c) 2021 Sierra Wireless and others.
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

public class WriteCompositeResponse extends AbstractLwM2mResponse {

    public WriteCompositeResponse(ResponseCode code, String errorMessage) {
        super(code, errorMessage, null);
    }

    public WriteCompositeResponse(ResponseCode code, String errorMessage, Object coapResponse) {
        super(code, errorMessage, coapResponse);
    }

    @Override
    public boolean isSuccess() {
        return ResponseCode.CHANGED.equals(getCode());
    }

    @Override
    public boolean isValid() {
        switch (code.getCode()) {
        case ResponseCode.CHANGED_CODE:
        case ResponseCode.BAD_REQUEST_CODE:
        case ResponseCode.UNAUTHORIZED_CODE:
        case ResponseCode.NOT_FOUND_CODE:
        case ResponseCode.METHOD_NOT_ALLOWED_CODE:
        case ResponseCode.NOT_ACCEPTABLE_CODE:
        case ResponseCode.INTERNAL_SERVER_ERROR_CODE:
            return true;
        default:
            return false;
        }
    }

    public static WriteCompositeResponse success() {
        return new WriteCompositeResponse(ResponseCode.CHANGED, null);
    }

    public static WriteCompositeResponse badRequest(String errorMessage) {
        return new WriteCompositeResponse(ResponseCode.BAD_REQUEST, errorMessage);
    }

    public static WriteCompositeResponse unauthorized() {
        return new WriteCompositeResponse(ResponseCode.UNAUTHORIZED, null);
    }

    public static WriteCompositeResponse notFound(String errorMessage) {
        return new WriteCompositeResponse(ResponseCode.NOT_FOUND, errorMessage);
    }

    public static WriteCompositeResponse methodNotAllowed(String errorMessage) {
        return new WriteCompositeResponse(ResponseCode.METHOD_NOT_ALLOWED, errorMessage);
    }

    public static WriteCompositeResponse notAcceptable() {
        return new WriteCompositeResponse(ResponseCode.NOT_ACCEPTABLE, null);
    }

    public static WriteCompositeResponse internalServerError(String errorMessage) {
        return new WriteCompositeResponse(ResponseCode.INTERNAL_SERVER_ERROR, errorMessage);
    }
}