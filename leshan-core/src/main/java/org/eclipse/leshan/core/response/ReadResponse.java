/*******************************************************************************
 * Copyright (c) 2013-2015 Sierra Wireless and others.
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v1.0 which accompany this distribution.
 * 
 * The Eclipse Public License is available at
 *    http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 *    http://www.eclipse.org/org/documents/edl-v10.html.
 * 
 * Contributors:
 *     Sierra Wireless - initial API and implementation
 *******************************************************************************/
package org.eclipse.leshan.core.response;

import org.eclipse.leshan.ResponseCode;
import org.eclipse.leshan.core.node.LwM2mNode;
import org.eclipse.leshan.util.Validate;

public class ReadResponse extends AbstractLwM2mResponse {

    protected final LwM2mNode content;

    public ReadResponse(ResponseCode code, LwM2mNode content, String errorMessage) {
        super(code, errorMessage);

        if (ResponseCode.CONTENT.equals(code)) {
            Validate.notNull(content);
        }
        this.content = content;
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
        return new ReadResponse(ResponseCode.CONTENT, content, null);
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

    public static ReadResponse internalServerError(String errorMessage) {
        return new ReadResponse(ResponseCode.INTERNAL_SERVER_ERROR, null, errorMessage);
    }
}
