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
package org.eclipse.leshan.server.bootstrap;

import org.eclipse.leshan.core.request.BootstrapRequest;
import org.eclipse.leshan.core.request.Identity;
import org.eclipse.leshan.server.security.BootstrapAuthorizer;
import org.eclipse.leshan.server.security.BootstrapSecurityStore;
import org.eclipse.leshan.server.security.SecurityChecker;
import org.eclipse.leshan.server.security.SecurityInfo;

import java.util.Iterator;

public class DefaultBootstrapAuthorizer implements BootstrapAuthorizer {

    private BootstrapSecurityStore bsSecurityStore;
    private SecurityChecker securityChecker;

    public DefaultBootstrapAuthorizer(BootstrapSecurityStore bsSecurityStore, SecurityChecker securityChecker) {
        this.bsSecurityStore = bsSecurityStore;
        this.securityChecker = securityChecker;
    }

    public DefaultBootstrapAuthorizer() {
    }

    @Override
    public boolean isAuthorized(BootstrapRequest request, Identity clientIdentity) {
        if (bsSecurityStore != null && securityChecker != null) {
            Iterator<SecurityInfo> securityInfos = bsSecurityStore.getAllByEndpoint(request.getEndpointName());
            return securityChecker.checkSecurityInfos(request.getEndpointName(), clientIdentity, securityInfos);
        } else {
            return true;
        }
    }
}
