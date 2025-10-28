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
package org.eclipse.leshan.integration.tests;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.leshan.core.ResponseCode.CONTENT;
import static org.eclipse.leshan.integration.tests.util.LeshanProxyBuilder.givenReverseProxyFor;
import static org.eclipse.leshan.integration.tests.util.LeshanTestClientBuilder.givenClientUsing;
import static org.eclipse.leshan.integration.tests.util.LeshanTestServerBuilder.givenServerUsing;
import static org.eclipse.leshan.integration.tests.util.assertion.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Arrays;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import org.eclipse.leshan.client.servers.LwM2mServer;
import org.eclipse.leshan.core.ResponseCode;
import org.eclipse.leshan.core.endpoint.Protocol;
import org.eclipse.leshan.core.link.LinkParseException;
import org.eclipse.leshan.core.node.LwM2mSingleResource;
import org.eclipse.leshan.core.observation.Observation;
import org.eclipse.leshan.core.observation.SingleObservation;
import org.eclipse.leshan.core.request.ContentFormat;
import org.eclipse.leshan.core.request.ObserveRequest;
import org.eclipse.leshan.core.request.WriteRequest;
import org.eclipse.leshan.core.response.ObserveResponse;
import org.eclipse.leshan.core.response.SendResponse;
import org.eclipse.leshan.integration.tests.util.Failure;
import org.eclipse.leshan.integration.tests.util.LeshanTestClient;
import org.eclipse.leshan.integration.tests.util.LeshanTestServer;
import org.eclipse.leshan.integration.tests.util.LeshanTestServerBuilder;
import org.eclipse.leshan.integration.tests.util.ReverseProxy;
import org.eclipse.leshan.server.registration.IRegistration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

public class MultiEndpointsTest {

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
    LeshanTestClient client;
    ReverseProxy proxy;

    public void setupTestFor(LeshanTestServerBuilder serverBuilder, Protocol givenProtocol,
            String givenClientEndpointProvider) {
        // create and start server
        server = serverBuilder.build();
        server.start();

        // create and start proxy
        proxy = givenReverseProxyFor(server, givenProtocol);
        proxy.start();

        /// create and start client
        client = givenClientUsing(givenProtocol).with(givenClientEndpointProvider).connectingTo(server).behind(proxy)
                .build();
    }

    public LeshanTestServerBuilder givenServerWithTwoEndpoint(Protocol givenProtocol,
            String givenServerEndpointProvider) {
        return givenServerUsing(givenProtocol).with(givenServerEndpointProvider, givenServerEndpointProvider);
    }

    @AfterEach
    public void stop() throws InterruptedException {
        if (client != null)
            client.destroy(false);
        if (proxy != null)
            proxy.stop();
        if (server != null)
            server.destroy();
    }

    /*---------------------------------/
     *  Tests
     * -------------------------------*/

    @TestAllTransportLayer
    public void register_then_update_on_different_endpoint(Protocol givenProtocol, String givenClientEndpointProvider,
            String givenServerEndpointProvider) throws LinkParseException {

        // set-up test
        setupTestFor(//
                givenServerWithTwoEndpoint(givenProtocol, givenServerEndpointProvider), //
                givenProtocol, //
                givenClientEndpointProvider);

        // Start it and wait for registration
        client.start();
        server.waitForNewRegistrationOf(client);
        client.waitForRegistrationTo(server);

        // Check client is well registered
        assertThat(client).isRegisteredAt(server);
        IRegistration registration = server.getRegistrationFor(client);

        // Check for update
        client.triggerRegistrationUpdate();
        client.waitForUpdateTo(server);
        server.waitForUpdateOf(registration);
        assertThat(client).isRegisteredAt(server);

        // Send request from client to another server endpoint.
        proxy.useNextServerAddress();

        // Check update failed
        client.triggerRegistrationUpdate();
        Failure failure = client.waitForUpdateFailureTo(server);
        assertThat(failure).failedWith(ResponseCode.BAD_REQUEST);
    }

    @TestAllTransportLayer
    public void register_then_deregister_on_different_endpoint(Protocol givenProtocol,
            String givenClientEndpointProvider, String givenServerEndpointProvider) throws LinkParseException {
        // set-up test
        setupTestFor(//
                givenServerWithTwoEndpoint(givenProtocol, givenServerEndpointProvider), //
                givenProtocol, //
                givenClientEndpointProvider);

        // Start it and wait for registration
        client.start();
        server.waitForNewRegistrationOf(client);
        client.waitForRegistrationTo(server);

        // Check client is well registered
        assertThat(client).isRegisteredAt(server);
        IRegistration registration = server.getRegistrationFor(client);

        // Check for update
        client.triggerRegistrationUpdate();
        client.waitForUpdateTo(server);
        server.waitForUpdateOf(registration);
        assertThat(client).isRegisteredAt(server);

        // Send request from client to another server endpoint.
        proxy.useNextServerAddress();

        // Deregister
        client.stop(true);
        Failure failure = client.waitForDeregistrationFailureTo(server);
        assertThat(failure).failedWith(ResponseCode.BAD_REQUEST);
    }

    @TestAllTransportLayer
    public void register_then_send_on_different_endpoint(Protocol givenProtocol, String givenClientEndpointProvider,
            String givenServerEndpointProvider) throws LinkParseException, InterruptedException {

        register_then_send_on_different_endpoint(//
                givenServerWithTwoEndpoint(givenProtocol, givenServerEndpointProvider), //
                givenProtocol, //
                givenClientEndpointProvider);
    }

    @TestAllTransportLayer
    public void register_then_send_on_different_endpoint_with_update_on_send(Protocol givenProtocol,
            String givenClientEndpointProvider, String givenServerEndpointProvider)
            throws LinkParseException, InterruptedException {

        register_then_send_on_different_endpoint(//
                givenServerWithTwoEndpoint(givenProtocol, givenServerEndpointProvider).withUpdateOnSendOperation(), //
                givenProtocol, //
                givenClientEndpointProvider);
    }

    protected void register_then_send_on_different_endpoint(LeshanTestServerBuilder givenServer, Protocol givenProtocol,
            String givenClientEndpointProvider) throws LinkParseException, InterruptedException {

        // set-up test
        setupTestFor(//
                givenServer, //
                givenProtocol, //
                givenClientEndpointProvider);

        // Start it and wait for registration
        client.start();
        server.waitForNewRegistrationOf(client);
        client.waitForRegistrationTo(server);

        // Check client is well registered
        assertThat(client).isRegisteredAt(server);
        IRegistration registration = server.getRegistrationFor(client);

        // Check for update
        client.triggerRegistrationUpdate();
        client.waitForUpdateTo(server);
        server.waitForUpdateOf(registration);
        assertThat(client).isRegisteredAt(server);

        // Send request from client to another server endpoint.
        proxy.useNextServerAddress();

        // Send Data
        IRegistration registrationBeforeSend = server.getRegistrationFor(client);
        LwM2mServer registeredServer = client.getRegisteredServers().values().iterator().next();
        SendResponse response = client.getSendService().sendData(registeredServer, ContentFormat.SENML_JSON,
                Arrays.asList("/3/0/1", "/3/0/2"), 1000);
        assertThat(response).hasCode(ResponseCode.BAD_REQUEST);
        IRegistration registrationAfterSend = server.getRegistrationFor(client);
        assertThat(registrationAfterSend).isEqualTo(registrationBeforeSend);
    }

    @TestAllTransportLayer
    public void observe_then_send_notification_on_different_endpoint(Protocol givenProtocol,
            String givenClientEndpointProvider, String givenServerEndpointProvider)
            throws LinkParseException, InterruptedException {

        observe_then_send_notification_on_different_endpoint(//
                givenServerWithTwoEndpoint(givenProtocol, givenServerEndpointProvider), //
                givenProtocol, //
                givenClientEndpointProvider);
    }

    @TestAllTransportLayer
    public void observe_then_send_notification_on_different_endpoint_with_update_on_notification(Protocol givenProtocol,
            String givenClientEndpointProvider, String givenServerEndpointProvider)
            throws LinkParseException, InterruptedException {

        observe_then_send_notification_on_different_endpoint(//
                givenServerWithTwoEndpoint(givenProtocol, givenServerEndpointProvider).withUpdateOnNotification(), //
                givenProtocol, //
                givenClientEndpointProvider);
    }

    protected void observe_then_send_notification_on_different_endpoint(LeshanTestServerBuilder givenServer,
            Protocol givenProtocol, String givenClientEndpointProvider)
            throws LinkParseException, InterruptedException {

        // set-up test
        setupTestFor(//
                givenServer, //
                givenProtocol, //
                givenClientEndpointProvider);

        // Start it and wait for registration
        client.start();
        server.waitForNewRegistrationOf(client);
        client.waitForRegistrationTo(server);

        // Check client is well registered
        assertThat(client).isRegisteredAt(server);
        IRegistration registration = server.getRegistrationFor(client);

        // observe device timezone
        ObserveResponse observeResponse = server.send(registration, new ObserveRequest(3, 0, 15));
        assertThat(observeResponse) //
                .hasCode(CONTENT);

        // an observation response should have been sent
        SingleObservation observation = observeResponse.getObservation();
        assertThat(observation.getPath()).asString().isEqualTo("/3/0/15");
        assertThat(observation.getRegistrationId()).isEqualTo(registration.getId());
        Set<Observation> observations = server.getObservationService().getObservations(registration);
        assertThat(observations).containsExactly(observation);

        // change value to trigger new notification
        client.getObjectTree().getObjectEnabler(3).write(LwM2mServer.SYSTEM,
                new WriteRequest(3, 0, 15, "Europe/London"));

        // verify result
        server.waitForNewObservation(observation);
        ObserveResponse response = server.waitForNotificationOf(observation);
        assertThat(response.getContent()).isEqualTo(LwM2mSingleResource.newStringResource(15, "Europe/London"));

        // Send request from client to another server endpoint.
        proxy.useNextServerAddress();

        // change value to trigger new notification
        IRegistration registrationBeforeNotification = server.getRegistrationFor(client);
        client.getObjectTree().getObjectEnabler(3).write(LwM2mServer.SYSTEM,
                new WriteRequest(3, 0, 15, "Europe/Paris"));
        server.ensureNoNotification(observation, 1, TimeUnit.SECONDS);
        IRegistration registrationAfterNotification = server.getRegistrationFor(client);
        assertThat(registrationAfterNotification).isEqualTo(registrationBeforeNotification);
    }
}
