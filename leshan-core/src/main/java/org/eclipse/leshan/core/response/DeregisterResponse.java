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

public class DeregisterResponse extends AbstractLwM2mResponse {

    public DeregisterResponse(final ResponseCode code, final String errorMessage) {
        super(code, errorMessage);
    }

    @Override
    public String toString() {
        if (errorMessage != null)
            return String.format("DeregisterResponse [code=%s, errormessage=%s]", code, errorMessage);
        else
            return String.format("DeregisterResponse [code=%s]", code);
    }

    // Syntactic sugar static constructors :

    public static DeregisterResponse success() {
        return new DeregisterResponse(ResponseCode.DELETED, null);
    }

    public static DeregisterResponse badRequest(String errorMessage) {
        return new DeregisterResponse(ResponseCode.BAD_REQUEST, errorMessage);
    }

    public static DeregisterResponse notFound() {
        return new DeregisterResponse(ResponseCode.NOT_FOUND, null);
    }

    public static DeregisterResponse internalServerError(String errorMessage) {
        return new DeregisterResponse(ResponseCode.INTERNAL_SERVER_ERROR, errorMessage);
    }
}
