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
 *     Rikard Höglund (RISE SICS) - Additions to support OSCORE
 *******************************************************************************/
package org.eclipse.leshan.client.endpoint;

import java.security.cert.Certificate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.eclipse.leshan.client.EndpointsManager;
import org.eclipse.leshan.client.servers.ServerIdentity;
import org.eclipse.leshan.client.servers.ServerInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An {@link EndpointsManager} which supports only 1 server.
 */
public class DefaultEndpointsManager implements EndpointsManager {

    private static final Logger LOG = LoggerFactory.getLogger(DefaultEndpointsManager.class);

    // utility
    protected LwM2mClientEndpointsProvider endpointProvider;
    protected ClientEndpointToolbox toolbox;
    protected List<Certificate> trustStore;

    // state
    protected boolean started = false;

    public DefaultEndpointsManager(LwM2mClientEndpointsProvider endpointProvider, ClientEndpointToolbox toolbox,
            List<Certificate> trustStore) {
        this.endpointProvider = endpointProvider;
        this.toolbox = toolbox;
        this.trustStore = trustStore;
    }

    @Override
    public synchronized ServerIdentity createEndpoint(ServerInfo serverInfo, boolean clientInitiatedOnly) {
        // Clear previous endpoint
        endpointProvider.destroyEndpoints();

        // Create new endpoint
        return endpointProvider.createEndpoint(serverInfo, clientInitiatedOnly, trustStore, toolbox);
    }

    @Override
    public synchronized Collection<ServerIdentity> createEndpoints(Collection<? extends ServerInfo> serverInfo,
            boolean clientInitiatedOnly) {
        if (serverInfo == null || serverInfo.isEmpty())
            return null;
        else {
            // TODO support multi server
            if (serverInfo.size() > 1) {
                LOG.warn(
                        "CaliforniumEndpointsManager support only connection to 1 LWM2M server, first server will be used from the server list of {}",
                        serverInfo.size());
            }
            ServerInfo firstServer = serverInfo.iterator().next();
            Collection<ServerIdentity> servers = new ArrayList<>(1);
            servers.add(createEndpoint(firstServer, clientInitiatedOnly));
            return servers;
        }
    }

    @Override
    public long getMaxCommunicationPeriodFor(ServerIdentity server, long lifetimeInMs) {
        LwM2mClientEndpoint endpoint = getEndpoint(server);
        if (endpoint != null) {
            return endpoint.getMaxCommunicationPeriodFor(lifetimeInMs);
        }
        // TODO TL : we should better handle this case
        return lifetimeInMs;
    }

    @Override
    public synchronized void forceReconnection(ServerIdentity server, boolean resume) {
        LwM2mClientEndpoint endpoint = getEndpoint(server);
        if (endpoint != null) {
            endpoint.forceReconnection(server, resume);
        }
    }

    public synchronized LwM2mClientEndpoint getEndpoint(ServerIdentity server) {
        return endpointProvider.getEndpoint(server);
    }

    @Override
    public synchronized void start() {
        endpointProvider.start();
    }

    @Override
    public synchronized void stop() {
        endpointProvider.stop();

    }

    @Override
    public synchronized void destroy() {
        endpointProvider.destroy();
    }
}
