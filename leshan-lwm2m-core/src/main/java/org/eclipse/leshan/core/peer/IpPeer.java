/*******************************************************************************
 * Copyright (c) 2023    Sierra Wireless and others.
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
package org.eclipse.leshan.core.peer;

import java.net.InetSocketAddress;
import java.util.Objects;

import org.eclipse.leshan.core.util.Validate;

public class IpPeer implements LwM2mPeer {

    private final InetSocketAddress peerAddress;
    private final LwM2mIdentity identity;
    private final String virtualHost;

    public IpPeer(InetSocketAddress peerAddress) {
        this(peerAddress, new SocketIdentity(peerAddress));
    }

    public IpPeer(InetSocketAddress peerAddress, LwM2mIdentity identity) {
        Validate.notNull(peerAddress);
        Validate.notNull(identity);
        this.peerAddress = peerAddress;
        this.identity = identity;
        this.virtualHost = null;
    }

    public IpPeer(InetSocketAddress peerAddress, String virtualHost, LwM2mIdentity identity) {
        Validate.notNull(peerAddress);
        Validate.notNull(identity);
        this.peerAddress = peerAddress;
        this.identity = identity;
        this.virtualHost = virtualHost;
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
    public LwM2mIdentity getIdentity() {
        return identity;
    }

    public InetSocketAddress getSocketAddress() {
        return peerAddress;
    }

    public String getVirtualHost() {
        return virtualHost;
    }

    @Override
    public String toString() {
        return String.format("IpPeer [peerAddress=%s, identity=%s]", peerAddress, identity);
    }

    @Override
    public final boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (!(obj instanceof IpPeer))
            return false;
        IpPeer other = (IpPeer) obj;
        return Objects.equals(identity, other.identity) && Objects.equals(peerAddress, other.peerAddress)
                && Objects.equals(virtualHost, other.virtualHost);
    }

    @Override
    public final int hashCode() {
        return Objects.hash(identity, peerAddress, virtualHost);
    }
}
