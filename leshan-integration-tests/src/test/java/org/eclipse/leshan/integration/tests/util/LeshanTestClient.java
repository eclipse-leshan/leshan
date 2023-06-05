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
import static org.mockito.ArgumentMatchers.isNotNull;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.notNull;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;

import java.security.cert.Certificate;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.eclipse.californium.core.network.CoapEndpoint;
import org.eclipse.californium.elements.Connector;
import org.eclipse.leshan.client.LeshanClient;
import org.eclipse.leshan.client.bootstrap.BootstrapConsistencyChecker;
import org.eclipse.leshan.client.californium.endpoint.CaliforniumClientEndpoint;
import org.eclipse.leshan.client.endpoint.LwM2mClientEndpointsProvider;
import org.eclipse.leshan.client.engine.RegistrationEngineFactory;
import org.eclipse.leshan.client.observer.LwM2mClientObserver;
import org.eclipse.leshan.client.resource.LwM2mObjectEnabler;
import org.eclipse.leshan.client.send.DataSender;
import org.eclipse.leshan.client.servers.ServerIdentity;
import org.eclipse.leshan.client.util.LinkFormatHelper;
import org.eclipse.leshan.core.link.LinkSerializer;
import org.eclipse.leshan.core.link.lwm2m.attributes.LwM2mAttributeParser;
import org.eclipse.leshan.core.node.codec.LwM2mDecoder;
import org.eclipse.leshan.core.node.codec.LwM2mEncoder;
import org.eclipse.leshan.server.bootstrap.LeshanBootstrapServer;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;

public class LeshanTestClient extends LeshanClient {

    private final LwM2mClientObserver clientObserver;

    private final String endpointName;
    private final InOrder inOrder;

    public LeshanTestClient(String endpoint, List<? extends LwM2mObjectEnabler> objectEnablers,
            List<DataSender> dataSenders, List<Certificate> trustStore, RegistrationEngineFactory engineFactory,
            BootstrapConsistencyChecker checker, Map<String, String> additionalAttributes,
            Map<String, String> bsAdditionalAttributes, LwM2mEncoder encoder, LwM2mDecoder decoder,
            ScheduledExecutorService sharedExecutor, LinkSerializer linkSerializer, LinkFormatHelper linkFormatHelper,
            LwM2mAttributeParser attributeParser, LwM2mClientEndpointsProvider endpointsProvider) {
        super(endpoint, objectEnablers, dataSenders, trustStore, engineFactory, checker, additionalAttributes,
                bsAdditionalAttributes, encoder, decoder, sharedExecutor, linkSerializer, linkFormatHelper,
                attributeParser, endpointsProvider);

        // Store some internal attribute
        this.endpointName = endpoint;

        // Add Mock Listener
        clientObserver = mock(LwM2mClientObserver.class);
        addObserver(clientObserver);
        inOrder = inOrder(clientObserver);
    }

    public String getEndpointName() {
        return endpointName;
    }

    public Connector getClientConnector(ServerIdentity server) {
        CaliforniumClientEndpoint endpoint = (CaliforniumClientEndpoint) getEndpoint(server);
        return ((CoapEndpoint) endpoint.getCoapEndpoint()).getConnector();
    }

    public ServerIdentity getServerIdForRegistrationId(String regId) {
        Map<String, ServerIdentity> registeredServers = getRegisteredServers();
        if (registeredServers != null && !registeredServers.isEmpty()) {
            return registeredServers.get(regId);
        }
        return null;
    }

    public void waitForRegistrationTo(LeshanTestServer server) {
        waitForRegistrationTo(server, 1, TimeUnit.SECONDS);
    }

    public void waitForRegistrationTo(LeshanTestServer server, long timeout, TimeUnit unit) {
        inOrder.verify(clientObserver, timeout(unit.toMillis(timeout)).times(1)).onRegistrationStarted(assertArg( //
                s -> {
                    assertThat(server.getEndpoints()) //
                            .filteredOn(ep -> ep.getURI().toString().equals(s.getUri())) //
                            .hasSize(1);

                }), //
                isNotNull());
        inOrder.verify(clientObserver, timeout(unit.toMillis(timeout)).times(1)).onRegistrationSuccess(assertArg( //
                s -> assertThat(server.getEndpoints()) //
                        .filteredOn(ep -> ep.getURI().toString().equals(s.getUri())) //
                        .hasSize(1)), //
                isNotNull(), isNotNull());
        inOrder.verifyNoMoreInteractions();
    }

    public void waitForUpdateTo(LeshanTestServer server, long timeout, TimeUnit unit) {
        inOrder.verify(clientObserver, timeout(unit.toMillis(timeout)).times(1)).onUpdateStarted(assertArg( //
                s -> assertThat(server.getEndpoints()) //
                        .filteredOn(ep -> ep.getURI().toString().equals(s.getUri())) //
                        .hasSize(1)), //
                notNull());
        inOrder.verify(clientObserver, timeout(unit.toMillis(timeout)).times(1)).onUpdateSuccess(assertArg( //
                s -> assertThat(server.getEndpoints()) //
                        .filteredOn(ep -> ep.getURI().toString().equals(s.getUri())) //
                        .hasSize(1)), //
                notNull());
        inOrder.verifyNoMoreInteractions();
    }

    public void waitForDeregistrationTo(LeshanTestServer server) {
        waitForDeregistrationTo(server, 1, TimeUnit.SECONDS);
    }

    public void waitForDeregistrationTo(LeshanTestServer server, long timeout, TimeUnit unit) {
        inOrder.verify(clientObserver, timeout(unit.toMillis(timeout)).times(1)).onDeregistrationStarted(assertArg( //
                s -> {
                    assertThat(server.getEndpoints()) //
                            .filteredOn(ep -> ep.getURI().toString().equals(s.getUri())) //
                            .hasSize(1);

                }), //
                isNotNull());
        inOrder.verify(clientObserver, timeout(unit.toMillis(timeout)).times(1)).onDeregistrationSuccess(assertArg( //
                s -> assertThat(server.getEndpoints()) //
                        .filteredOn(ep -> ep.getURI().toString().equals(s.getUri())) //
                        .hasSize(1)), //
                isNotNull());
        inOrder.verifyNoMoreInteractions();
    }

    public void waitForUpdateTimeoutTo(LeshanTestServer server) {
        waitForUpdateTimeoutTo(server, 1, TimeUnit.SECONDS);
    }

    public void waitForUpdateTimeoutTo(LeshanTestServer server, long timeout, TimeUnit unit) {
        inOrder.verify(clientObserver, timeout(unit.toMillis(timeout)).times(1)).onUpdateStarted(assertArg( //
                s -> assertThat(server.getEndpoints()) //
                        .filteredOn(ep -> ep.getURI().toString().equals(s.getUri())) //
                        .hasSize(1)), //
                notNull());

        inOrder.verify(clientObserver, timeout(unit.toMillis(timeout)).times(1)).onUpdateTimeout(assertArg( //
                s -> assertThat(server.getEndpoints()) //
                        .filteredOn(ep -> ep.getURI().toString().equals(s.getUri())) //
                        .hasSize(1)), //
                notNull());
        // if client update timeout, it will retry again then try a register so all events can not be consume by inOrder
        // ...
    }

    public void waitForUpdateFailureTo(LeshanTestServer server) {
        waitForUpdateFailureTo(server, 1, TimeUnit.SECONDS);
    }

    public void waitForUpdateFailureTo(LeshanTestServer server, long timeout, TimeUnit unit) {
        inOrder.verify(clientObserver, timeout(unit.toMillis(timeout)).times(1)).onUpdateStarted(assertArg( //
                s -> assertThat(server.getEndpoints()) //
                        .filteredOn(ep -> ep.getURI().toString().equals(s.getUri())) //
                        .hasSize(1)), //
                notNull());
        inOrder.verify(clientObserver, timeout(unit.toMillis(timeout)).times(1)).onUpdateFailure(assertArg( //
                s -> assertThat(server.getEndpoints()) //
                        .filteredOn(ep -> ep.getURI().toString().equals(s.getUri())) //
                        .hasSize(1)), //
                notNull(), //
                notNull(), // TODO we should be able to check response code
                any(), //
                any()); // TODO we should be able to check exception
        // if client update timeout, it will retry again then try a register so all events can not be consume by inOrder
        // ...
    }

    public void waitForBootstrapSuccess(LeshanBootstrapServer server, long timeout, TimeUnit unit) {
        inOrder.verify(clientObserver, timeout(unit.toMillis(timeout)).times(1)).onBootstrapStarted(assertArg( //
                s -> assertThat(server.getEndpoints()) //
                        .filteredOn(ep -> ep.getURI().toString().equals(s.getUri())) //
                        .hasSize(1)), //
                notNull());

        inOrder.verify(clientObserver, timeout(unit.toMillis(timeout)).times(1)).onBootstrapSuccess(assertArg( //
                s -> assertThat(server.getEndpoints()) //
                        .filteredOn(ep -> ep.getURI().toString().equals(s.getUri())) //
                        .hasSize(1)), //
                notNull());
    }

    public Exception waitForBootstrapFailure(LeshanBootstrapServer server, long timeout, TimeUnit unit) {
        final ArgumentCaptor<Exception> c = ArgumentCaptor.forClass(Exception.class);

        inOrder.verify(clientObserver, timeout(unit.toMillis(timeout)).times(1)).onBootstrapStarted(assertArg( //
                s -> assertThat(server.getEndpoints()) //
                        .filteredOn(ep -> ep.getURI().toString().equals(s.getUri())) //
                        .hasSize(1)), //
                notNull());

        inOrder.verify(clientObserver, timeout(unit.toMillis(timeout)).times(1)).onBootstrapFailure(assertArg( //
                s -> assertThat(server.getEndpoints()) //
                        .filteredOn(ep -> ep.getURI().toString().equals(s.getUri())) //
                        .hasSize(1)), //
                notNull(), //
                isNull(), // TODO we should be able to check response code
                any(), //
                c.capture());
        return c.getValue();
    }
}
