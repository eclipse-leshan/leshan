/*******************************************************************************
 * Copyright (c) 2015 Sierra Wireless and others.
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v1.0 which accompany this distribution.
 * 
 * The Eclipse Public License is available at
 *    http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 *    http://www.eclipse.org/org/documents/edl-v10.html.
 * 
 * Contributors:
 *     Sierra Wireless - initial API and implementation
 *******************************************************************************/
package org.eclipse.leshan.integration.tests;

import static org.eclipse.leshan.integration.tests.SecureIntegrationTestHelper.*;

import org.eclipse.leshan.SecurityMode;
import org.eclipse.leshan.server.security.NonUniqueSecurityInfoException;
import org.eclipse.leshan.server.security.SecurityInfo;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class BootstrapTest {

    private final BootstrapIntegrationTestHelper helper = new BootstrapIntegrationTestHelper();

    @Before
    public void start() {
        helper.initialize();
    }

    @After
    public void stop() {
        helper.client.destroy(true);
        helper.bootstrapServer.destroy();
        helper.server.destroy();
        helper.dispose();
    }

    @Test
    public void bootstrap() {
        // Create DM Server without security & start it
        helper.createServer();
        helper.server.start();

        // Create and start bootstrap server
        helper.createBootstrapServer(null);
        helper.bootstrapServer.start();

        // Create Client and check it is not already registered
        helper.createClient();
        helper.assertClientNotRegisterered();

        // Start it and wait for registration
        helper.client.start();
        helper.waitForRegistrationAtServerSide(1);

        // check the client is registered
        helper.assertClientRegisterered();
    }

    @Test
    public void bootstrapSecureWithPSK() {
        // Create DM Server without security & start it
        helper.createServer();
        helper.server.start();

        // Create and start bootstrap server
        helper.createBootstrapServer(helper.bsSecurityStore(SecurityMode.PSK));
        helper.bootstrapServer.start();

        // Create PSK Client and check it is not already registered
        helper.createPSKClient(GOOD_PSK_ID, GOOD_PSK_KEY);
        helper.assertClientNotRegisterered();

        // Start it and wait for registration
        helper.client.start();
        helper.waitForRegistrationAtServerSide(1);

        // check the client is registered
        helper.assertClientRegisterered();
    }

    @Test
    public void bootstrapSecureWithBadPSKKey() {
        // Create DM Server without security & start it
        helper.createServer();
        helper.server.start();

        // Create and start bootstrap server
        helper.createBootstrapServer(helper.bsSecurityStore(SecurityMode.PSK));
        helper.bootstrapServer.start();

        // Create PSK Client with bad credentials and check it is not already registered
        helper.createRPKClient();
        helper.assertClientNotRegisterered();

        // Start it and wait for registration
        helper.client.start();
        helper.ensureNoRegistration(1);

        // check the client is not registered
        helper.assertClientNotRegisterered();
    }

    @Test
    public void bootstrapSecureWithRPK() {
        // Create DM Server without security & start it
        helper.createServer();
        helper.server.start();

        // Create and start bootstrap server
        helper.createBootstrapServer(helper.bsSecurityStore(SecurityMode.RPK));
        helper.bootstrapServer.start();

        // Create RPK Client and check it is not already registered
        helper.createRPKClient();
        helper.assertClientNotRegisterered();

        // Start it and wait for registration
        helper.client.start();
        helper.waitForRegistrationAtServerSide(1);

        // check the client is registered
        helper.assertClientRegisterered();
    }

    @Test
    public void bootstrapToPSKServer() throws NonUniqueSecurityInfoException {
        // Create DM Server & start it
        helper.createServer(); // default server support PSK
        helper.server.start();

        // Create and start bootstrap server
        helper.createBootstrapServer(null, helper.pskBootstrapStore());
        helper.bootstrapServer.start();

        // Create Client and check it is not already registered
        helper.createClient();
        helper.assertClientNotRegisterered();

        // Add client credentials to the server
        helper.getSecurityStore()
                .add(SecurityInfo.newPreSharedKeyInfo(helper.getCurrentEndpoint(), GOOD_PSK_ID, GOOD_PSK_KEY));

        // Start it and wait for registration
        helper.client.start();
        helper.waitForRegistrationAtServerSide(1);

        // check the client is registered
        helper.assertClientRegisterered();
    }

    @Test
    public void bootstrapToRPKServer() throws NonUniqueSecurityInfoException {
        // Create DM Server with RPK support & start it
        helper.createServerWithRPK();
        helper.server.start();

        // Create and start bootstrap server
        helper.createBootstrapServer(null, helper.rpkBootstrapStore());
        helper.bootstrapServer.start();

        // Create Client and check it is not already registered
        helper.createClient();
        helper.assertClientNotRegisterered();

        // Add client credentials to the server
        helper.getSecurityStore()
                .add(SecurityInfo.newRawPublicKeyInfo(helper.getCurrentEndpoint(), helper.clientPublicKey));

        // Start it and wait for registration
        helper.client.start();
        helper.waitForRegistrationAtServerSide(1);

        // check the client is registered
        helper.assertClientRegisterered();
    }
}
