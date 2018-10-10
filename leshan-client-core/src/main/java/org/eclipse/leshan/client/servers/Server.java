/*******************************************************************************
 * Copyright (c) 2017 Sierra Wireless and others.
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

import org.eclipse.leshan.core.request.Identity;

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
        uri.append(identity.getPeerAddress().getAddress().getHostAddress());
        uri.append(":");
        uri.append(identity.getPeerAddress().getPort());
        return uri.toString();
    }
}
