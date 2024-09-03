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
import org.eclipse.leshan.core.node.TimestampedLwM2mNode;
import org.eclipse.leshan.core.node.TimestampedLwM2mNodes;
import org.eclipse.leshan.core.observation.CompositeObservation;

/**
 * Specialized ReadCompositeResponse to a Observe-Composite request, with the corresponding Observation.
 *
 * This can be useful to listen to updates on the specific Observation.
 */
public class ObserveCompositeResponse extends ReadCompositeResponse {

    protected final CompositeObservation observation;
    protected final TimestampedLwM2mNodes historicalValues;

    /**
     * Generic constructor to create an {@link ObserveCompositeResponse}
     *
     * On success response only one of that argument must be not <code>null</code> :
     * <ul>
     * <li>content
     * <li>timestampedContent
     * <li>historicalValues
     * </ul>
     *
     * errorMessage could only be null on error.
     *
     * @param responseCode The {@link ResponseCode} of the response
     * @param content If response code is success, content is all {@link LwM2mNode} requested. (else <code>null</code>)
     * @param timestampedContent If code is success, timestampedContent is all {@link LwM2mNode} requested with 1
     *        associated timestamp. (else <code>null</code>)
     * @param historicalValues If code is success and only for notification, historicalValues a representation of all
     *        {@link LwM2mNode} requested at different instant. This argument is used when "Notification Storing When
     *        Disabled or Offline" is used. (else <code>null</code>)
     * @param observation The composite observation linked to this response.
     * @param errorMessage A optional error message if {@link ResponseCode} is an error. (else <code>null</code>)
     * @param coapResponse The underlying protocol response object generally when you receive the response not when you
     *        send it.
     */
    public ObserveCompositeResponse(ResponseCode responseCode, Map<LwM2mPath, LwM2mNode> content,
            TimestampedLwM2mNodes timestampedContent, TimestampedLwM2mNodes historicalValues,
            CompositeObservation observation, String errorMessage, Object coapResponse) {
        super(responseCode, content, //
                timestampedContent != null ? timestampedContent : //
                        historicalValues != null ? historicalValues.getMostRecentTimestampedNodes() : null,
                errorMessage, coapResponse);
        this.observation = observation;
        if (content != null || timestampedContent != null) {
            if (historicalValues != null) {
                throw new IllegalArgumentException(
                        "content OR timestampedValue OR historicalValues but not several of them");
            }
        }
        this.historicalValues = historicalValues;
    }

    public CompositeObservation getObservation() {
        return observation;
    }

    /**
     * {@inheritDoc}
     * <p>
     * In case where client is using "Notification Storing When Disabled or Offline", the most recent value is returned.
     * If you want the full historical timestamped values, you should use use {@link #getTimestampedLwM2mNodes()}.
     */
    @Override
    public Map<LwM2mPath, LwM2mNode> getContent() {
        return super.getContent();
    }

    /**
     * {@inheritDoc}
     *
     * <p>
     * In case where client is using "Notification Storing When Disabled or Offline", the most recent value is returned.
     * If you want the full historical timestamped values, you should use use {@link #getTimestampedLwM2mNodes()}.
     */
    @Override
    public TimestampedLwM2mNodes getTimestampedLwM2mNode() {
        if (historicalValues != null) {
            return historicalValues.getMostRecentTimestampedNodes();
        } else {
            return super.getTimestampedLwM2mNode();
        }
    }

    /**
     * A representation of all {@link LwM2mNode} requested at different instant. This method returns value only on
     * notification when client are using "Notification Storing When Disabled or Offline" and content format support it.
     * <p>
     * The list is sorted by descending time-stamp order (most recent one at first place). If null time-stamp (meaning
     * no time information) exists it always at first place as we consider it as "now".
     *
     * @return a list of {@link TimestampedLwM2mNode} OR <code>null</code> if this is a error response or "Notification
     *         Storing When Disabled or Offline" is not used.
     */
    public TimestampedLwM2mNodes getTimestampedLwM2mNodes() {
        return historicalValues;
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
        return new ObserveCompositeResponse(ResponseCode.CONTENT, content, null, null, null, null, null);
    }

    public static ObserveCompositeResponse successWithTimestampedContent(TimestampedLwM2mNodes timestampedValues) {
        return new ObserveCompositeResponse(ResponseCode.CONTENT, null, timestampedValues, null, null, null, null);
    }

    public static ObserveCompositeResponse successWithHistoricalData(TimestampedLwM2mNodes historicalValues) {
        return new ObserveCompositeResponse(ResponseCode.CONTENT, null, null, historicalValues, null, null, null);
    }

    public static ObserveCompositeResponse badRequest(String errorMessage) {
        return new ObserveCompositeResponse(ResponseCode.BAD_REQUEST, null, null, null, null, errorMessage, null);
    }

    public static ObserveCompositeResponse notFound() {
        return new ObserveCompositeResponse(ResponseCode.NOT_FOUND, null, null, null, null, null, null);
    }

    public static ObserveCompositeResponse unauthorized() {
        return new ObserveCompositeResponse(ResponseCode.UNAUTHORIZED, null, null, null, null, null, null);
    }

    public static ObserveCompositeResponse methodNotAllowed() {
        return new ObserveCompositeResponse(ResponseCode.METHOD_NOT_ALLOWED, null, null, null, null, null, null);
    }

    public static ObserveCompositeResponse notAcceptable() {
        return new ObserveCompositeResponse(ResponseCode.UNSUPPORTED_CONTENT_FORMAT, null, null, null, null, null,
                null);
    }

    public static ObserveCompositeResponse internalServerError(String errorMessage) {
        return new ObserveCompositeResponse(ResponseCode.INTERNAL_SERVER_ERROR, null, null, null, null, errorMessage,
                null);
    }
}
