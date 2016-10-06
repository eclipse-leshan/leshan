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

public class UpdateResponse extends AbstractLwM2mResponse {

    public UpdateResponse(final ResponseCode code, final String errorMessage) {
        this(code, errorMessage, null);
    }

    public UpdateResponse(final ResponseCode code, final String errorMessage, Object coapResponse) {
        super(code, errorMessage, coapResponse);
    }

    @Override
    public boolean isSuccess() {
        return getCode() == ResponseCode.CHANGED;
    }

    @Override
    public String toString() {
        if (errorMessage != null)
            return String.format("UpdateResponse [code=%s, errormessage=%s]", code, errorMessage);
        else
            return String.format("UpdateResponse [code=%s]", code);
    }

    // Syntactic sugar static constructors :

    public static UpdateResponse success() {
        return new UpdateResponse(ResponseCode.CHANGED, null);
    }

    public static UpdateResponse badRequest(String errorMessage) {
        return new UpdateResponse(ResponseCode.BAD_REQUEST, errorMessage);
    }

    public static UpdateResponse notFound() {
        return new UpdateResponse(ResponseCode.NOT_FOUND, null);
    }

    public static UpdateResponse internalServerError(String errorMessage) {
        return new UpdateResponse(ResponseCode.INTERNAL_SERVER_ERROR, errorMessage);
    }
}
