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

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.leshan.integration.tests.util.assertion.Assertions.assertArg;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.booleanThat;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import java.util.concurrent.TimeUnit;

import org.eclipse.leshan.core.link.lwm2m.LwM2mLinkParser;
import org.eclipse.leshan.core.node.codec.LwM2mDecoder;
import org.eclipse.leshan.core.node.codec.LwM2mEncoder;
import org.eclipse.leshan.server.LeshanServer;
import org.eclipse.leshan.server.endpoint.LwM2mServerEndpointsProvider;
import org.eclipse.leshan.server.model.LwM2mModelProvider;
import org.eclipse.leshan.server.queue.ClientAwakeTimeProvider;
import org.eclipse.leshan.server.registration.Registration;
import org.eclipse.leshan.server.registration.RegistrationIdProvider;
import org.eclipse.leshan.server.registration.RegistrationListener;
import org.eclipse.leshan.server.registration.RegistrationStore;
import org.eclipse.leshan.server.security.Authorizer;
import org.eclipse.leshan.server.security.SecurityStore;
import org.eclipse.leshan.server.security.ServerSecurityInfo;

public class LeshanTestServer extends LeshanServer {

    private final RegistrationListener registrationListener = mock(RegistrationListener.class);

    public LeshanTestServer(LwM2mServerEndpointsProvider endpointsProvider, RegistrationStore registrationStore,
            SecurityStore securityStore, Authorizer authorizer, LwM2mModelProvider modelProvider, LwM2mEncoder encoder,
            LwM2mDecoder decoder, boolean noQueueMode, ClientAwakeTimeProvider awakeTimeProvider,
            RegistrationIdProvider registrationIdProvider, LwM2mLinkParser linkParser,
            ServerSecurityInfo serverSecurityInfo, boolean updateRegistrationOnNotification) {
        super(endpointsProvider, registrationStore, securityStore, authorizer, modelProvider, encoder, decoder,
                noQueueMode, awakeTimeProvider, registrationIdProvider, updateRegistrationOnNotification, linkParser,
                serverSecurityInfo);

        // add mocked listener
        this.getRegistrationService().addListener(registrationListener);
    }

    public Registration getRegistrationFor(LeshanTestClient client) {
        return getRegistrationService().getByEndpoint(client.getEndpointName());
    }

    public void waitForNewRegistrationOf(LeshanTestClient client) {
        waitForNewRegistrationOf(client, 1, TimeUnit.SECONDS);
    }

    public void waitForNewRegistrationOf(LeshanTestClient client, int timeout, TimeUnit unit) {
        verify(registrationListener, timeout(unit.toMillis(timeout)).times(1)).registered(//
                assertArg(reg -> assertThat(reg.getEndpoint()).isEqualTo(client.getEndpointName())), //
                isNull(), //
                isNull());
        verifyNoMoreInteractions(registrationListener);
    }

    public void waitForUpdateOf(Registration expectedRegistration) {
        waitForUpdateOf(expectedRegistration, 1, TimeUnit.SECONDS);
    }

    public void waitForUpdateOf(Registration expectedPreviousReg, int timeout, TimeUnit unit) {
        verify(registrationListener, timeout(unit.toMillis(timeout)).times(1)).updated( //
                any(), //
                assertArg(updatedReg -> updatedReg.getId().equals(expectedPreviousReg.getId())), //
                assertArg(previousReg -> previousReg.equals(expectedPreviousReg)));
        verifyNoMoreInteractions(registrationListener);
    }

    public void waitForDeregistrationOf(Registration expectedRegistration) {
        waitForDeregistrationOf(expectedRegistration, 1, TimeUnit.SECONDS);
    }

    public void waitForDeregistrationOf(Registration expectedRegistration, int timeout, TimeUnit unit) {
        verify(registrationListener, timeout(unit.toMillis(timeout)).times(1)).unregistered(
                assertArg(reg -> reg.getId().equals(expectedRegistration.getId())), //
                assertArg(obs -> assertThat(obs).isEmpty()), //
                booleanThat(expired -> expired == false), //
                isNull());
        verifyNoMoreInteractions(registrationListener);
    }

    @Override
    public void destroy() {
        super.destroy();
        // remove all registration on destroy
        getRegistrationStore().getAllRegistrations()
                .forEachRemaining(r -> getRegistrationStore().removeRegistration(r.getId()));
    }
}
