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

import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;

import org.eclipse.leshan.core.util.Validate;

public class EndpointUriUtil {

    public static URI createUri(String scheme, String host, int port) {
        try {
            return new URI(scheme, null, host, port, null, null, null);
        } catch (URISyntaxException e) {
            throw new IllegalStateException(e);
        }
    }

    public static URI createUri(String scheme, InetSocketAddress addr) {
        try {
            return new URI(scheme, null, toUriHostName(addr), addr.getPort(), null, null, null);
        } catch (URISyntaxException e) {
            throw new IllegalStateException(e);
        }
    }

    public static URI createUri(String uri) {
        try {
            return new URI(uri);
        } catch (URISyntaxException e) {
            throw new IllegalStateException(e);
        }
    }

    public static URI replaceAddress(URI originalUri, InetSocketAddress newAddress) {
        try {
            return new URI(originalUri.getScheme(), null, toUriHostName(newAddress), newAddress.getPort(), null, null,
                    null);
        } catch (URISyntaxException e) {
            throw new IllegalStateException(e);
        }
    }

    public static InetSocketAddress getSocketAddr(URI uri) {
        return new InetSocketAddress(uri.getHost(), uri.getPort());
    }

    public static void validateURI(URI uri) throws IllegalArgumentException {
        Validate.notNull(uri);

        if (uri.getScheme() == null) {
            throw new IllegalArgumentException(String.format("Invalid URI[%s]: Scheme MUST NOT be null", uri));
        }

        if (uri.getHost() == null) {
            throw new IllegalArgumentException(String.format("Invalid URI[%s]: Host MUST NOT be null", uri));
        }

        if (uri.getPort() == -1) {
            throw new IllegalArgumentException(String.format("Invalid URI[%s]: Post MUST NOT be undefined", uri));
        }
    }

    /**
     * This convert socket address in URI hostname.
     * <p>
     * Following https://www.rfc-editor.org/rfc/rfc6874#section-2, zone id (also called scope id) in URI should be
     * prefixed by <code>%25</code>
     */
    private static String toUriHostName(InetSocketAddress socketAddr) {
        if (socketAddr == null) {
            Validate.notNull(socketAddr);
        }
        InetAddress addr = socketAddr.getAddress();
        String host = addr.getHostAddress();
        if (addr instanceof Inet6Address) {
            Inet6Address address6 = (Inet6Address) addr;
            if (address6.getScopedInterface() != null || address6.getScopeId() > 0) {
                int pos = host.indexOf('%');
                if (pos > 0 && pos + 1 < host.length()) {
                    String separator = "%25";
                    String scope = host.substring(pos + 1);
                    String hostAddress = host.substring(0, pos);
                    host = hostAddress + separator + scope;
                }
            }
        }
        return host;
    }
}
