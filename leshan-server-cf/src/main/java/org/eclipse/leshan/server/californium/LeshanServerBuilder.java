/*******************************************************************************
 * Copyright (c) 2013-2016 Sierra Wireless and others.
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v1.0 which accompany this distribution.
 * 
 * The Eclipse Public License is available at
 *    http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 *    http://www.eclipse.org/org/documents/edl-v10.html.
 * 
 * Contributors:
 *     Sierra Wireless - initial API and implementation
 *     Bosch Software Innovations - add support for setting Endpoints
 *******************************************************************************/
package org.eclipse.leshan.server.californium;

import java.net.InetSocketAddress;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.californium.core.coap.CoAP;
import org.eclipse.californium.core.network.Endpoint;
import org.eclipse.leshan.core.node.codec.DefaultLwM2mNodeDecoder;
import org.eclipse.leshan.core.node.codec.DefaultLwM2mNodeEncoder;
import org.eclipse.leshan.core.node.codec.LwM2mNodeDecoder;
import org.eclipse.leshan.core.node.codec.LwM2mNodeEncoder;
import org.eclipse.leshan.server.LwM2mServer;
import org.eclipse.leshan.server.californium.impl.CaliforniumObservationRegistryImpl;
import org.eclipse.leshan.server.californium.impl.InMemoryLwM2mObservationStore;
import org.eclipse.leshan.server.californium.impl.LeshanServer;
import org.eclipse.leshan.server.client.ClientRegistry;
import org.eclipse.leshan.server.impl.ClientRegistryImpl;
import org.eclipse.leshan.server.impl.SecurityRegistryImpl;
import org.eclipse.leshan.server.model.LwM2mModelProvider;
import org.eclipse.leshan.server.model.StandardModelProvider;
import org.eclipse.leshan.server.security.SecurityRegistry;
import org.eclipse.leshan.util.Validate;

/**
 * Class helping you to build and configure a Californium based Leshan Lightweight M2M server. Usage: create it, call
 * the different setters for changing the configuration and then call the {@link #build()} method for creating the
 * {@link LwM2mServer} ready to operate.
 */
public class LeshanServerBuilder {

    private SecurityRegistry securityRegistry;
    private CaliforniumObservationRegistry observationRegistry;
    private ClientRegistry clientRegistry;
    private LwM2mModelProvider modelProvider;
    private InetSocketAddress localAddress;
    private InetSocketAddress localSecureAddress;
    private Set<Endpoint> endpoints;
    private Set<String> endpointUris;

    private LwM2mNodeEncoder encoder;

    private LwM2mNodeDecoder decoder;

    public LeshanServerBuilder setLocalAddress(String hostname, int port) {
        if (hostname == null) {
            this.localAddress = new InetSocketAddress(port);
        } else {
            this.localAddress = new InetSocketAddress(hostname, port);
        }
        return this;
    }

    public LeshanServerBuilder setLocalAddress(InetSocketAddress localAddress) {
        this.localAddress = localAddress;
        return this;
    }

    public LeshanServerBuilder setLocalSecureAddress(String hostname, int port) {
        if (hostname == null) {
            this.localSecureAddress = new InetSocketAddress(port);
        } else {
            this.localSecureAddress = new InetSocketAddress(hostname, port);
        }
        return this;
    }

    public LeshanServerBuilder setLocalSecureAddress(InetSocketAddress localSecureAddress) {
        this.localSecureAddress = localSecureAddress;
        return this;
    }

    /**
     * Sets the CoAP {@code Endpoint}s that the LWM2M server should use for communication with peers.
     * <p>
     * If this property is set, the <em>localAddress</em> and <em>localSecureAddress</em> properties are ignored when
     * building the server.
     * 
     * @param endpoints The endpoints to use.
     * @return This builder for fluent access.
     * @throws IllegalArgumentException if the set is empty or {@code null}
     */
    public LeshanServerBuilder setEndpoints(Set<Endpoint> endpoints) {
        Validate.notEmpty(endpoints);
        this.endpoints = new HashSet<>(endpoints);
        return this;
    }

    public LeshanServerBuilder setEndpointUris(Set<String> endpointUris) {
        Validate.notEmpty(endpointUris);
        this.endpointUris = new HashSet<>(endpointUris);
        return this;
    }

    public LeshanServerBuilder setClientRegistry(ClientRegistry clientRegistry) {
        this.clientRegistry = clientRegistry;
        return this;
    }

    public LeshanServerBuilder setObservationRegistry(CaliforniumObservationRegistry observationRegistry) {
        this.observationRegistry = observationRegistry;
        return this;
    }

    public LeshanServerBuilder setSecurityRegistry(SecurityRegistry securityRegistry) {
        this.securityRegistry = securityRegistry;
        return this;
    }

    public LeshanServerBuilder setObjectModelProvider(LwM2mModelProvider objectModelProvider) {
        this.modelProvider = objectModelProvider;
        return this;
    }

    public LeshanServerBuilder setEncoder(LwM2mNodeEncoder encoder) {
        this.encoder = encoder;
        return this;
    }

    public LeshanServerBuilder setDecoder(LwM2mNodeDecoder decoder) {
        this.decoder = decoder;
        return this;
    }

    public LeshanServer build() {
        if (clientRegistry == null)
            clientRegistry = new ClientRegistryImpl();
        if (securityRegistry == null)
            securityRegistry = new SecurityRegistryImpl();
        if (modelProvider == null)
            modelProvider = new StandardModelProvider();
        if (encoder == null)
            encoder = new DefaultLwM2mNodeEncoder();
        if (decoder == null)
            decoder = new DefaultLwM2mNodeDecoder();

        if (observationRegistry == null)
            observationRegistry = new CaliforniumObservationRegistryImpl(new InMemoryLwM2mObservationStore(),
                    clientRegistry, modelProvider, decoder);

        if (endpoints != null) {
            return new LeshanServer(endpoints, clientRegistry, securityRegistry, observationRegistry, modelProvider,
                    encoder, decoder);
        } else if (endpointUris != null) {
            return new LeshanServer(clientRegistry, securityRegistry, observationRegistry, modelProvider, encoder,
                    decoder, endpointUris);
        } else {
            if (localAddress == null)
                localAddress = new InetSocketAddress(CoAP.DEFAULT_COAP_PORT);
            if (localSecureAddress == null)
                localSecureAddress = new InetSocketAddress(CoAP.DEFAULT_COAP_SECURE_PORT);
            return new LeshanServer(localAddress, localSecureAddress, clientRegistry, securityRegistry,
                    observationRegistry, modelProvider, encoder, decoder);
        }
    }
}
