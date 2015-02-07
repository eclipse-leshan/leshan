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
 *     Zebra Technologies - initial API and implementation
 *******************************************************************************/
package org.eclipse.leshan.client.response;

import org.eclipse.leshan.ResponseCode;

public class ExecuteResponse extends BaseLwM2mResponse {

    private ExecuteResponse(final ResponseCode code) {
        super(code, new byte[0]);
    }

    public static ExecuteResponse success() {
        return new ExecuteResponse(ResponseCode.CHANGED);
    }

    // TODO Evaluate whether this needs to be used
    public static ExecuteResponse failure() {
        return new ExecuteResponse(ResponseCode.METHOD_NOT_ALLOWED);
    }

    public static ExecuteResponse notAllowed() {
        return new ExecuteResponse(ResponseCode.METHOD_NOT_ALLOWED);
    }

}
