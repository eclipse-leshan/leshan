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
import static org.eclipse.leshan.core.ResponseCode.CONTENT;
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

import java.io.IOException;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.net.InetSocketAddress;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import org.eclipse.californium.core.coap.CoAP.Code;
import org.eclipse.californium.core.coap.Request;
import org.eclipse.californium.core.coap.Response;
import org.eclipse.californium.core.config.CoapConfig;
import org.eclipse.californium.core.network.CoapEndpoint;
import org.eclipse.californium.elements.AddressEndpointContext;
import org.eclipse.californium.elements.config.Configuration;
import org.eclipse.californium.elements.config.SystemConfig;
import org.eclipse.californium.elements.config.UdpConfig;
import org.eclipse.leshan.core.endpoint.Protocol;
import org.eclipse.leshan.core.link.LinkParseException;
import org.eclipse.leshan.core.node.LwM2mPath;
import org.eclipse.leshan.core.observation.Observation;
import org.eclipse.leshan.core.observation.SingleObservation;
import org.eclipse.leshan.core.request.ContentFormat;
import org.eclipse.leshan.core.request.ObserveRequest;
import org.eclipse.leshan.core.request.ReadRequest;
import org.eclipse.leshan.core.request.exception.RequestCanceledException;
import org.eclipse.leshan.core.request.exception.SendFailedException;
import org.eclipse.leshan.core.response.ErrorCallback;
import org.eclipse.leshan.core.response.ObserveResponse;
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
                arguments(Protocol.COAP, "Californium", "Californium"));
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
                "</>;rt=\"oma.lwm2m\";ct=\"60 110 112 1542 1543 11542 11543\",</1>;ver=1.1,</1/0>,</2>,</3>;ver=1.1,</3/0>,</3442/0>");

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
        client.start();
        server.waitForReRegistrationOf(registration);
        assertThat(client).isRegisteredAt(server);
    }

    @TestAllTransportLayer
    public void register_observe_deregister_observe(Protocol protocol, String clientEndpointProvider,
            String serverEndpointProvider) throws NonUniqueSecurityInfoException, InterruptedException {
        // TODO java-coap does not raise expected SendFailedException at the end of this tests
        // But not sure what should be the right behavior.
        // Waiting for https://github.com/open-coap/java-coap/issues/36 before to move forward on this.
        assumeTrue(serverEndpointProvider.equals("Californium"));

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

        // observe device timezone
        ObserveResponse observeResponse = server.send(registration, new ObserveRequest(3, 0));
        assertThat(observeResponse) //
                .hasCode(CONTENT) //
                .hasValidUnderlyingResponseFor(serverEndpointProvider);

        // check observation registry is not null
        Set<Observation> observations = server.getObservationService().getObservations(registration);
        assertThat(observations) //
                .hasSize(1) //
                .first().isInstanceOfSatisfying(SingleObservation.class, obs -> {
                    assertThat(obs.getRegistrationId()).isEqualTo(registration.getId());
                    assertThat(obs.getPath()).isEqualTo(new LwM2mPath(3, 0));
                });

        // Check de-registration
        client.stop(true);
        server.waitForDeregistrationOf(registration, observations);
        assertThat(client).isNotRegisteredAt(server);
        client.waitForDeregistrationTo(server);
        observations = server.getObservationService().getObservations(registration);
        assertThat(observations).isEmpty();

        // try to send a new observation
        assertThrowsExactly(SendFailedException.class, () -> server.send(registration, new ObserveRequest(3, 0), 50));
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

    // TODO should maybe moved as it only tests server
    @TestAllTransportLayer
    public void register_with_invalid_request(Protocol protocol, String clientEndpointProvider,
            String serverEndpointProvider) throws InterruptedException, IOException {
        // Check client is not registered
        client = givenClient.build();
        assertThat(client).isNotRegisteredAt(server);

        // create a register request without the list of supported object
        Request coapRequest = new Request(Code.POST);
        URI destinationURI = server.getEndpoint(Protocol.COAP).getURI();
        coapRequest
                .setDestinationContext(new AddressEndpointContext(destinationURI.getHost(), destinationURI.getPort()));
        coapRequest.getOptions().setContentFormat(ContentFormat.LINK.getCode());
        coapRequest.getOptions().addUriPath("rd");
        coapRequest.getOptions().addUriQuery("ep=" + client.getEndpointName());

        // send request
        CoapEndpoint.Builder builder = new CoapEndpoint.Builder();
        builder.setConfiguration(
                new Configuration(CoapConfig.DEFINITIONS, UdpConfig.DEFINITIONS, SystemConfig.DEFINITIONS));
        builder.setInetSocketAddress(new InetSocketAddress(0));
        CoapEndpoint coapEndpoint = builder.build();
        coapEndpoint.start();
        coapEndpoint.sendRequest(coapRequest);

        // check response
        Response response = coapRequest.waitForResponse(1000);
        assertThat(response.getCode()).isEqualTo(org.eclipse.californium.core.coap.CoAP.ResponseCode.BAD_REQUEST);
        coapEndpoint.stop();
    }
}
