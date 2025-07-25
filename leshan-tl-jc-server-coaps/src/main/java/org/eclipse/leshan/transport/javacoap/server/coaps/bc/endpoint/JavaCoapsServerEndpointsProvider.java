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
package org.eclipse.leshan.transport.javacoap.server.coaps.bc.endpoint;

import java.net.InetSocketAddress;
import java.security.Principal;

import javax.security.auth.x500.X500Principal;

import org.bouncycastle.tls.crypto.impl.bc.BcTlsCrypto;
import org.eclipse.leshan.core.endpoint.Protocol;
import org.eclipse.leshan.core.security.certificate.util.X509CertUtil;
import org.eclipse.leshan.servers.security.EditableSecurityStore;
import org.eclipse.leshan.servers.security.SecurityInfo;
import org.eclipse.leshan.servers.security.SecurityStore;
import org.eclipse.leshan.servers.security.ServerSecurityInfo;
import org.eclipse.leshan.transport.javacoap.identity.DefaultTlsIdentityHandler;
import org.eclipse.leshan.transport.javacoap.identity.PreSharedKeyPrincipal;
import org.eclipse.leshan.transport.javacoap.identity.RawPublicKeyPrincipal;
import org.eclipse.leshan.transport.javacoap.server.endpoint.AbstractJavaCoapServerEndpointsProvider;
import org.eclipse.leshan.transport.javacoap.server.endpoint.ConnectionsManager;
import org.eclipse.leshan.transport.javacoap.transport.context.keys.TlsTransportContextKeys;

import com.mbed.coap.packet.CoapRequest;
import com.mbed.coap.packet.CoapResponse;
import com.mbed.coap.server.CoapServer;
import com.mbed.coap.server.CoapServerBuilder;
import com.mbed.coap.server.filter.TokenGeneratorFilter;
import com.mbed.coap.server.observe.NotificationsReceiver;
import com.mbed.coap.server.observe.ObservationsStore;
import com.mbed.coap.transport.CoapTransport;
import com.mbed.coap.transport.TransportContext;
import com.mbed.coap.utils.Service;

public class JavaCoapsServerEndpointsProvider extends AbstractJavaCoapServerEndpointsProvider {

    private final BcTlsCrypto crypto = new BcTlsCrypto();

    public JavaCoapsServerEndpointsProvider(InetSocketAddress localAddress) {
        super(Protocol.COAPS, "CoAP over DTLS experimental endpoint based on java-coap and Bouncy Castle librar",
                localAddress, new DefaultTlsIdentityHandler());
    }

    @Override
    protected CoapServer createCoapServer(CoapTransport transport, Service<CoapRequest, CoapResponse> resources,
            NotificationsReceiver notificationReceiver, ObservationsStore observationsStore) {
        return createCoapServer() //
                .transport(transport) //
                .route(resources) //
                .notificationsReceiver(notificationReceiver) //
                .observationsStore(observationsStore) //
                .build();
    }

    protected CoapServerBuilder createCoapServer() {
        return CoapServer.builder().outboundFilter(TokenGeneratorFilter.RANDOM);
    }

    @Override
    protected CoapTransport createCoapTransport(InetSocketAddress localAddress, ServerSecurityInfo serverSecurityInfo,
            SecurityStore securityStore) {
        BouncyCastleDtlsTransport dtlsTransport = new BouncyCastleDtlsTransport(
                createTlsServerFactory(serverSecurityInfo, securityStore), localAddress, null, null);
        if (securityStore instanceof EditableSecurityStore) {
            ((EditableSecurityStore) securityStore).addListener((infosAreCompromised, infos) -> {
                if (infosAreCompromised) {
                    for (SecurityInfo info : infos) {
                        cleanConnection(dtlsTransport, info);
                    }
                }
            });
        }

        return dtlsTransport;
    }

    protected LwM2mTlsServerFactory createTlsServerFactory(ServerSecurityInfo serverSecurityInfo,
            SecurityStore securityStore) {
        return new LwM2mTlsServerFactory(crypto, serverSecurityInfo, securityStore);
    }

    private void cleanConnection(BouncyCastleDtlsTransport dtlsTransport, SecurityInfo info) {
        dtlsTransport.removeConnection(con -> {
            TransportContext transportContext = con.getValue().getTransportContext();
            Principal principal = transportContext != null ? transportContext.get(TlsTransportContextKeys.PRINCIPAL)
                    : null;
            if (info.usePSK() && principal instanceof PreSharedKeyPrincipal) {
                return info.getPskIdentity().equals(((PreSharedKeyPrincipal) principal).getIdentity());
            } else if (info.useRPK() && principal instanceof RawPublicKeyPrincipal) {
                return info.getRawPublicKey().equals(((RawPublicKeyPrincipal) principal).getPublicKey());
            } else if (info.useX509Cert() && principal instanceof X500Principal) {
                // Extract common name
                String x509CommonName = X509CertUtil.extractCN(principal.getName());
                if (x509CommonName.equals(info.getEndpoint())) {
                    return true;
                }
            }
            return false;
        });
    }

    @Override
    protected ConnectionsManager createConnectionManager(CoapTransport transport) {
        return ((BouncyCastleDtlsTransport) transport)::removeAllConnections;
    }
}
