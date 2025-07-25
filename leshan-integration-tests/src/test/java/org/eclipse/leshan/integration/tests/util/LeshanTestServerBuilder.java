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
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.eclipse.californium.elements.config.Configuration;
import org.eclipse.californium.scandium.config.DtlsConfig;
import org.eclipse.californium.scandium.config.DtlsConfig.DtlsRole;
import org.eclipse.leshan.core.endpoint.EndPointUriHandler;
import org.eclipse.leshan.core.endpoint.EndpointUri;
import org.eclipse.leshan.core.endpoint.Protocol;
import org.eclipse.leshan.core.link.lwm2m.LwM2mLinkParser;
import org.eclipse.leshan.core.node.codec.DefaultLwM2mDecoder;
import org.eclipse.leshan.core.node.codec.DefaultLwM2mEncoder;
import org.eclipse.leshan.core.node.codec.LwM2mDecoder;
import org.eclipse.leshan.core.node.codec.LwM2mEncoder;
import org.eclipse.leshan.integration.tests.util.cf.CertPair;
import org.eclipse.leshan.integration.tests.util.cf.MapBasedCertificateProvider;
import org.eclipse.leshan.integration.tests.util.cf.MapBasedRawPublicKeyProvider;
import org.eclipse.leshan.server.LeshanServerBuilder;
import org.eclipse.leshan.server.endpoint.DefaultCompositeServerEndpointsProvider;
import org.eclipse.leshan.server.endpoint.LwM2mServerEndpointsProvider;
import org.eclipse.leshan.server.model.LwM2mModelProvider;
import org.eclipse.leshan.server.model.VersionedModelProvider;
import org.eclipse.leshan.server.queue.ClientAwakeTimeProvider;
import org.eclipse.leshan.server.queue.StaticClientAwakeTimeProvider;
import org.eclipse.leshan.server.registration.RegistrationDataExtractor;
import org.eclipse.leshan.server.registration.RegistrationIdProvider;
import org.eclipse.leshan.server.registration.RegistrationStore;
import org.eclipse.leshan.server.security.Authorizer;
import org.eclipse.leshan.servers.ServerEndpointNameProvider;
import org.eclipse.leshan.servers.security.EditableSecurityStore;
import org.eclipse.leshan.servers.security.SecurityStore;
import org.eclipse.leshan.servers.security.ServerSecurityInfo;
import org.eclipse.leshan.transport.californium.server.endpoint.CaliforniumServerEndpointFactory;
import org.eclipse.leshan.transport.californium.server.endpoint.CaliforniumServerEndpointsProvider;
import org.eclipse.leshan.transport.californium.server.endpoint.ServerProtocolProvider;
import org.eclipse.leshan.transport.californium.server.endpoint.coap.CoapOscoreServerEndpointFactory;
import org.eclipse.leshan.transport.californium.server.endpoint.coap.CoapServerProtocolProvider;
import org.eclipse.leshan.transport.californium.server.endpoint.coaps.CoapsServerProtocolProvider;
import org.eclipse.leshan.transport.javacoap.server.coaps.bc.endpoint.JavaCoapsServerEndpointsProvider;
import org.eclipse.leshan.transport.javacoap.server.coaptcp.endpoint.JavaCoapTcpServerEndpointsProvider;
import org.eclipse.leshan.transport.javacoap.server.coaptcp.endpoint.JavaCoapsTcpServerEndpointsProvider;
import org.eclipse.leshan.transport.javacoap.server.endpoint.JavaCoapServerEndpointsProvider;

public class LeshanTestServerBuilder extends LeshanServerBuilder {

    private Protocol protocolToUse;
    private final List<String> endpointProviderNames = new ArrayList<>();
    private boolean serverOnly = false;
    private boolean useSNI = false;
    private final Map<String, KeyPair> keyPairs = new HashMap<>();
    private final Map<String, CertPair> CertPairs = new HashMap<>();

    public LeshanTestServerBuilder(Protocol protocolToUse) {
        this();
        using(protocolToUse);
    }

    public LeshanTestServerBuilder() {
        setDecoder(new DefaultLwM2mDecoder(true));
        setEncoder(new DefaultLwM2mEncoder(true));
        setObjectModelProvider(new VersionedModelProvider(TestObjectLoader.loadDefaultObject()));
    }

    @Override
    public LeshanTestServer build() {
        return (LeshanTestServer) super.build();
    }

    @Override
    protected LeshanTestServer createServer(LwM2mServerEndpointsProvider endpointsProvider,
            RegistrationStore registrationStore, SecurityStore securityStore, Authorizer authorizer,
            LwM2mModelProvider modelProvider, LwM2mEncoder encoder, LwM2mDecoder decoder, boolean noQueueMode,
            ClientAwakeTimeProvider awakeTimeProvider, RegistrationIdProvider registrationIdProvider,
            RegistrationDataExtractor registrationDataExtractor, LwM2mLinkParser linkParser,
            EndPointUriHandler uriHandler, ServerSecurityInfo serverSecurityInfo,
            ServerEndpointNameProvider endpointNameProvider, boolean updateRegistrationOnNotification,
            boolean updateRegistrationOnSend) {

        // create endpoint provider.
        if (endpointsProvider == null) {
            List<LwM2mServerEndpointsProvider> providers = new ArrayList<>();

            for (String endpointProviderName : endpointProviderNames) {
                CaliforniumServerEndpointsProvider.Builder builder;
                switch (endpointProviderName) {
                case "Californium":
                    builder = new CaliforniumServerEndpointsProvider.Builder(
                            getCaliforniumProtocolProvider(protocolToUse));
                    builder.addEndpoint(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), protocolToUse);
                    endpointsProvider = builder.build();
                    break;
                case "Californium-OSCORE":
                    builder = new CaliforniumServerEndpointsProvider.Builder(
                            getCaliforniumProtocolProviderSupportingOscore(protocolToUse));
                    builder.addEndpoint(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), protocolToUse);
                    endpointsProvider = builder.build();
                    break;
                case "java-coap":
                    endpointsProvider = getJavaCoapProtocolProvider(protocolToUse);
                    break;
                default:
                    throw new IllegalStateException(
                            String.format("Unknown endpoint provider : [%s]", endpointProviderName));
                }
                providers.add(endpointsProvider);
            }
            endpointsProvider = new DefaultCompositeServerEndpointsProvider(providers);
        }
        return new LeshanTestServer(endpointsProvider, registrationStore, securityStore, authorizer, modelProvider,
                encoder, decoder, noQueueMode, awakeTimeProvider, registrationIdProvider, registrationDataExtractor,
                linkParser, uriHandler, serverSecurityInfo, endpointNameProvider, updateRegistrationOnNotification,
                updateRegistrationOnSend);
    }

    public static LeshanTestServerBuilder givenServerUsing(Protocol protocolToUse) {
        return new LeshanTestServerBuilder(protocolToUse);
    }

    public static LeshanTestServerBuilder givenServer() {
        return new LeshanTestServerBuilder();
    }

    public LeshanTestServerBuilder with(String... endpointProviders) {
        for (String endpointProvider : endpointProviders) {
            this.endpointProviderNames.add(endpointProvider);
        }
        return this;
    }

    public LeshanTestServerBuilder actingAsServerOnly() {
        this.serverOnly = true;
        return this;
    }

    public LeshanTestServerBuilder usingSni() {
        this.useSNI = true;
        return this;
    }

    public LeshanTestServerBuilder using(Protocol protocol) {
        this.protocolToUse = protocol;
        return this;
    }

    public LeshanTestServerBuilder using(PublicKey serverPublicKey, PrivateKey serverPrivateKey) {
        this.setPrivateKey(serverPrivateKey);
        this.setPublicKey(serverPublicKey);
        return this;
    }

    public LeshanTestServerBuilder using(String host, PublicKey serverPublicKey, PrivateKey serverPrivateKey) {
        keyPairs.put(host, new KeyPair(serverPublicKey, serverPrivateKey));
        return this;
    }

    public LeshanTestServerBuilder using(String host, PrivateKey serverPrivateKey, X509Certificate... certChain) {
        CertPairs.put(host, new CertPair(certChain, serverPrivateKey));
        return this;
    }

    public LeshanTestServerBuilder using(X509Certificate serverCertificate, PrivateKey serverPrivateKey) {
        this.setPrivateKey(serverPrivateKey);
        this.setCertificateChain(new X509Certificate[] { serverCertificate });
        return this;
    }

    public LeshanTestServerBuilder using(PrivateKey serverPrivateKey, X509Certificate... certChain) {
        this.setPrivateKey(serverPrivateKey);
        this.setCertificateChain(certChain);
        return this;
    }

    public LeshanTestServerBuilder trusting(X509Certificate... trustedCertificates) {
        this.setTrustedCertificates(trustedCertificates);
        return this;
    }

    public LeshanTestServerBuilder withAwakeTime(long clientAwakeTime, TimeUnit unit) {
        setClientAwakeTimeProvider(new StaticClientAwakeTimeProvider((int) unit.toMillis(clientAwakeTime)));
        return this;
    }

    public LeshanTestServerBuilder with(EditableSecurityStore securityStore) {
        setSecurityStore(securityStore);
        return this;
    }

    public LeshanTestServerBuilder withUpdateOnNotification() {
        setUpdateRegistrationOnNotification(true);
        return this;
    }

    public LeshanTestServerBuilder withUpdateOnSendOperation() {
        setUpdateRegistrationOnSend(true);
        return this;
    }

    public LeshanTestServerBuilder withRedisRegistrationStore() {
        setRegistrationStore(RedisTestUtil.createRedisRegistrationStore());
        return this;
    }

    protected ServerProtocolProvider getCaliforniumProtocolProvider(Protocol protocol) {
        if (protocol.equals(Protocol.COAP)) {
            return new CoapServerProtocolProvider();
        } else if (protocol.equals(Protocol.COAPS)) {
            return new CoapsServerProtocolProvider(dtlsConfig -> {
                if (!keyPairs.isEmpty()) {
                    dtlsConfig.setCertificateIdentityProvider(new MapBasedRawPublicKeyProvider(keyPairs));
                } else if (!CertPairs.isEmpty()) {
                    dtlsConfig.setCertificateIdentityProvider(new MapBasedCertificateProvider(CertPairs));
                }
            }) {

                @Override
                public void applyDefaultValue(Configuration configuration) {
                    super.applyDefaultValue(configuration);
                    if (serverOnly) {
                        configuration.set(DtlsConfig.DTLS_ROLE, DtlsRole.SERVER_ONLY);
                    }
                    configuration.set(DtlsConfig.DTLS_MAX_RETRANSMISSIONS, 1);
                    configuration.set(DtlsConfig.DTLS_RETRANSMISSION_TIMEOUT, 300, TimeUnit.MILLISECONDS);
                    if (useSNI) {
                        configuration.set(DtlsConfig.DTLS_USE_SERVER_NAME_INDICATION, true);
                    }
                }
            };
        }
        throw new IllegalStateException(String.format("No Californium Protocol Provider for protocol %s", protocol));
    }

    private ServerProtocolProvider getCaliforniumProtocolProviderSupportingOscore(Protocol protocol) {
        if (protocol.equals(Protocol.COAP)) {
            return new CoapServerProtocolProvider() {
                @Override
                public CaliforniumServerEndpointFactory createDefaultEndpointFactory(EndpointUri uri,
                        EndPointUriHandler uriHandler) {
                    return new CoapOscoreServerEndpointFactory(uri);
                }
            };
        }
        throw new IllegalStateException(
                String.format("No Californium Protocol Provider supporting OSCORE for protocol %s", protocol));
    }

    protected LwM2mServerEndpointsProvider getJavaCoapProtocolProvider(Protocol protocol) {
        if (protocol.equals(Protocol.COAP)) {
            return new JavaCoapServerEndpointsProvider(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0));
        } else if (protocol.equals(Protocol.COAPS)) {
            return new JavaCoapsServerEndpointsProvider(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0));
        } else if (protocol.equals(Protocol.COAP_TCP)) {
            return new JavaCoapTcpServerEndpointsProvider(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0));
        } else if (protocol.equals(Protocol.COAPS_TCP)) {
            return new JavaCoapsTcpServerEndpointsProvider(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0));
        }
        throw new IllegalStateException(String.format("No Californium Protocol Provider for protocol %s", protocol));
    }
}
