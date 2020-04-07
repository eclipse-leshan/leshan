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
 * Response to a client registration request
 */
public class RegisterResponse extends AbstractLwM2mResponse {

    private final String registrationID;

    public RegisterResponse(ResponseCode code, String registrationID, String errorMessage) {
        this(code, registrationID, errorMessage, null);
    }

    public RegisterResponse(ResponseCode code, String registrationID, String errorMessage, Object coapResponse) {
        super(code, errorMessage, coapResponse);
        this.registrationID = registrationID;
    }

    public String getRegistrationID() {
        return registrationID;
    }

    @Override
    public boolean isSuccess() {
        return getCode() == ResponseCode.CREATED;
    }

    @Override
    public boolean isValid() {
        switch (code.getCode()) {
        case ResponseCode.CREATED_CODE:
        case ResponseCode.BAD_REQUEST_CODE:
        case ResponseCode.FORBIDDEN_CODE:
        case ResponseCode.PRECONDITION_FAILED_CODE:
        case ResponseCode.INTERNAL_SERVER_ERROR_CODE:
            return true;
        default:
            return false;
        }
    }

    @Override
    public String toString() {
        if (errorMessage != null)
            return String.format("RegisterResponse [code=%s, errormessage=%s]", code, errorMessage);
        else
            return String.format("RegisterResponse [code=%s, registrationID=%s]", code, registrationID);
    }

    // Syntactic sugar static constructors :

    public static RegisterResponse success(String registrationID) {
        return new RegisterResponse(ResponseCode.CREATED, registrationID, null);
    }

    public static RegisterResponse badRequest(String errorMessage) {
        return new RegisterResponse(ResponseCode.BAD_REQUEST, null, errorMessage);
    }

    public static RegisterResponse forbidden(String errorMessage) {
        return new RegisterResponse(ResponseCode.FORBIDDEN, null, errorMessage);
    }

    public static RegisterResponse preconditionFailed(String errorMessage) {
        return new RegisterResponse(ResponseCode.PRECONDITION_FAILED, null, errorMessage);
    }

    public static RegisterResponse internalServerError(String errorMessage) {
        return new RegisterResponse(ResponseCode.INTERNAL_SERVER_ERROR, null, errorMessage);
    }
}
