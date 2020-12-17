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
 *     Rikard Höglund (RISE) - additions to support OSCORE
 *******************************************************************************/
package org.eclipse.leshan.server.californium.bootstrap;

import static org.eclipse.leshan.core.californium.ResponseCodeUtil.toCoapResponseCode;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.californium.core.CoapResource;
import org.eclipse.californium.core.coap.CoAP.Type;
import org.eclipse.californium.core.coap.Request;
import org.eclipse.californium.core.server.resources.CoapExchange;
import org.eclipse.californium.oscore.HashMapCtxDB;
import org.eclipse.californium.oscore.OSCoreCtx;
import org.eclipse.californium.oscore.OSException;
import org.eclipse.leshan.core.californium.LwM2mCoapResource;
import org.eclipse.leshan.core.request.BootstrapRequest;
import org.eclipse.leshan.core.request.Identity;
import org.eclipse.leshan.core.response.BootstrapResponse;
import org.eclipse.leshan.core.response.SendableResponse;
import org.eclipse.leshan.server.OscoreHandler;
import org.eclipse.leshan.server.bootstrap.BootstrapHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link CoapResource} used to handle /bs request sent to {@link LeshanBootstrapServer}.
 */
public class BootstrapResource extends LwM2mCoapResource {

    private static final Logger LOG = LoggerFactory.getLogger(BootstrapResource.class);
    private static final String QUERY_PARAM_ENDPOINT = "ep=";

    private final BootstrapHandler bootstrapHandler;

    public BootstrapResource(BootstrapHandler handler) {
        super("bs");
        bootstrapHandler = handler;
    }

    @Override
    public void handlePOST(CoapExchange exchange) {
        Request request = exchange.advanced().getRequest();
        LOG.trace("POST received : {}", request);

        // TODO OSCORE : should we really need to do this ?
        // Check if this incoming request is using OSCORE
        if (exchange.advanced().getRequest().getOptions().getOscore() != null) {
            LOG.trace("Client bootstrapped using OSCORE");

            // Update the URI of the associated OSCORE Context with the client's URI
            // So the server can send requests to the client
            HashMapCtxDB db = OscoreHandler.getContextDB();
            OSCoreCtx clientCtx = db.getContext(exchange.advanced().getCryptographicContextID());

            try {
                db.addContext(request.getScheme() + "://"
                        + request.getSourceContext().getPeerAddress().getHostString().toString(), clientCtx);
            } catch (OSException e) {
                LOG.error("Failed to update OSCORE Context for registering client.", request, e);
            }
        }

        // The LW M2M spec (section 8.2) mandates the usage of Confirmable
        // messages
        if (!Type.CON.equals(request.getType())) {
            handleInvalidRequest(exchange, "CON CoAP type expected");
            return;
        }

        // which endpoint?
        String endpoint = null;
        Map<String, String> additionalParams = new HashMap<>();
        for (String param : request.getOptions().getUriQuery()) {
            if (param.startsWith(QUERY_PARAM_ENDPOINT)) {
                endpoint = param.substring(QUERY_PARAM_ENDPOINT.length());
            } else {
                String[] tokens = param.split("\\=");
                if (tokens != null && tokens.length == 2) {
                    additionalParams.put(tokens[0], tokens[1]);
                }
            }
        }

        // Extract client identity
        Identity clientIdentity = extractIdentity(request.getSourceContext());

        // handle bootstrap request
        SendableResponse<BootstrapResponse> sendableResponse = bootstrapHandler.bootstrap(clientIdentity,
                new BootstrapRequest(endpoint, additionalParams));
        BootstrapResponse response = sendableResponse.getResponse();
        if (response.isSuccess()) {
            exchange.respond(toCoapResponseCode(response.getCode()));
        } else {
            exchange.respond(toCoapResponseCode(response.getCode()), response.getErrorMessage());
        }
        sendableResponse.sent();
    }
}
