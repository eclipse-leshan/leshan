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

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.security.PublicKey;
import java.security.cert.CertPath;
import java.security.cert.X509Certificate;

import org.eclipse.leshan.util.Validate;
import org.eclipse.leshan.util.X509Util;

/**
 * A request sender identity.
 */
public class Identity {

    private final InetSocketAddress peerAddress;

    // PSK
    private final String pskIdentity;

    // RPK
    private final PublicKey rawPublicKey;

    // X509
    private final String x509CommonName;
    // the certificate chain presented by the sender
    private final CertPath certificates;

    private Identity(InetSocketAddress peerAddress, String pskIdentity, PublicKey rawPublicKey, String x509CommonName,
            CertPath certificates) {
        Validate.notNull(peerAddress);
        this.peerAddress = peerAddress;
        this.pskIdentity = pskIdentity;
        this.rawPublicKey = rawPublicKey;
        this.x509CommonName = x509CommonName;
        this.certificates = certificates;
    }

    protected Identity(Identity identity) {
        this.peerAddress = identity.peerAddress;
        this.pskIdentity = identity.pskIdentity;
        this.rawPublicKey = identity.rawPublicKey;
        this.x509CommonName = identity.x509CommonName;
        this.certificates = identity.certificates;
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

    /**
     * Only valid for an X509 identity
     * 
     * @return the certificate chain presented by the sender
     */
    public CertPath getCertificates() {
        return certificates;
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
        return new Identity(peerAddress, null, null, null, null);
    }

    public static Identity unsecure(InetAddress address, int port) {
        return new Identity(new InetSocketAddress(address, port), null, null, null, null);
    }

    public static Identity psk(InetSocketAddress peerAddress, String identity) {
        return new Identity(peerAddress, identity, null, null, null);
    }

    public static Identity psk(InetAddress address, int port, String identity) {
        return new Identity(new InetSocketAddress(address, port), identity, null, null, null);
    }

    public static Identity rpk(InetSocketAddress peerAddress, PublicKey publicKey) {
        return new Identity(peerAddress, null, publicKey, null, null);
    }

    public static Identity rpk(InetAddress address, int port, PublicKey publicKey) {
        return new Identity(new InetSocketAddress(address, port), null, publicKey, null, null);
    }

    public static Identity x509(InetSocketAddress peerAddress, String commonName) {
        return new Identity(peerAddress, null, null, commonName, null);
    }

    public static Identity x509(InetAddress address, int port, String commonName) {
        return new Identity(new InetSocketAddress(address, port), null, null, commonName, null);
    }

    public static Identity x509(InetSocketAddress peerAddress, CertPath certificates) {
        return new Identity(peerAddress, null, null,
                X509Util.extractCN(
                        ((X509Certificate) certificates.getCertificates().get(0)).getSubjectX500Principal().getName()),
                certificates);
    }

    @Override
    public String toString() {
        if (pskIdentity != null)
            return String.format("Identity %s[psk=%s]", peerAddress, pskIdentity);
        else if (rawPublicKey != null)
            return String.format("Identity %s[rpk=%s]", peerAddress, rawPublicKey);
        else if (x509CommonName != null)
            return String.format("Identity %s[x509=%s]", peerAddress, x509CommonName);
        else
            return String.format("Identity %s[unsecure]", peerAddress);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((certificates == null) ? 0 : certificates.hashCode());
        result = prime * result + ((peerAddress == null) ? 0 : peerAddress.hashCode());
        result = prime * result + ((pskIdentity == null) ? 0 : pskIdentity.hashCode());
        result = prime * result + ((rawPublicKey == null) ? 0 : rawPublicKey.hashCode());
        result = prime * result + ((x509CommonName == null) ? 0 : x509CommonName.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Identity other = (Identity) obj;
        if (certificates == null) {
            if (other.certificates != null)
                return false;
        } else if (!certificates.equals(other.certificates))
            return false;
        if (peerAddress == null) {
            if (other.peerAddress != null)
                return false;
        } else if (!peerAddress.equals(other.peerAddress))
            return false;
        if (pskIdentity == null) {
            if (other.pskIdentity != null)
                return false;
        } else if (!pskIdentity.equals(other.pskIdentity))
            return false;
        if (rawPublicKey == null) {
            if (other.rawPublicKey != null)
                return false;
        } else if (!rawPublicKey.equals(other.rawPublicKey))
            return false;
        if (x509CommonName == null) {
            if (other.x509CommonName != null)
                return false;
        } else if (!x509CommonName.equals(other.x509CommonName))
            return false;
        return true;
    }
}
