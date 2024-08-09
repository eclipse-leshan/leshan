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

import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.eclipse.leshan.core.ResponseCode;
import org.eclipse.leshan.core.node.LwM2mNode;
import org.eclipse.leshan.core.node.LwM2mPath;
import org.eclipse.leshan.core.node.TimestampedLwM2mNodes;

public class ReadCompositeResponse extends AbstractLwM2mResponse {

    protected final Map<LwM2mPath, LwM2mNode> content;

    protected final TimestampedLwM2mNodes timestampedValues;

    public ReadCompositeResponse(ResponseCode code, Map<LwM2mPath, LwM2mNode> content,
            TimestampedLwM2mNodes timestampedValues, String errorMessage, Object coapResponse) {
        super(code, errorMessage, coapResponse);

        Map<LwM2mPath, LwM2mNode> responseContent;
        TimestampedLwM2mNodes responsetimestampedValues;

        if (timestampedValues != null) {
            // handle if timestamped value is passed
            if (content != null) {
                throw new IllegalArgumentException("content OR timestampedValue should be passed but not both");
            }
            // store value if all timestamps in timestampedValues are null
            if (!timestampedValues.getNodes().isEmpty()
                    && timestampedValues.getTimestamps().stream().noneMatch(Objects::nonNull)) {

                responseContent = timestampedValues.getNodes();
                responsetimestampedValues = null;
            } else {
                // check if we have only timestamp in timestampedValues
                if (timestampedValues.getTimestamps().stream()
                        .collect(Collectors.groupingBy(Function.identity(), Collectors.counting())).size() >= 2) {
                    throw new IllegalArgumentException("only one timestamp in the content is allowed");
                }

                responseContent = null;
                responsetimestampedValues = timestampedValues;
            }
        } else {
            // handle if content (not timestamped) value is passed
            responsetimestampedValues = null;
            responseContent = content;
        }

        this.content = responseContent;
        this.timestampedValues = responsetimestampedValues;

    }

    public Map<LwM2mPath, LwM2mNode> getContent() {
        return content;
    }

    public LwM2mNode getContent(String path) {
        return content.get(new LwM2mPath(path));
    }

    public TimestampedLwM2mNodes getTimestampedLwM2mNodes() {
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
