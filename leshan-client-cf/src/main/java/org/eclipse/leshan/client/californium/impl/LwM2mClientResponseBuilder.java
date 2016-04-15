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
package org.eclipse.leshan.client.californium.impl;

import org.eclipse.californium.core.coap.CoAP;
import org.eclipse.californium.core.coap.Request;
import org.eclipse.californium.core.coap.Response;
import org.eclipse.leshan.ResponseCode;
import org.eclipse.leshan.core.request.BootstrapRequest;
import org.eclipse.leshan.core.request.DeregisterRequest;
import org.eclipse.leshan.core.request.RegisterRequest;
import org.eclipse.leshan.core.request.UpdateRequest;
import org.eclipse.leshan.core.request.UplinkRequestVisitor;
import org.eclipse.leshan.core.request.exception.ResourceAccessException;
import org.eclipse.leshan.core.response.BootstrapResponse;
import org.eclipse.leshan.core.response.DeregisterResponse;
import org.eclipse.leshan.core.response.LwM2mResponse;
import org.eclipse.leshan.core.response.RegisterResponse;
import org.eclipse.leshan.core.response.UpdateResponse;
import org.eclipse.leshan.util.Validate;

public class LwM2mClientResponseBuilder<T extends LwM2mResponse> implements UplinkRequestVisitor {

    // private static final Logger LOG = LoggerFactory.getLogger(LwM2mClientResponseBuilder.class);

    private final Request coapRequest;
    private final Response coapResponse;

    private LwM2mResponse lwM2mresponse;

    // TODO leshan-code-cf: this code should be factorize in a leshan-core-cf project.
    // duplicate from org.eclipse.leshan.server.californium.impl.LwM2mResponseBuilder<T>
    public static ResponseCode fromCoapCode(final int code) {
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

    public LwM2mClientResponseBuilder(final Request coapRequest, final Response coapResponse) {
        this.coapRequest = coapRequest;
        this.coapResponse = coapResponse;
    }

    @Override
    public void visit(final RegisterRequest request) {
        switch (coapResponse.getCode()) {
        case CREATED:
            lwM2mresponse = RegisterResponse.success(coapResponse.getOptions().getLocationString());
            break;
        case BAD_REQUEST:
        case FORBIDDEN:
        case INTERNAL_SERVER_ERROR:
            lwM2mresponse = new RegisterResponse(fromCoapCode(coapResponse.getCode().value), null,
                    coapResponse.getPayloadString());
            break;
        default:
            handleUnexpectedResponseCode(coapRequest, coapResponse);
        }
    }

    @Override
    public void visit(final DeregisterRequest request) {
        switch (coapResponse.getCode()) {
        case DELETED:
            lwM2mresponse = DeregisterResponse.success();
            break;
        case BAD_REQUEST:
        case NOT_FOUND:
        case INTERNAL_SERVER_ERROR:
            lwM2mresponse = new DeregisterResponse(fromCoapCode(coapResponse.getCode().value),
                    coapResponse.getPayloadString());
            break;
        default:
            handleUnexpectedResponseCode(coapRequest, coapResponse);
        }
    }

    @Override
    public void visit(final UpdateRequest request) {
        switch (coapResponse.getCode()) {
        case CHANGED:
            lwM2mresponse = UpdateResponse.success();
            break;
        case BAD_REQUEST:
        case NOT_FOUND:
        case INTERNAL_SERVER_ERROR:
            lwM2mresponse = new UpdateResponse(fromCoapCode(coapResponse.getCode().value),
                    coapResponse.getPayloadString());
            break;
        default:
            handleUnexpectedResponseCode(coapRequest, coapResponse);
        }
    }

    @Override
    public void visit(final BootstrapRequest request) {
        switch (coapResponse.getCode()) {
        case CHANGED:
            lwM2mresponse = BootstrapResponse.success();
            break;
        case BAD_REQUEST:
        case INTERNAL_SERVER_ERROR:
            lwM2mresponse = new BootstrapResponse(fromCoapCode(coapResponse.getCode().value),
                    coapResponse.getPayloadString());
            break;
        default:
            handleUnexpectedResponseCode(coapRequest, coapResponse);
        }
    }

    @SuppressWarnings("unchecked")
    public T getResponse() {
        return (T) lwM2mresponse;
    }

    /**
     * Throws a generic {@link ResourceAccessException} indicating that the server returned an unexpected response code.
     *
     * @param coapRequest
     * @param coapResponse
     */
    private void handleUnexpectedResponseCode(final Request coapRequest, final Response coapResponse) {
        final String msg = String.format("Server returned unexpected response code [%s]", coapResponse.getCode());
        throw new ResourceAccessException(coapRequest.getURI(), msg);
    }
}
