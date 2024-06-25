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
package org.eclipse.leshan.server.bootstrap.endpoint;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.eclipse.leshan.core.util.Validate;
import org.eclipse.leshan.server.bootstrap.LeshanBootstrapServer;
import org.eclipse.leshan.server.bootstrap.request.BootstrapUplinkRequestReceiver;
import org.eclipse.leshan.server.security.ServerSecurityInfo;

/**
 * Default implementation of {@link CompositeBootstrapServerEndpointsProvider}.
 * <p>
 * It allows to use several {@link LwM2mBootstrapServerEndpointsProvider} on same Leshan server.
 */
public class DefaultCompositeBootstrapServerEndpointsProvider implements CompositeBootstrapServerEndpointsProvider {

    private final List<LwM2mBootstrapServerEndpointsProvider> providers;

    public DefaultCompositeBootstrapServerEndpointsProvider(LwM2mBootstrapServerEndpointsProvider... providers) {
        this(Arrays.asList(providers));
    }

    public DefaultCompositeBootstrapServerEndpointsProvider(
            Collection<LwM2mBootstrapServerEndpointsProvider> providers) {
        Validate.notEmpty(providers);
        this.providers = Collections.unmodifiableList(new ArrayList<>(providers));
    }

    @Override
    public List<LwM2mBootstrapServerEndpoint> getEndpoints() {
        List<LwM2mBootstrapServerEndpoint> endpoints = new ArrayList<>();
        for (LwM2mBootstrapServerEndpointsProvider provider : providers) {
            endpoints.addAll(provider.getEndpoints());
        }
        return endpoints;
    }

    @Override
    public LwM2mBootstrapServerEndpoint getEndpoint(URI uri) {
        for (LwM2mBootstrapServerEndpointsProvider provider : providers) {
            LwM2mBootstrapServerEndpoint endpoint = provider.getEndpoint(uri);
            if (endpoint != null) {
                return endpoint;
            }
        }
        return null;
    }

    @Override
    public void createEndpoints(BootstrapUplinkRequestReceiver requestReceiver, BootstrapServerEndpointToolbox toolbox,
            ServerSecurityInfo serverSecurityInfo, LeshanBootstrapServer server) {
        for (LwM2mBootstrapServerEndpointsProvider provider : providers) {
            provider.createEndpoints(requestReceiver, toolbox, serverSecurityInfo, server);
        }
    }

    @Override
    public void start() {
        for (LwM2mBootstrapServerEndpointsProvider provider : providers) {
            provider.start();
        }
    }

    @Override
    public void stop() {
        for (LwM2mBootstrapServerEndpointsProvider provider : providers) {
            provider.stop();
        }
    }

    @Override
    public void destroy() {
        for (LwM2mBootstrapServerEndpointsProvider provider : providers) {
            provider.destroy();
        }
    }

    @Override
    public Collection<LwM2mBootstrapServerEndpointsProvider> getProviders() {
        return providers;
    }
}
