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
 *     Alexander Ellwein (Bosch Software Innovations GmbH) 
 *                     - allow to provide own coap server and request sender
 *******************************************************************************/
package org.eclipse.leshan.server.californium;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.HashSet;

import org.eclipse.californium.core.CoapServer;
import org.eclipse.californium.core.network.CoAPEndpoint;
import org.eclipse.californium.core.network.Endpoint;
import org.eclipse.californium.scandium.DTLSConnector;
import org.eclipse.californium.scandium.dtls.cipher.CipherSuite;
import org.eclipse.leshan.server.LwM2mServer;
import org.eclipse.leshan.server.californium.impl.CaliforniumLwM2mRequestSender;
import org.eclipse.leshan.server.californium.impl.LeshanServer;
import org.eclipse.leshan.server.californium.impl.LwM2mPskStore;
import org.eclipse.leshan.server.californium.impl.RegisterResource;
import org.eclipse.leshan.server.californium.impl.SecureEndpoint;
import org.eclipse.leshan.server.client.ClientRegistry;
import org.eclipse.leshan.server.impl.ClientRegistryImpl;
import org.eclipse.leshan.server.impl.ObservationRegistryImpl;
import org.eclipse.leshan.server.impl.SecurityRegistryImpl;
import org.eclipse.leshan.server.model.LwM2mModelProvider;
import org.eclipse.leshan.server.model.StandardModelProvider;
import org.eclipse.leshan.server.observation.ObservationRegistry;
import org.eclipse.leshan.server.registration.RegistrationHandler;
import org.eclipse.leshan.server.request.LwM2mRequestSender;
import org.eclipse.leshan.server.security.SecurityRegistry;

/**
 * Class helping you to build and configure a Californium based Leshan Lightweight M2M server. Usage: create it, call
 * the different setters for changing the configuration and then call the {@link #build()} method for creating the
 * {@link LwM2mServer} ready to operate.
 */
public class LeshanServerBuilder {

    /** IANA assigned UDP port for CoAP (so for LWM2M) */
    public static final int PORT = 5683;

    /** IANA assigned UDP port for CoAP with DTLS (so for LWM2M) */
    public static final int PORT_DTLS = 5684;

    private SecurityRegistry securityRegistry;
    private ObservationRegistry observationRegistry;
    private ClientRegistry clientRegistry;
    private LwM2mModelProvider modelProvider;
    private InetSocketAddress localAddress;
    private InetSocketAddress localAddressSecure;
    private CoapServer coapServer;
    private LwM2mRequestSender requestSender;

    public LeshanServerBuilder setLocalAddress(String hostname, int port) {
        this.localAddress = new InetSocketAddress(hostname, port);
        return this;
    }

    public LeshanServerBuilder setLocalAddress(InetSocketAddress localAddress) {
        this.localAddress = localAddress;
        return this;
    }

    public LeshanServerBuilder setLocalAddressSecure(String hostname, int port) {
        this.localAddressSecure = new InetSocketAddress(hostname, port);
        return this;
    }

    public LeshanServerBuilder setLocalAddressSecure(InetSocketAddress localAddressSecure) {
        this.localAddressSecure = localAddressSecure;
        return this;
    }

    public LeshanServerBuilder setClientRegistry(ClientRegistry clientRegistry) {
        this.clientRegistry = clientRegistry;
        return this;
    }

    public LeshanServerBuilder setObservationRegistry(ObservationRegistry observationRegistry) {
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

    public LeshanServerBuilder setCoapServer(CoapServer coapServer) {
        this.coapServer = coapServer;
        return this;
    }

    public LeshanServerBuilder setRequestSender(LwM2mRequestSender requestSender) {
        this.requestSender = requestSender;
        return this;
    }

    CoapServer createCoapServer() {
        // default endpoint
        CoapServer coapServer = new CoapServer();
        final Endpoint endpoint = new CoAPEndpoint(localAddress);
        coapServer.addEndpoint(endpoint);

        // secure endpoint
        DTLSConnector connector = new DTLSConnector(localAddressSecure);
        connector.getConfig().setPskStore(new LwM2mPskStore(securityRegistry, clientRegistry));
        PrivateKey privateKey = securityRegistry.getServerPrivateKey();
        PublicKey publicKey = securityRegistry.getServerPublicKey();

        if (privateKey != null && publicKey != null) {
            connector.getConfig().setPrivateKey(privateKey, publicKey);
            // TODO this should be automatically done by scandium
            connector.getConfig().setPreferredCipherSuite(CipherSuite.TLS_ECDHE_ECDSA_WITH_AES_128_CCM_8);
        } else {
            // TODO this should be automatically done by scandium
            connector.getConfig().setPreferredCipherSuite(CipherSuite.TLS_PSK_WITH_AES_128_CCM_8);
        }

        final Endpoint secureEndpoint = new SecureEndpoint(connector);
        coapServer.addEndpoint(secureEndpoint);

        // define /rd resource
        final RegisterResource rdResource = new RegisterResource(new RegistrationHandler(this.clientRegistry,
                this.securityRegistry));
        coapServer.add(rdResource);

        return coapServer;
    }

    public LeshanServer build() {
        if (localAddress == null)
            localAddress = new InetSocketAddress((InetAddress) null, PORT);
        if (localAddressSecure == null)
            localAddressSecure = new InetSocketAddress((InetAddress) null, PORT_DTLS);
        if (clientRegistry == null)
            clientRegistry = new ClientRegistryImpl();
        if (securityRegistry == null)
            securityRegistry = new SecurityRegistryImpl();
        if (observationRegistry == null)
            observationRegistry = new ObservationRegistryImpl();
        if (modelProvider == null) {
            modelProvider = new StandardModelProvider();
        }
        if (coapServer == null) {
            coapServer = createCoapServer();
        }
        if (requestSender == null) {
            requestSender = new CaliforniumLwM2mRequestSender(new HashSet<>(coapServer.getEndpoints()),
                    observationRegistry, modelProvider);
        }
        return new LeshanServer(localAddress, localAddressSecure, clientRegistry, securityRegistry,
                observationRegistry, modelProvider, coapServer, requestSender);
    }
}
