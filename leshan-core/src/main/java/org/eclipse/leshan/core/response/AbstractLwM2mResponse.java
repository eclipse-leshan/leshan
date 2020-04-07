/*******************************************************************************
 * Copyright (c) 2015 Sierra Wireless and others.
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
import org.eclipse.leshan.core.request.exception.InvalidResponseException;

/**
 * A base class for concrete LWM2M response.
 */
public abstract class AbstractLwM2mResponse implements LwM2mResponse {

    protected final ResponseCode code;
    protected final String errorMessage;
    private final Object coapResponse;

    public AbstractLwM2mResponse(ResponseCode code, String errorMessage, Object coapResponse) {
        if (code == null)
            throw new InvalidResponseException("response code is mandatory");
        if (errorMessage != null && !code.isError())
            throw new InvalidResponseException("Only error response could have an error message");
        this.code = code;
        this.errorMessage = errorMessage;
        this.coapResponse = coapResponse;
    }

    @Override
    public final ResponseCode getCode() {
        return this.code;
    }

    @Override
    public Object getCoapResponse() {
        return coapResponse;
    }

    @Override
    public String getErrorMessage() {
        return errorMessage;
    }

    @Override
    public boolean isFailure() {
        return !isSuccess();
    }
}
