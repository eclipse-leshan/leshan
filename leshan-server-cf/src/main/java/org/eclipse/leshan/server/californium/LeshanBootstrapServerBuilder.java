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

import org.eclipse.californium.core.network.config.NetworkConfig;
import org.eclipse.californium.core.network.config.NetworkConfig.Keys;
import org.eclipse.californium.scandium.config.DtlsConnectorConfig;
import org.eclipse.californium.scandium.config.DtlsConnectorConfig.Builder;
import org.eclipse.leshan.LwM2m;
import org.eclipse.leshan.core.model.LwM2mModel;
import org.eclipse.leshan.core.model.ObjectLoader;
import org.eclipse.leshan.server.bootstrap.BootstrapSessionManager;
import org.eclipse.leshan.server.bootstrap.BootstrapStore;
import org.eclipse.leshan.server.bootstrap.LwM2mBootstrapServer;
import org.eclipse.leshan.server.californium.impl.LeshanBootstrapServer;
import org.eclipse.leshan.server.californium.impl.LwM2mBootstrapPskStore;
import org.eclipse.leshan.server.impl.DefaultBootstrapSessionManager;
import org.eclipse.leshan.server.security.BootstrapSecurityStore;

/**
 * Class helping you to build and configure a Californium based Leshan Bootstrap Lightweight M2M server. Usage: create
 * it, call the different setters for changing the configuration and then call the {@link #build()} method for creating
 * the {@link LwM2mBootstrapServer} ready to operate.
 */
public class LeshanBootstrapServerBuilder {

    private InetSocketAddress localAddress;
    private InetSocketAddress localAddressSecure;
    private BootstrapStore configStore;
    private BootstrapSecurityStore securityStore;
    private BootstrapSessionManager sessionManager;
    private LwM2mModel model;
    private NetworkConfig coapConfig;
    private DtlsConnectorConfig dtlsConfig;

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

    public LeshanBootstrapServerBuilder setCoapConfig(DtlsConnectorConfig dtlsConfig) {
        this.dtlsConfig = dtlsConfig;
        return this;
    }

    public LeshanBootstrapServer build() {
        if (localAddress == null)
            localAddress = new InetSocketAddress(LwM2m.DEFAULT_COAP_PORT);

        // TODO we should have default implementation for BootstrapStore, BootstrapSecurityStore in leshan.server
        // project.
        if (configStore == null)
            throw new IllegalStateException("BootstrapStore is mandatory");
        if (securityStore == null)
            throw new IllegalStateException("BootstrapSecurityStore is mandatory");

        if (sessionManager == null)
            sessionManager = new DefaultBootstrapSessionManager(securityStore);
        if (model == null)
            model = new LwM2mModel(ObjectLoader.loadDefault());
        if (coapConfig == null)
            coapConfig = new NetworkConfig();

        if (dtlsConfig == null) {
            if (localAddressSecure == null)
                localAddressSecure = new InetSocketAddress(LwM2m.DEFAULT_COAP_SECURE_PORT);

            Builder builder = new DtlsConnectorConfig.Builder(localAddressSecure);
            builder.setPskStore(new LwM2mBootstrapPskStore(securityStore));
            builder.setMaxConnections(coapConfig.getInt(Keys.MAX_ACTIVE_PEERS));
            builder.setStaleConnectionThreshold(coapConfig.getLong(Keys.MAX_PEER_INACTIVITY_PERIOD));

            dtlsConfig = builder.build();
        }

        return new LeshanBootstrapServer(localAddress, configStore, securityStore, sessionManager, model, coapConfig,
                dtlsConfig);
    }
}
