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

import static org.eclipse.leshan.integration.tests.IntegrationTestHelper.ENDPOINT_IDENTIFIER;
import static org.junit.Assert.assertEquals;

import java.security.PublicKey;

import org.eclipse.leshan.ResponseCode;
import org.eclipse.leshan.core.request.RegisterRequest;
import org.eclipse.leshan.core.request.exception.RequestTimeoutException;
import org.eclipse.leshan.core.response.RegisterResponse;
import org.eclipse.leshan.server.security.NonUniqueSecurityInfoException;
import org.eclipse.leshan.server.security.SecurityInfo;
import org.junit.After;
import org.junit.Ignore;
import org.junit.Test;

public class SecurityTest {

    private final SecureIntegrationTestHelper helper = new SecureIntegrationTestHelper();

    @After
    public void stop() {
        helper.client.stop();
        helper.server.destroy();
    }

    @Test
    public void registered_device_with_psk_to_server_with_psk() throws NonUniqueSecurityInfoException {
        helper.createServer(); // default server support PSK
        helper.server.start();

        helper.createPSKClient();
        helper.client.start();

        helper.server.getSecurityRegistry().add(
                SecurityInfo.newPreSharedKeyInfo(ENDPOINT_IDENTIFIER, helper.pskIdentity, helper.pskKey));

        // client registration
        RegisterResponse response = helper.client.send(new RegisterRequest(ENDPOINT_IDENTIFIER));

        // verify result
        assertEquals(ResponseCode.CREATED, response.getCode());
    }

    // The good point is that client with bad credential can not connect to the server but
    // TODO I am not sure this must end with a timeout ...
    // We will ignore this test case waiting we could configure timeout or we handle this in a better way
    @Ignore
    @Test(expected = RequestTimeoutException.class)
    public void registered_device_with_bad_psk_identity_to_server_with_psk() throws NonUniqueSecurityInfoException {
        helper.createServer(); // default server support PSK
        helper.server.start();

        helper.createPSKClient();
        helper.client.start();

        helper.server.getSecurityRegistry().add(
                SecurityInfo.newPreSharedKeyInfo(ENDPOINT_IDENTIFIER, "bad_psk_identity", helper.pskKey));

        // client registration
        RegisterResponse response = helper.client.send(new RegisterRequest(ENDPOINT_IDENTIFIER));

        // verify result
        assertEquals(ResponseCode.CREATED, response.getCode());
    }

    // The good point is that client with bad credential can not connect to the server but
    // TODO I am not sure this must end with a timeout ...
    // We will ignore this test case waiting we could configure timeout or we handle this in a better way
    @Ignore
    @Test(expected = RequestTimeoutException.class)
    public void registered_device_with_bad_psk_key_to_server_with_psk() throws NonUniqueSecurityInfoException {
        helper.createServer(); // default server support PSK
        helper.server.start();

        helper.createPSKClient();
        helper.client.start();

        helper.server.getSecurityRegistry().add(
                SecurityInfo.newPreSharedKeyInfo(ENDPOINT_IDENTIFIER, helper.pskIdentity, "bad_key".getBytes()));

        // client registration
        RegisterResponse response = helper.client.send(new RegisterRequest(ENDPOINT_IDENTIFIER));

        // verify result
        assertEquals(ResponseCode.CREATED, response.getCode());
    }

    @Test
    public void registered_device_with_psk_and_bad_endpoint_to_server_with_psk() throws NonUniqueSecurityInfoException {
        helper.createServer(); // default server support PSK
        helper.server.start();

        helper.createPSKClient();
        helper.client.start();

        helper.server.getSecurityRegistry().add(
                SecurityInfo.newPreSharedKeyInfo("bad_endpoint", helper.pskIdentity, helper.pskKey));

        // client registration
        RegisterResponse response = helper.client.send(new RegisterRequest(ENDPOINT_IDENTIFIER));

        // verify result
        assertEquals(ResponseCode.FORBIDDEN, response.getCode());
    }

    @Test
    public void registered_device_with_rpk_to_server_with_rpk() throws NonUniqueSecurityInfoException {
        helper.createServerWithRPK();
        helper.server.start();

        helper.createRPKClient();
        helper.client.start();

        helper.server.getSecurityRegistry().add(
                SecurityInfo.newRawPublicKeyInfo(ENDPOINT_IDENTIFIER, helper.clientPublicKey));

        // client registration
        RegisterResponse response = helper.client.send(new RegisterRequest(ENDPOINT_IDENTIFIER));

        // verify result
        assertEquals(ResponseCode.CREATED, response.getCode());
    }

    public void registered_device_with_bad_rpk_to_server_with_rpk() throws NonUniqueSecurityInfoException {
        helper.createServerWithRPK();
        helper.server.start();

        helper.createRPKClient();
        helper.client.start();

        // as it is complex to create a public key, I use the server one :p
        PublicKey bad_client_public_key = helper.server.getSecurityRegistry().getServerPublicKey();
        helper.server.getSecurityRegistry().add(
                SecurityInfo.newRawPublicKeyInfo(ENDPOINT_IDENTIFIER, bad_client_public_key));

        // client registration
        RegisterResponse response = helper.client.send(new RegisterRequest(ENDPOINT_IDENTIFIER));

        // verify result
        assertEquals(ResponseCode.FORBIDDEN, response.getCode());
    }

    @Test
    public void registered_device_with_rpk_and_bad_endpoint_to_server_with_rpk() throws NonUniqueSecurityInfoException {
        helper.createServerWithRPK();
        helper.server.start();

        helper.createRPKClient();
        helper.client.start();

        helper.server.getSecurityRegistry().add(
                SecurityInfo.newRawPublicKeyInfo("bad_endpoint", helper.clientPublicKey));

        // client registration
        RegisterResponse response = helper.client.send(new RegisterRequest(ENDPOINT_IDENTIFIER));

        // verify result
        assertEquals(ResponseCode.FORBIDDEN, response.getCode());
    }

    @Test
    public void registered_device_with_rpk_and_psk_to_server_with_rpk() throws NonUniqueSecurityInfoException {
        helper.createServerWithRPK();
        helper.server.start();

        helper.createPSKandRPKClient();
        helper.client.start();

        helper.server.getSecurityRegistry().add(
                SecurityInfo.newRawPublicKeyInfo(ENDPOINT_IDENTIFIER, helper.clientPublicKey));

        // client registration
        RegisterResponse response = helper.client.send(new RegisterRequest(ENDPOINT_IDENTIFIER));

        // verify result
        assertEquals(ResponseCode.CREATED, response.getCode());
    }

    // TODO there is a bug in scandium server always said that it supports RPK even if no RPK is configured.
    @Ignore
    @Test
    public void registered_device_with_rpk_and_psk_to_server_with_psk() throws NonUniqueSecurityInfoException {
        helper.createServer(); // default server support PSK
        helper.server.start();

        helper.createPSKandRPKClient();
        helper.client.start();

        helper.server.getSecurityRegistry().add(
                SecurityInfo.newPreSharedKeyInfo(ENDPOINT_IDENTIFIER, helper.pskIdentity, helper.pskKey));

        // client registration
        RegisterResponse response = helper.client.send(new RegisterRequest(ENDPOINT_IDENTIFIER));

        // verify result
        assertEquals(ResponseCode.CREATED, response.getCode());
    }
}
