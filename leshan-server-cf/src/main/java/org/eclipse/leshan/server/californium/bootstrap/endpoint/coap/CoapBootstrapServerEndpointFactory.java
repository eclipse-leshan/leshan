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
package org.eclipse.leshan.server.californium.bootstrap.endpoint.coap;

import java.net.InetSocketAddress;
import java.net.URI;

import org.eclipse.californium.core.network.CoapEndpoint;
import org.eclipse.californium.core.network.CoapEndpoint.Builder;
import org.eclipse.californium.elements.Connector;
import org.eclipse.californium.elements.EndpointContextMatcher;
import org.eclipse.californium.elements.UDPConnector;
import org.eclipse.californium.elements.config.Configuration;
import org.eclipse.leshan.core.californium.DefaultExceptionTranslator;
import org.eclipse.leshan.core.californium.ExceptionTranslator;
import org.eclipse.leshan.core.californium.identity.DefaultCoapIdentityHandler;
import org.eclipse.leshan.core.californium.identity.IdentityHandler;
import org.eclipse.leshan.core.endpoint.EndpointUriUtil;
import org.eclipse.leshan.core.endpoint.Protocol;
import org.eclipse.leshan.server.bootstrap.LeshanBootstrapServer;
import org.eclipse.leshan.server.californium.bootstrap.endpoint.CaliforniumBootstrapServerEndpointFactory;
import org.eclipse.leshan.server.security.ServerSecurityInfo;

public class CoapBootstrapServerEndpointFactory implements CaliforniumBootstrapServerEndpointFactory {

    protected EndpointContextMatcher unsecuredContextMatcher = null;
    protected URI endpointUri = null;

    public CoapBootstrapServerEndpointFactory(URI uri) {
        this.endpointUri = uri;
    }

    @Override
    public Protocol getProtocol() {
        return Protocol.COAP;
    }

    @Override
    public URI getUri() {
        return endpointUri;
    }

    @Override
    public CoapEndpoint createCoapEndpoint(Configuration defaultConfiguration, ServerSecurityInfo serverSecurityInfo,
            LeshanBootstrapServer server) {
        return createUnsecuredEndpointBuilder(EndpointUriUtil.getSocketAddr(endpointUri), defaultConfiguration, server)
                .build();
    }

    /**
     * This method is intended to be overridden.
     *
     * @param address the IP address and port, if null the connector is bound to an ephemeral port on the wildcard
     *        address.
     * @param coapConfig the CoAP config used to create this endpoint.
     * @return the {@link Builder} used for unsecured communication.
     */
    protected CoapEndpoint.Builder createUnsecuredEndpointBuilder(InetSocketAddress address, Configuration coapConfig,
            LeshanBootstrapServer server) {
        CoapEndpoint.Builder builder = new CoapEndpoint.Builder();
        builder.setConnector(createUnsecuredConnector(address, coapConfig));
        builder.setConfiguration(coapConfig);
        builder.setLoggingTag("[LWM2M BS Server-coap://]");
        if (unsecuredContextMatcher != null) {
            builder.setEndpointContextMatcher(unsecuredContextMatcher);
        }
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
    protected Connector createUnsecuredConnector(InetSocketAddress address, Configuration coapConfig) {
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
