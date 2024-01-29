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

import static org.eclipse.leshan.core.ResponseCode.CHANGED;
import static org.eclipse.leshan.core.ResponseCode.CONTENT;
import static org.eclipse.leshan.integration.tests.util.LeshanTestClientBuilder.givenClientUsing;
import static org.eclipse.leshan.integration.tests.util.LeshanTestServerBuilder.givenServerUsing;
import static org.eclipse.leshan.integration.tests.util.assertion.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import org.eclipse.leshan.client.object.Device;
import org.eclipse.leshan.core.LwM2mId;
import org.eclipse.leshan.core.endpoint.Protocol;
import org.eclipse.leshan.core.link.lwm2m.attributes.LwM2mAttributeSet;
import org.eclipse.leshan.core.link.lwm2m.attributes.LwM2mAttributes;
import org.eclipse.leshan.core.model.ObjectModel;
import org.eclipse.leshan.core.request.DiscoverRequest;
import org.eclipse.leshan.core.request.WriteAttributesRequest;
import org.eclipse.leshan.core.response.DiscoverResponse;
import org.eclipse.leshan.core.response.WriteAttributesResponse;
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
public class WriteAttributeDiscoverTest {

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
        // arguments(Protocol.COAP, "Californium", "java-coap"), //
        // arguments(Protocol.COAP, "java-coap", "Californium"), //
        // arguments(Protocol.COAP, "java-coap", "java-coap"))
        );
    }

    /*---------------------------------/
     *  Set-up and Tear-down Tests
     * -------------------------------*/

    LeshanTestServer server;
    LeshanTestClient client;
    Registration currentRegistration;

    private static class WriteAttributeTestDevice extends Device {

        private static final List<Integer> supportedResources = Arrays.asList(0, 7, 9, 16);

        public WriteAttributeTestDevice() {
            super("test_manufacturer", "model_number", "serial");
        }

        @Override
        public List<Integer> getAvailableResourceIds(ObjectModel model) {
            return supportedResources;
        }
    }

    @BeforeEach
    public void start(Protocol givenProtocol, String givenClientEndpointProvider, String givenServerEndpointProvider) {
        server = givenServerUsing(givenProtocol).with(givenServerEndpointProvider).build();
        server.start();
        client = givenClientUsing(givenProtocol)
                // we create client with a custom device to have pretty small discover response (making test readable)
                .withInstancesForObject(LwM2mId.DEVICE, new WriteAttributeTestDevice())
                .with(givenClientEndpointProvider).connectingTo(server).build();
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
    public void write_attribute_on_object_then_discover(Protocol givenProtocol, String givenClientEndpointProvider,
            String givenServerEndpointProvider) throws InterruptedException {

        // Check there is no attribute already set
        DiscoverResponse response = server.send(currentRegistration, new DiscoverRequest(LwM2mId.DEVICE));
        assertThat(response) //
                .hasCode(CONTENT) //
                .hasValidUnderlyingResponseFor(givenServerEndpointProvider) //
                .hasObjectLinksLike("</3>,</3/0>,</3/0/0>,</3/0/7>,</3/0/9>,</3/0/16>");

        // Write some attributes
        WriteAttributesResponse writeAttributesResponse = server.send(currentRegistration,
                new WriteAttributesRequest(LwM2mId.DEVICE, new LwM2mAttributeSet( //
                        LwM2mAttributes.create(LwM2mAttributes.MINIMUM_PERIOD, 100l), //
                        LwM2mAttributes.create(LwM2mAttributes.MAXIMUM_PERIOD, 200l) //
                )));
        assertThat(writeAttributesResponse) //
                .hasCode(CHANGED) //
                .hasValidUnderlyingResponseFor(givenServerEndpointProvider);

        // Check those attributes are now visible when discovering at object level
        response = server.send(currentRegistration, new DiscoverRequest(LwM2mId.DEVICE));
        assertThat(response) //
                .hasCode(CONTENT) //
                .hasValidUnderlyingResponseFor(givenServerEndpointProvider) //
                .hasObjectLinksLike("</3>;pmin=100;pmax=200,</3/0>,</3/0/0>,</3/0/7>,</3/0/9>,</3/0/16>");

        // override attribute
        writeAttributesResponse = server.send(currentRegistration,
                new WriteAttributesRequest(LwM2mId.DEVICE, new LwM2mAttributeSet( //
                        LwM2mAttributes.create(LwM2mAttributes.MINIMUM_PERIOD, 150l) //
                )));
        assertThat(writeAttributesResponse) //
                .hasCode(CHANGED) //
                .hasValidUnderlyingResponseFor(givenServerEndpointProvider);

        // Check it
        response = server.send(currentRegistration, new DiscoverRequest(LwM2mId.DEVICE));
        assertThat(response) //
                .hasCode(CONTENT) //
                .hasValidUnderlyingResponseFor(givenServerEndpointProvider) //
                .hasObjectLinksLike("</3>;pmin=150;pmax=200,</3/0>,</3/0/0>,</3/0/7>,</3/0/9>,</3/0/16>");

        // remove attribute
        writeAttributesResponse = server.send(currentRegistration,
                new WriteAttributesRequest(LwM2mId.DEVICE, new LwM2mAttributeSet( //
                        LwM2mAttributes.create(LwM2mAttributes.MAXIMUM_PERIOD) //
                )));
        assertThat(writeAttributesResponse) //
                .hasCode(CHANGED) //
                .hasValidUnderlyingResponseFor(givenServerEndpointProvider);

        // Check it
        response = server.send(currentRegistration, new DiscoverRequest(LwM2mId.DEVICE));
        assertThat(response) //
                .hasCode(CONTENT) //
                .hasValidUnderlyingResponseFor(givenServerEndpointProvider) //
                .hasObjectLinksLike("</3>;pmin=150,</3/0>,</3/0/0>,</3/0/7>,</3/0/9>,</3/0/16>");

        // add + override attribute
        writeAttributesResponse = server.send(currentRegistration,
                new WriteAttributesRequest(LwM2mId.DEVICE, new LwM2mAttributeSet( //
                        LwM2mAttributes.create(LwM2mAttributes.MINIMUM_PERIOD, 300l), //
                        LwM2mAttributes.create(LwM2mAttributes.MAXIMUM_PERIOD, 600l) //
                )));
        assertThat(writeAttributesResponse) //
                .hasCode(CHANGED) //
                .hasValidUnderlyingResponseFor(givenServerEndpointProvider);

        // Check it
        response = server.send(currentRegistration, new DiscoverRequest(LwM2mId.DEVICE));
        assertThat(response) //
                .hasCode(CONTENT) //
                .hasValidUnderlyingResponseFor(givenServerEndpointProvider) //
                .hasObjectLinksLike("</3>;pmin=300;pmax=600,</3/0>,</3/0/0>,</3/0/7>,</3/0/9>,</3/0/16>");
    }

    @TestAllTransportLayer
    public void write_attribute_on_object_instance_then_discover(Protocol givenProtocol,
            String givenClientEndpointProvider, String givenServerEndpointProvider) throws InterruptedException {

        // Check there is no attribute already set
        DiscoverResponse response = server.send(currentRegistration, new DiscoverRequest(LwM2mId.DEVICE, 0));
        assertThat(response) //
                .hasCode(CONTENT) //
                .hasValidUnderlyingResponseFor(givenServerEndpointProvider) //
                .hasObjectLinksLike("</3/0>,</3/0/0>,</3/0/7>,</3/0/9>,</3/0/16>");

        // Write some attributes
        WriteAttributesResponse writeAttributesResponse = server.send(currentRegistration,
                new WriteAttributesRequest(LwM2mId.DEVICE, 0, new LwM2mAttributeSet( //
                        LwM2mAttributes.create(LwM2mAttributes.MINIMUM_PERIOD, 100l), //
                        LwM2mAttributes.create(LwM2mAttributes.MAXIMUM_PERIOD, 200l) //
                )));
        assertThat(writeAttributesResponse) //
                .hasCode(CHANGED) //
                .hasValidUnderlyingResponseFor(givenServerEndpointProvider);

        // Check those attributes are now visible when discovering at object level
        response = server.send(currentRegistration, new DiscoverRequest(LwM2mId.DEVICE));
        assertThat(response) //
                .hasCode(CONTENT) //
                .hasValidUnderlyingResponseFor(givenServerEndpointProvider) //
                .hasObjectLinksLike("</3>,</3/0>;pmin=100;pmax=200,</3/0/0>,</3/0/7>,</3/0/9>,</3/0/16>");

        // Check those attributes are now visible when discovering at object instance level
        response = server.send(currentRegistration, new DiscoverRequest(LwM2mId.DEVICE, 0));
        assertThat(response) //
                .hasCode(CONTENT) //
                .hasValidUnderlyingResponseFor(givenServerEndpointProvider) //
                .hasObjectLinksLike("</3/0>;pmin=100;pmax=200,</3/0/0>,</3/0/7>,</3/0/9>,</3/0/16>");

    }

    @TestAllTransportLayer
    public void write_attribute_on_single_resource_then_discover(Protocol givenProtocol,
            String givenClientEndpointProvider, String givenServerEndpointProvider) throws InterruptedException {

        // Check there is no attribute already set
        DiscoverResponse response = server.send(currentRegistration, new DiscoverRequest(LwM2mId.DEVICE, 0, 9));
        assertThat(response) //
                .hasCode(CONTENT) //
                .hasValidUnderlyingResponseFor(givenServerEndpointProvider) //
                .hasObjectLinksLike("</3/0/9>");

        // Write some attributes
        WriteAttributesResponse writeAttributesResponse = server.send(currentRegistration,
                new WriteAttributesRequest(3, 0, 9, new LwM2mAttributeSet( //
                        LwM2mAttributes.create(LwM2mAttributes.MINIMUM_PERIOD, 100l), //
                        LwM2mAttributes.create(LwM2mAttributes.MINIMUM_PERIOD, 200l), //
                        LwM2mAttributes.create(LwM2mAttributes.STEP, 1d), //
                        LwM2mAttributes.create(LwM2mAttributes.LESSER_THAN, 20d), //
                        LwM2mAttributes.create(LwM2mAttributes.GREATER_THAN, 50d) //
                )));
        assertThat(writeAttributesResponse) //
                .hasCode(CHANGED) //
                .hasValidUnderlyingResponseFor(givenServerEndpointProvider);

        // Check those attributes are now visible when discovering at object level
        response = server.send(currentRegistration, new DiscoverRequest(3));
        assertThat(response) //
                .hasCode(CONTENT) //
                .hasValidUnderlyingResponseFor(givenServerEndpointProvider) //
                .hasObjectLinksLike(
                        "</3>,</3/0>,</3/0/0>,</3/0/7>,</3/0/9>;pmin=100;pmax=200;st=1;lt=20;gt=50,</3/0/16>");

        // Check those attributes are now visible when discovering at object instance level
        response = server.send(currentRegistration, new DiscoverRequest(3, 0));
        assertThat(response) //
                .hasCode(CONTENT) //
                .hasValidUnderlyingResponseFor(givenServerEndpointProvider) //
                .hasObjectLinksLike("</3/0>,</3/0/0>,</3/0/7>,</3/0/9>;pmin=100;pmax=200;st=1;lt=20;gt=50,</3/0/16>");

        // Check those attributes are now visible when discovering at resource level
        response = server.send(currentRegistration, new DiscoverRequest(3, 0, 9));
        assertThat(response) //
                .hasCode(CONTENT) //
                .hasValidUnderlyingResponseFor(givenServerEndpointProvider) //
                .hasObjectLinksLike("</3/0/9>;pmin=100;pmax=200;st=1;lt=20;gt=50");
    }

    @TestAllTransportLayer
    public void write_attribute_on_resource_instance_then_discover(Protocol givenProtocol,
            String givenClientEndpointProvider, String givenServerEndpointProvider) throws InterruptedException {

        // Check there is no attribute already set
        DiscoverResponse response = server.send(currentRegistration, new DiscoverRequest(LwM2mId.DEVICE, 0, 7));
        assertThat(response) //
                .hasCode(CONTENT) //
                .hasValidUnderlyingResponseFor(givenServerEndpointProvider) //
                .hasObjectLinksLike("</3/0/7>;dim=2,</3/0/7/0>,</3/0/7/1>");

        // Write some attributes
        WriteAttributesResponse writeAttributesResponse = server.send(currentRegistration,
                new WriteAttributesRequest(3, 0, 7, 0, new LwM2mAttributeSet( //
                        LwM2mAttributes.create(LwM2mAttributes.MINIMUM_PERIOD, 100l), //
                        LwM2mAttributes.create(LwM2mAttributes.MINIMUM_PERIOD, 200l), //
                        LwM2mAttributes.create(LwM2mAttributes.STEP, 1d), //
                        LwM2mAttributes.create(LwM2mAttributes.LESSER_THAN, 20d), //
                        LwM2mAttributes.create(LwM2mAttributes.GREATER_THAN, 50d) //
                )));
        assertThat(writeAttributesResponse) //
                .hasCode(CHANGED) //
                .hasValidUnderlyingResponseFor(givenServerEndpointProvider);

        // Check those attributes are now visible when discovering at object level
        response = server.send(currentRegistration, new DiscoverRequest(3));
        assertThat(response) //
                .hasCode(CONTENT) //
                .hasValidUnderlyingResponseFor(givenServerEndpointProvider) //
                .hasObjectLinksLike("</3>,</3/0>,</3/0/0>,</3/0/7>,</3/0/9>,</3/0/16>");

        // Check those attributes are now visible when discovering at object instance level
        response = server.send(currentRegistration, new DiscoverRequest(3, 0));
        assertThat(response) //
                .hasCode(CONTENT) //
                .hasValidUnderlyingResponseFor(givenServerEndpointProvider) //
                .hasObjectLinksLike("</3/0>,</3/0/0>,</3/0/7>,</3/0/9></3/0/16>");

        // Check those attributes are now visible when discovering at resource level
        response = server.send(currentRegistration, new DiscoverRequest(3, 0, 7));
        assertThat(response) //
                .hasCode(CONTENT) //
                .hasValidUnderlyingResponseFor(givenServerEndpointProvider) //
                .hasObjectLinksLike("</3/0/7>;dim=2,</3/0/7/0>;pmin=100;pmax=200;st=1;lt=20;gt=50,</3/0/7/1>");
    }

    @TestAllTransportLayer
    public void write_attribute_at_all_level_then_discover(Protocol givenProtocol, String givenClientEndpointProvider,
            String givenServerEndpointProvider) throws InterruptedException {

        // Check there is no attribute already set
        DiscoverResponse response = server.send(currentRegistration, new DiscoverRequest(LwM2mId.DEVICE));
        assertThat(response) //
                .hasCode(CONTENT) //
                .hasValidUnderlyingResponseFor(givenServerEndpointProvider) //
                .hasObjectLinksLike("</3>,</3/0>,</3/0/0>,</3/0/7>,</3/0/9>,</3/0/16>");

        // Write some attributes at object level
        WriteAttributesResponse writeAttributesResponse = server.send(currentRegistration,
                new WriteAttributesRequest(LwM2mId.DEVICE, new LwM2mAttributeSet( //
                        LwM2mAttributes.create(LwM2mAttributes.MINIMUM_PERIOD, 1000l), //
                        LwM2mAttributes.create(LwM2mAttributes.MAXIMUM_PERIOD, 2000l) //
                )));
        assertThat(writeAttributesResponse) //
                .hasCode(CHANGED) //
                .hasValidUnderlyingResponseFor(givenServerEndpointProvider);
        // Write some attributes at object install level
        writeAttributesResponse = server.send(currentRegistration,
                new WriteAttributesRequest(LwM2mId.DEVICE, 0, new LwM2mAttributeSet( //
                        LwM2mAttributes.create(LwM2mAttributes.MINIMUM_PERIOD, 100l), //
                        LwM2mAttributes.create(LwM2mAttributes.MAXIMUM_PERIOD, 200l) //
                )));
        assertThat(writeAttributesResponse) //
                .hasCode(CHANGED) //
                .hasValidUnderlyingResponseFor(givenServerEndpointProvider);
        // Write some attributes at resource level
        writeAttributesResponse = server.send(currentRegistration,
                new WriteAttributesRequest(3, 0, 7, new LwM2mAttributeSet( //
                        LwM2mAttributes.create(LwM2mAttributes.MINIMUM_PERIOD, 10l), //
                        LwM2mAttributes.create(LwM2mAttributes.MINIMUM_PERIOD, 20l), //
                        LwM2mAttributes.create(LwM2mAttributes.STEP, 10d), //
                        LwM2mAttributes.create(LwM2mAttributes.LESSER_THAN, 200d), //
                        LwM2mAttributes.create(LwM2mAttributes.GREATER_THAN, 500d) //
                )));
        assertThat(writeAttributesResponse) //
                .hasCode(CHANGED) //
                .hasValidUnderlyingResponseFor(givenServerEndpointProvider);
        // Write some attributes at resource instance level
        writeAttributesResponse = server.send(currentRegistration,
                new WriteAttributesRequest(3, 0, 7, 0, new LwM2mAttributeSet( //
                        LwM2mAttributes.create(LwM2mAttributes.MINIMUM_PERIOD, 1l), //
                        LwM2mAttributes.create(LwM2mAttributes.MINIMUM_PERIOD, 2l), //
                        LwM2mAttributes.create(LwM2mAttributes.STEP, 1d), //
                        LwM2mAttributes.create(LwM2mAttributes.LESSER_THAN, 20d), //
                        LwM2mAttributes.create(LwM2mAttributes.GREATER_THAN, 50d) //
                )));
        assertThat(writeAttributesResponse) //
                .hasCode(CHANGED) //
                .hasValidUnderlyingResponseFor(givenServerEndpointProvider);

        // Check those attributes are now visible when discovering at object level
        response = server.send(currentRegistration, new DiscoverRequest(3));
        assertThat(response) //
                .hasCode(CONTENT) //
                .hasValidUnderlyingResponseFor(givenServerEndpointProvider) //
                .hasObjectLinksLike(
                        "</3>;pmin=1000;pmax=2000,</3/0>;pmin=100;pmax=200,</3/0/0>,</3/0/7>;dim=2;pmin=10;pmax=20;st=10;lt=200;gt=500,</3/0/9></3/0/16>");

        // Check those attributes are now visible when discovering at object instance level
        response = server.send(currentRegistration, new DiscoverRequest(3, 0));
        assertThat(response) //
                .hasCode(CONTENT) //
                .hasValidUnderlyingResponseFor(givenServerEndpointProvider) //
                .hasObjectLinksLike(
                        "</3/0>;pmin=100;pmax=200,</3/0/0>,</3/0/7>;dim=2;pmin=10;pmax=20;st=10;lt=200;gt=500,</3/0/9></3/0/16>");

        // Check those attributes are now visible when discovering at resource level
        response = server.send(currentRegistration, new DiscoverRequest(3, 0, 7));
        assertThat(response) //
                .hasCode(CONTENT) //
                .hasValidUnderlyingResponseFor(givenServerEndpointProvider) //
                .hasObjectLinksLike(
                        "</3/0/7>;dim=2;pmin=10;pmax=20;st=10;lt=200;gt=500,</3/0/7/0>;pmin=1;pmax=2;st=1;lt=20;gt=50,</3/0/7/1>");
    }
}
