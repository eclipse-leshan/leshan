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
package org.eclipse.leshan.transport.javacoap.client.coaptcp.endpoint;

import javax.net.SocketFactory;

import org.eclipse.leshan.client.servers.LwM2mServer;
import org.eclipse.leshan.core.endpoint.Protocol;
import org.eclipse.leshan.core.peer.IpPeer;
import org.eclipse.leshan.transport.javacoap.client.endpoint.AbstractJavaCoapClientEndpointsProvider;

import com.mbed.coap.packet.BlockSize;
import com.mbed.coap.packet.CoapRequest;
import com.mbed.coap.packet.CoapResponse;
import com.mbed.coap.server.CoapServer;
import com.mbed.coap.server.TcpCoapServer;
import com.mbed.coap.server.filter.TokenGeneratorFilter;
import com.mbed.coap.transport.javassl.SocketClientTransport;
import com.mbed.coap.utils.Service;

public class JavaCoapTcpClientEndpointsProvider extends AbstractJavaCoapClientEndpointsProvider {

    public JavaCoapTcpClientEndpointsProvider() {
        super(Protocol.COAP_TCP, "CoAP over TCP experimental endpoint based on java-coap library");
    }

    @Override
    protected CoapServer createCoapServer(LwM2mServer server, Service<CoapRequest, CoapResponse> router) {
        return TcpCoapServer.builder() ///
                .transport(new SocketClientTransport(((IpPeer) server.getTransportData()).getSocketAddress(),
                        SocketFactory.getDefault(), true)) //
                .blockSize(BlockSize.S_1024_BERT) //
                .outboundFilter(TokenGeneratorFilter.RANDOM)//
                .route(router) //
                .build();
    }
}
