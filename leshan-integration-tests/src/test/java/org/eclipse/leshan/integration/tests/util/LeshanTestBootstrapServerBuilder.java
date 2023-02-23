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
package org.eclipse.leshan.integration.tests.util;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.util.concurrent.TimeUnit;

import org.eclipse.californium.elements.config.Configuration;
import org.eclipse.californium.scandium.config.DtlsConfig;
import org.eclipse.leshan.core.endpoint.Protocol;
import org.eclipse.leshan.core.link.lwm2m.LwM2mLinkParser;
import org.eclipse.leshan.core.node.codec.DefaultLwM2mDecoder;
import org.eclipse.leshan.core.node.codec.DefaultLwM2mEncoder;
import org.eclipse.leshan.core.node.codec.LwM2mDecoder;
import org.eclipse.leshan.core.node.codec.LwM2mEncoder;
import org.eclipse.leshan.core.request.BootstrapDownlinkRequest;
import org.eclipse.leshan.server.bootstrap.BootstrapHandlerFactory;
import org.eclipse.leshan.server.bootstrap.BootstrapSessionManager;
import org.eclipse.leshan.server.bootstrap.InMemoryBootstrapConfigStore;
import org.eclipse.leshan.server.bootstrap.LeshanBootstrapServerBuilder;
import org.eclipse.leshan.server.bootstrap.endpoint.LwM2mBootstrapServerEndpointsProvider;
import org.eclipse.leshan.server.californium.bootstrap.endpoint.BootstrapServerProtocolProvider;
import org.eclipse.leshan.server.californium.bootstrap.endpoint.CaliforniumBootstrapServerEndpointFactory;
import org.eclipse.leshan.server.californium.bootstrap.endpoint.CaliforniumBootstrapServerEndpointsProvider;
import org.eclipse.leshan.server.californium.bootstrap.endpoint.coap.CoapBootstrapServerProtocolProvider;
import org.eclipse.leshan.server.californium.bootstrap.endpoint.coap.CoapOscoreBootstrapServerEndpointFactory;
import org.eclipse.leshan.server.californium.bootstrap.endpoint.coaps.CoapsBootstrapServerProtocolProvider;
import org.eclipse.leshan.server.security.BootstrapSecurityStore;
import org.eclipse.leshan.server.security.BootstrapSecurityStoreAdapter;
import org.eclipse.leshan.server.security.EditableSecurityStore;
import org.eclipse.leshan.server.security.ServerSecurityInfo;

public class LeshanTestBootstrapServerBuilder extends LeshanBootstrapServerBuilder {

    private Protocol protocolToUse;
    private String endpointProviderName;
    InMemoryBootstrapConfigStore configStore;
    private EditableSecurityStore editableSecurityStore;

    public LeshanTestBootstrapServerBuilder(Protocol protocolToUse) {
        this();
        using(protocolToUse);
    }

    public LeshanTestBootstrapServerBuilder() {
        setDecoder(new DefaultLwM2mDecoder(true));
        setEncoder(new DefaultLwM2mEncoder(true));

        configStore = new InMemoryBootstrapConfigStore();
        setConfigStore(configStore);
    }

    @Override
    public LeshanTestBootstrapServer build() {
        return (LeshanTestBootstrapServer) super.build();
    }

    @Override
    protected LeshanTestBootstrapServer createBootstrapServer(LwM2mBootstrapServerEndpointsProvider endpointsProvider,
            BootstrapSessionManager bsSessionManager, BootstrapHandlerFactory bsHandlerFactory, LwM2mEncoder encoder,
            LwM2mDecoder decoder, LwM2mLinkParser linkParser, BootstrapSecurityStore securityStore,
            ServerSecurityInfo serverSecurityInfo) {

        // create endpoint provider.
        if (endpointsProvider == null) {
            CaliforniumBootstrapServerEndpointsProvider.Builder builder;
            switch (endpointProviderName) {
            case "Californium":
                builder = new CaliforniumBootstrapServerEndpointsProvider.Builder(
                        getCaliforniumProtocolProvider(protocolToUse));
                builder.addEndpoint(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), protocolToUse);
                endpointsProvider = builder.build();
                break;
            case "Californium-OSCORE":
                builder = new CaliforniumBootstrapServerEndpointsProvider.Builder(
                        getCaliforniumProtocolProviderSupportingOscore(protocolToUse));
                builder.addEndpoint(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), protocolToUse);
                endpointsProvider = builder.build();
                break;
            default:
                throw new IllegalStateException(
                        String.format("Unknown endpoint provider : [%s]", endpointProviderName));
            }
        }

        return new LeshanTestBootstrapServer(endpointsProvider, bsSessionManager, bsHandlerFactory, encoder, decoder,
                linkParser, securityStore, serverSecurityInfo, //
                // arguments only needed for LeshanTestBootstrapServer
                configStore, editableSecurityStore);
    }

    public static LeshanTestBootstrapServerBuilder givenBootstrapServerUsing(Protocol protocolToUse) {
        return new LeshanTestBootstrapServerBuilder(protocolToUse);
    }

    public static LeshanTestBootstrapServerBuilder givenBootstrapServer() {
        return new LeshanTestBootstrapServerBuilder();
    }

    public LeshanTestBootstrapServerBuilder with(String endpointProvider) {
        this.endpointProviderName = endpointProvider;
        return this;
    }

    public LeshanTestBootstrapServerBuilder startingSessionWith(BootstrapDownlinkRequest<?> request) {
        TestBootstrapConfigStoreTaskProvider taskProvider = new TestBootstrapConfigStoreTaskProvider(configStore);
        taskProvider.startBootstrapSessionWith(request);
        setTaskProvider(taskProvider);
        return this;
    }

    public LeshanTestBootstrapServerBuilder using(Protocol protocol) {
        this.protocolToUse = protocol;
        return this;
    }

    public LeshanTestBootstrapServerBuilder using(PublicKey serverPublicKey, PrivateKey serverPrivateKey) {
        this.setPrivateKey(serverPrivateKey);
        this.setPublicKey(serverPublicKey);
        return this;
    }

    public LeshanTestBootstrapServerBuilder using(X509Certificate serverCertificate, PrivateKey serverPrivateKey) {
        this.setPrivateKey(serverPrivateKey);
        this.setCertificateChain(new X509Certificate[] { serverCertificate });
        return this;
    }

    public LeshanTestBootstrapServerBuilder using(PrivateKey serverPrivateKey, X509Certificate... certChain) {
        this.setPrivateKey(serverPrivateKey);
        this.setCertificateChain(certChain);
        return this;
    }

    public LeshanTestBootstrapServerBuilder trusting(X509Certificate... trustedCertificates) {
        this.setTrustedCertificates(trustedCertificates);
        return this;
    }

    public LeshanTestBootstrapServerBuilder with(EditableSecurityStore securityStore) {
        setSecurityStore(new BootstrapSecurityStoreAdapter(securityStore));
        this.editableSecurityStore = securityStore;
        return this;
    }

    private BootstrapServerProtocolProvider getCaliforniumProtocolProvider(Protocol protocol) {
        if (protocolToUse.equals(Protocol.COAP)) {
            return new CoapBootstrapServerProtocolProvider();
        } else if (protocolToUse.equals(Protocol.COAPS)) {
            return new CoapsBootstrapServerProtocolProvider() {
                @Override
                public void applyDefaultValue(Configuration configuration) {
                    super.applyDefaultValue(configuration);
                    configuration.set(DtlsConfig.DTLS_MAX_RETRANSMISSIONS, 1);
                    configuration.set(DtlsConfig.DTLS_RETRANSMISSION_TIMEOUT, 300, TimeUnit.MILLISECONDS);
                }
            };
        }
        throw new IllegalStateException(String.format("No Californium Protocol Provider for protocol %s", protocol));
    }

    private BootstrapServerProtocolProvider getCaliforniumProtocolProviderSupportingOscore(Protocol protocol) {
        if (protocolToUse.equals(Protocol.COAP)) {
            return new CoapBootstrapServerProtocolProvider() {
                @Override
                public CaliforniumBootstrapServerEndpointFactory createDefaultEndpointFactory(URI uri) {
                    return new CoapOscoreBootstrapServerEndpointFactory(uri);
                }
            };
        }
        throw new IllegalStateException(
                String.format("No Californium Protocol Provider supporting OSCORE for protocol %s", protocol));
    }
}
