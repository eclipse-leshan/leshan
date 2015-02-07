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
import org.eclipse.leshan.util.Validate;

/**
 * A response to a server request.
 */
public class LwM2mResponse {

    protected final ResponseCode code;

    public LwM2mResponse(final ResponseCode code) {
        Validate.notNull(code);
        this.code = code;
    }

    /**
     * Gets the response code.
     *
     * @return the code
     */
    public final ResponseCode getCode() {
        return this.code;
    }

    @Override
    public String toString() {
        return String.format("LwM2mResponse [code=%s]", code);
    }
}
