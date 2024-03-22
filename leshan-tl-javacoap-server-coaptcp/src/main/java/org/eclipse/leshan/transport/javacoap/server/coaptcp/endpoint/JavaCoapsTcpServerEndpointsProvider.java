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

import javax.net.ssl.SSLException;
import javax.net.ssl.X509KeyManager;
import javax.net.ssl.X509TrustManager;

import org.eclipse.leshan.core.endpoint.Protocol;
import org.eclipse.leshan.server.security.ServerSecurityInfo;
import org.eclipse.leshan.transport.javacoap.LwM2mClientTrustedManager;
import org.eclipse.leshan.transport.javacoap.SingleX509KeyManager;
import org.eclipse.leshan.transport.javacoap.identity.DefaultTlsIdentityHandler;
import org.eclipse.leshan.transport.javacoap.server.coaptcp.transport.CoapsTcpTransportResolver;
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

import io.netty.handler.ssl.ClientAuth;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;

public class JavaCoapsTcpServerEndpointsProvider extends AbstractJavaCoapServerEndpointsProvider {

    public JavaCoapsTcpServerEndpointsProvider(InetSocketAddress localAddress) {
        super(Protocol.COAPS_TCP, "CoAP over TLS experimental endpoint based on java-coap and netty libraries",
                localAddress, new DefaultTlsIdentityHandler());
    }

    @Override
    protected CoapServer createCoapServer(InetSocketAddress localAddress, ServerSecurityInfo serverSecurityInfo,
            Service<CoapRequest, CoapResponse> resources, NotificationsReceiver notificationReceiver,
            ObservationsStore observationsStore) {

        // Create SSL Handler with right Credentials
        SslContext sslContext;
        try {
            // Create context
            X509KeyManager keys = new SingleX509KeyManager(serverSecurityInfo.getPrivateKey(),
                    serverSecurityInfo.getCertificateChain());
            X509TrustManager trustManger = new LwM2mClientTrustedManager();

            sslContext = SslContextBuilder //
                    .forServer(keys) //
                    .startTls(false) //
                    .trustManager(trustManger) //
                    .protocols("TLSv1.2") //
                    .clientAuth(ClientAuth.REQUIRE) //
                    .build();

        } catch (SSLException e) {
            throw new IllegalStateException("Unable to create tls endpoint point", e);
        }

        if (sslContext == null) {
            throw new IllegalStateException("Unable to create tls endpoint point : sslcontext must not be null");
        }

        return createCoapServer() //
                .transport(new NettyCoapTcpTransport(localAddress, new CoapsTcpTransportResolver(),
                        new LwM2mTransportContextMatcher(), sslContext)) //
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
