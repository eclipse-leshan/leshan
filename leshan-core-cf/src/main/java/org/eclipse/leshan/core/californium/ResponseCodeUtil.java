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

import org.eclipse.californium.core.coap.CoAP;
import org.eclipse.californium.core.coap.MessageFormatException;
import org.eclipse.leshan.core.ResponseCode;
import org.eclipse.leshan.core.util.Validate;

public class ResponseCodeUtil {

    public static int toLwM2mCode(org.eclipse.californium.core.coap.CoAP.ResponseCode coapResponseCode) {
        return coapResponseCode.codeClass * 100 + coapResponseCode.codeDetail;
    }

    public static ResponseCode toLwM2mResponseCode(
            org.eclipse.californium.core.coap.CoAP.ResponseCode coapResponseCode) {
        return ResponseCode.fromCode(toLwM2mCode(coapResponseCode));
    }

    public static int toLwM2mCode(int coapCode) {
        int codeClass = CoAP.getCodeClass(coapCode);
        int codeDetail = CoAP.getCodeDetail(coapCode);
        return codeClass * 100 + codeDetail;
    }

    public static ResponseCode toLwM2mResponseCode(int coapCode) {
        return ResponseCode.fromCode(toLwM2mCode(coapCode));
    }

    public static int toCoapCode(int lwm2mCode) {
        int codeClass = lwm2mCode / 100;
        int codeDetail = lwm2mCode % 100;
        if (codeClass > 7 || codeDetail > 31)
            throw new IllegalArgumentException("Could not be translated into a valid COAP code");

        return codeClass << 5 | codeDetail;
    }

    public static org.eclipse.californium.core.coap.CoAP.ResponseCode toCoapResponseCode(
            ResponseCode Lwm2mResponseCode) {
        Validate.notNull(Lwm2mResponseCode);
        try {
            return org.eclipse.californium.core.coap.CoAP.ResponseCode.valueOf(toCoapCode(Lwm2mResponseCode.getCode()));
        } catch (MessageFormatException e) {
            throw new IllegalArgumentException("Invalid CoAP code for LWM2M response: " + Lwm2mResponseCode);
        }
    }
}
