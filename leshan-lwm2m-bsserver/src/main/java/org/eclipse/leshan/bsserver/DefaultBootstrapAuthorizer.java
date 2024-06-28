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
 *     Orange - initial API and implementation
 *******************************************************************************/
package org.eclipse.leshan.bsserver;

import java.util.Iterator;

import org.eclipse.leshan.bsserver.security.BootstrapAuthorizer;
import org.eclipse.leshan.bsserver.security.BootstrapSecurityStore;
import org.eclipse.leshan.core.peer.LwM2mPeer;
import org.eclipse.leshan.core.request.BootstrapRequest;
import org.eclipse.leshan.servers.security.Authorization;
import org.eclipse.leshan.servers.security.SecurityChecker;
import org.eclipse.leshan.servers.security.SecurityInfo;

public class DefaultBootstrapAuthorizer implements BootstrapAuthorizer {

    private final BootstrapSecurityStore bsSecurityStore;
    private final SecurityChecker securityChecker;

    public DefaultBootstrapAuthorizer(BootstrapSecurityStore bsSecurityStore) {
        this(bsSecurityStore, new SecurityChecker());
    }

    public DefaultBootstrapAuthorizer(BootstrapSecurityStore bsSecurityStore, SecurityChecker securityChecker) {
        this.bsSecurityStore = bsSecurityStore;
        this.securityChecker = securityChecker;
    }

    @Override
    public Authorization isAuthorized(BootstrapRequest request, LwM2mPeer client) {
        if (bsSecurityStore != null && securityChecker != null) {
            Iterator<SecurityInfo> securityInfos = bsSecurityStore.getAllByEndpoint(request.getEndpointName());
            if (securityChecker.checkSecurityInfos(request.getEndpointName(), client, securityInfos)) {
                return Authorization.approved();
            } else {
                return Authorization.declined();
            }
        } else {
            return Authorization.approved();
        }
    }
}
