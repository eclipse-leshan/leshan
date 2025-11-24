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
import java.util.Objects;

public class PreSharedKeyPrincipal implements Principal {

    private final String identity;

    public PreSharedKeyPrincipal(String identity) {
        this.identity = identity;
    }

    public String getIdentity() {
        return identity;
    }

    @Override
    public String getName() {
        return getIdentity();
    }

    @Override
    public final int hashCode() {
        return Objects.hash(identity);
    }

    @Override
    public final boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (!(obj instanceof PreSharedKeyPrincipal))
            return false;
        PreSharedKeyPrincipal other = (PreSharedKeyPrincipal) obj;
        return Objects.equals(identity, other.identity);
    }
}
