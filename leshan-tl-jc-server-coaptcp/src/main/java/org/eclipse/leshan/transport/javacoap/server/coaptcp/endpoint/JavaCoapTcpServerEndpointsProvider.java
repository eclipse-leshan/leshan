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
package org.eclipse.leshan.transport.javacoap.server.coaptcp.endpoint;

import java.net.InetSocketAddress;

import org.eclipse.leshan.core.endpoint.Protocol;
import org.eclipse.leshan.server.security.SecurityStore;
import org.eclipse.leshan.server.security.ServerSecurityInfo;
import org.eclipse.leshan.transport.javacoap.identity.DefaultCoapIdentityHandler;
import org.eclipse.leshan.transport.javacoap.server.coaptcp.transport.CoapTcpTransportResolver;
import org.eclipse.leshan.transport.javacoap.server.coaptcp.transport.DefaultTransportContextMatcher;
import org.eclipse.leshan.transport.javacoap.server.coaptcp.transport.NettyCoapTcpTransport;
import org.eclipse.leshan.transport.javacoap.server.endpoint.AbstractJavaCoapServerEndpointsProvider;

import com.mbed.coap.packet.BlockSize;
import com.mbed.coap.packet.CoapRequest;
import com.mbed.coap.packet.CoapResponse;
import com.mbed.coap.server.CoapServer;
import com.mbed.coap.server.CoapServerBuilderForTcp;
import com.mbed.coap.server.TcpCoapServer;
import com.mbed.coap.server.filter.TokenGeneratorFilter;
import com.mbed.coap.server.observe.NotificationsReceiver;
import com.mbed.coap.server.observe.ObservationsStore;
import com.mbed.coap.utils.Service;

public class JavaCoapTcpServerEndpointsProvider extends AbstractJavaCoapServerEndpointsProvider {

    public JavaCoapTcpServerEndpointsProvider(InetSocketAddress localAddress) {
        super(Protocol.COAP_TCP, "CoAP over TCP experimental endpoint based on java-coap and netty libraries",
                localAddress, new DefaultCoapIdentityHandler());
    }

    @Override
    protected CoapServer createCoapServer(InetSocketAddress localAddress, ServerSecurityInfo serverSecurityInfo,
            SecurityStore securityStore, Service<CoapRequest, CoapResponse> resources,
            NotificationsReceiver notificationReceiver, ObservationsStore observationsStore) {
        return createCoapServer() //
                .transport(new NettyCoapTcpTransport(localAddress, new CoapTcpTransportResolver(),
                        new DefaultTransportContextMatcher(), null)) //
                .blockSize(BlockSize.S_1024_BERT) //
                .maxIncomingBlockTransferSize(4000) //
                .maxMessageSize(2100) //
                .route(resources) //
                .notificationsReceiver(notificationReceiver) //
                .observationsStore(observationsStore) //
                .build();
    }

    protected CoapServerBuilderForTcp createCoapServer() {
        return TcpCoapServer.builder().outboundFilter(TokenGeneratorFilter.RANDOM);
    }
}
