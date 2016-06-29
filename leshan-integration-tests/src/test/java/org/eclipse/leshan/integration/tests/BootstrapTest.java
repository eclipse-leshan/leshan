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

import static org.junit.Assert.*;

import org.eclipse.leshan.util.Hex;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class BootstrapTest {

    private final BootstrapIntegrationTestHelper helper = new BootstrapIntegrationTestHelper();

    @Before
    public void start() {
        // DM server
        helper.createServer();
        helper.server.start();

    }

    @After
    public void stop() {

        helper.client.stop(true);
        helper.bootstrapServer.stop();
        helper.server.stop();
    }

    @Test
    public void bootstrap() {
        // Bootstrap server
        helper.createBootstrapServer(null);
        helper.bootstrapServer.start();

        helper.createClient();
        helper.client.start();

        helper.waitForRegistration(1);

        // check the client is registered
        assertEquals(1, helper.server.getClientRegistry().allClients().size());
        assertNotNull(helper.server.getClientRegistry().get(BootstrapIntegrationTestHelper.ENDPOINT_IDENTIFIER));
    }

    @Test
    public void bootstrapSecure() {

        // Bootstrap server
        helper.createBootstrapServer(BootstrapIntegrationTestHelper.bsSecurityStore());
        helper.bootstrapServer.start();

        helper.createPSKClient("Client_identity", Hex.decodeHex("73656372657450534b".toCharArray()));
        helper.client.start();

        helper.waitForRegistration(1);

        assertEquals(1, helper.server.getClientRegistry().allClients().size());
        assertNotNull(helper.server.getClientRegistry().get(IntegrationTestHelper.ENDPOINT_IDENTIFIER));
    }

    @Test
    public void bootstrapSecureWithBadCredentials() {

        // Bootstrap server
        helper.createBootstrapServer(BootstrapIntegrationTestHelper.bsSecurityStore());
        helper.bootstrapServer.start();

        helper.createPSKClient("Client_identity", Hex.decodeHex("010101010101010101".toCharArray()));
        helper.client.start();

        helper.waitForRegistration(1);

        assertEquals(0, helper.server.getClientRegistry().allClients().size());
        assertNull(helper.server.getClientRegistry().get(IntegrationTestHelper.ENDPOINT_IDENTIFIER));
    }

}
