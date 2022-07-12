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

import org.eclipse.leshan.core.ResponseCode;
import org.eclipse.leshan.core.node.LwM2mNode;
import org.eclipse.leshan.core.request.exception.InvalidResponseException;

public class BootstrapReadResponse extends AbstractLwM2mResponse {

    protected final LwM2mNode content;

    public BootstrapReadResponse(ResponseCode code, LwM2mNode content, String errorMessage) {
        this(code, content, errorMessage, null);
    }

    public BootstrapReadResponse(ResponseCode code, LwM2mNode content, String errorMessage, Object coapResponse) {
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
            return String.format("BootstrapReadResponse [code=%s, errormessage=%s]", code, errorMessage);
        else
            return String.format("BootstrapReadResponse [code=%s, content=%s]", code, content);
    }

    // Syntactic sugar static constructors :

    public static BootstrapReadResponse success(LwM2mNode content) {
        return new BootstrapReadResponse(ResponseCode.CONTENT, content, null, null);
    }

    public static BootstrapReadResponse notFound() {
        return new BootstrapReadResponse(ResponseCode.NOT_FOUND, null, null);
    }

    public static BootstrapReadResponse unauthorized() {
        return new BootstrapReadResponse(ResponseCode.UNAUTHORIZED, null, null);
    }

    public static BootstrapReadResponse methodNotAllowed() {
        return new BootstrapReadResponse(ResponseCode.METHOD_NOT_ALLOWED, null, null);
    }

    public static BootstrapReadResponse notAcceptable() {
        return new BootstrapReadResponse(ResponseCode.NOT_ACCEPTABLE, null, null);
    }

    public static BootstrapReadResponse badRequest(String errorMessage) {
        return new BootstrapReadResponse(ResponseCode.BAD_REQUEST, null, errorMessage);
    }

    public static BootstrapReadResponse internalServerError(String errorMessage) {
        return new BootstrapReadResponse(ResponseCode.INTERNAL_SERVER_ERROR, null, errorMessage);
    }
}
