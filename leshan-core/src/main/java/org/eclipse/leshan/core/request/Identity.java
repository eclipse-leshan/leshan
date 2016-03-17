/*******************************************************************************
 * Copyright (c) 2015 Sierra Wireless and others.
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
 *     Achim Kraus (Bosch Software Innovations GmbH) - add protected constructor for sub-classing
 *******************************************************************************/
package org.eclipse.leshan.core.request;

import java.net.InetSocketAddress;
import java.security.PublicKey;

import org.eclipse.leshan.util.Validate;

/**
 * A request sender identity.
 */
public class Identity {

    private final InetSocketAddress peerAddress;
    private final String pskIdentity;
    private final PublicKey rawPublicKey;
    private final String x509CommonName;

    private Identity(InetSocketAddress peerAddress, String pskIdentity, PublicKey rawPublicKey, String x509CommonName) {
        Validate.notNull(peerAddress);
        this.peerAddress = peerAddress;
        this.pskIdentity = pskIdentity;
        this.rawPublicKey = rawPublicKey;
        this.x509CommonName = x509CommonName;
    }

    protected Identity(Identity identity) {
        this.peerAddress = identity.peerAddress;
        this.pskIdentity = identity.pskIdentity;
        this.rawPublicKey = identity.rawPublicKey;
        this.x509CommonName = identity.x509CommonName;
    }

    public InetSocketAddress getPeerAddress() {
        return peerAddress;
    }

    public String getPskIdentity() {
        return pskIdentity;
    }

    public PublicKey getRawPublicKey() {
        return rawPublicKey;
    }

    public String getX509CommonName() {
        return x509CommonName;
    }

    public boolean isPSK() {
        return pskIdentity != null && !pskIdentity.isEmpty();
    }

    public boolean isRPK() {
        return rawPublicKey != null;
    }

    public boolean isX509() {
        return x509CommonName != null && !x509CommonName.isEmpty();
    }

    public boolean isSecure() {
        return isPSK() || isRPK() || isX509();
    }

    public static Identity unsecure(InetSocketAddress peerAddress) {
        return new Identity(peerAddress, null, null, null);
    }

    public static Identity psk(InetSocketAddress peerAddress, String identity) {
        return new Identity(peerAddress, identity, null, null);
    }

    public static Identity rpk(InetSocketAddress peerAddress, PublicKey publicKey) {
        return new Identity(peerAddress, null, publicKey, null);
    }

    public static Identity x509(InetSocketAddress peerAddress, String commonName) {
        return new Identity(peerAddress, null, null, commonName);
    }
}
