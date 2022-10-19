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

import java.net.URI;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.leshan.core.request.BootstrapDownlinkRequest;
import org.eclipse.leshan.core.request.BootstrapFinishRequest;
import org.eclipse.leshan.core.request.BootstrapRequest;
import org.eclipse.leshan.core.request.Identity;
import org.eclipse.leshan.core.response.LwM2mResponse;
import org.eclipse.leshan.core.util.Validate;
import org.eclipse.leshan.server.bootstrap.BootstrapTaskProvider.Tasks;
import org.eclipse.leshan.server.model.LwM2mBootstrapModelProvider;
import org.eclipse.leshan.server.model.StandardBootstrapModelProvider;
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

    private final BootstrapSecurityStore bsSecurityStore;
    private final SecurityChecker securityChecker;
    private final BootstrapTaskProvider tasksProvider;
    private final LwM2mBootstrapModelProvider modelProvider;

    /**
     * Create a {@link DefaultBootstrapSessionManager} using a default {@link SecurityChecker} to accept or refuse new
     * {@link BootstrapSession}.
     *
     * @param bsSecurityStore the {@link BootstrapSecurityStore} used by default {@link SecurityChecker}.
     */
    public DefaultBootstrapSessionManager(BootstrapSecurityStore bsSecurityStore, BootstrapConfigStore configStore) {
        this(bsSecurityStore, new SecurityChecker(), new BootstrapConfigStoreTaskProvider(configStore),
                new StandardBootstrapModelProvider());
    }

    /**
     * Create a {@link DefaultBootstrapSessionManager}.
     *
     * @param bsSecurityStore the {@link BootstrapSecurityStore} used by {@link SecurityChecker}.
     * @param securityChecker used to accept or refuse new {@link BootstrapSession}.
     */
    public DefaultBootstrapSessionManager(BootstrapSecurityStore bsSecurityStore, SecurityChecker securityChecker,
            BootstrapTaskProvider tasksProvider, LwM2mBootstrapModelProvider modelProvider) {
        Validate.notNull(tasksProvider);
        Validate.notNull(modelProvider);
        this.bsSecurityStore = bsSecurityStore;
        this.securityChecker = securityChecker;
        this.tasksProvider = tasksProvider;
        this.modelProvider = modelProvider;
    }

    @Override
    public BootstrapSession begin(BootstrapRequest request, Identity clientIdentity, URI endpointUsed) {
        boolean authorized;
        if (bsSecurityStore != null && securityChecker != null) {
            Iterator<SecurityInfo> securityInfos = bsSecurityStore.getAllByEndpoint(request.getEndpointName());
            authorized = securityChecker.checkSecurityInfos(request.getEndpointName(), clientIdentity, securityInfos);
        } else {
            authorized = true;
        }
        DefaultBootstrapSession session = new DefaultBootstrapSession(request, clientIdentity, authorized,
                endpointUsed);
        LOG.trace("Bootstrap session started : {}", session);

        return session;
    }

    @Override
    public boolean hasConfigFor(BootstrapSession session) {
        Tasks firstTasks = tasksProvider.getTasks(session, null);
        if (firstTasks == null)
            return false;

        initTasks(session, firstTasks);
        return true;
    }

    protected void initTasks(BootstrapSession bssession, Tasks tasks) {
        DefaultBootstrapSession session = (DefaultBootstrapSession) bssession;
        // set models
        if (tasks.supportedObjects != null)
            session.setModel(modelProvider.getObjectModel(session, tasks.supportedObjects));

        // set Requests to Send
        session.setRequests(tasks.requestsToSend);

        // prepare list where we will store Responses
        session.setResponses(new ArrayList<LwM2mResponse>(tasks.requestsToSend.size()));

        // is last Tasks ?
        session.setMoreTasks(!tasks.last);
    }

    @Override
    public BootstrapDownlinkRequest<? extends LwM2mResponse> getFirstRequest(BootstrapSession bsSession) {
        return nextRequest(bsSession);
    }

    protected BootstrapDownlinkRequest<? extends LwM2mResponse> nextRequest(BootstrapSession bsSession) {
        DefaultBootstrapSession session = (DefaultBootstrapSession) bsSession;
        List<BootstrapDownlinkRequest<? extends LwM2mResponse>> requestsToSend = session.getRequests();

        if (!requestsToSend.isEmpty()) {
            // get next requests
            return requestsToSend.remove(0);
        } else {
            if (session.hasMoreTasks()) {
                Tasks nextTasks = tasksProvider.getTasks(session, session.getResponses());
                if (nextTasks == null) {
                    session.setMoreTasks(false);
                    return new BootstrapFinishRequest();
                }

                initTasks(session, nextTasks);
                return nextRequest(bsSession);
            } else {
                return new BootstrapFinishRequest();
            }
        }
    }

    @Override
    public BootstrapPolicy onResponseSuccess(BootstrapSession bsSession,
            BootstrapDownlinkRequest<? extends LwM2mResponse> request, LwM2mResponse response) {
        if (LOG.isTraceEnabled())
            LOG.trace("{} {} receives success response for {} : {}", request.getClass().getSimpleName(),
                    request.getPath(), bsSession, request);

        if (!(request instanceof BootstrapFinishRequest)) {
            // store response
            DefaultBootstrapSession session = (DefaultBootstrapSession) bsSession;
            session.getResponses().add(response);
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
            // store response
            DefaultBootstrapSession session = (DefaultBootstrapSession) bsSession;
            session.getResponses().add(response);

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
