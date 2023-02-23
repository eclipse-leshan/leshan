/*******************************************************************************
 * Copyright (c) 2021 Orange.
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
 *     Michał Wadowski (Orange) - Add Observe-Composite feature.
 *******************************************************************************/
package org.eclipse.leshan.integration.tests.observe;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.eclipse.leshan.core.ResponseCode.CHANGED;
import static org.eclipse.leshan.core.ResponseCode.CONTENT;
import static org.eclipse.leshan.integration.tests.util.LeshanTestClientBuilder.givenClientUsing;
import static org.eclipse.leshan.integration.tests.util.LeshanTestServerBuilder.givenServerUsing;
import static org.eclipse.leshan.integration.tests.util.assertion.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import org.eclipse.leshan.core.endpoint.Protocol;
import org.eclipse.leshan.core.node.LwM2mNode;
import org.eclipse.leshan.core.node.LwM2mPath;
import org.eclipse.leshan.core.node.LwM2mSingleResource;
import org.eclipse.leshan.core.observation.CompositeObservation;
import org.eclipse.leshan.core.observation.Observation;
import org.eclipse.leshan.core.request.CancelCompositeObservationRequest;
import org.eclipse.leshan.core.request.ContentFormat;
import org.eclipse.leshan.core.request.ObserveCompositeRequest;
import org.eclipse.leshan.core.request.ReadRequest;
import org.eclipse.leshan.core.request.WriteCompositeRequest;
import org.eclipse.leshan.core.request.WriteRequest;
import org.eclipse.leshan.core.response.CancelCompositeObservationResponse;
import org.eclipse.leshan.core.response.LwM2mResponse;
import org.eclipse.leshan.core.response.ObserveCompositeResponse;
import org.eclipse.leshan.core.response.ReadResponse;
import org.eclipse.leshan.core.response.WriteCompositeResponse;
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
public class ObserveCompositeTest {

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
    public void can_composite_observe_on_single_resource(Protocol givenProtocol, String givenClientEndpointProvider,
            String givenServerEndpointProvider) throws InterruptedException {
        // Send ObserveCompositeRequest
        ObserveCompositeResponse observeResponse = server.send(currentRegistration,
                new ObserveCompositeRequest(ContentFormat.SENML_JSON, ContentFormat.SENML_JSON, "/3/0/15"));

        // Assert that ObserveCompositeResponse is valid
        assertThat(observeResponse) //
                .hasCode(CONTENT) //
                .hasValidUnderlyingResponseFor(givenServerEndpointProvider);

        // Assert that CompositeObservation contains expected paths
        CompositeObservation observation = observeResponse.getObservation();
        assertThat(observation).isNotNull();
        assertThat(observation.getPaths()).containsOnly(new LwM2mPath("/3/0/15"));

        // Assert that there is one valid observation
        server.waitForNewObservation(observation);
        assertThat(observation.getRegistrationId()).isEqualTo(currentRegistration.getId());
        Set<Observation> observations = server.getObservationService().getObservations(currentRegistration);
        assertThat(observations).containsExactly(observation);

        // Write single example value
        LwM2mResponse writeResponse = server.send(currentRegistration, new WriteRequest(3, 0, 15, "Europe/Paris"));
        assertThat(writeResponse).hasCode(CHANGED);

        // Assert that response contains expected paths
        ObserveCompositeResponse response = server.waitForNotificationOf(observation);
        assertThat(response).hasValidUnderlyingResponseFor(givenServerEndpointProvider);
        assertThat(response.getContent()).containsOnly(
                entry(new LwM2mPath("/3/0/15"), LwM2mSingleResource.newStringResource(15, "Europe/Paris")));
    }

    @TestAllTransportLayer
    public void should_not_get_response_if_modified_other_resource_than_observed(Protocol givenProtocol,
            String givenClientEndpointProvider, String givenServerEndpointProvider) throws InterruptedException {
        // Send ObserveCompositeRequest
        ObserveCompositeResponse observeResponse = server.send(currentRegistration,
                new ObserveCompositeRequest(ContentFormat.SENML_JSON, ContentFormat.SENML_JSON, "/3/0/14"));

        // Assert that ObserveCompositeResponse is valid
        assertThat(observeResponse) //
                .hasCode(CONTENT) //
                .hasValidUnderlyingResponseFor(givenServerEndpointProvider);

        // Assert that CompositeObservation contains expected paths
        CompositeObservation observation = observeResponse.getObservation();
        assertThat(observation).isNotNull();
        assertThat(observation.getPaths()).containsOnly(new LwM2mPath("/3/0/14"));

        // Assert that there is one valid observation
        server.waitForNewObservation(observation);
        assertThat(observation.getRegistrationId()).isEqualTo(currentRegistration.getId());
        Set<Observation> observations = server.getObservationService().getObservations(currentRegistration);
        assertThat(observations).containsExactly(observation);

        // Write single example value
        LwM2mResponse writeResponse = server.send(currentRegistration, new WriteRequest(3, 0, 15, "Europe/Paris"));
        assertThat(writeResponse).hasCode(CHANGED);

        // Assert that listener has no response
        server.ensureNoNotification(observation, 1, TimeUnit.SECONDS);
    }

    @TestAllTransportLayer
    public void can_composite_observe_on_multiple_resources(Protocol givenProtocol, String givenClientEndpointProvider,
            String givenServerEndpointProvider) throws InterruptedException {
        // Send ObserveCompositeRequest
        ObserveCompositeResponse observeResponse = server.send(currentRegistration,
                new ObserveCompositeRequest(ContentFormat.SENML_JSON, ContentFormat.SENML_JSON, "/3/0/15", "/3/0/14"));

        // Assert that ObserveCompositeResponse is valid
        assertThat(observeResponse) //
                .hasCode(CONTENT) //
                .hasValidUnderlyingResponseFor(givenServerEndpointProvider);

        LwM2mNode previousOffset = observeResponse.getContent("/3/0/14");

        // Assert that CompositeObservation contains expected paths
        CompositeObservation observation = observeResponse.getObservation();
        assertThat(observation).isNotNull();
        assertThat(observation.getPaths()).containsOnly(new LwM2mPath("/3/0/15"), new LwM2mPath("/3/0/14"));

        // Assert that there is one valid observation
        server.waitForNewObservation(observation);
        assertThat(observation.getRegistrationId()).isEqualTo(currentRegistration.getId());
        Set<Observation> observations = server.getObservationService().getObservations(currentRegistration);
        assertThat(observations).containsExactly(observation);

        // Write single example value
        LwM2mResponse writeResponse = server.send(currentRegistration, new WriteRequest(3, 0, 15, "Europe/Paris"));
        assertThat(writeResponse).hasCode(CHANGED);

        // Assert that response contains exactly the same paths
        ObserveCompositeResponse response = server.waitForNotificationOf(observation);
        assertThat(response).hasValidUnderlyingResponseFor(givenServerEndpointProvider);
        assertThat(response.getContent()).containsOnly(
                entry(new LwM2mPath("/3/0/15"), LwM2mSingleResource.newStringResource(15, "Europe/Paris")),
                entry(new LwM2mPath("/3/0/14"), previousOffset));
    }

    @TestAllTransportLayer
    public void can_composite_observe_on_multiple_resources_with_write_composite(Protocol givenProtocol,
            String givenClientEndpointProvider, String givenServerEndpointProvider) throws InterruptedException {
        // Send ObserveCompositeRequest
        ObserveCompositeResponse observeResponse = server.send(currentRegistration,
                new ObserveCompositeRequest(ContentFormat.SENML_JSON, ContentFormat.SENML_JSON, "/3/0/15", "/3/0/14"));

        // Assert that ObserveCompositeResponse is valid
        assertThat(observeResponse) //
                .hasCode(CONTENT) //
                .hasValidUnderlyingResponseFor(givenServerEndpointProvider);

        // Assert that CompositeObservation contains expected paths
        CompositeObservation observation = observeResponse.getObservation();
        assertThat(observation).isNotNull();
        assertThat(observation.getPaths()).containsOnly(new LwM2mPath("/3/0/15"), new LwM2mPath("/3/0/14"));

        // Assert that there is one valid observation
        server.waitForNewObservation(observation);
        assertThat(observation.getRegistrationId()).isEqualTo(currentRegistration.getId());
        Set<Observation> observations = server.getObservationService().getObservations(currentRegistration);
        assertThat(observations).containsExactly(observation);

        // Write example composite values
        Map<String, Object> nodes = new HashMap<>();
        nodes.put("/3/0/15", "Europe/Paris");
        nodes.put("/3/0/14", "+11");
        WriteCompositeResponse writeResponse = server.send(currentRegistration,
                new WriteCompositeRequest(ContentFormat.SENML_JSON, nodes));
        assertThat(writeResponse).hasCode(CHANGED);

        // Assert that response contains expected paths
        ObserveCompositeResponse response = server.waitForNotificationOf(observation);
        assertThat(response).hasValidUnderlyingResponseFor(givenServerEndpointProvider);
        assertThat(response.getContent()).containsOnly(
                entry(new LwM2mPath("/3/0/15"), LwM2mSingleResource.newStringResource(15, "Europe/Paris")),
                entry(new LwM2mPath("/3/0/14"), LwM2mSingleResource.newStringResource(14, "+11")));
    }

    @TestAllTransportLayer
    public void can_observe_instance(Protocol givenProtocol, String givenClientEndpointProvider,
            String givenServerEndpointProvider) throws InterruptedException {
        // Send ObserveCompositeRequest
        ObserveCompositeResponse observeResponse = server.send(currentRegistration,
                new ObserveCompositeRequest(ContentFormat.SENML_JSON, ContentFormat.SENML_JSON, "/3/0"));

        // Assert that ObserveCompositeResponse is valid
        assertThat(observeResponse) //
                .hasCode(CONTENT) //
                .hasValidUnderlyingResponseFor(givenServerEndpointProvider);

        // Assert that CompositeObservation contains expected paths
        CompositeObservation observation = observeResponse.getObservation();
        assertThat(observation).isNotNull();
        assertThat(observation.getPaths()).containsOnly(new LwM2mPath("/3/0"));

        // Assert that there is one valid observation
        server.waitForNewObservation(observation);
        assertThat(observation.getRegistrationId()).isEqualTo(currentRegistration.getId());
        Set<Observation> observations = server.getObservationService().getObservations(currentRegistration);
        assertThat(observations).containsExactly(observation);

        // Write single example value
        LwM2mResponse writeResponse = server.send(currentRegistration, new WriteRequest(3, 0, 15, "Europe/Paris"));
        assertThat(writeResponse).hasCode(CHANGED);

        // Assert that response contains expected paths
        ObserveCompositeResponse response = server.waitForNotificationOf(observation);
        assertThat(response).hasValidUnderlyingResponseFor(givenServerEndpointProvider);

        ReadResponse readResp = server.send(currentRegistration, new ReadRequest(ContentFormat.SENML_JSON, "/3/0"));
        assertThat(response.getContent()).containsOnly(entry(new LwM2mPath("/3/0"), readResp.getContent()));
    }

    @TestAllTransportLayer
    public void can_observe_object(Protocol givenProtocol, String givenClientEndpointProvider,
            String givenServerEndpointProvider) throws InterruptedException {
        // Send ObserveCompositeRequest
        ObserveCompositeResponse observeResponse = server.send(currentRegistration,
                new ObserveCompositeRequest(ContentFormat.SENML_JSON, ContentFormat.SENML_JSON, "/3"));

        // Assert that ObserveCompositeResponse is valid
        assertThat(observeResponse) //
                .hasCode(CONTENT) //
                .hasValidUnderlyingResponseFor(givenServerEndpointProvider);

        // Assert that CompositeObservation contains expected paths
        CompositeObservation observation = observeResponse.getObservation();
        assertThat(observation).isNotNull();
        assertThat(observation.getPaths()).containsOnly(new LwM2mPath("/3"));

        // Assert that there is one valid observation
        server.waitForNewObservation(observation);
        assertThat(observation.getRegistrationId()).isEqualTo(currentRegistration.getId());
        Set<Observation> observations = server.getObservationService().getObservations(currentRegistration);
        assertThat(observations).containsExactly(observation);

        // Write single example value
        LwM2mResponse writeResponse = server.send(currentRegistration, new WriteRequest(3, 0, 15, "Europe/Paris"));
        assertThat(writeResponse).hasCode(CHANGED);

        // Assert that response contains expected paths
        ObserveCompositeResponse response = server.waitForNotificationOf(observation);
        assertThat(response).hasValidUnderlyingResponseFor(givenServerEndpointProvider);

        ReadResponse readResp = server.send(currentRegistration, new ReadRequest(ContentFormat.SENML_JSON, "/3"));
        assertThat(response.getContent()).containsOnly(entry(new LwM2mPath("/3"), readResp.getContent()));
    }

    @TestAllTransportLayer
    public void can_passive_cancel_composite_observation(Protocol givenProtocol, String givenClientEndpointProvider,
            String givenServerEndpointProvider) throws InterruptedException {
        // Send ObserveCompositeRequest
        ObserveCompositeResponse observeCompositeResponse = server.send(currentRegistration,
                new ObserveCompositeRequest(ContentFormat.SENML_JSON, ContentFormat.SENML_JSON, "/3/0/15"));
        CompositeObservation observation = observeCompositeResponse.getObservation();
        server.waitForNewObservation(observation);

        // Write single example value
        LwM2mResponse writeResponse = server.send(currentRegistration, new WriteRequest(3, 0, 15, "Europe/Paris"));
        assertThat(writeResponse).hasCode(CHANGED);
        server.waitForNotificationOf(observation);

        // cancel observation : passive way
        server.getObservationService().cancelObservation(observation);
        Set<Observation> observations = server.getObservationService().getObservations(currentRegistration);
        assertThat(observations).isEmpty();
        server.waitForCancellationOf(observation, 500, TimeUnit.MILLISECONDS);

        // Write single value
        writeResponse = server.send(currentRegistration, new WriteRequest(3, 0, 15, "Europe/London"));
        assertThat(writeResponse).hasCode(CHANGED);

        server.ensureNoNotification(observation, 500, TimeUnit.MILLISECONDS);
    }

    @TestAllTransportLayer
    public void can_active_cancel_composite_observation(Protocol givenProtocol, String givenClientEndpointProvider,
            String givenServerEndpointProvider) throws InterruptedException {
        // Send ObserveCompositeRequest
        ObserveCompositeResponse observeCompositeResponse = server.send(currentRegistration,
                new ObserveCompositeRequest(ContentFormat.SENML_JSON, ContentFormat.SENML_JSON, "/3/0/15"));
        CompositeObservation observation = observeCompositeResponse.getObservation();
        server.waitForNewObservation(observation);

        // Write single example value
        LwM2mResponse writeResponse = server.send(currentRegistration, new WriteRequest(3, 0, 15, "Europe/Paris"));
        assertThat(writeResponse).hasCode(CHANGED);

        // Assert that response contains exactly the same paths
        ObserveCompositeResponse response = server.waitForNotificationOf(observation);
        assertThat(response).hasValidUnderlyingResponseFor(givenServerEndpointProvider);
        assertThat(response.getContent()).containsOnly(
                entry(new LwM2mPath("/3/0/15"), LwM2mSingleResource.newStringResource(15, "Europe/Paris")));

        // cancel observation : active way
        CancelCompositeObservationResponse cancelResponse = server.send(currentRegistration,
                new CancelCompositeObservationRequest(observation));
        assertThat(cancelResponse.isSuccess()).isTrue();
        assertThat(cancelResponse).hasCode(CONTENT);

        // active cancellation does not remove observation from store : it should be done manually using
        // ObservationService().cancelObservation(observation)

        // Assert that there is one valid observation
        assertThat(observation.getRegistrationId()).isEqualTo(currentRegistration.getId());
        Set<Observation> observations = server.getObservationService().getObservations(currentRegistration);
        assertThat(observations).containsExactly(observation);

        writeResponse = server.send(currentRegistration, new WriteRequest(3, 0, 15, "Europe/London"));
        assertThat(writeResponse).hasCode(CHANGED);

        server.ensureNoNotification(observation, 500, TimeUnit.MILLISECONDS);
    }

}
