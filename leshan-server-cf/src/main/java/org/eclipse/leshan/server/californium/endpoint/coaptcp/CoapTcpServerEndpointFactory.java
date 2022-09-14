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
package org.eclipse.leshan.server.californium.endpoint.coaptcp;

import java.net.InetSocketAddress;
import java.net.URI;

import org.eclipse.californium.core.network.CoapEndpoint;
import org.eclipse.californium.core.network.serialization.TcpDataParser;
import org.eclipse.californium.core.network.serialization.TcpDataSerializer;
import org.eclipse.californium.elements.Connector;
import org.eclipse.californium.elements.config.Configuration;
import org.eclipse.californium.elements.tcp.netty.TcpServerConnector;
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

public class CoapTcpServerEndpointFactory implements CaliforniumServerEndpointFactory {

    protected final String loggingTagPrefix;
    protected URI endpointUri = null;

    public CoapTcpServerEndpointFactory(URI uri) {
        this(uri, "LWM2M Server");
    }

    public CoapTcpServerEndpointFactory(URI uri, String loggingTagPrefix) {
        this.endpointUri = uri;
        this.loggingTagPrefix = loggingTagPrefix;
    }

    @Override
    public Protocol getProtocol() {
        return Protocol.COAP_TCP;
    }

    @Override
    public URI getUri() {
        return endpointUri;
    }

    protected String getLoggingTag() {
        if (loggingTagPrefix != null) {
            return String.format("[%s-%s]", loggingTagPrefix, getUri().toString());
        } else {
            return String.format("[%s-%s]", getUri().toString());
        }
    }

    @Override
    public CoapEndpoint createCoapEndpoint(Configuration defaultCaliforniumConfiguration,
            ServerSecurityInfo serverSecurityInfo, LwM2mNotificationReceiver notificationReceiver,
            LeshanServer server) {
        return createEndpointBuilder(EndpointUriUtil.getSocketAddr(endpointUri), defaultCaliforniumConfiguration,
                notificationReceiver, server).build();
    }

    protected CoapEndpoint.Builder createEndpointBuilder(InetSocketAddress address, Configuration coapConfig,
            LwM2mNotificationReceiver notificationReceiver, LeshanServer server) {
        CoapEndpoint.Builder builder = new CoapEndpoint.Builder();
        builder.setConnector(createConnector(address, coapConfig));
        builder.setConfiguration(coapConfig);
        builder.setLoggingTag(getLoggingTag());

        builder.setObservationStore(new LwM2mObservationStore(server.getRegistrationStore(), notificationReceiver,
                new ObservationSerDes(new TcpDataParser(), new TcpDataSerializer())));
        return builder;
    }

    protected Connector createConnector(InetSocketAddress address, Configuration coapConfig) {
        return new TcpServerConnector(address, coapConfig);
    }

    @Override
    public IdentityHandler createIdentityHandler() {
        // TODO TCP : maybe we need a more specific one
        return new DefaultCoapIdentityHandler();
    }

    @Override
    public ExceptionTranslator createExceptionTranslator() {
        // TODO TCP : maybe we need a more specific one
        return new DefaultExceptionTranslator();
    }
}
