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
 *     Michał Wadowski (Orange) - Improved compliance with rfc6690
 *******************************************************************************/
package org.eclipse.leshan.server.bootstrap;

import java.util.List;

import org.eclipse.leshan.core.Destroyable;
import org.eclipse.leshan.core.Startable;
import org.eclipse.leshan.core.Stoppable;
import org.eclipse.leshan.core.endpoint.Protocol;
import org.eclipse.leshan.core.link.lwm2m.LwM2mLinkParser;
import org.eclipse.leshan.core.node.codec.LwM2mDecoder;
import org.eclipse.leshan.core.node.codec.LwM2mEncoder;
import org.eclipse.leshan.core.util.Validate;
import org.eclipse.leshan.server.bootstrap.endpoint.BootstrapServerEndpointToolbox;
import org.eclipse.leshan.server.bootstrap.endpoint.LwM2mBootstrapServerEndpoint;
import org.eclipse.leshan.server.bootstrap.endpoint.LwM2mBootstrapServerEndpointsProvider;
import org.eclipse.leshan.server.bootstrap.request.BootstrapDownlinkRequestSender;
import org.eclipse.leshan.server.bootstrap.request.DefaultBootstrapDownlinkRequestSender;
import org.eclipse.leshan.server.bootstrap.request.DefaultBootstrapUplinkRequestReceiver;
import org.eclipse.leshan.server.security.BootstrapSecurityStore;
import org.eclipse.leshan.server.security.ServerSecurityInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A Lightweight M2M server, serving bootstrap information on /bs.
 */
public class LeshanBootstrapServer {

    private final static Logger LOG = LoggerFactory.getLogger(LeshanBootstrapServer.class);

    private final BootstrapSessionDispatcher dispatcher = new BootstrapSessionDispatcher();

    private final BootstrapDownlinkRequestSender requestSender;
    private final LwM2mBootstrapServerEndpointsProvider endpointsProvider;
    private final BootstrapSecurityStore securityStore;

    /**
     * /** Initialize a server which will bind to the specified address and port.
     * <p>
     * {@link LeshanBootstrapServerBuilder} is the priviledged way to create a {@link LeshanBootstrapServer}.
     *
     * @param bsSessionManager manages life cycle of a bootstrap process
     * @param bsHandlerFactory responsible to create the {@link BootstrapHandler}
     * @param encoder encode used to encode request payload.
     * @param decoder decoder used to decode response payload.
     * @param linkParser a parser {@link LwM2mLinkParser} used to parse a CoRE Link.
     */
    public LeshanBootstrapServer(LwM2mBootstrapServerEndpointsProvider endpointsProvider,
            BootstrapSessionManager bsSessionManager, BootstrapHandlerFactory bsHandlerFactory, LwM2mEncoder encoder,
            LwM2mDecoder decoder, LwM2mLinkParser linkParser, BootstrapSecurityStore securityStore,
            ServerSecurityInfo serverSecurityInfo) {

        Validate.notNull(endpointsProvider, "endpoints provider must not be null");
        Validate.notNull(bsSessionManager, "session manager must not be null");
        Validate.notNull(bsHandlerFactory, "BootstrapHandler factory must not be null");
        this.endpointsProvider = endpointsProvider;
        this.securityStore = securityStore;

        // create request sender
        requestSender = createRequestSender(endpointsProvider);

        // create endpoints
        BootstrapServerEndpointToolbox toolbox = new BootstrapServerEndpointToolbox(decoder, encoder, linkParser);
        DefaultBootstrapUplinkRequestReceiver requestReceiver = new DefaultBootstrapUplinkRequestReceiver(
                bsHandlerFactory.create(requestSender, bsSessionManager, dispatcher));
        endpointsProvider.createEndpoints(requestReceiver, toolbox, serverSecurityInfo, this);
    }

    protected BootstrapDownlinkRequestSender createRequestSender(
            LwM2mBootstrapServerEndpointsProvider endpointsProvider) {
        return new DefaultBootstrapDownlinkRequestSender(endpointsProvider);
    }

    /**
     * Starts the server and binds it to the specified port.
     */
    public void start() {
        if (requestSender instanceof Startable) {
            ((Startable) requestSender).start();
        }

        endpointsProvider.start();

        if (LOG.isInfoEnabled()) {
            LOG.info("Bootstrap server started.");
            for (LwM2mBootstrapServerEndpoint endpoint : endpointsProvider.getEndpoints()) {
                LOG.info("{} endpoint available at {}.", endpoint.getProtocol().getName(), endpoint.getURI());
            }
        }
    }

    /**
     * Stops the server and unbinds it from assigned ports (can be restarted).
     */
    public void stop() {
        endpointsProvider.stop();

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
        endpointsProvider.destroy();

        if (requestSender instanceof Destroyable) {
            ((Destroyable) requestSender).destroy();
        } else if (requestSender instanceof Stoppable) {
            ((Stoppable) requestSender).stop();
        }
        LOG.info("Bootstrap server destroyed.");
    }

    public void addListener(BootstrapSessionListener listener) {
        dispatcher.addListener(listener);
    }

    public void removeListener(BootstrapSessionListener listener) {
        dispatcher.removeListener(listener);
    }

    public List<LwM2mBootstrapServerEndpoint> getEndpoints() {
        return endpointsProvider.getEndpoints();
    }

    public LwM2mBootstrapServerEndpoint getEndpoint(Protocol protocol) {
        for (LwM2mBootstrapServerEndpoint endpoint : endpointsProvider.getEndpoints()) {
            if (endpoint.getProtocol().equals(protocol)) {
                return endpoint;
            }
        }
        return null;
    }

    public BootstrapSecurityStore getSecurityStore() {
        return securityStore;
    }
}
