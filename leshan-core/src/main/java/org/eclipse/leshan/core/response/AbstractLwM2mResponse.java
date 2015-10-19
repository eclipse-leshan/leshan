/*******************************************************************************
 * Copyright (c) 2015 Sierra Wireless and others.
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
 * A base class for concrete LWM2M response.
 */
public class AbstractLwM2mResponse implements LwM2mResponse {

    protected final ResponseCode code;
    protected final String errorMessage;

    public AbstractLwM2mResponse(final ResponseCode code, final String errorMessage) {
        Validate.notNull(code);
        if (errorMessage != null)
            Validate.isTrue(code.isError(), "Only error response could have an error message.");
        this.code = code;
        this.errorMessage = errorMessage;
    }

    @Override
    public final ResponseCode getCode() {
        return this.code;
    }

    @Override
    public String getErrorMessage() {
        return errorMessage;
    }
}
