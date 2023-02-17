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
import static org.mockito.ArgumentMatchers.isNotNull;
import static org.mockito.ArgumentMatchers.notNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import java.security.cert.Certificate;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.eclipse.leshan.client.LeshanClient;
import org.eclipse.leshan.client.bootstrap.BootstrapConsistencyChecker;
import org.eclipse.leshan.client.endpoint.LwM2mClientEndpointsProvider;
import org.eclipse.leshan.client.engine.RegistrationEngineFactory;
import org.eclipse.leshan.client.observer.LwM2mClientObserver;
import org.eclipse.leshan.client.resource.LwM2mObjectEnabler;
import org.eclipse.leshan.client.send.DataSender;
import org.eclipse.leshan.core.link.LinkSerializer;
import org.eclipse.leshan.core.link.lwm2m.attributes.LwM2mAttributeParser;
import org.eclipse.leshan.core.node.codec.LwM2mDecoder;
import org.eclipse.leshan.core.node.codec.LwM2mEncoder;

public class LeshanTestClient extends LeshanClient {

    private final LwM2mClientObserver clientObserver = mock(LwM2mClientObserver.class);

    private final String endpointName;

    public LeshanTestClient(String endpoint, List<? extends LwM2mObjectEnabler> objectEnablers,
            List<DataSender> dataSenders, List<Certificate> trustStore, RegistrationEngineFactory engineFactory,
            BootstrapConsistencyChecker checker, Map<String, String> additionalAttributes,
            Map<String, String> bsAdditionalAttributes, LwM2mEncoder encoder, LwM2mDecoder decoder,
            ScheduledExecutorService sharedExecutor, LinkSerializer linkSerializer,
            LwM2mAttributeParser attributeParser, LwM2mClientEndpointsProvider endpointsProvider) {
        super(endpoint, objectEnablers, dataSenders, trustStore, engineFactory, checker, additionalAttributes,
                bsAdditionalAttributes, encoder, decoder, sharedExecutor, linkSerializer, attributeParser,
                endpointsProvider);

        // Store some internal attribute
        this.endpointName = endpoint;

        // Add Mock Listener
        this.addObserver(clientObserver);
    }

    public String getEndpointName() {
        return endpointName;
    }

    public void waitForRegistrationTo(LeshanTestServer server) {
        waitForRegistrationTo(server, 5, TimeUnit.SECONDS);
    }

    public void waitForRegistrationTo(LeshanTestServer server, long timeout, TimeUnit unit) {
        verify(clientObserver, timeout(unit.toMillis(timeout)).times(1)).onRegistrationStarted(assertArg( //
                s -> {
                    assertThat(server.getEndpoints()) //
                            .filteredOn(ep -> ep.getURI().toString().equals(s.getUri())) //
                            .hasSize(1);

                }), //
                isNotNull());
        verify(clientObserver, timeout(unit.toMillis(timeout)).times(1)).onRegistrationSuccess(assertArg( //
                s -> assertThat(server.getEndpoints()) //
                        .filteredOn(ep -> ep.getURI().toString().equals(s.getUri())) //
                        .hasSize(1)), //
                isNotNull(), isNotNull());
        verifyNoMoreInteractions(clientObserver);
    }

    public void waitForUpdateTo(LeshanTestServer server, long timeout, TimeUnit unit) {
        verify(clientObserver, timeout(unit.toMillis(timeout)).times(1)).onUpdateStarted(assertArg( //
                s -> assertThat(server.getEndpoints()) //
                        .filteredOn(ep -> ep.getURI().toString().equals(s.getUri())) //
                        .hasSize(1)), //
                notNull());
        verify(clientObserver, timeout(unit.toMillis(timeout)).times(1)).onUpdateSuccess(assertArg( //
                s -> assertThat(server.getEndpoints()) //
                        .filteredOn(ep -> ep.getURI().toString().equals(s.getUri())) //
                        .hasSize(1)), //
                notNull());
        verifyNoMoreInteractions(clientObserver);
    }
}
