/*******************************************************************************
 * Copyright (c) 2013-2015 Sierra Wireless and others.
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
package org.eclipse.leshan.server.californium.impl;

import java.net.InetAddress;
import java.net.InetSocketAddress;

import org.eclipse.californium.core.CoapServer;
import org.eclipse.californium.core.network.CoapEndpoint;
import org.eclipse.californium.core.network.Endpoint;
import org.eclipse.californium.core.network.config.NetworkConfig;
import org.eclipse.californium.scandium.DTLSConnector;
import org.eclipse.californium.scandium.config.DtlsConnectorConfig;
import org.eclipse.californium.scandium.config.DtlsConnectorConfig.Builder;
import org.eclipse.leshan.server.bootstrap.BootstrapStore;
import org.eclipse.leshan.server.bootstrap.LwM2mBootstrapServer;
import org.eclipse.leshan.server.security.SecurityStore;
import org.eclipse.leshan.util.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A Lightweight M2M server, serving bootstrap information on /bs.
 */
public class LwM2mBootstrapServerImpl implements LwM2mBootstrapServer {

    private final static Logger LOG = LoggerFactory.getLogger(LwM2mBootstrapServerImpl.class);

    /** IANA assigned UDP port for CoAP (so for LWM2M) */
    public static final int PORT = 5683;

    /** IANA assigned UDP port for CoAP with DTLS (so for LWM2M) */
    public static final int PORT_DTLS = 5684;

    private final CoapServer coapServer;

    private final BootstrapStore bsStore;

    private final SecurityStore securityStore;

    public LwM2mBootstrapServerImpl(BootstrapStore bsStore, SecurityStore securityStore) {
        this(new InetSocketAddress((InetAddress) null, PORT), new InetSocketAddress((InetAddress) null, PORT_DTLS),
                bsStore, securityStore);

    }

    public LwM2mBootstrapServerImpl(InetSocketAddress localAddress, InetSocketAddress localAddressSecure,
            BootstrapStore bsStore, SecurityStore securityStore) {
        Validate.notNull(bsStore, "bootstrap store must not be null");

        this.bsStore = bsStore;
        this.securityStore = securityStore;
        // init CoAP server
        coapServer = new CoapServer();
        Endpoint endpoint = new CoapEndpoint(localAddress);
        coapServer.addEndpoint(endpoint);

        // init DTLS server
        Builder builder = new DtlsConnectorConfig.Builder(localAddressSecure);
        builder.setPskStore(new LwM2mPskStore(this.securityStore));

        Endpoint secureEndpoint = new CoapEndpoint(new DTLSConnector(builder.build()), NetworkConfig.getStandard());
        coapServer.addEndpoint(secureEndpoint);

        // define /bs ressource
        BootstrapResource bsResource = new BootstrapResource(bsStore);
        coapServer.add(bsResource);
    }

    @Override
    public BootstrapStore getBoostrapStore() {
        return bsStore;
    }

    @Override
    public SecurityStore getSecurityStore() {
        return securityStore;
    }

    /**
     * Starts the server and binds it to the specified port.
     */
    public void start() {
        coapServer.start();
        LOG.info("LW-M2M server started");
    }

    /**
     * Stops the server and unbinds it from assigned ports (can be restarted).
     */
    public void stop() {
        coapServer.stop();
    }

    /**
     * Stops the server and unbinds it from assigned ports.
     */
    public void destroy() {
        coapServer.destroy();
    }
}