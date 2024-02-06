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
 *     Achim Kraus (Bosch Software Innovations GmbH) - replace close() with destroy()
 *     Michał Wadowski (Orange) - Improved compliance with rfc6690
 *******************************************************************************/

package org.eclipse.leshan.integration.tests;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.leshan.integration.tests.util.LeshanTestClientBuilder.givenClientUsing;
import static org.eclipse.leshan.integration.tests.util.assertion.Assertions.assertArg;
import static org.eclipse.leshan.integration.tests.util.assertion.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import org.eclipse.leshan.core.endpoint.Protocol;
import org.eclipse.leshan.core.link.LinkParseException;
import org.eclipse.leshan.core.request.ReadRequest;
import org.eclipse.leshan.core.request.exception.RequestCanceledException;
import org.eclipse.leshan.core.response.ErrorCallback;
import org.eclipse.leshan.core.response.ReadResponse;
import org.eclipse.leshan.core.response.ResponseCallback;
import org.eclipse.leshan.integration.tests.util.LeshanTestClient;
import org.eclipse.leshan.integration.tests.util.LeshanTestClientBuilder;
import org.eclipse.leshan.integration.tests.util.LeshanTestServer;
import org.eclipse.leshan.integration.tests.util.LeshanTestServerBuilder;
import org.eclipse.leshan.integration.tests.util.junit5.extensions.BeforeEachParameterizedResolver;
import org.eclipse.leshan.server.registration.Registration;
import org.eclipse.leshan.server.security.NonUniqueSecurityInfoException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

@ExtendWith(BeforeEachParameterizedResolver.class)
public class RegistrationTest {

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
                arguments(Protocol.COAP, "Californium", "Californium"), //
                arguments(Protocol.COAP, "Californium", "java-coap"), //
                arguments(Protocol.COAP, "java-coap", "Californium"), //
                arguments(Protocol.COAP, "java-coap", "java-coap"));
    }

    /*---------------------------------/
     *  Set-up and Tear-down Tests
     * -------------------------------*/

    LeshanTestServer server;
    LeshanTestClientBuilder givenClient;
    LeshanTestClient client;

    @BeforeEach
    public void start(Protocol givenProtocol, String givenClientEndpointProvider, String givenServerEndpointProvider) {
        server = givenServerUsing(givenProtocol).with(givenServerEndpointProvider).build();
        server.start();
        givenClient = givenClientUsing(givenProtocol).with(givenClientEndpointProvider).connectingTo(server);
    }

    @AfterEach
    public void stop() throws InterruptedException {
        if (client != null)
            client.destroy(false);
        if (server != null)
            server.destroy();
    }

    protected LeshanTestServerBuilder givenServerUsing(Protocol givenProtocol) {
        return new LeshanTestServerBuilder(givenProtocol);
    }

    /*---------------------------------/
     *  Tests
     * -------------------------------*/
    @TestAllTransportLayer
    public void register_update_deregister(Protocol protocol, String clientEndpointProvider,
            String serverEndpointProvider) throws LinkParseException {

        // Check client is not registered
        client = givenClient.usingLifeTimeOf(SHORT_LIFETIME, TimeUnit.SECONDS).build();
        assertThat(client).isNotRegisteredAt(server);

        // Start it and wait for registration
        client.start();
        server.waitForNewRegistrationOf(client);
        client.waitForRegistrationTo(server);

        // Check client is well registered
        assertThat(client).isRegisteredAt(server);
        Registration registration = server.getRegistrationFor(client);
        assertThat(registration.getObjectLinks()).isLikeLinks(
                "</>;rt=\"oma.lwm2m\";ct=\"60 110 112 1542 1543 11542 11543\",</1/0>,</2>,</3/0>,</3442/0>");

        // Check for update
        client.waitForUpdateTo(server, SHORT_LIFETIME, TimeUnit.SECONDS);
        server.waitForUpdateOf(registration);
        assertThat(client).isRegisteredAt(server);

        // Check deregistration
        client.stop(true);
        server.waitForDeregistrationOf(registration);
        assertThat(client).isNotRegisteredAt(server);
    }

    @TestAllTransportLayer
    public void deregister_cancel_multiple_pending_request(Protocol protocol, String clientEndpointProvider,
            String serverEndpointProvider) throws InterruptedException, LinkParseException {
        // Check client is not registered
        client = givenClient.build();
        assertThat(client).isNotRegisteredAt(server);

        // Start it and wait for registration
        client.start();
        server.waitForNewRegistrationOf(client);
        client.waitForRegistrationTo(server);

        // Check client is well registered
        assertThat(client).isRegisteredAt(server);
        Registration registration = server.getRegistrationFor(client);

        // Stop client with out de-registration
        client.stop(false);

        // Send multiple reads which should be retransmitted.
        int numberOfRequests = 4;
        List<ResponseCallback<ReadResponse>> responseCallbacks = new ArrayList<>(numberOfRequests);
        List<ErrorCallback> errorCallbacks = new ArrayList<>(numberOfRequests);

        for (int index = 0; index < numberOfRequests; ++index) {
            // create mock and store it
            @SuppressWarnings("unchecked")
            ResponseCallback<ReadResponse> responseCallback = mock(ResponseCallback.class);
            ErrorCallback errorCallback = mock(ErrorCallback.class);
            responseCallbacks.add(responseCallback);
            errorCallbacks.add(errorCallback);

            // send request
            server.send(registration, new ReadRequest(3, 0, 1), responseCallback, errorCallback);
        }

        // Restart client (de-registration/re-registration)
        client.start();

        // Check the request was cancelled.
        for (int index = 0; index < numberOfRequests; ++index) {
            verify(errorCallbacks.get(index), timeout(1000).times(1)) //
                    .onError(assertArg(e -> assertThat(e).isExactlyInstanceOf(RequestCanceledException.class)));
        }
        // and we don't receive any response.
        for (int index = 0; index < numberOfRequests; ++index) {
            verify(responseCallbacks.get(index), never()).onResponse(any());
        }

    }

    @TestAllTransportLayer
    public void register_update_deregister_reregister(Protocol protocol, String clientEndpointProvider,
            String serverEndpointProvider) throws NonUniqueSecurityInfoException, InterruptedException {
        // Check client is not registered
        client = givenClient.usingLifeTimeOf(SHORT_LIFETIME, TimeUnit.SECONDS).build();
        assertThat(client).isNotRegisteredAt(server);

        // Start it and wait for registration
        client.start();
        server.waitForNewRegistrationOf(client);
        client.waitForRegistrationTo(server);

        // Check client is well registered
        assertThat(client).isRegisteredAt(server);
        Registration registration = server.getRegistrationFor(client);

        // Check for update
        client.waitForUpdateTo(server, SHORT_LIFETIME, TimeUnit.SECONDS);
        server.waitForUpdateOf(registration);
        assertThat(client).isRegisteredAt(server);

        // Check deregistration
        client.stop(true);
        server.waitForDeregistrationOf(registration);
        client.waitForDeregistrationTo(server);
        assertThat(client).isNotRegisteredAt(server);

        // Check new registration
        client.start();
        server.waitForNewRegistrationOf(client);
        client.waitForRegistrationTo(server);
        assertThat(client).isRegisteredAt(server);
    }

    @TestAllTransportLayer
    public void register_update_reregister(Protocol protocol, String clientEndpointProvider,
            String serverEndpointProvider) throws NonUniqueSecurityInfoException, InterruptedException {
        // Check client is not registered
        client = givenClient.usingLifeTimeOf(SHORT_LIFETIME, TimeUnit.SECONDS).build();
        assertThat(client).isNotRegisteredAt(server);

        // Start it and wait for registration
        client.start();
        server.waitForNewRegistrationOf(client);
        client.waitForRegistrationTo(server);

        // Check client is well registered
        assertThat(client).isRegisteredAt(server);
        Registration registration = server.getRegistrationFor(client);

        // Check for update
        client.waitForUpdateTo(server, SHORT_LIFETIME, TimeUnit.SECONDS);
        server.waitForUpdateOf(registration);
        assertThat(client).isRegisteredAt(server);

        // check stop do not de-register
        client.stop(false);
        server.ensureNoDeregistration();
        assertThat(client).isRegisteredAt(server);

        // check new registration
        Registration registrationUpdated = server.getRegistrationFor(client);
        client.start();
        // server.waitForReRegistrationOf(registration);
        server.waitForUpdateOf(registrationUpdated);
        assertThat(client).isRegisteredAt(server);
    }

    @TestAllTransportLayer
    public void register_with_additional_attributes(Protocol protocol, String clientEndpointProvider,
            String serverEndpointProvider) throws InterruptedException, LinkParseException {

        // Create client with additional attributes
        Map<String, String> expectedAdditionalAttributes = new HashMap<>();
        expectedAdditionalAttributes.put("key1", "value1");
        expectedAdditionalAttributes.put("imei", "2136872368");
        client = givenClient.withAdditiontalAttributes(expectedAdditionalAttributes).build();

        // Check client is not registered
        assertThat(client).isNotRegisteredAt(server);

        // Start it and wait for registration
        client.start();
        server.waitForNewRegistrationOf(client);
        client.waitForRegistrationTo(server);

        // Check we are registered with the expected attributes
        assertThat(client).isRegisteredAt(server);
        Registration registration = server.getRegistrationFor(client);
        assertThat(registration.getAdditionalRegistrationAttributes())
                .containsExactlyEntriesOf(expectedAdditionalAttributes);
    }
}
