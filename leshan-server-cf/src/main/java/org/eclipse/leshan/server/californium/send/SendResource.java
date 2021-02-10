/*******************************************************************************
 * Copyright (c) 2021 Sierra Wireless and others.
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
package org.eclipse.leshan.server.californium.send;

import static org.eclipse.leshan.core.californium.ResponseCodeUtil.toCoapResponseCode;

import java.util.List;
import java.util.Map;

import org.eclipse.californium.core.coap.CoAP.ResponseCode;
import org.eclipse.californium.core.coap.Request;
import org.eclipse.californium.core.server.resources.CoapExchange;
import org.eclipse.leshan.core.californium.LwM2mCoapResource;
import org.eclipse.leshan.core.model.LwM2mModel;
import org.eclipse.leshan.core.node.LwM2mNode;
import org.eclipse.leshan.core.node.LwM2mPath;
import org.eclipse.leshan.core.node.codec.LwM2mNodeDecoder;
import org.eclipse.leshan.core.request.ContentFormat;
import org.eclipse.leshan.core.request.Identity;
import org.eclipse.leshan.core.request.SendRequest;
import org.eclipse.leshan.core.response.SendResponse;
import org.eclipse.leshan.core.response.SendableResponse;
import org.eclipse.leshan.server.model.LwM2mModelProvider;
import org.eclipse.leshan.server.registration.Registration;
import org.eclipse.leshan.server.registration.RegistrationStore;
import org.eclipse.leshan.server.send.SendHandler;

/**
 * A CoAP Resource used to handle "Send" request sent by LWM2M devices.
 * 
 * @see SendRequest
 */
public class SendResource extends LwM2mCoapResource {
    private RegistrationStore registrationStore;
    private LwM2mNodeDecoder decoder;
    private LwM2mModelProvider modelProvider;
    private SendHandler sendHandler;

    public SendResource(SendHandler sendHandler, LwM2mModelProvider modelProvider, LwM2mNodeDecoder decoder,
            RegistrationStore registrationStore) {
        super("dp");
        this.registrationStore = registrationStore;
        this.decoder = decoder;
        this.modelProvider = modelProvider;
        this.sendHandler = sendHandler;
    }

    @Override
    public void handlePOST(CoapExchange exchange) {
        Request coapRequest = exchange.advanced().getRequest();
        Identity sender = extractIdentity(coapRequest.getSourceContext());
        Registration registration = registrationStore.getRegistrationByIdentity(sender);

        // check we have a registration for this identity
        if (registration == null) {
            exchange.respond(ResponseCode.NOT_FOUND, "no registration found");
            return;
        }

        // Decode payload
        LwM2mModel model = modelProvider.getObjectModel(registration);
        byte[] payload = exchange.getRequestPayload();
        ContentFormat contentFormat = ContentFormat.fromCode(exchange.getRequestOptions().getContentFormat());
        if (!decoder.isSupported(contentFormat)) {
            exchange.respond(ResponseCode.BAD_REQUEST, "Unsupported content format");
            return;
        }
        Map<LwM2mPath, LwM2mNode> data = decoder.decodeNodes(payload, contentFormat, (List<LwM2mPath>) null, model);

        // Handle "send op request
        SendRequest sendRequest = new SendRequest(contentFormat, data, coapRequest);
        SendableResponse<SendResponse> sendableResponse = sendHandler.handleSend(registration, sendRequest);
        SendResponse response = sendableResponse.getResponse();

        // send reponse
        if (response.isSuccess()) {
            exchange.respond(toCoapResponseCode(response.getCode()));
            sendableResponse.sent();
            return;
        } else {
            exchange.respond(toCoapResponseCode(response.getCode()), response.getErrorMessage());
            sendableResponse.sent();
            return;
        }
    }
}
