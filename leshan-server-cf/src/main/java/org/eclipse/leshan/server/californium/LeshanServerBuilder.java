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
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;

import org.eclipse.leshan.core.node.codec.DefaultLwM2mNodeDecoder;
import org.eclipse.leshan.core.node.codec.DefaultLwM2mNodeEncoder;
import org.eclipse.leshan.core.node.codec.LwM2mNodeDecoder;
import org.eclipse.leshan.core.node.codec.LwM2mNodeEncoder;
import org.eclipse.leshan.server.LwM2mServer;
import org.eclipse.leshan.server.californium.impl.InMemoryRegistrationStore;
import org.eclipse.leshan.server.californium.impl.LeshanServer;
import org.eclipse.leshan.server.impl.FileSecurityStore;
import org.eclipse.leshan.server.model.LwM2mModelProvider;
import org.eclipse.leshan.server.model.StandardModelProvider;
import org.eclipse.leshan.server.security.SecurityStore;

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

    private CaliforniumRegistrationStore registrationStore;
    private SecurityStore securityStore;
    private LwM2mModelProvider modelProvider;

    private InetSocketAddress localAddress;
    private InetSocketAddress localSecureAddress;

    private LwM2mNodeEncoder encoder;
    private LwM2mNodeDecoder decoder;

    private PublicKey publicKey;
    private PrivateKey privateKey;
    private X509Certificate[] certificateChain;
    private Certificate[] trustedCertificates;

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

    public LeshanServerBuilder setRegistrationStore(CaliforniumRegistrationStore registrationStore) {
        this.registrationStore = registrationStore;
        return this;
    }

    public LeshanServerBuilder setSecurityStore(SecurityStore securityStore) {
        this.securityStore = securityStore;
        return this;
    }

    public LeshanServerBuilder setObjectModelProvider(LwM2mModelProvider objectModelProvider) {
        this.modelProvider = objectModelProvider;
        return this;
    }

    public LeshanServerBuilder setPublicKey(PublicKey publicKey) {
        this.publicKey = publicKey;
        return this;
    }

    public LeshanServerBuilder setPrivateKey(PrivateKey privateKey) {
        this.privateKey = privateKey;
        return this;
    }

    public LeshanServerBuilder setCertificateChain(X509Certificate[] certificateChain) {
        this.certificateChain = certificateChain;
        return this;
    }

    public LeshanServerBuilder setTrustedCertificates(Certificate[] trustedCertificates) {
        this.trustedCertificates = trustedCertificates;
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
        if (localAddress == null)
            localAddress = new InetSocketAddress((InetAddress) null, PORT);
        if (localSecureAddress == null)
            localSecureAddress = new InetSocketAddress((InetAddress) null, PORT_DTLS);
        if (registrationStore == null)
            registrationStore = new InMemoryRegistrationStore();
        if (securityStore == null)
            securityStore = new FileSecurityStore();
        if (modelProvider == null)
            modelProvider = new StandardModelProvider();
        if (encoder == null)
            encoder = new DefaultLwM2mNodeEncoder();
        if (decoder == null)
            decoder = new DefaultLwM2mNodeDecoder();

        return new LeshanServer(localAddress, localSecureAddress, registrationStore, securityStore, modelProvider,
                encoder, decoder, publicKey, privateKey, certificateChain, trustedCertificates);
    }
}
