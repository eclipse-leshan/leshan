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
 *     Achim Kraus (Bosch Software Innovations GmbH) - use destination context
 *                                                     instead of address for response
 *******************************************************************************/

package org.eclipse.leshan.integration.tests.observe;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.leshan.core.ResponseCode.CHANGED;
import static org.eclipse.leshan.core.ResponseCode.CONTENT;
import static org.eclipse.leshan.integration.tests.util.LeshanTestClientBuilder.givenClientUsing;
import static org.eclipse.leshan.integration.tests.util.assertion.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import org.eclipse.californium.elements.Connector;
import org.eclipse.leshan.core.endpoint.Protocol;
import org.eclipse.leshan.core.node.LwM2mObject;
import org.eclipse.leshan.core.node.LwM2mObjectInstance;
import org.eclipse.leshan.core.node.LwM2mPath;
import org.eclipse.leshan.core.node.LwM2mResourceInstance;
import org.eclipse.leshan.core.node.LwM2mSingleResource;
import org.eclipse.leshan.core.node.codec.LwM2mValueChecker;
import org.eclipse.leshan.core.node.codec.json.LwM2mNodeJsonEncoder;
import org.eclipse.leshan.core.observation.Observation;
import org.eclipse.leshan.core.observation.SingleObservation;
import org.eclipse.leshan.core.request.CancelObservationRequest;
import org.eclipse.leshan.core.request.ContentFormat;
import org.eclipse.leshan.core.request.ObserveRequest;
import org.eclipse.leshan.core.request.ReadRequest;
import org.eclipse.leshan.core.request.WriteRequest;
import org.eclipse.leshan.core.request.WriteRequest.Mode;
import org.eclipse.leshan.core.response.CancelObservationResponse;
import org.eclipse.leshan.core.response.LwM2mResponse;
import org.eclipse.leshan.core.response.ObserveResponse;
import org.eclipse.leshan.core.response.ReadResponse;
import org.eclipse.leshan.core.util.TestLwM2mId;
import org.eclipse.leshan.integration.tests.util.LeshanTestClient;
import org.eclipse.leshan.integration.tests.util.LeshanTestServer;
import org.eclipse.leshan.integration.tests.util.LeshanTestServerBuilder;
import org.eclipse.leshan.integration.tests.util.junit5.extensions.BeforeEachParameterizedResolver;
import org.eclipse.leshan.server.registration.Registration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

@ExtendWith(BeforeEachParameterizedResolver.class)
public class ObserveTest {

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

    protected LeshanTestServerBuilder givenServerUsing(Protocol givenProtocol) {
        return new LeshanTestServerBuilder(givenProtocol);
    }

    /*---------------------------------/
     *  Tests
     * -------------------------------*/
    @TestAllTransportLayer
    public void can_observe_resource(Protocol givenProtocol, String givenClientEndpointProvider,
            String givenServerEndpointProvider) throws InterruptedException {

        // observe device timezone
        ObserveResponse observeResponse = server.send(currentRegistration, new ObserveRequest(3, 0, 15));
        assertThat(observeResponse) //
                .hasCode(CONTENT) //
                .hasValidUnderlyingResponseFor(givenServerEndpointProvider);

        // an observation response should have been sent
        SingleObservation observation = observeResponse.getObservation();
        assertThat(observation.getPath()).asString().isEqualTo("/3/0/15");
        assertThat(observation.getRegistrationId()).isEqualTo(currentRegistration.getId());
        Set<Observation> observations = server.getObservationService().getObservations(currentRegistration);
        assertThat(observations).containsExactly(observation);

        // write device timezone
        LwM2mResponse writeResponse = server.send(currentRegistration, new WriteRequest(3, 0, 15, "Europe/Paris"));
        assertThat(writeResponse).hasCode(CHANGED);

        // verify result
        server.waitForNewObservation(observation);
        ObserveResponse response = server.waitForNotificationOf(observation);
        assertThat(response.getContent()).isEqualTo(LwM2mSingleResource.newStringResource(15, "Europe/Paris"));
        assertThat(response).hasValidUnderlyingResponseFor(givenServerEndpointProvider);
    }

    @TestAllTransportLayer
    public void can_observe_resource_instance(Protocol givenProtocol, String givenClientEndpointProvider,
            String givenServerEndpointProvider) throws InterruptedException {
        // multi instance string
        String expectedPath = "/" + TestLwM2mId.TEST_OBJECT + "/0/" + TestLwM2mId.MULTIPLE_STRING_VALUE + "/0";
        ObserveResponse observeResponse = server.send(currentRegistration, new ObserveRequest(expectedPath));
        assertThat(observeResponse) //
                .hasCode(CONTENT) //
                .hasValidUnderlyingResponseFor(givenServerEndpointProvider);

        // an observation response should have been sent
        SingleObservation observation = observeResponse.getObservation();
        assertThat(observation.getPath()).asString().isEqualTo(expectedPath);
        assertThat(observation.getRegistrationId()).isEqualTo(currentRegistration.getId());
        Set<Observation> observations = server.getObservationService().getObservations(currentRegistration);
        assertThat(observations).containsExactly(observation);

        // write a new value
        LwM2mResponse writeResponse = server.send(currentRegistration, new WriteRequest(Mode.REPLACE, ContentFormat.TLV,
                expectedPath, LwM2mResourceInstance.newStringInstance(0, "a new string")));
        assertThat(writeResponse).hasCode(CHANGED);

        // verify result
        server.waitForNewObservation(observation);
        ObserveResponse response = server.waitForNotificationOf(observation);
        assertThat(response.getContent()).isEqualTo(LwM2mResourceInstance.newStringInstance(0, "a new string"));
        assertThat(response).hasValidUnderlyingResponseFor(givenServerEndpointProvider);
    }

    @TestAllTransportLayer
    public void can_observe_resource_instance_then_passive_cancel(Protocol givenProtocol,
            String givenClientEndpointProvider, String givenServerEndpointProvider) throws InterruptedException {
        // multi instance string
        String expectedPath = "/" + TestLwM2mId.TEST_OBJECT + "/0/" + TestLwM2mId.MULTIPLE_STRING_VALUE + "/0";
        ObserveResponse observeResponse = server.send(currentRegistration, new ObserveRequest(expectedPath));
        assertThat(observeResponse) //
                .hasCode(CONTENT) //
                .hasValidUnderlyingResponseFor(givenServerEndpointProvider);

        // an observation response should have been sent
        SingleObservation observation = observeResponse.getObservation();
        assertThat(observation.getPath()).asString().isEqualTo(expectedPath);
        assertThat(observation.getRegistrationId()).isEqualTo(currentRegistration.getId());
        Set<Observation> observations = server.getObservationService().getObservations(currentRegistration);
        assertThat(observations).containsExactly(observation);

        // write a new value
        LwM2mResponse writeResponse = server.send(currentRegistration, new WriteRequest(Mode.REPLACE, ContentFormat.TLV,
                expectedPath, LwM2mResourceInstance.newStringInstance(0, "a new string")));
        assertThat(writeResponse).hasCode(CHANGED);

        // verify result
        server.waitForNewObservation(observation);
        ObserveResponse response = server.waitForNotificationOf(observation);
        assertThat(response.getContent()).isEqualTo(LwM2mResourceInstance.newStringInstance(0, "a new string"));
        assertThat(response).hasValidUnderlyingResponseFor(givenServerEndpointProvider);

        // cancel observation : passive way
        server.getObservationService().cancelObservation(observation);
        server.waitForCancellationOf(observation, 500, TimeUnit.MILLISECONDS);
        observations = server.getObservationService().getObservations(currentRegistration);
        assertThat(observations).isEmpty();

        // write device timezone
        writeResponse = server.send(currentRegistration, new WriteRequest(Mode.REPLACE, ContentFormat.TLV, expectedPath,
                LwM2mResourceInstance.newStringInstance(0, "a another new string")));
        assertThat(writeResponse).hasCode(CHANGED);

        // verify result
        server.ensureNoNotification(observation, 1, TimeUnit.SECONDS);
    }

    @TestAllTransportLayer
    public void can_observe_resource_instance_then_active_cancel(Protocol givenProtocol,
            String givenClientEndpointProvider, String givenServerEndpointProvider) throws InterruptedException {

        // multi instance string
        String expectedPath = "/" + TestLwM2mId.TEST_OBJECT + "/0/" + TestLwM2mId.MULTIPLE_STRING_VALUE + "/0";
        ObserveResponse observeResponse = server.send(currentRegistration, new ObserveRequest(expectedPath));
        assertThat(observeResponse) //
                .hasCode(CONTENT) //
                .hasValidUnderlyingResponseFor(givenServerEndpointProvider);

        // an observation response should have been sent
        SingleObservation observation = observeResponse.getObservation();
        assertThat(observation.getPath()).asString().isEqualTo(expectedPath);
        assertThat(observation.getRegistrationId()).isEqualTo(currentRegistration.getId());
        Set<Observation> observations = server.getObservationService().getObservations(currentRegistration);
        assertThat(observations).containsExactly(observation);

        // write a new value
        LwM2mResponse writeResponse = server.send(currentRegistration, new WriteRequest(Mode.REPLACE, ContentFormat.TLV,
                expectedPath, LwM2mResourceInstance.newStringInstance(0, "a new string")));
        assertThat(writeResponse).hasCode(CHANGED);

        // verify result
        server.waitForNewObservation(observation);
        ObserveResponse response = server.waitForNotificationOf(observation);
        assertThat(response.getContent()).isEqualTo(LwM2mResourceInstance.newStringInstance(0, "a new string"));
        assertThat(response).hasValidUnderlyingResponseFor(givenServerEndpointProvider);

        // cancel observation : active way
        CancelObservationResponse cancelResponse = server.send(currentRegistration,
                new CancelObservationRequest(observation));
        assertThat(cancelResponse.isSuccess()).isTrue();
        assertThat(cancelResponse).hasCode(CONTENT);
        assertThat(((LwM2mResourceInstance) cancelResponse.getContent()).getValue()).isEqualTo("a new string");
        // active cancellation does not remove observation from store : it should be done manually using
        // ObservationService().cancelObservation(observation)
        observations = server.getObservationService().getObservations(currentRegistration);
        assertThat(observations).containsExactly(observation);

        // write device timezone
        writeResponse = server.send(currentRegistration, new WriteRequest(Mode.REPLACE, ContentFormat.TLV, expectedPath,
                LwM2mResourceInstance.newStringInstance(0, "a another new string")));
        assertThat(writeResponse).hasCode(CHANGED);

        // verify result
        server.ensureNoNotification(observation, 1, TimeUnit.SECONDS);
    }

    @TestAllTransportLayer
    public void can_observe_resource_then_passive_cancel(Protocol givenProtocol, String givenClientEndpointProvider,
            String givenServerEndpointProvider) throws InterruptedException {

        // observe device timezone
        ObserveResponse observeResponse = server.send(currentRegistration, new ObserveRequest(3, 0, 15));
        assertThat(observeResponse) //
                .hasCode(CONTENT) //
                .hasValidUnderlyingResponseFor(givenServerEndpointProvider);

        // an observation response should have been sent
        SingleObservation observation = observeResponse.getObservation();
        assertThat(observation.getPath()).asString().isEqualTo("/3/0/15");
        assertThat(observation.getRegistrationId()).isEqualTo(currentRegistration.getId());
        Set<Observation> observations = server.getObservationService().getObservations(currentRegistration);
        assertThat(observations).containsExactly(observation);

        // write device timezone
        LwM2mResponse writeResponse = server.send(currentRegistration, new WriteRequest(3, 0, 15, "Europe/Paris"));
        assertThat(writeResponse).hasCode(CHANGED);

        // verify result
        server.waitForNewObservation(observation);
        ObserveResponse response = server.waitForNotificationOf(observation);
        assertThat(response.getContent()).isEqualTo(LwM2mSingleResource.newStringResource(15, "Europe/Paris"));
        assertThat(response).hasValidUnderlyingResponseFor(givenServerEndpointProvider);

        // cancel observation : passive way
        server.getObservationService().cancelObservation(observation);
        server.waitForCancellationOf(observation, 500, TimeUnit.MILLISECONDS);
        observations = server.getObservationService().getObservations(currentRegistration);
        assertThat(observations).isEmpty();

        // write device timezone
        writeResponse = server.send(currentRegistration, new WriteRequest(3, 0, 15, "Europe/London"));
        assertThat(writeResponse).hasCode(CHANGED);

        // verify result
        server.ensureNoNotification(observation, 1, TimeUnit.SECONDS);
    }

    @TestAllTransportLayer
    public void can_observe_resource_then_active_cancel(Protocol givenProtocol, String givenClientEndpointProvider,
            String givenServerEndpointProvider) throws InterruptedException {

        // observe device timezone
        ObserveResponse observeResponse = server.send(currentRegistration, new ObserveRequest(3, 0, 15));
        assertThat(observeResponse) //
                .hasCode(CONTENT) //
                .hasValidUnderlyingResponseFor(givenServerEndpointProvider);

        // an observation response should have been sent
        SingleObservation observation = observeResponse.getObservation();
        assertThat(observation.getPath()).asString().isEqualTo("/3/0/15");
        assertThat(observation.getRegistrationId()).isEqualTo(currentRegistration.getId());
        Set<Observation> observations = server.getObservationService().getObservations(currentRegistration);
        assertThat(observations).containsExactly(observation);

        // write device timezone
        LwM2mResponse writeResponse = server.send(currentRegistration, new WriteRequest(3, 0, 15, "Europe/Paris"));
        assertThat(writeResponse).hasCode(CHANGED);

        // verify result
        server.waitForNewObservation(observation);
        ObserveResponse response = server.waitForNotificationOf(observation);
        assertThat(response.getContent()).isEqualTo(LwM2mSingleResource.newStringResource(15, "Europe/Paris"));
        assertThat(response).hasValidUnderlyingResponseFor(givenServerEndpointProvider);

        // cancel observation : active way
        CancelObservationResponse cancelResponse = server.send(currentRegistration,
                new CancelObservationRequest(observation));
        assertThat(cancelResponse.isSuccess()).isTrue();
        assertThat(cancelResponse).hasCode(CONTENT);
        assertThat(((LwM2mSingleResource) cancelResponse.getContent()).getValue()).isEqualTo("Europe/Paris");
        // active cancellation does not remove observation from store : it should be done manually using
        // ObservationService().cancelObservation(observation)
        observations = server.getObservationService().getObservations(currentRegistration);
        assertThat(observations).containsExactly(observation);

        // write device timezone
        writeResponse = server.send(currentRegistration, new WriteRequest(3, 0, 15, "Europe/London"));
        assertThat(writeResponse).hasCode(CHANGED);

        // verify result
        server.ensureNoNotification(observation, 1, TimeUnit.SECONDS);
    }

    @TestAllTransportLayer
    public void can_observe_instance(Protocol givenProtocol, String givenClientEndpointProvider,
            String givenServerEndpointProvider) throws InterruptedException {

        // observe device timezone
        ObserveResponse observeResponse = server.send(currentRegistration, new ObserveRequest(3, 0));
        assertThat(observeResponse) //
                .hasCode(CONTENT) //
                .hasValidUnderlyingResponseFor(givenServerEndpointProvider);

        // an observation response should have been sent
        SingleObservation observation = observeResponse.getObservation();
        assertThat(observation.getPath()).asString().isEqualTo("/3/0");
        assertThat(observation.getRegistrationId()).isEqualTo(currentRegistration.getId());
        Set<Observation> observations = server.getObservationService().getObservations(currentRegistration);
        assertThat(observations).containsExactly(observation);

        // write device timezone
        LwM2mResponse writeResponse = server.send(currentRegistration, new WriteRequest(3, 0, 15, "Europe/Paris"));
        assertThat(writeResponse).hasCode(CHANGED);

        // verify result
        server.waitForNewObservation(observation);
        ObserveResponse response = server.waitForNotificationOf(observation);
        assertThat(response.getContent()).isInstanceOf(LwM2mObjectInstance.class);
        assertThat(response).hasValidUnderlyingResponseFor(givenServerEndpointProvider);

        // try to read the object instance for comparing
        ReadResponse readResp = server.send(currentRegistration, new ReadRequest(3, 0));
        assertThat(response.getContent()).isEqualTo(readResp.getContent());
    }

    @TestAllTransportLayer
    public void can_observe_object(Protocol givenProtocol, String givenClientEndpointProvider,
            String givenServerEndpointProvider) throws InterruptedException {

        // observe device timezone
        ObserveResponse observeResponse = server.send(currentRegistration, new ObserveRequest(3));
        assertThat(observeResponse) //
                .hasCode(CONTENT) //
                .hasValidUnderlyingResponseFor(givenServerEndpointProvider);

        // an observation response should have been sent
        SingleObservation observation = observeResponse.getObservation();
        assertThat(observation.getPath()).asString().isEqualTo("/3");
        assertThat(observation.getRegistrationId()).isEqualTo(currentRegistration.getId());
        Set<Observation> observations = server.getObservationService().getObservations(currentRegistration);
        assertThat(observations).containsExactly(observation);

        // write device timezone
        LwM2mResponse writeResponse = server.send(currentRegistration, new WriteRequest(3, 0, 15, "Europe/Paris"));
        assertThat(writeResponse).hasCode(CHANGED);

        // verify result
        server.waitForNewObservation(observation);
        ObserveResponse response = server.waitForNotificationOf(observation);
        assertThat(response.getContent()).isInstanceOf(LwM2mObject.class);
        assertThat(response).hasValidUnderlyingResponseFor(givenServerEndpointProvider);

        // try to read the object for comparing
        ReadResponse readResp = server.send(currentRegistration, new ReadRequest(3));
        assertThat(response.getContent()).isEqualTo(readResp.getContent());
    }

    // TODO seems to be a server test only (lockstep client should be used)
    @TestAllTransportLayer
    public void can_handle_error_on_notification(Protocol givenProtocol, String givenClientEndpointProvider,
            String givenServerEndpointProvider) throws InterruptedException {

        // observe device timezone
        ObserveResponse observeResponse = server.send(currentRegistration, new ObserveRequest(3, 0, 15));
        assertThat(observeResponse) //
                .hasCode(CONTENT) //
                .hasValidUnderlyingResponseFor(givenServerEndpointProvider);

        // an observation response should have been sent
        SingleObservation observation = observeResponse.getObservation();
        assertThat(observation.getPath()).asString().isEqualTo("/3/0/15");
        assertThat(observation.getRegistrationId()).isEqualTo(currentRegistration.getId());
        Set<Observation> observations = server.getObservationService().getObservations(currentRegistration);
        assertThat(observations).containsExactly(observation);

        // *** HACK send a notification with unsupported content format *** //
        byte[] payload = new LwM2mNodeJsonEncoder().encode(LwM2mSingleResource.newStringResource(15, "Paris"),
                new LwM2mPath("/3/0/15"), client.getObjectTree().getModel(), new LwM2mValueChecker());

        // 666 is not a supported content format.
        Connector connector = client
                .getClientConnector(client.getServerIdForRegistrationId("/rd/" + currentRegistration.getId()));
        TestObserveUtil.sendNotification(connector, server.getEndpoint(Protocol.COAP).getURI(), payload,
                observeResponse.getObservation().getId().getBytes(), 2, ContentFormat.fromCode(666));
        // *** Hack End *** //

        // verify result
        server.waitForNewObservation(observation);
        server.waitForNotificationErrorOf(observation, 1, TimeUnit.SECONDS);
    }

}
