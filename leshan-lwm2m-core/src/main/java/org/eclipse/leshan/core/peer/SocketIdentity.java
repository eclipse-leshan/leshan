/*******************************************************************************
 * Copyright (c) 2023 Sierra Wireless and others.
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
 *     Sierra Wireless, Orange Polska S.A. - initial API and implementation
 *******************************************************************************/
package org.eclipse.leshan.core.peer;

import java.net.InetSocketAddress;
import java.util.Objects;

import org.eclipse.leshan.core.util.Validate;

public class SocketIdentity implements LwM2mIdentity {

    private final InetSocketAddress peerAddress;

    public SocketIdentity(InetSocketAddress peerAddress) {
        Validate.notNull(peerAddress);
        this.peerAddress = peerAddress;
    }

    public InetSocketAddress getSocketAddress() {
        return peerAddress;
    }

    @Override
    public boolean isSecure() {
        return false;
    }

    @Override
    public String toString() {
        return String.format("Identity [unsecure=%s]", peerAddress);
    }

    @Override
    public final boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof SocketIdentity))
            return false;
        SocketIdentity that = (SocketIdentity) o;
        return Objects.equals(peerAddress, that.peerAddress);
    }

    @Override
    public final int hashCode() {
        return Objects.hashCode(peerAddress);
    }
}
