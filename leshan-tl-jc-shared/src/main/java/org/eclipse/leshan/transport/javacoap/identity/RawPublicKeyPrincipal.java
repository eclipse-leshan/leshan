/*******************************************************************************
 * Copyright (c) 2025 Sierra Wireless and others.
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
 *     Sierra Wireless - initial API and implementation
 *******************************************************************************/
package org.eclipse.leshan.transport.javacoap.identity;

import java.security.Principal;
import java.security.PublicKey;
import java.util.Objects;

public class RawPublicKeyPrincipal implements Principal {

    private final PublicKey pubkey;

    public RawPublicKeyPrincipal(PublicKey pubkey) {
        this.pubkey = pubkey;
    }

    public PublicKey getPublicKey() {
        return pubkey;
    }

    @Override
    public String getName() {
        return getPublicKey().toString();
    }

    @Override
    public final int hashCode() {
        return Objects.hash(pubkey);
    }

    @Override
    public final boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (!(obj instanceof RawPublicKeyPrincipal))
            return false;
        RawPublicKeyPrincipal other = (RawPublicKeyPrincipal) obj;
        return Objects.equals(pubkey, other.pubkey);
    }
}
