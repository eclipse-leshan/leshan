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
import static org.eclipse.leshan.integration.tests.util.Credentials.OSCORE_AEAD_ALGORITHM;
import static org.eclipse.leshan.integration.tests.util.Credentials.OSCORE_BOOTSTRAP_MASTER_SALT;
import static org.eclipse.leshan.integration.tests.util.Credentials.OSCORE_BOOTSTRAP_MASTER_SECRET;
import static org.eclipse.leshan.integration.tests.util.Credentials.OSCORE_BOOTSTRAP_RECIPIENT_ID;
import static org.eclipse.leshan.integration.tests.util.Credentials.OSCORE_BOOTSTRAP_SENDER_ID;
import static org.eclipse.leshan.integration.tests.util.Credentials.OSCORE_HKDF_ALGORITHM;
import static org.eclipse.leshan.integration.tests.util.Credentials.OSCORE_MASTER_SALT;
import static org.eclipse.leshan.integration.tests.util.Credentials.OSCORE_MASTER_SECRET;
import static org.eclipse.leshan.integration.tests.util.Credentials.OSCORE_RECIPIENT_ID;
import static org.eclipse.leshan.integration.tests.util.Credentials.OSCORE_SENDER_ID;
import static org.eclipse.leshan.integration.tests.util.LeshanTestBootstrapServerBuilder.givenBootstrapServerUsing;
import static org.eclipse.leshan.integration.tests.util.LeshanTestClientBuilder.givenClientUsing;
import static org.eclipse.leshan.integration.tests.util.LeshanTestServerBuilder.givenServerUsing;
import static org.eclipse.leshan.integration.tests.util.assertion.Assertions.assertThat;

import org.eclipse.californium.oscore.OSException;
import org.eclipse.leshan.client.object.Oscore;
import org.eclipse.leshan.core.LwM2mId;
import org.eclipse.leshan.core.endpoint.Protocol;
import org.eclipse.leshan.core.oscore.OscoreSetting;
import org.eclipse.leshan.integration.tests.util.LeshanTestBootstrapServer;
import org.eclipse.leshan.integration.tests.util.LeshanTestBootstrapServerBuilder;
import org.eclipse.leshan.integration.tests.util.LeshanTestClient;
import org.eclipse.leshan.integration.tests.util.LeshanTestClientBuilder;
import org.eclipse.leshan.integration.tests.util.LeshanTestServer;
import org.eclipse.leshan.integration.tests.util.LeshanTestServerBuilder;
import org.eclipse.leshan.server.bootstrap.BootstrapConfig;
import org.eclipse.leshan.server.bootstrap.InvalidConfigurationException;
import org.eclipse.leshan.server.security.InMemorySecurityStore;
import org.eclipse.leshan.server.security.NonUniqueSecurityInfoException;
import org.eclipse.leshan.server.security.SecurityInfo;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class OscoreBootstrapTest {

    LeshanTestBootstrapServerBuilder givenBootstrapServer;
    LeshanTestBootstrapServer bootstrapServer;
    LeshanTestServerBuilder givenServer;
    LeshanTestServer server;
    LeshanTestClientBuilder givenClient;
    LeshanTestClient client;

    @BeforeEach
    public void start() {
        givenBootstrapServer = givenBootstrapServerUsing(Protocol.COAP).with("Californium-OSCORE");
        givenServer = givenServerUsing(Protocol.COAP).with(new InMemorySecurityStore()).with("Californium-OSCORE");
        givenClient = givenClientUsing(Protocol.COAP).with("Californium-OSCORE");
    }

    @AfterEach
    public void stop() throws InterruptedException {
        if (client != null)
            client.destroy(false);
        if (server != null)
            server.destroy();
    }

    public static OscoreSetting getServerOscoreSetting() {
        return new OscoreSetting(OSCORE_RECIPIENT_ID, OSCORE_SENDER_ID, OSCORE_MASTER_SECRET, OSCORE_AEAD_ALGORITHM,
                OSCORE_HKDF_ALGORITHM, OSCORE_MASTER_SALT);
    }

    protected static OscoreSetting getClientOscoreSetting() {
        return new OscoreSetting(OSCORE_SENDER_ID, OSCORE_RECIPIENT_ID, OSCORE_MASTER_SECRET, OSCORE_AEAD_ALGORITHM,
                OSCORE_HKDF_ALGORITHM, OSCORE_MASTER_SALT);
    }

    public static OscoreSetting getBootstrapServerOscoreSetting() {
        return new OscoreSetting(OSCORE_BOOTSTRAP_RECIPIENT_ID, OSCORE_BOOTSTRAP_SENDER_ID,
                OSCORE_BOOTSTRAP_MASTER_SECRET, OSCORE_AEAD_ALGORITHM, OSCORE_HKDF_ALGORITHM,
                OSCORE_BOOTSTRAP_MASTER_SALT);
    }

    protected static OscoreSetting getBootstrapClientOscoreSetting() {
        return new OscoreSetting(OSCORE_BOOTSTRAP_SENDER_ID, OSCORE_BOOTSTRAP_RECIPIENT_ID,
                OSCORE_BOOTSTRAP_MASTER_SECRET, OSCORE_AEAD_ALGORITHM, OSCORE_HKDF_ALGORITHM,
                OSCORE_BOOTSTRAP_MASTER_SALT);
    }

    protected static BootstrapConfig.OscoreObject getOscoreBootstrapObject(OscoreSetting oscoreSetting) {
        BootstrapConfig.OscoreObject oscoreObject = new BootstrapConfig.OscoreObject();

        oscoreObject.oscoreMasterSecret = oscoreSetting.getMasterSecret();
        oscoreObject.oscoreSenderId = oscoreSetting.getSenderId();
        oscoreObject.oscoreRecipientId = oscoreSetting.getRecipientId();
        oscoreObject.oscoreAeadAlgorithm = oscoreSetting.getAeadAlgorithm().getValue();
        oscoreObject.oscoreHmacAlgorithm = oscoreSetting.getHkdfAlgorithm().getValue();
        oscoreObject.oscoreMasterSalt = oscoreSetting.getMasterSalt();

        return oscoreObject;
    }

    @Test
    public void bootstrapUnsecuredToServerWithOscore()
            throws NonUniqueSecurityInfoException, InvalidConfigurationException {
        // Create DM Server without security & start it
        server = givenServer.build();
        server.start();

        // Create and start bootstrap server
        bootstrapServer = givenBootstrapServer.build();
        bootstrapServer.start();

        // Create Client and check it is not already registered
        client = givenClient.connectingTo(bootstrapServer).enabling(LwM2mId.OSCORE, Oscore.class).build();
        assertThat(client).isNotRegisteredAt(server);

        // Add client credentials to the server
        server.getSecurityStore().add(SecurityInfo.newOscoreInfo(client.getEndpointName(), getServerOscoreSetting()));

        // Add config for this client
        bootstrapServer.getConfigStore().add(client.getEndpointName(), //
                givenBootstrapConfig() //
                        .adding(Protocol.COAP, bootstrapServer) //
                        .adding(Protocol.COAP, server, getOscoreBootstrapObject(getClientOscoreSetting())) //
                        .build());

        // Start it and wait for registration
        client.start();
        server.waitForNewRegistrationOf(client);

        // check the client is registered
        assertThat(client).isRegisteredAt(server);
    }

    @Test
    public void bootstrapViaOscoreToServerWithOscore()
            throws NonUniqueSecurityInfoException, InvalidConfigurationException {
        // Create DM Server without security & start it
        server = givenServer.build();
        server.start();

        // Create and start bootstrap server
        bootstrapServer = givenBootstrapServer.with(new InMemorySecurityStore()).build();
        bootstrapServer.start();

        // Create Client and check it is not already registered
        client = givenClient.connectingTo(bootstrapServer).using(getBootstrapClientOscoreSetting()).build();
        assertThat(client).isNotRegisteredAt(server);

        // Add client credentials to the Bootstrap server
        bootstrapServer.getEditableSecurityStore()
                .add(SecurityInfo.newOscoreInfo(client.getEndpointName(), getBootstrapServerOscoreSetting()));

        // Add config for this client
        bootstrapServer.getConfigStore().add(client.getEndpointName(), //
                givenBootstrapConfig() //
                        .adding(Protocol.COAP, bootstrapServer,
                                getOscoreBootstrapObject(getBootstrapClientOscoreSetting())) //
                        .adding(Protocol.COAP, server, getOscoreBootstrapObject(getClientOscoreSetting())) //
                        .build());

        // Add client credentials to the server
        server.getSecurityStore().add(SecurityInfo.newOscoreInfo(client.getEndpointName(), getServerOscoreSetting()));

        // Start it and wait for registration
        client.start();
        server.waitForNewRegistrationOf(client);

        // check the client is registered
        assertThat(client).isRegisteredAt(server);

    }

    @Test
    public void bootstrapViaOscoreToUnsecuredServer()
            throws OSException, NonUniqueSecurityInfoException, InvalidConfigurationException {
        // Create DM Server without security & start it
        server = givenServer.build();
        server.start();

        // Create and start bootstrap server
        bootstrapServer = givenBootstrapServer.with(new InMemorySecurityStore()).build();
        bootstrapServer.start();

        // Create Client and check it is not already registered
        client = givenClient.connectingTo(bootstrapServer).using(getBootstrapClientOscoreSetting()).build();
        assertThat(client).isNotRegisteredAt(server);

        // Add client credentials to the Bootstrap server
        bootstrapServer.getEditableSecurityStore()
                .add(SecurityInfo.newOscoreInfo(client.getEndpointName(), getBootstrapServerOscoreSetting()));

        // Add config for this client
        bootstrapServer.getConfigStore().add(client.getEndpointName(), //
                givenBootstrapConfig() //
                        .adding(Protocol.COAP, bootstrapServer,
                                getOscoreBootstrapObject(getBootstrapClientOscoreSetting())) //
                        .adding(Protocol.COAP, server) //
                        .build());

        // Start it and wait for registration
        client.start();
        server.waitForNewRegistrationOf(client);

        // check the client is registered
        assertThat(client).isRegisteredAt(server);
    }
}
