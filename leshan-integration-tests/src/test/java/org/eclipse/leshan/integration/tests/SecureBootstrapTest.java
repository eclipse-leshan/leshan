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

import static org.eclipse.leshan.integration.tests.BootstrapConfigTestBuilder.givenBootstrapConfig;
import static org.eclipse.leshan.integration.tests.util.Credentials.GOOD_PSK_ID;
import static org.eclipse.leshan.integration.tests.util.Credentials.GOOD_PSK_KEY;
import static org.eclipse.leshan.integration.tests.util.Credentials.clientPrivateKey;
import static org.eclipse.leshan.integration.tests.util.Credentials.clientPublicKey;
import static org.eclipse.leshan.integration.tests.util.Credentials.serverPrivateKey;
import static org.eclipse.leshan.integration.tests.util.Credentials.serverPublicKey;
import static org.eclipse.leshan.integration.tests.util.assertion.Assertions.assertThat;

import java.util.concurrent.TimeUnit;

import org.eclipse.leshan.client.californium.endpoint.CaliforniumClientEndpointsProvider;
import org.eclipse.leshan.client.californium.endpoint.coap.CoapClientProtocolProvider;
import org.eclipse.leshan.client.californium.endpoint.coaps.CoapsClientProtocolProvider;
import org.eclipse.leshan.core.endpoint.Protocol;
import org.eclipse.leshan.integration.tests.util.Credentials;
import org.eclipse.leshan.integration.tests.util.LeshanTestBootstrapServer;
import org.eclipse.leshan.integration.tests.util.LeshanTestBootstrapServerBuilder;
import org.eclipse.leshan.integration.tests.util.LeshanTestClient;
import org.eclipse.leshan.integration.tests.util.LeshanTestClientBuilder;
import org.eclipse.leshan.integration.tests.util.LeshanTestServer;
import org.eclipse.leshan.integration.tests.util.LeshanTestServerBuilder;
import org.eclipse.leshan.server.bootstrap.InvalidConfigurationException;
import org.eclipse.leshan.server.security.InMemorySecurityStore;
import org.eclipse.leshan.server.security.NonUniqueSecurityInfoException;
import org.eclipse.leshan.server.security.SecurityInfo;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class SecureBootstrapTest {

    LeshanTestBootstrapServerBuilder givenBootstrapServer;
    LeshanTestBootstrapServer bootstrapServer;
    LeshanTestServerBuilder givenServer;
    LeshanTestServer server;
    LeshanTestClientBuilder givenClient;
    LeshanTestClient client;

    @BeforeEach
    public void start() {
        givenBootstrapServer = LeshanTestBootstrapServerBuilder.givenBootstrapServer().with("Californium")
                .with(new InMemorySecurityStore());
        givenServer = LeshanTestServerBuilder.givenServer().with("Californium").with(new InMemorySecurityStore());
        // TODO we probably need better API to create client supporting several protocol in tests.
        givenClient = LeshanTestClientBuilder.givenClient()
                .with(new CaliforniumClientEndpointsProvider.Builder(new CoapClientProtocolProvider(),
                        new CoapsClientProtocolProvider()).build());
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

    @Test
    public void bootstrapSecureWithPSK() throws NonUniqueSecurityInfoException, InvalidConfigurationException {
        // Create DM Server without security & start it
        server = givenServer.using(Protocol.COAP).build();
        server.start();

        // Create and start bootstrap server
        bootstrapServer = givenBootstrapServer.using(Protocol.COAPS).build();
        bootstrapServer.start();

        // Create Client and check it is not already registered
        client = givenClient.connectingTo(bootstrapServer).using(Protocol.COAPS).usingPsk(GOOD_PSK_ID, GOOD_PSK_KEY)
                .build();
        assertThat(client).isNotRegisteredAt(server);

        // Add client credentials to the server
        bootstrapServer.getEditableSecurityStore()
                .add(SecurityInfo.newPreSharedKeyInfo(client.getEndpointName(), GOOD_PSK_ID, GOOD_PSK_KEY));

        // Add config for this client
        bootstrapServer.getConfigStore().add(client.getEndpointName(), //
                givenBootstrapConfig() //
                        .adding(Protocol.COAPS, bootstrapServer, GOOD_PSK_ID, GOOD_PSK_KEY) //
                        .adding(Protocol.COAP, server) //
                        .build());

        // Start it and wait for registration
        client.start();
        server.waitForNewRegistrationOf(client);

        // check the client is registered
        assertThat(client).isRegisteredAt(server);
    }

    @Test
    public void bootstrapSecureWithBadPSKKey() throws InvalidConfigurationException, NonUniqueSecurityInfoException {
        // Create DM Server without security & start it
        server = givenServer.using(Protocol.COAP).build();
        server.start();

        // Create and start bootstrap server
        bootstrapServer = givenBootstrapServer.using(Protocol.COAPS).build();
        bootstrapServer.start();

        // Create Client and check it is not already registered
        client = givenClient.connectingTo(bootstrapServer).using(Protocol.COAPS).usingPsk(GOOD_PSK_ID, GOOD_PSK_KEY)
                .build();
        assertThat(client).isNotRegisteredAt(server);

        // Add client credentials to the server
        bootstrapServer.getEditableSecurityStore()
                .add(SecurityInfo.newPreSharedKeyInfo(client.getEndpointName(), GOOD_PSK_ID, Credentials.BAD_PSK_KEY));

        // Add config for this client
        bootstrapServer.getConfigStore().add(client.getEndpointName(), //
                givenBootstrapConfig() //
                        .adding(Protocol.COAPS, bootstrapServer, GOOD_PSK_ID, GOOD_PSK_KEY) //
                        .adding(Protocol.COAP, server) //
                        .build());

        // Start it and wait for registration
        client.start();
        assertThat(client).after(1, TimeUnit.SECONDS).isNotRegisteredAt(server);
    }

    @Test
    public void bootstrapSecureWithRPK() throws NonUniqueSecurityInfoException, InvalidConfigurationException {
        // Create DM Server without security & start it
        server = givenServer.using(Protocol.COAP).build();
        server.start();

        // Create and start bootstrap server
        bootstrapServer = givenBootstrapServer.using(Protocol.COAPS).using(serverPublicKey, serverPrivateKey).build();
        bootstrapServer.start();

        // Create Client and check it is not already registered
        client = givenClient.connectingTo(bootstrapServer).using(Protocol.COAPS)
                .using(clientPublicKey, clientPrivateKey)//
                .trusting(serverPublicKey).build();
        ;
        assertThat(client).isNotRegisteredAt(server);

        // Add client credentials to the server
        bootstrapServer.getEditableSecurityStore()
                .add(SecurityInfo.newRawPublicKeyInfo(client.getEndpointName(), clientPublicKey));

        // Add config for this client
        bootstrapServer.getConfigStore().add(client.getEndpointName(), //
                givenBootstrapConfig() //
                        .adding(Protocol.COAPS, bootstrapServer, clientPublicKey, clientPrivateKey, serverPublicKey) //
                        .adding(Protocol.COAP, server) //
                        .build());

        // Start it and wait for registration
        client.start();
        server.waitForNewRegistrationOf(client);

        // check the client is registered
        assertThat(client).isRegisteredAt(server);
    }

    @Test
    public void bootstrapToPSKServer() throws NonUniqueSecurityInfoException, InvalidConfigurationException {
        // Create DM Server without security & start it
        server = givenServer.using(Protocol.COAPS).build();
        server.start();

        // Create and start bootstrap server
        bootstrapServer = givenBootstrapServer.using(Protocol.COAP).build();
        bootstrapServer.start();

        // Create Client and check it is not already registered
        client = givenClient.connectingTo(bootstrapServer).using(Protocol.COAP).build();
        assertThat(client).isNotRegisteredAt(server);

        // Add client credentials to the server
        server.getSecurityStore()
                .add(SecurityInfo.newPreSharedKeyInfo(client.getEndpointName(), GOOD_PSK_ID, GOOD_PSK_KEY));

        // Add config for this client
        bootstrapServer.getConfigStore().add(client.getEndpointName(), //
                givenBootstrapConfig() //
                        .adding(Protocol.COAP, bootstrapServer) //
                        .adding(Protocol.COAPS, server, GOOD_PSK_ID, GOOD_PSK_KEY) //
                        .build());

        // Start it and wait for registration
        client.start();
        server.waitForNewRegistrationOf(client);
    }

    @Test
    public void bootstrapToRPKServer() throws NonUniqueSecurityInfoException, InvalidConfigurationException {
        // Create DM Server without security & start it
        server = givenServer.using(Protocol.COAPS).using(serverPublicKey, serverPrivateKey).build();
        server.start();

        // Create and start bootstrap server
        bootstrapServer = givenBootstrapServer.using(Protocol.COAP).build();
        bootstrapServer.start();

        // Create Client and check it is not already registered
        client = givenClient.connectingTo(bootstrapServer).using(Protocol.COAP).build();
        assertThat(client).isNotRegisteredAt(server);

        // Add client credentials to the server
        server.getSecurityStore().add(SecurityInfo.newRawPublicKeyInfo(client.getEndpointName(), clientPublicKey));

        // Add config for this client
        bootstrapServer.getConfigStore().add(client.getEndpointName(), //
                givenBootstrapConfig() //
                        .adding(Protocol.COAP, bootstrapServer) //
                        .adding(Protocol.COAPS, server, clientPublicKey, clientPrivateKey, serverPublicKey) //
                        .build());

        // Start it and wait for registration
        client.start();
        server.waitForNewRegistrationOf(client);

        // check the client is registered
        assertThat(client).isRegisteredAt(server);
    }
}
