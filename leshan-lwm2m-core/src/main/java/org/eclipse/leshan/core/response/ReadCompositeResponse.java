/*******************************************************************************
 * Copyright (c) 2021 Sierra Wireless and others.
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

import java.time.Instant;
import java.util.Map;

import org.eclipse.leshan.core.ResponseCode;
import org.eclipse.leshan.core.node.LwM2mNode;
import org.eclipse.leshan.core.node.LwM2mPath;
import org.eclipse.leshan.core.node.TimestampedLwM2mNodes;

public class ReadCompositeResponse extends AbstractLwM2mResponse {

    protected final Map<LwM2mPath, LwM2mNode> content;
    protected final TimestampedLwM2mNodes timestampedValues;

    /**
     * Generic constructor to create an {@link ReadCompositeResponse}
     *
     * On success response only one of that argument must be not <code>null</code> :
     * <ul>
     * <li>content
     * <li>timestampedContent
     * </ul>
     * errorMessage could only be null on error.
     *
     * @param responseCode The {@link ResponseCode} of the response
     * @param content If response code is success, content is all {@link LwM2mNode} requested. (else <code>null</code>)
     * @param timestampedContent If response code is success, timestampedContent is all {@link LwM2mNode} requested with
     *        1 associated timestamp.(else <code>null</code>)
     * @param errorMessage A optional error message if {@link ResponseCode} is an error. (else <code>null</code>)
     * @param coapResponse The underlying protocol response object generally when you receive the response not when you
     *        send it.
     */
    public ReadCompositeResponse(ResponseCode responseCode, Map<LwM2mPath, LwM2mNode> content,
            TimestampedLwM2mNodes timestampedContent, String errorMessage, Object coapResponse) {
        super(responseCode, errorMessage, coapResponse);

        if (timestampedContent != null) {
            // Handle if timestamped value is passed

            // check that we don't pass content too.
            if (content != null) {
                throw new IllegalArgumentException("content OR timestampedValue should be passed but not both");
            }
            // check invalid size
            if (timestampedContent.getTimestamps().size() == 0) {
                throw new IllegalArgumentException("timestampValue can not be empty");
            } else if (timestampedContent.getTimestamps().size() > 1) {
                throw new IllegalArgumentException("only one timestamp in the content is allowed");
            } else {
                // So we have only 1 timestamp value
                Instant timestamp = timestampedContent.getTimestamps().iterator().next();
                if (timestamp != null) {
                    this.timestampedValues = timestampedContent;
                    this.content = timestampedContent.getNodesAt(timestamp);

                } else {
                    this.timestampedValues = null;
                    this.content = timestampedContent.getMostRecentNodes();
                }
            }
        } else {
            // handle if content (not timestamped) value is passed
            this.timestampedValues = null;
            this.content = content;
        }

        if (ResponseCode.CONTENT.equals(code)) {
            // check content is not null
            if (this.content == null) {
                throw new IllegalArgumentException("content OR timestampedValue should be not null");
            }
        }
    }

    /**
     * Get all {@link LwM2mNode} returned as response payload.
     *
     * @return the value or <code>null</code> if the client returned an error response.
     */
    public Map<LwM2mPath, LwM2mNode> getContent() {
        return content;
    }

    /**
     * Get the {@link LwM2mNode} value for the given requested path
     *
     * @return the value or <code>null</code> if the client returned an error response.
     */
    public LwM2mNode getContent(String path) {
        if (content == null)
            return null;
        return content.get(new LwM2mPath(path));
    }

    /**
     * Get all {@link LwM2mNode} with the corresponding associated timestamp or <code>null</code> if the value is not
     * timestamped, in that case you should use {@link #getContent()} instead.
     *
     * @return the value or <code>null</code> if the value is not timestamped OR if this is an error response.
     */
    public TimestampedLwM2mNodes getTimestampedLwM2mNode() {
        return timestampedValues;
    }

    @Override
    public boolean isSuccess() {
        return getCode() == ResponseCode.CONTENT;
    }

    @Override
    public boolean isValid() {
        switch (code.getCode()) {
        case ResponseCode.CONTENT_CODE:
        case ResponseCode.BAD_REQUEST_CODE:
        case ResponseCode.UNAUTHORIZED_CODE:
        case ResponseCode.NOT_FOUND_CODE:
        case ResponseCode.METHOD_NOT_ALLOWED_CODE:
        case ResponseCode.NOT_ACCEPTABLE_CODE:
        case ResponseCode.INTERNAL_SERVER_ERROR_CODE:
        case ResponseCode.UNSUPPORTED_CONTENT_FORMAT_CODE:
            return true;
        default:
            return false;
        }
    }

    @Override
    public String toString() {
        if (errorMessage != null)
            return String.format("ReadCompositeResponse [code=%s, errormessage=%s]", code, errorMessage);
        else
            return String.format("ReadCompositeResponse [code=%s, content=%s]", code, content);
    }

    // Syntactic sugar static constructors :
    public static ReadCompositeResponse success(Map<LwM2mPath, LwM2mNode> content) {
        return new ReadCompositeResponse(ResponseCode.CONTENT, content, null, null, null);
    }

    public static ReadCompositeResponse success(TimestampedLwM2mNodes timestampedValues) {
        return new ReadCompositeResponse(ResponseCode.CONTENT, null, timestampedValues, null, null);
    }

    public static ReadCompositeResponse notFound() {
        return new ReadCompositeResponse(ResponseCode.NOT_FOUND, null, null, null, null);
    }

    public static ReadCompositeResponse unauthorized() {
        return new ReadCompositeResponse(ResponseCode.UNAUTHORIZED, null, null, null, null);
    }

    public static ReadCompositeResponse methodNotAllowed() {
        return new ReadCompositeResponse(ResponseCode.METHOD_NOT_ALLOWED, null, null, null, null);
    }

    public static ReadCompositeResponse notAcceptable() {
        return new ReadCompositeResponse(ResponseCode.NOT_ACCEPTABLE, null, null, null, null);
    }

    public static ReadCompositeResponse unsupportedContentFormat() {
        return new ReadCompositeResponse(ResponseCode.UNSUPPORTED_CONTENT_FORMAT, null, null, null, null);
    }

    public static ReadCompositeResponse badRequest(String errorMessage) {
        return new ReadCompositeResponse(ResponseCode.BAD_REQUEST, null, null, errorMessage, null);
    }

    public static ReadCompositeResponse internalServerError(String errorMessage) {
        return new ReadCompositeResponse(ResponseCode.INTERNAL_SERVER_ERROR, null, null, errorMessage, null);
    }
}
