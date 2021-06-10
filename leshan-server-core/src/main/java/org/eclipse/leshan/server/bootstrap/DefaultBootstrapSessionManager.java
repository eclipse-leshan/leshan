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

import java.util.Iterator;
import java.util.List;

import org.eclipse.leshan.core.request.BootstrapDownlinkRequest;
import org.eclipse.leshan.core.request.BootstrapFinishRequest;
import org.eclipse.leshan.core.request.BootstrapRequest;
import org.eclipse.leshan.core.request.Identity;
import org.eclipse.leshan.core.response.LwM2mResponse;
import org.eclipse.leshan.core.util.Validate;
import org.eclipse.leshan.server.security.BootstrapSecurityStore;
import org.eclipse.leshan.server.security.SecurityChecker;
import org.eclipse.leshan.server.security.SecurityInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Default implementation of a {@link BootstrapSessionManager}.
 * <p>
 * Starting a session only checks credentials from BootstrapSecurityStore.
 * <p>
 * Nothing specific is done on session's end.
 */
public class DefaultBootstrapSessionManager implements BootstrapSessionManager {

    private static final Logger LOG = LoggerFactory.getLogger(DefaultBootstrapSessionManager.class);

    private BootstrapSecurityStore bsSecurityStore;
    private SecurityChecker securityChecker;
    private BootstrapConfigStore configStore;

    /**
     * Create a {@link DefaultBootstrapSessionManager} using a default {@link SecurityChecker} to accept or refuse new
     * {@link BootstrapSession}.
     * 
     * @param bsSecurityStore the {@link BootstrapSecurityStore} used by default {@link SecurityChecker}.
     */
    public DefaultBootstrapSessionManager(BootstrapSecurityStore bsSecurityStore, BootstrapConfigStore configStore) {
        this(bsSecurityStore, new SecurityChecker(), configStore);
    }

    /**
     * Create a {@link DefaultBootstrapSessionManager}.
     * 
     * @param bsSecurityStore the {@link BootstrapSecurityStore} used by {@link SecurityChecker}.
     * @param securityChecker used to accept or refuse new {@link BootstrapSession}.
     */
    public DefaultBootstrapSessionManager(BootstrapSecurityStore bsSecurityStore, SecurityChecker securityChecker,
            BootstrapConfigStore configStore) {
        Validate.notNull(configStore);
        this.bsSecurityStore = bsSecurityStore;
        this.securityChecker = securityChecker;
        this.configStore = configStore;
    }

    @Override
    public BootstrapSession begin(BootstrapRequest request, Identity clientIdentity) {
        boolean authorized;
        if (bsSecurityStore != null) {
            Iterator<SecurityInfo> securityInfos = bsSecurityStore.getAllByEndpoint(request.getEndpointName());
            authorized = securityChecker.checkSecurityInfos(request.getEndpointName(), clientIdentity, securityInfos);
        } else {
            authorized = true;
        }
        DefaultBootstrapSession session = new DefaultBootstrapSession(request, clientIdentity, authorized);
        LOG.trace("Bootstrap session started : {}", session);
        return session;
    }

    @Override
    public boolean hasConfigFor(BootstrapSession session) {
        BootstrapConfig configuration = configStore.get(session.getEndpoint(), session.getIdentity(), session);
        if (configuration == null)
            return false;

        List<BootstrapDownlinkRequest<? extends LwM2mResponse>> requests = BootstrapUtil.toRequests(configuration,
                session.getContentFormat());

        ((DefaultBootstrapSession) session).setRequests(requests);
        return true;
    }

    @Override
    public BootstrapDownlinkRequest<? extends LwM2mResponse> getFirstRequest(BootstrapSession bsSession) {
        return nextRequest(bsSession);
    }

    protected BootstrapDownlinkRequest<? extends LwM2mResponse> nextRequest(BootstrapSession bsSession) {
        DefaultBootstrapSession session = (DefaultBootstrapSession) bsSession;
        List<BootstrapDownlinkRequest<? extends LwM2mResponse>> requestsToSend = session.getRequests();
        if (requestsToSend.isEmpty()) {
            return new BootstrapFinishRequest();
        } else {
            return requestsToSend.remove(0);
        }
    }

    @Override
    public BootstrapPolicy onResponseSuccess(BootstrapSession bsSession,
            BootstrapDownlinkRequest<? extends LwM2mResponse> request) {
        if (LOG.isTraceEnabled())
            LOG.trace("{} {} receives success response for {} : {}", request.getClass().getSimpleName(),
                    request.getPath(), bsSession, request);

        if (!(request instanceof BootstrapFinishRequest)) {
            // on success for NOT bootstrap finish request we send next request
            return BootstrapPolicy.continueWith(nextRequest(bsSession));
        } else {
            // on success for bootstrap finish request we stop the session
            return BootstrapPolicy.finished();
        }
    }

    @Override
    public BootstrapPolicy onResponseError(BootstrapSession bsSession,
            BootstrapDownlinkRequest<? extends LwM2mResponse> request, LwM2mResponse response) {
        if (LOG.isTraceEnabled())
            LOG.trace("{} {} receives error response {} for {} : {}", request.getClass().getSimpleName(),
                    request.getPath(), response, bsSession, request);

        if (!(request instanceof BootstrapFinishRequest)) {
            // on response error for NOT bootstrap finish request we continue any sending next request
            return BootstrapPolicy.continueWith(nextRequest(bsSession));
        } else {
            // on response error for bootstrap finish request we stop the session
            return BootstrapPolicy.failed();
        }
    }

    @Override
    public BootstrapPolicy onRequestFailure(BootstrapSession bsSession,
            BootstrapDownlinkRequest<? extends LwM2mResponse> request, Throwable cause) {
        if (LOG.isTraceEnabled())
            LOG.trace("{} {} failed because of {} for {} : {}", request.getClass().getSimpleName(), request.getPath(),
                    cause, bsSession, request);

        return BootstrapPolicy.failed();
    }

    @Override
    public void end(BootstrapSession bsSession) {
        LOG.trace("Bootstrap session finished : {}", bsSession);
    }

    @Override
    public void failed(BootstrapSession bsSession, BootstrapFailureCause cause) {
        LOG.trace("Bootstrap session failed by {}: {}", cause, bsSession);
    }

}
