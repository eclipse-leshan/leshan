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
import java.security.Principal;
import java.security.cert.CertificateException;
import java.util.Arrays;

import javax.net.ssl.SSLException;
import javax.net.ssl.X509KeyManager;
import javax.net.ssl.X509TrustManager;
import javax.security.auth.x500.X500Principal;

import org.eclipse.leshan.core.endpoint.Protocol;
import org.eclipse.leshan.core.security.certificate.util.X509CertUtil;
import org.eclipse.leshan.core.security.jsse.LwM2mX509TrustManager;
import org.eclipse.leshan.servers.security.ObservableSecurityStore;
import org.eclipse.leshan.servers.security.SecurityInfo;
import org.eclipse.leshan.servers.security.SecurityStore;
import org.eclipse.leshan.servers.security.ServerSecurityInfo;
import org.eclipse.leshan.transport.javacoap.SingleX509KeyManager;
import org.eclipse.leshan.transport.javacoap.identity.DefaultTlsIdentityHandler;
import org.eclipse.leshan.transport.javacoap.identity.TlsTransportContextKeys;
import org.eclipse.leshan.transport.javacoap.server.coaptcp.transport.CoapsTcpTransportResolver;
import org.eclipse.leshan.transport.javacoap.server.coaptcp.transport.NettyCoapTcpTransport;
import org.eclipse.leshan.transport.javacoap.server.coaptcp.transport.TransportContextHandler;
import org.eclipse.leshan.transport.javacoap.server.endpoint.AbstractJavaCoapServerEndpointsProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mbed.coap.packet.BlockSize;
import com.mbed.coap.packet.CoapRequest;
import com.mbed.coap.packet.CoapResponse;
import com.mbed.coap.server.CoapServer;
import com.mbed.coap.server.CoapServerBuilderForTcp;
import com.mbed.coap.server.TcpCoapServer;
import com.mbed.coap.server.filter.TokenGeneratorFilter;
import com.mbed.coap.server.observe.NotificationsReceiver;
import com.mbed.coap.server.observe.ObservationsStore;
import com.mbed.coap.transport.TransportContext;
import com.mbed.coap.utils.Service;

import io.netty.channel.Channel;
import io.netty.handler.ssl.ClientAuth;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;

public class JavaCoapsTcpServerEndpointsProvider extends AbstractJavaCoapServerEndpointsProvider {

    private static final Logger LOG = LoggerFactory.getLogger(JavaCoapsTcpServerEndpointsProvider.class);

    public JavaCoapsTcpServerEndpointsProvider(InetSocketAddress localAddress) {
        super(Protocol.COAPS_TCP, "CoAP over TLS experimental endpoint based on java-coap and netty libraries",
                localAddress, new DefaultTlsIdentityHandler());
    }

    @Override
    protected CoapServer createCoapServer(InetSocketAddress localAddress, ServerSecurityInfo serverSecurityInfo,
            SecurityStore securityStore, Service<CoapRequest, CoapResponse> resources,
            NotificationsReceiver notificationReceiver, ObservationsStore observationsStore) {

        // Create SSL Handler with right Credentials
        SslContext sslContext;
        try {
            // Create context
            X509KeyManager keys = new SingleX509KeyManager(serverSecurityInfo.getPrivateKey(),
                    serverSecurityInfo.getCertificateChain());
            X509TrustManager trustManger = new LwM2mX509TrustManager(new ClientCertificateVerifier(
                    Arrays.asList(X509CertUtil.asX509Certificates(serverSecurityInfo.getTrustedCertificates()))));

            sslContext = SslContextBuilder //
                    .forServer(keys) //
                    .startTls(false) //
                    .trustManager(trustManger) //
                    .protocols("TLSv1.2") //
                    .clientAuth(ClientAuth.REQUIRE) //
                    .build();

        } catch (SSLException | CertificateException e) {
            throw new IllegalStateException("Unable to create tls endpoint point", e);
        }

        if (sslContext == null) {
            throw new IllegalStateException("Unable to create tls endpoint point : sslcontext must not be null");
        }

        NettyCoapTcpTransport transport = new NettyCoapTcpTransport(localAddress, new CoapsTcpTransportResolver(),
                new LwM2mTransportContextMatcher(), sslContext);

        createAndAttachConnectionCleaner(transport, securityStore);

        return createCoapServer() //
                .transport(transport) //
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

    protected void createAndAttachConnectionCleaner(NettyCoapTcpTransport transport, SecurityStore securityStore) {
        if (securityStore instanceof ObservableSecurityStore) {
            ((ObservableSecurityStore) securityStore).addListener((infosAreCompromised, infos) -> transport
                    .closeConnections(channel -> isConnectionOfAny(channel, infos)));
        }
    }

    private static boolean isConnectionOfAny(Channel channel, SecurityInfo... infos) {
        Principal principal = getPrincipal(channel);
        return principal != null && matchesAnyX509(principal, infos);
    }

    private static Principal getPrincipal(Channel channel) {
        TransportContext transportContext = channel.attr(TransportContextHandler.TRANSPORT_CONTEXT_ATTR).get();
        return transportContext == null ? null : transportContext.get(TlsTransportContextKeys.PRINCIPAL);
    }

    private static boolean matchesAnyX509(Principal principal, SecurityInfo... infos) {
        for (SecurityInfo info : infos) {
            if (info != null && info.useX509Cert() && matchesX509(principal, info)) {
                return true;
            }
        }
        return false;
    }

    private static boolean matchesX509(Principal principal, SecurityInfo info) {
        if (!(principal instanceof X500Principal)) {
            return false;
        }
        try {
            // Extract common name
            String x509CommonName = X509CertUtil.extractCN((X500Principal) principal);
            return x509CommonName.equals(info.getEndpoint());
        } catch (IllegalStateException e) {
            // The identity of this connection can not be read (no CN, several CN, ...) so this is not the connection we
            // are looking for. The exception must not escape from the predicate given to closeConnections.
            LOG.debug("Ignore connection with unreadable X.509 identity while cleaning connections for '{}': {}",
                    info.getEndpoint(), e.getMessage());
            return false;
        }
    }
}
