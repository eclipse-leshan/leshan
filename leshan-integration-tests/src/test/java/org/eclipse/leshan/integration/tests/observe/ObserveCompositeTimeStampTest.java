/*******************************************************************************
 * Copyright (c) 2020 Sierra Wireless and others.
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
 *     Jarosław Legierski Orange Polska S.A. - initial API and implementation
 *******************************************************************************/

package org.eclipse.leshan.integration.tests.observe;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.leshan.core.ResponseCode.CONTENT;
import static org.eclipse.leshan.integration.tests.util.LeshanTestClientBuilder.givenClientUsing;
import static org.eclipse.leshan.integration.tests.util.LeshanTestServerBuilder.givenServerUsing;
import static org.eclipse.leshan.integration.tests.util.assertion.Assertions.assertThat;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import org.eclipse.leshan.core.endpoint.Protocol;
import org.eclipse.leshan.core.node.LwM2mNode;
import org.eclipse.leshan.core.node.LwM2mObject;
import org.eclipse.leshan.core.node.LwM2mObjectInstance;
import org.eclipse.leshan.core.node.LwM2mPath;
import org.eclipse.leshan.core.node.LwM2mResource;
import org.eclipse.leshan.core.node.LwM2mSingleResource;
import org.eclipse.leshan.core.node.TimestampedLwM2mNodes;
import org.eclipse.leshan.core.node.codec.DefaultLwM2mEncoder;
import org.eclipse.leshan.core.node.codec.LwM2mEncoder;
import org.eclipse.leshan.core.observation.CompositeObservation;
import org.eclipse.leshan.core.observation.Observation;
import org.eclipse.leshan.core.request.ContentFormat;
import org.eclipse.leshan.core.request.ObserveCompositeRequest;
import org.eclipse.leshan.core.response.ObserveCompositeResponse;
import org.eclipse.leshan.integration.tests.util.LeshanTestClient;
import org.eclipse.leshan.integration.tests.util.LeshanTestServer;
import org.eclipse.leshan.integration.tests.util.junit5.extensions.ArgumentsUtil;
import org.eclipse.leshan.integration.tests.util.junit5.extensions.BeforeEachParameterizedResolver;
import org.eclipse.leshan.server.registration.Registration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@ExtendWith(BeforeEachParameterizedResolver.class)
public class ObserveCompositeTimeStampTest {

    /*---------------------------------/
     *  Parameterized Tests
     * -------------------------------*/
    @ParameterizedTest(name = "{0} over COAP - Client using Californium - Server using {1}")
    @MethodSource("transports")
    @Retention(RetentionPolicy.RUNTIME)
    private @interface TestAllCases {
    }

    static Stream<Arguments> transports() {

        Object[][] transports = new Object[][] {
                // Server Endpoint Provider
                { "Californium" }, //
                { "java-coap" } };

        Object[] contentFormats = new Object[] { //
                ContentFormat.SENML_JSON, //
                ContentFormat.SENML_CBOR };

        // for each transport, create 1 test by format.
        return Stream.of(ArgumentsUtil.combine(contentFormats, transports));
    }

    /*---------------------------------/
     *  Set-up and Tear-down Tests
     * -------------------------------*/

    LeshanTestServer server;
    LeshanTestClient client;
    Registration currentRegistration;
    LwM2mEncoder encoder = new DefaultLwM2mEncoder();

    @BeforeEach
    public void start(ContentFormat format, String givenServerEndpointProvider) {
        server = givenServerUsing(Protocol.COAP).with(givenServerEndpointProvider).build();
        server.start();
        client = givenClientUsing(Protocol.COAP).with("Californium").connectingTo(server).build();
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
    @TestAllCases
    public void can_observecomposite_timestamped_resource(ContentFormat contentFormat,
            String givenServerEndpointProvider) throws InterruptedException {
        // observe device timezone

        // LwM2mPaths
        List<LwM2mPath> paths = new ArrayList<>();
        paths.add(new LwM2mPath("/1/0/1"));
        paths.add(new LwM2mPath("/3/0/15"));

        ObserveCompositeResponse observeResponse = server.send(currentRegistration,
                new ObserveCompositeRequest(contentFormat, contentFormat, paths));
        assertThat(observeResponse) //
                .hasCode(CONTENT) //
                .hasValidUnderlyingResponseFor(givenServerEndpointProvider);

        // an observation response should have been sent
        CompositeObservation observation = observeResponse.getObservation();

        assertThat(observation.getPaths()).asString().isEqualTo("[/1/0/1, /3/0/15]");
        assertThat(observation.getRegistrationId()).isEqualTo(currentRegistration.getId());
        Set<Observation> observations = server.getObservationService().getObservations(currentRegistration);
        assertThat(observations).containsExactly(observation);

        // *** HACK send time-stamped notification as Leshan client does not support it *** //
        // create time-stamped nodes

        TimestampedLwM2mNodes.Builder builder = new TimestampedLwM2mNodes.Builder();
        Map<LwM2mPath, LwM2mNode> currentValues = new HashMap<>();
        currentValues.put(paths.get(0), LwM2mSingleResource.newIntegerResource(1, 3600));
        builder.addNodes(Instant.ofEpochMilli(System.currentTimeMillis()), currentValues);
        currentValues.clear();
        currentValues.put(paths.get(1), LwM2mSingleResource.newStringResource(15, "Europe/Belgrade"));
        builder.addNodes(Instant.ofEpochMilli(System.currentTimeMillis()), currentValues);
        TimestampedLwM2mNodes timestampednodes = builder.build();

        byte[] payload = encoder.encodeTimestampedNodes(timestampednodes, contentFormat,
                client.getObjectTree().getModel());

        TestObserveUtil.sendNotification(
                client.getClientConnector(client.getServerIdForRegistrationId("/rd/" + currentRegistration.getId())),
                server.getEndpoint(Protocol.COAP).getURI(), payload,
                observeResponse.getObservation().getId().getBytes(), 2, contentFormat);
        // *** Hack End *** //

        // verify result
        server.waitForNewObservation(observation);
        ObserveCompositeResponse response = server.waitForNotificationOf(observation);
        assertThat(response).hasContentFormat(contentFormat, givenServerEndpointProvider);
        assertThat(response.getContent().get(new LwM2mPath("/1/0/1")))
                .isEqualTo(timestampednodes.getNodes().get(new LwM2mPath("/1/0/1")));
        assertThat(response.getContent().get(new LwM2mPath("/3/0/15")))
                .isEqualTo(timestampednodes.getNodes().get(new LwM2mPath("/3/0/15")));
        assertThat(response.getTimestampedLwM2mNodes()).isEqualTo(timestampednodes);
    }

    @TestAllCases
    public void can_observecomposite_timestamped_instance(ContentFormat contentFormat,
            String givenServerEndpointProvider) throws InterruptedException {

        // LwM2mPaths
        List<LwM2mPath> paths = new ArrayList<>();
        paths.add(new LwM2mPath("/1/0/1"));
        paths.add(new LwM2mPath("/3/0"));

        // observe device timezone
        ObserveCompositeResponse observeResponse = server.send(currentRegistration,
                new ObserveCompositeRequest(contentFormat, contentFormat, "/1/0/1", "/3/0"));
        assertThat(observeResponse) //
                .hasCode(CONTENT) //
                .hasValidUnderlyingResponseFor(givenServerEndpointProvider);

        // an observation response should have been sent
        CompositeObservation observation = observeResponse.getObservation();
        assertThat(observation.getPaths()).asString().isEqualTo("[/1/0/1, /3/0]");
        assertThat(observation.getRegistrationId()).isEqualTo(currentRegistration.getId());
        Set<Observation> observations = server.getObservationService().getObservations(currentRegistration);
        assertThat(observations).containsExactly(observation);

        // *** HACK send time-stamped notification as Leshan client does not support it *** //
        // create time-stamped nodes

        List<LwM2mResource> deviceresources = new ArrayList<LwM2mResource>();
        deviceresources.add(LwM2mSingleResource.newStringResource(0, "Leshan Demo Device"));
        deviceresources.add(LwM2mSingleResource.newStringResource(1, "Model 500"));
        deviceresources.add(LwM2mSingleResource.newStringResource(2, "LT-500-000-0001"));
        deviceresources.add(LwM2mSingleResource.newStringResource(3, "1.0.0"));
        deviceresources.add(LwM2mSingleResource.newIntegerResource(9, 75));
        deviceresources.add(LwM2mSingleResource.newIntegerResource(10, 423455));
        deviceresources.add(LwM2mSingleResource.newIntegerResource(11, 0));
        deviceresources.add(LwM2mSingleResource.newDateResource(13, new Date(1702050067000L)));
        deviceresources.add(LwM2mSingleResource.newStringResource(14, "+01"));
        deviceresources.add(LwM2mSingleResource.newStringResource(15, "Europe/Belgrade"));
        deviceresources.add(LwM2mSingleResource.newStringResource(16, "U"));
        deviceresources.add(LwM2mSingleResource.newStringResource(17, "Demo"));
        deviceresources.add(LwM2mSingleResource.newStringResource(18, "1.0.1"));
        deviceresources.add(LwM2mSingleResource.newStringResource(19, "1.0.2"));
        deviceresources.add(LwM2mSingleResource.newIntegerResource(20, 4));
        deviceresources.add(LwM2mSingleResource.newIntegerResource(21, 20500736));

        TimestampedLwM2mNodes.Builder builder = new TimestampedLwM2mNodes.Builder();
        Map<LwM2mPath, LwM2mNode> currentValues = new HashMap<>();
        currentValues.put(paths.get(0), LwM2mSingleResource.newIntegerResource(1, 3600));
        currentValues.put(paths.get(1), new LwM2mObjectInstance(0, deviceresources));
        builder.addNodes(Instant.ofEpochMilli(System.currentTimeMillis()), currentValues);
        TimestampedLwM2mNodes timestampednodes = builder.build();

        byte[] payload = encoder.encodeTimestampedNodes(timestampednodes, contentFormat,
                client.getObjectTree().getModel());

        TestObserveUtil.sendNotification(
                client.getClientConnector(client.getServerIdForRegistrationId("/rd/" + currentRegistration.getId())),
                server.getEndpoint(Protocol.COAP).getURI(), payload,
                observeResponse.getObservation().getId().getBytes(), 2, contentFormat);
        // *** Hack End *** //

        // verify result
        server.waitForNewObservation(observation);
        ObserveCompositeResponse response = server.waitForNotificationOf(observation);

        assertThat(response).hasContentFormat(contentFormat, givenServerEndpointProvider);

        assertThat(response.getTimestampedLwM2mNodes().getNodes().get(new LwM2mPath("/1/0/1")))
                .isEqualTo((timestampednodes.getNodes().get(new LwM2mPath("/1/0/1"))));

        for (LwM2mResource deviceresource : deviceresources) {
            assertThat(
                    response.getTimestampedLwM2mNodes().getNodes().get(new LwM2mPath("/3/0/" + deviceresource.getId())))
                            .isEqualTo(((LwM2mObjectInstance) timestampednodes.getNodes().get(new LwM2mPath("/3/0")))
                                    .getResource(deviceresource.getId()));
        }
        assertThat(response.getTimestampedLwM2mNodes().getTimestamps()).isEqualTo(timestampednodes.getTimestamps());

    }

    @TestAllCases
    public void can_observecomposite__timestamped_object(ContentFormat contentFormat,
            String givenServerEndpointProvider) throws InterruptedException {
        // LwM2mPaths
        List<LwM2mPath> paths = new ArrayList<>();
        paths.add(new LwM2mPath("/1/0/1"));
        paths.add(new LwM2mPath("/3"));

        // observe device timezone
        ObserveCompositeResponse observeResponse = server.send(currentRegistration,
                new ObserveCompositeRequest(contentFormat, contentFormat, "/1/0/1", "/3"));
        assertThat(observeResponse) //
                .hasCode(CONTENT) //
                .hasValidUnderlyingResponseFor(givenServerEndpointProvider);

        // an observation response should have been sent
        CompositeObservation observation = observeResponse.getObservation();
        assertThat(observation.getPaths()).asString().isEqualTo("[/1/0/1, /3]");
        assertThat(observation.getRegistrationId()).isEqualTo(currentRegistration.getId());
        Set<Observation> observations = server.getObservationService().getObservations(currentRegistration);
        assertThat(observations).containsExactly(observation);

        // *** HACK send time-stamped notification as Leshan client does not support it *** //
        // create time-stamped nodes

        List<LwM2mResource> deviceresources = new ArrayList<LwM2mResource>();
        deviceresources.add(LwM2mSingleResource.newStringResource(0, "Leshan Demo Device"));
        deviceresources.add(LwM2mSingleResource.newStringResource(1, "Model 500"));
        deviceresources.add(LwM2mSingleResource.newStringResource(2, "LT-500-000-0001"));
        deviceresources.add(LwM2mSingleResource.newStringResource(3, "1.0.0"));
        deviceresources.add(LwM2mSingleResource.newIntegerResource(9, 75));
        deviceresources.add(LwM2mSingleResource.newIntegerResource(10, 423455));
        deviceresources.add(LwM2mSingleResource.newIntegerResource(11, 0));
        deviceresources.add(LwM2mSingleResource.newDateResource(13, new Date(1702050067000L)));
        deviceresources.add(LwM2mSingleResource.newStringResource(14, "+01"));
        deviceresources.add(LwM2mSingleResource.newStringResource(15, "Europe/Belgrade"));
        deviceresources.add(LwM2mSingleResource.newStringResource(16, "U"));
        deviceresources.add(LwM2mSingleResource.newStringResource(17, "Demo"));
        deviceresources.add(LwM2mSingleResource.newStringResource(18, "1.0.1"));
        deviceresources.add(LwM2mSingleResource.newStringResource(19, "1.0.2"));
        deviceresources.add(LwM2mSingleResource.newIntegerResource(20, 4));
        deviceresources.add(LwM2mSingleResource.newIntegerResource(21, 20500736));

        TimestampedLwM2mNodes.Builder builder = new TimestampedLwM2mNodes.Builder();
        Map<LwM2mPath, LwM2mNode> currentValues = new HashMap<>();
        currentValues.put(paths.get(0), LwM2mSingleResource.newIntegerResource(1, 3600));
        currentValues.put(paths.get(1), new LwM2mObject(3, new LwM2mObjectInstance(0, deviceresources)));
        builder.addNodes(Instant.ofEpochMilli(System.currentTimeMillis()), currentValues);
        TimestampedLwM2mNodes timestampednodes = builder.build();

        byte[] payload = encoder.encodeTimestampedNodes(timestampednodes, contentFormat,
                client.getObjectTree().getModel());

        TestObserveUtil.sendNotification(
                client.getClientConnector(client.getServerIdForRegistrationId("/rd/" + currentRegistration.getId())),
                server.getEndpoint(Protocol.COAP).getURI(), payload,
                observeResponse.getObservation().getId().getBytes(), 2, contentFormat);
        // *** Hack End *** //

        // verify result
        server.waitForNewObservation(observation);
        ObserveCompositeResponse response = server.waitForNotificationOf(observation);

        assertThat(response).hasContentFormat(contentFormat, givenServerEndpointProvider);

        assertThat(response.getTimestampedLwM2mNodes().getNodes().get(new LwM2mPath("/1/0/1")))
                .isEqualTo((timestampednodes.getNodes().get(new LwM2mPath("/1/0/1"))));

        for (LwM2mResource deviceresource : deviceresources) {
            assertThat(
                    response.getTimestampedLwM2mNodes().getNodes().get(new LwM2mPath("/3/0/" + deviceresource.getId())))
                            .isEqualTo(((LwM2mObject) timestampednodes.getNodes().get(new LwM2mPath("/3")))
                                    .getInstances().get(0).getResource(deviceresource.getId()));
        }
        assertThat(response.getTimestampedLwM2mNodes().getTimestamps()).isEqualTo(timestampednodes.getTimestamps());
    }

}
