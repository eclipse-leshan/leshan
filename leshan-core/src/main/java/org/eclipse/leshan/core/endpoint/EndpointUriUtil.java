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

import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;

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
            return new URI(scheme, null, addr.getHostString(), addr.getPort(), null, null, null);
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

    public static InetSocketAddress getSocketAddr(URI uri) {
        return new InetSocketAddress(uri.getHost(), uri.getPort());
    }
}
