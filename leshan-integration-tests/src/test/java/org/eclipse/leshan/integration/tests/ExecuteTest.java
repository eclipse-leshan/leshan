/*******************************************************************************
 * Copyright (c) 2013-2015 Sierra Wireless and others.
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
 *     Zebra Technologies - initial API and implementation
 *     Kiran Pradeep - add more test cases
 *     Achim Kraus (Bosch Software Innovations GmbH) - add test for execute security object
 *     Achim Kraus (Bosch Software Innovations GmbH) - replace close() with destroy()
 *******************************************************************************/

package org.eclipse.leshan.integration.tests;

import static org.eclipse.leshan.core.ResponseCode.CHANGED;
import static org.eclipse.leshan.core.ResponseCode.METHOD_NOT_ALLOWED;
import static org.eclipse.leshan.core.ResponseCode.NOT_FOUND;
import static org.eclipse.leshan.integration.tests.util.LeshanTestClientBuilder.givenClientUsing;
import static org.eclipse.leshan.integration.tests.util.LeshanTestServerBuilder.givenServerUsing;
import static org.eclipse.leshan.integration.tests.util.assertion.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.stream.Stream;

import org.eclipse.leshan.core.endpoint.Protocol;
import org.eclipse.leshan.core.request.ExecuteRequest;
import org.eclipse.leshan.core.response.ExecuteResponse;
import org.eclipse.leshan.integration.tests.util.LeshanTestClient;
import org.eclipse.leshan.integration.tests.util.LeshanTestServer;
import org.eclipse.leshan.integration.tests.util.junit5.extensions.BeforeEachParameterizedResolver;
import org.eclipse.leshan.server.registration.Registration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

@ExtendWith(BeforeEachParameterizedResolver.class)
public class ExecuteTest {

    /*---------------------------------/
     *  Parameterized Tests
     * -------------------------------*/
    @ParameterizedTest(name = "{0} - Client using {1} - Server using {2}")
    @MethodSource("transports")
    @Retention(RetentionPolicy.RUNTIME)
    private @interface TestAllTransportLayer {
    }

    static Stream<org.junit.jupiter.params.provider.Arguments> transports() {
        return Stream.of(//
                // ProtocolUsed - Client Endpoint Provider - Server Endpoint Provider
                arguments(Protocol.COAP, "Californium", "Californium"));
    }

    /*---------------------------------/
     *  Set-up and Tear-down Tests
     * -------------------------------*/

    LeshanTestServer server;
    LeshanTestClient client;
    Registration currentRegistration;

    @BeforeEach
    public void start(Protocol givenProtocol, String givenClientEndpointProvider, String givenServerEndpointProvider) {
        server = givenServerUsing(givenProtocol).with(givenServerEndpointProvider).build();
        server.start();
        client = givenClientUsing(givenProtocol).with(givenClientEndpointProvider).connectingTo(server).build();
        client.start();
        server.waitForNewRegistrationOf(client);
        client.waitForRegistrationTo(server);

        currentRegistration = server.getRegistrationFor(client);

    }

    @AfterEach
    public void stop() throws InterruptedException {
        if (client != null)
            client.destroy(false);
        if (server != null)
            server.destroy();
    }

    /*---------------------------------/
     *  Tests
     * -------------------------------*/
    @TestAllTransportLayer
    public void cannot_execute_read_only_resource(Protocol givenProtocol, String givenClientEndpointProvider,
            String givenServerEndpointProvider) throws InterruptedException {
        // execute manufacturer resource on device
        ExecuteResponse response = server.send(currentRegistration, new ExecuteRequest(3, 0, 0));

        // verify result
        assertThat(response) //
                .hasCode(METHOD_NOT_ALLOWED) //
                .hasValidUnderlyingResponseFor(givenServerEndpointProvider);
    }

    @TestAllTransportLayer
    public void cannot_execute_read_write_resource(Protocol givenProtocol, String givenClientEndpointProvider,
            String givenServerEndpointProvider) throws InterruptedException {
        // execute current time resource on device
        ExecuteResponse response = server.send(currentRegistration, new ExecuteRequest(3, 0, 13));

        // verify result
        assertThat(response) //
                .hasCode(METHOD_NOT_ALLOWED) //
                .hasValidUnderlyingResponseFor(givenServerEndpointProvider);
    }

    @TestAllTransportLayer
    public void cannot_execute_nonexisting_resource_on_existing_object(Protocol givenProtocol,
            String givenClientEndpointProvider, String givenServerEndpointProvider) throws InterruptedException {
        int nonExistingResourceId = 9999;
        // execute non existing resource on device
        ExecuteResponse response = server.send(currentRegistration, new ExecuteRequest(3, 0, nonExistingResourceId));

        // verify result
        assertThat(response) //
                .hasCode(NOT_FOUND) //
                .hasValidUnderlyingResponseFor(givenServerEndpointProvider);
    }

    @TestAllTransportLayer
    public void cannot_execute_nonexisting_resource_on_non_existing_object(Protocol givenProtocol,
            String givenClientEndpointProvider, String givenServerEndpointProvider) throws InterruptedException {
        int nonExistingObjectId = 9999;
        ExecuteResponse response = server.send(currentRegistration, new ExecuteRequest(nonExistingObjectId, 0, 0));

        // verify result
        assertThat(response) //
                .hasCode(NOT_FOUND) //
                .hasValidUnderlyingResponseFor(givenServerEndpointProvider);
    }

    @TestAllTransportLayer
    public void cannot_execute_security_object(Protocol givenProtocol, String givenClientEndpointProvider,
            String givenServerEndpointProvider) throws InterruptedException {
        ExecuteResponse response = server.send(currentRegistration, new ExecuteRequest(0, 0, 0));

        // verify result
        assertThat(response) //
                .hasCode(NOT_FOUND) //
                .hasValidUnderlyingResponseFor(givenServerEndpointProvider);
    }

    @TestAllTransportLayer
    public void can_execute_resource(Protocol givenProtocol, String givenClientEndpointProvider,
            String givenServerEndpointProvider) throws InterruptedException {
        // execute reboot resource on device
        ExecuteResponse response = server.send(currentRegistration, new ExecuteRequest(3, 0, 4));

        // verify result
        assertThat(response) //
                .hasCode(CHANGED) //
                .hasValidUnderlyingResponseFor(givenServerEndpointProvider);
    }

    @TestAllTransportLayer
    public void can_execute_resource_with_parameters(Protocol givenProtocol, String givenClientEndpointProvider,
            String givenServerEndpointProvider) throws InterruptedException {
        // execute reboot after 60 seconds on device
        ExecuteResponse response = server.send(currentRegistration, new ExecuteRequest(3, 0, 4, "6"));

        // verify result
        assertThat(response) //
                .hasCode(CHANGED) //
                .hasValidUnderlyingResponseFor(givenServerEndpointProvider);
    }
}
