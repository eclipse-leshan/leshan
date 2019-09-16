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

import org.eclipse.californium.core.CoapResource;
import org.eclipse.californium.core.CoapServer;
import org.eclipse.californium.core.network.CoapEndpoint;
import org.eclipse.californium.core.network.Endpoint;
import org.eclipse.californium.core.network.config.NetworkConfig;
import org.eclipse.californium.core.server.resources.Resource;
import org.eclipse.leshan.core.model.LwM2mModel;
import org.eclipse.leshan.core.node.codec.LwM2mNodeDecoder;
import org.eclipse.leshan.core.node.codec.LwM2mNodeEncoder;
import org.eclipse.leshan.server.bootstrap.BootstrapConfigStore;
import org.eclipse.leshan.server.bootstrap.BootstrapHandler;
import org.eclipse.leshan.server.bootstrap.BootstrapHandlerFactory;
import org.eclipse.leshan.server.bootstrap.BootstrapSessionManager;
import org.eclipse.leshan.server.bootstrap.LwM2mBootstrapRequestSender;
import org.eclipse.leshan.server.security.BootstrapSecurityStore;
import org.eclipse.leshan.util.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A Lightweight M2M server, serving bootstrap information on /bs.
 */
public class LeshanBootstrapServer {

    private final static Logger LOG = LoggerFactory.getLogger(LeshanBootstrapServer.class);

    // CoAP/Californium attributes
    private final CoapAPI coapApi;
    private final CoapServer coapServer;
    private final CoapEndpoint unsecuredEndpoint;
    private final CoapEndpoint securedEndpoint;

    // LWM2M attributes
    private final BootstrapConfigStore bsStore;
    private final BootstrapSecurityStore bsSecurityStore;

    public LeshanBootstrapServer(CoapEndpoint unsecuredEndpoint, CoapEndpoint securedEndpoint,
            BootstrapConfigStore bsStore, BootstrapSecurityStore bsSecurityStore,
            BootstrapSessionManager bsSessionManager, BootstrapHandlerFactory bsHandlerFactory, LwM2mModel model,
            NetworkConfig coapConfig, LwM2mNodeEncoder encoder, LwM2mNodeDecoder decoder) {

        Validate.notNull(bsStore, "bootstrap store must not be null");
        Validate.notNull(bsSessionManager, "session manager must not be null");
        Validate.notNull(bsHandlerFactory, "BootstrapHandler factory must not be null");
        Validate.notNull(model, "model must not be null");
        Validate.notNull(coapConfig, "coapConfig must not be null");

        this.bsStore = bsStore;
        this.bsSecurityStore = bsSecurityStore;
        this.coapApi = new CoapAPI();

        // init CoAP server
        coapServer = createCoapServer(coapConfig);
        this.unsecuredEndpoint = unsecuredEndpoint;
        if (unsecuredEndpoint != null)
            coapServer.addEndpoint(unsecuredEndpoint);

        // init DTLS server
        this.securedEndpoint = securedEndpoint;
        if (securedEndpoint != null)
            coapServer.addEndpoint(securedEndpoint);

        // create request sender
        LwM2mBootstrapRequestSender requestSender = createRequestSender(securedEndpoint, unsecuredEndpoint, model,
                encoder, decoder);

        // create bootstrap resource
        CoapResource bsResource = createBootstrapResource(
                bsHandlerFactory.create(bsStore, requestSender, bsSessionManager));
        coapServer.add(bsResource);
    }

    protected CoapServer createCoapServer(NetworkConfig coapConfig) {
        return new CoapServer(coapConfig) {
            @Override
            protected Resource createRoot() {
                return new RootResource(this);
            }
        };
    }

    protected LwM2mBootstrapRequestSender createRequestSender(Endpoint securedEndpoint, Endpoint unsecuredEndpoint,
            LwM2mModel model, LwM2mNodeEncoder encoder, LwM2mNodeDecoder decoder) {
        return new CaliforniumLwM2mBootstrapRequestSender(securedEndpoint, unsecuredEndpoint, model, encoder, decoder);
    }

    protected CoapResource createBootstrapResource(BootstrapHandler handler) {
        return new BootstrapResource(handler);
    }

    public BootstrapSecurityStore getBootstrapSecurityStore() {
        return bsSecurityStore;
    }

    public BootstrapConfigStore getBoostrapStore() {
        return bsStore;
    }

    /**
     * Starts the server and binds it to the specified port.
     */
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

    /**
     * <p>
     * A CoAP API, generally needed when need to access to underlying CoAP protocol.
     * </p>
     * e.g. for CoAP monitoring or to directly use underlying {@link Coap Server}.
     */
    public CoapAPI coap() {
        return coapApi;
    }

    public class CoapAPI {

        /**
         * @return the underlying {@link CoapServer}
         */
        public CoapServer getServer() {
            return coapServer;
        }

        /**
         * @return the {@link CoapEndpoint} used for secured CoAP communication (coaps://)
         */
        public CoapEndpoint getSecuredEndpoint() {
            return securedEndpoint;
        }

        /**
         * @return the {@link CoapEndpoint} used for unsecured CoAP communication (coap://)
         */
        public CoapEndpoint getUnsecuredEndpoint() {
            return unsecuredEndpoint;
        }
    }
}