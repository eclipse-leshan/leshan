/*******************************************************************************
 * Copyright (c) 2013-2015 Sierra Wireless and others.
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
 *******************************************************************************/
package org.eclipse.leshan.server.californium.impl;

import java.net.InetSocketAddress;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.californium.core.CoapServer;
import org.eclipse.californium.core.network.CoAPEndpoint;
import org.eclipse.californium.core.network.Endpoint;
import org.eclipse.californium.scandium.DTLSConnector;
import org.eclipse.leshan.core.objectspec.Resources;
import org.eclipse.leshan.core.request.DownlinkRequest;
import org.eclipse.leshan.core.response.ExceptionConsumer;
import org.eclipse.leshan.core.response.LwM2mResponse;
import org.eclipse.leshan.core.response.ResponseConsumer;
import org.eclipse.leshan.server.Destroyable;
import org.eclipse.leshan.server.LwM2mServer;
import org.eclipse.leshan.server.Startable;
import org.eclipse.leshan.server.Stopable;
import org.eclipse.leshan.server.client.Client;
import org.eclipse.leshan.server.client.ClientRegistry;
import org.eclipse.leshan.server.client.ClientRegistryListener;
import org.eclipse.leshan.server.observation.ObservationRegistry;
import org.eclipse.leshan.server.registration.RegistrationHandler;
import org.eclipse.leshan.server.security.SecurityRegistry;
import org.eclipse.leshan.util.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A Lightweight M2M server.
 * <p>
 * This CoAP server defines a /rd resources as described in the CoRE RD specification. A {@link ClientRegistry} must be
 * provided to host the description of all the registered LW-M2M clients.
 * </p>
 * <p>
 * A {@link RequestHandler} is provided to perform server-initiated requests to LW-M2M clients.
 * </p>
 */
public class LeshanServer implements LwM2mServer {

    private final CoapServer coapServer;

    private static final Logger LOG = LoggerFactory.getLogger(LeshanServer.class);

    private final CaliforniumLwM2mRequestSender requestSender;

    private final ClientRegistry clientRegistry;

    private final ObservationRegistry observationRegistry;

    private final SecurityRegistry securityRegistry;

    /**
     * Initialize a server which will bind to default UDP port for CoAP (5684).
     */
    public LeshanServer() {
        this(null, null, null);
    }

    /**
     * Initialize a server which will bind to the specified address and port.
     *
     * @param localAddress the address to bind the CoAP server.
     * @param localAddressSecure the address to bind the CoAP server for DTLS connection.
     */
    public LeshanServer(final InetSocketAddress localAddress, final InetSocketAddress localAddressSecure) {
        this(localAddress, localAddressSecure, null, null, null);
    }

    /**
     * Initialize a server which will bind to default UDP port for CoAP (5684).
     */
    public LeshanServer(final ClientRegistry clientRegistry, final SecurityRegistry securityRegistry,
            final ObservationRegistry observationRegistry) {
        this(null, null, clientRegistry, securityRegistry, observationRegistry);
    }

    /**
     * Initialize a server which will bind to the specified address and port.
     *
     * @param localAddress the address to bind the CoAP server.
     * @param localAddressSecure the address to bind the CoAP server for DTLS connection.
     * @param privateKey for RPK authentication mode
     * @param publicKey for RPK authentication mode
     */
    public LeshanServer(InetSocketAddress localAddress, InetSocketAddress localAddressSecure,
            final ClientRegistry clientRegistry, final SecurityRegistry securityRegistry,
            final ObservationRegistry observationRegistry) {
        Validate.notNull(localAddress, "IP address cannot be null");
        Validate.notNull(localAddressSecure, "Secure IP address cannot be null");
        Validate.notNull(clientRegistry, "clientRegistry cannot be null");
        Validate.notNull(securityRegistry, "securityRegistry cannot be null");
        Validate.notNull(observationRegistry, "observationRegistry cannot be null");

        // Init registries
        this.clientRegistry = clientRegistry;
        this.securityRegistry = securityRegistry;
        this.observationRegistry = observationRegistry;

        // Cancel observations on client unregistering
        this.clientRegistry.addListener(new ClientRegistryListener() {

            @Override
            public void updated(final Client clientUpdated) {
            }

            @Override
            public void unregistered(final Client client) {
                LeshanServer.this.observationRegistry.cancelObservations(client);
            }

            @Override
            public void registered(final Client client) {
            }
        });

        // init CoAP server
        coapServer = new CoapServer();
        final Endpoint endpoint = new CoAPEndpoint(localAddress);
        coapServer.addEndpoint(endpoint);

        // init init DTLS server
        DTLSConnector connector = new DTLSConnector(localAddressSecure);
        connector.getConfig().setPskStore(new LwM2mPskStore(this.securityRegistry, this.clientRegistry));
        PrivateKey privateKey = this.securityRegistry.getServerPrivateKey();
        PublicKey publicKey = this.securityRegistry.getServerPublicKey();
        if (privateKey != null && publicKey != null)
            connector.getConfig().setPrivateKey(privateKey, publicKey);

        final Endpoint secureEndpoint = new SecureEndpoint(connector);
        coapServer.addEndpoint(secureEndpoint);

        // define /rd resource
        final RegisterResource rdResource = new RegisterResource(new RegistrationHandler(this.clientRegistry,
                this.securityRegistry));
        coapServer.add(rdResource);

        // create sender
        final Set<Endpoint> endpoints = new HashSet<>();
        endpoints.add(endpoint);
        endpoints.add(secureEndpoint);
        requestSender = new CaliforniumLwM2mRequestSender(endpoints, this.clientRegistry, this.observationRegistry);
    }

    @Override
    public void start() {
        // load resource definitions
        Resources.load();

        // Start registries
        if (clientRegistry instanceof Startable) {
            ((Startable) clientRegistry).start();
        }
        if (securityRegistry instanceof Startable) {
            ((Startable) securityRegistry).start();
        }
        if (observationRegistry instanceof Startable) {
            ((Startable) observationRegistry).start();
        }

        // Start server
        coapServer.start();

        LOG.info("LW-M2M server started");
    }

    @Override
    public void stop() {
        // Stop server
        coapServer.stop();

        // Start registries
        if (clientRegistry instanceof Stopable) {
            ((Stopable) clientRegistry).stop();
        }
        if (securityRegistry instanceof Stopable) {
            ((Stopable) securityRegistry).stop();
        }
        if (observationRegistry instanceof Stopable) {
            ((Stopable) observationRegistry).stop();
        }

        LOG.info("LW-M2M server stoped");
    }

    public void destroy() {
        // Destroy server
        coapServer.destroy();

        // Destroy registries
        if (clientRegistry instanceof Destroyable) {
            ((Destroyable) clientRegistry).destroy();
        }
        if (securityRegistry instanceof Destroyable) {
            ((Destroyable) securityRegistry).destroy();
        }
        if (observationRegistry instanceof Destroyable) {
            ((Destroyable) observationRegistry).destroy();
        }

        LOG.info("LW-M2M server destroyed");
    }

    @Override
    public ClientRegistry getClientRegistry() {
        return this.clientRegistry;
    }

    @Override
    public ObservationRegistry getObservationRegistry() {
        return this.observationRegistry;
    }

    @Override
    public SecurityRegistry getSecurityRegistry() {
        return this.securityRegistry;
    }

    @Override
    public <T extends LwM2mResponse> T send(final Client destination, final DownlinkRequest<T> request) {
        return requestSender.send(destination, request);
    }

    @Override
    public <T extends LwM2mResponse> void send(final Client destination, final DownlinkRequest<T> request,
            final ResponseConsumer<T> responseCallback, final ExceptionConsumer errorCallback) {
        requestSender.send(destination, request, responseCallback, errorCallback);
    }
}
