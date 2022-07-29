/*******************************************************************************
 * Copyright (c) 2022 Sierra Wireless and others.
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
package org.eclipse.leshan.server.californium.endpoint.coaps;

import java.net.InetSocketAddress;
import java.net.URI;
import java.util.List;

import org.eclipse.californium.core.network.CoapEndpoint;
import org.eclipse.californium.core.network.CoapEndpoint.Builder;
import org.eclipse.californium.core.network.Endpoint;
import org.eclipse.californium.core.network.serialization.UdpDataParser;
import org.eclipse.californium.core.network.serialization.UdpDataSerializer;
import org.eclipse.californium.core.observe.ObservationStore;
import org.eclipse.californium.elements.Connector;
import org.eclipse.californium.elements.EndpointContextMatcher;
import org.eclipse.californium.elements.PrincipalEndpointContextMatcher;
import org.eclipse.californium.elements.config.Configuration;
import org.eclipse.californium.scandium.DTLSConnector;
import org.eclipse.californium.scandium.config.DtlsConfig;
import org.eclipse.californium.scandium.config.DtlsConnectorConfig;
import org.eclipse.californium.scandium.dtls.CertificateType;
import org.eclipse.californium.scandium.dtls.cipher.CipherSuite;
import org.eclipse.californium.scandium.dtls.x509.SingleCertificateProvider;
import org.eclipse.californium.scandium.dtls.x509.StaticNewAdvancedCertificateVerifier;
import org.eclipse.leshan.core.californium.Lwm2mEndpointContextMatcher;
import org.eclipse.leshan.core.endpoint.EndpointUriUtil;
import org.eclipse.leshan.server.californium.ConnectionCleaner;
import org.eclipse.leshan.server.californium.LwM2mPskStore;
import org.eclipse.leshan.server.californium.endpoint.CaliforniumEndpointFactory;
import org.eclipse.leshan.server.californium.observation.LwM2mObservationStore;
import org.eclipse.leshan.server.californium.observation.ObservationSerDes;
import org.eclipse.leshan.server.endpoint.LwM2mNotificationReceiver;
import org.eclipse.leshan.server.endpoint.LwM2mServer;
import org.eclipse.leshan.server.endpoint.Protocol;
import org.eclipse.leshan.server.endpoint.ServerSecurityInfo;
import org.eclipse.leshan.server.security.EditableSecurityStore;
import org.eclipse.leshan.server.security.SecurityInfo;
import org.eclipse.leshan.server.security.SecurityStore;
import org.eclipse.leshan.server.security.SecurityStoreListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CoapsEndpointFactory implements CaliforniumEndpointFactory {

    private static final Logger LOG = LoggerFactory.getLogger(CoapsEndpointFactory.class);

    private final String loggingTag = null;
    private final URI uri;

    public CoapsEndpointFactory(URI uri) {
        this.uri = uri;

    }

    @Override
    public URI getUri() {
        return uri;
    }

    @Override
    public Protocol getProtocol() {
        return Protocol.COAPS;
    }

    @Override
    public Endpoint createEndpoint(Configuration defaultConfiguration, ServerSecurityInfo serverSecurityInfo,
            LwM2mServer server, LwM2mNotificationReceiver notificationReceiver) {
        if (server.getSecurityStore() == null) {
            return null;
        }
        CoapEndpoint endpoint = createSecuredEndpointBuilder(
                handle(EndpointUriUtil.getSocketAddr(uri), createDtlsConfigBuilder(defaultConfiguration),
                        defaultConfiguration, server, serverSecurityInfo),
                defaultConfiguration, new LwM2mObservationStore(server.getRegistrationStore(), notificationReceiver,
                        new ObservationSerDes(new UdpDataParser(), new UdpDataSerializer()))).build();

        createConnectionCleaner(server.getSecurityStore(), endpoint);
        return endpoint;
    }

    protected DtlsConnectorConfig.Builder createDtlsConfigBuilder(Configuration configuration) {
        return new DtlsConnectorConfig.Builder(configuration);
    }

    protected DtlsConnectorConfig handle(InetSocketAddress localSecureAddress,
            DtlsConnectorConfig.Builder dtlsConfigBuilder, Configuration coapConfig, LwM2mServer server,
            ServerSecurityInfo serverSecurityInfo) {

        // Set default DTLS setting for Leshan unless user change it.
        DtlsConnectorConfig incompleteConfig = dtlsConfigBuilder.getIncompleteConfig();

        // Handle PSK Store
        if (incompleteConfig.getAdvancedPskStore() != null) {
            LOG.warn("PskStore should be automatically set by Leshan. Using a custom implementation is not advised.");
        } else if (server.getSecurityStore() != null) {
            List<CipherSuite> ciphers = incompleteConfig.getConfiguration().get(DtlsConfig.DTLS_CIPHER_SUITES);
            if (ciphers == null // if null, ciphers will be chosen automatically by Scandium
                    || CipherSuite.containsPskBasedCipherSuite(ciphers)) {
                dtlsConfigBuilder.setAdvancedPskStore(
                        new LwM2mPskStore(server.getSecurityStore(), server.getRegistrationStore()));
            }
        }

        // Handle secure address
        if (incompleteConfig.getAddress() == null) {
            dtlsConfigBuilder.setAddress(localSecureAddress);
        } else if (localSecureAddress != null && !localSecureAddress.equals(incompleteConfig.getAddress())) {
            throw new IllegalStateException(String.format(
                    "Configuration conflict between LeshanBuilder and DtlsConnectorConfig.Builder for secure address: %s != %s",
                    localSecureAddress, incompleteConfig.getAddress()));
        }

        // check conflict in configuration
        if (incompleteConfig.getCertificateIdentityProvider() != null) {
            if (serverSecurityInfo.getPrivateKey() != null) {
                throw new IllegalStateException(String.format(
                        "Configuration conflict between LeshanBuilder and DtlsConnectorConfig.Builder for private key"));
            }
            if (serverSecurityInfo.getPublicKey() != null) {
                throw new IllegalStateException(String.format(
                        "Configuration conflict between LeshanBuilder and DtlsConnectorConfig.Builder for public key"));
            }
            if (serverSecurityInfo.getCertificateChain() != null) {
                throw new IllegalStateException(String.format(
                        "Configuration conflict between LeshanBuilder and DtlsConnectorConfig.Builder for certificate chain"));
            }
        } else if (serverSecurityInfo.getPrivateKey() != null) {
            // if in raw key mode and not in X.509 set the raw keys
            if (serverSecurityInfo.getCertificateChain() == null && serverSecurityInfo.getPublicKey() != null) {

                dtlsConfigBuilder.setCertificateIdentityProvider(new SingleCertificateProvider(
                        serverSecurityInfo.getPrivateKey(), serverSecurityInfo.getPublicKey()));
            }
            // if in X.509 mode set the private key, certificate chain, public key is extracted from the certificate
            if (serverSecurityInfo.getCertificateChain() != null
                    && serverSecurityInfo.getCertificateChain().length > 0) {

                dtlsConfigBuilder.setCertificateIdentityProvider(new SingleCertificateProvider(
                        serverSecurityInfo.getPrivateKey(), serverSecurityInfo.getCertificateChain(),
                        CertificateType.X_509, CertificateType.RAW_PUBLIC_KEY));
            }
        }

        // handle trusted certificates or RPK
        if (incompleteConfig.getAdvancedCertificateVerifier() != null) {
            if (serverSecurityInfo.getTrustedCertificates() != null) {
                throw new IllegalStateException(
                        "Configuration conflict between LeshanBuilder and DtlsConnectorConfig.Builder: if a AdvancedCertificateVerifier is set, trustedCertificates must not be set.");
            }
        } else if (incompleteConfig.getCertificateIdentityProvider() != null) {
            StaticNewAdvancedCertificateVerifier.Builder verifierBuilder = StaticNewAdvancedCertificateVerifier
                    .builder();
            // by default trust all RPK
            verifierBuilder.setTrustAllRPKs();
            if (serverSecurityInfo.getTrustedCertificates() != null) {
                verifierBuilder.setTrustedCertificates(serverSecurityInfo.getTrustedCertificates());
            }
            dtlsConfigBuilder.setAdvancedCertificateVerifier(verifierBuilder.build());
        }

        // we try to build the dtlsConfig, if it fail we will just not create the secured endpoint
        try {
            return dtlsConfigBuilder.build();
        } catch (IllegalStateException e) {
            LOG.warn("Unable to create DTLS config and so secured endpoint.", e);
        }

        return null;
    }

    /**
     * This method is intended to be overridden.
     *
     * @param dtlsConfig the DTLS config used to create this endpoint.
     * @param coapConfig the CoAP config used to create this endpoint.
     * @param store the CoAP observation store used to create this endpoint.
     * @return the {@link Builder} used for secured communication.
     */
    protected CoapEndpoint.Builder createSecuredEndpointBuilder(DtlsConnectorConfig dtlsConfig,
            Configuration coapConfig, ObservationStore store) {
        CoapEndpoint.Builder builder = new CoapEndpoint.Builder();
        builder.setConnector(createSecuredConnector(dtlsConfig));
        builder.setConfiguration(coapConfig);
        if (loggingTag != null) {
            builder.setLoggingTag("[" + loggingTag + "-coaps://]");
        } else {
            builder.setLoggingTag("[coaps://]");
        }
        EndpointContextMatcher securedContextMatcher = createSecuredContextMatcher();
        builder.setEndpointContextMatcher(securedContextMatcher);
        if (store != null) {
            builder.setObservationStore(store);
        }
        return builder;
    }

    /**
     * For server {@link Lwm2mEndpointContextMatcher} is created. <br>
     * For client {@link PrincipalEndpointContextMatcher} is created.
     * <p>
     * This method is intended to be overridden.
     *
     * @return the {@link EndpointContextMatcher} used for secured communication
     */
    protected EndpointContextMatcher createSecuredContextMatcher() {
        return new Lwm2mEndpointContextMatcher();
    }

    /**
     * By default create a {@link DTLSConnector}.
     * <p>
     * This method is intended to be overridden.
     *
     * @param dtlsConfig the DTLS config used to create the Secured Connector.
     * @return the {@link Connector} used for unsecured {@link CoapEndpoint}
     */
    protected Connector createSecuredConnector(DtlsConnectorConfig dtlsConfig) {
        return new DTLSConnector(dtlsConfig);
    }

    protected void createConnectionCleaner(SecurityStore securityStore, CoapEndpoint securedEndpoint) {
        if (securedEndpoint != null && securedEndpoint.getConnector() instanceof DTLSConnector
                && securityStore instanceof EditableSecurityStore) {

            final ConnectionCleaner connectionCleaner = new ConnectionCleaner(
                    (DTLSConnector) securedEndpoint.getConnector());

            ((EditableSecurityStore) securityStore).addListener(new SecurityStoreListener() {
                @Override
                public void securityInfoRemoved(boolean infosAreCompromised, SecurityInfo... infos) {
                    if (infosAreCompromised) {
                        connectionCleaner.cleanConnectionFor(infos);
                    }
                }
            });
        }
    }
}
