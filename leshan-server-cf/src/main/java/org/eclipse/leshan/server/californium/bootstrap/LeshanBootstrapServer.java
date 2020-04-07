/*******************************************************************************
 * Copyright (c) 2013-2015 Sierra Wireless and others.
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
package org.eclipse.leshan.server.californium.bootstrap;

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
import org.eclipse.leshan.core.util.Validate;
import org.eclipse.leshan.server.Destroyable;
import org.eclipse.leshan.server.Startable;
import org.eclipse.leshan.server.Stoppable;
import org.eclipse.leshan.server.bootstrap.BootstrapConfigStore;
import org.eclipse.leshan.server.bootstrap.BootstrapHandler;
import org.eclipse.leshan.server.bootstrap.BootstrapHandlerFactory;
import org.eclipse.leshan.server.bootstrap.BootstrapSessionManager;
import org.eclipse.leshan.server.bootstrap.LwM2mBootstrapRequestSender;
import org.eclipse.leshan.server.californium.RootResource;
import org.eclipse.leshan.server.security.BootstrapSecurityStore;
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

    private LwM2mBootstrapRequestSender requestSender;

    /**
     * /** Initialize a server which will bind to the specified address and port.
     * <p>
     * {@link LeshanBootstrapServerBuilder} is the priviledged way to create a {@link LeshanBootstrapServer}.
     * 
     * @param unsecuredEndpoint CoAP endpoint used for <code>coap://</code> communication.
     * @param securedEndpoint CoAP endpoint used for <code>coaps://</code> communication.
     * @param bsStore the store containing bootstrap configuration to apply during a bootstrap session.
     * @param bsSecurityStore the store containing security information needed to authenticate a client.
     * @param bsSessionManager manages life cycle of a bootstrap process
     * @param bsHandlerFactory responsible to create the {@link BootstrapHandler}
     * @param model the {@link LwM2mModel} used mainly to decode an encode LWM2M payload.
     * @param coapConfig the CoAP {@link NetworkConfig}.
     * @param encoder encode used to encode request payload.
     * @param decoder decoder used to decode response payload.
     */
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
        requestSender = createRequestSender(securedEndpoint, unsecuredEndpoint, model, encoder, decoder);

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

    /**
     * Security store used for DTLS authentication on the bootstrap resource.
     * 
     * @return the {@link BootstrapSecurityStore} containing data used to authenticate devices.
     */
    public BootstrapSecurityStore getBootstrapSecurityStore() {
        return bsSecurityStore;
    }

    /**
     * Access to the bootstrap configuration store. It's used for sending configuration to the devices initiating a
     * bootstrap.
     * 
     * @return the {@link BootstrapConfigStore} containing configuration to apply to each devices.
     */
    public BootstrapConfigStore getBoostrapStore() {
        return bsStore;
    }

    /**
     * Starts the server and binds it to the specified port.
     */
    public void start() {
        if (requestSender instanceof Startable) {
            ((Startable) requestSender).start();
        }
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
        if (requestSender instanceof Stoppable) {
            ((Stoppable) requestSender).stop();
        }
        LOG.info("Bootstrap server stopped.");
    }

    /**
     * Destroys the server, unbinds from all ports and frees all system resources.
     * <p>
     * Server can not be restarted anymore.
     */
    public void destroy() {
        coapServer.destroy();

        if (requestSender instanceof Destroyable) {
            ((Destroyable) requestSender).destroy();
        } else if (requestSender instanceof Stoppable) {
            ((Stoppable) requestSender).stop();
        }
        LOG.info("Bootstrap server destroyed.");
    }

    /**
     * @return the {@link InetSocketAddress} used for <code>coap://</code>
     */
    public InetSocketAddress getUnsecuredAddress() {
        if (unsecuredEndpoint != null) {
            return unsecuredEndpoint.getAddress();
        } else {
            return null;
        }
    }

    /**
     * @return the {@link InetSocketAddress} used for <code>coaps://</code>
     */
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
     * e.g. for CoAP monitoring or to directly use underlying {@link CoapServer}.
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