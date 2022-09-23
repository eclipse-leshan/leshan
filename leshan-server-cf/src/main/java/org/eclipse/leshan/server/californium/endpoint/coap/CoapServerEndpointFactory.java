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

import org.eclipse.californium.core.network.CoapEndpoint;
import org.eclipse.californium.core.network.CoapEndpoint.Builder;
import org.eclipse.californium.core.network.serialization.UdpDataParser;
import org.eclipse.californium.core.network.serialization.UdpDataSerializer;
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
import org.eclipse.leshan.server.LeshanServer;
import org.eclipse.leshan.server.californium.endpoint.CaliforniumServerEndpointFactory;
import org.eclipse.leshan.server.californium.observation.LwM2mObservationStore;
import org.eclipse.leshan.server.californium.observation.ObservationSerDes;
import org.eclipse.leshan.server.observation.LwM2mNotificationReceiver;
import org.eclipse.leshan.server.security.ServerSecurityInfo;

public class CoapServerEndpointFactory implements CaliforniumServerEndpointFactory {

    private final String loggingTag = null;
    protected EndpointContextMatcher unsecuredContextMatcher = null;
    protected URI endpointUri = null;

    public CoapServerEndpointFactory(URI uri) {
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
            LwM2mNotificationReceiver notificationReceiver, LeshanServer server) {
        return createUnsecuredEndpointBuilder(EndpointUriUtil.getSocketAddr(endpointUri), defaultConfiguration,
                notificationReceiver, server).build();
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
            LwM2mNotificationReceiver notificationReceiver, LeshanServer server) {
        CoapEndpoint.Builder builder = new CoapEndpoint.Builder();
        builder.setConnector(createUnsecuredConnector(address, coapConfig));
        builder.setConfiguration(coapConfig);
        if (loggingTag != null) {
            builder.setLoggingTag("[" + loggingTag + "-coap://]");
        } else {
            builder.setLoggingTag("[coap://]");
        }
        if (unsecuredContextMatcher != null) {
            builder.setEndpointContextMatcher(unsecuredContextMatcher);
        }
        builder.setObservationStore(new LwM2mObservationStore(server.getRegistrationStore(), notificationReceiver,
                new ObservationSerDes(new UdpDataParser(), new UdpDataSerializer())));
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
