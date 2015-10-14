/*******************************************************************************
 * Copyright (c) 2013-2015 Sierra Wireless and others.
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
import org.eclipse.leshan.core.node.LwM2mNode;
import org.eclipse.leshan.core.observation.Observation;

/**
 * Specialized ReadResponse to a Observe request, with the corresponding Observation.
 *
 * This can be useful to listen to updates on the specific Observation.
 */
public class ObserveResponse extends ReadResponse {

    private Observation observation;

    public ObserveResponse(ResponseCode code, LwM2mNode content, Observation observation, String errorMessage) {
        super(code, content, errorMessage);
        this.observation = observation;
    }

    @Override
    public String toString() {
        if (errorMessage != null)
            return String.format("ObserveResponse [code=%s, errormessage=%s]", code, errorMessage);
        else
            return String.format("ObserveResponse [code=%s, content=%s, observation=%s]", code, content, observation);
    }

    public Observation getObservation() {
        return observation;
    }

    // Syntactic sugar static constructors :

    public static ObserveResponse success(LwM2mNode content) {
        return new ObserveResponse(ResponseCode.CONTENT, content, null, null);
    }

    public static ObserveResponse badRequest(String errorMessage) {
        return new ObserveResponse(ResponseCode.BAD_REQUEST, null, null, errorMessage);
    }

    public static ObserveResponse notFound() {
        return new ObserveResponse(ResponseCode.NOT_FOUND, null, null, null);
    }

    public static ObserveResponse unauthorized() {
        return new ObserveResponse(ResponseCode.UNAUTHORIZED, null, null, null);
    }

    public static ObserveResponse methodNotAllowed() {
        return new ObserveResponse(ResponseCode.METHOD_NOT_ALLOWED, null, null, null);
    }

    public static ObserveResponse internalServerError(String errorMessage) {
        return new ObserveResponse(ResponseCode.INTERNAL_SERVER_ERROR, null, null, errorMessage);
    }
}
