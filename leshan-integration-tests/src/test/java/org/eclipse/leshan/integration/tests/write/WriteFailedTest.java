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
 *     Sierra Wireless - initial API and implementation
 *******************************************************************************/
package org.eclipse.leshan.integration.tests.write;

import static org.eclipse.leshan.core.ResponseCode.BAD_REQUEST;
import static org.eclipse.leshan.core.ResponseCode.METHOD_NOT_ALLOWED;
import static org.eclipse.leshan.core.ResponseCode.NOT_FOUND;
import static org.eclipse.leshan.integration.tests.util.LeshanTestClientBuilder.givenClientUsing;
import static org.eclipse.leshan.integration.tests.util.LeshanTestServerBuilder.givenServerUsing;
import static org.eclipse.leshan.integration.tests.util.assertion.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.stream.Stream;

import org.eclipse.leshan.core.endpoint.Protocol;
import org.eclipse.leshan.core.node.LwM2mResource;
import org.eclipse.leshan.core.node.LwM2mSingleResource;
import org.eclipse.leshan.core.request.WriteRequest;
import org.eclipse.leshan.core.request.WriteRequest.Mode;
import org.eclipse.leshan.core.response.WriteResponse;
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
public class WriteFailedTest {

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
    public void cannot_write_non_writable_resource(Protocol givenProtocol, String givenClientEndpointProvider,
            String givenServerEndpointProvider) throws InterruptedException {
        // try to write unwritable resource like manufacturer on device
        String manufacturer = "new manufacturer";
        WriteResponse response = server.send(currentRegistration, new WriteRequest(3, 0, 0, manufacturer));

        // verify result
        assertThat(response) //
                .hasCode(METHOD_NOT_ALLOWED) //
                .hasValidUnderlyingResponseFor(givenServerEndpointProvider);
    }

    @TestAllTransportLayer
    public void cannot_write_security_resource(Protocol givenProtocol, String givenClientEndpointProvider,
            String givenServerEndpointProvider) throws InterruptedException {
        // try to write unwritable resource like manufacturer on device
        String uri = "new.dest.server";
        WriteResponse response = server.send(currentRegistration, new WriteRequest(0, 0, 0, uri));

        // verify result
        assertThat(response) //
                .hasCode(NOT_FOUND) //
                .hasValidUnderlyingResponseFor(givenServerEndpointProvider);
    }

    @TestAllTransportLayer
    public void cannot_write_replacing_incomplete_object_instance(Protocol givenProtocol,
            String givenClientEndpointProvider, String givenServerEndpointProvider) throws InterruptedException {
        // write server object
        LwM2mResource lifetime = LwM2mSingleResource.newIntegerResource(1, 120);
        LwM2mResource defaultMinPeriod = LwM2mSingleResource.newIntegerResource(2, 10);
        WriteResponse response = server.send(currentRegistration,
                new WriteRequest(Mode.REPLACE, 1, 0, lifetime, defaultMinPeriod));

        // verify result
        assertThat(response) //
                .hasCode(BAD_REQUEST) //
                .hasValidUnderlyingResponseFor(givenServerEndpointProvider);
    }
}
