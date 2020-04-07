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

import java.util.List;

import org.eclipse.leshan.core.ResponseCode;
import org.eclipse.leshan.core.node.LwM2mNode;
import org.eclipse.leshan.core.node.TimestampedLwM2mNode;
import org.eclipse.leshan.core.observation.Observation;

public class CancelObservationResponse extends ObserveResponse {

    public CancelObservationResponse(ResponseCode code, LwM2mNode content, List<TimestampedLwM2mNode> timestampedValues,
            Observation observation, String errorMessage) {
        super(code, content, timestampedValues, observation, errorMessage);
    }

    public CancelObservationResponse(ResponseCode code, LwM2mNode content, List<TimestampedLwM2mNode> timestampedValues,
            Observation observation, String errorMessage, Object coapResponse) {
        super(code, content, timestampedValues, observation, errorMessage, coapResponse);
    }

    @Override
    public String toString() {
        if (errorMessage != null)
            return String.format("CancelObservationResponse [code=%s, errormessage=%s]", code, errorMessage);
        else if (timestampedValues != null)
            return String.format(
                    "CancelObservationResponse [code=%s, content=%s, observation=%s, timestampedValues= %d nodes]",
                    code, content, observation, timestampedValues.size());
        else
            return String.format("CancelObservationResponse [code=%s, content=%s, observation=%s]", code, content,
                    observation);
    }

    // Syntactic sugar static constructors :

    public static CancelObservationResponse success(LwM2mNode content) {
        return new CancelObservationResponse(ResponseCode.CONTENT, content, null, null, null);
    }

    public static CancelObservationResponse success(List<TimestampedLwM2mNode> timestampedValues) {
        return new CancelObservationResponse(ResponseCode.CONTENT, null, timestampedValues, null, null);
    }

    public static CancelObservationResponse badRequest(String errorMessage) {
        return new CancelObservationResponse(ResponseCode.BAD_REQUEST, null, null, null, errorMessage);
    }

    public static CancelObservationResponse notFound() {
        return new CancelObservationResponse(ResponseCode.NOT_FOUND, null, null, null, null);
    }

    public static CancelObservationResponse unauthorized() {
        return new CancelObservationResponse(ResponseCode.UNAUTHORIZED, null, null, null, null);
    }

    public static CancelObservationResponse methodNotAllowed() {
        return new CancelObservationResponse(ResponseCode.METHOD_NOT_ALLOWED, null, null, null, null);
    }

    public static CancelObservationResponse notAcceptable() {
        return new CancelObservationResponse(ResponseCode.NOT_ACCEPTABLE, null, null, null, null);
    }

    public static CancelObservationResponse internalServerError(String errorMessage) {
        return new CancelObservationResponse(ResponseCode.INTERNAL_SERVER_ERROR, null, null, null, errorMessage);
    }
}
