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

import java.util.Date;
import java.util.Map;

import org.eclipse.leshan.core.ResponseCode;
import org.eclipse.leshan.core.model.ResourceModel.Type;
import org.eclipse.leshan.core.node.LwM2mChildNode;
import org.eclipse.leshan.core.node.LwM2mMultipleResource;
import org.eclipse.leshan.core.node.LwM2mNode;
import org.eclipse.leshan.core.node.LwM2mSingleResource;
import org.eclipse.leshan.core.node.ObjectLink;
import org.eclipse.leshan.core.node.TimestampedLwM2mNode;
import org.eclipse.leshan.core.request.exception.InvalidResponseException;
import org.eclipse.leshan.core.util.datatype.ULong;

public class ReadResponse extends AbstractLwM2mResponse {

    protected final LwM2mChildNode content;

    protected final TimestampedLwM2mNode timestampedValue;

    public ReadResponse(ResponseCode code, LwM2mNode content, TimestampedLwM2mNode timestampedValue,
            String errorMessage) {
        this(code, content, timestampedValue, errorMessage, null);
    }

    public ReadResponse(ResponseCode code, LwM2mNode content, TimestampedLwM2mNode timestampedValue,
            String errorMessage, Object coapResponse) {
        super(code, errorMessage, coapResponse);

        LwM2mNode responseContent;
        if (timestampedValue != null) {
            // handle if timestamped value is passed
            if (content != null) {
                throw new IllegalArgumentException("content OR timestampedValue should be passed but not both");
            }

            // store value
            if (timestampedValue.isTimestamped()) {
                this.timestampedValue = timestampedValue;
            } else {
                this.timestampedValue = null;
            }
            responseContent = timestampedValue.getNode();
        } else {
            // handle if content (not timestamped) value is passed
            this.timestampedValue = null;
            responseContent = content;
        }

        if (ResponseCode.CONTENT.equals(code)) {
            if (responseContent == null)
                throw new InvalidResponseException("Content is mandatory for successful response");

            if (!(responseContent instanceof LwM2mChildNode))
                throw new InvalidResponseException("Invalid Content : node should be a LwM2mChildNode not a %s",
                        responseContent.getClass().getSimpleName());
        }
        this.content = (LwM2mChildNode) responseContent;

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
            return true;
        default:
            return false;
        }
    }

    /**
     * Get the {@link LwM2mNode} value returned as response payload.
     *
     * @return the value or <code>null</code> if the client returned an error response.
     */
    public LwM2mChildNode getContent() {
        return content;
    }

    /**
     * Get the {@link TimestampedLwM2mNode} value returned as response payload or <code>null</code> if the value is not
     * timestamped, in that case you should use {@link #getContent()} instead.
     *
     * @return the value or <code>null</code> if the value is not timestamped OR if this is an error response.
     */
    public TimestampedLwM2mNode getTimestampedLwM2mNode() {
        return timestampedValue;
    }

    @Override
    public String toString() {
        if (errorMessage != null)
            return String.format("ReadResponse [code=%s, errormessage=%s]", code, errorMessage);
        else if (timestampedValue != null)
            return String.format("ReadResponse [code=%s, timestampedValues= %s]", code, timestampedValue);
        else
            return String.format("ReadResponse [code=%s, content=%s]", code, content);

    }

    // Syntactic sugar static constructors :

    public static ReadResponse success(LwM2mNode content) {
        return new ReadResponse(ResponseCode.CONTENT, content, null, null);
    }

    public static ReadResponse success(TimestampedLwM2mNode timestampedValue) {
        return new ReadResponse(ResponseCode.CONTENT, null, timestampedValue, null, null);
    }

    public static ReadResponse success(int resourceId, String value) {
        return new ReadResponse(ResponseCode.CONTENT, LwM2mSingleResource.newStringResource(resourceId, value), null,
                null);
    }

    public static ReadResponse success(int resourceId, Date value) {
        return new ReadResponse(ResponseCode.CONTENT, LwM2mSingleResource.newDateResource(resourceId, value), null,
                null);
    }

    public static ReadResponse success(int resourceId, long value) {
        return new ReadResponse(ResponseCode.CONTENT, LwM2mSingleResource.newIntegerResource(resourceId, value), null,
                null);
    }

    public static ReadResponse success(int resourceId, ULong value) {
        return new ReadResponse(ResponseCode.CONTENT, LwM2mSingleResource.newUnsignedIntegerResource(resourceId, value),
                null, null);
    }

    public static ReadResponse success(int resourceId, ObjectLink value) {
        return new ReadResponse(ResponseCode.CONTENT, LwM2mSingleResource.newObjectLinkResource(resourceId, value),
                null, null);
    }

    public static ReadResponse success(int resourceId, double value) {
        return new ReadResponse(ResponseCode.CONTENT, LwM2mSingleResource.newFloatResource(resourceId, value), null,
                null);
    }

    public static ReadResponse success(int resourceId, boolean value) {
        return new ReadResponse(ResponseCode.CONTENT, LwM2mSingleResource.newBooleanResource(resourceId, value), null,
                null);
    }

    public static ReadResponse success(int resourceId, byte[] value) {
        return new ReadResponse(ResponseCode.CONTENT, LwM2mSingleResource.newBinaryResource(resourceId, value), null,
                null);
    }

    public static ReadResponse success(int resourceId, Map<Integer, ?> value, Type type) {
        return new ReadResponse(ResponseCode.CONTENT, LwM2mMultipleResource.newResource(resourceId, value, type), null,
                null);
    }

    public static ReadResponse notFound() {
        return new ReadResponse(ResponseCode.NOT_FOUND, null, null, null);
    }

    public static ReadResponse unauthorized() {
        return new ReadResponse(ResponseCode.UNAUTHORIZED, null, null, null);
    }

    public static ReadResponse methodNotAllowed() {
        return new ReadResponse(ResponseCode.METHOD_NOT_ALLOWED, null, null, null);
    }

    public static ReadResponse notAcceptable() {
        return new ReadResponse(ResponseCode.NOT_ACCEPTABLE, null, null, null);
    }

    public static ReadResponse badRequest(String errorMessage) {
        return new ReadResponse(ResponseCode.BAD_REQUEST, null, null, errorMessage);
    }

    public static ReadResponse internalServerError(String errorMessage) {
        return new ReadResponse(ResponseCode.INTERNAL_SERVER_ERROR, null, null, errorMessage);
    }
}
