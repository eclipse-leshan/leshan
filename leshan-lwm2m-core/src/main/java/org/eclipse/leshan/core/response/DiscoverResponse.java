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
 *     Michał Wadowski (Orange) - Improved compliance with rfc6690
 *******************************************************************************/
package org.eclipse.leshan.core.response;

import java.util.Arrays;

import org.eclipse.leshan.core.ResponseCode;
import org.eclipse.leshan.core.link.lwm2m.LwM2mLink;
import org.eclipse.leshan.core.request.exception.InvalidResponseException;

public class DiscoverResponse extends AbstractLwM2mResponse {

    private final LwM2mLink[] links;

    public DiscoverResponse(ResponseCode code, LwM2mLink[] links, String errorMessage) {
        this(code, links, errorMessage, null);
    }

    public DiscoverResponse(ResponseCode code, LwM2mLink[] links, String errorMessage, Object coapResponse) {
        super(code, errorMessage, coapResponse);
        if (ResponseCode.CONTENT.equals(code)) {
            if (links == null)
                throw new InvalidResponseException("links is mandatory for successful response");
            this.links = Arrays.copyOf(links, links.length);
        } else {
            this.links = null;
        }
    }

    /**
     * Get the list of {@link LwM2mLink} returned as response payload.
     *
     * @return the object links or <code>null</code> if the client returned an error response.
     */
    public LwM2mLink[] getObjectLinks() {
        return links != null ? Arrays.copyOf(links, links.length) : null;
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
        case ResponseCode.INTERNAL_SERVER_ERROR_CODE:
            return true;
        default:
            return false;
        }
    }

    @Override
    public String toString() {
        if (errorMessage != null)
            return String.format("DiscoverResponse [code=%s, errormessage=%s]", code, errorMessage);
        else
            return String.format("DiscoverResponse [code=%s, content=%s]", code, Arrays.toString(links));
    }

    // Syntactic sugar static constructors :

    public static DiscoverResponse success(LwM2mLink[] links) {
        return new DiscoverResponse(ResponseCode.CONTENT, links, null);
    }

    public static DiscoverResponse badRequest(String errorMessage) {
        return new DiscoverResponse(ResponseCode.BAD_REQUEST, null, errorMessage);
    }

    public static DiscoverResponse notFound() {
        return new DiscoverResponse(ResponseCode.NOT_FOUND, null, null);
    }

    public static DiscoverResponse unauthorized() {
        return new DiscoverResponse(ResponseCode.UNAUTHORIZED, null, null);
    }

    public static DiscoverResponse methodNotAllowed() {
        return new DiscoverResponse(ResponseCode.METHOD_NOT_ALLOWED, null, null);
    }

    public static DiscoverResponse internalServerError(String errorMessage) {
        return new DiscoverResponse(ResponseCode.INTERNAL_SERVER_ERROR, null, errorMessage);
    }
}
