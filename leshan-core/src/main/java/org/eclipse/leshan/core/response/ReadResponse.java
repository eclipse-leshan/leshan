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
import org.eclipse.leshan.core.node.LwM2mMultipleResource;
import org.eclipse.leshan.core.node.LwM2mNode;
import org.eclipse.leshan.core.node.LwM2mSingleResource;
import org.eclipse.leshan.core.node.ObjectLink;
import org.eclipse.leshan.core.request.exception.InvalidResponseException;
import org.eclipse.leshan.core.util.datatype.ULong;

public class ReadResponse extends AbstractLwM2mResponse {

    protected final LwM2mNode content;

    public ReadResponse(ResponseCode code, LwM2mNode content, String errorMessage) {
        this(code, content, errorMessage, null);
    }

    public ReadResponse(ResponseCode code, LwM2mNode content, String errorMessage, Object coapResponse) {
        super(code, errorMessage, coapResponse);

        if (ResponseCode.CONTENT.equals(code)) {
            if (content == null)
                throw new InvalidResponseException("Content is mandatory for successful response");
        }
        this.content = content;
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
    public LwM2mNode getContent() {
        return content;
    }

    @Override
    public String toString() {
        if (errorMessage != null)
            return String.format("ReadResponse [code=%s, errormessage=%s]", code, errorMessage);
        else
            return String.format("ReadResponse [code=%s, content=%s]", code, content);
    }

    // Syntactic sugar static constructors :

    public static ReadResponse success(LwM2mNode content) {
        return new ReadResponse(ResponseCode.CONTENT, content, null, null);
    }

    public static ReadResponse success(int resourceId, String value) {
        return new ReadResponse(ResponseCode.CONTENT, LwM2mSingleResource.newStringResource(resourceId, value), null);
    }

    public static ReadResponse success(int resourceId, Date value) {
        return new ReadResponse(ResponseCode.CONTENT, LwM2mSingleResource.newDateResource(resourceId, value), null);
    }

    public static ReadResponse success(int resourceId, long value) {
        return new ReadResponse(ResponseCode.CONTENT, LwM2mSingleResource.newIntegerResource(resourceId, value), null);
    }

    public static ReadResponse success(int resourceId, ULong value) {
        return new ReadResponse(ResponseCode.CONTENT, LwM2mSingleResource.newUnsignedIntegerResource(resourceId, value),
                null);
    }

    public static ReadResponse success(int resourceId, ObjectLink value) {
        return new ReadResponse(ResponseCode.CONTENT, LwM2mSingleResource.newObjectLinkResource(resourceId, value),
                null);
    }

    public static ReadResponse success(int resourceId, double value) {
        return new ReadResponse(ResponseCode.CONTENT, LwM2mSingleResource.newFloatResource(resourceId, value), null);
    }

    public static ReadResponse success(int resourceId, boolean value) {
        return new ReadResponse(ResponseCode.CONTENT, LwM2mSingleResource.newBooleanResource(resourceId, value), null);
    }

    public static ReadResponse success(int resourceId, byte[] value) {
        return new ReadResponse(ResponseCode.CONTENT, LwM2mSingleResource.newBinaryResource(resourceId, value), null);
    }

    public static ReadResponse success(int resourceId, Map<Integer, ?> value, Type type) {
        return new ReadResponse(ResponseCode.CONTENT, LwM2mMultipleResource.newResource(resourceId, value, type), null);
    }

    public static ReadResponse notFound() {
        return new ReadResponse(ResponseCode.NOT_FOUND, null, null);
    }

    public static ReadResponse unauthorized() {
        return new ReadResponse(ResponseCode.UNAUTHORIZED, null, null);
    }

    public static ReadResponse methodNotAllowed() {
        return new ReadResponse(ResponseCode.METHOD_NOT_ALLOWED, null, null);
    }

    public static ReadResponse notAcceptable() {
        return new ReadResponse(ResponseCode.NOT_ACCEPTABLE, null, null);
    }

    public static ReadResponse badRequest(String errorMessage) {
        return new ReadResponse(ResponseCode.BAD_REQUEST, null, errorMessage);
    }

    public static ReadResponse internalServerError(String errorMessage) {
        return new ReadResponse(ResponseCode.INTERNAL_SERVER_ERROR, null, errorMessage);
    }
}
