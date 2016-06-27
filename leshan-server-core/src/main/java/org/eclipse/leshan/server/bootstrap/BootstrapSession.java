/*******************************************************************************
 * Copyright (c) 2016 Sierra Wireless and others.
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
package org.eclipse.leshan.server.bootstrap;

import org.eclipse.leshan.core.request.Identity;

public class BootstrapSession {

    private final String endpoint;
    private final Identity clientIdentity;
    private final boolean authenticated;

    protected BootstrapSession(String endpoint, Identity clientIdentity, boolean authenticated) {
        this.endpoint = endpoint;
        this.clientIdentity = clientIdentity;
        this.authenticated = authenticated;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public Identity getClientIdentity() {
        return clientIdentity;
    }

    public boolean isAuthenticated() {
        return authenticated;
    }

    @Override
    public String toString() {
        return "BootstrapSession [endpoint=" + endpoint + ", clientIdentity=" + clientIdentity + ", authenticated="
                + authenticated + "]";
    }

    public static BootstrapSession authenticationFailed() {
        return new BootstrapSession(null, null, false);
    }

    public static BootstrapSession authenticationSuccess(String endpoint, Identity clientIdentity) {
        return new BootstrapSession(endpoint, clientIdentity, true);
    }

}
