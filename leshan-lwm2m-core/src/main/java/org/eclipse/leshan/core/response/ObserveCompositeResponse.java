/*******************************************************************************
 * Copyright (c) 2021 Orange.
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
 *     Michał Wadowski (Orange) - Add Observe-Composite feature.
 *******************************************************************************/
package org.eclipse.leshan.core.response;

import java.util.Map;

import org.eclipse.leshan.core.ResponseCode;
import org.eclipse.leshan.core.node.LwM2mNode;
import org.eclipse.leshan.core.node.LwM2mPath;
import org.eclipse.leshan.core.node.TimestampedLwM2mNodes;
import org.eclipse.leshan.core.observation.CompositeObservation;

/**
 * Specialized ReadCompositeResponse to a Observe-Composite request, with the corresponding Observation.
 *
 * This can be useful to listen to updates on the specific Observation.
 */
public class ObserveCompositeResponse extends ReadCompositeResponse {

    protected final CompositeObservation observation;
    protected final TimestampedLwM2mNodes timestampedValues;

    public ObserveCompositeResponse(ResponseCode code, Map<LwM2mPath, LwM2mNode> content,
            TimestampedLwM2mNodes timestampedValues, CompositeObservation observation, String errorMessage,
            Object coapResponse) {
        super(code, timestampedValues != null && !timestampedValues.isEmpty() ? timestampedValues.getNodes() : content,
                null, errorMessage, coapResponse);
        this.observation = observation;
        this.timestampedValues = timestampedValues;
    }

    public CompositeObservation getObservation() {
        return observation;
    }

    public TimestampedLwM2mNodes getTimestampedLwM2mNodes() {
        return timestampedValues;
    }

    @Override
    public String toString() {
        if (errorMessage != null)
            return String.format("ObserveCompositeResponse [code=%s, errorMessage=%s]", code, errorMessage);
        else if (timestampedValues != null)
            return String.format(
                    "ObserveCompositeResponse [code=%s, content=%s, observation=%s, timestampedValues= %d values]",
                    code, content, observation, timestampedValues.getTimestamps().size());
        else
            return String.format("ObserveCompositeResponse [code=%s,  content=%s, observation=%s]", code, observation,
                    content);
    }

    @Override
    public boolean isValid() {
        switch (code.getCode()) {
        case ResponseCode.CONTENT_CODE:
        case ResponseCode.BAD_REQUEST_CODE:
        case ResponseCode.NOT_FOUND_CODE:
        case ResponseCode.UNAUTHORIZED_CODE:
        case ResponseCode.METHOD_NOT_ALLOWED_CODE:
        case ResponseCode.UNSUPPORTED_CONTENT_FORMAT_CODE:
        case ResponseCode.INTERNAL_SERVER_ERROR_CODE:
            return true;
        default:
            return false;
        }
    }

    // Syntactic sugar static constructors:

    public static ObserveCompositeResponse success(Map<LwM2mPath, LwM2mNode> content) {
        return new ObserveCompositeResponse(ResponseCode.CONTENT, content, null, null, null, null);
    }

    public static ObserveCompositeResponse success(TimestampedLwM2mNodes timestampedValues) {
        return new ObserveCompositeResponse(ResponseCode.CONTENT, null, timestampedValues, null, null, null);
    }

    public static ObserveCompositeResponse badRequest(String errorMessage) {
        return new ObserveCompositeResponse(ResponseCode.BAD_REQUEST, null, null, null, errorMessage, null);
    }

    public static ObserveCompositeResponse notFound() {
        return new ObserveCompositeResponse(ResponseCode.NOT_FOUND, null, null, null, null, null);
    }

    public static ObserveCompositeResponse unauthorized() {
        return new ObserveCompositeResponse(ResponseCode.UNAUTHORIZED, null, null, null, null, null);
    }

    public static ObserveCompositeResponse methodNotAllowed() {
        return new ObserveCompositeResponse(ResponseCode.METHOD_NOT_ALLOWED, null, null, null, null, null);
    }

    public static ObserveCompositeResponse notAcceptable() {
        return new ObserveCompositeResponse(ResponseCode.UNSUPPORTED_CONTENT_FORMAT, null, null, null, null, null);
    }

    public static ObserveCompositeResponse internalServerError(String errorMessage) {
        return new ObserveCompositeResponse(ResponseCode.INTERNAL_SERVER_ERROR, null, null, null, errorMessage, null);
    }
}
