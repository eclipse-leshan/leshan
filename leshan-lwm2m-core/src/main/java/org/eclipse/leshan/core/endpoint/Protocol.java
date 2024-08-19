/*******************************************************************************
 * Copyright (c) 2022 Sierra Wireless and others.
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

public class Protocol {

    public static final Protocol COAP = new Protocol("COAP", "coap");
    public static final Protocol COAPS = new Protocol("COAPS", "coaps");
    public static final Protocol COAP_TCP = new Protocol("COAP_TCP", "coap+tcp");
    public static final Protocol COAPS_TCP = new Protocol("COAPS_TCP", "coaps+tcp");

    private static final Protocol[] knownProtocols = new Protocol[] { Protocol.COAP, Protocol.COAPS, Protocol.COAP_TCP,
            Protocol.COAPS_TCP };

    private final String name;
    private final String uriScheme;

    public Protocol(String name, String uriScheme) {
        this.name = name;
        this.uriScheme = uriScheme;
    }

    public String getName() {
        return name;
    }

    public String getUriScheme() {
        return uriScheme;
    }

    @Override
    public String toString() {
        return getName();
    }

    public static Protocol fromUri(String uri) {
        for (Protocol protocol : knownProtocols) {
            if (uri.startsWith(protocol.getUriScheme() + ":")) {
                return protocol;
            }
        }
        return null;
    }

    @Override
    public final boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof Protocol))
            return false;
        Protocol protocol = (Protocol) o;
        return Objects.equals(name, protocol.name);
    }

    @Override
    public final int hashCode() {
        return Objects.hashCode(name);
    }
}
