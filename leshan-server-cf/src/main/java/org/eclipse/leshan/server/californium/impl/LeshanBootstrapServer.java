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

import java.net.InetSocketAddress;

import org.eclipse.californium.core.CoapServer;
import org.eclipse.californium.core.network.CoapEndpoint;
import org.eclipse.californium.core.network.config.NetworkConfig;
import org.eclipse.leshan.core.model.LwM2mModel;
import org.eclipse.leshan.server.bootstrap.BootstrapHandler;
import org.eclipse.leshan.server.bootstrap.BootstrapSessionManager;
import org.eclipse.leshan.server.bootstrap.BootstrapStore;
import org.eclipse.leshan.server.bootstrap.LwM2mBootstrapRequestSender;
import org.eclipse.leshan.server.bootstrap.LwM2mBootstrapServer;
import org.eclipse.leshan.server.security.BootstrapSecurityStore;
import org.eclipse.leshan.util.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A Lightweight M2M server, serving bootstrap information on /bs.
 */
public class LeshanBootstrapServer implements LwM2mBootstrapServer {

    private final static Logger LOG = LoggerFactory.getLogger(LeshanBootstrapServer.class);

    private final CoapServer coapServer;
    private final CoapEndpoint unsecuredEndpoint;
    private final CoapEndpoint securedEndpoint;

    private final BootstrapStore bsStore;
    private final BootstrapSecurityStore bsSecurityStore;

    public LeshanBootstrapServer(CoapEndpoint unsecuredEndpoint, CoapEndpoint securedEndpoint, BootstrapStore bsStore,
            BootstrapSecurityStore bsSecurityStore, BootstrapSessionManager bsSessionManager, LwM2mModel model,
            NetworkConfig coapConfig) {

        Validate.notNull(bsStore, "bootstrap store must not be null");
        Validate.notNull(bsSessionManager, "session manager must not be null");
        Validate.notNull(model, "model must not be null");
        Validate.notNull(coapConfig, "coapConfig must not be null");

        this.bsStore = bsStore;
        this.bsSecurityStore = bsSecurityStore;

        // init CoAP server
        coapServer = new CoapServer(coapConfig);
        this.unsecuredEndpoint = unsecuredEndpoint;
        if (unsecuredEndpoint != null)
            coapServer.addEndpoint(unsecuredEndpoint);

        // init DTLS server
        this.securedEndpoint = securedEndpoint;
        if (securedEndpoint != null)
            coapServer.addEndpoint(securedEndpoint);

        // create request sender
        LwM2mBootstrapRequestSender requestSender = new CaliforniumLwM2mBootstrapRequestSender(securedEndpoint,
                unsecuredEndpoint, model);

        BootstrapResource bsResource = new BootstrapResource(
                new BootstrapHandler(bsStore, requestSender, bsSessionManager));
        coapServer.add(bsResource);
    }

    @Override
    public BootstrapSecurityStore getBootstrapSecurityStore() {
        return bsSecurityStore;
    }

    @Override
    public BootstrapStore getBoostrapStore() {
        return bsStore;
    }

    /**
     * Starts the server and binds it to the specified port.
     */
    @Override
    public void start() {
        coapServer.start();

        if (LOG.isInfoEnabled()) {
            LOG.info("Bootstrap server started at {} {}",
                    getUnsecuredAddress() == null ? "" : "coap://" + getUnsecuredAddress(),
                    getSecuredAddress() == null ? "" : "coaps://" + getSecuredAddress());
        }
    }

    /**
     * Stops the server and unbinds it from assigned ports (can be restarted).
     */
    @Override
    public void stop() {
        coapServer.stop();
        LOG.info("Bootstrap server stopped.");
    }

    /**
     * Stops the server and unbinds it from assigned ports.
     */
    public void destroy() {
        coapServer.destroy();
        LOG.info("Bootstrap server destroyed.");
    }

    public InetSocketAddress getUnsecuredAddress() {
        if (unsecuredEndpoint != null) {
            return unsecuredEndpoint.getAddress();
        } else {
            return null;
        }
    }

    public InetSocketAddress getSecuredAddress() {
        if (securedEndpoint != null) {
            return securedEndpoint.getAddress();
        } else {
            return null;
        }
    }
}