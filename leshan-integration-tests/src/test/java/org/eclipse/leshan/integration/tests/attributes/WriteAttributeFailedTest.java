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

import static org.eclipse.leshan.integration.tests.util.LeshanTestClientBuilder.givenClientUsing;
import static org.eclipse.leshan.integration.tests.util.LeshanTestServerBuilder.givenServerUsing;
import static org.eclipse.leshan.integration.tests.util.assertion.Assertions.assertThat;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.stream.Stream;

import org.eclipse.leshan.core.ResponseCode;
import org.eclipse.leshan.core.endpoint.Protocol;
import org.eclipse.leshan.core.link.lwm2m.attributes.LwM2mAttributeSet;
import org.eclipse.leshan.core.link.lwm2m.attributes.LwM2mAttributes;
import org.eclipse.leshan.core.node.LwM2mPath;
import org.eclipse.leshan.core.request.WriteAttributesRequest;
import org.eclipse.leshan.core.response.WriteAttributesResponse;
import org.eclipse.leshan.integration.tests.util.LeshanTestClient;
import org.eclipse.leshan.integration.tests.util.LeshanTestServer;
import org.eclipse.leshan.integration.tests.util.junit5.extensions.ArgumentsUtil;
import org.eclipse.leshan.integration.tests.util.junit5.extensions.BeforeEachParameterizedResolver;
import org.eclipse.leshan.server.registration.Registration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

@ExtendWith(BeforeEachParameterizedResolver.class)
public class WriteAttributeFailedTest {

    /*---------------------------------/
     *  Parameterized Tests
     * -------------------------------*/
    @ParameterizedTest(name = "{0} - Client using {1} - Server using {2} - {3} - {4},{5}")
    @MethodSource("transports")
    @Retention(RetentionPolicy.RUNTIME)
    private @interface TestAllCases {
    }

    static Stream<org.junit.jupiter.params.provider.Arguments> transports() {

        Object[][] transports = new Object[][] {
                // ProtocolUsed - Client Endpoint Provider - Server Endpoint Provider
                { Protocol.COAP, "Californium", "Californium" }, //
                { Protocol.COAP, "Californium", "java-coap" }, //
                { Protocol.COAP, "java-coap", "Californium" }, //
                { Protocol.COAP, "java-coap", "java-coap" } };

        Object[][] testCases = new Object[][] { //
                // targeted path - initial state - invalid attributes to write

                { // test pmin > pmax
                        "/3", //
                        new LwM2mAttributeSet(LwM2mAttributes.create(LwM2mAttributes.MINIMUM_PERIOD, 300l)), //
                        new LwM2mAttributeSet(LwM2mAttributes.create(LwM2mAttributes.MAXIMUM_PERIOD, 200l)) //
                }, //
                { // test epmin > epmax
                        "/3", //
                        new LwM2mAttributeSet(LwM2mAttributes.create(LwM2mAttributes.EVALUATE_MINIMUM_PERIOD, 300l)), //
                        new LwM2mAttributeSet(LwM2mAttributes.create(LwM2mAttributes.EVALUATE_MAXIMUM_PERIOD, 200l)) //
                }, //
                { // test gt < lt
                        "/3/0/9", //
                        new LwM2mAttributeSet(LwM2mAttributes.create(LwM2mAttributes.GREATER_THAN, 100d)), //
                        new LwM2mAttributeSet(LwM2mAttributes.create(LwM2mAttributes.LESSER_THAN, 200d)) //
                }, //
                { // test gt == lt
                        "/3/0/9", //
                        new LwM2mAttributeSet(LwM2mAttributes.create(LwM2mAttributes.GREATER_THAN, 200d)), //
                        new LwM2mAttributeSet(LwM2mAttributes.create(LwM2mAttributes.LESSER_THAN, 200d)) //
                }, //
                { // test lt - 2*st > gt
                        "/3/0/9", //
                        new LwM2mAttributeSet(LwM2mAttributes.create(LwM2mAttributes.GREATER_THAN, 11d)), //
                        new LwM2mAttributeSet(LwM2mAttributes.create( //
                                LwM2mAttributes.LESSER_THAN, 10d), //
                                LwM2mAttributes.create(LwM2mAttributes.STEP, 2d)) //
                }, //
                { // test lt - 2*st == gt
                        "/3/0/9", //
                        new LwM2mAttributeSet(LwM2mAttributes.create(LwM2mAttributes.GREATER_THAN, 14d)), //
                        new LwM2mAttributeSet(LwM2mAttributes.create( //
                                LwM2mAttributes.LESSER_THAN, 10d), //
                                LwM2mAttributes.create(LwM2mAttributes.STEP, 2d)) //
                } };

        // for each transport, create 1 test by format.
        return Stream.of(ArgumentsUtil.combine(transports, testCases));
    }

    /*---------------------------------/
     *  Set-up and Tear-down Tests
     * -------------------------------*/

    LeshanTestServer server;
    LeshanTestClient client;
    Registration currentRegistration;

    @BeforeEach
    public void start(Protocol givenProtocol, String givenClientEndpointProvider, String givenServerEndpointProvider,
            String targetedPath, LwM2mAttributeSet initialState, LwM2mAttributeSet invalidAttributes) {
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
    @TestAllCases
    public void test_failing(Protocol givenProtocol, String givenClientEndpointProvider,
            String givenServerEndpointProvider, String targetedPath, LwM2mAttributeSet initialState,
            LwM2mAttributeSet invalidAttributes) throws InterruptedException {

        LwM2mPath path = new LwM2mPath(targetedPath);

        // Set initial state if needed
        if (initialState != null) {
            WriteAttributesResponse writeAttributesResponse = server.send(currentRegistration,
                    new WriteAttributesRequest(path, initialState));
            assertThat(writeAttributesResponse).isSuccess();
        }

        // Write failing attributes
        WriteAttributesResponse writeAttributesResponse = server.send(currentRegistration,
                new WriteAttributesRequest(path, invalidAttributes));
        assertThat(writeAttributesResponse).hasCode(ResponseCode.BAD_REQUEST)
                .hasValidUnderlyingResponseFor(givenServerEndpointProvider);

    }
}
