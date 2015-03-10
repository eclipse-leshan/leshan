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
package org.eclipse.leshan.server.californium;

import java.net.InetAddress;
import java.net.InetSocketAddress;

import org.eclipse.leshan.server.LwM2mServer;
import org.eclipse.leshan.server.californium.impl.LeshanServer;
import org.eclipse.leshan.server.client.ClientRegistry;
import org.eclipse.leshan.server.impl.ClientRegistryImpl;
import org.eclipse.leshan.server.impl.ObservationRegistryImpl;
import org.eclipse.leshan.server.impl.SecurityRegistryImpl;
import org.eclipse.leshan.server.model.LwM2mModelProvider;
import org.eclipse.leshan.server.model.StandardModelProvider;
import org.eclipse.leshan.server.observation.ObservationRegistry;
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
        return new LeshanServer(localAddress, localAddressSecure, clientRegistry, securityRegistry,
                observationRegistry, modelProvider);
    }
}
