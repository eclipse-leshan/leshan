/*******************************************************************************
 * Copyright (c) 2023 Sierra Wireless and others.
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
package org.eclipse.leshan.transport.javacoap.client.resource;

import java.util.concurrent.CompletableFuture;

import org.eclipse.leshan.client.servers.LwM2mServer;
import org.eclipse.leshan.core.ResponseCode;
import org.eclipse.leshan.core.peer.IpPeer;
import org.eclipse.leshan.transport.javacoap.identity.IdentityHandler;
import org.eclipse.leshan.transport.javacoap.resource.LwM2mCoapResource;

import com.mbed.coap.packet.CoapRequest;
import com.mbed.coap.packet.CoapResponse;

/**
 * A {@link LwM2mCoapResource} with some specific method for LWM2M client.
 */
public class LwM2mClientCoapResource extends LwM2mCoapResource {

    protected final ServerIdentityExtractor serverIdentityExtractor;

    public LwM2mClientCoapResource(String uri, IdentityHandler identityHandler,
            ServerIdentityExtractor identityExtractor) {
        super(uri, identityHandler);
        this.serverIdentityExtractor = identityExtractor;
    }

    protected CompletableFuture<CoapResponse> unknownServer() {
        return errorMessage(ResponseCode.INTERNAL_SERVER_ERROR, "unknown server");
    }

    /**
     * Extract the {@link LwM2mServer} from this request. If there is no corresponding server currently in communication
     * with this client, return {@code null}.
     */
    protected LwM2mServer extractIdentity(CoapRequest request) {
        IpPeer foreignPeerIdentity = getForeignPeerIdentity(request);
        if (foreignPeerIdentity == null)
            return null;
        return serverIdentityExtractor.extractIdentity(foreignPeerIdentity);
    }
}
