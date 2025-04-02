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
package org.eclipse.leshan.transport.javacoap.client.coaptcp.endpoint;

import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.Principal;
import java.security.cert.Certificate;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509KeyManager;
import javax.net.ssl.X509TrustManager;

import org.eclipse.leshan.client.security.CertificateVerifierFactory;
import org.eclipse.leshan.client.servers.ServerInfo;
import org.eclipse.leshan.core.endpoint.Protocol;
import org.eclipse.leshan.core.security.jsse.LwM2mX509TrustManager;
import org.eclipse.leshan.transport.javacoap.SingleX509KeyManager;
import org.eclipse.leshan.transport.javacoap.client.endpoint.AbstractJavaCoapClientEndpointsProvider;
import org.eclipse.leshan.transport.javacoap.identity.DefaultTlsIdentityHandler;
import org.eclipse.leshan.transport.javacoap.identity.TlsTransportContextKeys;

import com.mbed.coap.packet.BlockSize;
import com.mbed.coap.packet.CoapPacket;
import com.mbed.coap.packet.CoapRequest;
import com.mbed.coap.packet.CoapResponse;
import com.mbed.coap.packet.Opaque;
import com.mbed.coap.server.CoapServer;
import com.mbed.coap.server.TcpCoapServer;
import com.mbed.coap.server.filter.TokenGeneratorFilter;
import com.mbed.coap.transport.TransportContext;
import com.mbed.coap.utils.Service;

public class JavaCoapsTcpClientEndpointsProvider extends AbstractJavaCoapClientEndpointsProvider {

    private final CertificateVerifierFactory certificateVerifierFactory = new CertificateVerifierFactory();

    public JavaCoapsTcpClientEndpointsProvider() {
        super(Protocol.COAPS_TCP, "CoAP over TLS experimental endpoint based on java-coap library",
                new DefaultTlsIdentityHandler());
    }

    @Override
    protected CoapServer createCoapServer(ServerInfo serverInfo, Service<CoapRequest, CoapResponse> router,
            List<Certificate> trustStore) {

        // Create SSL Socket Factory using right credentials.
        SSLContext tlsContext;
        try {
            // Create context
            tlsContext = SSLContext.getInstance("TLSv1.2");

            // Configure it
            X509KeyManager keys = new SingleX509KeyManager(serverInfo.privateKey,
                    serverInfo.getX509ClientCertificates());

            X509TrustManager trustManger = new LwM2mX509TrustManager(
                    certificateVerifierFactory.create(serverInfo, trustStore));

            // Initialize it
            tlsContext.init(new KeyManager[] { keys }, new TrustManager[] { trustManger }, null);
        } catch (KeyManagementException | NoSuchAlgorithmException e) {
            throw new IllegalArgumentException("Unable to create tls endpoint point", e);
        }

        return TcpCoapServer.builder() ///
                .transport(new SSLSocketClientTransport(serverInfo.getAddress(), tlsContext.getSocketFactory(), true) {

                    @Override
                    public CompletableFuture<CoapPacket> receive() {
                        // HACK to define transport context
                        return super.receive().thenApply(packet -> {
                            packet.setTransportContext(getTransportContext(getSslSocket()));
                            return packet;
                        });
                    };
                })//
                .blockSize(BlockSize.S_1024_BERT) //
                .outboundFilter(TokenGeneratorFilter.RANDOM)//
                .route(router) //
                .build();
    }

    private TransportContext getTransportContext(SSLSocket sslSocket) {
        SSLSession sslSession = sslSocket.getSession();

        if (sslSession == null) {
            throw new IllegalStateException("Missing Session");
        }

        // Get Principal
        Principal principal;
        try {
            principal = sslSession.getPeerPrincipal();
        } catch (SSLPeerUnverifiedException e) {
            throw new IllegalStateException("Unable to get Principal", e);
        }
        if (principal == null) {
            throw new IllegalStateException("Missing Principal");
        }

        // Get Cipher Suite
        String cipherSuite = sslSession.getCipherSuite();
        if (cipherSuite == null) {
            throw new IllegalStateException("Missing Cipher Suite");

        }

        return TransportContext.of(TlsTransportContextKeys.TLS_SESSION_ID, new Opaque(sslSession.getId()).toHex()) //
                .with(TlsTransportContextKeys.PRINCIPAL, principal) //
                .with(TlsTransportContextKeys.CIPHER_SUITE, cipherSuite);

    }
}
