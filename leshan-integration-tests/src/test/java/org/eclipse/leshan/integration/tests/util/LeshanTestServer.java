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
import static org.hamcrest.CoreMatchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.booleanThat;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.notNull;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.hamcrest.MockitoHamcrest.argThat;

import java.util.Collection;
import java.util.concurrent.TimeUnit;

import org.eclipse.californium.elements.Connector;
import org.eclipse.californium.scandium.DTLSConnector;
import org.eclipse.leshan.core.endpoint.Protocol;
import org.eclipse.leshan.core.link.lwm2m.LwM2mLinkParser;
import org.eclipse.leshan.core.node.TimestampedLwM2mNodes;
import org.eclipse.leshan.core.node.codec.LwM2mDecoder;
import org.eclipse.leshan.core.node.codec.LwM2mEncoder;
import org.eclipse.leshan.core.observation.CompositeObservation;
import org.eclipse.leshan.core.observation.Observation;
import org.eclipse.leshan.core.observation.SingleObservation;
import org.eclipse.leshan.core.response.ObserveCompositeResponse;
import org.eclipse.leshan.core.response.ObserveResponse;
import org.eclipse.leshan.server.LeshanServer;
import org.eclipse.leshan.server.californium.endpoint.CaliforniumServerEndpoint;
import org.eclipse.leshan.server.endpoint.LwM2mServerEndpoint;
import org.eclipse.leshan.server.endpoint.LwM2mServerEndpointsProvider;
import org.eclipse.leshan.server.model.LwM2mModelProvider;
import org.eclipse.leshan.server.observation.ObservationListener;
import org.eclipse.leshan.server.queue.ClientAwakeTimeProvider;
import org.eclipse.leshan.server.queue.PresenceListener;
import org.eclipse.leshan.server.registration.Registration;
import org.eclipse.leshan.server.registration.RegistrationDataExtractor;
import org.eclipse.leshan.server.registration.RegistrationIdProvider;
import org.eclipse.leshan.server.registration.RegistrationListener;
import org.eclipse.leshan.server.registration.RegistrationStore;
import org.eclipse.leshan.server.security.Authorizer;
import org.eclipse.leshan.server.security.EditableSecurityStore;
import org.eclipse.leshan.server.security.SecurityStore;
import org.eclipse.leshan.server.security.ServerSecurityInfo;
import org.eclipse.leshan.server.send.SendListener;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;

public class LeshanTestServer extends LeshanServer {

    private final RegistrationListener registrationListener;
    private final PresenceListener presenceListener;
    private final SendListener sendListener;
    private final ObservationListener notificationListener;
    private final InOrder registrationEventInOrder;
    private final InOrder awakeEventInOrder;
    private final InOrder sendEventInOrder;
    private final InOrder notificationEventInOrder;

    public LeshanTestServer(LwM2mServerEndpointsProvider endpointsProvider, RegistrationStore registrationStore,
            SecurityStore securityStore, Authorizer authorizer, LwM2mModelProvider modelProvider, LwM2mEncoder encoder,
            LwM2mDecoder decoder, boolean noQueueMode, ClientAwakeTimeProvider awakeTimeProvider,
            RegistrationIdProvider registrationIdProvider, RegistrationDataExtractor registrationDataExtractor,
            LwM2mLinkParser linkParser, ServerSecurityInfo serverSecurityInfo, boolean updateRegistrationOnNotification,
            boolean updateRegistrationOnSend) {
        super(endpointsProvider, registrationStore, securityStore, authorizer, modelProvider, encoder, decoder,
                noQueueMode, awakeTimeProvider, registrationIdProvider, registrationDataExtractor,
                updateRegistrationOnNotification, updateRegistrationOnSend, linkParser, serverSecurityInfo);

        if (securityStore != null && !(securityStore instanceof EditableSecurityStore)) {
            throw new IllegalStateException(
                    String.format("We need EditableSecurityStore for integrations tests. %s is not",
                            securityStore.getClass().getCanonicalName()));
        }

        // add mocked listener
        registrationListener = mock(RegistrationListener.class);
        getRegistrationService().addListener(registrationListener);
        registrationEventInOrder = inOrder(registrationListener);

        presenceListener = mock(PresenceListener.class);
        getPresenceService().addListener(presenceListener);
        awakeEventInOrder = inOrder(presenceListener);

        sendListener = mock(SendListener.class);
        getSendService().addListener(sendListener);
        sendEventInOrder = inOrder(sendListener);

        notificationListener = mock(ObservationListener.class);
        getObservationService().addListener(notificationListener);
        notificationEventInOrder = inOrder(notificationListener);

    }

    @Override
    public EditableSecurityStore getSecurityStore() {
        return (EditableSecurityStore) super.getSecurityStore();
    }

    public void clearSecurityContextFor(Protocol protocol) {
        // TODO there is something not so good with this abstraction ...
        if (protocol.equals(Protocol.COAP)) {
            return;
        } else if (protocol.equals(Protocol.COAPS)) {
            LwM2mServerEndpoint endpoint = this.getEndpoint(protocol);
            if (endpoint instanceof CaliforniumServerEndpoint) {
                Connector connector = ((CaliforniumServerEndpoint) endpoint).getCoapEndpoint().getConnector();
                if (connector instanceof DTLSConnector) {
                    ((DTLSConnector) connector).clearConnectionState();
                    return;
                } else {
                    throw new IllegalStateException(
                            String.format("clearSecurityContext not implemented for connector %s",
                                    connector.getClass().getSimpleName()));
                }
            }
            throw new IllegalStateException(String.format("clearSecurityContext not implemented for endpoint %s",
                    endpoint.getClass().getSimpleName()));
        } else {
            throw new IllegalStateException(
                    String.format("clearSecurityContext not implemented for protocol %s", protocol));
        }
    }

    public Registration getRegistrationFor(LeshanTestClient client) {
        return getRegistrationService().getByEndpoint(client.getEndpointName());
    }

    public void waitForNewRegistrationOf(LeshanTestClient client) {
        waitForNewRegistrationOf(client, 1, TimeUnit.SECONDS);
    }

    public void waitForNewRegistrationOf(String clientEndpoint) {
        waitForNewRegistrationOf(clientEndpoint, 1, TimeUnit.SECONDS);
    }

    public void waitForNewRegistrationOf(LeshanTestClient client, int timeout, TimeUnit unit) {
        waitForNewRegistrationOf(client.getEndpointName(), timeout, unit);
    }

    public void waitForNewRegistrationOf(String clientEndpoint, int timeout, TimeUnit unit) {
        registrationEventInOrder.verify(registrationListener, timeout(unit.toMillis(timeout)).times(1)).registered(//
                assertArg(reg -> assertThat(reg.getEndpoint()).isEqualTo(clientEndpoint)), //
                isNull(), //
                isNull());
        registrationEventInOrder.verifyNoMoreInteractions();
    }

    public void waitForUpdateOf(Registration expectedRegistration) {
        waitForUpdateOf(expectedRegistration, 1, TimeUnit.SECONDS);
    }

    public void waitForUpdateOf(Registration expectedPreviousReg, int timeout, TimeUnit unit) {
        registrationEventInOrder.verify(registrationListener, timeout(unit.toMillis(timeout)).times(1)).updated( //
                assertArg(
                        regUpdate -> assertThat(regUpdate.getRegistrationId()).isEqualTo(expectedPreviousReg.getId())), //
                assertArg(updatedReg -> assertThat(updatedReg.getId()).isEqualTo(expectedPreviousReg.getId())), //
                assertArg(previousReg -> assertThat(previousReg).isEqualTo(expectedPreviousReg)));
        registrationEventInOrder.verifyNoMoreInteractions();
    }

    public void waitForDeregistrationOf(Registration expectedRegistration) {
        waitForDeregistrationOf(expectedRegistration, 1, TimeUnit.SECONDS);
    }

    public void waitForDeregistrationOf(Registration expectedRegistration, int timeout, TimeUnit unit) {
        registrationEventInOrder.verify(registrationListener, timeout(unit.toMillis(timeout)).times(1)).unregistered(
                assertArg(reg -> assertThat(reg.getId()).isEqualTo(expectedRegistration.getId())), //
                assertArg(obs -> assertThat(obs).isEmpty()), //
                booleanThat(expired -> expired == false), //
                isNull());
        registrationEventInOrder.verifyNoMoreInteractions();
    }

    public void waitForDeregistrationOf(Registration expectedRegistration,
            Collection<Observation> expectedObservations) {
        waitForDeregistrationOf(expectedRegistration, 1, TimeUnit.SECONDS, expectedObservations);
    }

    public void waitForDeregistrationOf(Registration expectedRegistration, int timeout, TimeUnit unit,
            Collection<Observation> expectedObservations) {
        registrationEventInOrder.verify(registrationListener, timeout(unit.toMillis(timeout)).times(1)).unregistered(
                assertArg(reg -> assertThat(reg.getId()).isEqualTo(expectedRegistration.getId())), //
                assertArg(obs -> assertThat(obs).containsExactlyElementsOf(expectedObservations)), //
                booleanThat(expired -> expired == false), //
                isNull());
        registrationEventInOrder.verifyNoMoreInteractions();
    }

    public void waitForReRegistrationOf(Registration expectedRegistration) {
        waitForReRegistrationOf(expectedRegistration, 1, TimeUnit.SECONDS);
    }

    public void waitForReRegistrationOf(Registration expectedPreviousReg, int timeout, TimeUnit unit) {
        registrationEventInOrder.verify(registrationListener, timeout(unit.toMillis(timeout)).times(1)).unregistered(
                assertArg(reg -> assertThat(reg.getId()).isEqualTo(expectedPreviousReg.getId())), //
                assertArg(obs -> assertThat(obs).isEmpty()), //
                booleanThat(expired -> expired == false), //
                assertArg(newReg -> assertThat(newReg.getEndpoint()).isEqualTo(expectedPreviousReg.getEndpoint())));

        registrationEventInOrder.verify(registrationListener, timeout(unit.toMillis(timeout)).times(1)).registered(//
                assertArg(newReg -> assertThat(newReg.getEndpoint()).isEqualTo(expectedPreviousReg.getEndpoint())), //
                assertArg(previousReg -> assertThat(previousReg.getId()).isEqualTo(expectedPreviousReg.getId())), //
                assertArg(obs -> assertThat(obs).isEmpty()));

        registrationEventInOrder.verifyNoMoreInteractions();

    }

    public void ensureNoDeregistration() {
        ensureNoDeregistration(1, TimeUnit.SECONDS);
    }

    public void ensureNoDeregistration(int timeout, TimeUnit unit) {
        try {
            Thread.sleep(unit.toMillis(timeout));
        } catch (InterruptedException e) {
        }
        registrationEventInOrder.verify(registrationListener, never()).unregistered(any(), //
                any(), //
                anyBoolean(), //
                any());
        registrationEventInOrder.verifyNoMoreInteractions();
    }

    public void waitWakingOf(LeshanTestClient client) {
        waitWakingOf(client, 1, TimeUnit.SECONDS);
    }

    public void waitWakingOf(LeshanTestClient client, int timeout, TimeUnit unit) {
        awakeEventInOrder.verify(presenceListener, timeout(unit.toMillis(timeout)).times(1))
                .onAwake(assertArg(reg -> assertThat(reg.getEndpoint()).isEqualTo(client.getEndpointName())));
        awakeEventInOrder.verifyNoMoreInteractions();
    }

    public void waitSleepingOf(LeshanTestClient client) {
        waitSpleepingOf(client, 1, TimeUnit.SECONDS);
    }

    public void waitSpleepingOf(LeshanTestClient client, int timeout, TimeUnit unit) {
        awakeEventInOrder.verify(presenceListener, timeout(unit.toMillis(timeout)).times(1))
                .onSleeping(assertArg(reg -> assertThat(reg.getEndpoint()).isEqualTo(client.getEndpointName())));
        awakeEventInOrder.verifyNoMoreInteractions();
    }

    public TimestampedLwM2mNodes waitForData(String clientEndpoint, int timeout, TimeUnit unit) {
        final ArgumentCaptor<TimestampedLwM2mNodes> c = ArgumentCaptor.forClass(TimestampedLwM2mNodes.class);
        sendEventInOrder.verify(sendListener, timeout(unit.toMillis(timeout)).times(1)).dataReceived(
                assertArg(reg -> assertThat(reg.getEndpoint()).isEqualTo(clientEndpoint)), c.capture(), notNull());
        awakeEventInOrder.verifyNoMoreInteractions();
        return c.getValue();
    }

    public Exception waitForSendDataError(String clientEndpoint, int timeout, TimeUnit unit) {
        final ArgumentCaptor<Exception> c = ArgumentCaptor.forClass(Exception.class);
        sendEventInOrder.verify(sendListener, timeout(unit.toMillis(timeout)).times(1))
                .onError(assertArg(reg -> assertThat(reg.getEndpoint()).isEqualTo(clientEndpoint)), c.capture());
        awakeEventInOrder.verifyNoMoreInteractions();
        return c.getValue();
    }

    public Observation waitForNewObservation(LeshanTestClient client) {
        return waitForNewObservation(client.getEndpointName(), 1, TimeUnit.SECONDS);
    }

    public Observation waitForNewObservation(LeshanTestClient client, int timeout, TimeUnit unit) {
        return waitForNewObservation(client.getEndpointName(), timeout, unit);
    }

    public void waitForNewObservation(Observation obs) {
        waitForNewObservation(obs, 1, TimeUnit.SECONDS);
    }

    public void waitForNewObservation(Observation obs, int timeout, TimeUnit unit) {
        notificationEventInOrder.verify(notificationListener, timeout(unit.toMillis(timeout)).times(1))
                .newObservation(argThat(is(obs)), notNull());
    }

    public Observation waitForNewObservation(String clientEndpoint, int timeout, TimeUnit unit) {
        final ArgumentCaptor<Observation> c = ArgumentCaptor.forClass(Observation.class);
        notificationEventInOrder.verify(notificationListener, timeout(unit.toMillis(timeout)).times(1))
                .newObservation(c.capture(), assertArg(reg -> assertThat(reg.getEndpoint()).isEqualTo(clientEndpoint)));
        return c.getValue();
    }

    public ObserveResponse waitForNotificationOf(SingleObservation obs) {
        return waitForNotificationOf(obs, 1, TimeUnit.SECONDS);
    }

    public ObserveResponse waitForNotificationOf(SingleObservation obs, int timeout, TimeUnit unit) {
        final ArgumentCaptor<ObserveResponse> c = ArgumentCaptor.forClass(ObserveResponse.class);
        notificationEventInOrder.verify(notificationListener, timeout(unit.toMillis(timeout)).times(1)).onResponse(
                assertArg(o -> assertThat(o).isEqualTo(obs)), //
                assertArg(reg -> assertThat(reg.getId()).isEqualTo(obs.getRegistrationId())), //
                c.capture());
        notificationEventInOrder.verifyNoMoreInteractions();
        return c.getValue();
    }

    public ObserveCompositeResponse waitForNotificationOf(CompositeObservation obs) {
        return waitForNotificationOf(obs, 1, TimeUnit.SECONDS);
    }

    public ObserveCompositeResponse waitForNotificationOf(CompositeObservation obs, int timeout, TimeUnit unit) {
        final ArgumentCaptor<ObserveCompositeResponse> c = ArgumentCaptor.forClass(ObserveCompositeResponse.class);
        notificationEventInOrder.verify(notificationListener, timeout(unit.toMillis(timeout)).times(1)).onResponse(
                assertArg(o -> assertThat(o).isEqualTo(obs)), //
                assertArg(reg -> assertThat(reg.getId()).isEqualTo(obs.getRegistrationId())), //
                c.capture());
        notificationEventInOrder.verifyNoMoreInteractions();
        return c.getValue();
    }

    public Exception waitForNotificationErrorOf(Observation obs, int timeout, TimeUnit unit) {
        final ArgumentCaptor<Exception> c = ArgumentCaptor.forClass(Exception.class);
        notificationEventInOrder.verify(notificationListener, timeout(unit.toMillis(timeout)).times(1)).onError(
                assertArg(o -> assertThat(o).isEqualTo(obs)), //
                assertArg(reg -> assertThat(reg.getId()).isEqualTo(obs.getRegistrationId())), //
                c.capture());
        notificationEventInOrder.verifyNoMoreInteractions();
        return c.getValue();
    }

    public void waitForCancellationOf(Observation obs, int timeout, TimeUnit unit) {
        notificationEventInOrder.verify(notificationListener, timeout(unit.toMillis(timeout)).times(1)).cancelled(obs);
        notificationEventInOrder.verifyNoMoreInteractions();
    }

    public void ensureNoNotification(CompositeObservation obs, int timeout, TimeUnit unit) {
        try {
            Thread.sleep(unit.toMillis(timeout));
        } catch (InterruptedException e) {
        }
        notificationEventInOrder.verify(notificationListener, never()).onResponse(argThat(is(obs)), any(), any());
        notificationEventInOrder.verifyNoMoreInteractions();
    }

    public void ensureNoNotification(SingleObservation obs, int timeout, TimeUnit unit) {
        try {
            Thread.sleep(unit.toMillis(timeout));
        } catch (InterruptedException e) {
        }
        notificationEventInOrder.verify(notificationListener, never()).onResponse(argThat(is(obs)), any(), any());
        notificationEventInOrder.verifyNoMoreInteractions();
    }

    public void ensureNoMoreAwakeSleepingEvent() {
        ensureNoMoreAwakeSleepingEvent(1, TimeUnit.SECONDS);
    }

    public void ensureNoMoreAwakeSleepingEvent(int timeout, TimeUnit unit) {
        try {
            Thread.sleep(unit.toMillis(timeout));
        } catch (InterruptedException e) {
        }
        awakeEventInOrder.verifyNoMoreInteractions();
    }

    @Override
    public void destroy() {
        super.destroy();
        // remove all registration on destroy
        getRegistrationStore().getAllRegistrations()
                .forEachRemaining(r -> getRegistrationStore().removeRegistration(r.getId()));
        // remove all registration on destroy
        if (getSecurityStore() != null)
            getSecurityStore().getAll().iterator()
                    .forEachRemaining(s -> getSecurityStore().remove(s.getEndpoint(), false));
    }

}
