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

import org.eclipse.leshan.core.peer.LwM2mPeer;

/**
 * A Bean which represents a LWM2M Server at client side.
 */
public class LwM2mServer {

    /**
     * Server for system calls.
     */
    public final static LwM2mServer SYSTEM = new LwM2mServer(null, null, Role.SYSTEM, null);

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

    private final LwM2mPeer peer;
    private final Long id;
    private final Role role;
    private final URI uri;

    public LwM2mServer(LwM2mPeer peer, Long id, URI uri) {
        this(peer, id, Role.LWM2M_SERVER, uri);
    }

    public LwM2mServer(LwM2mPeer peer, URI uri) {
        this(peer, null, Role.LWM2M_BOOTSTRAP_SERVER, uri);
    }

    public LwM2mServer(LwM2mPeer peer, Long id, Role role, URI uri) {
        this.peer = peer;
        this.id = id;
        this.role = role;
        this.uri = uri;
    }

    public LwM2mPeer getTransportData() {
        return peer;
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
        return null;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((id == null) ? 0 : id.hashCode());
        result = prime * result + ((peer == null) ? 0 : peer.hashCode());
        result = prime * result + ((role == null) ? 0 : role.hashCode());
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
        LwM2mServer other = (LwM2mServer) obj;
        if (id == null) {
            if (other.id != null)
                return false;
        } else if (!id.equals(other.id))
            return false;
        if (peer == null) {
            if (other.peer != null)
                return false;
        } else if (!peer.equals(other.peer))
            return false;
        if (role != other.role)
            return false;
        return true;
    }
}
