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
 *     Sierra Wireless - initial API and implementation
 *******************************************************************************/
package org.eclipse.leshan.server.californium.impl;

import java.net.InetSocketAddress;
import java.security.Principal;
import java.security.PublicKey;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.security.auth.x500.X500Principal;

import org.eclipse.californium.core.CoapResource;
import org.eclipse.californium.core.coap.CoAP.ResponseCode;
import org.eclipse.californium.core.coap.CoAP.Type;
import org.eclipse.californium.core.coap.Request;
import org.eclipse.californium.core.coap.Response;
import org.eclipse.californium.core.network.Exchange;
import org.eclipse.californium.core.server.resources.CoapExchange;
import org.eclipse.californium.scandium.auth.PreSharedKeyIdentity;
import org.eclipse.californium.scandium.auth.RawPublicKeyIdentity;
import org.eclipse.leshan.core.request.BootstrapRequest;
import org.eclipse.leshan.core.request.Identity;
import org.eclipse.leshan.core.response.BootstrapResponse;
import org.eclipse.leshan.server.bootstrap.BootstrapHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BootstrapResource extends CoapResource {

    private static final Logger LOG = LoggerFactory.getLogger(BootstrapResource.class);
    private static final String QUERY_PARAM_ENDPOINT = "ep=";

    private final BootstrapHandler bootstrapHandler;

    public BootstrapResource(BootstrapHandler handler) {
        super("bs");
        bootstrapHandler = handler;
    }

    @Override
    public void handleRequest(Exchange exchange) {
        try {
            super.handleRequest(exchange);
        } catch (Exception e) {
            LOG.error("Exception while handling a request on the /bs resource", e);
            exchange.sendResponse(new Response(ResponseCode.INTERNAL_SERVER_ERROR));
        }
    }

    // TODO leshan-core-cf: this code should be factorized in a leshan-core-cf project.
    // TODO code is also in RegisterResource from leshan-server-cf
    private static Identity extractIdentity(CoapExchange exchange) {
        InetSocketAddress peerAddress = new InetSocketAddress(exchange.getSourceAddress(), exchange.getSourcePort());

        Principal senderIdentity = exchange.advanced().getRequest().getSenderIdentity();
        if (senderIdentity != null) {
            if (senderIdentity instanceof PreSharedKeyIdentity) {
                return Identity.psk(peerAddress, senderIdentity.getName());
            } else if (senderIdentity instanceof RawPublicKeyIdentity) {
                PublicKey publicKey = ((RawPublicKeyIdentity) senderIdentity).getKey();
                return Identity.rpk(peerAddress, publicKey);
            } else if (senderIdentity instanceof X500Principal) {
                // Extract common name
                Matcher endpointMatcher = Pattern.compile("CN=.*?,").matcher(senderIdentity.getName());
                if (endpointMatcher.find()) {
                    String x509CommonName = endpointMatcher.group().substring(3, endpointMatcher.group().length() - 1);
                    return Identity.x509(peerAddress, x509CommonName);
                } else {
                    return null;
                }
            }
        }
        return Identity.unsecure(peerAddress);
    }

    @Override
    public void handlePOST(final CoapExchange exchange) {
        Request request = exchange.advanced().getRequest();
        LOG.debug("POST received : {}", request);

        // The LW M2M spec (section 8.2) mandates the usage of Confirmable
        // messages
        if (!Type.CON.equals(request.getType())) {
            exchange.respond(ResponseCode.BAD_REQUEST);
            return;
        }

        // which endpoint?
        String endpoint = null;
        for (String param : request.getOptions().getUriQuery()) {
            if (param.startsWith(QUERY_PARAM_ENDPOINT)) {
                endpoint = param.substring(QUERY_PARAM_ENDPOINT.length());
                break;
            }
        }

        // Extract client identity
        Identity clientIdentity = extractIdentity(exchange);

        // handle bootstrap request
        BootstrapResponse response = bootstrapHandler.bootstrap(clientIdentity, new BootstrapRequest(endpoint));
        if (response.isSuccess()) {
            exchange.respond(RegisterResource.fromLwM2mCode(response.getCode()));
        } else {
            exchange.respond(RegisterResource.fromLwM2mCode(response.getCode()), response.getErrorMessage());
        }
    }
}
