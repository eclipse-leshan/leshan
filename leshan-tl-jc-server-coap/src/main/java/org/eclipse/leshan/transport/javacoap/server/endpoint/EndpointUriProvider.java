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
package org.eclipse.leshan.transport.javacoap.server.endpoint;

import org.eclipse.leshan.core.endpoint.EndPointUriHandler;
import org.eclipse.leshan.core.endpoint.EndpointUri;
import org.eclipse.leshan.core.endpoint.Protocol;

import com.mbed.coap.server.CoapServer;

public class EndpointUriProvider {

    private CoapServer coapServer;
    private final Protocol protocol;
    private final EndPointUriHandler uriHandler;

    public EndpointUriProvider(EndPointUriHandler uriHandler, Protocol protocol) {
        this.protocol = protocol;
        this.uriHandler = uriHandler;
    }

    public void setCoapServer(CoapServer coapServer) {
        this.coapServer = coapServer;
    }

    public EndpointUri getEndpointUri() {
        return uriHandler.createUri(protocol.getUriScheme(), coapServer.getLocalSocketAddress());
    }
}
