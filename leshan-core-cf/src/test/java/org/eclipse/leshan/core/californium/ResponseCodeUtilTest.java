/*******************************************************************************
 * Copyright (c) 2017 Sierra Wireless and others.
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
package org.eclipse.leshan.core.californium;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;

import org.eclipse.californium.core.coap.CoAP.ResponseCode;
import org.junit.jupiter.api.Test;

public class ResponseCodeUtilTest {

    @Test
    public void known_coap_code_to_known_lwm2m_Code() {
        org.eclipse.leshan.core.ResponseCode lwM2mResponseCode = ResponseCodeUtil
                .toLwM2mResponseCode(ResponseCode.CREATED);

        assertEquals(org.eclipse.leshan.core.ResponseCode.CREATED, lwM2mResponseCode);
        assertEquals("CREATED(201)", lwM2mResponseCode.toString());
    }

    @Test
    public void known_coap_code_to_unknown_lwm2m_code() {
        org.eclipse.leshan.core.ResponseCode lwM2mResponseCode = ResponseCodeUtil
                .toLwM2mResponseCode(ResponseCode.GATEWAY_TIMEOUT);

        assertEquals(504, lwM2mResponseCode.getCode());
        assertEquals(org.eclipse.leshan.core.ResponseCode.UNKNOWN, lwM2mResponseCode.getName());
        assertEquals("UNKNOWN(504)", lwM2mResponseCode.toString());
    }

    @Test
    public void known_lwm2m_code_to_known_coap_code() {
        ResponseCode coapResponseCode = ResponseCodeUtil
                .toCoapResponseCode(org.eclipse.leshan.core.ResponseCode.BAD_REQUEST);

        assertEquals(ResponseCode.BAD_REQUEST, coapResponseCode);
    }

    @Test
    public void unknown_lwm2m_code_to_known_coap_code() {
        ResponseCode coapResponseCode = ResponseCodeUtil
                .toCoapResponseCode(new org.eclipse.leshan.core.ResponseCode(503));

        assertEquals(ResponseCode.SERVICE_UNAVAILABLE, coapResponseCode);
    }

    @Test()
    public void unknown_lwm2m_code_to_invalid_coap_code() {
        assertThrowsExactly(IllegalArgumentException.class, () -> {
            ResponseCodeUtil.toCoapResponseCode(new org.eclipse.leshan.core.ResponseCode(301));
        });
    }

    @Test()
    public void unknown_lwm2m_code_to_invalid_coap_code2() {
        assertThrowsExactly(IllegalArgumentException.class, () -> {
            ResponseCodeUtil.toCoapResponseCode(new org.eclipse.leshan.core.ResponseCode(441));
        });
    }

    @Test
    public void unknown_lwm2m_code_to_unknown_coap_code() {
        // californium behavior is not really consistent

        // for success : code value is lost but we know we use an unknown code
        ResponseCode coapResponseCode = ResponseCodeUtil
                .toCoapResponseCode(new org.eclipse.leshan.core.ResponseCode(206));
        assertEquals(ResponseCode._UNKNOWN_SUCCESS_CODE, coapResponseCode);

        // for client error,: unknown code is replace by BAD REQUEST ...
        coapResponseCode = ResponseCodeUtil.toCoapResponseCode(new org.eclipse.leshan.core.ResponseCode(425));
        assertEquals(ResponseCode.BAD_REQUEST, coapResponseCode);

        // for server error : unknown code is replace by INTERNAL SERVER ERROR ...
        coapResponseCode = ResponseCodeUtil.toCoapResponseCode(new org.eclipse.leshan.core.ResponseCode(509));
        assertEquals(ResponseCode.INTERNAL_SERVER_ERROR, coapResponseCode);

    }
}
