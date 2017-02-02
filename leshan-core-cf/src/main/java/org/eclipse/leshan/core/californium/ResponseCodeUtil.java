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
import org.eclipse.leshan.ResponseCode;
import org.eclipse.leshan.util.Validate;

public class ResponseCodeUtil {

    public static ResponseCode fromCoapCode(int code) {
        Validate.notNull(code);

        if (code == CoAP.ResponseCode.CREATED.value) {
            return ResponseCode.CREATED;
        } else if (code == CoAP.ResponseCode.DELETED.value) {
            return ResponseCode.DELETED;
        } else if (code == CoAP.ResponseCode.CHANGED.value) {
            return ResponseCode.CHANGED;
        } else if (code == CoAP.ResponseCode.CONTENT.value) {
            return ResponseCode.CONTENT;
        } else if (code == CoAP.ResponseCode.BAD_REQUEST.value) {
            return ResponseCode.BAD_REQUEST;
        } else if (code == CoAP.ResponseCode.UNAUTHORIZED.value) {
            return ResponseCode.UNAUTHORIZED;
        } else if (code == CoAP.ResponseCode.NOT_FOUND.value) {
            return ResponseCode.NOT_FOUND;
        } else if (code == CoAP.ResponseCode.METHOD_NOT_ALLOWED.value) {
            return ResponseCode.METHOD_NOT_ALLOWED;
        } else if (code == CoAP.ResponseCode.FORBIDDEN.value) {
            return ResponseCode.FORBIDDEN;
        } else if (code == CoAP.ResponseCode.UNSUPPORTED_CONTENT_FORMAT.value) {
            return ResponseCode.UNSUPPORTED_CONTENT_FORMAT;
        } else if (code == CoAP.ResponseCode.NOT_ACCEPTABLE.value) {
            return ResponseCode.NOT_ACCEPTABLE;
        } else if (code == CoAP.ResponseCode.INTERNAL_SERVER_ERROR.value) {
            return ResponseCode.INTERNAL_SERVER_ERROR;
        } else {
            throw new IllegalArgumentException("Invalid CoAP code for LWM2M response: " + code);
        }
    }

    public static org.eclipse.californium.core.coap.CoAP.ResponseCode fromLwM2mCode(ResponseCode code) {
        Validate.notNull(code);

        switch (code) {
        case CREATED:
            return org.eclipse.californium.core.coap.CoAP.ResponseCode.CREATED;
        case DELETED:
            return org.eclipse.californium.core.coap.CoAP.ResponseCode.DELETED;
        case CHANGED:
            return org.eclipse.californium.core.coap.CoAP.ResponseCode.CHANGED;
        case CONTENT:
            return org.eclipse.californium.core.coap.CoAP.ResponseCode.CONTENT;
        case BAD_REQUEST:
            return org.eclipse.californium.core.coap.CoAP.ResponseCode.BAD_REQUEST;
        case UNAUTHORIZED:
            return org.eclipse.californium.core.coap.CoAP.ResponseCode.UNAUTHORIZED;
        case NOT_FOUND:
            return org.eclipse.californium.core.coap.CoAP.ResponseCode.NOT_FOUND;
        case METHOD_NOT_ALLOWED:
            return org.eclipse.californium.core.coap.CoAP.ResponseCode.METHOD_NOT_ALLOWED;
        case FORBIDDEN:
            return org.eclipse.californium.core.coap.CoAP.ResponseCode.FORBIDDEN;
        case INTERNAL_SERVER_ERROR:
            return org.eclipse.californium.core.coap.CoAP.ResponseCode.INTERNAL_SERVER_ERROR;
        default:
            throw new IllegalArgumentException("Invalid CoAP code for LWM2M response: " + code);
        }
    }
}
