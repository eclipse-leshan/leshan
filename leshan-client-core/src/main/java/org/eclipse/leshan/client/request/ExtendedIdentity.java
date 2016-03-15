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

/**
 * Extend the identity with client relevant additional information. Introduce roles for the identity and makes those
 * roles accessible within the processing context of the Identity.
 * 
 * @author Achim Kraus (Bosch Software Innovations GmbH)
 *
 */
public class ExtendedIdentity extends Identity {

    enum Role {
        /**
         * Indicate internal call. Enables the "system" to read protected resources (e.g. resources of the security
         * object).
         */
        SYSTEM,
        /**
         * Indicate call from a LWM2M server.
         */
        LWM2M_SERVER,
        /**
         * Indicate call from a LWM2M bootstrap server.
         */
        LWM2M_BOOTSTRAP_SERVER
    }

    /**
     * Identity for system calls.
     */
    public final static ExtendedIdentity SYSTEM = new ExtendedIdentity(Identity.unsecure(InetSocketAddress
            .createUnresolved("system", 1)), Role.SYSTEM);

    /**
     * Role of the associated identity.
     */
    private final Role role;

    /**
     * Create ExtendedIdentity using a Identity and the related role. Intended to be used by calling
     * {@link #createLwm2mBootstrapServerIdentity(Identity)} or {@link #createLwm2mServerIdentity(Identity)}.
     * 
     * @param identity identity to be used
     * @param role related role
     */
    private ExtendedIdentity(final Identity identity, final Role role) {
        super(identity);
        this.role = role;
    }

    /**
     * Get related role.
     * 
     * @return {@link Role#SYSTEM}, {@link Role#LWM2M_SERVER}, or {@link Role#LWM2M_BOOTSTRAP_SERVER}.
     */
    public Role getRole() {
        return role;
    }

    /**
     * Test, if identity has role {@link Role#LWM2M_BOOTSTRAP_SERVER}.
     * 
     * @return true, if identity is from a LWM2M bootstrap server, false, otherwise
     */
    public boolean isLwm2mBootstrapServer() {
        return Role.LWM2M_BOOTSTRAP_SERVER == role;
    }

    /**
     * Test, if identity has role {@link Role#LWM2M_SERVER}.
     * 
     * @return true, if identity is from a LWM2M server, false, otherwise
     */
    public boolean isLwm2mServer() {
        return Role.LWM2M_SERVER == role;
    }

    /**
     * Test, if identity has role {@link Role#SYSTEM}.
     * 
     * @return true, if identity is from system, false, otherwise
     */
    public boolean isSystem() {
        return Role.SYSTEM == role;
    }

    /**
     * Create a extended identity with role {@link Role#LWM2M_BOOTSTRAP_SERVER}.
     * 
     * @param identity identity to be used
     * @return extended identity with {@link Role#LWM2M_BOOTSTRAP_SERVER} associated.
     */
    public static ExtendedIdentity createLwm2mBootstrapServerIdentity(final Identity identity) {
        return new ExtendedIdentity(identity, Role.LWM2M_BOOTSTRAP_SERVER);
    }

    /**
     * Create a extended identity with role {@link Role#LWM2M_SERVER}.
     * 
     * @param identity identity to be used
     * @return extended identity with {@link Role#LWM2M_SERVER} associated.
     */
    public static ExtendedIdentity createLwm2mServerIdentity(final Identity identity) {
        return new ExtendedIdentity(identity, Role.LWM2M_SERVER);
    }

}
