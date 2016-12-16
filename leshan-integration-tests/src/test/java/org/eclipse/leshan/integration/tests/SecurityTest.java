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


import static org.eclipse.leshan.integration.tests.IntegrationTestHelper.LIFETIME;
import static org.eclipse.leshan.integration.tests.SecureIntegrationTestHelper.*;
import static org.junit.Assert.*;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.Certificate;

import org.eclipse.leshan.server.client.Registration;
import org.eclipse.leshan.server.security.NonUniqueSecurityInfoException;
import org.eclipse.leshan.server.security.SecurityInfo;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

public class SecurityTest {

    protected SecureIntegrationTestHelper helper = new SecureIntegrationTestHelper();

    @Before
    public void start() {
        helper.initialize();
    }

    @After
    public void stop() {
        helper.client.stop(true);
        helper.server.destroy();
        helper.deregisterClient();
        helper.dispose();
    }

    @Test
    public void registered_device_with_psk_to_server_with_psk() throws NonUniqueSecurityInfoException {
        // Create PSK server & start it
        helper.createServer(); // default server support PSK
        helper.server.start();

        // Create PSK Client
        helper.createPSKClient();

        // Add client credentials to the server
        helper.getSecurityStore()
                .add(SecurityInfo.newPreSharedKeyInfo(helper.getCurrentEndpoint(), GOOD_PSK_ID, GOOD_PSK_KEY));

        // Check client is not registered
        helper.assertClientNotRegisterered();

        // Start it and wait for registration
        helper.client.start();
        helper.waitForRegistration(1);

        // Check client is well registered
        helper.assertClientRegisterered();
    }

    @Test
    public void register_update_deregister_reregister_device_with_psk_to_server_with_psk()
            throws NonUniqueSecurityInfoException {
        // Create PSK server & start it
        helper.createServer(); // default server support PSK
        helper.server.start();

        // Create PSK Client
        helper.createPSKClient();

        // Add client credentials to the server
        helper.getSecurityStore()
                .add(SecurityInfo.newPreSharedKeyInfo(helper.getCurrentEndpoint(), GOOD_PSK_ID, GOOD_PSK_KEY));

        // Check client is not registered
        helper.assertClientNotRegisterered();

        // Start it and wait for registration
        helper.client.start();
        helper.waitForRegistration(1);

        // Check client is well registered
        helper.assertClientRegisterered();

        // Check for update
        helper.waitForUpdate(LIFETIME);
        helper.assertClientRegisterered();

        // Check de-registration
        helper.client.stop(true);
        helper.waitForDeregistration(1);
        helper.assertClientNotRegisterered();

        // check new registration
        helper.resetLatch();
        helper.client.start();
        helper.waitForRegistration(1);
        helper.assertClientRegisterered();
    }

    @Test
    public void register_update_reregister_device_with_psk_to_server_with_psk() throws NonUniqueSecurityInfoException {
        // Create PSK server & start it
        helper.createServer(); // default server support PSK
        helper.server.start();

        // Create PSK Client
        helper.createPSKClient();

        // Add client credentials to the server
        helper.getSecurityStore()
                .add(SecurityInfo.newPreSharedKeyInfo(helper.getCurrentEndpoint(), GOOD_PSK_ID, GOOD_PSK_KEY));

        // Check for registration
        helper.assertClientNotRegisterered();
        helper.client.start();
        helper.waitForRegistration(1);
        Registration registration = helper.getCurrentRegistration();
        helper.assertClientRegisterered();

        // Check for update
        helper.waitForUpdate(LIFETIME);
        helper.assertClientRegisterered();

        // Check stop do not de-register
        helper.client.stop(false);
        helper.waitForDeregistration(1);
        helper.assertClientRegisterered();

        // Check new registration
        helper.resetLatch();
        helper.client.start();
        helper.waitForRegistration(1);
        helper.assertClientRegisterered();
        Registration newRegistration = helper.getCurrentRegistration();
        assertNotEquals(registration.getId(), newRegistration.getId());

    }

    @Test
    public void registered_device_with_bad_psk_identity_to_server_with_psk() throws NonUniqueSecurityInfoException {
        // Create PSK server & start it
        helper.createServer(); // default server support PSK
        helper.server.start();

        // Create PSK Client
        helper.createPSKClient();

        // Add client credentials with BAD PSK ID to the server
        helper.getSecurityStore()
                .add(SecurityInfo.newPreSharedKeyInfo(helper.getCurrentEndpoint(), BAD_PSK_ID, GOOD_PSK_KEY));

        // Check client can not register
        helper.assertClientNotRegisterered();
        helper.client.start();
        boolean timedout = !helper.waitForRegistration(1);

        assertTrue(timedout);
    }

    @Test
    public void registered_device_with_bad_psk_key_to_server_with_psk() throws NonUniqueSecurityInfoException {
        // Create PSK server & start it
        helper.createServer(); // default server support PSK
        helper.server.start();

        // Create PSK Client
        helper.createPSKClient();

        // Add client credentials with BAD PSK KEY to the server
        helper.getSecurityStore()
                .add(SecurityInfo.newPreSharedKeyInfo(helper.getCurrentEndpoint(), GOOD_PSK_ID, BAD_PSK_KEY));

        // Check client can not register
        helper.assertClientNotRegisterered();
        helper.client.start();
        boolean timedout = !helper.waitForRegistration(1);

        assertTrue(timedout);
    }

    @Test
    public void registered_device_with_psk_and_bad_endpoint_to_server_with_psk() throws NonUniqueSecurityInfoException {
        // Create PSK server & start it
        helper.createServer(); // default server support PSK
        helper.server.start();

        // Create PSK Client
        helper.createPSKClient();

        // Add client credentials for another endpoint to the server
        helper.getSecurityStore()
                .add(SecurityInfo.newPreSharedKeyInfo(BAD_ENDPOINT, GOOD_PSK_ID, GOOD_PSK_KEY));

        // Check client can not register
        helper.assertClientNotRegisterered();
        helper.client.start();
        boolean timedout = !helper.waitForRegistration(1);

        assertTrue(timedout);
    }

    @Ignore
    // TODO implement RPK support for client
    @Test
    public void registered_device_with_rpk_to_server_with_rpk() throws NonUniqueSecurityInfoException {
        helper.createServerWithRPK();
        helper.server.start();

        helper.createRPKClient();

        helper.getSecurityStore()
                .add(SecurityInfo.newRawPublicKeyInfo(helper.getCurrentEndpoint(), helper.clientPublicKey));

        helper.client.start();
        helper.waitForRegistration(1);

        assertEquals(1, helper.server.getRegistrationService().getAllRegistrations().size());
        assertNotNull(helper.getCurrentRegistration());
    }

    @Ignore
    // TODO implement RPK support for client
    @Test
    public void registered_device_with_bad_rpk_to_server_with_rpk() throws NonUniqueSecurityInfoException {
        helper.createServerWithRPK();
        helper.server.start();

        helper.createRPKClient();

        // as it is complex to create a public key, I use the server one :p as bad client public key
        PublicKey bad_client_public_key = helper.getServerPublicKey();
        helper.getSecurityStore()
                .add(SecurityInfo.newRawPublicKeyInfo(helper.getCurrentEndpoint(), bad_client_public_key));

        helper.client.start();
        boolean timedout = !helper.waitForRegistration(1);

        assertTrue(timedout);
    }

    @Ignore
    // TODO implement RPK support for client
    @Test
    public void registered_device_with_rpk_and_bad_endpoint_to_server_with_rpk() throws NonUniqueSecurityInfoException {
        helper.createServerWithRPK();
        helper.server.start();

        helper.createRPKClient();

        helper.getSecurityStore()
                .add(SecurityInfo.newRawPublicKeyInfo("bad_endpoint", helper.clientPublicKey));

        helper.client.start();
        boolean timedout = !helper.waitForRegistration(1);

        assertTrue(timedout);
    }

    @Ignore
    // TODO implement X509 support for client
    @Test
    public void registered_device_with_x509cert_to_server_with_x509cert() throws NonUniqueSecurityInfoException {
        helper.createServerWithX509Cert(helper.trustedCertificates);
        helper.server.start();

        helper.createX509CertClient(helper.clientPrivateKeyFromCert, helper.trustedCertificates);

        helper.getSecurityStore().add(SecurityInfo.newX509CertInfo(helper.getCurrentEndpoint()));

        helper.client.start();
        helper.waitForRegistration(1);

        assertEquals(1, helper.server.getRegistrationService().getAllRegistrations().size());
        assertNotNull(helper.getCurrentRegistration());
    }

    @Ignore
    // TODO implement X509 support for client
    @Test
    public void registered_device_with_x509cert_and_bad_endpoint_to_server_with_x509cert()
            throws NonUniqueSecurityInfoException {
        helper.createServerWithX509Cert(helper.trustedCertificates);
        helper.server.start();

        helper.createX509CertClient(helper.clientPrivateKeyFromCert, helper.trustedCertificates);

        helper.getSecurityStore().add(SecurityInfo.newX509CertInfo("bad_endpoint"));

        helper.client.start();
        boolean timedout = !helper.waitForRegistration(1);

        assertTrue(timedout);
    }

    @Ignore
    // TODO implement X509 support for client
    @Test
    public void registered_device_with_x509cert_and_bad_cn_certificate_to_server_with_x509cert()
            throws NonUniqueSecurityInfoException {
        helper.createServerWithX509Cert(helper.trustedCertificates);
        helper.server.start();

        helper.createX509CertClient(helper.clientPrivateKeyFromCert, helper.trustedCertificates);

        helper.getSecurityStore().add(SecurityInfo.newX509CertInfo("good_endpoint"));

        helper.client.start();
        boolean timedout = !helper.waitForRegistration(1);

        assertTrue(timedout);
    }

    @Ignore
    // TODO implement X509 support for client
    @Test
    public void registered_device_with_x509cert_and_bad_private_key_to_server_with_x509cert()
            throws NonUniqueSecurityInfoException {
        helper.createServerWithX509Cert(helper.trustedCertificates);
        helper.server.start();

        // we use the server private key as bad key, this key will not be compatible with the client certificate
        PrivateKey badPrivateKey = helper.serverPrivateKey;

        helper.createX509CertClient(badPrivateKey, helper.trustedCertificates);
        helper.getSecurityStore().add(SecurityInfo.newX509CertInfo(helper.getCurrentEndpoint()));

        helper.client.start();
        boolean timedout = !helper.waitForRegistration(1);

        assertTrue(timedout);
    }

    @Ignore
    // TODO implement X509 support for client
    @Test
    public void registered_device_with_x509cert_and_untrusted_CA_to_server_with_x509cert()
            throws NonUniqueSecurityInfoException {
        // the server will not trust the client Certificate authority
        helper.createServerWithX509Cert(new Certificate[] { helper.serverCAX509Cert });
        helper.server.start();

        helper.createX509CertClient(helper.clientPrivateKeyFromCert, helper.trustedCertificates);

        helper.getSecurityStore().add(SecurityInfo.newX509CertInfo(helper.getCurrentEndpoint()));

        helper.client.start();
        boolean timedout = !helper.waitForRegistration(1);

        assertTrue(timedout);
    }

    @Ignore
    // TODO implement X509 support for client
    @Test
    public void registered_device_with_x509cert_to_server_with_rpk() throws NonUniqueSecurityInfoException {
        helper.createServerWithRPK();
        helper.server.start();

        helper.createX509CertClient(helper.clientPrivateKeyFromCert, helper.trustedCertificates);

        helper.getSecurityStore().add(
                SecurityInfo.newRawPublicKeyInfo(helper.getCurrentEndpoint(),
                        helper.clientX509CertChain[0].getPublicKey()));

        helper.client.start();
        helper.waitForRegistration(1);

        assertEquals(1, helper.server.getRegistrationService().getAllRegistrations().size());
        assertNotNull(helper.getCurrentRegistration());
    }

    @Ignore
    // TODO implement RPK support for client
    @Test
    public void registered_device_with_rpk_to_server_with_x509cert() throws NonUniqueSecurityInfoException {
        helper.createServerWithX509Cert(helper.trustedCertificates);
        helper.server.start();

        helper.createRPKClient();
        helper.client.start();

        helper.getSecurityStore()
                .add(SecurityInfo.newRawPublicKeyInfo(helper.getCurrentEndpoint(), helper.clientPublicKey));

        helper.client.start();
        helper.waitForRegistration(1);

        assertEquals(1, helper.server.getRegistrationService().getAllRegistrations().size());
        assertNotNull(helper.getCurrentRegistration());
    }
}
