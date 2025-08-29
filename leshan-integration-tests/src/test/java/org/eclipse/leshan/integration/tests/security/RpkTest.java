/*******************************************************************************
 * Copyright (c) 2024 Sierra Wireless and others.
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
package org.eclipse.leshan.integration.tests.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.leshan.integration.tests.util.Credentials.BAD_ENDPOINT;
import static org.eclipse.leshan.integration.tests.util.Credentials.clientPrivateKey;
import static org.eclipse.leshan.integration.tests.util.Credentials.clientPublicKey;
import static org.eclipse.leshan.integration.tests.util.Credentials.serverPrivateKey;
import static org.eclipse.leshan.integration.tests.util.Credentials.serverPublicKey;
import static org.eclipse.leshan.integration.tests.util.LeshanTestClientBuilder.givenClientUsing;
import static org.eclipse.leshan.integration.tests.util.assertion.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.security.PublicKey;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import org.eclipse.leshan.core.ResponseCode;
import org.eclipse.leshan.core.endpoint.Protocol;
import org.eclipse.leshan.core.request.ReadRequest;
import org.eclipse.leshan.core.response.ReadResponse;
import org.eclipse.leshan.integration.tests.util.Failure;
import org.eclipse.leshan.integration.tests.util.LeshanTestClient;
import org.eclipse.leshan.integration.tests.util.LeshanTestClientBuilder;
import org.eclipse.leshan.integration.tests.util.LeshanTestServer;
import org.eclipse.leshan.integration.tests.util.LeshanTestServerBuilder;
import org.eclipse.leshan.integration.tests.util.junit5.extensions.BeforeEachParameterizedResolver;
import org.eclipse.leshan.server.registration.Registration;
import org.eclipse.leshan.servers.security.InMemorySecurityStore;
import org.eclipse.leshan.servers.security.NonUniqueSecurityInfoException;
import org.eclipse.leshan.servers.security.SecurityInfo;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

@ExtendWith(BeforeEachParameterizedResolver.class)
class RpkTest {

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
                arguments(Protocol.COAPS, "Californium", "Californium"), //
                arguments(Protocol.COAPS, "java-coap", "Californium"));
    }

    /*---------------------------------/
     *  Set-up and Tear-down Tests
     * -------------------------------*/
    LeshanTestServerBuilder givenServer;
    LeshanTestServer server;
    LeshanTestClientBuilder givenClient;
    LeshanTestClient client;

    @BeforeEach
    void start(Protocol givenProtocol, String givenClientEndpointProvider, String givenServerEndpointProvider) {
        givenServer = givenServerUsing(givenProtocol).with(givenServerEndpointProvider);
        givenClient = givenClientUsing(givenProtocol).with(givenClientEndpointProvider);
    }

    @AfterEach
    void stop() {
        if (client != null)
            client.destroy(false);
        if (server != null)
            server.destroy();
    }

    protected LeshanTestServerBuilder givenServerUsing(Protocol givenProtocol) {
        return new LeshanTestServerBuilder(givenProtocol).with(new InMemorySecurityStore());
    }

    /*---------------------------------/
     *  Tests
     * -------------------------------*/
    @TestAllTransportLayer
    public void registered_device_with_rpk_to_server_with_rpk(Protocol givenProtocol,
            String givenClientEndpointProvider, String givenServerEndpointProvider)
            throws NonUniqueSecurityInfoException, InterruptedException {
        // Create RPK server & start it
        server = givenServer.using(serverPublicKey, serverPrivateKey).build();
        server.start();

        // Create RPK Client
        client = givenClient.connectingTo(server) //
                .using(clientPublicKey, clientPrivateKey)//
                .trusting(serverPublicKey).build();

        // Add client credentials to the server
        server.getSecurityStore().add(SecurityInfo.newRawPublicKeyInfo(client.getEndpointName(), clientPublicKey));

        // Check client is not registered
        assertThat(client).isNotRegisteredAt(server);

        // Start it and wait for registration
        client.start();
        server.waitForNewRegistrationOf(client);

        // Check client is well registered
        assertThat(client).isRegisteredAt(server);
        Registration registration = server.getRegistrationFor(client);

        // check we can send request to client.
        ReadResponse response = server.send(registration, new ReadRequest(3, 0, 1), 500);
        assertThat(response.isSuccess()).isTrue();
    }

    @TestAllTransportLayer
    public void registered_device_with_rpk_to_server_with_rpk_without_endpointname(Protocol givenProtocol,
            String givenClientEndpointProvider, String givenServerEndpointProvider)
            throws NonUniqueSecurityInfoException {
        // Create RPK server & start it
        server = givenServer.using(serverPublicKey, serverPrivateKey).build();
        server.start();

        // Create RPK Client
        client = givenClient.connectingTo(server) //
                .using(clientPublicKey, clientPrivateKey)//
                .trusting(serverPublicKey)//
                .dontSendEndpointName() //
                .build();

        // Add client credentials to the server
        server.getSecurityStore().add(SecurityInfo.newRawPublicKeyInfo(client.getEndpointName(), clientPublicKey));

        // Check client is not registered
        assertThat(client).isNotRegisteredAt(server);

        // Start it and wait for registration
        client.start();
        Failure failure = client.waitForRegistrationFailureTo(server);
        assertThat(failure).failedWith(ResponseCode.FORBIDDEN);

        // Check we are registered with the expected attributes
        assertThat(client).isNotRegisteredAt(server);
    }

    @TestAllTransportLayer
    public void registered_device_with_bad_rpk_to_server_with_rpk(Protocol givenProtocol,
            String givenClientEndpointProvider, String givenServerEndpointProvider)
            throws NonUniqueSecurityInfoException {
        // Create RPK server & start it
        server = givenServer.using(serverPublicKey, serverPrivateKey).build();
        server.start();

        // Create RPK Client
        client = givenClient.connectingTo(server) //
                .using(clientPublicKey, clientPrivateKey)//
                .trusting(serverPublicKey).build();

        // We use the server public key as bad client public key
        PublicKey badClientPublicKey = serverPublicKey;
        server.getSecurityStore().add(SecurityInfo.newRawPublicKeyInfo(client.getEndpointName(), badClientPublicKey));

        // Check client is not registered
        assertThat(client).isNotRegisteredAt(server);

        // Start it and wait for registration
        client.start();
        assertThat(client).after(1, TimeUnit.SECONDS).isNotRegisteredAt(server);
    }

    @TestAllTransportLayer
    public void registered_device_with_rpk_to_server_with_rpk_then_remove_security_info(Protocol givenProtocol,
            String givenClientEndpointProvider, String givenServerEndpointProvider)
            throws NonUniqueSecurityInfoException {
        // Create RPK server & start it
        server = givenServer.using(serverPublicKey, serverPrivateKey).build();
        server.start();

        // Create RPK Client
        client = givenClient.connectingTo(server) //
                .using(clientPublicKey, clientPrivateKey)//
                .trusting(serverPublicKey).build();

        // Add client credentials to the server
        server.getSecurityStore().add(SecurityInfo.newRawPublicKeyInfo(client.getEndpointName(), clientPublicKey));

        // Check client is not registered
        assertThat(client).isNotRegisteredAt(server);

        // Start it and wait for registration
        client.start();
        server.waitForNewRegistrationOf(client);

        // Check client is well registered
        assertThat(client).isRegisteredAt(server);

        // remove compromised credentials
        server.getSecurityStore().remove(client.getEndpointName(), true);

        // try to update
        client.triggerRegistrationUpdate();
        client.waitForUpdateTimeoutTo(server);
    }

    @TestAllTransportLayer
    public void registered_device_with_rpk_and_bad_endpoint_to_server_with_rpk(Protocol givenProtocol,
            String givenClientEndpointProvider, String givenServerEndpointProvider)
            throws NonUniqueSecurityInfoException {

        // Create RPK server & start it
        server = givenServer.using(serverPublicKey, serverPrivateKey).build();
        server.start();

        // Create RPK Client
        client = givenClient.connectingTo(server) //
                .using(clientPublicKey, clientPrivateKey)//
                .trusting(serverPublicKey).build();

        // We use the server public key as bad client public key
        server.getSecurityStore().add(SecurityInfo.newRawPublicKeyInfo(BAD_ENDPOINT, clientPublicKey));

        // Check client is not registered
        assertThat(client).isNotRegisteredAt(server);

        // Start it and wait for registration
        client.start();
        assertThat(client).after(1, TimeUnit.SECONDS).isNotRegisteredAt(server);
    }

}
