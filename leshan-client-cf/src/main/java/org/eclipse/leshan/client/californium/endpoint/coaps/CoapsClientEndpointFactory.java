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
package org.eclipse.leshan.client.californium.endpoint.coaps;

import java.net.InetSocketAddress;
import java.security.Principal;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;

import javax.security.auth.x500.X500Principal;

import org.eclipse.californium.core.coap.Message;
import org.eclipse.californium.core.coap.Request;
import org.eclipse.californium.core.network.CoapEndpoint;
import org.eclipse.californium.core.network.CoapEndpoint.Builder;
import org.eclipse.californium.core.network.Endpoint;
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
import org.eclipse.californium.elements.util.CertPathUtil;
import org.eclipse.californium.scandium.DTLSConnector;
import org.eclipse.californium.scandium.config.DtlsConfig;
import org.eclipse.californium.scandium.config.DtlsConfig.DtlsRole;
import org.eclipse.californium.scandium.config.DtlsConnectorConfig;
import org.eclipse.californium.scandium.dtls.DtlsHandshakeTimeoutException;
import org.eclipse.californium.scandium.dtls.cipher.CipherSuite;
import org.eclipse.californium.scandium.dtls.pskstore.AdvancedSinglePskStore;
import org.eclipse.californium.scandium.dtls.x509.NewAdvancedCertificateVerifier;
import org.eclipse.californium.scandium.dtls.x509.SingleCertificateProvider;
import org.eclipse.californium.scandium.dtls.x509.StaticNewAdvancedCertificateVerifier;
import org.eclipse.leshan.client.californium.CaConstraintCertificateVerifier;
import org.eclipse.leshan.client.californium.CaliforniumConnectionController;
import org.eclipse.leshan.client.californium.DomainIssuerCertificateVerifier;
import org.eclipse.leshan.client.californium.ServiceCertificateConstraintCertificateVerifier;
import org.eclipse.leshan.client.californium.TrustAnchorAssertionCertificateVerifier;
import org.eclipse.leshan.client.californium.endpoint.CaliforniumClientEndpointFactory;
import org.eclipse.leshan.client.endpoint.ClientEndpointToolbox;
import org.eclipse.leshan.client.servers.ServerIdentity;
import org.eclipse.leshan.client.servers.ServerInfo;
import org.eclipse.leshan.core.CertificateUsage;
import org.eclipse.leshan.core.SecurityMode;
import org.eclipse.leshan.core.californium.DefaultExceptionTranslator;
import org.eclipse.leshan.core.californium.EndpointContextUtil;
import org.eclipse.leshan.core.californium.ExceptionTranslator;
import org.eclipse.leshan.core.californium.Lwm2mEndpointContextMatcher;
import org.eclipse.leshan.core.californium.identity.IdentityHandler;
import org.eclipse.leshan.core.endpoint.Protocol;
import org.eclipse.leshan.core.request.Identity;
import org.eclipse.leshan.core.request.exception.TimeoutException;
import org.eclipse.leshan.core.request.exception.TimeoutException.Type;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CoapsClientEndpointFactory implements CaliforniumClientEndpointFactory {

    private static final Logger LOG = LoggerFactory.getLogger(CoapsClientEndpointFactory.class);

    private final String loggingTag = null;
    private final InetSocketAddress addr;

    public CoapsClientEndpointFactory(InetSocketAddress addr) {
        this.addr = addr;

    }

    @Override
    public Protocol getProtocol() {
        return Protocol.COAPS;
    }

    @Override
    public Endpoint createEndpoint(Configuration defaultConfiguration, ServerInfo serverInfo,
            boolean clientInitiatedOnly, List<Certificate> trustStore, ClientEndpointToolbox toolbox) {
        CoapEndpoint endpoint = createSecuredEndpointBuilder(handle(addr, serverInfo,
                createDtlsConfigBuilder(defaultConfiguration), defaultConfiguration, clientInitiatedOnly, trustStore),
                defaultConfiguration).build();

        return endpoint;
    }

    protected DtlsConnectorConfig.Builder createDtlsConfigBuilder(Configuration configuration) {
        return new DtlsConnectorConfig.Builder(configuration);
    }

    protected DtlsConnectorConfig handle(InetSocketAddress addr, ServerInfo serverInfo,
            DtlsConnectorConfig.Builder dtlsConfigBuilder, Configuration coapConfig, boolean clientInitiatedOnly,
            List<Certificate> trustStore) {

        if (serverInfo.isSecure()) {
            DtlsConnectorConfig incompleteConfig = dtlsConfigBuilder.getIncompleteConfig();
            DtlsConnectorConfig.Builder newBuilder = DtlsConnectorConfig.builder(incompleteConfig);
            newBuilder.setAddress(addr);

            // Support PSK
            if (serverInfo.secureMode == SecurityMode.PSK) {
                AdvancedSinglePskStore staticPskStore = new AdvancedSinglePskStore(serverInfo.pskId, serverInfo.pskKey);
                newBuilder.setAdvancedPskStore(staticPskStore);
                filterCipherSuites(newBuilder, newBuilder.getIncompleteConfig().getSupportedCipherSuites(), true,
                        false);
            } else if (serverInfo.secureMode == SecurityMode.RPK) {
                // set identity
                SingleCertificateProvider singleCertificateProvider = new SingleCertificateProvider(
                        serverInfo.privateKey, serverInfo.publicKey);
                // we don't want to check Key Pair here, if we do it this should be done in BootstrapConsistencyChecker
                singleCertificateProvider.setVerifyKeyPair(false);
                newBuilder.setCertificateIdentityProvider(singleCertificateProvider);
                // set RPK truststore
                final PublicKey expectedKey = serverInfo.serverPublicKey;
                NewAdvancedCertificateVerifier rpkVerifier = new StaticNewAdvancedCertificateVerifier.Builder()
                        .setTrustedRPKs(new RawPublicKeyIdentity(expectedKey)).build();
                newBuilder.setAdvancedCertificateVerifier(rpkVerifier);
                filterCipherSuites(newBuilder, incompleteConfig.getSupportedCipherSuites(), false, true);
            } else if (serverInfo.secureMode == SecurityMode.X509) {
                // set identity
                SingleCertificateProvider singleCertificateProvider = new SingleCertificateProvider(
                        serverInfo.privateKey, new Certificate[] { serverInfo.clientCertificate });
                // we don't want to check Key Pair here, if we do it this should be done in BootstrapConsistencyChecker
                singleCertificateProvider.setVerifyKeyPair(false);
                newBuilder.setCertificateIdentityProvider(singleCertificateProvider);

                // LWM2M v1.1.1 - 5.2.8.7. Certificate Usage Field
                //
                // 0: Certificate usage 0 ("CA constraint")
                // - trustStore is combination of client's configured trust store and provided certificate in server
                // info
                // - must do PKIX validation with trustStore to build certPath
                // - must check that given certificate is part of certPath
                // - validate server name
                //
                // 1: Certificate usage 1 ("service certificate constraint")
                // - trustStore is client's configured trust store
                // - must do PKIX validation with trustStore
                // - target certificate must match what is provided certificate in server info
                // - validate server name
                //
                // 2: Certificate usage 2 ("trust anchor assertion")
                // - trustStore is only the provided certificate in server info
                // - must do PKIX validation with trustStore
                // - validate server name
                //
                // 3: Certificate usage 3 ("domain-issued certificate") (default mode if missing)
                // - no trustStore used in this mode
                // - target certificate must match what is provided certificate in server info
                // - validate server name

                CertificateUsage certificateUsage = serverInfo.certificateUsage != null ? serverInfo.certificateUsage
                        : CertificateUsage.DOMAIN_ISSUER_CERTIFICATE;

                if (certificateUsage == CertificateUsage.CA_CONSTRAINT) {
                    X509Certificate[] trustedCertificates = null;
                    if (trustStore != null) {
                        trustedCertificates = CertPathUtil.toX509CertificatesList(trustStore)
                                .toArray(new X509Certificate[trustStore.size()]);
                    }
                    newBuilder.setAdvancedCertificateVerifier(
                            new CaConstraintCertificateVerifier(serverInfo.serverCertificate, trustedCertificates));
                } else if (certificateUsage == CertificateUsage.SERVICE_CERTIFICATE_CONSTRAINT) {
                    X509Certificate[] trustedCertificates = null;

                    // - trustStore is client's configured trust store
                    if (trustStore != null) {
                        trustedCertificates = CertPathUtil.toX509CertificatesList(trustStore)
                                .toArray(new X509Certificate[trustStore.size()]);
                    }

                    newBuilder.setAdvancedCertificateVerifier(new ServiceCertificateConstraintCertificateVerifier(
                            serverInfo.serverCertificate, trustedCertificates));
                } else if (certificateUsage == CertificateUsage.TRUST_ANCHOR_ASSERTION) {
                    newBuilder.setAdvancedCertificateVerifier(new TrustAnchorAssertionCertificateVerifier(
                            (X509Certificate) serverInfo.serverCertificate));
                } else if (certificateUsage == CertificateUsage.DOMAIN_ISSUER_CERTIFICATE) {
                    newBuilder.setAdvancedCertificateVerifier(
                            new DomainIssuerCertificateVerifier(serverInfo.serverCertificate));
                }

                // TODO We set CN with '*' as we are not able to know the CN for some certificate usage and so this is
                // not used anymore to identify a server with x509.
                // See : https://github.com/eclipse/leshan/issues/992
                filterCipherSuites(newBuilder, incompleteConfig.getSupportedCipherSuites(), false, true);
            } else {
                throw new RuntimeException("Unable to create connector : unsupported security mode");
            }

            // Handle DTLS mode
            DtlsRole dtlsRole = incompleteConfig.getConfiguration().get(DtlsConfig.DTLS_ROLE);
            if (dtlsRole == null) {
                if (serverInfo.bootstrap) {
                    // For bootstrap no need to have DTLS role exchange
                    // and so we can set DTLS Connection as client only by default.
                    newBuilder.set(DtlsConfig.DTLS_ROLE, DtlsRole.CLIENT_ONLY);
                } else if (clientInitiatedOnly) {
                    // if client initiated only we don't allow connector to work as server role.
                    newBuilder.set(DtlsConfig.DTLS_ROLE, DtlsRole.CLIENT_ONLY);
                } else {
                    newBuilder.set(DtlsConfig.DTLS_ROLE, DtlsRole.BOTH);
                }
            }

            if (incompleteConfig.getConfiguration().get(DtlsConfig.DTLS_ROLE) == DtlsRole.BOTH) {
                // Ensure that BOTH mode can be used or fallback to CLIENT_ONLY
                if (serverInfo.secureMode == SecurityMode.X509) {
                    X509Certificate certificate = (X509Certificate) serverInfo.clientCertificate;
                    if (CertPathUtil.canBeUsedForAuthentication(certificate, true)) {
                        if (!CertPathUtil.canBeUsedForAuthentication(certificate, false)) {
                            newBuilder.set(DtlsConfig.DTLS_ROLE, DtlsRole.CLIENT_ONLY);
                            LOG.warn("Client certificate does not allow Server Authentication usage."
                                    + "\nThis will prevent a LWM2M server to initiate DTLS connection to this client."
                                    + "\nSee : https://github.com/eclipse/leshan/wiki/Server-Failover#about-connections");
                        }
                    }
                }
            }
            return newBuilder.build();
        }
        return null;
    }

    private void filterCipherSuites(DtlsConnectorConfig.Builder dtlsConfigurationBuilder, List<CipherSuite> ciphers,
            boolean psk, boolean requireServerCertificateMessage) {
        if (ciphers == null)
            return;

        List<CipherSuite> filteredCiphers = new ArrayList<>();
        for (CipherSuite cipher : ciphers) {
            if (psk && cipher.isPskBased()) {
                filteredCiphers.add(cipher);
            } else if (requireServerCertificateMessage && cipher.requiresServerCertificateMessage()) {
                filteredCiphers.add(cipher);
            }
        }
        dtlsConfigurationBuilder.set(DtlsConfig.DTLS_CIPHER_SUITES, filteredCiphers);
    }

    /**
     * This method is intended to be overridden.
     *
     * @param dtlsConfig the DTLS config used to create this endpoint.
     * @param coapConfig the CoAP config used to create this endpoint.
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
        return new PrincipalEndpointContextMatcher() {
            @Override
            protected boolean matchPrincipals(Principal requestedPrincipal, Principal availablePrincipal) {
                // As we are using 1 connector/endpoint by server at client side,
                // and connector strongly limit connection from/to the expected foreign peer,
                // we don't need to re-check principal at EndpointContextMatcher level.
                return true;
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

    @Override
    public CaliforniumConnectionController createConnectionController() {
        return new CaliforniumConnectionController() {
            @Override
            public void forceReconnection(Endpoint endpoint, ServerIdentity server, boolean resume) {
                Connector connector = ((CoapEndpoint) endpoint).getConnector();
                if (connector instanceof DTLSConnector) {
                    if (resume) {
                        LOG.info("Clear DTLS session for resumption for server {}", server.getUri());
                        ((DTLSConnector) connector).forceResumeAllSessions();
                    } else {
                        LOG.info("Clear DTLS session for server {}", server.getUri());
                        ((DTLSConnector) connector).clearConnectionState();
                    }
                }
            }
        };
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
