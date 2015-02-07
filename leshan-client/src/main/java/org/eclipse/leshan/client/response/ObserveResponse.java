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

public class ObserveResponse extends BaseLwM2mResponse {

    private ObserveResponse(final ResponseCode code, final byte[] payload) {
        super(code, payload);
    }

    public static ObserveResponse notifyWithContent(final byte[] payload) {
        return new ObserveResponse(ResponseCode.CHANGED, payload);
    }

}
