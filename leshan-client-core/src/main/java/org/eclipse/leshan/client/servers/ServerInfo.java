/*******************************************************************************
 * Copyright (c) 2015 Sierra Wireless and others.
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v1.0 which accompany this distribution.
 * 
 * The Eclipse Public License is available at
 *    http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 *    http://www.eclipse.org/org/documents/edl-v10.html.
 * 
 * Contributors:
 *     Sierra Wireless - initial API and implementation
 *******************************************************************************/
package org.eclipse.leshan.client.servers;

import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ServerInfo {

    private static final Logger LOG = LoggerFactory.getLogger(ServerInfo.class);

    public long serverId;
    public URI serverUri;
    // TODO use SecureMode from server.core
    public long secureMode;

    public InetSocketAddress getAddress() {
        return getAddress(serverUri);
    }

    public URI getFullUri() {
        return getFullUri(serverUri);
    }

    public boolean isSecure() {
        return secureMode != 3;
    }

    @Override
    public String toString() {
        return String.format("Bootstrap Server [uri=%s]", serverUri);
    }

    public static URI getFullUri(URI serverUri) {
        // define port
        int port = serverUri.getPort();
        if (port == -1) {
            if ("coap".equals(serverUri.getScheme())) {
                port = 5683;
            } else if ("coaps".equals(serverUri.getScheme())) {
                port = 5684;
            }
        }
        // define scheme
        String scheme = serverUri.getScheme();
        if (scheme == null) {
            if (port == 5683) {
                scheme = "coap";
            } else if (port == 5684) {
                scheme = "coaps";
            }
        }
        // create the full URI
        try {
            return new URI(scheme, serverUri.getUserInfo(), serverUri.getHost(), port, serverUri.getPath(),
                    serverUri.getQuery(), serverUri.getFragment());
        } catch (URISyntaxException e) {
            LOG.warn("Unable to extract full URI", e);
            return serverUri;
        }
    }

    public static InetSocketAddress getAddress(URI serverUri) {
        URI fullUri = getFullUri(serverUri);
        return new InetSocketAddress(fullUri.getHost(), fullUri.getPort());
    }
}
