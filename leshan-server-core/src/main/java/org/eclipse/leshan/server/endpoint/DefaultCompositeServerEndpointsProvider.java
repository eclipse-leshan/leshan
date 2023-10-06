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
package org.eclipse.leshan.server.endpoint;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.eclipse.leshan.core.util.Validate;
import org.eclipse.leshan.server.LeshanServer;
import org.eclipse.leshan.server.observation.LwM2mNotificationReceiver;
import org.eclipse.leshan.server.request.UplinkRequestReceiver;
import org.eclipse.leshan.server.security.ServerSecurityInfo;

/**
 * Default implementation of {@link CompositeServerEndpointsProvider}.
 * <p>
 * It allows to use several {@link LwM2mServerEndpointsProvider} on same Leshan server.
 */
public class DefaultCompositeServerEndpointsProvider implements CompositeServerEndpointsProvider {

    private final List<LwM2mServerEndpointsProvider> providers;

    public DefaultCompositeServerEndpointsProvider(LwM2mServerEndpointsProvider... providers) {
        this(Arrays.asList(providers));
    }

    public DefaultCompositeServerEndpointsProvider(Collection<LwM2mServerEndpointsProvider> providers) {
        Validate.notEmpty(providers);
        this.providers = Collections.unmodifiableList(new ArrayList<>(providers));
    }

    @Override
    public List<LwM2mServerEndpoint> getEndpoints() {
        List<LwM2mServerEndpoint> endpoints = new ArrayList<>();
        for (LwM2mServerEndpointsProvider provider : providers) {
            endpoints.addAll(provider.getEndpoints());
        }
        return endpoints;
    }

    @Override
    public LwM2mServerEndpoint getEndpoint(URI uri) {
        for (LwM2mServerEndpointsProvider provider : providers) {
            LwM2mServerEndpoint endpoint = provider.getEndpoint(uri);
            if (endpoint != null) {
                return endpoint;
            }
        }
        return null;
    }

    @Override
    public void createEndpoints(UplinkRequestReceiver requestReceiver, LwM2mNotificationReceiver observationService,
            ServerEndpointToolbox toolbox, ServerSecurityInfo serverSecurityInfo, LeshanServer server) {
        for (LwM2mServerEndpointsProvider provider : providers) {
            provider.createEndpoints(requestReceiver, observationService, toolbox, serverSecurityInfo, server);
        }
    }

    @Override
    public void start() {
        for (LwM2mServerEndpointsProvider provider : providers) {
            provider.start();
        }
    }

    @Override
    public void stop() {
        for (LwM2mServerEndpointsProvider provider : providers) {
            provider.stop();
        }
    }

    @Override
    public void destroy() {
        for (LwM2mServerEndpointsProvider provider : providers) {
            provider.destroy();
        }
    }

    @Override
    public Collection<LwM2mServerEndpointsProvider> getProviders() {
        return providers;
    }
}
