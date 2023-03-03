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
package org.eclipse.leshan.server.californium.endpoint;

import java.net.InetSocketAddress;
import java.net.URI;
import java.util.List;
import java.util.function.Consumer;

import org.eclipse.californium.core.network.CoapEndpoint;
import org.eclipse.californium.elements.config.Configuration;
import org.eclipse.californium.elements.config.Configuration.ModuleDefinitionsProvider;
import org.eclipse.leshan.core.endpoint.EndpointUriUtil;
import org.eclipse.leshan.core.endpoint.Protocol;

public abstract class AbstractEndpointFactoryBuilder<SELF extends AbstractEndpointFactoryBuilder<SELF, TTarget>, TTarget> {

    protected URI uri;
    protected String loggingTagPrefix;
    protected Configuration configuration;
    protected Consumer<CoapEndpoint.Builder> coapEndpointConfigInitializer;

    @SuppressWarnings("unchecked")
    private SELF self() {
        return (SELF) this;
    }

    protected abstract Protocol getSupportedProtocol();

    protected abstract void applyDefaultValue(Configuration configuration);

    protected abstract List<ModuleDefinitionsProvider> getModuleDefinitionsProviders();

    public abstract TTarget build();

    public SELF setURI(URI uri) {
        EndpointUriUtil.validateURI(uri);
        this.uri = uri;
        return self();
    }

    public SELF setURI(int port) {
        return setURI(new InetSocketAddress(port));
    }

    public SELF setURI(InetSocketAddress addr) {
        this.uri = EndpointUriUtil.createUri(getSupportedProtocol().getUriScheme(), addr);
        return self();
    }

    public SELF setURI(String uriAsString) {
        URI uri = EndpointUriUtil.createUri(uriAsString);
        EndpointUriUtil.validateURI(uri);

        if (!getSupportedProtocol().getUriScheme().equals(uri.getScheme())) {
            throw new IllegalArgumentException(String.format("Invalid URI[%s]: Protocol Scheme MUST NOT be [%s]", uri,
                    getSupportedProtocol().getUriScheme()));
        }

        return self();
    }

    public SELF setLoggingTagPrefix(String loggingTagPrefix) {
        this.loggingTagPrefix = loggingTagPrefix;
        return self();
    }

    public SELF setConfiguration(Consumer<Configuration> configurationInitializer) {
        // Create Configuration
        List<ModuleDefinitionsProvider> moduleDefinitions = getModuleDefinitionsProviders();
        Configuration configuration = new Configuration(
                moduleDefinitions.toArray(new ModuleDefinitionsProvider[moduleDefinitions.size()]));
        // Apply default value
        applyDefaultValue(configuration);

        // Apply initializer
        configurationInitializer.accept(configuration);

        // if all OK, save configuration
        this.configuration = configuration;
        return self();
    }

    public SELF setCoapEndpointConfig(Consumer<CoapEndpoint.Builder> coapEndpointConfigInitializer) {
        this.coapEndpointConfigInitializer = coapEndpointConfigInitializer;
        return self();
    }

}
