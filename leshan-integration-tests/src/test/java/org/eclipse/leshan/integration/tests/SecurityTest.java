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

import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.Certificate;

import org.eclipse.leshan.ResponseCode;
import org.eclipse.leshan.core.request.RegisterRequest;
import org.eclipse.leshan.core.response.RegisterResponse;
import org.eclipse.leshan.server.security.NonUniqueSecurityInfoException;
import org.eclipse.leshan.server.security.SecurityInfo;
import org.junit.After;
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
    @Test
    public void registered_device_with_bad_psk_identity_to_server_with_psk() throws NonUniqueSecurityInfoException {
        helper.createServer(); // default server support PSK
        helper.server.start();

        helper.createPSKClient();
        helper.client.start();

        helper.server.getSecurityRegistry().add(
                SecurityInfo.newPreSharedKeyInfo(ENDPOINT_IDENTIFIER, "bad_psk_identity", helper.pskKey));

        // client registration
        RegisterResponse response = helper.client.send(new RegisterRequest(ENDPOINT_IDENTIFIER), 500);

        // verify result
        assertEquals(null, response);
    }

    // The good point is that client with bad credential can not connect to the server but
    // TODO I am not sure this must end with a timeout ...
    @Test
    public void registered_device_with_bad_psk_key_to_server_with_psk() throws NonUniqueSecurityInfoException {
        helper.createServer(); // default server support PSK
        helper.server.start();

        helper.createPSKClient();
        helper.client.start();

        helper.server.getSecurityRegistry().add(
                SecurityInfo.newPreSharedKeyInfo(ENDPOINT_IDENTIFIER, helper.pskIdentity, "bad_key".getBytes()));

        // client registration
        RegisterResponse response = helper.client.send(new RegisterRequest(ENDPOINT_IDENTIFIER), 500);

        // verify result
        assertEquals(null, response);
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

        // as it is complex to create a public key, I use the server one :p as bad client public key
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
    public void registered_device_with_x509cert_to_server_with_x509cert() throws NonUniqueSecurityInfoException {
        helper.createServerWithX509Cert(helper.trustedCertificates);
        helper.server.start();

        helper.createX509CertClient(helper.clientPrivateKeyFromCert, helper.trustedCertificates);
        helper.client.start();

        helper.server.getSecurityRegistry().add(SecurityInfo.newX509CertInfo(ENDPOINT_IDENTIFIER));

        // client registration
        RegisterResponse response = helper.client.send(new RegisterRequest(ENDPOINT_IDENTIFIER));

        // verify result
        assertEquals(ResponseCode.CREATED, response.getCode());
    }

    @Test
    public void registered_device_with_x509cert_and_bad_endpoint_to_server_with_x509cert()
            throws NonUniqueSecurityInfoException {
        helper.createServerWithX509Cert(helper.trustedCertificates);
        helper.server.start();

        helper.createX509CertClient(helper.clientPrivateKeyFromCert, helper.trustedCertificates);
        helper.client.start();

        helper.server.getSecurityRegistry().add(SecurityInfo.newX509CertInfo("bad_endpoint"));

        // client registration
        RegisterResponse response = helper.client.send(new RegisterRequest(ENDPOINT_IDENTIFIER));

        // verify result
        assertEquals(ResponseCode.FORBIDDEN, response.getCode());
    }

    @Test
    public void registered_device_with_x509cert_and_bad_cn_certificate_to_server_with_x509cert()
            throws NonUniqueSecurityInfoException {
        helper.createServerWithX509Cert(helper.trustedCertificates);
        helper.server.start();

        helper.createX509CertClient(helper.clientPrivateKeyFromCert, helper.trustedCertificates);
        helper.client.start();

        helper.server.getSecurityRegistry().add(SecurityInfo.newX509CertInfo("good_endpoint"));

        // client registration
        RegisterResponse response = helper.client.send(new RegisterRequest("good_endpoint"));

        // verify result
        assertEquals(ResponseCode.FORBIDDEN, response.getCode());
    }

    // TODO HandshakeException not re-thrown in cf CoapServer.start() when calling CoapEndpoint.start()
    // Exception origin : CertificateVerify.verifySignature()
    // failed with timeout
    @Test
    public void registered_device_with_x509cert_and_bad_private_key_to_server_with_x509cert()
            throws NonUniqueSecurityInfoException {
        helper.createServerWithX509Cert(helper.trustedCertificates);
        helper.server.start();

        // we use the server private key as bad key, this key will not be compatible with the client certificate
        PrivateKey badPrivateKey = helper.serverPrivateKey;

        helper.createX509CertClient(badPrivateKey, helper.trustedCertificates);
        helper.client.start();

        helper.server.getSecurityRegistry().add(SecurityInfo.newX509CertInfo(ENDPOINT_IDENTIFIER));

        // client registration
        RegisterResponse response = helper.client.send(new RegisterRequest(ENDPOINT_IDENTIFIER), 500);

        // verify result
        assertEquals(null, response);
    }

    // TODO HandshakeException not re-thrown in cf CoapServer.start() when calling CoapEndpoint.start()
    // Exception origin : CertificateMessage.verifyCertificate()
    // failed with timeout
    @Test
    public void registered_device_with_x509cert_and_untrusted_CA_to_server_with_x509cert()
            throws NonUniqueSecurityInfoException {
        // the server will not trust the client Certificate authority
        helper.createServerWithX509Cert(new Certificate[] { helper.serverCAX509Cert });
        helper.server.start();

        helper.createX509CertClient(helper.clientPrivateKeyFromCert, helper.trustedCertificates);
        helper.client.start();

        helper.server.getSecurityRegistry().add(SecurityInfo.newX509CertInfo(ENDPOINT_IDENTIFIER));

        // client registration
        RegisterResponse response = helper.client.send(new RegisterRequest(ENDPOINT_IDENTIFIER), 500);

        // verify result
        assertEquals(null, response);
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

    @Test
    public void registered_device_with_psk_and_x509cert_to_server_with_psk() throws NonUniqueSecurityInfoException {
        helper.createServer(); // default server support PSK
        helper.server.start();

        helper.createPSKandX509CertClient();
        helper.client.start();

        helper.server.getSecurityRegistry().add(
                SecurityInfo.newPreSharedKeyInfo(ENDPOINT_IDENTIFIER, helper.pskIdentity, helper.pskKey));

        // client registration
        RegisterResponse response = helper.client.send(new RegisterRequest(ENDPOINT_IDENTIFIER));

        // verify result
        assertEquals(ResponseCode.CREATED, response.getCode());
    }

    @Test
    public void registered_device_with_psk_and_x509cert_to_server_with_x509cert() throws NonUniqueSecurityInfoException {
        helper.createServerWithX509Cert(helper.trustedCertificates);
        helper.server.start();

        helper.createPSKandX509CertClient();
        helper.client.start();

        helper.server.getSecurityRegistry().add(SecurityInfo.newX509CertInfo(ENDPOINT_IDENTIFIER));

        // client registration
        RegisterResponse response = helper.client.send(new RegisterRequest(ENDPOINT_IDENTIFIER));

        // verify result
        assertEquals(ResponseCode.CREATED, response.getCode());
    }

    @Test
    public void registered_device_with_x509cert_to_server_with_rpk() throws NonUniqueSecurityInfoException {
        helper.createServerWithRPK();
        helper.server.start();

        helper.createX509CertClient(helper.clientPrivateKeyFromCert, helper.trustedCertificates);
        helper.client.start();

        helper.server.getSecurityRegistry().add(
                SecurityInfo.newRawPublicKeyInfo(ENDPOINT_IDENTIFIER, helper.clientX509CertChain[0].getPublicKey()));

        // client registration
        RegisterResponse response = helper.client.send(new RegisterRequest(ENDPOINT_IDENTIFIER));

        // verify result
        assertEquals(ResponseCode.CREATED, response.getCode());
    }

    @Test
    public void registered_device_with_rpk_to_server_with_x509cert() throws NonUniqueSecurityInfoException {
        helper.createServerWithX509Cert(helper.trustedCertificates);
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
}
