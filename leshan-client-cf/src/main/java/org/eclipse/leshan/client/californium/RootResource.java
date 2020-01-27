/*******************************************************************************
 * Copyright (c) 2015 Sierra Wireless and others.
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
 *     Achim Kraus (Bosch Software Innovations GmbH) - use ServerIdentity
 *******************************************************************************/
package org.eclipse.leshan.client.californium;

import static org.eclipse.leshan.core.californium.ResponseCodeUtil.toCoapResponseCode;

import java.util.List;

import org.eclipse.californium.core.CoapResource;
import org.eclipse.californium.core.CoapServer;
import org.eclipse.californium.core.coap.CoAP.ResponseCode;
import org.eclipse.californium.core.network.Endpoint;
import org.eclipse.californium.core.server.resources.CoapExchange;
import org.eclipse.leshan.client.bootstrap.BootstrapHandler;
import org.eclipse.leshan.client.request.ServerIdentity;
import org.eclipse.leshan.core.request.BootstrapDeleteRequest;
import org.eclipse.leshan.core.response.BootstrapDeleteResponse;
import org.eclipse.leshan.util.StringUtils;

/**
 * A root {@link CoapResource} resource in charge of handling Bootstrap Delete requests targeting the "/" URI.
 */
public class RootResource extends LwM2mClientCoapResource {

    protected CoapServer coapServer;

    public RootResource(BootstrapHandler bootstrapHandler, CoapServer coapServer) {
        super("", bootstrapHandler);
        setVisible(false);
        this.coapServer = coapServer;
    }

    @Override
    public void handleGET(CoapExchange exchange) {
        exchange.respond(ResponseCode.NOT_FOUND);
    }

    @Override
    public void handleDELETE(CoapExchange exchange) {
        if (!StringUtils.isEmpty(exchange.getRequestOptions().getUriPathString())) {
            exchange.respond(ResponseCode.METHOD_NOT_ALLOWED);
            return;
        }

        ServerIdentity identity = extractServerIdentity(exchange);
        BootstrapDeleteResponse response = bootstrapHandler.delete(identity, new BootstrapDeleteRequest());
        exchange.respond(toCoapResponseCode(response.getCode()), response.getErrorMessage());
    }

    @Override
    public List<Endpoint> getEndpoints() {
        return coapServer.getEndpoints();
    }
}
