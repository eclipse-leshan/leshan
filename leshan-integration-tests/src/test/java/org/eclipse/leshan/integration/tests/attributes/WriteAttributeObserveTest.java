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

package org.eclipse.leshan.integration.tests.attributes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.leshan.core.util.TestLwM2mId.FLOAT_VALUE;
import static org.eclipse.leshan.core.util.TestLwM2mId.INTEGER_VALUE;
import static org.eclipse.leshan.core.util.TestLwM2mId.MULTIPLE_INTEGER_VALUE;
import static org.eclipse.leshan.core.util.TestLwM2mId.TEST_OBJECT;
import static org.eclipse.leshan.core.util.TestLwM2mId.UNSIGNED_INTEGER_VALUE;
import static org.eclipse.leshan.integration.tests.util.LeshanTestClientBuilder.givenClientUsing;
import static org.eclipse.leshan.integration.tests.util.assertion.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import org.eclipse.leshan.core.ResponseCode;
import jdk.nashorn.internal.ir.annotations.Ignore;
import org.eclipse.leshan.core.endpoint.Protocol;
import org.eclipse.leshan.core.link.lwm2m.attributes.LwM2mAttributeSet;
import org.eclipse.leshan.core.link.lwm2m.attributes.LwM2mAttributes;
import org.eclipse.leshan.core.node.LwM2mNode;
import org.eclipse.leshan.core.node.LwM2mPath;
import org.eclipse.leshan.core.node.LwM2mSingleResource;
import org.eclipse.leshan.core.observation.SingleObservation;
import org.eclipse.leshan.core.request.ContentFormat;
import org.eclipse.leshan.core.request.ObserveRequest;
import org.eclipse.leshan.core.request.WriteAttributesRequest;
import org.eclipse.leshan.core.request.WriteRequest;
import org.eclipse.leshan.core.request.WriteRequest.Mode;
import org.eclipse.leshan.core.response.LwM2mResponse;
import org.eclipse.leshan.core.response.ObserveResponse;
import org.eclipse.leshan.core.response.WriteAttributesResponse;
import org.eclipse.leshan.core.util.datatype.ULong;
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
                arguments(Protocol.COAP, "Californium", "Californium"), //
                arguments(Protocol.COAP, "java-coap", "Californium"), //
                arguments(Protocol.COAP, "Californium", "java-coap"), //
                arguments(Protocol.COAP, "java-coap", "java-coap"));
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
    public void test_pmin_and_pmax(Protocol givenProtocol, String givenClientEndpointProvider,
                                   String givenServerEndpointProvider) throws InterruptedException {

        Long pmin = 1l; // seconds
        Long pmax = 10l; // seconds

        // Setting attribute
        WriteAttributesResponse writeAttributeResponse = server.send(currentRegistration,
                new WriteAttributesRequest(TEST_OBJECT, 0, INTEGER_VALUE,
                        new LwM2mAttributeSet(LwM2mAttributes.create(LwM2mAttributes.MINIMUM_PERIOD, pmin),
                                LwM2mAttributes.create(LwM2mAttributes.MAXIMUM_PERIOD, pmax))));
        assertThat(writeAttributeResponse).isSuccess();

        // Setting observe relation
        ObserveResponse observeResponse = server.send(currentRegistration,
                new ObserveRequest(TEST_OBJECT, 0, INTEGER_VALUE));
        assertThat(observeResponse).isSuccess();
        SingleObservation observation = observeResponse.getObservation();
        assertThat(observation.getPath()).isEqualTo(new LwM2mPath(TEST_OBJECT, 0, INTEGER_VALUE));
        server.waitForNewObservation(observation);

        // Trigger new notification (changing value using Write Request)
        LwM2mResponse writeResponse = server.send(currentRegistration,
                new WriteRequest(TEST_OBJECT, 0, INTEGER_VALUE, 100l));
        assertThat(writeResponse).isSuccess();

        // Verify Behavior
        server.ensureNoNotification(observation, (int) (TimeUnit.MILLISECONDS.convert(pmin, TimeUnit.SECONDS) * 0.8),
                TimeUnit.MILLISECONDS);

        ObserveResponse response = server.waitForNotificationOf(observation,
                (int) (TimeUnit.MILLISECONDS.convert(pmin, TimeUnit.SECONDS) * 0.3), TimeUnit.MILLISECONDS);

        // Verify response
        assertThat(response.getContent()).isEqualTo(LwM2mSingleResource.newIntegerResource(INTEGER_VALUE, 100));
        assertThat(response).hasValidUnderlyingResponseFor(givenServerEndpointProvider);
    }

    @TestAllTransportLayer
    public void test_lt_integer_resource(Protocol givenProtocol, String givenClientEndpointProvider,
            String givenServerEndpointProvider) throws InterruptedException {

        test_first_change_didnt_trigger_then_second_did(givenServerEndpointProvider, //
                // target LWM2M node : initial value is 1024
                new LwM2mPath(TEST_OBJECT, 0, INTEGER_VALUE),
                // "LESSER THAN" attribute value
                new LwM2mAttributeSet(LwM2mAttributes.create(LwM2mAttributes.LESSER_THAN, 500d)),
                // value which doesn't cross the less_than limit
                LwM2mSingleResource.newIntegerResource(INTEGER_VALUE, 800l),
                // value which crosses the less_than limit
                LwM2mSingleResource.newIntegerResource(INTEGER_VALUE, 450l));
    }

    @TestAllTransportLayer
    public void test_lt_float_resource(Protocol givenProtocol, String givenClientEndpointProvider,
            String givenServerEndpointProvider) throws InterruptedException {

        test_first_change_didnt_trigger_then_second_did(givenServerEndpointProvider, //
                // target LWM2M node : initial value is 3.14159
                new LwM2mPath(TEST_OBJECT, 0, FLOAT_VALUE),
                // "LESSER THAN" attribute value
                new LwM2mAttributeSet(LwM2mAttributes.create(LwM2mAttributes.LESSER_THAN, 2.5d)),
                // value which doesn't cross the less_than limit
                LwM2mSingleResource.newFloatResource(FLOAT_VALUE, 4.3d),
                // value which crosses the less_than limit
                LwM2mSingleResource.newFloatResource(FLOAT_VALUE, 2.1d));
    }

    @TestAllTransportLayer
    public void test_lt_unsigned_integer_resource(Protocol givenProtocol, String givenClientEndpointProvider,
            String givenServerEndpointProvider) throws InterruptedException {

        test_first_change_didnt_trigger_then_second_did(givenServerEndpointProvider, //
                // target LWM2M node : initial value is 9223372036854775808
                new LwM2mPath(TEST_OBJECT, 0, UNSIGNED_INTEGER_VALUE),
                // "LESSER THAN" attribute value
                new LwM2mAttributeSet(LwM2mAttributes.create(LwM2mAttributes.LESSER_THAN, 500d)),
                // value which doesn't cross the less_than limit
                LwM2mSingleResource.newUnsignedIntegerResource(UNSIGNED_INTEGER_VALUE, ULong.valueOf(800l)),
                // value which crosses the less_than limit
                LwM2mSingleResource.newUnsignedIntegerResource(UNSIGNED_INTEGER_VALUE, ULong.valueOf(450l)));
    }

    @TestAllTransportLayer
    public void test_gt_integer_resource(Protocol givenProtocol, String givenClientEndpointProvider,
            String givenServerEndpointProvider) throws InterruptedException {

        test_first_change_didnt_trigger_then_second_did(givenServerEndpointProvider, //
                // target LWM2M node : initial value is 1024
                new LwM2mPath(TEST_OBJECT, 0, INTEGER_VALUE),
                // "GREATER THAN" attribute value
                new LwM2mAttributeSet(LwM2mAttributes.create(LwM2mAttributes.GREATER_THAN, 2000d)),
                // value which doesn't cross the greater_than limit
                LwM2mSingleResource.newIntegerResource(INTEGER_VALUE, 1500l),
                // value which crosses the greater_than limit
                LwM2mSingleResource.newIntegerResource(INTEGER_VALUE, 3000l));
    }

    @TestAllTransportLayer
    public void test_gt_float_resource(Protocol givenProtocol, String givenClientEndpointProvider,
            String givenServerEndpointProvider) throws InterruptedException {

        test_first_change_didnt_trigger_then_second_did(givenServerEndpointProvider, //
                // target LWM2M node : initial value is 3.14159
                new LwM2mPath(TEST_OBJECT, 0, FLOAT_VALUE),
                // "GREATER THAN" attribute value
                new LwM2mAttributeSet(LwM2mAttributes.create(LwM2mAttributes.GREATER_THAN, 5.5d)),
                // value which doesn't cross the greater_than limit
                LwM2mSingleResource.newFloatResource(FLOAT_VALUE, 4.3d),
                // value which crosses the greater_than limit
                LwM2mSingleResource.newFloatResource(FLOAT_VALUE, 5.6d));
    }

    @TestAllTransportLayer
    public void test_gt_unsigned_integer_resource(Protocol givenProtocol, String givenClientEndpointProvider,
            String givenServerEndpointProvider) throws InterruptedException {

        test_first_change_didnt_trigger_then_second_did(givenServerEndpointProvider, //
                // target LWM2M node : initial value is 9223372036854775808
                new LwM2mPath(TEST_OBJECT, 0, UNSIGNED_INTEGER_VALUE),
                // "GREATER THAN" attribute value
                new LwM2mAttributeSet(LwM2mAttributes.create(LwM2mAttributes.GREATER_THAN, 10000000000000000000d)),
                // value which doesn't cross the greater_than limit
                LwM2mSingleResource.newUnsignedIntegerResource(UNSIGNED_INTEGER_VALUE,
                        ULong.valueOf("9999999999999990000")),
                // value which crosses the greater_than limit
                LwM2mSingleResource.newUnsignedIntegerResource(UNSIGNED_INTEGER_VALUE,
                        ULong.valueOf("10000000000000010000")));
    }

    @TestAllTransportLayer
    public void test_lt_and_gt(Protocol givenProtocol, String givenClientEndpointProvider,
                               String givenServerEndpointProvider) throws InterruptedException {

        Double lt = 500d;
        Double gt = 2000d;

        // Setting attribute
        WriteAttributesResponse writeAttributeResponse = server.send(currentRegistration,
                new WriteAttributesRequest(TEST_OBJECT, 0, INTEGER_VALUE,
                        new LwM2mAttributeSet(LwM2mAttributes.create(LwM2mAttributes.LESSER_THAN, lt),
                                LwM2mAttributes.create(LwM2mAttributes.GREATER_THAN, gt))));
        assertThat(writeAttributeResponse).isSuccess();

        // Setting observe relation
        ObserveResponse observeResponse = server.send(currentRegistration,
                new ObserveRequest(TEST_OBJECT, 0, INTEGER_VALUE));
        assertThat(observeResponse).isSuccess();

        SingleObservation observation = observeResponse.getObservation();
        assertThat(observation.getPath()).isEqualTo(new LwM2mPath(TEST_OBJECT, 0, INTEGER_VALUE));

        server.waitForNewObservation(observation);

        // Changing value using WriteRequest (between lt and gt)
        LwM2mResponse writeResponse = server.send(currentRegistration,
                new WriteRequest(TEST_OBJECT, 0, INTEGER_VALUE, 1200l));
        assertThat(writeResponse).isSuccess();

        // Verify Behavior
        server.ensureNoNotification(observation, 500, TimeUnit.MILLISECONDS);

        // Changing value which crosses the lt limit
        writeResponse = server.send(currentRegistration, new WriteRequest(TEST_OBJECT, 0, INTEGER_VALUE, 400));
        assertThat(writeResponse).isSuccess();

        // Verify Behavior
        ObserveResponse response = server.waitForNotificationOf(observation);
        assertThat(response.getContent()).isEqualTo(LwM2mSingleResource.newIntegerResource(INTEGER_VALUE, 400));
        assertThat(response).hasValidUnderlyingResponseFor(givenServerEndpointProvider);
        server.ensureNoNotification(observation, 500, TimeUnit.MILLISECONDS);
    }

    @TestAllTransportLayer
    public void test_st_with_positive_gap_on_integer_resource(Protocol givenProtocol,
            String givenClientEndpointProvider, String givenServerEndpointProvider) throws InterruptedException {

        test_first_change_didnt_trigger_then_second_did(givenServerEndpointProvider, //
                // target LWM2M node : initial value is 1024
                new LwM2mPath(TEST_OBJECT, 0, INTEGER_VALUE),
                // "STEP" attribute value
                new LwM2mAttributeSet(LwM2mAttributes.create(LwM2mAttributes.STEP, 200d)),
                // First change value which isn't a big enough step
                LwM2mSingleResource.newIntegerResource(INTEGER_VALUE, 1124l),
                // Second change value which is big enough
                LwM2mSingleResource.newIntegerResource(INTEGER_VALUE, 1225l));
    }

    @TestAllTransportLayer
    public void test_with_positive_gap_on_float_resource(Protocol givenProtocol, String givenClientEndpointProvider,
            String givenServerEndpointProvider) throws InterruptedException {

        test_first_change_didnt_trigger_then_second_did(givenServerEndpointProvider, //
                // target LWM2M node : initial value is 3.14159
                new LwM2mPath(TEST_OBJECT, 0, FLOAT_VALUE),
                // "STEP" attribute value
                new LwM2mAttributeSet(LwM2mAttributes.create(LwM2mAttributes.STEP, 200d)),
                // First change value which isn't a big enough step
                LwM2mSingleResource.newFloatResource(FLOAT_VALUE, 103.14159d),
                // Second change value which is big enough
                LwM2mSingleResource.newFloatResource(FLOAT_VALUE, 204.14159d));
    }

    @TestAllTransportLayer
    public void test_with_positive_gap_on_unsigned_integer_resource(Protocol givenProtocol,
            String givenClientEndpointProvider, String givenServerEndpointProvider) throws InterruptedException {

        test_first_change_didnt_trigger_then_second_did(givenServerEndpointProvider, //
                // target LWM2M node : initial value is 9223372036854775808
                new LwM2mPath(TEST_OBJECT, 0, UNSIGNED_INTEGER_VALUE),
                // "STEP" attribute value
                new LwM2mAttributeSet(LwM2mAttributes.create(LwM2mAttributes.STEP, 20d)),
                // First change value which isn't a big enough step
                LwM2mSingleResource.newUnsignedIntegerResource(UNSIGNED_INTEGER_VALUE,
                        ULong.valueOf("9223372036854775818")),
                // Second change value which is big enough
                LwM2mSingleResource.newUnsignedIntegerResource(UNSIGNED_INTEGER_VALUE,
                        ULong.valueOf("9223372036854775828")));
    }

    @TestAllTransportLayer
    public void test_st_with_negative_gap_on_integer_resource(Protocol givenProtocol,
            String givenClientEndpointProvider, String givenServerEndpointProvider) throws InterruptedException {

        test_first_change_didnt_trigger_then_second_did(givenServerEndpointProvider, //
                // target LWM2M node : initial value is 1024
                new LwM2mPath(TEST_OBJECT, 0, INTEGER_VALUE),
                // "STEP" attribute value
                new LwM2mAttributeSet(LwM2mAttributes.create(LwM2mAttributes.STEP, 200d)),
                // First change value which isn't a big enough step
                LwM2mSingleResource.newIntegerResource(INTEGER_VALUE, 924l),
                // Second change value which is big enough
                LwM2mSingleResource.newIntegerResource(INTEGER_VALUE, 823l));
    }

    @TestAllTransportLayer
    public void test_with_negative_gap_on_float_resource(Protocol givenProtocol, String givenClientEndpointProvider,
            String givenServerEndpointProvider) throws InterruptedException {

        test_first_change_didnt_trigger_then_second_did(givenServerEndpointProvider, //
                // target LWM2M node : initial value is 3.14159
                new LwM2mPath(TEST_OBJECT, 0, FLOAT_VALUE),
                // "STEP" attribute value
                new LwM2mAttributeSet(LwM2mAttributes.create(LwM2mAttributes.STEP, 200d)),
                // First change value which isn't a big enough step
                LwM2mSingleResource.newFloatResource(FLOAT_VALUE, -103.14159d),
                // Second change value which is big enough
                LwM2mSingleResource.newFloatResource(FLOAT_VALUE, -203.14159d));
    }

    @TestAllTransportLayer
    public void test_with_negative_gap_on_unsigned_integer_resource(Protocol givenProtocol,
            String givenClientEndpointProvider, String givenServerEndpointProvider) throws InterruptedException {

        test_first_change_didnt_trigger_then_second_did(givenServerEndpointProvider, //
                // target LWM2M node : initial value is 9223372036854775808
                new LwM2mPath(TEST_OBJECT, 0, UNSIGNED_INTEGER_VALUE),
                // "STEP" attribute value
                new LwM2mAttributeSet(LwM2mAttributes.create(LwM2mAttributes.STEP, 20d)),
                // First change value which isn't a big enough step
                LwM2mSingleResource.newUnsignedIntegerResource(UNSIGNED_INTEGER_VALUE,
                        ULong.valueOf("9223372036854775800")),
                // Second change value which is big enough
                LwM2mSingleResource.newUnsignedIntegerResource(UNSIGNED_INTEGER_VALUE,
                        ULong.valueOf("9223372036854775758")));
    }

    @Ignore
    @TestAllTransportLayer
    public void test_epmin(Protocol givenProtocol, String givenClientEndpointProvider,
                           String givenServerEndpointProvider) throws InterruptedException {

        Long epmin = 5l; // seconds

        // Setting attribute
        WriteAttributesResponse writeAttributeResponse = server.send(currentRegistration,
                new WriteAttributesRequest(TEST_OBJECT, 0, INTEGER_VALUE, new LwM2mAttributeSet( //
                        LwM2mAttributes.create(LwM2mAttributes.EVALUATE_MAXIMUM_PERIOD, epmin) //
                )));
        assertThat(writeAttributeResponse).isSuccess();

        // Setting observe relation
        ObserveResponse observeResponse = server.send(currentRegistration,
                new ObserveRequest(TEST_OBJECT, 0, INTEGER_VALUE));
        assertThat(observeResponse).isSuccess();
        SingleObservation observation = observeResponse.getObservation();
        assertThat(observation.getPath()).isEqualTo(new LwM2mPath(TEST_OBJECT, 0, INTEGER_VALUE));
        server.waitForNewObservation(observation);

        // Verify Behavior
        server.ensureNoNotification(observation, (int) (TimeUnit.MILLISECONDS.convert(epmin, TimeUnit.SECONDS)),
                TimeUnit.MILLISECONDS);
        ObserveResponse response = server.waitForNotificationOf(observation,
                (int) (TimeUnit.MILLISECONDS.convert(epmin, TimeUnit.SECONDS) * 0.5), TimeUnit.MILLISECONDS);
        assertThat(response.getContent()).isEqualTo(LwM2mSingleResource.newIntegerResource(INTEGER_VALUE, 1024l));
        assertThat(response).hasValidUnderlyingResponseFor(givenServerEndpointProvider);
    }

    @Ignore
    @TestAllTransportLayer
    public void test_epmax(Protocol givenProtocol, String givenClientEndpointProvider,
                           String givenServerEndpointProvider) throws InterruptedException {

        Long epmax = 10l; // seconds

        // Setting attribute
        WriteAttributesResponse writeAttributeResponse = server.send(currentRegistration,
                new WriteAttributesRequest(TEST_OBJECT, 0, INTEGER_VALUE, new LwM2mAttributeSet( //
                        LwM2mAttributes.create(LwM2mAttributes.EVALUATE_MAXIMUM_PERIOD, epmax) //
                )));
        assertThat(writeAttributeResponse).isSuccess();

        // Setting observe relation
        ObserveResponse observeResponse = server.send(currentRegistration,
                new ObserveRequest(TEST_OBJECT, 0, INTEGER_VALUE));
        assertThat(observeResponse).isSuccess();
        SingleObservation observation = observeResponse.getObservation();
        assertThat(observation.getPath()).isEqualTo(new LwM2mPath(TEST_OBJECT, 0, INTEGER_VALUE));
        server.waitForNewObservation(observation);

        // Verify Behavior
        server.ensureNoNotification(observation, (int) (TimeUnit.MILLISECONDS.convert(epmax, TimeUnit.SECONDS)),
                TimeUnit.MILLISECONDS);
        ObserveResponse response = server.waitForNotificationOf(observation,
                (int) (TimeUnit.MILLISECONDS.convert(epmax, TimeUnit.SECONDS) * 1.5), TimeUnit.MILLISECONDS);
        assertThat(response.getContent()).isEqualTo(LwM2mSingleResource.newIntegerResource(INTEGER_VALUE, 1024l));
        assertThat(response).hasValidUnderlyingResponseFor(givenServerEndpointProvider);
    }

    @TestAllTransportLayer
    public void test_several_attributes(Protocol givenProtocol, String givenClientEndpointProvider,
                                        String givenServerEndpointProvider) throws InterruptedException {

        Double lt = 500d;
        Double gt = 2000d;
        Long pmax = 10l;

        // Setting attribute
        WriteAttributesResponse writeAttributeResponse = server.send(currentRegistration,
                new WriteAttributesRequest(TEST_OBJECT, 0, INTEGER_VALUE,
                        new LwM2mAttributeSet(LwM2mAttributes.create(LwM2mAttributes.LESSER_THAN, lt),
                                LwM2mAttributes.create(LwM2mAttributes.GREATER_THAN, gt),
                                LwM2mAttributes.create(LwM2mAttributes.MAXIMUM_PERIOD, pmax))));
        assertThat(writeAttributeResponse).isSuccess();

        // Setting observe relation
        ObserveResponse observeResponse = server.send(currentRegistration,
                new ObserveRequest(TEST_OBJECT, 0, INTEGER_VALUE));
        assertThat(observeResponse).isSuccess();

        SingleObservation observation = observeResponse.getObservation();
        assertThat(observation.getPath()).isEqualTo(new LwM2mPath(TEST_OBJECT, 0, INTEGER_VALUE));

        server.waitForNewObservation(observation);

        // Changing value using WriteRequest
        LwM2mResponse writeResponse = server.send(currentRegistration,
                new WriteRequest(TEST_OBJECT, 0, INTEGER_VALUE, 1200));
        assertThat(writeResponse).isSuccess();

        // Verify Behavior
        server.ensureNoNotification(observation, 500, TimeUnit.MILLISECONDS);

        // Changing value which crosses the lt limit
        writeResponse = server.send(currentRegistration, new WriteRequest(TEST_OBJECT, 0, INTEGER_VALUE, 400));
        assertThat(writeResponse).isSuccess();

        // Verify Behavior
        ObserveResponse response = server.waitForNotificationOf(observation,
                (int) (TimeUnit.MILLISECONDS.convert(pmax, TimeUnit.SECONDS) * 0.3), TimeUnit.MILLISECONDS);
        assertThat(response.getContent()).isEqualTo(LwM2mSingleResource.newIntegerResource(INTEGER_VALUE, 400));
        assertThat(response).hasValidUnderlyingResponseFor(givenServerEndpointProvider);
        server.ensureNoNotification(observation, 500, TimeUnit.MILLISECONDS);
    }

    @TestAllTransportLayer
    public void test_pmin_equals_pmax(Protocol givenProtocol, String givenClientEndpointProvider,
                                      String givenServerEndpointProvider) throws InterruptedException {

        Long pmin = 1l; // seconds
        Long pmax = 1l; // seconds

        // Setting attribute
        WriteAttributesResponse writeAttributeResponse = server.send(currentRegistration,
                new WriteAttributesRequest(TEST_OBJECT, 0, INTEGER_VALUE,
                        new LwM2mAttributeSet(LwM2mAttributes.create(LwM2mAttributes.MINIMUM_PERIOD, pmin),
                                LwM2mAttributes.create(LwM2mAttributes.MAXIMUM_PERIOD, pmax))));

        // Verify write attribute response
        assertThat(writeAttributeResponse).isSuccess();

        // Setting observe
        ObserveResponse observeResponse = server.send(currentRegistration,
                new ObserveRequest(TEST_OBJECT, 0, INTEGER_VALUE));
        assertThat(observeResponse).isSuccess();

        SingleObservation observation = observeResponse.getObservation();
        assertThat(observation.getPath()).isEqualTo(new LwM2mPath(TEST_OBJECT, 0, INTEGER_VALUE));
        server.waitForNewObservation(observation);

        // Trigger new notification
        LwM2mResponse writeResponse = server.send(currentRegistration,
                new WriteRequest(TEST_OBJECT, 0, INTEGER_VALUE, 30));
        assertThat(writeResponse).isSuccess();

        // Verify
        server.ensureNoNotification(observation, (int) (TimeUnit.MILLISECONDS.convert(pmin, TimeUnit.SECONDS) * 0.8),
                TimeUnit.MILLISECONDS);
        ObserveResponse response = server.waitForNotificationOf(observation,
                (int) (TimeUnit.MILLISECONDS.convert(pmin, TimeUnit.SECONDS) * 0.3), TimeUnit.MILLISECONDS);
        assertThat(response.getContent()).isEqualTo(LwM2mSingleResource.newIntegerResource(INTEGER_VALUE, 30));
        assertThat(response).hasValidUnderlyingResponseFor(givenServerEndpointProvider);
    }

    @TestAllTransportLayer
    public void test_objectId_inheritance(Protocol givenProtocol, String givenClientEndpointProvider,
                                          String givenServerEndpointProvider) throws InterruptedException {

        Double lt = 500d;

        // Set attribute on objectId
        WriteAttributesResponse writeAttributeResponse = server.send(currentRegistration,
                new WriteAttributesRequest(TEST_OBJECT, new LwM2mAttributeSet( //
                        LwM2mAttributes.create(LwM2mAttributes.MINIMUM_PERIOD, 150l) //
                )));
        assertThat(writeAttributeResponse).isSuccess();

        // Set observe relation on TEST_OBJECT
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
    }


    @TestAllTransportLayer
    public void test_objectInstanceId_inheritance(Protocol givenProtocol, String givenClientEndpointProvider,
                                                  String givenServerEndpointProvider) throws InterruptedException {

        Long pmax = 5l;

        // Set attribute
        WriteAttributesResponse writeAttributeResponse = server.send(currentRegistration,
                new WriteAttributesRequest(TEST_OBJECT, 0, new LwM2mAttributeSet( //
                        LwM2mAttributes.create(LwM2mAttributes.MAXIMUM_PERIOD, pmax) //
                )));
        assertThat(writeAttributeResponse).isSuccess();

        // Set observe relation on TEST_OBJECT
        ObserveResponse observeResponse = server.send(currentRegistration,
                new ObserveRequest(TEST_OBJECT, 0, INTEGER_VALUE));
        assertThat(observeResponse).isSuccess();
        SingleObservation observation = observeResponse.getObservation();
        assertThat(observation.getPath()).isEqualTo(new LwM2mPath(TEST_OBJECT, 0, INTEGER_VALUE));
        server.waitForNewObservation(observation);

        // Verify Behavior
        server.ensureNoNotification(observation, (int) (TimeUnit.MILLISECONDS.convert(pmax, TimeUnit.SECONDS) * 0.8),
                TimeUnit.MILLISECONDS);
        ObserveResponse response = server.waitForNotificationOf(observation,
                (int) (TimeUnit.MILLISECONDS.convert(pmax, TimeUnit.SECONDS) * 0.3), TimeUnit.MILLISECONDS);
        assertThat(response.getContent()).isEqualTo(LwM2mSingleResource.newIntegerResource(INTEGER_VALUE, 1024l));
        assertThat(response).hasValidUnderlyingResponseFor(givenServerEndpointProvider);
    }

    protected void test_first_change_didnt_trigger_then_second_did(String givenServerEndpointProvider,
            LwM2mPath targetedResource, LwM2mAttributeSet attributesToWrite,
            LwM2mNode valueWhichDidntTriggerNotification, LwM2mNode valueWhichTriggerNotification)
            throws InterruptedException {

        // Set attribute
        WriteAttributesResponse writeAttributeResponse = server.send(currentRegistration,
                new WriteAttributesRequest(targetedResource, attributesToWrite));
        assertThat(writeAttributeResponse).isSuccess();

        // Set observe relation
        ObserveResponse observeResponse = server.send(currentRegistration, new ObserveRequest(targetedResource));
        assertThat(observeResponse).isSuccess();
        SingleObservation observation = observeResponse.getObservation();
        assertThat(observation.getPath()).isEqualTo(targetedResource);
        server.waitForNewObservation(observation);

        // Change value which should NOT trigger notification
        LwM2mResponse writeResponse = server.send(currentRegistration, new WriteRequest(Mode.REPLACE, ContentFormat.TLV,
                targetedResource, valueWhichDidntTriggerNotification));
        assertThat(writeResponse).isSuccess();

        // Verify Behavior
        server.ensureNoNotification(observation, 500, TimeUnit.MILLISECONDS);

        // Change value which should trigger notification
        writeResponse = server.send(currentRegistration,
                new WriteRequest(Mode.REPLACE, ContentFormat.TLV, targetedResource, valueWhichTriggerNotification));
        assertThat(writeResponse).isSuccess();

        // Verify Behavior
        ObserveResponse response = server.waitForNotificationOf(observation);
        assertThat(response.getContent()).isEqualTo(valueWhichTriggerNotification);
        assertThat(response).hasValidUnderlyingResponseFor(givenServerEndpointProvider);
        server.ensureNoNotification(observation, 500, TimeUnit.MILLISECONDS);
    }

    @TestAllTransportLayer
    public void test_invalid_inheritance_raise_exception(Protocol givenProtocol, String givenClientEndpointProvider,
            String givenServerEndpointProvider) throws InterruptedException {

        // Set attribute pmin at resource level > pmax at resource instance level
        WriteAttributesResponse writeAttributeResponse = server.send(currentRegistration,
                new WriteAttributesRequest(new LwM2mPath(TEST_OBJECT, 0, MULTIPLE_INTEGER_VALUE), new LwM2mAttributeSet( //
                        LwM2mAttributes.create(LwM2mAttributes.MINIMUM_PERIOD, 200l) //
                )));
        assertThat(writeAttributeResponse).isSuccess();

        writeAttributeResponse = server.send(currentRegistration,
                new WriteAttributesRequest(new LwM2mPath(TEST_OBJECT, 0, MULTIPLE_INTEGER_VALUE, 0),
                        new LwM2mAttributeSet( //
                                LwM2mAttributes.create(LwM2mAttributes.MAXIMUM_PERIOD, 100l) //
                        )));
        assertThat(writeAttributeResponse).isSuccess();

        // Set observe relation
        ObserveResponse observeResponse = server.send(currentRegistration,
                new ObserveRequest(new LwM2mPath(TEST_OBJECT, 0, MULTIPLE_INTEGER_VALUE, 0)));
        assertThat(observeResponse).hasCode(ResponseCode.INTERNAL_SERVER_ERROR);

        assertThat(client).hasNoNotificationData();
    }
}
