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
package org.eclipse.leshan.transport.californium.server.endpoint.coaps;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.function.Consumer;

import org.eclipse.californium.core.config.CoapConfig;
import org.eclipse.californium.elements.config.Configuration;
import org.eclipse.californium.elements.config.Configuration.ModuleDefinitionsProvider;
import org.eclipse.californium.scandium.config.DtlsConnectorConfig;
import org.eclipse.leshan.core.endpoint.EndpointUri;
import org.eclipse.leshan.core.endpoint.EndpointUriUtil;
import org.eclipse.leshan.core.endpoint.Protocol;
import org.eclipse.leshan.transport.californium.server.endpoint.CaliforniumServerEndpointFactory;
import org.eclipse.leshan.transport.californium.server.endpoint.ServerProtocolProvider;

public class CoapsServerProtocolProvider implements ServerProtocolProvider {

    protected Consumer<DtlsConnectorConfig.Builder> dtlsConnectorConfigInitializer;

    public CoapsServerProtocolProvider() {
    }

    public CoapsServerProtocolProvider(Consumer<DtlsConnectorConfig.Builder> dtlsConnectorConfigInitializer) {
        this.dtlsConnectorConfigInitializer = dtlsConnectorConfigInitializer;
    }

    @Override
    public Protocol getProtocol() {
        return CoapsServerEndpointFactory.getSupportedProtocol();
    }

    @Override
    public void applyDefaultValue(Configuration configuration) {
        CoapsServerEndpointFactory.applyDefaultValue(configuration);
    }

    @Override
    public List<ModuleDefinitionsProvider> getModuleDefinitionsProviders() {
        return CoapsServerEndpointFactory.getModuleDefinitionsProviders();
    }

    @Override
    public CaliforniumServerEndpointFactory createDefaultEndpointFactory(EndpointUri uri) {
        return new CoapsServerEndpointFactoryBuilder().setURI(uri)
                .setDtlsConnectorConfig(dtlsConnectorConfigInitializer).build();
    }

    @Override
    public EndpointUri getDefaultUri(Configuration configuration) {
        return EndpointUriUtil.createUri(getProtocol().getUriScheme(),
                new InetSocketAddress(configuration.get(CoapConfig.COAP_SECURE_PORT)));
    }
}
