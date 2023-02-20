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
import static org.eclipse.leshan.integration.tests.util.IntegrationTestHelper.LIFETIME;
import static org.eclipse.leshan.integration.tests.util.IntegrationTestHelper.linkParser;
import static org.eclipse.leshan.integration.tests.util.LeshanTestClientBuilder.givenClientUsing;
import static org.eclipse.leshan.integration.tests.util.assertion.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import org.eclipse.leshan.core.endpoint.Protocol;
import org.eclipse.leshan.core.link.LinkParseException;
import org.eclipse.leshan.integration.tests.util.LeshanTestClient;
import org.eclipse.leshan.integration.tests.util.LeshanTestClientBuilder;
import org.eclipse.leshan.integration.tests.util.LeshanTestServer;
import org.eclipse.leshan.integration.tests.util.LeshanTestServerBuilder;
import org.eclipse.leshan.integration.tests.util.junit5.TestingUtils;
import org.eclipse.leshan.server.registration.Registration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DynamicNode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;

public class RegistrationTest4 {

    /*---------------------------------/
     *  Parameterized Tests
     * -------------------------------*/
    @TestFactory
    Stream<DynamicNode> testRegistrations() throws Exception {
        return TestingUtils.parameterizedClassTester("protocol={0} clientProvider={1}, ServerProvider={1}",
                InnerRegistrationTest4.class, transports());
    }

    static Stream<org.junit.jupiter.params.provider.Arguments> transports() {
        return Stream.of(//
                // ProtocolUsed - Client Endpoint Provider - Server Endpoint Provider
                arguments(Protocol.COAP, "Californium", "Californium"), //
                arguments(Protocol.COAP, "Californium", "java-coap") //
        );
    }

    /*---------------------------------/
     *  Set-up and Tear-down Tests
     * -------------------------------*/
    public static class InnerRegistrationTest4 {
        private final Protocol givenProtocol;
        private final String givenServerEndpointProvider;
        private final String givenClientEndpointProvider;

        public InnerRegistrationTest4(Protocol protocol, String clientProvider, String serverProvider) {
            this.givenProtocol = protocol;
            this.givenClientEndpointProvider = clientProvider;
            this.givenServerEndpointProvider = serverProvider;
        }

        LeshanTestServer server;
        LeshanTestClientBuilder givenClient;
        LeshanTestClient client;

        @BeforeEach
        public void start() {
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

        @Test
        public void register_update_deregister() throws LinkParseException {

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
            assertThat(registration.getObjectLinks()) //
                    .isEqualTo(linkParser.parseCoreLinkFormat(
                            "</>;rt=\"oma.lwm2m\";ct=\"60 110 112 1542 1543 11542 11543\",</1>;ver=1.1,</1/0>,</2>,</3>;ver=1.1,</3/0>,</3442/0>"
                                    .getBytes()));

            // Check for update
            client.waitForUpdateTo(server, LIFETIME, TimeUnit.SECONDS);
            server.waitForUpdateOf(registration);
            assertThat(client).isRegisteredAt(server);

            // Check deregistration
            client.stop(true);
            server.waitForDeregistrationOf(registration);
            assertThat(client).isNotRegisteredAt(server);
        }
    }
}
