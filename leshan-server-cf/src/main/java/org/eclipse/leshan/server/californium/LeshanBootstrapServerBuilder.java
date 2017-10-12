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
 *******************************************************************************/
package org.eclipse.leshan.server.californium;

import java.net.InetSocketAddress;

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
                unsecuredEndpoint = new CoapEndpoint(localAddress, coapConfig);
            }
        }

        CoapEndpoint securedEndpoint = null;
        if (!noSecuredEndpoint && dtlsConfig != null) {
            if (endpointFactory != null) {
                securedEndpoint = endpointFactory.createSecuredEndpoint(dtlsConfig, coapConfig, null);
            } else {
                securedEndpoint = new CoapEndpoint(new DTLSConnector(dtlsConfig), coapConfig, null, null,
                        new Lwm2mEndpointContextMatcher());
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
