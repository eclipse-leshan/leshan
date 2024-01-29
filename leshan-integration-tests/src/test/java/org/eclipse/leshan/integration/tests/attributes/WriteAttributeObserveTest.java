/*******************************************************************************
 * Copyright (c) 2023 Sierra Wireless and others.
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
 *******************************************************************************/

package org.eclipse.leshan.integration.tests.attributes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.leshan.core.util.TestLwM2mId.INTEGER_VALUE;
import static org.eclipse.leshan.core.util.TestLwM2mId.TEST_OBJECT;
import static org.eclipse.leshan.integration.tests.util.LeshanTestClientBuilder.givenClientUsing;
import static org.eclipse.leshan.integration.tests.util.assertion.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import org.eclipse.leshan.core.endpoint.Protocol;
import org.eclipse.leshan.core.link.lwm2m.attributes.LwM2mAttributeSet;
import org.eclipse.leshan.core.link.lwm2m.attributes.LwM2mAttributes;
import org.eclipse.leshan.core.node.LwM2mPath;
import org.eclipse.leshan.core.node.LwM2mSingleResource;
import org.eclipse.leshan.core.observation.SingleObservation;
import org.eclipse.leshan.core.request.ObserveRequest;
import org.eclipse.leshan.core.request.WriteAttributesRequest;
import org.eclipse.leshan.core.request.WriteRequest;
import org.eclipse.leshan.core.response.LwM2mResponse;
import org.eclipse.leshan.core.response.ObserveResponse;
import org.eclipse.leshan.core.response.WriteAttributesResponse;
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
public class WriteAttributeObserveTest {

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
                arguments(Protocol.COAP, "Californium", "Californium") // , //
//                arguments(Protocol.COAP, "java-coap", "Californium"), //
//                arguments(Protocol.COAP, "Californium", "java-coap"), //
//                arguments(Protocol.COAP, "java-coap", "java-coap")
        );
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
    public void test_pmin(Protocol givenProtocol, String givenClientEndpointProvider,
            String givenServerEndpointProvider) throws InterruptedException {

        Long pmin = 1l; // seconds

        // Set attribute
        WriteAttributesResponse writeAttributeResponse = server.send(currentRegistration,
                new WriteAttributesRequest(TEST_OBJECT, 0, INTEGER_VALUE, new LwM2mAttributeSet( //
                        LwM2mAttributes.create(LwM2mAttributes.MINIMUM_PERIOD, pmin) //
                )));
        assertThat(writeAttributeResponse).isSuccess();

        // Set observe relation
        ObserveResponse observeResponse = server.send(currentRegistration,
                new ObserveRequest(TEST_OBJECT, 0, INTEGER_VALUE));
        assertThat(observeResponse).isSuccess();
        SingleObservation observation = observeResponse.getObservation();
        assertThat(observation.getPath()).isEqualTo(new LwM2mPath(TEST_OBJECT, 0, INTEGER_VALUE));
        server.waitForNewObservation(observation);

        // Trigger new notification (changing value using Write Request)
        LwM2mResponse writeResponse = server.send(currentRegistration,
                new WriteRequest(TEST_OBJECT, 0, INTEGER_VALUE, 50l));
        assertThat(writeResponse).isSuccess();

        // Verify Behavior
        server.ensureNoNotification(observation, (int) (TimeUnit.MILLISECONDS.convert(pmin, TimeUnit.SECONDS) * 0.8),
                TimeUnit.MILLISECONDS);
        ObserveResponse response = server.waitForNotificationOf(observation,
                (int) (TimeUnit.MILLISECONDS.convert(pmin, TimeUnit.SECONDS) * 0.3), TimeUnit.MILLISECONDS);
        assertThat(response.getContent()).isEqualTo(LwM2mSingleResource.newIntegerResource(INTEGER_VALUE, 50l));
        assertThat(response).hasValidUnderlyingResponseFor(givenServerEndpointProvider);
    }

    @TestAllTransportLayer
    public void test_pmax(Protocol givenProtocol, String givenClientEndpointProvider,
            String givenServerEndpointProvider) throws InterruptedException {

        Long pmax = 1l; // seconds

        // Set attribute
        WriteAttributesResponse writeAttributeResponse = server.send(currentRegistration,
                new WriteAttributesRequest(TEST_OBJECT, 0, INTEGER_VALUE, new LwM2mAttributeSet( //
                        LwM2mAttributes.create(LwM2mAttributes.MAXIMUM_PERIOD, pmax) //
                )));
        assertThat(writeAttributeResponse).isSuccess();

        // Set observe relation
        ObserveResponse observeResponse = server.send(currentRegistration,
                new ObserveRequest(TEST_OBJECT, 0, INTEGER_VALUE));
        assertThat(observeResponse).isSuccess();
        SingleObservation observation = observeResponse.getObservation();
        assertThat(observation.getPath()).isEqualTo(new LwM2mPath(TEST_OBJECT, 0, INTEGER_VALUE));
        server.waitForNewObservation(observation);

        // Do nothing to trigger notification

        // Verify Behavior
        server.ensureNoNotification(observation, (int) (TimeUnit.MILLISECONDS.convert(pmax, TimeUnit.SECONDS) * 0.8),
                TimeUnit.MILLISECONDS);
        ObserveResponse response = server.waitForNotificationOf(observation,
                (int) (TimeUnit.MILLISECONDS.convert(pmax, TimeUnit.SECONDS) * 0.3), TimeUnit.MILLISECONDS);
        assertThat(response.getContent()).isEqualTo(LwM2mSingleResource.newIntegerResource(INTEGER_VALUE, 1024l));
        assertThat(response).hasValidUnderlyingResponseFor(givenServerEndpointProvider);

    }

    @TestAllTransportLayer
    public void test_lt(Protocol givenProtocol, String givenClientEndpointProvider, String givenServerEndpointProvider)
            throws InterruptedException {

        Double lt = 500d;

        // Set attribute
        WriteAttributesResponse writeAttributeResponse = server.send(currentRegistration,
                new WriteAttributesRequest(TEST_OBJECT, 0, INTEGER_VALUE, new LwM2mAttributeSet( //
                        LwM2mAttributes.create(LwM2mAttributes.LESSER_THAN, lt) //
                )));
        assertThat(writeAttributeResponse).isSuccess();

        // Set observe relation
        ObserveResponse observeResponse = server.send(currentRegistration,
                new ObserveRequest(TEST_OBJECT, 0, INTEGER_VALUE));
        assertThat(observeResponse).isSuccess();
        SingleObservation observation = observeResponse.getObservation();
        assertThat(observation.getPath()).isEqualTo(new LwM2mPath(TEST_OBJECT, 0, INTEGER_VALUE));
        server.waitForNewObservation(observation);

        // Change value which doesn't cross the less_than limit (initial value 1024)
        LwM2mResponse writeResponse = server.send(currentRegistration,
                new WriteRequest(TEST_OBJECT, 0, INTEGER_VALUE, 800l));
        assertThat(writeResponse).isSuccess();

        // Verify Behavior
        server.ensureNoNotification(observation, 500, TimeUnit.MILLISECONDS);

        // Change value which crosses the less_than limit
        writeResponse = server.send(currentRegistration, new WriteRequest(TEST_OBJECT, 0, INTEGER_VALUE, 450l));
        assertThat(writeResponse).isSuccess();

        // Verify Behavior
        ObserveResponse response = server.waitForNotificationOf(observation);
        assertThat(response.getContent()).isEqualTo(LwM2mSingleResource.newIntegerResource(INTEGER_VALUE, 450l));
        assertThat(response).hasValidUnderlyingResponseFor(givenServerEndpointProvider);
        server.ensureNoNotification(observation, 500, TimeUnit.MILLISECONDS);

    }

    @TestAllTransportLayer
    public void test_gt(Protocol givenProtocol, String givenClientEndpointProvider, String givenServerEndpointProvider)
            throws InterruptedException {

        Double gt = 2000d;

        // Set attribute
        WriteAttributesResponse writeAttributeResponse = server.send(currentRegistration,
                new WriteAttributesRequest(TEST_OBJECT, 0, INTEGER_VALUE, new LwM2mAttributeSet( //
                        LwM2mAttributes.create(LwM2mAttributes.GREATER_THAN, gt) //
                )));
        assertThat(writeAttributeResponse).isSuccess();

        // Set observe relation
        ObserveResponse observeResponse = server.send(currentRegistration,
                new ObserveRequest(TEST_OBJECT, 0, INTEGER_VALUE));
        assertThat(observeResponse).isSuccess();
        SingleObservation observation = observeResponse.getObservation();
        assertThat(observation.getPath()).isEqualTo(new LwM2mPath(TEST_OBJECT, 0, INTEGER_VALUE));
        server.waitForNewObservation(observation);

        // Change value which doesn't cross the greater_than limit (initial value 1024)
        LwM2mResponse writeResponse = server.send(currentRegistration,
                new WriteRequest(TEST_OBJECT, 0, INTEGER_VALUE, 1500l));
        assertThat(writeResponse).isSuccess();

        // Verify Behavior
        server.ensureNoNotification(observation, 500, TimeUnit.MILLISECONDS);

        // Change value which crosses the greater_than limit
        writeResponse = server.send(currentRegistration, new WriteRequest(TEST_OBJECT, 0, INTEGER_VALUE, 3000l));
        assertThat(writeResponse).isSuccess();

        // Verify Behavior
        ObserveResponse response = server.waitForNotificationOf(observation);
        assertThat(response.getContent()).isEqualTo(LwM2mSingleResource.newIntegerResource(INTEGER_VALUE, 3000l));
        assertThat(response).hasValidUnderlyingResponseFor(givenServerEndpointProvider);
        server.ensureNoNotification(observation, 500, TimeUnit.MILLISECONDS);
    }

    @TestAllTransportLayer
    public void test_st_with_positive_gap(Protocol givenProtocol, String givenClientEndpointProvider,
            String givenServerEndpointProvider) throws InterruptedException {

        Double st = 200d;

        // Set attribute
        WriteAttributesResponse writeAttributeResponse = server.send(currentRegistration,
                new WriteAttributesRequest(TEST_OBJECT, 0, INTEGER_VALUE, new LwM2mAttributeSet( //
                        LwM2mAttributes.create(LwM2mAttributes.STEP, st) //
                )));
        assertThat(writeAttributeResponse).isSuccess();

        // Set observe relation
        ObserveResponse observeResponse = server.send(currentRegistration,
                new ObserveRequest(TEST_OBJECT, 0, INTEGER_VALUE));
        assertThat(observeResponse).isSuccess();
        SingleObservation observation = observeResponse.getObservation();
        assertThat(observation.getPath()).isEqualTo(new LwM2mPath(TEST_OBJECT, 0, INTEGER_VALUE));
        server.waitForNewObservation(observation);

        // Change value which isn't a big enough step (initial value 1024)
        LwM2mResponse writeResponse = server.send(currentRegistration,
                new WriteRequest(TEST_OBJECT, 0, INTEGER_VALUE, 1124l));
        assertThat(writeResponse).isSuccess();

        // Verify Behavior
        server.ensureNoNotification(observation, 500, TimeUnit.MILLISECONDS);

        // Change value which crosses the less_than limit
        writeResponse = server.send(currentRegistration, new WriteRequest(TEST_OBJECT, 0, INTEGER_VALUE, 1225l));
        assertThat(writeResponse).isSuccess();

        // Verify Behavior
        ObserveResponse response = server.waitForNotificationOf(observation);
        assertThat(response.getContent()).isEqualTo(LwM2mSingleResource.newIntegerResource(INTEGER_VALUE, 1225l));
        assertThat(response).hasValidUnderlyingResponseFor(givenServerEndpointProvider);
        server.ensureNoNotification(observation, 500, TimeUnit.MILLISECONDS);

    }

    @TestAllTransportLayer
    public void test_st_with_negative_gap(Protocol givenProtocol, String givenClientEndpointProvider,
            String givenServerEndpointProvider) throws InterruptedException {

        Double st = 200d;

        // Set attribute
        WriteAttributesResponse writeAttributeResponse = server.send(currentRegistration,
                new WriteAttributesRequest(TEST_OBJECT, 0, INTEGER_VALUE, new LwM2mAttributeSet( //
                        LwM2mAttributes.create(LwM2mAttributes.STEP, st) //
                )));
        assertThat(writeAttributeResponse).isSuccess();

        // Set observe relation
        ObserveResponse observeResponse = server.send(currentRegistration,
                new ObserveRequest(TEST_OBJECT, 0, INTEGER_VALUE));
        assertThat(observeResponse).isSuccess();
        SingleObservation observation = observeResponse.getObservation();
        assertThat(observation.getPath()).isEqualTo(new LwM2mPath(TEST_OBJECT, 0, INTEGER_VALUE));
        server.waitForNewObservation(observation);

        // Change value which isn't a big enough step (initial value 1024)
        LwM2mResponse writeResponse = server.send(currentRegistration,
                new WriteRequest(TEST_OBJECT, 0, INTEGER_VALUE, 924l));
        assertThat(writeResponse).isSuccess();

        // Verify Behavior
        server.ensureNoNotification(observation, 500, TimeUnit.MILLISECONDS);

        // Change value which crosses the less_than limit
        writeResponse = server.send(currentRegistration, new WriteRequest(TEST_OBJECT, 0, INTEGER_VALUE, 823l));
        assertThat(writeResponse).isSuccess();

        // Verify Behavior
        ObserveResponse response = server.waitForNotificationOf(observation);
        assertThat(response.getContent()).isEqualTo(LwM2mSingleResource.newIntegerResource(INTEGER_VALUE, 823l));
        assertThat(response).hasValidUnderlyingResponseFor(givenServerEndpointProvider);
        server.ensureNoNotification(observation, 500, TimeUnit.MILLISECONDS);

    }
}
