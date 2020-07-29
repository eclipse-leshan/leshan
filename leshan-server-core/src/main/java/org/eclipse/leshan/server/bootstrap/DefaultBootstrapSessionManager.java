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
package org.eclipse.leshan.server.bootstrap;

import java.util.List;

import org.eclipse.leshan.core.request.BootstrapFinishRequest;
import org.eclipse.leshan.core.request.BootstrapRequest;
import org.eclipse.leshan.core.request.DownlinkRequest;
import org.eclipse.leshan.core.request.Identity;
import org.eclipse.leshan.core.response.LwM2mResponse;
import org.eclipse.leshan.server.security.BootstrapSecurityStore;
import org.eclipse.leshan.server.security.SecurityChecker;
import org.eclipse.leshan.server.security.SecurityInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of a session manager.
 * <p>
 * Starting a session only checks credentials from BootstrapSecurityStore.
 * <p>
 * Nothing specific is done on session's end.
 */
public class DefaultBootstrapSessionManager implements BootstrapSessionManager {

    private static final Logger LOG = LoggerFactory.getLogger(DefaultBootstrapSessionManager.class);

    private BootstrapSecurityStore bsSecurityStore;
    private SecurityChecker securityChecker;

    /**
     * Create a {@link DefaultBootstrapSessionManager} using a default {@link SecurityChecker} to accept or refuse new
     * {@link BootstrapSession}.
     * 
     * @param bsSecurityStore the {@link BootstrapSecurityStore} used by default {@link SecurityChecker}.
     */
    public DefaultBootstrapSessionManager(BootstrapSecurityStore bsSecurityStore) {
        this(bsSecurityStore, new SecurityChecker());
    }

    /**
     * Create a {@link DefaultBootstrapSessionManager}.
     * 
     * @param bsSecurityStore the {@link BootstrapSecurityStore} used by {@link SecurityChecker}.
     * @param securityChecker used to accept or refuse new {@link BootstrapSession}.
     */
    public DefaultBootstrapSessionManager(BootstrapSecurityStore bsSecurityStore, SecurityChecker securityChecker) {
        this.bsSecurityStore = bsSecurityStore;
        this.securityChecker = securityChecker;
    }

    @Override
    public BootstrapSession begin(BootstrapRequest request, Identity clientIdentity) {
        boolean authorized;
        if (bsSecurityStore != null) {
            List<SecurityInfo> securityInfos = bsSecurityStore.getAllByEndpoint(request.getEndpointName());
            authorized = securityChecker.checkSecurityInfos(request.getEndpointName(), clientIdentity, securityInfos);
        } else {
            authorized = true;
        }
        DefaultBootstrapSession session = new DefaultBootstrapSession(request, clientIdentity, authorized);
        LOG.trace("Bootstrap session started : {}", session);
        return session;
    }

    @Override
    public void end(BootstrapSession bsSession) {
        LOG.trace("Bootstrap session finished : {}", bsSession);
    }

    @Override
    public void failed(BootstrapSession bsSession, BootstrapFailureCause cause) {
        LOG.trace("Bootstrap session failed by {}: {}", cause, bsSession);
    }

    @Override
    public void onResponseSuccess(BootstrapSession bsSession, DownlinkRequest<? extends LwM2mResponse> request) {
        if (LOG.isTraceEnabled())
            LOG.trace("{} {} receives success response for {} : {}", request.getClass().getSimpleName(),
                    request.getPath(), bsSession, request);
    }

    @Override
    public BootstrapPolicy onResponseError(BootstrapSession bsSession, DownlinkRequest<? extends LwM2mResponse> request,
            LwM2mResponse response) {
        if (LOG.isTraceEnabled())
            LOG.trace("{} {} receives error response {} for {} : {}", request.getClass().getSimpleName(),
                    request.getPath(), response, bsSession, request);

        if (request instanceof BootstrapFinishRequest) {
            return BootstrapPolicy.STOP;
        }
        return BootstrapPolicy.CONTINUE;
    }

    @Override
    public BootstrapPolicy onRequestFailure(BootstrapSession bsSession,
            DownlinkRequest<? extends LwM2mResponse> request, Throwable cause) {
        if (LOG.isTraceEnabled())
            LOG.trace("{} {} failed because of {} for {} : {}", request.getClass().getSimpleName(), request.getPath(),
                    cause, bsSession, request);

        return BootstrapPolicy.STOP;
    }
}
