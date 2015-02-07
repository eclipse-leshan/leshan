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

public class WriteResponse extends BaseLwM2mResponse {

    private WriteResponse(final ResponseCode code) {
        super(code, new byte[0]);
    }

    public static WriteResponse success() {
        return new WriteResponse(ResponseCode.CHANGED);
    }

    public static WriteResponse failure() {
        return new WriteResponse(ResponseCode.BAD_REQUEST);
    }

    public static WriteResponse notAllowed() {
        return new WriteResponse(ResponseCode.METHOD_NOT_ALLOWED);
    }

    public static WriteResponse badRequest() {
        return new WriteResponse(ResponseCode.BAD_REQUEST);
    }

}
