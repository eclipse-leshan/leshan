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

import java.net.InetAddress;
import java.net.InetSocketAddress;

import org.eclipse.californium.core.network.config.NetworkConfig;
import org.eclipse.leshan.LwM2m;
import org.eclipse.leshan.core.model.LwM2mModel;
import org.eclipse.leshan.server.bootstrap.BootstrapSessionManager;
import org.eclipse.leshan.server.bootstrap.BootstrapStore;
import org.eclipse.leshan.server.californium.impl.LeshanBootstrapServer;
import org.eclipse.leshan.server.californium.impl.PkiService;
import org.eclipse.leshan.server.impl.DefaultBootstrapSessionManager;
import org.eclipse.leshan.server.security.BootstrapSecurityStore;

/**
 * This class helps you to configure an Eclipse Californium based bootstrap server. Set the different configuration
 * values and call {@link #build()} for creating the server.
 * 
 * All the setter calls can be chained. They return the current instance of the builder so you can write:
 * 
 * <pre>
 * {@code
 * LeshanBootstrapServer server = new LeshanBsServerBuilder(bsStore)
 *              .setLocalAddress("0.0.0.0",5683).setPkiService(pki).build();
 * ...
 * }
 * </pre>
 */
public class LeshanBsServerBuilder {

    private BootstrapSecurityStore securityStore = null;
    private BootstrapStore bsStore;
    private BootstrapSessionManager bsSessionManager = null;
    private PkiService pki = null;

    private InetSocketAddress localAddress;
    private InetSocketAddress localSecureAddress;

    private NetworkConfig networkConfig;
    private LwM2mModel model;

    /**
     * Create the builder, the {@link BootstrapStore} is mandatory, it's storing the configuration the bootstrap server
     * will push to the boostrapting clients.
     */
    public LeshanBsServerBuilder(BootstrapStore bsStore) {
        this.bsStore = bsStore;
    }

    /**
     * <p>
     * Set the address/port for unsecured CoAP Server.
     * </p>
     * 
     * By default a wildcard address and the default CoAP port(5683) is used.
     * 
     * @param hostname The address to bind. If <code>null</code> wildcard address is used.
     * @param port A valid port value is between 0 and 65535. A port number of zero will let the system pick up an
     *        ephemeral port in a bind operation.
     */
    public LeshanBsServerBuilder setLocalAddress(String hostname, int port) {
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
    public LeshanBsServerBuilder setLocalAddress(InetSocketAddress localAddress) {
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
     * 
     */
    public LeshanBsServerBuilder setLocalSecureAddress(String hostname, int port) {
        if (hostname == null) {
            this.localSecureAddress = new InetSocketAddress(port);
        } else {
            this.localSecureAddress = new InetSocketAddress(hostname, port);
        }
        return this;
    }

    /**
     * <p>
     * Set the address for secured CoAP Server (Using DTLS).
     * </p>
     * 
     * By default a wildcard address and the default CoAP port(5684) is used.
     * 
     */
    public LeshanBsServerBuilder setLocalSecureAddress(InetSocketAddress localSecureAddress) {
        this.localSecureAddress = localSecureAddress;
        return this;
    }

    /**
     * Set the security store used by the bootstrap server for authenticating the device over DTLS. If not set the DTLS
     * port will not be bound.
     */
    public LeshanBsServerBuilder setSecurityStore(BootstrapSecurityStore securityStore) {
        this.securityStore = securityStore;
        return this;
    }

    /**
     * Set the implementation in charge of the handling the bootstrap session (start, end and failure).
     * 
     * If not set, the {@link DefaultBootstrapSessionManager} is used (its only role is to authorize an end-point when
     * the session is started).
     */
    public LeshanBsServerBuilder setSessionManager(BootstrapSessionManager sessionManager) {
        this.bsSessionManager = sessionManager;
        return this;
    }

    /**
     * Set the Californium CoAP level configuration: {@link NetworkConfig}
     */
    public LeshanBsServerBuilder setNetworkConfig(NetworkConfig config) {
        this.networkConfig = config;
        return this;
    }

    /**
     * Set the LWM2M model (objects,resources,types..) used for encoding the configuration values into TLV or other
     * format over the wire.
     */
    public LeshanBsServerBuilder setModel(LwM2mModel model) {
        this.model = model;
        return this;
    }

    public LeshanBsServerBuilder setPkiService(PkiService pki) {
        this.pki = pki;
        return this;
    }

    /**
     * Instantiate a {@link LeshanBootstrapServer} using the default values or the values changed using the different
     * setters.
     */
    public LeshanBootstrapServer build() {
        if (localAddress == null) {
            localAddress = new InetSocketAddress((InetAddress) null, LwM2m.DEFAULT_COAP_PORT);
        }
        if (localSecureAddress == null) {
            localSecureAddress = new InetSocketAddress((InetAddress) null, LwM2m.DEFAULT_COAP_SECURE_PORT);
        }
        if (bsSessionManager == null) {
            bsSessionManager = new DefaultBootstrapSessionManager(securityStore);
        }

        return new LeshanBootstrapServer(localAddress, localSecureAddress, bsStore, securityStore, bsSessionManager,
                model, networkConfig, pki);
    }
}
