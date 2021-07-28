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

import org.eclipse.leshan.core.ResponseCode;
import org.eclipse.leshan.core.node.LwM2mNode;
import org.eclipse.leshan.core.node.LwM2mPath;
import org.eclipse.leshan.core.node.TimestampedLwM2mNode;
import org.eclipse.leshan.core.observation.CompositeObservation;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Specialized ReadCompositeResponse to a Observe-Composite request, with the corresponding Observation.
 *
 * This can be useful to listen to updates on the specific Observation.
 */
public class ObserveCompositeResponse extends ReadCompositeResponse {

    protected final CompositeObservation observation;

    protected final Map<LwM2mPath, List<TimestampedLwM2mNode>> timestampedValues;

    public ObserveCompositeResponse(ResponseCode code, Map<LwM2mPath, LwM2mNode> content, String errorMessage,
            Object coapResponse, CompositeObservation observation) {
        this(code, content, null, errorMessage, coapResponse, observation);
    }

    public ObserveCompositeResponse(ResponseCode code, Map<LwM2mPath, LwM2mNode> content,
            Map<LwM2mPath, List<TimestampedLwM2mNode>> timestampedValues,
            String errorMessage, Object coapResponse, CompositeObservation observation) {
        super(code, getContent(content, timestampedValues), errorMessage, coapResponse);
        this.timestampedValues = timestampedValues;
        this.observation = observation;
    }

    private static Map<LwM2mPath, LwM2mNode> getContent(Map<LwM2mPath, LwM2mNode> content, Map<LwM2mPath,
            List<TimestampedLwM2mNode>> timestampedValues) {
        if (content != null || timestampedValues == null || timestampedValues.isEmpty()) {
            return content;
        }

        Map<LwM2mPath, LwM2mNode> result = new HashMap<>();
        for (Map.Entry<LwM2mPath, List<TimestampedLwM2mNode>> entry : timestampedValues.entrySet()) {
            if (entry.getValue().size() > 0) {
                LwM2mPath path = entry.getKey();
                LwM2mNode node = entry.getValue().get(0).getNode();
                result.put(path, node);
            }
        }
        return result;
    }

    public CompositeObservation getObservation() {
        return observation;
    }

    public Map<LwM2mPath, List<TimestampedLwM2mNode>> getTimestampedValues() {
        return timestampedValues;
    }

    @Override
    public String toString() {
        if (errorMessage != null) {
            return String.format("ObserveCompositeResponse [code=%s, errormessage=%s]", code, errorMessage);
        } else if (timestampedValues != null) {
            return String.format("ObserveCompositeResponse [code=%s, content=%s, observation=%s, timestampedValues=" +
                    "%d entries]", code, content, observation, timestampedValues.keySet().size());
        } else {
            return String.format("ObserveCompositeResponse [code=%s, content=%s, observation=%s]", code,
                    content, observation
            );
        }
    }

    // Syntactic sugar static constructors:

    public static ObserveCompositeResponse success(Map<LwM2mPath, LwM2mNode> content) {
        return new ObserveCompositeResponse(ResponseCode.CONTENT, content, null, null, null);
    }

    public static ObserveCompositeResponse badRequest(String errorMessage) {
        return new ObserveCompositeResponse(ResponseCode.BAD_REQUEST, null, errorMessage, null, null);
    }

    public static ObserveCompositeResponse notFound() {
        return new ObserveCompositeResponse(ResponseCode.NOT_FOUND, null, null, null, null);
    }

    public static ObserveCompositeResponse unauthorized() {
        return new ObserveCompositeResponse(ResponseCode.UNAUTHORIZED, null, null, null, null);
    }

    public static ObserveCompositeResponse methodNotAllowed() {
        return new ObserveCompositeResponse(ResponseCode.METHOD_NOT_ALLOWED, null, null, null, null);
    }

    public static ObserveCompositeResponse notAcceptable() {
        return new ObserveCompositeResponse(ResponseCode.NOT_ACCEPTABLE, null, null, null, null);
    }

    public static ObserveCompositeResponse internalServerError(String errorMessage) {
        return new ObserveCompositeResponse(ResponseCode.INTERNAL_SERVER_ERROR, null, errorMessage, null, null);
    }
}
