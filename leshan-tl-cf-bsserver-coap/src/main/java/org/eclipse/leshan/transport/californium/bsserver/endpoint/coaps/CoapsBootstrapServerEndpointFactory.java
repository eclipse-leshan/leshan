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
package org.eclipse.leshan.transport.californium.bsserver.endpoint.coaps;

import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

import org.eclipse.californium.core.config.CoapConfig;
import org.eclipse.californium.core.config.CoapConfig.TrackerMode;
import org.eclipse.californium.core.network.CoapEndpoint;
import org.eclipse.californium.core.network.CoapEndpoint.Builder;
import org.eclipse.californium.elements.Connector;
import org.eclipse.californium.elements.EndpointContextMatcher;
import org.eclipse.californium.elements.PrincipalEndpointContextMatcher;
import org.eclipse.californium.elements.config.Configuration;
import org.eclipse.californium.elements.config.Configuration.ModuleDefinitionsProvider;
import org.eclipse.californium.elements.config.SystemConfig;
import org.eclipse.californium.elements.config.UdpConfig;
import org.eclipse.californium.scandium.DTLSConnector;
import org.eclipse.californium.scandium.config.DtlsConfig;
import org.eclipse.californium.scandium.config.DtlsConfig.DtlsRole;
import org.eclipse.californium.scandium.config.DtlsConnectorConfig;
import org.eclipse.californium.scandium.dtls.CertificateType;
import org.eclipse.californium.scandium.dtls.cipher.CipherSuite;
import org.eclipse.californium.scandium.dtls.x509.SingleCertificateProvider;
import org.eclipse.californium.scandium.dtls.x509.StaticCertificateVerifier;
import org.eclipse.leshan.bsserver.LeshanBootstrapServer;
import org.eclipse.leshan.core.endpoint.DefaultEndPointUriHandler;
import org.eclipse.leshan.core.endpoint.EndPointUriHandler;
import org.eclipse.leshan.core.endpoint.EndpointUri;
import org.eclipse.leshan.core.endpoint.Protocol;
import org.eclipse.leshan.servers.security.ServerSecurityInfo;
import org.eclipse.leshan.transport.californium.DefaultCoapsExceptionTranslator;
import org.eclipse.leshan.transport.californium.ExceptionTranslator;
import org.eclipse.leshan.transport.californium.Lwm2mEndpointContextMatcher;
import org.eclipse.leshan.transport.californium.bsserver.LwM2mBootstrapPskStore;
import org.eclipse.leshan.transport.californium.bsserver.endpoint.CaliforniumBootstrapServerEndpointFactory;
import org.eclipse.leshan.transport.californium.identity.DefaultCoapsIdentityHandler;
import org.eclipse.leshan.transport.californium.identity.IdentityHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CoapsBootstrapServerEndpointFactory implements CaliforniumBootstrapServerEndpointFactory {

    private static final Logger LOG = LoggerFactory.getLogger(CoapsBootstrapServerEndpointFactory.class);

    public static Protocol getSupportedProtocol() {
        return Protocol.COAPS;
    }

    @Override
    public String getEndpointDescription() {
        return "CoAP over DTLS endpoint based on Californium/Scandium library";
    }

    public static void applyDefaultValue(Configuration configuration) {
        configuration.set(CoapConfig.MID_TRACKER, TrackerMode.NULL);
        configuration.set(DtlsConfig.DTLS_ROLE, DtlsRole.SERVER_ONLY);
    }

    public static List<ModuleDefinitionsProvider> getModuleDefinitionsProviders() {
        return Arrays.asList(SystemConfig.DEFINITIONS, CoapConfig.DEFINITIONS, UdpConfig.DEFINITIONS,
                DtlsConfig.DEFINITIONS);
    }

    protected final EndpointUri endpointUri;
    protected final String loggingTagPrefix;
    protected final Configuration configuration;
    protected final Consumer<DtlsConnectorConfig.Builder> dtlsConnectorConfigInitializer;
    protected final Consumer<CoapEndpoint.Builder> coapEndpointConfigInitializer;
    protected final EndPointUriHandler uriHandler;

    public CoapsBootstrapServerEndpointFactory(EndpointUri uri) {
        this(uri, null, null, null, null, new DefaultEndPointUriHandler());
    }

    public CoapsBootstrapServerEndpointFactory(EndpointUri uri, String loggingTagPrefix, Configuration configuration,
            Consumer<org.eclipse.californium.scandium.config.DtlsConnectorConfig.Builder> dtlsConnectorConfigInitializer,
            Consumer<Builder> coapEndpointConfigInitializer, EndPointUriHandler uriHandler) {
        this.uriHandler = uriHandler;
        uriHandler.validateURI(uri);

        this.endpointUri = uri;
        this.loggingTagPrefix = loggingTagPrefix == null ? "Bootstrap Server" : loggingTagPrefix;
        this.configuration = configuration;
        this.dtlsConnectorConfigInitializer = dtlsConnectorConfigInitializer;
        this.coapEndpointConfigInitializer = coapEndpointConfigInitializer;
    }

    @Override
    public Protocol getProtocol() {
        return getSupportedProtocol();
    }

    @Override
    public EndpointUri getUri() {
        return endpointUri;
    }

    protected String getLoggingTag() {
        if (loggingTagPrefix != null) {
            return String.format("[%s-%s]", loggingTagPrefix, getUri().toString());
        } else {
            return String.format("[%s]", getUri().toString());
        }
    }

    @Override
    public CoapEndpoint createCoapEndpoint(Configuration defaultConfiguration, ServerSecurityInfo serverSecurityInfo,
            LeshanBootstrapServer server) {
        // we do no create coaps endpoint if server does have security store
        if (server.getSecurityStore() == null) {
            return null;
        }

        // defined Configuration to use
        Configuration configurationToUse;
        if (configuration == null) {
            // if specific configuration for this endpoint is null, used the default one which is the coapServer
            // Configuration shared with all endpoints by default.
            configurationToUse = defaultConfiguration;
        } else {
            configurationToUse = configuration;
        }

        // create DTLS connector Config
        DtlsConnectorConfig.Builder dtlsConfigBuilder = createDtlsConnectorConfigBuilder(configurationToUse);
        setUpDtlsConfig(dtlsConfigBuilder, uriHandler.getSocketAddr(endpointUri), serverSecurityInfo, server);
        DtlsConnectorConfig dtlsConfig;
        try {
            dtlsConfig = dtlsConfigBuilder.build();
        } catch (IllegalStateException e) {
            LOG.warn("Unable to create DTLS config for endpont {}.", endpointUri, e);
            return null;
        }

        // create CoAP endpoint
        return createEndpointBuilder(dtlsConfig, configurationToUse).build();
    }

    protected DtlsConnectorConfig.Builder createDtlsConnectorConfigBuilder(Configuration configuration) {
        DtlsConnectorConfig.Builder builder = new DtlsConnectorConfig.Builder(configuration);
        if (dtlsConnectorConfigInitializer != null)
            dtlsConnectorConfigInitializer.accept(builder);
        return builder;
    }

    protected void setUpDtlsConfig(DtlsConnectorConfig.Builder dtlsConfigBuilder, InetSocketAddress address,
            ServerSecurityInfo serverSecurityInfo, LeshanBootstrapServer server) {

        // Set default DTLS setting for Leshan unless user change it.
        DtlsConnectorConfig incompleteConfig = dtlsConfigBuilder.getIncompleteConfig();

        // Handle PSK Store
        if (incompleteConfig.getPskStore() != null) {
            LOG.warn("PskStore should be automatically set by Leshan. Using a custom implementation is not advised.");
        } else if (server.getSecurityStore() != null) {
            List<CipherSuite> ciphers = incompleteConfig.getConfiguration().get(DtlsConfig.DTLS_CIPHER_SUITES);
            if (ciphers == null // if null ciphers will be chosen automatically by Scandium
                    || CipherSuite.containsPskBasedCipherSuite(ciphers)) {
                dtlsConfigBuilder.setPskStore(new LwM2mBootstrapPskStore(server.getSecurityStore()));
            }
        }

        // Handle secure address
        if (incompleteConfig.getAddress() == null) {
            dtlsConfigBuilder.setAddress(address);
        } else if (address != null && !address.equals(incompleteConfig.getAddress())) {
            throw new IllegalStateException(String.format(
                    "Configuration conflict between LeshanBuilder and DtlsConnectorConfig.Builder for secure address: %s != %s",
                    address, incompleteConfig.getAddress()));
        }

        // check conflict in configuration
        if (incompleteConfig.getCertificateIdentityProvider() != null) {
            if (serverSecurityInfo.getPrivateKey() != null) {
                throw new IllegalStateException(
                        "Configuration conflict between LeshanBuilder and DtlsConnectorConfig.Builder for private key");
            }
            if (serverSecurityInfo.getPublicKey() != null) {
                throw new IllegalStateException(
                        "Configuration conflict between LeshanBuilder and DtlsConnectorConfig.Builder for public key");
            }
            if (serverSecurityInfo.getCertificateChain() != null) {
                throw new IllegalStateException(
                        "Configuration conflict between LeshanBuilder and DtlsConnectorConfig.Builder for certificate chain");
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
        if (incompleteConfig.getCertificateVerifier() != null) {
            if (serverSecurityInfo.getTrustedCertificates() != null) {
                throw new IllegalStateException(
                        "Configuration conflict between LeshanBuilder and DtlsConnectorConfig.Builder: if a AdvancedCertificateVerifier is set, trustedCertificates must not be set.");
            }
        } else if (incompleteConfig.getCertificateIdentityProvider() != null) {
            StaticCertificateVerifier.Builder verifierBuilder = StaticCertificateVerifier.builder();
            // by default trust all RPK
            verifierBuilder.setTrustAllRPKs();
            if (serverSecurityInfo.getTrustedCertificates() != null) {
                verifierBuilder.setTrustedCertificates(serverSecurityInfo.getTrustedCertificates());
            }
            dtlsConfigBuilder.setCertificateVerifier(verifierBuilder.build());
        }
    }

    /**
     * This method is intended to be overridden.
     *
     * @param dtlsConfig the DTLS config used to create this endpoint.
     * @param coapConfig the CoAP config used to create this endpoint.
     * @return the {@link Builder} used for secured communication.
     */
    protected CoapEndpoint.Builder createEndpointBuilder(DtlsConnectorConfig dtlsConfig, Configuration coapConfig) {
        CoapEndpoint.Builder builder = new CoapEndpoint.Builder();

        builder.setConnector(createConnector(dtlsConfig));
        builder.setConfiguration(coapConfig);
        builder.setLoggingTag(getLoggingTag());
        builder.setEndpointContextMatcher(createEndpointContextMatcher());

        if (coapEndpointConfigInitializer != null)
            coapEndpointConfigInitializer.accept(builder);
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
    protected EndpointContextMatcher createEndpointContextMatcher() {
        return new Lwm2mEndpointContextMatcher();
    }

    @Override
    public IdentityHandler createIdentityHandler() {
        return new DefaultCoapsIdentityHandler();
    }

    /**
     * By default create a {@link DTLSConnector}.
     * <p>
     * This method is intended to be overridden.
     *
     * @param dtlsConfig the DTLS config used to create the Secured Connector.
     * @return the {@link Connector} used for unsecured {@link CoapEndpoint}
     */
    protected Connector createConnector(DtlsConnectorConfig dtlsConfig) {
        return new DTLSConnector(dtlsConfig);
    }

    @Override
    public ExceptionTranslator createExceptionTranslator() {
        return new DefaultCoapsExceptionTranslator();
    }
}
