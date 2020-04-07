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

/**
 * Response to a delete request from the bootstrap server.
 */
public class BootstrapDeleteResponse extends AbstractLwM2mResponse {

    public BootstrapDeleteResponse(ResponseCode code, String errorMessage) {
        this(code, errorMessage, null);
    }

    public BootstrapDeleteResponse(ResponseCode code, String errorMessage, Object coapResponse) {
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
        case ResponseCode.INTERNAL_SERVER_ERROR_CODE:
            return true;
        default:
            return false;
        }
    }

    @Override
    public String toString() {
        if (errorMessage != null)
            return String.format("BootstrapDeleteResponse [code=%s, errormessage=%s]", code, errorMessage);
        else
            return String.format("BootstrapDeleteResponse [code=%s]", code);
    }

    // Syntactic sugar static constructors :

    public static BootstrapDeleteResponse success() {
        return new BootstrapDeleteResponse(ResponseCode.DELETED, null);
    }

    public static BootstrapDeleteResponse badRequest(String errorMessage) {
        return new BootstrapDeleteResponse(ResponseCode.BAD_REQUEST, errorMessage);
    }

    public static BootstrapDeleteResponse internalServerError(String errorMessage) {
        return new BootstrapDeleteResponse(ResponseCode.INTERNAL_SERVER_ERROR, errorMessage);
    }
}
