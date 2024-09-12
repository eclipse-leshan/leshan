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
package org.eclipse.leshan.transport.californium.server.endpoint;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.function.Consumer;

import org.eclipse.californium.core.network.CoapEndpoint;
import org.eclipse.californium.elements.config.Configuration;
import org.eclipse.californium.elements.config.Configuration.ModuleDefinitionsProvider;
import org.eclipse.leshan.core.endpoint.EndPointUriHandler;
import org.eclipse.leshan.core.endpoint.EndpointUri;
import org.eclipse.leshan.core.endpoint.Protocol;
import org.eclipse.leshan.core.util.Validate;

public abstract class AbstractEndpointFactoryBuilder<SELF extends AbstractEndpointFactoryBuilder<SELF, TTarget>, TTarget> {

    protected EndpointUri uri;
    protected String loggingTagPrefix;
    protected Configuration configuration;
    protected Consumer<CoapEndpoint.Builder> coapEndpointConfigInitializer;
    protected EndPointUriHandler uriHandler;

    public AbstractEndpointFactoryBuilder(EndPointUriHandler uriHandler) {
        Validate.notNull(uriHandler);
        this.uriHandler = uriHandler;
    }

    @SuppressWarnings("unchecked")
    private SELF self() {
        return (SELF) this;
    }

    protected abstract Protocol getSupportedProtocol();

    protected abstract void applyDefaultValue(Configuration configuration);

    protected abstract List<ModuleDefinitionsProvider> getModuleDefinitionsProviders();

    protected EndPointUriHandler getUriHandler() {
        return uriHandler;
    }

    public abstract TTarget build();

    public SELF setURI(EndpointUri uri) {
        uriHandler.validateURI(uri);
        this.uri = uri;
        return self();
    }

    public SELF setURI(int port) {
        return setURI(new InetSocketAddress(port));
    }

    public SELF setURI(InetSocketAddress addr) {
        this.uri = uriHandler.createUri(getSupportedProtocol().getUriScheme(), addr);
        return self();
    }

    public SELF setURI(String uriAsString) {
        EndpointUri uri = uriHandler.createUri(uriAsString);
        uriHandler.validateURI(uri);

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
