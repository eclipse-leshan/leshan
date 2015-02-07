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

public class ValueResponse extends LwM2mResponse {

    private final LwM2mNode content;

    public ValueResponse(ResponseCode code) {
        this(code, null);
    }

    public ValueResponse(ResponseCode code, LwM2mNode content) {
        super(code);

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
        return String.format("ValueResponse [content=%s, code=%s]", content, code);
    }
}
