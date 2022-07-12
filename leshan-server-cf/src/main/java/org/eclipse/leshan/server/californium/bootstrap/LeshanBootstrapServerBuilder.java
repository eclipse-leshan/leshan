/*******************************************************************************
 * Copyright (c) 2017 Sierra Wireless and others.
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
 *     Achim Kraus (Bosch Software Innovations GmbH) - use CoapEndpointBuilder
 *     Michał Wadowski (Orange) - Improved compliance with rfc6690
 *******************************************************************************/
package org.eclipse.leshan.server.californium.bootstrap;

import java.net.InetSocketAddress;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.List;

import org.eclipse.californium.core.config.CoapConfig;
import org.eclipse.californium.core.config.CoapConfig.TrackerMode;
import org.eclipse.californium.core.network.CoapEndpoint;
import org.eclipse.californium.elements.UDPConnector;
import org.eclipse.californium.elements.config.Configuration;
import org.eclipse.californium.elements.config.SystemConfig;
import org.eclipse.californium.elements.config.UdpConfig;
import org.eclipse.californium.oscore.OSCoreCtxDB;
import org.eclipse.californium.scandium.DTLSConnector;
import org.eclipse.californium.scandium.config.DtlsConfig;
import org.eclipse.californium.scandium.config.DtlsConfig.DtlsRole;
import org.eclipse.californium.scandium.config.DtlsConnectorConfig;
import org.eclipse.californium.scandium.config.DtlsConnectorConfig.Builder;
import org.eclipse.californium.scandium.dtls.CertificateType;
import org.eclipse.californium.scandium.dtls.cipher.CipherSuite;
import org.eclipse.californium.scandium.dtls.x509.SingleCertificateProvider;
import org.eclipse.californium.scandium.dtls.x509.StaticNewAdvancedCertificateVerifier;
import org.eclipse.leshan.core.LwM2m;
import org.eclipse.leshan.core.californium.DefaultEndpointFactory;
import org.eclipse.leshan.core.californium.EndpointFactory;
import org.eclipse.leshan.core.californium.oscore.cf.InMemoryOscoreContextDB;
import org.eclipse.leshan.core.link.lwm2m.DefaultLwM2mLinkParser;
import org.eclipse.leshan.core.link.lwm2m.LwM2mLinkParser;
import org.eclipse.leshan.core.node.LwM2mNode;
import org.eclipse.leshan.core.node.codec.DefaultLwM2mDecoder;
import org.eclipse.leshan.core.node.codec.DefaultLwM2mEncoder;
import org.eclipse.leshan.core.node.codec.LwM2mDecoder;
import org.eclipse.leshan.core.node.codec.LwM2mEncoder;
import org.eclipse.leshan.server.bootstrap.BootstrapConfig;
import org.eclipse.leshan.server.bootstrap.BootstrapConfigStore;
import org.eclipse.leshan.server.bootstrap.BootstrapConfigStoreTaskProvider;
import org.eclipse.leshan.server.bootstrap.BootstrapHandler;
import org.eclipse.leshan.server.bootstrap.BootstrapHandlerFactory;
import org.eclipse.leshan.server.bootstrap.BootstrapSessionListener;
import org.eclipse.leshan.server.bootstrap.BootstrapSessionManager;
import org.eclipse.leshan.server.bootstrap.DefaultBootstrapHandler;
import org.eclipse.leshan.server.bootstrap.DefaultBootstrapSessionManager;
import org.eclipse.leshan.server.bootstrap.InMemoryBootstrapConfigStore;
import org.eclipse.leshan.server.bootstrap.LwM2mBootstrapRequestSender;
import org.eclipse.leshan.server.model.LwM2mBootstrapModelProvider;
import org.eclipse.leshan.server.model.StandardBootstrapModelProvider;
import org.eclipse.leshan.server.security.BootstrapSecurityStore;
import org.eclipse.leshan.server.security.SecurityChecker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class helping you to build and configure a Californium based Leshan Bootstrap Lightweight M2M server.
 * <p>
 * Usage: create it, call the different setters for changing the configuration and then call the {@link #build()} method
 * for creating the {@link LeshanBootstrapServer} ready to operate.
 */
public class LeshanBootstrapServerBuilder {

    private static final Logger LOG = LoggerFactory.getLogger(LeshanBootstrapServerBuilder.class);

    private InetSocketAddress localAddress;
    private InetSocketAddress localAddressSecure;
    private BootstrapConfigStore configStore;
    private BootstrapSecurityStore securityStore;
    private BootstrapSessionManager sessionManager;
    private BootstrapHandlerFactory bootstrapHandlerFactory;

    private LwM2mBootstrapModelProvider modelProvider;
    private Configuration coapConfig;
    private Builder dtlsConfigBuilder;

    private LwM2mEncoder encoder;
    private LwM2mDecoder decoder;

    private PublicKey publicKey;
    private PrivateKey privateKey;
    private X509Certificate[] certificateChain;
    private Certificate[] trustedCertificates;

    private EndpointFactory endpointFactory;
    private boolean noSecuredEndpoint;
    private boolean noUnsecuredEndpoint;

    private LwM2mLinkParser linkParser;

    private boolean enableOscore = false;

    /**
     * Set the address/port for unsecured CoAP communication (<code>coap://</code>).
     * <p>
     * By default a wildcard address and the default CoAP port(5683) is used.
     *
     * @param hostname The address to bind. If null wildcard address is used.
     * @param port A valid port value is between 0 and 65535. A port number of zero will let the system pick up an
     *        ephemeral port in a bind operation.
     * @return the builder for fluent Bootstrap Server creation.
     */
    public LeshanBootstrapServerBuilder setLocalAddress(String hostname, int port) {
        if (hostname == null) {
            this.localAddress = new InetSocketAddress(port);
        } else {
            this.localAddress = new InetSocketAddress(hostname, port);
        }
        return this;
    }

    /**
     * Set the address for unsecured CoAP communication (<code>coap://</code>).
     * <p>
     * By default a wildcard address and the default CoAP port(5683) is used.
     *
     * @param localAddress the socket address for <code>coap://</code>.
     * @return the builder for fluent Bootstrap Server creation.
     */
    public LeshanBootstrapServerBuilder setLocalAddress(InetSocketAddress localAddress) {
        this.localAddress = localAddress;
        return this;
    }

    /**
     * Set the address/port for secured CoAP over DTLS communication (<code>coaps://</code>).
     * <p>
     * By default a wildcard address and the default CoAPs port(5684) is used.
     *
     * @param hostname The address to bind. If null wildcard address is used.
     * @param port A valid port value is between 0 and 65535. A port number of zero will let the system pick up an
     *        ephemeral port in a bind operation.
     * @return the builder for fluent Bootstrap Server creation.
     */
    public LeshanBootstrapServerBuilder setLocalSecureAddress(String hostname, int port) {
        if (hostname == null) {
            this.localAddressSecure = new InetSocketAddress(port);
        } else {
            this.localAddressSecure = new InetSocketAddress(hostname, port);
        }
        return this;
    }

    /**
     * Set the address for secured CoAP over DTLS communication Server (<code>coaps://</code>).
     * <p>
     * By default a wildcard address and the default CoAP port(5684) is used.
     *
     * @param localSecureAddress the socket address for <code>coaps://</code>.
     * @return the builder for fluent Bootstrap Server creation.
     */
    public LeshanBootstrapServerBuilder setLocalSecureAddress(InetSocketAddress localSecureAddress) {
        this.localAddressSecure = localSecureAddress;
        return this;
    }

    /**
     * Set the {@link PublicKey} of the server which will be used for Raw Public Key DTLS authentication.
     * <p>
     * This should be used for RPK support only.
     * <p>
     * Setting <code>publicKey</code> and <code>privateKey</code> will enable RawPublicKey DTLS authentication, see also
     * {@link LeshanBootstrapServerBuilder#setPrivateKey(PrivateKey)}.
     *
     * @param publicKey the Raw Public Key of the bootstrap server.
     * @return the builder for fluent Bootstrap Server creation.
     */
    public LeshanBootstrapServerBuilder setPublicKey(PublicKey publicKey) {
        this.publicKey = publicKey;
        return this;
    }

    /**
     * Set the CertificateChain of the server which will be used for X.509 DTLS authentication.
     * <p>
     * Setting <code>publicKey</code> and <code>privateKey</code> will enable RPK and X.509 DTLS authentication, see
     * also {@link LeshanBootstrapServerBuilder#setPrivateKey(PrivateKey)}.
     * <p>
     * For RPK the public key will be extracted from the first X.509 certificate of the certificate chain. If you only
     * need RPK support, use {@link LeshanBootstrapServerBuilder#setPublicKey(PublicKey)} instead.
     * <p>
     * If you want to deactivate RPK mode, look at
     * {@link LeshanBootstrapServerBuilder#setDtlsConfig(org.eclipse.californium.scandium.config.DtlsConnectorConfig.Builder)}
     * and
     * {@link Builder#setCertificateIdentityProvider(org.eclipse.californium.scandium.dtls.x509.CertificateProvider)}.
     *
     * @param certificateChain the certificate chain of the bootstrap server.
     * @return the builder for fluent Bootstrap Server creation.
     */
    public <T extends X509Certificate> LeshanBootstrapServerBuilder setCertificateChain(T[] certificateChain) {
        this.certificateChain = certificateChain;
        return this;
    }

    /**
     * Set the {@link PrivateKey} of the server which will be used for RawPublicKey(RPK) and/or X.509 DTLS
     * authentication.
     *
     * @param privateKey the Private Key of the bootstrap server.
     * @return the builder for fluent Bootstrap Server creation.
     */
    public LeshanBootstrapServerBuilder setPrivateKey(PrivateKey privateKey) {
        this.privateKey = privateKey;
        return this;
    }

    /**
     * The list of trusted certificates used to authenticate devices using X.509 DTLS authentication.
     * <p>
     * If you need more complex/dynamic trust behavior, look at
     * {@link LeshanBootstrapServerBuilder#setDtlsConfig(org.eclipse.californium.scandium.config.DtlsConnectorConfig.Builder)}
     * and
     * {@link Builder#setAdvancedCertificateVerifier(org.eclipse.californium.scandium.dtls.x509.NewAdvancedCertificateVerifier)}
     * instead.
     *
     * @param trustedCertificates certificates trusted by the bootstrap server.
     * @return the builder for fluent Bootstrap Server creation.
     */
    public <T extends Certificate> LeshanBootstrapServerBuilder setTrustedCertificates(T[] trustedCertificates) {
        this.trustedCertificates = trustedCertificates;
        return this;
    }

    /**
     * Set the {@link BootstrapConfigStore} containing bootstrap configuration to apply to each devices.
     * <p>
     * By default an {@link InMemoryBootstrapConfigStore} is used.
     * <p>
     * See {@link BootstrapConfig} to see what is could be done during a bootstrap session.
     *
     * @param configStore the bootstrap configuration store.
     * @return the builder for fluent Bootstrap Server creation.
     *
     */
    public LeshanBootstrapServerBuilder setConfigStore(BootstrapConfigStore configStore) {
        this.configStore = configStore;
        return this;
    }

    /**
     * Set the {@link BootstrapSecurityStore} which contains data needed to authenticate devices.
     * <p>
     * WARNING: without security store all devices will be accepted which is not really recommended in production
     * environment.
     * <p>
     * There is not default implementation.
     *
     * @param securityStore the security store used to authenticate devices.
     * @return the builder for fluent Bootstrap Server creation.
     */
    public LeshanBootstrapServerBuilder setSecurityStore(BootstrapSecurityStore securityStore) {
        this.securityStore = securityStore;
        return this;
    }

    /**
     * Advanced setter used to define {@link BootstrapSessionManager}.
     * <p>
     * See {@link BootstrapSessionManager} and {@link DefaultBootstrapSessionManager} for more details.
     *
     * @param sessionManager the manager responsible to handle bootstrap session.
     * @return the builder for fluent Bootstrap Server creation.
     */
    public LeshanBootstrapServerBuilder setSessionManager(BootstrapSessionManager sessionManager) {
        this.sessionManager = sessionManager;
        return this;
    }

    /**
     * Advanced setter used to customize default bootstrap server behavior.
     * <p>
     * If default bootstrap server behavior is not flexible enough, you can create your own {@link BootstrapHandler} by
     * inspiring yourself from {@link DefaultBootstrapHandler}.
     *
     * @param bootstrapHandlerFactory the factory used to create {@link BootstrapHandler}.
     * @return the builder for fluent Bootstrap Server creation.
     */
    public LeshanBootstrapServerBuilder setBootstrapHandlerFactory(BootstrapHandlerFactory bootstrapHandlerFactory) {
        this.bootstrapHandlerFactory = bootstrapHandlerFactory;
        return this;
    }

    /**
     * <p>
     * Set your {@link LwM2mBootstrapModelProvider} implementation.
     * </p>
     * By default the {@link StandardBootstrapModelProvider}.
     *
     */
    public LeshanBootstrapServerBuilder setObjectModelProvider(LwM2mBootstrapModelProvider objectModelProvider) {
        this.modelProvider = objectModelProvider;
        return this;
    }

    /**
     * <p>
     * Set the {@link LwM2mEncoder} which will encode {@link LwM2mNode} with supported content format.
     * </p>
     * By default the {@link DefaultLwM2mEncoder} is used. It supports Text, Opaque, TLV and JSON format.
     */
    public LeshanBootstrapServerBuilder setEncoder(LwM2mEncoder encoder) {
        this.encoder = encoder;
        return this;
    }

    /**
     * <p>
     * Set the {@link LwM2mDecoder} which will decode data in supported content format to create {@link LwM2mNode}.
     * </p>
     * By default the {@link DefaultLwM2mDecoder} is used. It supports Text, Opaque, TLV and JSON format.
     */
    public LeshanBootstrapServerBuilder setDecoder(LwM2mDecoder decoder) {
        this.decoder = decoder;
        return this;
    }

    /**
     * Set the CoAP/Californium {@link Configuration}.
     * <p>
     * This is strongly recommended to create the {@link Configuration} with {@link #createDefaultCoapConfiguration()}
     * before to modify it.
     *
     * @param coapConfig the CoAP configuration.
     * @return the builder for fluent Bootstrap Server creation.
     */
    public LeshanBootstrapServerBuilder setCoapConfig(Configuration coapConfig) {
        this.coapConfig = coapConfig;
        return this;
    }

    /**
     * Set the DTLS/Scandium {@link DtlsConnectorConfig}.
     * <p>
     * For advanced DTLS setting.
     *
     * @param dtlsConfig the DTLS configuration builder.
     * @return the builder for fluent Bootstrap Server creation.
     */
    public LeshanBootstrapServerBuilder setDtlsConfig(Builder dtlsConfig) {
        this.dtlsConfigBuilder = dtlsConfig;
        return this;
    }

    /**
     * Advanced setter used to create custom CoAP endpoint.
     * <p>
     * An {@link UDPConnector} is expected for unsecured endpoint and a {@link DTLSConnector} is expected for secured
     * endpoint.
     *
     * @param endpointFactory An {@link EndpointFactory}, you can extends {@link DefaultEndpointFactory}.
     * @return the builder for fluent Bootstrap Server creation.
     */
    public LeshanBootstrapServerBuilder setEndpointFactory(EndpointFactory endpointFactory) {
        this.endpointFactory = endpointFactory;
        return this;
    }

    /**
     * Set the CoRE Link parser {@link LwM2mLinkParser}
     * <p>
     * By default the {@link DefaultLwM2mLinkParser} is used.
     */
    public void setLinkParser(LwM2mLinkParser linkParser) {
        this.linkParser = linkParser;
    }

    /**
     * Deactivate unsecured CoAP endpoint, meaning that <code>coap://</code> communication will be impossible.
     *
     * @return the builder for fluent Bootstrap Server creation.
     */
    public LeshanBootstrapServerBuilder disableUnsecuredEndpoint() {
        this.noUnsecuredEndpoint = true;
        return this;
    }

    /**
     * Deactivate secured CoAP endpoint (DTLS), meaning that <code>coaps://</code> communication will be impossible.
     *
     * @return the builder for fluent Bootstrap Server creation.
     */
    public LeshanBootstrapServerBuilder disableSecuredEndpoint() {
        this.noSecuredEndpoint = true;
        return this;
    }

    /**
     * Enable EXPERIMENTAL OSCORE feature.
     * <p>
     * By default OSCORE is not enabled.
     */
    public LeshanBootstrapServerBuilder setEnableOscore(boolean enableOscore) {
        this.enableOscore = enableOscore;
        return this;
    }

    /**
     * Create the default CoAP/Californium {@link Configuration} used by the builder.
     * <p>
     * It could be used as a base to create a custom CoAP configuration, then use it with
     * {@link #setCoapConfig(Configuration)}
     *
     * @return the default CoAP config.
     */
    public static Configuration createDefaultCoapConfiguration() {
        Configuration networkConfig = new Configuration(CoapConfig.DEFINITIONS, DtlsConfig.DEFINITIONS,
                UdpConfig.DEFINITIONS, SystemConfig.DEFINITIONS);
        networkConfig.set(CoapConfig.MID_TRACKER, TrackerMode.NULL);
        networkConfig.set(DtlsConfig.DTLS_ROLE, DtlsRole.SERVER_ONLY);
        return networkConfig;
    }

    /**
     * Create the {@link LeshanBootstrapServer}.
     * <p>
     * Next step will be to start it : {@link LeshanBootstrapServer#start()}.
     *
     * @return the LWM2M Bootstrap server.
     * @throws IllegalStateException if builder configuration is not consistent.
     */
    public LeshanBootstrapServer build() {
        if (localAddress == null)
            localAddress = new InetSocketAddress(LwM2m.DEFAULT_COAP_PORT);
        if (bootstrapHandlerFactory == null)
            bootstrapHandlerFactory = new BootstrapHandlerFactory() {
                @Override
                public BootstrapHandler create(LwM2mBootstrapRequestSender sender,
                        BootstrapSessionManager sessionManager, BootstrapSessionListener listener) {
                    return new DefaultBootstrapHandler(sender, sessionManager, listener);
                }
            };
        if (configStore == null) {
            configStore = new InMemoryBootstrapConfigStore();
        } else if (sessionManager != null) {
            LOG.warn("configStore is set but you also provide a custom SessionManager so this store will not be used");
        }
        if (modelProvider == null) {
            modelProvider = new StandardBootstrapModelProvider();
        } else if (sessionManager != null) {
            LOG.warn(
                    "modelProvider is set but you also provide a custom SessionManager so this provider will not be used");
        }
        if (sessionManager == null) {
            sessionManager = new DefaultBootstrapSessionManager(securityStore, new SecurityChecker(),
                    new BootstrapConfigStoreTaskProvider(configStore), modelProvider);
        }
        if (coapConfig == null) {
            coapConfig = createDefaultCoapConfiguration();
        }
        if (endpointFactory == null) {
            endpointFactory = new DefaultEndpointFactory("LWM2M BS Server", false);
        }
        if (encoder == null)
            encoder = new DefaultLwM2mEncoder();
        if (decoder == null)
            decoder = new DefaultLwM2mDecoder();
        if (linkParser == null)
            linkParser = new DefaultLwM2mLinkParser();

        // handle dtlsConfig
        DtlsConnectorConfig dtlsConfig = null;
        if (!noSecuredEndpoint && shouldTryToCreateSecureEndpoint()) {
            if (dtlsConfigBuilder == null) {
                dtlsConfigBuilder = DtlsConnectorConfig.builder(coapConfig);
            }
            // Set default DTLS setting for Leshan unless user change it.
            DtlsConnectorConfig incompleteConfig = dtlsConfigBuilder.getIncompleteConfig();

            // Handle PSK Store
            if (incompleteConfig.getAdvancedPskStore() != null) {
                LOG.warn(
                        "PskStore should be automatically set by Leshan. Using a custom implementation is not advised.");
            } else if (securityStore != null) {
                List<CipherSuite> ciphers = incompleteConfig.getConfiguration().get(DtlsConfig.DTLS_CIPHER_SUITES);
                if (ciphers == null // if null ciphers will be chosen automatically by Scandium
                        || CipherSuite.containsPskBasedCipherSuite(ciphers)) {
                    dtlsConfigBuilder.setAdvancedPskStore(new LwM2mBootstrapPskStore(securityStore));
                }
            }

            // Handle secure address
            if (incompleteConfig.getAddress() == null) {
                if (localAddressSecure == null) {
                    localAddressSecure = new InetSocketAddress(LwM2m.DEFAULT_COAP_SECURE_PORT);
                }
                dtlsConfigBuilder.setAddress(localAddressSecure);
            } else if (localAddressSecure != null && !localAddressSecure.equals(incompleteConfig.getAddress())) {
                throw new IllegalStateException(String.format(
                        "Configuration conflict between LeshanBuilder and DtlsConnectorConfig.Builder for secure address: %s != %s",
                        localAddressSecure, incompleteConfig.getAddress()));
            }

            // check conflict in configuration
            if (incompleteConfig.getCertificateIdentityProvider() != null) {
                if (privateKey != null) {
                    throw new IllegalStateException(String.format(
                            "Configuration conflict between LeshanBuilder and DtlsConnectorConfig.Builder for private key"));
                }
                if (publicKey != null) {
                    throw new IllegalStateException(String.format(
                            "Configuration conflict between LeshanBuilder and DtlsConnectorConfig.Builder for public key"));
                }
                if (certificateChain != null) {
                    throw new IllegalStateException(String.format(
                            "Configuration conflict between LeshanBuilder and DtlsConnectorConfig.Builder for certificate chain"));
                }
            } else if (privateKey != null) {
                // if in raw key mode and not in X.509 set the raw keys
                if (certificateChain == null && publicKey != null) {
                    dtlsConfigBuilder
                            .setCertificateIdentityProvider(new SingleCertificateProvider(privateKey, publicKey));
                }
                // if in X.509 mode set the private key, certificate chain, public key is extracted from the certificate
                if (certificateChain != null && certificateChain.length > 0) {

                    dtlsConfigBuilder.setCertificateIdentityProvider(new SingleCertificateProvider(privateKey,
                            certificateChain, CertificateType.X_509, CertificateType.RAW_PUBLIC_KEY));
                }
            }

            // handle trusted certificates or RPK
            if (incompleteConfig.getAdvancedCertificateVerifier() != null) {
                if (trustedCertificates != null) {
                    throw new IllegalStateException(
                            "Configuration conflict between LeshanBuilder and DtlsConnectorConfig.Builder: if a AdvancedCertificateVerifier is set, trustedCertificates must not be set.");
                }
            } else if (incompleteConfig.getCertificateIdentityProvider() != null) {
                StaticNewAdvancedCertificateVerifier.Builder verifierBuilder = StaticNewAdvancedCertificateVerifier
                        .builder();
                // by default trust all RPK
                verifierBuilder.setTrustAllRPKs();
                if (trustedCertificates != null) {
                    verifierBuilder.setTrustedCertificates(trustedCertificates);
                }
                dtlsConfigBuilder.setAdvancedCertificateVerifier(verifierBuilder.build());
            }

            // we try to build the dtlsConfig, if it fail we will just not create the secured endpoint
            try {
                dtlsConfig = dtlsConfigBuilder.build();
            } catch (IllegalStateException e) {
                LOG.warn("Unable to create DTLS config and so secured endpoint.", e);
            }
        }

        // Handle OSCORE support.
        OSCoreCtxDB oscoreCtxDB = null;
        OscoreBootstrapListener sessionHolder = null;
        BootstrapOscoreContextCleaner oscoreContextCleaner = null;
        if (enableOscore) {
            if (securityStore != null) {
                sessionHolder = new OscoreBootstrapListener();
                oscoreCtxDB = new InMemoryOscoreContextDB(new LwM2mBootstrapOscoreStore(securityStore, sessionHolder));
                oscoreContextCleaner = new BootstrapOscoreContextCleaner(oscoreCtxDB);
                LOG.warn("Experimental OSCORE feature is enabled.");
            }
        }

        CoapEndpoint unsecuredEndpoint = null;
        if (!noUnsecuredEndpoint) {
            unsecuredEndpoint = endpointFactory.createUnsecuredEndpoint(localAddress, coapConfig, null, oscoreCtxDB);
        }

        CoapEndpoint securedEndpoint = null;
        if (!noSecuredEndpoint && dtlsConfig != null) {
            securedEndpoint = endpointFactory.createSecuredEndpoint(dtlsConfig, coapConfig, null, null);
        }

        if (securedEndpoint == null && unsecuredEndpoint == null) {
            throw new IllegalStateException(
                    "All CoAP enpoints are deactivated, at least one endpoint should be activated");
        }

        // TODO OSCORE
        // <temporary code>
        LeshanBootstrapServer bootstrapServer = createBootstrapServer(unsecuredEndpoint, securedEndpoint,
                sessionManager, bootstrapHandlerFactory, coapConfig, encoder, decoder, linkParser);

        if (sessionHolder != null) {
            bootstrapServer.addListener(sessionHolder);
        }
        if (oscoreContextCleaner != null) {
            bootstrapServer.addListener(oscoreContextCleaner);
        }
        return bootstrapServer;
        // </temporay code>
        // replacing ===>
        // return createBootstrapServer(unsecuredEndpoint, securedEndpoint,
        // sessionManager, bootstrapHandlerFactory, coapConfig, encoder, decoder, linkParser);

    }

    /**
     * @return true if we should try to create a secure endpoint on {@link #build()}
     */
    protected boolean shouldTryToCreateSecureEndpoint() {
        return dtlsConfigBuilder != null || certificateChain != null || privateKey != null || publicKey != null
                || securityStore != null || trustedCertificates != null;
    }

    /**
     * Create the <code>LeshanBootstrapServer</code>.
     * <p>
     * You can extend <code>LeshanBootstrapServerBuilder</code> and override this method to create a new builder which
     * will be able to build an extended <code>LeshanBootstrapServer</code>.
     *
     * @param unsecuredEndpoint CoAP endpoint used for <code>coap://</code> communication.
     * @param securedEndpoint CoAP endpoint used for <code>coaps://</code> communication.
     * @param bsSessionManager the manager responsible to handle bootstrap session.
     * @param bsHandlerFactory the factory used to create {@link BootstrapHandler}.
     * @param coapConfig the CoAP configuration.
     * @param decoder decoder used to decode response payload.
     * @param encoder encode used to encode request payload.
     * @param linkParser a parser {@link LwM2mLinkParser} used to parse a CoRE Link.
     * @return the LWM2M Bootstrap server.
     */
    protected LeshanBootstrapServer createBootstrapServer(CoapEndpoint unsecuredEndpoint, CoapEndpoint securedEndpoint,
            BootstrapSessionManager bsSessionManager, BootstrapHandlerFactory bsHandlerFactory,
            Configuration coapConfig, LwM2mEncoder encoder, LwM2mDecoder decoder, LwM2mLinkParser linkParser) {
        return new LeshanBootstrapServer(unsecuredEndpoint, securedEndpoint, bsSessionManager, bsHandlerFactory,
                coapConfig, encoder, decoder, linkParser);
    }
}
