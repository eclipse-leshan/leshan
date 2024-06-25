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
import org.eclipse.leshan.core.node.LwM2mChildNode;
import org.eclipse.leshan.core.node.LwM2mNode;
import org.eclipse.leshan.core.node.TimestampedLwM2mNode;
import org.eclipse.leshan.core.observation.SingleObservation;
import org.eclipse.leshan.core.request.exception.InvalidResponseException;

/**
 * Specialized ReadResponse to a Observe request, with the corresponding Observation.
 *
 * This can be useful to listen to updates on the specific Observation.
 */
public class ObserveResponse extends ReadResponse {

    protected final SingleObservation observation;
    protected final List<TimestampedLwM2mNode> timestampedValues;

    public ObserveResponse(ResponseCode code, LwM2mNode content, TimestampedLwM2mNode timestampedContent,
            List<TimestampedLwM2mNode> timestampedValues, SingleObservation observation, String errorMessage) {
        this(code, content, timestampedContent, timestampedValues, observation, errorMessage, null);
    }

    public ObserveResponse(ResponseCode code, LwM2mNode content, TimestampedLwM2mNode timestampedContent,
            List<TimestampedLwM2mNode> timestampedValues, SingleObservation observation, String errorMessage,
            Object coapResponse) {
        super(code, content, //
                timestampedContent != null ? timestampedContent : //
                        timestampedValues != null && !timestampedValues.isEmpty() ? timestampedValues.get(0) : null,
                errorMessage, coapResponse);

        if (timestampedValues != null) {
            if (content != null || timestampedContent != null) {
                throw new IllegalArgumentException(
                        "Only one of 'content' OR 'timestampedContent' OR 'timestampedValues' should not be null");
            }
            this.timestampedValues = timestampedValues;
        } else {
            this.timestampedValues = null;
        }

        // CHANGED is out of spec but is supported for backward compatibility. (previous draft version)
        if (ResponseCode.CHANGED.equals(code)) {
            if (getContent() == null)
                throw new InvalidResponseException("Content is mandatory for successful response");
        }
        this.observation = observation;
    }

    /**
     * {@inheritDoc}
     * <p>
     * In case where client is using "Notification Storing When Disabled or Offline", the most recent value is returned.
     * If you want the full historical timestamped values, you should use use {@link #getTimestampedLwM2mNodes()}.
     */
    @Override
    public LwM2mChildNode getContent() {
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
    public TimestampedLwM2mNode getTimestampedLwM2mNode() {
        if (timestampedValues != null && !timestampedValues.isEmpty()) {
            return timestampedValues.get(0);
        } else {
            return super.getTimestampedLwM2mNode();
        }
    }

    /**
     * A list of {@link LwM2mNode} representing different state of this resources at different instant. This method
     * returns value only on notification when client are using "Notification Storing When Disabled or Offline" and
     * content format support it.
     * <p>
     * The list is sorted by descending time-stamp order (most recent one at first place). If null time-stamp (meaning
     * no time information) exists it always at first place as we consider it as "now".
     *
     * @return a list of {@link TimestampedLwM2mNode} OR <code>null</code> if this is a error response or "Notification
     *         Storing When Disabled or Offline" is not used.
     */
    public List<TimestampedLwM2mNode> getTimestampedLwM2mNodes() {
        return timestampedValues;
    }

    @Override
    public boolean isSuccess() {
        // CHANGED is out of spec but is supported for backward compatibility. (previous draft version)
        return getCode().equals(ResponseCode.CONTENT) || getCode().equals(ResponseCode.CHANGED);
    }

    @Override
    public String toString() {
        if (errorMessage != null)
            return String.format("ObserveResponse [code=%s, errormessage=%s]", code, errorMessage);
        else if (timestampedValues != null)
            return String.format("ObserveResponse [code=%s, content=%s, observation=%s, timestampedValues= %d nodes]",
                    code, content, observation, timestampedValues.size());
        else
            return String.format("ObserveResponse [code=%s, content=%s, observation=%s]", code, content, observation);
    }

    public SingleObservation getObservation() {
        return observation;
    }

    // Syntactic sugar static constructors :

    public static ObserveResponse success(LwM2mNode content) {
        return new ObserveResponse(ResponseCode.CONTENT, content, null, null, null, null);
    }

    public static ObserveResponse success(TimestampedLwM2mNode content) {
        return new ObserveResponse(ResponseCode.CONTENT, null, content, null, null, null);
    }

    public static ObserveResponse success(List<TimestampedLwM2mNode> timestampedValues) {
        return new ObserveResponse(ResponseCode.CONTENT, null, null, timestampedValues, null, null);
    }

    public static ObserveResponse badRequest(String errorMessage) {
        return new ObserveResponse(ResponseCode.BAD_REQUEST, null, null, null, null, errorMessage);
    }

    public static ObserveResponse notFound() {
        return new ObserveResponse(ResponseCode.NOT_FOUND, null, null, null, null, null);
    }

    public static ObserveResponse unauthorized() {
        return new ObserveResponse(ResponseCode.UNAUTHORIZED, null, null, null, null, null);
    }

    public static ObserveResponse methodNotAllowed() {
        return new ObserveResponse(ResponseCode.METHOD_NOT_ALLOWED, null, null, null, null, null);
    }

    public static ObserveResponse notAcceptable() {
        return new ObserveResponse(ResponseCode.NOT_ACCEPTABLE, null, null, null, null, null);
    }

    public static ObserveResponse internalServerError(String errorMessage) {
        return new ObserveResponse(ResponseCode.INTERNAL_SERVER_ERROR, null, null, null, null, errorMessage);
    }
}
