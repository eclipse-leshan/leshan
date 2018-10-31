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
package org.eclipse.leshan.server.impl;

import java.util.List;

import org.eclipse.leshan.core.request.DownlinkRequest;
import org.eclipse.leshan.core.request.Identity;
import org.eclipse.leshan.core.response.LwM2mResponse;
import org.eclipse.leshan.server.bootstrap.BootstrapFailureCause;
import org.eclipse.leshan.server.bootstrap.BootstrapSession;
import org.eclipse.leshan.server.bootstrap.BootstrapSessionManager;
import org.eclipse.leshan.server.security.BootstrapSecurityStore;
import org.eclipse.leshan.server.security.SecurityChecker;
import org.eclipse.leshan.server.security.SecurityInfo;

/**
 * Implementation of a session manager.
 * 
 * Starting a session only checks credentials from BootstrapSecurityStore.
 * 
 * Nothing specific is done on session's end.
 *
 */
public class DefaultBootstrapSessionManager implements BootstrapSessionManager {

    private BootstrapSecurityStore bsSecurityStore;
    private SecurityChecker securityChecker;

    public DefaultBootstrapSessionManager(BootstrapSecurityStore bsSecurityStore) {
        this(bsSecurityStore, new SecurityChecker());
    }

    public DefaultBootstrapSessionManager(BootstrapSecurityStore bsSecurityStore, SecurityChecker securityChecker) {
        this.bsSecurityStore = bsSecurityStore;
        this.securityChecker = securityChecker;
    }

    @Override
    public BootstrapSession begin(String endpoint, Identity clientIdentity) {
        boolean authorized;
        if (bsSecurityStore != null) {
            List<SecurityInfo> securityInfos = bsSecurityStore.getAllByEndpoint(endpoint);
            authorized = securityChecker.checkSecurityInfos(endpoint, clientIdentity, securityInfos);
        } else {
            authorized = true;
        }

        return new DefaultBootstrapSession(endpoint, clientIdentity, authorized);
    }

    @Override
    public void end(BootstrapSession bsSession) {
    }

    @Override
    public void failed(BootstrapSession bsSession, BootstrapFailureCause cause,
            DownlinkRequest<? extends LwM2mResponse> request) {
    }

}
