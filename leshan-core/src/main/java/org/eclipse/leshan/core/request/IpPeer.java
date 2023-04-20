/*******************************************************************************
 * Copyright (c) 2022    Sierra Wireless and others.
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
 *     Sierra Wireless, Orange Polska S.A. - initial API and implementation
 *******************************************************************************/
package org.eclipse.leshan.core.request;

import java.net.InetSocketAddress;
import java.util.Objects;

public class IpPeer implements Peer {

    private final InetSocketAddress peerAddress;
    private final LwM2MIdentity identity;

    public IpPeer(InetSocketAddress peerAddress) {
        this.peerAddress = peerAddress;
        this.identity = null;
    }

    public IpPeer(InetSocketAddress peerAddress, PskIdentity pskidentity) {
        this.peerAddress = peerAddress;
        this.identity = pskidentity;
    }

    public IpPeer(InetSocketAddress peerAddress, RpkIdentity identity) {
        this.peerAddress = peerAddress;
        this.identity = identity;
    }

    public IpPeer(InetSocketAddress peerAddress, X509Identity identity) {
        this.peerAddress = peerAddress;
        this.identity = identity;
    }

    public IpPeer(InetSocketAddress peerAddress, OscoreIdentity identity) {
        this.peerAddress = peerAddress;
        this.identity = identity;
    }

    public boolean isPSK() {
        return (identity instanceof PskIdentity);
    }

    public boolean isRPK() {
        return (identity instanceof RpkIdentity);
    }

    public boolean isX509() {
        return (identity instanceof X509Identity);
    }

    public boolean isOSCORE() {
        return (identity instanceof OscoreIdentity);
    }

    public boolean isSecure() {
        return isPSK() || isRPK() || isX509() || isOSCORE();
    }

    @Override
    public LwM2MIdentity getIdentity() {
        return identity;
    }

    public InetSocketAddress getSocketAddress() {
        return peerAddress;
    }

    @Override
    public String toString() {
        if (isPSK()) {
            return "IpPeer{" + "peerAddress=" + peerAddress + ", identity=" + ((PskIdentity) identity).getpskIdentity()
                    + '}';
        } else if (isRPK()) {
            return "IpPeer{" + "peerAddress=" + peerAddress + ", identity=" + ((RpkIdentity) identity).getPublicKey()
                    + '}';
        } else if (isX509()) {
            return "IpPeer{" + "peerAddress=" + peerAddress + ", identity="
                    + ((X509Identity) identity).getX509CommonName() + '}';
        } else
            return "IpPeer{" + "peerAddress=" + peerAddress + ", identity=" + identity + '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        IpPeer ipPeer = (IpPeer) o;
        return peerAddress.equals(ipPeer.peerAddress) && Objects.equals(identity, ipPeer.identity);
    }

    @Override
    public int hashCode() {
        return Objects.hash(peerAddress, identity);
    }
}
