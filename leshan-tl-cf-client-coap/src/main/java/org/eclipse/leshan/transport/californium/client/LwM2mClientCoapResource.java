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
package org.eclipse.leshan.transport.californium.client;

import org.eclipse.californium.core.CoapExchange;
import org.eclipse.californium.core.CoapResource;
import org.eclipse.californium.core.coap.CoAP.ResponseCode;
import org.eclipse.californium.core.coap.Message;
import org.eclipse.californium.core.coap.Request;
import org.eclipse.californium.core.network.Exchange;
import org.eclipse.leshan.client.servers.LwM2mServer;
import org.eclipse.leshan.core.peer.IpPeer;
import org.eclipse.leshan.transport.californium.LwM2mCoapResource;
import org.eclipse.leshan.transport.californium.client.endpoint.ServerIdentityExtractor;
import org.eclipse.leshan.transport.californium.identity.IdentityHandlerProvider;

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
     * Extract the {@link LwM2mServer} for this exchange. If there is no corresponding server currently in communication
     * with this client. Answer with an {@link ResponseCode#INTERNAL_SERVER_ERROR}.
     */
    protected LwM2mServer getServerOrRejectRequest(CoapExchange exchange, Message receivedMessage) {
        // search if we are in communication with this server.
        LwM2mServer server = extractIdentity(exchange.advanced(), receivedMessage);
        if (server != null)
            return server;

        exchange.respond(ResponseCode.INTERNAL_SERVER_ERROR, "unknown server");
        return null;
    }

    /**
     * Get Leshan {@link LwM2mServer} from Californium {@link Exchange}.
     *
     * @param exchange The Californium {@link Exchange} containing the request for which we search sender identity.
     * @return The corresponding Leshan {@link LwM2mServer}.
     * @throws IllegalStateException if we are not able to extract {@link LwM2mServer}.
     */
    protected LwM2mServer extractIdentity(Exchange exchange, Message receivedMessage) {
        IpPeer foreignPeer = getForeignPeerIdentity(exchange, receivedMessage);
        if (foreignPeer == null)
            return null;
        return serverIdentityExtractor.extractIdentity(exchange, foreignPeer);
    }

    protected String extractNodeURI(Request request) {
        String rootPath = getPath();
        String fullUri = "/" + request.getOptions().getUriPathString();
        return rootPath != null && fullUri.startsWith(rootPath) ? fullUri.substring(rootPath.length()) : fullUri;
    }
}
