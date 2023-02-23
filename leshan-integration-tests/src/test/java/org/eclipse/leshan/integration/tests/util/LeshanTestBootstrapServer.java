/*******************************************************************************
 * Copyright (c) 2023 Sierra Wireless and others.
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
package org.eclipse.leshan.integration.tests.util;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.notNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.concurrent.TimeUnit;

import org.eclipse.leshan.core.link.lwm2m.LwM2mLinkParser;
import org.eclipse.leshan.core.node.codec.LwM2mDecoder;
import org.eclipse.leshan.core.node.codec.LwM2mEncoder;
import org.eclipse.leshan.core.request.BootstrapDownlinkRequest;
import org.eclipse.leshan.core.response.LwM2mResponse;
import org.eclipse.leshan.server.bootstrap.BootstrapFailureCause;
import org.eclipse.leshan.server.bootstrap.BootstrapHandlerFactory;
import org.eclipse.leshan.server.bootstrap.BootstrapSession;
import org.eclipse.leshan.server.bootstrap.BootstrapSessionListener;
import org.eclipse.leshan.server.bootstrap.BootstrapSessionManager;
import org.eclipse.leshan.server.bootstrap.EditableBootstrapConfigStore;
import org.eclipse.leshan.server.bootstrap.LeshanBootstrapServer;
import org.eclipse.leshan.server.bootstrap.endpoint.LwM2mBootstrapServerEndpointsProvider;
import org.eclipse.leshan.server.security.BootstrapSecurityStore;
import org.eclipse.leshan.server.security.EditableSecurityStore;
import org.eclipse.leshan.server.security.ServerSecurityInfo;
import org.mockito.ArgumentCaptor;

public class LeshanTestBootstrapServer extends LeshanBootstrapServer {

    private final EditableBootstrapConfigStore configStore;
    private final EditableSecurityStore securityStore;
    private BootstrapSessionListener bootstrapSession;

    public LeshanTestBootstrapServer(LwM2mBootstrapServerEndpointsProvider endpointsProvider,
            BootstrapSessionManager bsSessionManager, BootstrapHandlerFactory bsHandlerFactory, LwM2mEncoder encoder,
            LwM2mDecoder decoder, LwM2mLinkParser linkParser, BootstrapSecurityStore securityStore,
            ServerSecurityInfo serverSecurityInfo, //
            // arguments only needed for LeshanTestBootstrapServer
            EditableBootstrapConfigStore configStore, EditableSecurityStore editableSecurityStore) {

        super(endpointsProvider, bsSessionManager, bsHandlerFactory, encoder, decoder, linkParser, securityStore,
                serverSecurityInfo);
        // keep store reference for getter.
        this.configStore = configStore;
        this.securityStore = editableSecurityStore;

        // add mocked listener
        bootstrapSession = mock(BootstrapSessionListener.class);
        addListener(bootstrapSession);
    }

    public EditableBootstrapConfigStore getConfigStore() {
        return configStore;
    }

    public EditableSecurityStore getEditableSecurityStore() {
        return securityStore;
    }

    public void resetInvocations() {
        removeListener(bootstrapSession);
        bootstrapSession = mock(BootstrapSessionListener.class);
        addListener(bootstrapSession);
    }

    public BootstrapSession waitForSuccessfullBootstrap(int timeout, TimeUnit unit) {
        BootstrapSession session = waitForBootstrapAuthorized(timeout, unit);
        verify(bootstrapSession, timeout(unit.toMillis(timeout)).times(1)).end(session);
        return session;
    }

    public BootstrapSession waitForBootstrapAuthorized(int timeout, TimeUnit unit) {
        final ArgumentCaptor<BootstrapSession> c = ArgumentCaptor.forClass(BootstrapSession.class);
        verify(bootstrapSession, timeout(unit.toMillis(timeout)).times(1)).sessionInitiated(notNull(), notNull());
        verify(bootstrapSession, timeout(unit.toMillis(timeout)).times(1)).authorized(c.capture());
        return c.getValue();
    }

    public BootstrapFailureCause waitForBootstrapFailure(BootstrapSession session, int timeout, TimeUnit unit) {
        final ArgumentCaptor<BootstrapFailureCause> c = ArgumentCaptor.forClass(BootstrapFailureCause.class);
        verify(bootstrapSession, timeout(unit.toMillis(timeout)).times(1)).failed(eq(session), c.capture());
        return c.getValue();
    }

    public BootstrapFailureCause waitForBootstrapFailure(int timeout, TimeUnit unit) {
        BootstrapSession session = waitForBootstrapAuthorized(timeout, unit);
        return waitForBootstrapFailure(session, timeout, unit);
    }

    public BootstrapSession verifyForSuccessfullBootstrap() {
        return waitForSuccessfullBootstrap(0, TimeUnit.SECONDS);
    }

    public LwM2mResponse getFirstResponseFor(BootstrapSession session, BootstrapDownlinkRequest<?> request) {
        final ArgumentCaptor<LwM2mResponse> c = ArgumentCaptor.forClass(LwM2mResponse.class);
        verify(bootstrapSession, times(1)).onResponseSuccess(eq(session), eq(request), c.capture());
        return c.getValue();
    }
}
