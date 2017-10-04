/*******************************************************************************
 * Copyright (c) 2017 Sierra Wireless and others.
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
package org.eclipse.leshan.core.californium;

import org.eclipse.californium.core.coap.CoAP;
import org.eclipse.californium.core.coap.MessageFormatException;
import org.eclipse.leshan.ResponseCode;
import org.eclipse.leshan.util.Validate;

public class ResponseCodeUtil {

    public static int toLwM2mCode(org.eclipse.californium.core.coap.CoAP.ResponseCode coapResponseCode) {
        return coapResponseCode.codeClass * 100 + coapResponseCode.codeDetail;
    }

    public static int toLwM2mCode(int coapCode) {
        int codeClass = CoAP.getCodeClass(coapCode);
        int codeDetail = CoAP.getCodeDetail(coapCode);
        return codeClass * 100 + codeDetail;
    }

    public static ResponseCode fromCoapCode(int code) {
        ResponseCode lwm2mResponseCode = ResponseCode.fromCode(toLwM2mCode(code));
        if (lwm2mResponseCode == null)
            throw new IllegalArgumentException("Invalid CoAP code for LWM2M response: " + code);

        return lwm2mResponseCode;
    }

    public static int toCoapCode(int lwm2mCode) {
        int codeClass = lwm2mCode / 100;
        int codeDetail = lwm2mCode % 100;
        if (codeClass > 7 || codeDetail > 31)
            throw new IllegalArgumentException("Could not be translated into a valid COAP code");

        return codeClass << 5 | codeDetail;
    }

    public static org.eclipse.californium.core.coap.CoAP.ResponseCode fromLwM2mCode(ResponseCode code) {
        Validate.notNull(code);
        try {
            return org.eclipse.californium.core.coap.CoAP.ResponseCode.valueOf(toCoapCode(code.getCode()));
        } catch (MessageFormatException e) {
            throw new IllegalArgumentException("Invalid CoAP code for LWM2M response: " + code);
        }
    }
}
