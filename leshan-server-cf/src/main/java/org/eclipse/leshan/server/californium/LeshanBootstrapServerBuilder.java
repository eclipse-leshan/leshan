/*******************************************************************************
 * Copyright (c) 2017 Sierra Wireless and others.
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v1.0 which accompany this distribution.
 * 
 * The Eclipse Public License is available at
 *    http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 *    http://www.eclipse.org/org/documents/edl-v10.html.
 * 
 * Contributors:
 *     Sierra Wireless - initial API and implementation
 *     Achim Kraus (Bosch Software Innovations GmbH) - use CoapEndpointBuilder
 *******************************************************************************/
package org.eclipse.leshan.server.californium;

import java.net.InetSocketAddress;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Arrays;

import org.eclipse.californium.core.network.CoapEndpoint;
import org.eclipse.californium.core.network.config.NetworkConfig;
import org.eclipse.californium.core.network.config.NetworkConfig.Keys;
import org.eclipse.californium.scandium.DTLSConnector;
import org.eclipse.californium.scandium.config.DtlsConnectorConfig;
import org.eclipse.californium.scandium.config.DtlsConnectorConfig.Builder;
import org.eclipse.leshan.LwM2m;
import org.eclipse.leshan.core.californium.EndpointFactory;
import org.eclipse.leshan.core.californium.Lwm2mEndpointContextMatcher;
import org.eclipse.leshan.core.model.LwM2mModel;
import org.eclipse.leshan.core.model.ObjectLoader;
import org.eclipse.leshan.server.bootstrap.BootstrapSessionManager;
import org.eclipse.leshan.server.bootstrap.BootstrapStore;
import org.eclipse.leshan.server.bootstrap.LwM2mBootstrapServer;
import org.eclipse.leshan.server.californium.impl.LeshanBootstrapServer;
import org.eclipse.leshan.server.californium.impl.LwM2mBootstrapPskStore;
import org.eclipse.leshan.server.impl.DefaultBootstrapSessionManager;
import org.eclipse.leshan.server.security.BootstrapSecurityStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class helping you to build and configure a Californium based Leshan Bootstrap Lightweight M2M server. Usage: create
 * it, call the different setters for changing the configuration and then call the {@link #build()} method for creating
 * the {@link LwM2mBootstrapServer} ready to operate.
 */
public class LeshanBootstrapServerBuilder {

    private static final Logger LOG = LoggerFactory.getLogger(LeshanBootstrapServerBuilder.class);

    private InetSocketAddress localAddress;
    private InetSocketAddress localAddressSecure;
    private BootstrapStore configStore;
    private BootstrapSecurityStore securityStore;
    private BootstrapSessionManager sessionManager;
    private LwM2mModel model;
    private NetworkConfig coapConfig;
    private Builder dtlsConfigBuilder;

    private PublicKey publicKey;
    private PrivateKey privateKey;
    private X509Certificate[] certificateChain;
    private Certificate[] trustedCertificates;

    private EndpointFactory endpointFactory;

    private boolean noSecuredEndpoint;

    private boolean noUnsecuredEndpoint;

    /**
     * <p>
     * Set the address/port for unsecured CoAP Server.
     * </p>
     * 
     * By default a wildcard address and the default CoAP port(5683) is used
     * 
     * @param hostname The address to bind. If null wildcard address is used.
     * @param port A valid port value is between 0 and 65535. A port number of zero will let the system pick up an
     *        ephemeral port in a bind operation.
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
     * <p>
     * Set the address for unsecured CoAP Server.
     * </p>
     * 
     * By default a wildcard address and the default CoAP port(5683) is used.
     */
    public LeshanBootstrapServerBuilder setLocalAddress(InetSocketAddress localAddress) {
        this.localAddress = localAddress;
        return this;
    }

    /**
     * <p>
     * Set the address/port for secured CoAP Server (Using DTLS).
     * <p>
     * 
     * By default a wildcard address and the default CoAPs port(5684) is used.
     * 
     * @param hostname The address to bind. If null wildcard address is used.
     * @param port A valid port value is between 0 and 65535. A port number of zero will let the system pick up an
     *        ephemeral port in a bind operation.
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
     * <p>
     * Set the {@link PublicKey} of the server which will be used for RawPublicKey DTLS authentication.
     * </p>
     * This should be used for RPK support only.
     */
    public LeshanBootstrapServerBuilder setPublicKey(PublicKey publicKey) {
        this.publicKey = publicKey;
        return this;
    }

    /**
     * Set the {@link PrivateKey} of the server which will be used for RawPublicKey(RPK).
     */
    public LeshanBootstrapServerBuilder setPrivateKey(PrivateKey privateKey) {
        this.privateKey = privateKey;
        return this;
    }

    /**
     * <p>
     * Set the CertificateChain of the server which will be used for X509 DTLS authentication.
     * </p>
     * For RPK the public key will be extract from the first X509 certificate of the certificate chain. If you only need
     * RPK support, use {@link LeshanServerBuilder#setPublicKey(PublicKey)} instead.
     */
    public <T extends X509Certificate> LeshanBootstrapServerBuilder setCertificateChain(T[] certificateChain) {
        this.certificateChain = certificateChain;
        return this;
    }

    /**
     * The list of trusted certificates used to authenticate devices.
     */
    public <T extends Certificate> LeshanBootstrapServerBuilder setTrustedCertificates(T[] trustedCertificates) {
        this.trustedCertificates = trustedCertificates;
        return this;
    }

    /**
     * <p>
     * Set the address for secured CoAP Server (Using DTLS).
     * </p>
     * 
     * By default a wildcard address and the default CoAP port(5684) is used.
     */
    public LeshanBootstrapServerBuilder setLocalSecureAddress(InetSocketAddress localSecureAddress) {
        this.localAddressSecure = localSecureAddress;
        return this;
    }

    public LeshanBootstrapServerBuilder setConfigStore(BootstrapStore configStore) {
        this.configStore = configStore;
        return this;
    }

    public LeshanBootstrapServerBuilder setSecurityStore(BootstrapSecurityStore securityStore) {
        this.securityStore = securityStore;
        return this;
    }

    public LeshanBootstrapServerBuilder setSessionManager(BootstrapSessionManager sessionManager) {
        this.sessionManager = sessionManager;
        return this;
    }

    public LeshanBootstrapServerBuilder setModel(LwM2mModel model) {
        this.model = model;
        return this;
    }

    public LeshanBootstrapServerBuilder setCoapConfig(NetworkConfig coapConfig) {
        this.coapConfig = coapConfig;
        return this;
    }

    public LeshanBootstrapServerBuilder setDtlsConfig(DtlsConnectorConfig.Builder dtlsConfig) {
        this.dtlsConfigBuilder = dtlsConfig;
        return this;
    }

    /**
     * Used to create custom CoAP endpoint, this is only for advanced users. <br>
     * DTLSConnector is expected for secured endpoint.
     */
    public LeshanBootstrapServerBuilder setEndpointFactory(EndpointFactory endpointFactory) {
        this.endpointFactory = endpointFactory;
        return this;
    }

    /**
     * deactivate unsecured CoAP endpoint
     */
    public LeshanBootstrapServerBuilder disableUnsecuredEndpoint() {
        this.noUnsecuredEndpoint = true;
        return this;
    }

    /**
     * deactivate secured CoAP endpoint (DTLS)
     */
    public LeshanBootstrapServerBuilder disableSecuredEndpoint() {
        this.noSecuredEndpoint = true;
        return this;
    }

    /**
     * The default Californium/CoAP {@link NetworkConfig} used by the builder.
     */
    public static NetworkConfig createDefaultNetworkConfig() {
        NetworkConfig networkConfig = new NetworkConfig();
        networkConfig.set(Keys.MID_TRACKER, "NULL");
        // Workaround for https://github.com/eclipse/leshan/issues/502
        // TODO remove this line when we will integrate Cf 2.0.0-M10
        networkConfig.set(Keys.HEALTH_STATUS_INTERVAL, 0);
        return networkConfig;
    }

    public LeshanBootstrapServer build() {
        if (localAddress == null)
            localAddress = new InetSocketAddress(LwM2m.DEFAULT_COAP_PORT);

        // TODO we should have default implementation for BootstrapStore in leshan.server project.
        if (configStore == null)
            throw new IllegalStateException("BootstrapStore is mandatory");

        if (sessionManager == null)
            sessionManager = new DefaultBootstrapSessionManager(securityStore);
        if (model == null)
            model = new LwM2mModel(ObjectLoader.loadDefault());
        if (coapConfig == null) {
            coapConfig = createDefaultNetworkConfig();
        }

        // handle dtlsConfig
        DtlsConnectorConfig dtlsConfig = null;
        if (!noSecuredEndpoint) {
            if (dtlsConfigBuilder == null) {
                dtlsConfigBuilder = new DtlsConnectorConfig.Builder();
            }
            DtlsConnectorConfig incompleteConfig = dtlsConfigBuilder.getIncompleteConfig();

            // Handle PSK Store
            if (incompleteConfig.getPskStore() == null && securityStore != null) {
                dtlsConfigBuilder.setPskStore(new LwM2mBootstrapPskStore(securityStore));
            } else {
                LOG.warn(
                        "PskStore should be automatically set by Leshan. Using a custom implementation is not advised.");
            }

            // Handle secure address
            if (incompleteConfig.getAddress() == null) {
                if (localAddressSecure == null) {
                    localAddressSecure = new InetSocketAddress(0);
                }
                dtlsConfigBuilder.setAddress(localAddressSecure);
            } else if (localAddressSecure != null && !localAddressSecure.equals(incompleteConfig.getAddress())) {
                throw new IllegalStateException(String.format(
                        "Configuration conflict between LeshanBuilder and DtlsConnectorConfig.Builder for secure address: %s != %s",
                        localAddressSecure, incompleteConfig.getAddress()));
            }

            // Handle active peers
            if (incompleteConfig.getMaxConnections() == null)
                dtlsConfigBuilder.setMaxConnections(coapConfig.getInt(Keys.MAX_ACTIVE_PEERS));
            if (incompleteConfig.getStaleConnectionThreshold() == null)
                dtlsConfigBuilder.setStaleConnectionThreshold(coapConfig.getLong(Keys.MAX_PEER_INACTIVITY_PERIOD));

            // handle trusted certificates
            if (trustedCertificates != null) {
                if (incompleteConfig.getTrustStore() == null) {
                    dtlsConfigBuilder.setTrustStore(trustedCertificates);
                } else if (!Arrays.equals(trustedCertificates, incompleteConfig.getTrustStore())) {
                    throw new IllegalStateException(String.format(
                            "Configuration conflict between LeshanBuilder and DtlsConnectorConfig.Builder for trusted Certificates (trustStore) : \n%s != \n%s",
                            Arrays.toString(trustedCertificates), Arrays.toString(incompleteConfig.getTrustStore())));
                }
            }

            // check conflict for private key
            if (privateKey != null) {
                if (incompleteConfig.getPrivateKey() != null && !incompleteConfig.getPrivateKey().equals(privateKey)) {
                    throw new IllegalStateException(String.format(
                            "Configuration conflict between LeshanBuilder and DtlsConnectorConfig.Builder for private key: %s != %s",
                            privateKey, incompleteConfig.getPrivateKey()));
                }

                // if in raw key mode and not in X.509 set the raw keys
                if (certificateChain == null && publicKey != null) {
                    if (incompleteConfig.getPublicKey() != null && !incompleteConfig.getPublicKey().equals(publicKey)) {
                        throw new IllegalStateException(String.format(
                                "Configuration conflict between LeshanBuilder and DtlsConnectorConfig.Builder for public key: %s != %s",
                                publicKey, incompleteConfig.getPublicKey()));
                    }

                    dtlsConfigBuilder.setIdentity(privateKey, publicKey);
                }
                // if in X.509 mode set the private key, certificate chain, public key is extracted from the certificate
                if (certificateChain != null && certificateChain.length > 0) {
                    if (incompleteConfig.getCertificateChain() != null
                            && !Arrays.equals(incompleteConfig.getCertificateChain(), certificateChain)) {
                        throw new IllegalStateException(String.format(
                                "Configuration conflict between LeshanBuilder and DtlsConnectorConfig.Builder for certificate chain: %s != %s",
                                Arrays.toString(certificateChain),
                                Arrays.toString(incompleteConfig.getCertificateChain())));
                    }

                    dtlsConfigBuilder.setIdentity(privateKey, certificateChain, false);
                }
            }

            // Deactivate SNI by default
            // TODO should we support SNI ?
            if (incompleteConfig.isSniEnabled() == null) {
                dtlsConfigBuilder.setSniEnabled(false);
            }

            // we try to build the dtlsConfig, if it fail we will just not create the secured endpoint
            try {
                dtlsConfig = dtlsConfigBuilder.build();
            } catch (IllegalStateException e) {
            }
        }

        CoapEndpoint unsecuredEndpoint = null;
        if (!noUnsecuredEndpoint) {
            if (endpointFactory != null) {
                unsecuredEndpoint = endpointFactory.createUnsecuredEndpoint(localAddress, coapConfig, null);
            } else {
                CoapEndpoint.CoapEndpointBuilder builder = new CoapEndpoint.CoapEndpointBuilder();
                builder.setInetSocketAddress(localAddress);
                builder.setNetworkConfig(coapConfig);
                unsecuredEndpoint = builder.build();
            }
        }

        CoapEndpoint securedEndpoint = null;
        if (!noSecuredEndpoint && dtlsConfig != null) {
            if (endpointFactory != null) {
                securedEndpoint = endpointFactory.createSecuredEndpoint(dtlsConfig, coapConfig, null);
            } else {
                CoapEndpoint.CoapEndpointBuilder builder = new CoapEndpoint.CoapEndpointBuilder();
                builder.setConnector(new DTLSConnector(dtlsConfig));
                builder.setNetworkConfig(coapConfig);
                builder.setEndpointContextMatcher(new Lwm2mEndpointContextMatcher());
                securedEndpoint = builder.build();
            }
        }

        if (securedEndpoint == null && unsecuredEndpoint == null) {
            throw new IllegalStateException(
                    "All CoAP enpoints are deactivated, at least one endpoint should be activated");
        }

        return new LeshanBootstrapServer(unsecuredEndpoint, securedEndpoint, configStore, securityStore, sessionManager,
                model, coapConfig);
    }
}
