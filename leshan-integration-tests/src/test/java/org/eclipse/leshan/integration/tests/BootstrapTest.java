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

import static org.eclipse.leshan.integration.tests.BootstrapIntegrationTestHelper.*;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class BootstrapTest {

    private final BootstrapIntegrationTestHelper helper = new BootstrapIntegrationTestHelper();

    @Before
    public void start() {
        helper.initialize();
        helper.createServer(); // DM server
        helper.server.start();
    }

    @After
    public void stop() {
        helper.client.stop(true);
        helper.bootstrapServer.stop();
        helper.server.stop();
        helper.dispose();
    }

    @Test
    public void bootstrap() {
        // Create and start bootstrap server
        helper.createBootstrapServer(null);
        helper.bootstrapServer.start();

        // Create Client and check it is not already registered
        helper.createClient();
        helper.assertClientNotRegisterered();

        // Start it and wait for registration
        helper.client.start();
        helper.waitForRegistration(1);

        // check the client is registered
        helper.assertClientRegisterered();
    }

    @Test
    public void bootstrapSecure() {
        // Create and start bootstrap server
        helper.createBootstrapServer(helper.bsSecurityStore());
        helper.bootstrapServer.start();

        // Create PSK Client and check it is not already registered
        helper.createPSKClient(GOOD_PSK_ID, GOOD_PSK_KEY);
        helper.assertClientNotRegisterered();

        // Start it and wait for registration
        helper.client.start();
        helper.waitForRegistration(1);

        // check the client is registered
        helper.assertClientRegisterered();
    }

    @Test
    public void bootstrapSecureWithBadCredentials() {
        // Create and start bootstrap server
        helper.createBootstrapServer(helper.bsSecurityStore());
        helper.bootstrapServer.start();

        // Create PSK Client with bad credentials and check it is not already registered
        helper.createPSKClient(GOOD_PSK_ID, BAD_PSK_KEY);
        helper.assertClientNotRegisterered();

        // Start it and wait for registration
        helper.client.start();
        helper.waitForRegistration(1);

        // check the client is registered
        helper.assertClientNotRegisterered();
    }

}
