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
import org.eclipse.californium.core.coap.Message;
import org.eclipse.californium.core.network.Exchange;
import org.eclipse.californium.core.server.resources.CoapExchange;
import org.eclipse.leshan.client.californium.endpoint.ServerIdentityExtractor;
import org.eclipse.leshan.client.servers.ServerIdentity;
import org.eclipse.leshan.core.californium.LwM2mCoapResource;
import org.eclipse.leshan.core.californium.identity.IdentityHandlerProvider;
import org.eclipse.leshan.core.request.Identity;

/**
 * A Common {@link CoapResource} used to handle LWM2M request with some specific method for LWM2M client.
 */
public class LwM2mClientCoapResource extends LwM2mCoapResource {

    protected final ServerIdentityExtractor serverIdentityExtractor;

    public LwM2mClientCoapResource(String name, IdentityHandlerProvider identityHandlerProvider,
            ServerIdentityExtractor identityExtractor) {
        super(name, identityHandlerProvider);
        this.serverIdentityExtractor = identityExtractor;
    }

    /**
     * Extract the {@link ServerIdentity} for this exchange. If there is no corresponding server currently in
     * communication with this client. Answer with an {@link ResponseCode#INTERNAL_SERVER_ERROR}.
     */
    protected ServerIdentity getServerOrRejectRequest(CoapExchange exchange, Message receivedMessage) {
        // search if we are in communication with this server.
        ServerIdentity server = extractIdentity(exchange.advanced(), receivedMessage);
        if (server != null)
            return server;

        exchange.respond(ResponseCode.INTERNAL_SERVER_ERROR, "unknown server");
        return null;
    }

    /**
     * Get Leshan {@link ServerIdentity} from Californium {@link Exchange}.
     *
     * @param exchange The Californium {@link Exchange} containing the request for which we search sender identity.
     * @return The corresponding Leshan {@link ServerIdentity}.
     * @throws IllegalStateException if we are not able to extract {@link ServerIdentity}.
     */
    protected ServerIdentity extractIdentity(Exchange exchange, Message receivedMessage) {
        Identity foreignPeerIdentity = getForeignPeerIdentity(exchange, receivedMessage);
        if (foreignPeerIdentity == null)
            return null;
        return serverIdentityExtractor.extractIdentity(exchange, foreignPeerIdentity);
    }
}
