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
 *     Michał Wadowski (Orange) - Improved compliance with rfc6690
 *******************************************************************************/

package org.eclipse.leshan.integration.tests;

import static org.eclipse.leshan.core.ResponseCode.CONTENT;
import static org.eclipse.leshan.core.ResponseCode.NOT_FOUND;
import static org.eclipse.leshan.integration.tests.util.LeshanTestClientBuilder.givenClientUsing;
import static org.eclipse.leshan.integration.tests.util.LeshanTestServerBuilder.givenServerUsing;
import static org.eclipse.leshan.integration.tests.util.assertion.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.stream.Stream;

import org.eclipse.leshan.core.endpoint.Protocol;
import org.eclipse.leshan.core.request.DiscoverRequest;
import org.eclipse.leshan.core.response.DiscoverResponse;
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
public class DiscoverTest {

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
    public void can_discover_object(Protocol givenProtocol, String givenClientEndpointProvider,
            String givenServerEndpointProvider) throws InterruptedException {
        // read ACL object
        DiscoverResponse response = server.send(currentRegistration, new DiscoverRequest(3));

        // verify result
        assertThat(response) //
                .hasCode(CONTENT) //
                .hasValidUnderlyingResponseFor(givenServerEndpointProvider) // */
                .hasObjectLinksLike(
                        "</3>;ver=1.1,</3/0>,</3/0/0>,</3/0/1>,</3/0/2>,</3/0/11>,</3/0/14>,</3/0/15>,</3/0/16>");

    }

    @TestAllTransportLayer
    public void cant_discover_non_existent_object(Protocol givenProtocol, String givenClientEndpointProvider,
            String givenServerEndpointProvider) throws InterruptedException {
        // read ACL object
        DiscoverResponse response = server.send(currentRegistration, new DiscoverRequest(4));

        // verify result
        assertThat(response) //
                .hasCode(NOT_FOUND) //
                .hasValidUnderlyingResponseFor(givenServerEndpointProvider);
    }

    @TestAllTransportLayer
    public void can_discover_object_instance(Protocol givenProtocol, String givenClientEndpointProvider,
            String givenServerEndpointProvider) throws InterruptedException {
        // read ACL object
        DiscoverResponse response = server.send(currentRegistration, new DiscoverRequest(3, 0));

        // verify result
        assertThat(response) //
                .hasCode(CONTENT) //
                .hasValidUnderlyingResponseFor(givenServerEndpointProvider) //
                .hasObjectLinksLike("</3/0>,</3/0/0>,</3/0/1>,</3/0/2>,</3/0/11>,</3/0/14>,</3/0/15>,</3/0/16>");
    }

    @TestAllTransportLayer
    public void cant_discover_non_existent_instance(Protocol givenProtocol, String givenClientEndpointProvider,
            String givenServerEndpointProvider) throws InterruptedException {
        // read ACL object
        DiscoverResponse response = server.send(currentRegistration, new DiscoverRequest(3, 1));

        // verify result
        assertThat(response) //
                .hasCode(NOT_FOUND) //
                .hasValidUnderlyingResponseFor(givenServerEndpointProvider);
    }

    @TestAllTransportLayer
    public void can_discover_resource(Protocol givenProtocol, String givenClientEndpointProvider,
            String givenServerEndpointProvider) throws InterruptedException {
        // read ACL object
        DiscoverResponse response = server.send(currentRegistration, new DiscoverRequest(3, 0, 0));

        // verify result
        assertThat(response) //
                .hasCode(CONTENT) //
                .hasValidUnderlyingResponseFor(givenServerEndpointProvider) //
                .hasObjectLinksLike("</3/0/0>");

        assertThat(response.getObjectLinks()).isLikeLwM2mLinks("</3/0/0>");
    }

    @TestAllTransportLayer
    public void cant_discover_resource_of_non_existent_object(Protocol givenProtocol,
            String givenClientEndpointProvider, String givenServerEndpointProvider) throws InterruptedException {
        // read ACL object
        DiscoverResponse response = server.send(currentRegistration, new DiscoverRequest(4, 0, 0));

        // verify result
        assertThat(response) //
                .hasCode(NOT_FOUND) //
                .hasValidUnderlyingResponseFor(givenServerEndpointProvider);
    }

    @TestAllTransportLayer
    public void cant_discover_resource_of_non_existent_instance(Protocol givenProtocol,
            String givenClientEndpointProvider, String givenServerEndpointProvider) throws InterruptedException {
        // read ACL object
        DiscoverResponse response = server.send(currentRegistration, new DiscoverRequest(3, 1, 0));

        // verify result
        assertThat(response) //
                .hasCode(NOT_FOUND) //
                .hasValidUnderlyingResponseFor(givenServerEndpointProvider);
    }

    @TestAllTransportLayer
    public void cant_discover_resource_of_non_existent_instance_and_resource(Protocol givenProtocol,
            String givenClientEndpointProvider, String givenServerEndpointProvider) throws InterruptedException {
        // read ACL object
        DiscoverResponse response = server.send(currentRegistration, new DiscoverRequest(3, 1, 20));

        // verify result
        assertThat(response) //
                .hasCode(NOT_FOUND) //
                .hasValidUnderlyingResponseFor(givenServerEndpointProvider);
    }

    @TestAllTransportLayer
    public void cant_discover_resource_of_non_existent_resource(Protocol givenProtocol,
            String givenClientEndpointProvider, String givenServerEndpointProvider) throws InterruptedException {
        // read ACL object
        DiscoverResponse response = server.send(currentRegistration, new DiscoverRequest(3, 0, 42));

        // verify result
        assertThat(response) //
                .hasCode(NOT_FOUND) //
                .hasValidUnderlyingResponseFor(givenServerEndpointProvider);
    }
}
