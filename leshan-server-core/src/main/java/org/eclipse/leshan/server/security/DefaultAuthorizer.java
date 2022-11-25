/*******************************************************************************
 * Copyright (c) 2016 Sierra Wireless and others.
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
package org.eclipse.leshan.server.security;

import org.eclipse.leshan.core.request.Identity;
import org.eclipse.leshan.core.request.UplinkRequest;
import org.eclipse.leshan.server.registration.Registration;

/**
 * A default {@link Authorizer} implementation
 *
 * It checks in {@link SecurityStore} if there is a corresponding {@link SecurityInfo} for this registration endpoint.
 * If there is a {@link SecurityInfo} it check the identity is correct, else it checks if the LWM2M client use an
 * unsecure connection.
 */
public class DefaultAuthorizer implements Authorizer {

    private final SecurityStore securityStore;
    private final SecurityChecker securityChecker;

    public DefaultAuthorizer(SecurityStore store) {
        this(store, new SecurityChecker());
    }

    public DefaultAuthorizer(SecurityStore store, SecurityChecker checker) {
        securityStore = store;
        securityChecker = checker;
    }

    @Override
    public Authorization isAuthorized(UplinkRequest<?> request, Registration registration, Identity senderIdentity) {

        // do we have security information for this client?
        SecurityInfo expectedSecurityInfo = null;
        if (securityStore != null)
            expectedSecurityInfo = securityStore.getByEndpoint(registration.getEndpoint());

        if (securityChecker.checkSecurityInfo(registration.getEndpoint(), senderIdentity, expectedSecurityInfo)) {
            return Authorization.approved();
        } else {
            return Authorization.declined();
        }
    }
}
