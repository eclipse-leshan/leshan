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
package org.eclipse.leshan.transport.javacoap.endpoint;

import java.net.URI;

import org.eclipse.leshan.core.endpoint.EndpointUriUtil;
import org.eclipse.leshan.core.endpoint.Protocol;

import com.mbed.coap.server.CoapServer;

public class EndpointUriProvider {

    private CoapServer coapServer;
    private final Protocol protocol;

    public EndpointUriProvider(Protocol protocol) {
        this.protocol = protocol;
    }

    public void setCoapServer(CoapServer coapServer) {
        this.coapServer = coapServer;
    }

    public URI getEndpointUri() {
        return EndpointUriUtil.createUri(protocol.getUriScheme(), coapServer.getLocalSocketAddress());
    }
}
