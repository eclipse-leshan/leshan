/*******************************************************************************
 * Copyright (c) 2019 Sierra Wireless and others.
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
package org.eclipse.leshan.client.californium;

import org.eclipse.californium.core.CoapResource;
import org.eclipse.californium.core.coap.CoAP.ResponseCode;
import org.eclipse.californium.core.network.Exchange;
import org.eclipse.californium.core.server.resources.CoapExchange;
import org.eclipse.leshan.client.engine.RegistrationEngine;
import org.eclipse.leshan.client.servers.ServerIdentity;
import org.eclipse.leshan.core.californium.LwM2mCoapResource;
import org.eclipse.leshan.core.request.Identity;

/**
 * A Common {@link CoapResource} used to handle LWM2M request with some specific method for LWM2M client.
 */
public class LwM2mClientCoapResource extends LwM2mCoapResource {

    protected final RegistrationEngine registrationEngine;

    public LwM2mClientCoapResource(String name, RegistrationEngine registrationEngine) {
        super(name);
        this.registrationEngine = registrationEngine;
    }

    /**
     * @return the server identity of a registered or bootstrap server, return null if this identity does match to any
     *         server for which we are in communication.
     */
    protected ServerIdentity getServer(Identity identity) {
        return registrationEngine.getServer(identity);
    }

    /**
     * Extract the {@link ServerIdentity} for this exchange. If there is no corresponding server currently in
     * communication with this client. Answer with an {@link ResponseCode#INTERNAL_SERVER_ERROR}.
     */
    protected ServerIdentity getServerOrRejectRequest(CoapExchange exchange) {
        Identity extractedIdentity = extractIdentity(exchange);

        // search if we are in communication with this server.
        ServerIdentity server = getServer(extractedIdentity);
        if (server != null)
            return server;

        exchange.respond(ResponseCode.INTERNAL_SERVER_ERROR, "unknown server");
        return null;
    }

    /**
     * Create Leshan {@link Identity} from Californium {@link Exchange}.
     * 
     * @param exchange The Californium {@link Exchange} containing the request for which we search sender identity.
     * @return The corresponding Leshan {@link Identity}.
     * @throws IllegalStateException if we are not able to extract {@link Identity}.
     */
    protected Identity extractIdentity(CoapExchange exchange) {
        return extractIdentity(exchange.advanced().getRequest().getSourceContext());
    }
}
