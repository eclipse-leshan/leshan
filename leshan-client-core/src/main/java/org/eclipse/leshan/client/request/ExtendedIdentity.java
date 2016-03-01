/*******************************************************************************
 * Copyright (c) 2016 Bosch Software Innovations GmbH and others.
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
 *    Achim Kraus (Bosch Software Innovations GmbH) - Initial creation
 ******************************************************************************/

package org.eclipse.leshan.client.request;

import java.net.InetSocketAddress;

import org.eclipse.leshan.core.request.Identity;

public class ExtendedIdentity extends Identity {

    enum Role {
        SYSTEM, DATA_MANAGEMENT, BOOTSTRAP
    }

    public final static ExtendedIdentity SYSTEM = new ExtendedIdentity(Identity.unsecure(InetSocketAddress
            .createUnresolved("system", 1)), Role.SYSTEM);
    private final Role role;

    private ExtendedIdentity(final Identity identity, final Role role) {
        super(identity);
        this.role = role;
    }

    public Role getRole() {
        return role;
    }

    public boolean isBootstrap() {
        return Role.BOOTSTRAP == role;
    }

    public boolean isDataManagement() {
        return Role.DATA_MANAGEMENT == role;
    }

    public boolean isSystem() {
        return Role.SYSTEM == role;
    }

    public static ExtendedIdentity bootstrap(final Identity identity) {
        return new ExtendedIdentity(identity, Role.BOOTSTRAP);
    }

    public static ExtendedIdentity dataManagement(final Identity identity) {
        return new ExtendedIdentity(identity, Role.DATA_MANAGEMENT);
    }

}
