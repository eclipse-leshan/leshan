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

public class ServerInfo {

    public long serverId;
    public URI serverUri;
    // TODO use SecureMode from server.core
    public long secureMode;

    public InetSocketAddress getAddress() {
        return new InetSocketAddress(serverUri.getHost(), serverUri.getPort());
    }

    public boolean isSecure() {
        return secureMode != 3;
    }

    @Override
    public String toString() {
        return String.format("Bootstrap Server [uri=%s]", serverUri);
    }
}
