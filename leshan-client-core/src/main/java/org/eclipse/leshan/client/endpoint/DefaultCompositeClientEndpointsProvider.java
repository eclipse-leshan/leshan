/*******************************************************************************
 * Copyright (c) 2023 Sierra Wireless and others.
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
package org.eclipse.leshan.client.endpoint;

import java.security.cert.Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.eclipse.leshan.client.notification.NotificationManager;
import org.eclipse.leshan.client.request.DownlinkRequestReceiver;
import org.eclipse.leshan.client.resource.LwM2mObjectTree;
import org.eclipse.leshan.client.servers.LwM2mServer;
import org.eclipse.leshan.client.servers.ServerInfo;

/**
 * Default implementation of {@link CompositeClientEndpointsProvider}.
 * <p>
 * It allows to use several {@link LwM2mClientEndpointsProvider} on same Leshan server.
 */
public class DefaultCompositeClientEndpointsProvider implements CompositeClientEndpointsProvider {

    private final List<LwM2mClientEndpointsProvider> providers;

    public DefaultCompositeClientEndpointsProvider(LwM2mClientEndpointsProvider... providers) {
        this(Arrays.asList(providers));
    }

    public DefaultCompositeClientEndpointsProvider(Collection<LwM2mClientEndpointsProvider> providers) {
        this.providers = Collections.unmodifiableList(new ArrayList<>(providers));
    }

    @Override
    public void init(LwM2mObjectTree objectTree, DownlinkRequestReceiver requestReceiver,
            NotificationManager notificationManager, ClientEndpointToolbox toolbox) {
        for (LwM2mClientEndpointsProvider provider : providers) {
            provider.init(objectTree, requestReceiver, notificationManager, toolbox);
        }
    }

    @Override
    public LwM2mServer createEndpoint(ServerInfo serverInfo, boolean clientInitiatedOnly, List<Certificate> trustStore,
            ClientEndpointToolbox toolbox) {
        for (LwM2mClientEndpointsProvider provider : providers) {
            LwM2mServer server = provider.createEndpoint(serverInfo, clientInitiatedOnly, trustStore, toolbox);
            if (server != null) {
                return server;
            }
        }
        return null;
    }

    @Override
    public Collection<LwM2mServer> createEndpoints(Collection<? extends ServerInfo> serverInfo,
            boolean clientInitiatedOnly, List<Certificate> trustStore, ClientEndpointToolbox toolbox) {
        // not implemented yet ...
        return null;
    }

    @Override
    public void destroyEndpoints() {
        for (LwM2mClientEndpointsProvider provider : providers) {
            provider.destroyEndpoints();
        }
    }

    @Override
    public void start() {
        for (LwM2mClientEndpointsProvider provider : providers) {
            provider.start();
        }
    }

    @Override
    public List<LwM2mClientEndpoint> getEndpoints() {
        List<LwM2mClientEndpoint> endpoints = new ArrayList<>();
        for (LwM2mClientEndpointsProvider provider : providers) {
            endpoints.addAll(provider.getEndpoints());
        }
        return endpoints;
    }

    @Override
    public LwM2mClientEndpoint getEndpoint(LwM2mServer server) {
        for (LwM2mClientEndpointsProvider provider : providers) {
            LwM2mClientEndpoint endpoint = provider.getEndpoint(server);
            if (endpoint != null) {
                return endpoint;
            }
        }
        return null;
    }

    @Override
    public void stop() {
        for (LwM2mClientEndpointsProvider provider : providers) {
            provider.stop();
        }

    }

    @Override
    public void destroy() {
        for (LwM2mClientEndpointsProvider provider : providers) {
            provider.destroy();
        }
    }

    @Override
    public Collection<LwM2mClientEndpointsProvider> getProviders() {
        return providers;
    }
}
