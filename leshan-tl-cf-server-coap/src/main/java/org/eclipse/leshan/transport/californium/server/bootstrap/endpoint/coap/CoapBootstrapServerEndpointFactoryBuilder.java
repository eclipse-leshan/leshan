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
package org.eclipse.leshan.server.californium.bootstrap.endpoint.coap;

import java.util.List;

import org.eclipse.californium.elements.config.Configuration;
import org.eclipse.californium.elements.config.Configuration.ModuleDefinitionsProvider;
import org.eclipse.leshan.core.endpoint.Protocol;
import org.eclipse.leshan.server.californium.endpoint.AbstractEndpointFactoryBuilder;
import org.eclipse.leshan.server.californium.endpoint.coap.CoapServerEndpointFactory;

public class CoapBootstrapServerEndpointFactoryBuilder extends
        AbstractEndpointFactoryBuilder<CoapBootstrapServerEndpointFactoryBuilder, CoapBootstrapServerEndpointFactory> {

    @Override
    protected Protocol getSupportedProtocol() {
        return CoapServerEndpointFactory.getSupportedProtocol();
    }

    @Override
    public void applyDefaultValue(Configuration configuration) {
        CoapServerEndpointFactory.applyDefaultValue(configuration);

    }

    @Override
    public List<ModuleDefinitionsProvider> getModuleDefinitionsProviders() {
        return CoapServerEndpointFactory.getModuleDefinitionsProviders();
    }

    @Override
    public CoapBootstrapServerEndpointFactory build() {
        return new CoapBootstrapServerEndpointFactory(uri, loggingTagPrefix, configuration,
                coapEndpointConfigInitializer);
    }
}
