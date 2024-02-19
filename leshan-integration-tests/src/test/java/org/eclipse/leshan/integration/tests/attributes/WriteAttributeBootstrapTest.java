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
 *     Michał Wadowski (Orange) - Improved compliance with rfc6690
 *******************************************************************************/
package org.eclipse.leshan.integration.tests.attributes;

import static org.eclipse.leshan.core.util.TestLwM2mId.MULTIPLE_INTEGER_VALUE;
import static org.eclipse.leshan.core.util.TestLwM2mId.TEST_OBJECT;
import static org.eclipse.leshan.integration.tests.BootstrapConfigTestBuilder.givenBootstrapConfig;
import static org.eclipse.leshan.integration.tests.util.LeshanTestBootstrapServerBuilder.givenBootstrapServerUsing;
import static org.eclipse.leshan.integration.tests.util.LeshanTestClientBuilder.givenClientUsing;
import static org.eclipse.leshan.integration.tests.util.LeshanTestServerBuilder.givenServerUsing;
import static org.eclipse.leshan.integration.tests.util.assertion.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import org.eclipse.leshan.core.endpoint.Protocol;
import org.eclipse.leshan.core.link.lwm2m.attributes.LwM2mAttributeSet;
import org.eclipse.leshan.core.link.lwm2m.attributes.LwM2mAttributes;
import org.eclipse.leshan.core.node.InvalidLwM2mPathException;
import org.eclipse.leshan.core.node.LwM2mPath;
import org.eclipse.leshan.core.request.ObserveRequest;
import org.eclipse.leshan.core.request.WriteAttributesRequest;
import org.eclipse.leshan.core.response.ObserveResponse;
import org.eclipse.leshan.core.response.WriteAttributesResponse;
import org.eclipse.leshan.integration.tests.util.LeshanTestBootstrapServer;
import org.eclipse.leshan.integration.tests.util.LeshanTestBootstrapServerBuilder;
import org.eclipse.leshan.integration.tests.util.LeshanTestClient;
import org.eclipse.leshan.integration.tests.util.LeshanTestClientBuilder;
import org.eclipse.leshan.integration.tests.util.LeshanTestServer;
import org.eclipse.leshan.integration.tests.util.LeshanTestServerBuilder;
import org.eclipse.leshan.integration.tests.util.junit5.extensions.BeforeEachParameterizedResolver;
import org.eclipse.leshan.server.bootstrap.InvalidConfigurationException;
import org.eclipse.leshan.server.registration.Registration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

@ExtendWith(BeforeEachParameterizedResolver.class)
public class WriteAttributeBootstrapTest {

    /*---------------------------------/
     *  Parameterized Tests
     * -------------------------------*/
    @ParameterizedTest(name = "{0} - Client using {1} - Server using {2}- BS Server using {3}")
    @MethodSource("transports")
    @Retention(RetentionPolicy.RUNTIME)
    private @interface TestAllTransportLayer {
    }

    static Stream<org.junit.jupiter.params.provider.Arguments> transports() {
        return Stream.of(//
                // ProtocolUsed - Client Endpoint Provider - Server Endpoint Provider - BS Server Endpoint Provider
                arguments(Protocol.COAP, "Californium", "Californium", "Californium"), //
                arguments(Protocol.COAP, "Californium", "java-coap", "Californium"), //
                arguments(Protocol.COAP, "java-coap", "Californium", "Californium"), //
                arguments(Protocol.COAP, "java-coap", "java-coap", "Californium"));
    }

    /*---------------------------------/
     *  Set-up and Tear-down Tests
     * -------------------------------*/
    LeshanTestBootstrapServerBuilder givenBootstrapServer;
    LeshanTestBootstrapServer bootstrapServer;
    LeshanTestServerBuilder givenServer;
    LeshanTestServer server;
    LeshanTestClientBuilder givenClient;
    LeshanTestClient client;

    @BeforeEach
    public void start(Protocol givenProtocol, String givenClientEndpointProvider, String givenServerEndpointProvider,
            String givenBootstrapServerEndpointProvider) {
        givenServer = givenServerUsing(givenProtocol).with(givenServerEndpointProvider);
        givenBootstrapServer = givenBootstrapServerUsing(givenProtocol).with(givenBootstrapServerEndpointProvider);
        givenClient = givenClientUsing(givenProtocol).with(givenClientEndpointProvider);
    }

    @AfterEach
    public void stop() throws InterruptedException {
        if (client != null)
            client.destroy(false);
        if (server != null)
            server.destroy();
        if (bootstrapServer != null)
            bootstrapServer.destroy();
    }

    /*---------------------------------/
     *  Tests
     * -------------------------------*/
    @TestAllTransportLayer
    public void write_attribute_then_observe_object_then_bootstrap_then_check_no_more_notification_data(
            Protocol givenProtocol, String givenClientEndpointProvider, String givenServerEndpointProvider,
            String givenBootstrapServerEndpointProvider)
            throws InterruptedException, InvalidLwM2mPathException, InvalidConfigurationException {

        write_attribute_then_observe_then_bootstrap_then_check_no_more_notification_data(givenProtocol,
                new LwM2mPath(TEST_OBJECT));
    }

    @TestAllTransportLayer
    public void write_attribute_then_observe_object_instance_then_bootstrap_then_check_no_more_notification_data(
            Protocol givenProtocol, String givenClientEndpointProvider, String givenServerEndpointProvider,
            String givenBootstrapServerEndpointProvider)
            throws InterruptedException, InvalidLwM2mPathException, InvalidConfigurationException {

        write_attribute_then_observe_then_bootstrap_then_check_no_more_notification_data(givenProtocol,
                new LwM2mPath(TEST_OBJECT, 0));
    }

    @TestAllTransportLayer
    public void write_attribute_then_observe_resource_then_bootstrap_then_check_no_more_notification_data(
            Protocol givenProtocol, String givenClientEndpointProvider, String givenServerEndpointProvider,
            String givenBootstrapServerEndpointProvider)
            throws InterruptedException, InvalidLwM2mPathException, InvalidConfigurationException {

        write_attribute_then_observe_then_bootstrap_then_check_no_more_notification_data(givenProtocol,
                new LwM2mPath(TEST_OBJECT, 0, MULTIPLE_INTEGER_VALUE));
    }

    @TestAllTransportLayer
    public void write_attribute_then_observe_resource_instance_then_bootstrap_then_check_no_more_notification_data(
            Protocol givenProtocol, String givenClientEndpointProvider, String givenServerEndpointProvider,
            String givenBootstrapServerEndpointProvider)
            throws InterruptedException, InvalidLwM2mPathException, InvalidConfigurationException {

        write_attribute_then_observe_then_bootstrap_then_check_no_more_notification_data(givenProtocol,
                new LwM2mPath(TEST_OBJECT, 0, MULTIPLE_INTEGER_VALUE, 0));
    }

    public void write_attribute_then_observe_then_bootstrap_then_check_no_more_notification_data(Protocol givenProtocol,
            LwM2mPath targetedPath) throws InterruptedException, InvalidConfigurationException {

        // Create DM Server without security & start it
        server = givenServer.build();
        server.start();

        // Create and start bootstrap server
        bootstrapServer = givenBootstrapServer.build();
        bootstrapServer.start();

        // Create Client and check it is not already registered
        client = givenClient.connectingTo(bootstrapServer).build();
        assertThat(client).isNotRegisteredAt(server);

        // Add config for this client
        bootstrapServer.getConfigStore().add(client.getEndpointName(), //
                givenBootstrapConfig() //
                        .adding(givenProtocol, bootstrapServer) //
                        .adding(givenProtocol, server) //
                        .build());

        // Start it and wait for registration
        client.start();
        client.waitForBootstrapSuccess(bootstrapServer, 1, TimeUnit.SECONDS);
        server.waitForNewRegistrationOf(client);
        Registration currentRegistration = server.getRegistrationFor(client);

        // check the client is registered
        assertThat(client).isRegisteredAt(server);

        // Add Attribute pmin=1seconds to targeted node
        WriteAttributesResponse writeAttributeResponse = server.send(currentRegistration, new WriteAttributesRequest(
                targetedPath, new LwM2mAttributeSet(LwM2mAttributes.create(LwM2mAttributes.MINIMUM_PERIOD, 1l))));
        assertThat(writeAttributeResponse).isSuccess();

        // Check there is no notification data
        assertThat(client).hasNoNotificationData();

        // Observe targeted node
        ObserveResponse response = server.send(currentRegistration, new ObserveRequest(targetedPath));
        assertThat(response).isSuccess();

        // Check that we have new NotificationData
        Thread.sleep(100); // wait to be sure client handle observation : TODO We should do better.
        assertThat(client).hasNotificationData();

        // Force Bootstrap
        client.triggerClientInitiatedBootstrap(false);

        // then assert the is no more notification data
        client.waitForBootstrapSuccess(bootstrapServer, 1, TimeUnit.SECONDS);
        assertThat(client).hasNoNotificationData();
    }

}
