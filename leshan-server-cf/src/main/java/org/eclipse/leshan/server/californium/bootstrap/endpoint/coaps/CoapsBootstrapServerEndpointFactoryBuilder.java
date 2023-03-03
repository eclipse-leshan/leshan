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
package org.eclipse.leshan.server.californium.bootstrap.endpoint.coaps;

import java.util.List;
import java.util.function.Consumer;

import org.eclipse.californium.elements.config.Configuration;
import org.eclipse.californium.elements.config.Configuration.ModuleDefinitionsProvider;
import org.eclipse.californium.scandium.config.DtlsConnectorConfig;
import org.eclipse.leshan.core.endpoint.Protocol;
import org.eclipse.leshan.server.californium.endpoint.AbstractEndpointFactoryBuilder;

public class CoapsBootstrapServerEndpointFactoryBuilder extends
        AbstractEndpointFactoryBuilder<CoapsBootstrapServerEndpointFactoryBuilder, CoapsBootstrapServerEndpointFactory> {

    protected Consumer<DtlsConnectorConfig.Builder> dtlsConnectorConfigInitializer;

    @Override
    protected Protocol getSupportedProtocol() {
        return CoapsBootstrapServerEndpointFactory.getSupportedProtocol();
    }

    @Override
    public void applyDefaultValue(Configuration configuration) {
        CoapsBootstrapServerEndpointFactory.applyDefaultValue(configuration);

    }

    @Override
    public List<ModuleDefinitionsProvider> getModuleDefinitionsProviders() {
        return CoapsBootstrapServerEndpointFactory.getModuleDefinitionsProviders();
    }

    public CoapsBootstrapServerEndpointFactoryBuilder setDtlsConnectorConfig(
            Consumer<DtlsConnectorConfig.Builder> dtlsConnectorConfigInitializer) {
        this.dtlsConnectorConfigInitializer = dtlsConnectorConfigInitializer;
        return this;
    }

    @Override
    public CoapsBootstrapServerEndpointFactory build() {
        return new CoapsBootstrapServerEndpointFactory(uri, loggingTagPrefix, configuration,
                dtlsConnectorConfigInitializer, coapEndpointConfigInitializer);
    }
}
