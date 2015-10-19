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

public class ExecuteResponse extends AbstractLwM2mResponse {

    public ExecuteResponse(final ResponseCode code, final String errorMessage) {
        super(code, errorMessage);
    }

    @Override
    public String toString() {
        if (errorMessage != null)
            return String.format("ExecuteResponse [code=%s, errormessage=%s]", code, errorMessage);
        else
            return String.format("ExecuteResponse [code=%s]", code);
    }

    // Syntactic sugar static constructors :

    public static ExecuteResponse success() {
        return new ExecuteResponse(ResponseCode.CHANGED, null);
    }

    public static ExecuteResponse badRequest(String errorMessage) {
        return new ExecuteResponse(ResponseCode.BAD_REQUEST, errorMessage);
    }

    public static ExecuteResponse notFound() {
        return new ExecuteResponse(ResponseCode.NOT_FOUND, null);
    }

    public static ExecuteResponse unauthorized() {
        return new ExecuteResponse(ResponseCode.UNAUTHORIZED, null);
    }

    public static ExecuteResponse methodNotAllowed() {
        return new ExecuteResponse(ResponseCode.METHOD_NOT_ALLOWED, null);
    }

    public static ExecuteResponse internalServerError(String errorMessage) {
        return new ExecuteResponse(ResponseCode.INTERNAL_SERVER_ERROR, errorMessage);
    }
}