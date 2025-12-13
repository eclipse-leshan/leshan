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

import java.security.cert.Certificate;
import java.util.List;

import javax.net.SocketFactory;

import org.eclipse.leshan.client.servers.ServerInfo;
import org.eclipse.leshan.core.endpoint.Protocol;
import org.eclipse.leshan.transport.javacoap.client.endpoint.AbstractJavaCoapClientEndpointsProvider;
import org.eclipse.leshan.transport.javacoap.client.endpoint.JavaCoapConnectionController;
import org.eclipse.leshan.transport.javacoap.identity.DefaultCoapIdentityHandler;

import com.mbed.coap.packet.BlockSize;
import com.mbed.coap.packet.CoapRequest;
import com.mbed.coap.packet.CoapResponse;
import com.mbed.coap.server.CoapServer;
import com.mbed.coap.server.TcpCoapServer;
import com.mbed.coap.server.filter.TokenGeneratorFilter;
import com.mbed.coap.transport.CoapTcpTransport;
import com.mbed.coap.transport.CoapTransport;
import com.mbed.coap.transport.javassl.SocketClientTransport;
import com.mbed.coap.utils.Service;

public class JavaCoapTcpClientEndpointsProvider extends AbstractJavaCoapClientEndpointsProvider {

    public JavaCoapTcpClientEndpointsProvider() {
        super(Protocol.COAP_TCP, "CoAP over TCP experimental endpoint based on java-coap library",
                new DefaultCoapIdentityHandler());
    }

    @Override
    protected CoapTransport createCoapTransport(ServerInfo serverInfo, List<Certificate> trustStore) {
        return new SocketClientTransport(serverInfo.getAddress(), SocketFactory.getDefault(), true);
    }

    @Override
    protected JavaCoapConnectionController createConnectionController(CoapTransport transport) {
        return (server, resume) -> {
            // nothing to do in coap over tcp
        };
    }

    @Override
    protected CoapServer createCoapServer(CoapTransport transport, Service<CoapRequest, CoapResponse> router) {
        return TcpCoapServer.builder().transport((CoapTcpTransport) transport) //
                .blockSize(BlockSize.S_1024_BERT) //
                .outboundFilter(TokenGeneratorFilter.RANDOM)//
                .route(router) //
                .build();
    }
}
