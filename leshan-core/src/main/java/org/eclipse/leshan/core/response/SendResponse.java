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

public class SendResponse extends AbstractLwM2mResponse {

    public SendResponse(ResponseCode code, String errorMessage) {
        this(code, errorMessage, null);
    }

    public SendResponse(ResponseCode code, String errorMessage, Object coapResponse) {
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
        case ResponseCode.NOT_FOUND_CODE:
        case ResponseCode.INTERNAL_SERVER_ERROR_CODE:
            return true;
        default:
            return false;
        }
    }

    public static SendResponse success() {
        return new SendResponse(ResponseCode.CHANGED, null);
    }

    public static SendResponse badRequest(String errorMessage) {
        return new SendResponse(ResponseCode.BAD_REQUEST, errorMessage);
    }

    public static SendResponse notFound() {
        return new SendResponse(ResponseCode.NOT_FOUND, null);
    }

    public static SendResponse notFound(String errorMessage) {
        return new SendResponse(ResponseCode.NOT_FOUND, errorMessage);
    }

    public static SendResponse internalServerError(String errorMessage) {
        return new SendResponse(ResponseCode.INTERNAL_SERVER_ERROR, errorMessage);
    }
}
