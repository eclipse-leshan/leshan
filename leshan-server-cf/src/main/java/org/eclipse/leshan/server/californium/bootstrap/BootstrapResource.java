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
 *     Sierra Wireless - initial API and implementation
 *******************************************************************************/
package org.eclipse.leshan.server.californium.bootstrap;

import static org.eclipse.leshan.core.californium.ResponseCodeUtil.toCoapResponseCode;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.californium.core.CoapResource;
import org.eclipse.californium.core.coap.CoAP.Type;
import org.eclipse.californium.core.coap.Request;
import org.eclipse.californium.core.server.resources.CoapExchange;
import org.eclipse.leshan.core.californium.LwM2mCoapResource;
import org.eclipse.leshan.core.californium.identity.IdentityHandlerProvider;
import org.eclipse.leshan.core.request.BootstrapRequest;
import org.eclipse.leshan.core.request.ContentFormat;
import org.eclipse.leshan.core.request.Identity;
import org.eclipse.leshan.core.response.BootstrapResponse;
import org.eclipse.leshan.core.response.SendableResponse;
import org.eclipse.leshan.server.bootstrap.LeshanBootstrapServer;
import org.eclipse.leshan.server.bootstrap.request.BootstrapUplinkRequestReceiver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link CoapResource} used to handle /bs request sent to {@link LeshanBootstrapServer}.
 */
public class BootstrapResource extends LwM2mCoapResource {

    private static final Logger LOG = LoggerFactory.getLogger(BootstrapResource.class);
    private static final String QUERY_PARAM_ENDPOINT = "ep=";
    private static final String QUERY_PARAM_PREFERRED_CONTENT_FORMAT = "pct=";

    private final BootstrapUplinkRequestReceiver receiver;

    public BootstrapResource(BootstrapUplinkRequestReceiver receiver, IdentityHandlerProvider identityHandlerProvider) {
        super("bs", identityHandlerProvider);
        this.receiver = receiver;
    }

    @Override
    public void handlePOST(CoapExchange exchange) {
        Request request = exchange.advanced().getRequest();
        LOG.trace("POST received : {}", request);

        // The LW M2M spec (section 8.2) mandates the usage of Confirmable
        // messages
        if (!Type.CON.equals(request.getType())) {
            handleInvalidRequest(exchange, "CON CoAP type expected");
            return;
        }

        // Get parameters
        String endpoint = null;
        ContentFormat preferredContentFomart = null;
        Map<String, String> additionalParams = new HashMap<>();
        for (String param : request.getOptions().getUriQuery()) {
            if (param.startsWith(QUERY_PARAM_ENDPOINT)) {
                endpoint = param.substring(QUERY_PARAM_ENDPOINT.length());
            } else if (param.startsWith(QUERY_PARAM_PREFERRED_CONTENT_FORMAT)) {
                try {
                    preferredContentFomart = ContentFormat
                            .fromCode(param.substring(QUERY_PARAM_PREFERRED_CONTENT_FORMAT.length()));
                } catch (NumberFormatException e) {
                    handleInvalidRequest(exchange.advanced(),
                            "Invalid preferre content format (pct) query param : must be a number", e);
                    return;
                }
            } else {
                String[] tokens = param.split("\\=");
                if (tokens != null && tokens.length == 2) {
                    additionalParams.put(tokens[0], tokens[1]);
                }
            }
        }

        // Extract client identity
        Identity clientIdentity = getForeignPeerIdentity(exchange.advanced(), request);

        // handle bootstrap request
        Request coapRequest = exchange.advanced().getRequest();
        SendableResponse<BootstrapResponse> sendableResponse = receiver.requestReceived(clientIdentity,
                new BootstrapRequest(endpoint, preferredContentFomart, additionalParams, coapRequest),
                exchange.advanced().getEndpoint().getUri());
        BootstrapResponse response = sendableResponse.getResponse();
        if (response.isSuccess()) {
            exchange.respond(toCoapResponseCode(response.getCode()));
        } else {
            exchange.respond(toCoapResponseCode(response.getCode()), response.getErrorMessage());
        }
        sendableResponse.sent();
    }
}
