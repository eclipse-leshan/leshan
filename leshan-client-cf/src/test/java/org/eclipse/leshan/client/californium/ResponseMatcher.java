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
package org.eclipse.leshan.client.californium;

import java.util.Arrays;

import org.eclipse.californium.core.coap.CoAP.ResponseCode;
import org.eclipse.californium.core.coap.Response;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;

public class ResponseMatcher extends BaseMatcher<Response> {

    private final ResponseCode code;
    private final byte[] payload;

    public ResponseMatcher(final ResponseCode code, final byte[] payload) {
        this.code = code;
        this.payload = payload;
    }

    @Override
    public boolean matches(final Object arg0) {
        final ResponseCode responseCode = ResponseCode.valueOf(((Response) arg0).getCode().value);
        return responseCode == code && Arrays.equals(payload, ((Response) arg0).getPayload());
    }

    @Override
    public void describeTo(final Description arg0) {
    }

}