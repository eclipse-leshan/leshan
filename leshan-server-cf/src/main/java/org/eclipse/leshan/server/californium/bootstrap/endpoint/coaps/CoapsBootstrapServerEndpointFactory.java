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
package org.eclipse.leshan.server.californium.bootstrap.endpoint.coaps;

import java.net.InetSocketAddress;
import java.net.URI;
import java.security.Principal;
import java.security.PublicKey;
import java.util.List;

import javax.security.auth.x500.X500Principal;

import org.eclipse.californium.core.coap.Message;
import org.eclipse.californium.core.coap.Request;
import org.eclipse.californium.core.network.CoapEndpoint;
import org.eclipse.californium.core.network.CoapEndpoint.Builder;
import org.eclipse.californium.elements.AddressEndpointContext;
import org.eclipse.californium.elements.Connector;
import org.eclipse.californium.elements.DtlsEndpointContext;
import org.eclipse.californium.elements.EndpointContext;
import org.eclipse.californium.elements.EndpointContextMatcher;
import org.eclipse.californium.elements.MapBasedEndpointContext;
import org.eclipse.californium.elements.MapBasedEndpointContext.Attributes;
import org.eclipse.californium.elements.PrincipalEndpointContextMatcher;
import org.eclipse.californium.elements.auth.PreSharedKeyIdentity;
import org.eclipse.californium.elements.auth.RawPublicKeyIdentity;
import org.eclipse.californium.elements.auth.X509CertPath;
import org.eclipse.californium.elements.config.Configuration;
import org.eclipse.californium.scandium.DTLSConnector;
import org.eclipse.californium.scandium.config.DtlsConfig;
import org.eclipse.californium.scandium.config.DtlsConnectorConfig;
import org.eclipse.californium.scandium.dtls.CertificateType;
import org.eclipse.californium.scandium.dtls.DtlsHandshakeTimeoutException;
import org.eclipse.californium.scandium.dtls.cipher.CipherSuite;
import org.eclipse.californium.scandium.dtls.x509.SingleCertificateProvider;
import org.eclipse.californium.scandium.dtls.x509.StaticNewAdvancedCertificateVerifier;
import org.eclipse.leshan.core.californium.DefaultExceptionTranslator;
import org.eclipse.leshan.core.californium.EndpointContextUtil;
import org.eclipse.leshan.core.californium.ExceptionTranslator;
import org.eclipse.leshan.core.californium.Lwm2mEndpointContextMatcher;
import org.eclipse.leshan.core.californium.identity.IdentityHandler;
import org.eclipse.leshan.core.endpoint.EndpointUriUtil;
import org.eclipse.leshan.core.endpoint.Protocol;
import org.eclipse.leshan.core.request.Identity;
import org.eclipse.leshan.core.request.exception.TimeoutException;
import org.eclipse.leshan.core.request.exception.TimeoutException.Type;
import org.eclipse.leshan.server.bootstrap.LeshanBootstrapServer;
import org.eclipse.leshan.server.californium.ConnectionCleaner;
import org.eclipse.leshan.server.californium.bootstrap.LwM2mBootstrapPskStore;
import org.eclipse.leshan.server.californium.bootstrap.endpoint.CaliforniumBootstrapServerEndpointFactory;
import org.eclipse.leshan.server.security.EditableSecurityStore;
import org.eclipse.leshan.server.security.SecurityInfo;
import org.eclipse.leshan.server.security.SecurityStore;
import org.eclipse.leshan.server.security.SecurityStoreListener;
import org.eclipse.leshan.server.security.ServerSecurityInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CoapsBootstrapServerEndpointFactory implements CaliforniumBootstrapServerEndpointFactory {

    private static final Logger LOG = LoggerFactory.getLogger(CoapsBootstrapServerEndpointFactory.class);

    private final String loggingTag = null;
    private final URI uri;

    public CoapsBootstrapServerEndpointFactory(URI uri) {
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
    public CoapEndpoint createCoapEndpoint(Configuration defaultConfiguration, ServerSecurityInfo serverSecurityInfo,
            LeshanBootstrapServer server) {
        if (server.getSecurityStore() == null) {
            return null;
        }
        CoapEndpoint endpoint = createSecuredEndpointBuilder(handle(EndpointUriUtil.getSocketAddr(uri),
                createDtlsConfigBuilder(defaultConfiguration), defaultConfiguration, serverSecurityInfo, server),
                defaultConfiguration).build();

        return endpoint;
    }

    protected DtlsConnectorConfig.Builder createDtlsConfigBuilder(Configuration configuration) {
        return new DtlsConnectorConfig.Builder(configuration);
    }

    protected DtlsConnectorConfig handle(InetSocketAddress localSecureAddress,
            DtlsConnectorConfig.Builder dtlsConfigBuilder, Configuration coapConfig,
            ServerSecurityInfo serverSecurityInfo, LeshanBootstrapServer server) {

        // Set default DTLS setting for Leshan unless user change it.
        DtlsConnectorConfig incompleteConfig = dtlsConfigBuilder.getIncompleteConfig();

        // Handle PSK Store
        if (incompleteConfig.getAdvancedPskStore() != null) {
            LOG.warn("PskStore should be automatically set by Leshan. Using a custom implementation is not advised.");
        } else if (server.getSecurityStore() != null) {
            List<CipherSuite> ciphers = incompleteConfig.getConfiguration().get(DtlsConfig.DTLS_CIPHER_SUITES);
            if (ciphers == null // if null ciphers will be chosen automatically by Scandium
                    || CipherSuite.containsPskBasedCipherSuite(ciphers)) {
                dtlsConfigBuilder.setAdvancedPskStore(new LwM2mBootstrapPskStore(server.getSecurityStore()));
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
            Configuration coapConfig) {
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

    @Override
    public IdentityHandler createIdentityHandler() {
        return new IdentityHandler() {

            @Override
            public Identity getIdentity(Message receivedMessage) {
                EndpointContext context = receivedMessage.getSourceContext();
                InetSocketAddress peerAddress = context.getPeerAddress();
                Principal senderIdentity = context.getPeerIdentity();
                if (senderIdentity != null) {
                    if (senderIdentity instanceof PreSharedKeyIdentity) {
                        return Identity.psk(peerAddress, ((PreSharedKeyIdentity) senderIdentity).getIdentity());
                    } else if (senderIdentity instanceof RawPublicKeyIdentity) {
                        PublicKey publicKey = ((RawPublicKeyIdentity) senderIdentity).getKey();
                        return Identity.rpk(peerAddress, publicKey);
                    } else if (senderIdentity instanceof X500Principal || senderIdentity instanceof X509CertPath) {
                        // Extract common name
                        String x509CommonName = EndpointContextUtil.extractCN(senderIdentity.getName());
                        return Identity.x509(peerAddress, x509CommonName);
                    }
                    throw new IllegalStateException(
                            String.format("Unable to extract sender identity : unexpected type of Principal %s [%s]",
                                    senderIdentity.getClass(), senderIdentity.toString()));
                }
                return null;
            }

            @Override
            public EndpointContext createEndpointContext(Identity identity, boolean allowConnectionInitiation) {
                Principal peerIdentity = null;
                if (identity != null) {
                    if (identity.isPSK()) {
                        peerIdentity = new PreSharedKeyIdentity(identity.getPskIdentity());
                    } else if (identity.isRPK()) {
                        peerIdentity = new RawPublicKeyIdentity(identity.getRawPublicKey());
                    } else if (identity.isX509()) {
                        /* simplify distinguished name to CN= part */
                        peerIdentity = new X500Principal("CN=" + identity.getX509CommonName());
                    }
                }
                if (peerIdentity != null && allowConnectionInitiation) {
                    return new MapBasedEndpointContext(identity.getPeerAddress(), peerIdentity, new Attributes()
                            .add(DtlsEndpointContext.KEY_HANDSHAKE_MODE, DtlsEndpointContext.HANDSHAKE_MODE_AUTO));
                }
                return new AddressEndpointContext(identity.getPeerAddress(), peerIdentity);
            }
        };
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

    @Override
    public ExceptionTranslator createExceptionTranslator() {
        return new DefaultExceptionTranslator() {
            @Override
            public Exception translate(Request coapRequest, Throwable error) {
                if (error instanceof DtlsHandshakeTimeoutException) {
                    return new TimeoutException(Type.DTLS_HANDSHAKE_TIMEOUT, error,
                            "Request %s timeout : dtls handshake timeout", coapRequest.getURI());
                } else {
                    return super.translate(coapRequest, error);
                }
            }
        };
    }
}
