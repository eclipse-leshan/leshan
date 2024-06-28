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
package org.eclipse.leshan.transport.californium.server.bootstrap.endpoint.coap;

import java.net.InetSocketAddress;
import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

import org.eclipse.californium.core.config.CoapConfig;
import org.eclipse.californium.core.config.CoapConfig.TrackerMode;
import org.eclipse.californium.core.network.CoapEndpoint;
import org.eclipse.californium.core.network.CoapEndpoint.Builder;
import org.eclipse.californium.elements.Connector;
import org.eclipse.californium.elements.UDPConnector;
import org.eclipse.californium.elements.config.Configuration;
import org.eclipse.californium.elements.config.Configuration.ModuleDefinitionsProvider;
import org.eclipse.californium.elements.config.SystemConfig;
import org.eclipse.californium.elements.config.UdpConfig;
import org.eclipse.leshan.core.endpoint.EndpointUriUtil;
import org.eclipse.leshan.core.endpoint.Protocol;
import org.eclipse.leshan.server.bootstrap.LeshanBootstrapServer;
import org.eclipse.leshan.server.security.ServerSecurityInfo;
import org.eclipse.leshan.transport.californium.DefaultExceptionTranslator;
import org.eclipse.leshan.transport.californium.ExceptionTranslator;
import org.eclipse.leshan.transport.californium.identity.DefaultCoapIdentityHandler;
import org.eclipse.leshan.transport.californium.identity.IdentityHandler;
import org.eclipse.leshan.transport.californium.server.bootstrap.endpoint.CaliforniumBootstrapServerEndpointFactory;

public class CoapBootstrapServerEndpointFactory implements CaliforniumBootstrapServerEndpointFactory {

    public static Protocol getSupportedProtocol() {
        return Protocol.COAP;
    }

    public static void applyDefaultValue(Configuration configuration) {
        configuration.set(CoapConfig.MID_TRACKER, TrackerMode.NULL);
    }

    public static List<ModuleDefinitionsProvider> getModuleDefinitionsProviders() {
        return Arrays.asList(SystemConfig.DEFINITIONS, CoapConfig.DEFINITIONS, UdpConfig.DEFINITIONS);
    }

    protected final URI endpointUri;
    protected final String loggingTagPrefix;
    protected final Configuration configuration;
    protected final Consumer<CoapEndpoint.Builder> coapEndpointConfigInitializer;

    public CoapBootstrapServerEndpointFactory(URI uri) {
        this(uri, null, null, null);
    }

    public CoapBootstrapServerEndpointFactory(URI uri, String loggingTagPrefix, Configuration configuration,
            Consumer<Builder> coapEndpointConfigInitializer) {
        EndpointUriUtil.validateURI(uri);

        this.endpointUri = uri;
        this.loggingTagPrefix = loggingTagPrefix == null ? "Bootstrap Server" : loggingTagPrefix;
        this.configuration = configuration;
        this.coapEndpointConfigInitializer = coapEndpointConfigInitializer;

    }

    @Override
    public Protocol getProtocol() {
        return getSupportedProtocol();
    }

    @Override
    public URI getUri() {
        return endpointUri;
    }

    @Override
    public String getEndpointDescription() {
        return "CoAP over UDP endpoint based on Californium library";
    }

    protected String getLoggingTag() {
        if (loggingTagPrefix != null) {
            return String.format("[%s-%s]", loggingTagPrefix, getUri().toString());
        } else {
            return String.format("[%s]", getUri().toString());
        }
    }

    @Override
    public CoapEndpoint createCoapEndpoint(Configuration defaultConfiguration, ServerSecurityInfo serverSecurityInfo,
            LeshanBootstrapServer server) {

        // defined Configuration to use
        Configuration configurationToUse;
        if (configuration == null) {
            // if specific configuration for this endpoint is null, used the default one which is the coapServer
            // Configuration shared with all endpoints by default.
            configurationToUse = defaultConfiguration;
        } else {
            configurationToUse = configuration;
        }

        return createEndpointBuilder(EndpointUriUtil.getSocketAddr(endpointUri), configurationToUse, server).build();
    }

    /**
     * This method is intended to be overridden.
     *
     * @param address the IP address and port, if null the connector is bound to an ephemeral port on the wildcard
     *        address.
     * @param coapConfig the CoAP config used to create this endpoint.
     * @return the {@link Builder} used for unsecured communication.
     */
    protected CoapEndpoint.Builder createEndpointBuilder(InetSocketAddress address, Configuration coapConfig,
            LeshanBootstrapServer server) {
        CoapEndpoint.Builder builder = new CoapEndpoint.Builder();
        builder.setConnector(createConnector(address, coapConfig));
        builder.setConfiguration(coapConfig);
        builder.setLoggingTag(getLoggingTag());

        if (coapEndpointConfigInitializer != null)
            coapEndpointConfigInitializer.accept(builder);

        return builder;
    }

    /**
     * By default create an {@link UDPConnector}.
     * <p>
     * This method is intended to be overridden.
     *
     * @param address the IP address and port, if null the connector is bound to an ephemeral port on the wildcard
     *        address
     * @param coapConfig the Configuration
     * @return the {@link Connector} used for unsecured {@link CoapEndpoint}
     */
    protected Connector createConnector(InetSocketAddress address, Configuration coapConfig) {
        return new UDPConnector(address, coapConfig);
    }

    @Override
    public IdentityHandler createIdentityHandler() {
        return new DefaultCoapIdentityHandler();
    }

    @Override
    public ExceptionTranslator createExceptionTranslator() {
        return new DefaultExceptionTranslator();
    }
}