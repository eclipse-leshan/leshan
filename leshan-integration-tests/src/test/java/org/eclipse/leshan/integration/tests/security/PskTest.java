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
import static org.eclipse.leshan.integration.tests.util.Credentials.BAD_PSK_ID;
import static org.eclipse.leshan.integration.tests.util.Credentials.BAD_PSK_KEY;
import static org.eclipse.leshan.integration.tests.util.Credentials.GOOD_PSK_ID;
import static org.eclipse.leshan.integration.tests.util.Credentials.GOOD_PSK_KEY;
import static org.eclipse.leshan.integration.tests.util.LeshanTestClientBuilder.givenClientUsing;
import static org.eclipse.leshan.integration.tests.util.assertion.Assertions.assertArg;
import static org.eclipse.leshan.integration.tests.util.assertion.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import org.eclipse.leshan.core.endpoint.Protocol;
import org.eclipse.leshan.core.request.ReadRequest;
import org.eclipse.leshan.core.request.exception.TimeoutException;
import org.eclipse.leshan.core.request.exception.TimeoutException.Type;
import org.eclipse.leshan.core.request.exception.UnconnectedPeerException;
import org.eclipse.leshan.core.response.ErrorCallback;
import org.eclipse.leshan.core.response.ReadResponse;
import org.eclipse.leshan.core.response.ResponseCallback;
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
class PskTest {

    private static final long SHORT_LIFETIME = 2; // seconds

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
                arguments(Protocol.COAPS, "java-coap", "Californium"), //
                arguments(Protocol.COAPS, "Californium", "java-coap"), //
                arguments(Protocol.COAPS, "java-coap", "java-coap"));
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
    public void registered_device_with_psk_to_server_with_psk(Protocol givenProtocol,
            String givenClientEndpointProvider, String givenServerEndpointProvider)
            throws NonUniqueSecurityInfoException, InterruptedException {
        // Create PSK server & start it
        server = givenServer.build(); // default server support PSK
        server.start();

        // Create PSK Client
        client = givenClient.connectingTo(server).usingPsk(GOOD_PSK_ID, GOOD_PSK_KEY).build();

        // Add client credentials to the server
        server.getSecurityStore()
                .add(SecurityInfo.newPreSharedKeyInfo(client.getEndpointName(), GOOD_PSK_ID, GOOD_PSK_KEY));

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
    public void registered_device_with_psk_to_server_with_psk_without_endpointname(Protocol givenProtocol,
            String givenClientEndpointProvider, String givenServerEndpointProvider)
            throws NonUniqueSecurityInfoException, InterruptedException {

        // Create PSK server & start it
        server = givenServer.build(); // default server support PSK
        server.start();

        // Create PSK Client
        client = givenClient.connectingTo(server).named(GOOD_PSK_ID).dontSendEndpointName()
                .usingPsk(GOOD_PSK_ID, GOOD_PSK_KEY).build();

        // Add client credentials to the server
        server.getSecurityStore()
                .add(SecurityInfo.newPreSharedKeyInfo(client.getEndpointName(), GOOD_PSK_ID, GOOD_PSK_KEY));

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
    public void register_update_deregister_reregister_device_with_psk_to_server_with_psk(Protocol givenProtocol,
            String givenClientEndpointProvider, String givenServerEndpointProvider)
            throws NonUniqueSecurityInfoException {
        // Create PSK server & start it
        server = givenServer.build(); // default server support PSK
        server.start();

        // Create PSK Client
        client = givenClient.connectingTo(server) //
                .usingPsk(GOOD_PSK_ID, GOOD_PSK_KEY) //
                .usingLifeTimeOf(SHORT_LIFETIME, TimeUnit.SECONDS).build();

        // Add client credentials to the server
        server.getSecurityStore()
                .add(SecurityInfo.newPreSharedKeyInfo(client.getEndpointName(), GOOD_PSK_ID, GOOD_PSK_KEY));

        // Check client is not registered
        assertThat(client).isNotRegisteredAt(server);

        // Start it.
        client.start();

        // Check for register
        server.waitForNewRegistrationOf(client);
        client.waitForRegistrationTo(server);
        assertThat(client).isRegisteredAt(server);
        Registration registration = server.getRegistrationFor(client);

        // Check for update
        client.waitForUpdateTo(server, SHORT_LIFETIME, TimeUnit.SECONDS);
        server.waitForUpdateOf(registration);
        assertThat(client).isRegisteredAt(server);

        // Check de-registration
        client.stop(true);
        server.waitForDeregistrationOf(registration);
        client.waitForDeregistrationTo(server);
        assertThat(client).isNotRegisteredAt(server);

        // check new registration
        client.start();
        server.waitForNewRegistrationOf(client);
        client.waitForRegistrationTo(server);
        assertThat(client).isRegisteredAt(server);
    }

    @TestAllTransportLayer
    public void register_update_reregister_device_with_psk_to_server_with_psk(Protocol givenProtocol,
            String givenClientEndpointProvider, String givenServerEndpointProvider)
            throws NonUniqueSecurityInfoException {
        // Create PSK server & start it
        server = givenServer.build(); // default server support PSK
        server.start();

        // Create PSK Client
        client = givenClient.connectingTo(server) //
                .usingPsk(GOOD_PSK_ID, GOOD_PSK_KEY) //
                .usingLifeTimeOf(SHORT_LIFETIME, TimeUnit.SECONDS).build();

        // Add client credentials to the server
        server.getSecurityStore()
                .add(SecurityInfo.newPreSharedKeyInfo(client.getEndpointName(), GOOD_PSK_ID, GOOD_PSK_KEY));

        // Check client is not registered
        assertThat(client).isNotRegisteredAt(server);

        // Start it.
        client.start();

        // Check for register
        server.waitForNewRegistrationOf(client);
        client.waitForRegistrationTo(server);
        assertThat(client).isRegisteredAt(server);
        Registration registration = server.getRegistrationFor(client);

        // Check for update
        client.waitForUpdateTo(server, SHORT_LIFETIME, TimeUnit.SECONDS);
        server.waitForUpdateOf(registration);
        assertThat(client).isRegisteredAt(server);

        // Check de-registration
        client.stop(false);
        assertThat(client).after(500, TimeUnit.MILLISECONDS).isRegisteredAt(server);

        // check new registration
        client.start();
        server.waitForReRegistrationOf(registration);
        client.waitForRegistrationTo(server);
        assertThat(client).isRegisteredAt(server);
    }

    @TestAllTransportLayer
    public void server_initiates_dtls_handshake(Protocol givenProtocol, String givenClientEndpointProvider,
            String givenServerEndpointProvider) throws NonUniqueSecurityInfoException, InterruptedException {

        // java-coap use bouncy castle which doesn't support server initiated (for now?)
        assumeTrue(!givenClientEndpointProvider.equals("java-coap") //
                && !givenServerEndpointProvider.equals("java-coap"));

        // Create PSK server & start it
        server = givenServer.build(); // default server support PSK
        server.start();

        // Create PSK Client
        client = givenClient.connectingTo(server).usingPsk(GOOD_PSK_ID, GOOD_PSK_KEY).build();

        // Add client credentials to the server
        server.getSecurityStore()
                .add(SecurityInfo.newPreSharedKeyInfo(client.getEndpointName(), GOOD_PSK_ID, GOOD_PSK_KEY));

        // Check client is not registered
        assertThat(client).isNotRegisteredAt(server);

        // Start it.
        client.start();

        // Check for register
        server.waitForNewRegistrationOf(client);
        client.waitForRegistrationTo(server);
        assertThat(client).isRegisteredAt(server);
        Registration registration = server.getRegistrationFor(client);

        // Remove Client Security info
        server.clearSecurityContextFor(givenProtocol);

        // try to send request
        ReadResponse readResponse = server.send(registration, new ReadRequest(3), 1000);
        assertThat(readResponse.isSuccess()).isTrue();
    }

    @TestAllTransportLayer
    public void server_initiates_dtls_handshake_timeout(Protocol givenProtocol, String givenClientEndpointProvider,
            String givenServerEndpointProvider) throws NonUniqueSecurityInfoException {

        // java-coap use bouncy castle which doesn't support server initiated (for now?)
        assumeTrue(!givenClientEndpointProvider.equals("java-coap") //
                && !givenServerEndpointProvider.equals("java-coap"));

        // Create PSK server & start it
        server = givenServer.build(); // default server support PSK
        server.start();

        // Create PSK Client
        client = givenClient.connectingTo(server).usingPsk(GOOD_PSK_ID, GOOD_PSK_KEY).build();

        // Add client credentials to the server
        server.getSecurityStore()
                .add(SecurityInfo.newPreSharedKeyInfo(client.getEndpointName(), GOOD_PSK_ID, GOOD_PSK_KEY));

        // Check client is not registered
        assertThat(client).isNotRegisteredAt(server);

        // Start it.
        client.start();

        // Check for register
        server.waitForNewRegistrationOf(client);
        client.waitForRegistrationTo(server);
        assertThat(client).isRegisteredAt(server);
        Registration registration = server.getRegistrationFor(client);

        // Remove Client Security info
        server.clearSecurityContextFor(givenProtocol);

        // stop client
        client.stop(false);

        // try to send request asynchronously, it should fail with security layer timeout
        @SuppressWarnings("unchecked")
        ResponseCallback<ReadResponse> responseCallback = mock(ResponseCallback.class);
        ErrorCallback errorCallback = mock(ErrorCallback.class);
        server.send(registration, new ReadRequest(3), 1000, responseCallback, errorCallback);

        verify(errorCallback, timeout(1100).times(1)) //
                .onError(assertArg(e -> {
                    assertThat(e).isInstanceOfSatisfying(TimeoutException.class, timeout -> {
                        assertThat(timeout.getType()).isEqualTo(Type.DTLS_HANDSHAKE_TIMEOUT);
                    });
                }));
        verify(responseCallback, never()).onResponse(any());
    }

    @TestAllTransportLayer
    public void server_does_not_initiate_dtls_handshake_with_queue_mode(Protocol givenProtocol,
            String givenClientEndpointProvider, String givenServerEndpointProvider)
            throws NonUniqueSecurityInfoException {

        // Create PSK server & start it
        server = givenServer.build(); // default server support PSK
        server.start();

        // Create PSK Client
        client = givenClient.connectingTo(server) //
                .usingPsk(GOOD_PSK_ID, GOOD_PSK_KEY) //
                .usingQueueMode() //
                .build();

        // Add client credentials to the server
        server.getSecurityStore()
                .add(SecurityInfo.newPreSharedKeyInfo(client.getEndpointName(), GOOD_PSK_ID, GOOD_PSK_KEY));

        // Check client is not registered
        assertThat(client).isNotRegisteredAt(server);

        // Start it.
        client.start();

        // Check for register
        server.waitForNewRegistrationOf(client);
        client.waitForRegistrationTo(server);
        assertThat(client).isRegisteredAt(server);
        Registration registration = server.getRegistrationFor(client);

        // Remove Client Security info
        server.clearSecurityContextFor(givenProtocol);

        // try to send request
        assertThrowsExactly(UnconnectedPeerException.class, () -> {
            server.send(registration, new ReadRequest(3), 1000);
        });
        assertThat(client).isSleepingOn(server);
    }

    @TestAllTransportLayer
    public void registered_device_with_bad_psk_identity_to_server_with_psk(Protocol givenProtocol,
            String givenClientEndpointProvider, String givenServerEndpointProvider)
            throws NonUniqueSecurityInfoException {
        // Create PSK server & start it
        server = givenServer.build(); // default server support PSK
        server.start();

        // Create PSK Client
        client = givenClient.connectingTo(server).usingPsk(GOOD_PSK_ID, GOOD_PSK_KEY).build();

        // Add client credentials with BAD PSK ID to the server
        server.getSecurityStore()
                .add(SecurityInfo.newPreSharedKeyInfo(client.getEndpointName(), BAD_PSK_ID, GOOD_PSK_KEY));

        // Check client is not registered
        assertThat(client).isNotRegisteredAt(server);

        // Start it.
        client.start();

        // Check client can not register
        assertThat(client).after(1, TimeUnit.SECONDS).isNotRegisteredAt(server);
    }

    @TestAllTransportLayer
    public void registered_device_with_bad_psk_key_to_server_with_psk(Protocol givenProtocol,
            String givenClientEndpointProvider, String givenServerEndpointProvider)
            throws NonUniqueSecurityInfoException {
        // Create PSK server & start it
        server = givenServer.build(); // default server support PSK
        server.start();

        // Create PSK Client
        client = givenClient.connectingTo(server).usingPsk(GOOD_PSK_ID, GOOD_PSK_KEY).build();

        // Add client credentials with BAD PSK KEY to the server
        server.getSecurityStore()
                .add(SecurityInfo.newPreSharedKeyInfo(client.getEndpointName(), BAD_PSK_ID, BAD_PSK_KEY));

        // Check client is not registered
        assertThat(client).isNotRegisteredAt(server);

        // Start it.
        client.start();

        // Check client can not register
        assertThat(client).after(1, TimeUnit.SECONDS).isNotRegisteredAt(server);
    }

    @TestAllTransportLayer
    public void registered_device_with_psk_and_bad_endpoint_to_server_with_psk(Protocol givenProtocol,
            String givenClientEndpointProvider, String givenServerEndpointProvider)
            throws NonUniqueSecurityInfoException {

        // Create PSK server & start it
        server = givenServer.build(); // default server support PSK
        server.start();

        // Create PSK Client
        client = givenClient.connectingTo(server).usingPsk(GOOD_PSK_ID, GOOD_PSK_KEY).build();

        // Add client credentials for another endpoint to the server
        server.getSecurityStore().add(SecurityInfo.newPreSharedKeyInfo(BAD_ENDPOINT, GOOD_PSK_ID, GOOD_PSK_KEY));

        // Check client is not registered
        assertThat(client).isNotRegisteredAt(server);

        // Start it.
        client.start();

        // Check client can not register
        assertThat(client).after(1, TimeUnit.SECONDS).isNotRegisteredAt(server);
    }

    @TestAllTransportLayer
    public void registered_device_with_psk_identity_to_server_with_psk_then_remove_security_info(Protocol givenProtocol,
            String givenClientEndpointProvider, String givenServerEndpointProvider)
            throws NonUniqueSecurityInfoException {
        // Create PSK server & start it
        server = givenServer.build(); // default server support PSK
        server.start();

        // Create PSK Client
        client = givenClient.connectingTo(server).usingPsk(GOOD_PSK_ID, GOOD_PSK_KEY).build();

        // Add client credentials to the server
        server.getSecurityStore()
                .add(SecurityInfo.newPreSharedKeyInfo(client.getEndpointName(), GOOD_PSK_ID, GOOD_PSK_KEY));

        // Check client is not registered
        assertThat(client).isNotRegisteredAt(server);

        // Start it.
        client.start();
        client.waitForRegistrationTo(server);

        // Check client is well registered
        assertThat(client).isRegisteredAt(server);

        // remove compromised credentials
        boolean credentialsCompromised = true;
        server.getSecurityStore().remove(client.getEndpointName(), credentialsCompromised);

        // try to update
        client.triggerRegistrationUpdate();
        client.waitForUpdateTimeoutTo(server);
    }
}
