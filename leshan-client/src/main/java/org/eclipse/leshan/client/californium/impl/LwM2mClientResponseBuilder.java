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

import java.util.logging.Logger;

import org.eclipse.californium.core.coap.CoAP.ResponseCode;
import org.eclipse.californium.core.coap.Request;
import org.eclipse.californium.core.coap.Response;
import org.eclipse.leshan.client.request.BootstrapRequest;
import org.eclipse.leshan.client.request.DeregisterRequest;
import org.eclipse.leshan.client.request.LwM2mClientRequest;
import org.eclipse.leshan.client.request.LwM2mClientRequestVisitor;
import org.eclipse.leshan.client.request.RegisterRequest;
import org.eclipse.leshan.client.request.UpdateRequest;
import org.eclipse.leshan.client.response.OperationResponse;

public class LwM2mClientResponseBuilder implements LwM2mClientRequestVisitor {
    private static final Logger LOG = Logger.getLogger(LwM2mClientResponseBuilder.class.getCanonicalName());

    private final Request coapRequest;
    private final Response coapResponse;
    private final CaliforniumLwM2mClientRequestSender californiumLwM2mClientRequestSender;
    private OperationResponse lwM2mresponse;
    private final CaliforniumClientIdentifierBuilder californiumClientIdentifierBuilder;

    public LwM2mClientResponseBuilder(final Request coapRequest, final Response coapResponse,
            final CaliforniumLwM2mClientRequestSender californiumLwM2mClientRequestSender) {
        this.coapRequest = coapRequest;
        this.coapResponse = coapResponse;
        this.californiumLwM2mClientRequestSender = californiumLwM2mClientRequestSender;
        this.californiumClientIdentifierBuilder = new CaliforniumClientIdentifierBuilder(coapResponse);
    }

    @Override
    public void visit(final RegisterRequest request) {
        buildClientIdentifier(request);
        buildResponse();
    }

    @Override
    public void visit(final DeregisterRequest request) {
        buildClientIdentifier(request);
        buildResponse();
    }

    @Override
    public void visit(final UpdateRequest request) {
        buildClientIdentifier(request);
        buildResponse();
    }

    @Override
    public void visit(final BootstrapRequest request) {
        buildClientIdentifier(request);
        buildResponse();
    }

    private void buildClientIdentifier(final LwM2mClientRequest request) {
        request.accept(californiumClientIdentifierBuilder);
    }

    private void buildResponse() {
        if (coapResponse == null) {
            lwM2mresponse = OperationResponse.failure(ResponseCode.GATEWAY_TIMEOUT, "Timed Out Waiting For Response.");
        } else if (ResponseCode.isSuccess(coapResponse.getCode())) {
            lwM2mresponse = OperationResponse
                    .of(coapResponse, californiumClientIdentifierBuilder.getClientIdentifier());
        } else {
            lwM2mresponse = OperationResponse.failure(coapResponse.getCode(), "Request Failed on Server "
                    + coapResponse.getOptions());
        }
    }

    public OperationResponse getResponse() {
        return lwM2mresponse;
    }

}
