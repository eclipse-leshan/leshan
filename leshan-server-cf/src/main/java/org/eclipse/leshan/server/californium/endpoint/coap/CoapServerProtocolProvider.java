/*******************************************************************************
 * Copyright (c) 2022 Sierra Wireless and others.
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
package org.eclipse.leshan.server.californium.endpoint.coap;

import java.net.InetSocketAddress;
import java.net.URI;
import java.util.Arrays;
import java.util.List;

import org.eclipse.californium.core.config.CoapConfig;
import org.eclipse.californium.core.config.CoapConfig.TrackerMode;
import org.eclipse.californium.elements.config.Configuration;
import org.eclipse.californium.elements.config.Configuration.ModuleDefinitionsProvider;
import org.eclipse.californium.elements.config.SystemConfig;
import org.eclipse.californium.elements.config.UdpConfig;
import org.eclipse.leshan.core.endpoint.EndpointUriUtil;
import org.eclipse.leshan.core.endpoint.Protocol;
import org.eclipse.leshan.server.californium.endpoint.CaliforniumServerEndpointFactory;
import org.eclipse.leshan.server.californium.endpoint.ServerProtocolProvider;

public class CoapServerProtocolProvider implements ServerProtocolProvider {

    @Override
    public Protocol getProtocol() {
        return Protocol.COAP;
    }

    @Override
    public void applyDefaultValue(Configuration configuration) {
        configuration.set(CoapConfig.MID_TRACKER, TrackerMode.NULL);
    }

    @Override
    public List<ModuleDefinitionsProvider> getModuleDefinitionsProviders() {
        return Arrays.asList(SystemConfig.DEFINITIONS, CoapConfig.DEFINITIONS, UdpConfig.DEFINITIONS);
    }

    @Override
    public CaliforniumServerEndpointFactory createDefaultEndpointFactory(URI uri) {
        return new CoapServerEndpointFactory(uri);
    }

    @Override
    public URI getDefaultUri(Configuration configuration) {
        return EndpointUriUtil.createUri(getProtocol().getUriScheme(),
                new InetSocketAddress(configuration.get(CoapConfig.COAP_PORT)));
    }
}
