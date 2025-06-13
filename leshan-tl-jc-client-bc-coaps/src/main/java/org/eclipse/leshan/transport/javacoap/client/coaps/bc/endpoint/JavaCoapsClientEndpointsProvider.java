/*******************************************************************************
 * Copyright (c) 2024 Sierra Wireless and others.
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
package org.eclipse.leshan.transport.javacoap.client.coaps.bc.endpoint;

import java.security.cert.Certificate;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.bouncycastle.tls.crypto.TlsCrypto;
import org.bouncycastle.tls.crypto.impl.bc.BcTlsCrypto;
import org.eclipse.leshan.client.servers.ServerInfo;
import org.eclipse.leshan.core.endpoint.Protocol;
import org.eclipse.leshan.transport.javacoap.client.endpoint.AbstractJavaCoapClientEndpointsProvider;
import org.eclipse.leshan.transport.javacoap.identity.DefaultTlsIdentityHandler;

import com.mbed.coap.packet.CoapPacket;
import com.mbed.coap.packet.CoapRequest;
import com.mbed.coap.packet.CoapResponse;
import com.mbed.coap.server.CoapServer;
import com.mbed.coap.server.filter.TokenGeneratorFilter;
import com.mbed.coap.utils.Service;

public class JavaCoapsClientEndpointsProvider extends AbstractJavaCoapClientEndpointsProvider {

    private final TlsCrypto crypto = new BcTlsCrypto();

    public JavaCoapsClientEndpointsProvider() {
        super(Protocol.COAPS, "CoAP over DTLS experimental endpoint based on java-coap and Bouncy Castle librar",
                new DefaultTlsIdentityHandler());
    }

    @Override
    protected CoapServer createCoapServer(ServerInfo serverInfo, Service<CoapRequest, CoapResponse> router,
            List<Certificate> trustStore) {
        return CoapServer.builder() //
                .outboundFilter(TokenGeneratorFilter.RANDOM) //
                .transport(new BouncyCastleDtlsTransport(new LwM2mTlsClient(crypto, serverInfo),
                        serverInfo.getAddress(), null, null) {

                    @Override
                    public CompletableFuture<CoapPacket> receive() {
                        // HACK to define transport context
//                return super.receive().thenApply(packet -> {
//                    packet.setTransportContext(getTransportContext(client));
//                    return packet;
//                });
                        return super.receive();
                    }
                })//
                .route(router) //
                .build();
    }
}
