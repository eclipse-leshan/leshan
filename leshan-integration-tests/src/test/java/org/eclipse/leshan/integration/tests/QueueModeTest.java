/*******************************************************************************
 * Copyright (c) 2017 RISE SICS AB.
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
 *     RISE SICS AB - initial API and implementation
 *     Michał Wadowski (Orange) - Improved compliance with rfc6690
 *******************************************************************************/

package org.eclipse.leshan.integration.tests;

import static org.eclipse.leshan.integration.tests.util.LeshanTestClientBuilder.givenClientUsing;
import static org.eclipse.leshan.integration.tests.util.LeshanTestServerBuilder.givenServerUsing;
import static org.eclipse.leshan.integration.tests.util.assertion.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import org.eclipse.leshan.core.endpoint.Protocol;
import org.eclipse.leshan.core.link.LinkParseException;
import org.eclipse.leshan.core.request.ReadRequest;
import org.eclipse.leshan.core.response.ReadResponse;
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
public class QueueModeTest {

    private static final int AWAKETIME = 1000; // milliseconds

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

    @BeforeEach
    public void start(Protocol givenProtocol, String givenClientEndpointProvider, String givenServerEndpointProvider) {
        server = givenServerUsing(givenProtocol).with(givenServerEndpointProvider) //
                .withAwakeTime(AWAKETIME, TimeUnit.MILLISECONDS).build();
        server.start();
        client = givenClientUsing(givenProtocol).with(givenClientEndpointProvider) //
                .connectingTo(server)//
                .usingQueueMode().build();
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
    public void awake_sleeping_awake_sleeping(Protocol givenProtocol, String givenClientEndpointProvider,
            String givenServerEndpointProvider) throws LinkParseException {
        // Check client is not registered
        assertThat(client).isNotRegisteredAt(server);

        // Start it
        client.start();

        // Check that client is awake
        server.waitWakingOf(client);
        assertThat(client).isAwakeOn(server);

        // Check client is well registered
        assertThat(client).isRegisteredAt(server);
        Registration registration = server.getRegistrationFor(client);
        assertThat(registration.getObjectLinks()).isLikeLinks(
                "</>;rt=\"oma.lwm2m\";ct=\"60 110 112 1542 1543 11542 11543\",</1>;ver=1.1,</1/0>,</2>,</3>;ver=1.1,</3/0>,</3442/0>");

        // Wait for client awake time expiration (20% margin)
        assertThat(client).after((long) (AWAKETIME * 0.8), TimeUnit.MILLISECONDS).isAwakeOn(server);

        // Check that client is sleeping
        server.waitSleepingOf(client);

        // Trigger update manually for waking up
        client.waitForRegistrationTo(server);
        client.triggerRegistrationUpdate();

        // Check that client is awake
        server.waitWakingOf(client);
        assertThat(client).isAwakeOn(server);

        // Wait for client awake time expiration (20% margin)
        assertThat(client).after((long) (AWAKETIME * 0.8), TimeUnit.MILLISECONDS).isAwakeOn(server);

        // Check that client is sleeping
        server.waitSleepingOf(client);

        // Stop client with out de-registration
        client.stop(false);
    }

    @TestAllTransportLayer
    public void one_awake_notification(Protocol givenProtocol, String givenClientEndpointProvider,
            String givenServerEndpointProvider) {
        // Check client is not registered
        assertThat(client).isNotRegisteredAt(server);

        // Start it
        client.start();

        // Check that client is awake
        server.waitWakingOf(client);
        assertThat(client).isAwakeOn(server);

        // Check client is well registered
        assertThat(client).isRegisteredAt(server);

        // Triggers one update
        client.waitForRegistrationTo(server);
        client.triggerRegistrationUpdate();
        client.waitForUpdateTo(server, 1, TimeUnit.SECONDS);

        // Wait for client awake time expiration (20% margin)
        assertThat(client).after((long) (AWAKETIME * 0.8), TimeUnit.MILLISECONDS).isAwakeOn(server);

        // Check that client is sleeping
        server.waitSleepingOf(client);
        assertThat(client).isSleepingOn(server);

        // Trigger update manually for waking up
        client.triggerRegistrationUpdate();
        client.waitForUpdateTo(server, 1, TimeUnit.SECONDS);

        // Check that client is awake
        server.waitWakingOf(client);

        // Triggers two updates
        client.triggerRegistrationUpdate();
        client.waitForUpdateTo(server, 1, TimeUnit.SECONDS);
        client.triggerRegistrationUpdate();
        client.waitForUpdateTo(server, 1, TimeUnit.SECONDS);

        // Check only one notification
        server.ensureNoMoreAwakeSleepingEvent(AWAKETIME / 2, TimeUnit.MILLISECONDS);
    }

    @TestAllTransportLayer
    public void sleeping_if_timeout(Protocol givenProtocol, String givenClientEndpointProvider,
            String givenServerEndpointProvider) throws InterruptedException {
        // Check client is not registered
        assertThat(client).isNotRegisteredAt(server);

        // Start it
        client.start();

        // Check that client is awake
        server.waitWakingOf(client);
        assertThat(client).isAwakeOn(server);

        // Check client is well registered
        assertThat(client).isRegisteredAt(server);
        Registration registration = server.getRegistrationFor(client);

        // Stop the client to ensure that TimeOut exception is thrown
        client.stop(false);
        // Send a response with very short timeout
        ReadResponse response = server.send(registration, new ReadRequest(3, 0, 1), 1);

        // Check that a timeout occurs
        assertThat(response).isNull();

        // Check that the client is sleeping
        assertThat(client).isSleepingOn(server);
    }

    @TestAllTransportLayer
    public void correct_sending_when_awake(Protocol givenProtocol, String givenClientEndpointProvider,
            String givenServerEndpointProvider) throws InterruptedException {
        ReadResponse response;

        // Check client is not registered
        assertThat(client).isNotRegisteredAt(server);

        // Start it
        client.start();

        // Check that client is awake
        server.waitWakingOf(client);
        assertThat(client).isAwakeOn(server);

        // Check client is well registered
        assertThat(client).isRegisteredAt(server);
        Registration registration = server.getRegistrationFor(client);

        // Send a response a check that it is received correctly
        response = server.send(registration, new ReadRequest(3, 0, 1));
        assertThat(response).isNotNull();

        // Wait for client awake time expiration (20% margin)
        assertThat(client).after((long) (AWAKETIME * 0.8), TimeUnit.MILLISECONDS).isAwakeOn(server);

        // Check that client is sleeping
        server.waitSleepingOf(client);
        assertThat(client).isSleepingOn(server);

        // Trigger update manually for waking up
        client.waitForRegistrationTo(server);
        client.triggerRegistrationUpdate();
        client.waitForUpdateTo(server, 1, TimeUnit.SECONDS);

        // Check that client is awake
        server.waitWakingOf(client);
        assertThat(client).isAwakeOn(server);

        // Send request and check that it is received correctly
        response = server.send(registration, new ReadRequest(3, 0, 1));
        assertThat(response).isNotNull();
    }
}
