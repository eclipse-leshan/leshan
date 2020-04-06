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

import org.eclipse.leshan.core.request.Identity;

/**
 * A Bean which identify a LWM2M Server.
 */
public class Server {

    private final Identity identity;
    private final Long id;

    public Server(Identity identity, Long id) {
        this.identity = identity;
        this.id = id;
    }

    public Identity getIdentity() {
        return identity;
    }

    public Long getId() {
        return id;
    }

    public String getUri() {
        StringBuilder uri = new StringBuilder();
        if (identity.isSecure())
            uri.append("coaps://");
        else
            uri.append("coap://");
        uri.append(identity.getPeerAddress().getHostString());
        uri.append(":");
        uri.append(identity.getPeerAddress().getPort());
        return uri.toString();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((id == null) ? 0 : id.hashCode());
        result = prime * result + ((identity == null) ? 0 : identity.hashCode());
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
        Server other = (Server) obj;
        if (id == null) {
            if (other.id != null)
                return false;
        } else if (!id.equals(other.id))
            return false;
        if (identity == null) {
            if (other.identity != null)
                return false;
        } else if (!identity.equals(other.identity))
            return false;
        return true;
    }
}
