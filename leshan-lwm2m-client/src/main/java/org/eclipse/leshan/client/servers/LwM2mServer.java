/*******************************************************************************
 * Copyright (c) 2017 Sierra Wireless and others.
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
package org.eclipse.leshan.client.servers;

import java.net.URI;
import java.util.Objects;

import org.eclipse.leshan.core.peer.LwM2mPeer;

/**
 * A Bean which represents a LWM2M Server at client side.
 */
public class LwM2mServer {

    /**
     * Server for system calls.
     */
    public static final LwM2mServer SYSTEM = new LwM2mServer(null, null, Role.SYSTEM, null);

    @Override
    public final boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof LwM2mServer))
            return false;
        LwM2mServer that = (LwM2mServer) o;
        return Objects.equals(transportData, that.transportData) && Objects.equals(id, that.id) && role == that.role
                && Objects.equals(uri, that.uri);
    }

    @Override
    public final int hashCode() {
        return Objects.hash(transportData, id, role, uri);
    }

    public enum Role {
        /**
         * Indicate internal call. Enables the "system" to read protected resources (e.g. resources of the security
         * object).
         */
        SYSTEM,
        /**
         * Indicate call from a LWM2M server.
         */
        LWM2M_SERVER,
        /**
         * Indicate call from a LWM2M bootstrap server.
         */
        LWM2M_BOOTSTRAP_SERVER
    }

    private final LwM2mPeer transportData;
    private final Long id;
    private final Role role;
    private final URI uri;

    public LwM2mServer(LwM2mPeer transportData, Long id, URI uri) {
        this(transportData, id, Role.LWM2M_SERVER, uri);
    }

    public LwM2mServer(LwM2mPeer transportData, URI uri) {
        this(transportData, null, Role.LWM2M_BOOTSTRAP_SERVER, uri);
    }

    public LwM2mServer(LwM2mPeer transportData, Long id, Role role, URI uri) {
        this.transportData = transportData;
        this.id = id;
        this.role = role;
        this.uri = uri;
    }

    public LwM2mPeer getTransportData() {
        return transportData;
    }

    public Long getId() {
        return id;
    }

    /**
     * Get related role.
     *
     * @return {@link Role#SYSTEM}, {@link Role#LWM2M_SERVER}, or {@link Role#LWM2M_BOOTSTRAP_SERVER}.
     */
    public Role getRole() {
        return role;
    }

    /**
     * Test, if server has role {@link Role#LWM2M_BOOTSTRAP_SERVER}.
     *
     * @return true, if server is from a LWM2M bootstrap server, false, otherwise
     */
    public boolean isLwm2mBootstrapServer() {
        return Role.LWM2M_BOOTSTRAP_SERVER == role;
    }

    /**
     * Test, if server has role {@link Role#LWM2M_SERVER}.
     *
     * @return true, if server is from a LWM2M server, false, otherwise
     */
    public boolean isLwm2mServer() {
        return Role.LWM2M_SERVER == role;
    }

    /**
     * Test, if server has role {@link Role#SYSTEM}.
     *
     * @return true, if this is a system call, false, otherwise
     */
    public boolean isSystem() {
        return Role.SYSTEM == role;
    }

    public String getUri() {
        return uri.toString();
    }

    @Override
    public String toString() {
        if (isSystem()) {
            return "System";
        } else if (isLwm2mBootstrapServer()) {
            return String.format("%s[%s]", getUri(), getRole());
        } else if (isLwm2mServer()) {
            return String.format("%s[%s %d]", getUri(), getRole(), getId());
        }
        return "unknown";
    }

}
