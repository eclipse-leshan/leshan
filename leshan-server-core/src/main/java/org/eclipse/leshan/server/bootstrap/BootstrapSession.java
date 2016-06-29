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

/**
 * Represent a single Bootstraping session.
 * 
 * Should be created by {@link BootstrapSessionManager} implementations in {@link BootstrapSessionManager.begin}.
 */
public class BootstrapSession {

    private final String endpoint;
    private final Identity clientIdentity;
    private final boolean authorized;

    protected BootstrapSession(String endpoint, Identity clientIdentity, boolean authorized) {
        this.endpoint = endpoint;
        this.clientIdentity = clientIdentity;
        this.authorized = authorized;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public Identity getClientIdentity() {
        return clientIdentity;
    }

    public boolean isAuthorized() {
        return authorized;
    }

    /**
     * A Bootstrapping session where the client was not authorized.
     */
    public static BootstrapSession unauthorized() {
        return new BootstrapSession(null, null, false);
    }

    /**
     * A Bootstrapping session where the client was properly authorized.
     */
    public static BootstrapSession authorized(String endpoint, Identity clientIdentity) {
        return new BootstrapSession(endpoint, clientIdentity, true);
    }

    @Override
    public String toString() {
        return String.format("BootstrapSession [endpoint=%s, clientIdentity=%s, authorized=%s]", endpoint,
                clientIdentity, authorized);
    }

}
