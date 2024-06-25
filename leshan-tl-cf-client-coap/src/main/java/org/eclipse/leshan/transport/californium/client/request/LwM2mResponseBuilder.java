/*******************************************************************************
 * Copyright (c) 2013-2015 Sierra Wireless and others.
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
 *     Zebra Technologies - initial API and implementation
 *******************************************************************************/
package org.eclipse.leshan.client.californium.request;

import static org.eclipse.leshan.core.californium.ResponseCodeUtil.toLwM2mResponseCode;

import org.eclipse.californium.core.coap.Response;
import org.eclipse.leshan.core.request.BootstrapRequest;
import org.eclipse.leshan.core.request.DeregisterRequest;
import org.eclipse.leshan.core.request.LwM2mRequest;
import org.eclipse.leshan.core.request.RegisterRequest;
import org.eclipse.leshan.core.request.SendRequest;
import org.eclipse.leshan.core.request.UpdateRequest;
import org.eclipse.leshan.core.request.UplinkRequestVisitor;
import org.eclipse.leshan.core.request.exception.InvalidResponseException;
import org.eclipse.leshan.core.response.BootstrapResponse;
import org.eclipse.leshan.core.response.DeregisterResponse;
import org.eclipse.leshan.core.response.LwM2mResponse;
import org.eclipse.leshan.core.response.RegisterResponse;
import org.eclipse.leshan.core.response.SendResponse;
import org.eclipse.leshan.core.response.UpdateResponse;

/**
 * This class is able to create a {@link LwM2mResponse} from a CoAP {@link Response}.
 * <p>
 * Call <code>LwM2mClientResponseBuilder#visit(coapResponse)</code>, then get the result using {@link #getResponse()}
 *
 * @param <T> the type of the response to build.
 */
public class LwM2mResponseBuilder<T extends LwM2mResponse> implements UplinkRequestVisitor {

    protected final Response coapResponse;

    protected LwM2mResponse lwM2mresponse;

    public LwM2mResponseBuilder(Response coapResponse) {
        this.coapResponse = coapResponse;
    }

    @Override
    public void visit(RegisterRequest request) {
        if (coapResponse.isError()) {
            // handle error response:
            lwM2mresponse = new RegisterResponse(toLwM2mResponseCode(coapResponse.getCode()), null,
                    coapResponse.getPayloadString());
        } else if (coapResponse.getCode() == org.eclipse.californium.core.coap.CoAP.ResponseCode.CREATED) {
            // handle success response:
            lwM2mresponse = RegisterResponse.success(coapResponse.getOptions().getLocationString());
        } else {
            // handle unexpected response:
            handleUnexpectedResponseCode(request, coapResponse);
        }
    }

    @Override
    public void visit(DeregisterRequest request) {
        if (coapResponse.isError()) {
            // handle error response:
            lwM2mresponse = new DeregisterResponse(toLwM2mResponseCode(coapResponse.getCode()),
                    coapResponse.getPayloadString());
        } else if (coapResponse.getCode() == org.eclipse.californium.core.coap.CoAP.ResponseCode.DELETED) {
            // handle success response:
            lwM2mresponse = DeregisterResponse.success();
        } else {
            // handle unexpected response:
            handleUnexpectedResponseCode(request, coapResponse);
        }
    }

    @Override
    public void visit(UpdateRequest request) {
        if (coapResponse.isError()) {
            // handle error response:
            lwM2mresponse = new UpdateResponse(toLwM2mResponseCode(coapResponse.getCode()),
                    coapResponse.getPayloadString());
        } else if (coapResponse.getCode() == org.eclipse.californium.core.coap.CoAP.ResponseCode.CHANGED) {
            // handle success response:
            lwM2mresponse = UpdateResponse.success();
        } else {
            // handle unexpected response:
            handleUnexpectedResponseCode(request, coapResponse);
        }
    }

    @Override
    public void visit(SendRequest request) {
        if (coapResponse.isError()) {
            // handle error response:
            lwM2mresponse = new SendResponse(toLwM2mResponseCode(coapResponse.getCode()),
                    coapResponse.getPayloadString());
        } else if (coapResponse.getCode() == org.eclipse.californium.core.coap.CoAP.ResponseCode.CHANGED) {
            // handle success response:
            lwM2mresponse = SendResponse.success();
        } else {
            // handle unexpected response:
            handleUnexpectedResponseCode(request, coapResponse);
        }
    }

    @Override
    public void visit(BootstrapRequest request) {
        if (coapResponse.isError()) {
            // handle error response:
            lwM2mresponse = new BootstrapResponse(toLwM2mResponseCode(coapResponse.getCode()),
                    coapResponse.getPayloadString());
        } else if (coapResponse.getCode() == org.eclipse.californium.core.coap.CoAP.ResponseCode.CHANGED) {
            // handle success response:
            lwM2mresponse = BootstrapResponse.success();
        } else {
            // handle unexpected response:
            handleUnexpectedResponseCode(request, coapResponse);
        }
    }

    @SuppressWarnings("unchecked")
    public T getResponse() {
        return (T) lwM2mresponse;
    }

    protected void handleUnexpectedResponseCode(LwM2mRequest<?> request, Response coapResponse) {
        throw new InvalidResponseException("Server returned unexpected response code [%s] for [%s]",
                coapResponse.getCode(), request);
    }
}
