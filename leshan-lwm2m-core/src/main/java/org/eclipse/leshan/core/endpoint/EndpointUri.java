/*******************************************************************************
 * Copyright (c) 2024 Sierra Wireless and others.
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
package org.eclipse.leshan.core.endpoint;

import java.util.Objects;

public class EndpointUri {

    private final String scheme;
    private final String host;
    private final Integer port;

    public EndpointUri(String scheme, String host, Integer port) {
        this.scheme = scheme;
        this.host = host;
        this.port = port;
    }

    public String getScheme() {
        return scheme;
    }

    public String getHost() {
        return host;
    }

    public Integer getPort() {
        return port;
    }

    @Override
    public final boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (!(obj instanceof EndpointUri))
            return false;
        EndpointUri other = (EndpointUri) obj;
        return Objects.equals(host, other.host) && Objects.equals(port, other.port)
                && Objects.equals(scheme, other.scheme);
    }

    @Override
    public final int hashCode() {
        return Objects.hash(host, port, scheme);
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append(scheme);
        builder.append("://");
        builder.append(host);
        if (port != null) {
            builder.append(":");
            builder.append(port);
        }
        return builder.toString();
    }
}
