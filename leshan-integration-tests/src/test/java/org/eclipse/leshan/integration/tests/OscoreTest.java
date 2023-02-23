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
 * Contributors:
 *     Sierra Wireless - initial API and implementation
 *******************************************************************************/
package org.eclipse.leshan.integration.tests;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.leshan.integration.tests.util.Credentials.OSCORE_AEAD_ALGORITHM;
import static org.eclipse.leshan.integration.tests.util.Credentials.OSCORE_HKDF_ALGORITHM;
import static org.eclipse.leshan.integration.tests.util.Credentials.OSCORE_MASTER_SALT;
import static org.eclipse.leshan.integration.tests.util.Credentials.OSCORE_MASTER_SECRET;
import static org.eclipse.leshan.integration.tests.util.Credentials.OSCORE_RECIPIENT_ID;
import static org.eclipse.leshan.integration.tests.util.Credentials.OSCORE_SENDER_ID;
import static org.eclipse.leshan.integration.tests.util.LeshanTestClientBuilder.givenClientUsing;
import static org.eclipse.leshan.integration.tests.util.assertion.Assertions.assertThat;

import java.util.concurrent.TimeUnit;

import org.eclipse.leshan.core.endpoint.Protocol;
import org.eclipse.leshan.core.oscore.OscoreSetting;
import org.eclipse.leshan.core.request.ReadRequest;
import org.eclipse.leshan.core.response.ReadResponse;
import org.eclipse.leshan.integration.tests.util.LeshanTestClient;
import org.eclipse.leshan.integration.tests.util.LeshanTestClientBuilder;
import org.eclipse.leshan.integration.tests.util.LeshanTestServer;
import org.eclipse.leshan.integration.tests.util.LeshanTestServerBuilder;
import org.eclipse.leshan.server.registration.Registration;
import org.eclipse.leshan.server.security.InMemorySecurityStore;
import org.eclipse.leshan.server.security.NonUniqueSecurityInfoException;
import org.eclipse.leshan.server.security.SecurityInfo;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class OscoreTest {

    LeshanTestServer server;
    LeshanTestClientBuilder givenClient;
    LeshanTestClient client;

    @BeforeEach
    public void start() {
        server = givenServerUsing(Protocol.COAP).with("Californium-OSCORE").build();
        server.start();
        givenClient = givenClientUsing(Protocol.COAP).with("Californium-OSCORE");
    }

    @AfterEach
    public void stop() throws InterruptedException {
        if (client != null)
            client.destroy(false);
        if (server != null)
            server.destroy();
    }

    protected LeshanTestServerBuilder givenServerUsing(Protocol givenProtocol) {
        return new LeshanTestServerBuilder(givenProtocol).with(new InMemorySecurityStore());
    }

    public static OscoreSetting getServerOscoreSetting() {
        return new OscoreSetting(OSCORE_RECIPIENT_ID, OSCORE_SENDER_ID, OSCORE_MASTER_SECRET, OSCORE_AEAD_ALGORITHM,
                OSCORE_HKDF_ALGORITHM, OSCORE_MASTER_SALT);
    }

    protected static OscoreSetting getClientOscoreSetting() {
        return new OscoreSetting(OSCORE_SENDER_ID, OSCORE_RECIPIENT_ID, OSCORE_MASTER_SECRET, OSCORE_AEAD_ALGORITHM,
                OSCORE_HKDF_ALGORITHM, OSCORE_MASTER_SALT);
    }

    @Test
    public void registered_device_with_oscore_to_server_with_oscore()
            throws NonUniqueSecurityInfoException, InterruptedException {
        // Create OSCORE Client
        client = givenClient.connectingTo(server).using(getClientOscoreSetting()).build();

        // Add client credentials to the server
        server.getSecurityStore().add(SecurityInfo.newOscoreInfo(client.getEndpointName(), getServerOscoreSetting()));

        // Check client is not registered
        assertThat(client).isNotRegisteredAt(server);

        // Start it and wait for registration
        client.start();
        server.waitForNewRegistrationOf(client);

        // Check client is well registered
        assertThat(client).isRegisteredAt(server);
        Registration registration = server.getRegistrationFor(client);

        // check we can send request to client.
        ReadResponse response = server.send(registration, new ReadRequest(3, 0, 1), 500);
        assertThat(response.isSuccess()).isTrue();
    }

    @Test
    public void registered_device_with_oscore_to_server_with_oscore_then_removed_security_info_then_server_fails_to_send_request()
            throws NonUniqueSecurityInfoException, InterruptedException {
        // Create OSCORE Client
        client = givenClient.connectingTo(server).using(getClientOscoreSetting()).build();

        // Add client credentials to the server
        server.getSecurityStore().add(SecurityInfo.newOscoreInfo(client.getEndpointName(), getServerOscoreSetting()));

        // Check client is not registered
        assertThat(client).isNotRegisteredAt(server);

        // Start it and wait for registration
        client.start();
        server.waitForNewRegistrationOf(client);

        // Check client is well registered
        assertThat(client).isRegisteredAt(server);
        Registration registration = server.getRegistrationFor(client);

        // check we can send request to client.
        ReadResponse response = server.send(registration, new ReadRequest(3, 0, 1), 500);
        assertThat(response.isSuccess()).isTrue();

        // Remove securityInfo
        server.getSecurityStore().remove(client.getEndpointName(), true);

        // check we can send request to client.
        response = server.send(registration, new ReadRequest(3, 0, 1), 500);
        assertThat(response).isNull();
        // TODO OSCORE we must defined the expected behavior here.
    }

    @Test
    public void registered_device_with_oscore_to_server_with_oscore_then_removed_security_info_then_client_fails_to_update()
            throws NonUniqueSecurityInfoException, InterruptedException {

        // Create OSCORE Client
        client = givenClient.connectingTo(server)//
                .using(getClientOscoreSetting()).build();

        // Add client credentials to the server
        server.getSecurityStore().add(SecurityInfo.newOscoreInfo(client.getEndpointName(), getServerOscoreSetting()));

        // Check client is not registered
        assertThat(client).isNotRegisteredAt(server);

        // Start it and wait for registration
        client.start();
        server.waitForNewRegistrationOf(client);

        // Check client is well registered
        assertThat(client).isRegisteredAt(server);
        Registration registration = server.getRegistrationFor(client);

        // check we can send request to client.
        ReadResponse response = server.send(registration, new ReadRequest(3, 0, 1), 500);
        assertThat(response.isSuccess()).isTrue();

        // Remove securityInfo
        server.getSecurityStore().remove(client.getEndpointName(), true);

        // check that next update will failed.
        client.triggerRegistrationUpdate();
        client.waitForUpdateFailureTo(server, 3, TimeUnit.SECONDS);
    }
}
