/*******************************************************************************
 * Copyright (c) 2015 Sierra Wireless and others.
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

import static org.eclipse.leshan.integration.tests.util.IntegrationTestHelper.LIFETIME;
import static org.eclipse.leshan.integration.tests.util.SecureIntegrationTestHelper.BAD_ENDPOINT;
import static org.eclipse.leshan.integration.tests.util.SecureIntegrationTestHelper.BAD_PSK_ID;
import static org.eclipse.leshan.integration.tests.util.SecureIntegrationTestHelper.BAD_PSK_KEY;
import static org.eclipse.leshan.integration.tests.util.SecureIntegrationTestHelper.GOOD_ENDPOINT;
import static org.eclipse.leshan.integration.tests.util.SecureIntegrationTestHelper.GOOD_PSK_ID;
import static org.eclipse.leshan.integration.tests.util.SecureIntegrationTestHelper.GOOD_PSK_KEY;
import static org.eclipse.leshan.integration.tests.util.SecureIntegrationTestHelper.getServerOscoreSetting;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.net.URI;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.californium.core.coap.CoAP.Type;
import org.eclipse.californium.core.coap.Request;
import org.eclipse.californium.core.coap.Token;
import org.eclipse.californium.core.network.serialization.UdpDataSerializer;
import org.eclipse.californium.elements.AddressEndpointContext;
import org.eclipse.californium.elements.EndpointContext;
import org.eclipse.californium.elements.RawData;
import org.eclipse.californium.elements.auth.PreSharedKeyIdentity;
import org.eclipse.californium.elements.exception.EndpointMismatchException;
import org.eclipse.californium.scandium.DTLSConnector;
import org.eclipse.californium.scandium.dtls.DTLSSession;
import org.eclipse.leshan.core.CertificateUsage;
import org.eclipse.leshan.core.endpoint.Protocol;
import org.eclipse.leshan.core.request.ReadRequest;
import org.eclipse.leshan.core.request.exception.SendFailedException;
import org.eclipse.leshan.core.request.exception.TimeoutException;
import org.eclipse.leshan.core.request.exception.UnconnectedPeerException;
import org.eclipse.leshan.core.response.ReadResponse;
import org.eclipse.leshan.integration.tests.util.Callback;
import org.eclipse.leshan.integration.tests.util.SecureIntegrationTestHelper;
import org.eclipse.leshan.integration.tests.util.cf.SimpleMessageCallback;
import org.eclipse.leshan.server.registration.Registration;
import org.eclipse.leshan.server.security.EditableSecurityStore;
import org.eclipse.leshan.server.security.NonUniqueSecurityInfoException;
import org.eclipse.leshan.server.security.SecurityInfo;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class SecurityTest {

    protected SecureIntegrationTestHelper helper = new SecureIntegrationTestHelper();

    @BeforeEach
    public void start() {
        helper.initialize();
    }

    @AfterEach
    public void stop() {
        if (helper.client != null)
            helper.client.destroy(true);
        helper.server.destroy();
        helper.dispose();
    }

    @Test
    public void registered_device_with_psk_to_server_with_psk()
            throws NonUniqueSecurityInfoException, InterruptedException {
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
        helper.waitForRegistrationAtServerSide(1);

        // Check client is well registered
        helper.assertClientRegisterered();

        // check we can send request to client.
        ReadResponse response = helper.server.send(helper.getCurrentRegistration(), new ReadRequest(3, 0, 1), 500);
        assertTrue(response.isSuccess());
    }

    @Test
    public void registered_device_with_oscore_to_server_with_oscore()
            throws NonUniqueSecurityInfoException, InterruptedException {

        helper.createOscoreServer();
        helper.server.start();

        helper.createOscoreClient();

        helper.getSecurityStore()
                .add(SecurityInfo.newOscoreInfo(helper.getCurrentEndpoint(), getServerOscoreSetting()));

        // Check client is not registered
        helper.assertClientNotRegisterered();

        // Start it and wait for registration
        helper.client.start();
        helper.waitForRegistrationAtServerSide(1);

        // Check client is well registered
        helper.assertClientRegisterered();

        // check we can send request to client.
        ReadResponse response = helper.server.send(helper.getCurrentRegistration(), new ReadRequest(3, 0, 1), 500);
        assertTrue(response.isSuccess());
    }

    @Test
    public void registered_device_with_oscore_to_server_with_oscore_then_removed_security_info_then_server_fails_to_send_request()
            throws NonUniqueSecurityInfoException, InterruptedException {

        helper.createOscoreServer();
        helper.server.start();

        helper.createOscoreClient();
        helper.getSecurityStore()
                .add(SecurityInfo.newOscoreInfo(helper.getCurrentEndpoint(), getServerOscoreSetting()));

        // Check client is not registered
        helper.assertClientNotRegisterered();

        // Start it and wait for registration
        helper.client.start();
        helper.waitForRegistrationAtServerSide(1);

        // Check client is well registered
        helper.assertClientRegisterered();

        // check we can send request to client.
        ReadResponse response = helper.server.send(helper.getCurrentRegistration(), new ReadRequest(3, 0, 1), 500);
        assertTrue(response.isSuccess());

        // Remove securityInfo
        helper.getSecurityStore().remove(helper.getCurrentEndpoint(), true);

        // check we can send request to client.
        response = helper.server.send(helper.getCurrentRegistration(), new ReadRequest(3, 0, 1), 500);
        assertNull(response);
        // TODO OSCORE we must defined the expected behavior here.
    }

    // TODO OSCORE should failed but does not because context by URI is not removed.
    @Test
    public void registered_device_with_oscore_to_server_with_oscore_then_removed_security_info_then_client_fails_to_update()
            throws NonUniqueSecurityInfoException, InterruptedException {

        helper.createOscoreServer();
        helper.server.start();

        helper.createOscoreClient();
        helper.getSecurityStore()
                .add(SecurityInfo.newOscoreInfo(helper.getCurrentEndpoint(), getServerOscoreSetting()));

        // Check client is not registered
        helper.assertClientNotRegisterered();

        // Start it and wait for registration
        helper.client.start();
        helper.waitForRegistrationAtServerSide(1);

        // Check client is well registered
        helper.assertClientRegisterered();

        // check we can send request to client.
        ReadResponse response = helper.server.send(helper.getCurrentRegistration(), new ReadRequest(3, 0, 1), 500);
        assertTrue(response.isSuccess());

        // Remove securityInfo
        helper.getSecurityStore().remove(helper.getCurrentEndpoint(), true);

        // check that next update will failed.
        helper.client.triggerRegistrationUpdate();
        helper.waitForUpdateFailureAtClientSide(500);

    }

    @Test
    public void dont_sent_request_if_identity_change()
            throws NonUniqueSecurityInfoException, InterruptedException, IOException {
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
        helper.waitForRegistrationAtServerSide(1);

        // Check client is well registered
        helper.assertClientRegisterered();

        // Ensure we can send a read request
        helper.server.send(helper.getCurrentRegistration(), new ReadRequest(3, 0, 1));

        // Add new credential to the server
        helper.getSecurityStore().add(SecurityInfo.newPreSharedKeyInfo(GOOD_ENDPOINT, "anotherPSK", GOOD_PSK_KEY));

        // Create new session with new credentials at client side.
        // Get connector
        DTLSConnector connector = (DTLSConnector) helper.getClientConnector(helper.getCurrentRegisteredServer());
        // Clear DTLS session to force new handshake
        connector.clearConnectionState();
        // Change PSK id
        helper.setNewPsk("anotherPSK", GOOD_PSK_KEY);
        // send and empty message to force a new handshake with new credentials
        SimpleMessageCallback callback = new SimpleMessageCallback();
        // create a ping message
        Request request = new Request(null, Type.CON);
        request.setToken(Token.EMPTY);
        request.setMID(0);
        byte[] ping = new UdpDataSerializer().getByteArray(request);
        // sent it
        URI destinationUri = helper.server.getEndpoint(Protocol.COAPS).getURI();
        connector.send(RawData.outbound(ping,
                new AddressEndpointContext(destinationUri.getHost(), destinationUri.getPort()), callback, false));
        // Wait until new handshake DTLS is done
        EndpointContext endpointContext = callback.getEndpointContext(1000);
        assertEquals(((PreSharedKeyIdentity) endpointContext.getPeerIdentity()).getIdentity(), "anotherPSK");

        // Try to send a read request this should failed with an SendFailedException.
        try {
            helper.server.send(helper.getCurrentRegistration(), new ReadRequest(3, 0, 1), 1000);
            fail("send must failed");
        } catch (SendFailedException e) {
            assertTrue(e.getCause() instanceof EndpointMismatchException,
                    "must be caused by an EndpointMismatchException");
        } finally {
            connector.stop();
            helper.client.destroy(false);
            helper.client = null;
        }
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
        helper.waitForRegistrationAtServerSide(1);

        // Check client is well registered
        helper.assertClientRegisterered();

        // Check for update
        helper.waitForUpdateAtClientSide(LIFETIME);
        helper.assertClientRegisterered();

        // Check de-registration
        helper.client.stop(true);
        helper.waitForDeregistrationAtServerSide(1);
        helper.assertClientNotRegisterered();

        // check new registration
        helper.client.start();
        helper.waitForRegistrationAtServerSide(1);
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
        helper.waitForRegistrationAtServerSide(1);
        Registration registration = helper.getCurrentRegistration();
        helper.assertClientRegisterered();

        // Check for update
        helper.waitForUpdateAtClientSide(LIFETIME);
        helper.assertClientRegisterered();

        // Check stop do not de-register
        helper.client.stop(false);
        helper.ensureNoDeregistration(1);
        helper.assertClientRegisterered();

        // Check new registration
        helper.client.start();
        helper.waitForRegistrationAtServerSide(1);
        helper.assertClientRegisterered();
        Registration newRegistration = helper.getCurrentRegistration();
        assertNotEquals(registration.getId(), newRegistration.getId());

    }

    @Test
    public void server_initiates_dtls_handshake() throws NonUniqueSecurityInfoException, InterruptedException {
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
        helper.waitForRegistrationAtServerSide(1);
        Registration registration = helper.getCurrentRegistration();
        helper.assertClientRegisterered();

        // Remove DTLS connection at server side.
        DTLSConnector dtlsServerConnector = helper.getServerDTLSConnector();
        dtlsServerConnector.clearConnectionState();

        // try to send request
        ReadResponse readResponse = helper.server.send(registration, new ReadRequest(3), 1000);
        assertTrue(readResponse.isSuccess());

        // ensure we have a new session for it
        DTLSSession session = dtlsServerConnector.getSessionByAddress(registration.getSocketAddress());
        assertNotNull(session);
    }

    @Test
    public void server_initiates_dtls_handshake_timeout() throws NonUniqueSecurityInfoException, InterruptedException {
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
        helper.waitForRegistrationAtServerSide(1);
        Registration registration = helper.getCurrentRegistration();
        helper.assertClientRegisterered();

        // Remove DTLS connection at server side.
        helper.getServerDTLSConnector().clearConnectionState();

        // stop client
        helper.client.stop(false);

        // try to send request synchronously
        ReadResponse readResponse = helper.server.send(registration, new ReadRequest(3), 1000);
        assertNull(readResponse);

        // try to send request asynchronously
        Callback<ReadResponse> callback = new Callback<>();
        helper.server.send(registration, new ReadRequest(3), 1000, callback, callback);
        callback.waitForResponse(1100);
        assertTrue(callback.getException() instanceof TimeoutException);
        assertEquals(TimeoutException.Type.DTLS_HANDSHAKE_TIMEOUT,
                ((TimeoutException) callback.getException()).getType());

    }

    @Test
    public void server_does_not_initiate_dtls_handshake_with_queue_mode()
            throws NonUniqueSecurityInfoException, InterruptedException {
        // Create PSK server & start it
        helper.createServer(); // default server support PSK
        helper.server.start();

        // Create PSK Client
        helper.createPSKClientUsingQueueMode();

        // Add client credentials to the server
        helper.getSecurityStore()
                .add(SecurityInfo.newPreSharedKeyInfo(helper.getCurrentEndpoint(), GOOD_PSK_ID, GOOD_PSK_KEY));

        // Check for registration
        helper.assertClientNotRegisterered();
        helper.client.start();
        helper.waitForRegistrationAtServerSide(1);
        Registration registration = helper.getCurrentRegistration();
        helper.assertClientRegisterered();

        // Remove DTLS connection at server side.
        helper.getServerDTLSConnector().clearConnectionState();

        // try to send request
        try {
            helper.server.send(registration, new ReadRequest(3), 1000);
            fail("Read request SHOULD have failed");
        } catch (UnconnectedPeerException e) {
            // expected result
            assertFalse(helper.server.getPresenceService().isClientAwake(registration), "client is still awake");
        }
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
        helper.ensureNoRegistration(1);
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
        helper.ensureNoRegistration(1);
    }

    @Test
    public void registered_device_with_psk_and_bad_endpoint_to_server_with_psk() throws NonUniqueSecurityInfoException {
        // Create PSK server & start it
        helper.createServer(); // default server support PSK
        helper.server.start();

        // Create PSK Client
        helper.createPSKClient();

        // Add client credentials for another endpoint to the server
        helper.getSecurityStore().add(SecurityInfo.newPreSharedKeyInfo(BAD_ENDPOINT, GOOD_PSK_ID, GOOD_PSK_KEY));

        // Check client can not register
        helper.assertClientNotRegisterered();
        helper.client.start();
        helper.ensureNoRegistration(1);
    }

    @Test
    public void registered_device_with_psk_identity_to_server_with_psk_then_remove_security_info()
            throws NonUniqueSecurityInfoException, InterruptedException {
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
        helper.waitForRegistrationAtClientSide(1);

        // Check client is well registered
        helper.assertClientRegisterered();

        // remove compromised credentials
        helper.getSecurityStore().remove(helper.getCurrentEndpoint(), true);

        // try to update
        helper.client.triggerRegistrationUpdate();
        helper.ensureNoUpdate(1);
    }

    @Test
    public void nonunique_psk_identity() throws NonUniqueSecurityInfoException {
        helper.createServer();
        helper.server.start();

        EditableSecurityStore ess = helper.getSecurityStore();

        ess.add(SecurityInfo.newPreSharedKeyInfo(GOOD_ENDPOINT, GOOD_PSK_ID, GOOD_PSK_KEY));
        try {
            ess.add(SecurityInfo.newPreSharedKeyInfo(BAD_ENDPOINT, GOOD_PSK_ID, GOOD_PSK_KEY));
            fail("Non-unique PSK identity should throw exception on add");
        } catch (NonUniqueSecurityInfoException e) {
        }
    }

    @Test
    public void change_psk_identity_cleanup() throws NonUniqueSecurityInfoException {
        helper.createServer();
        helper.server.start();

        EditableSecurityStore ess = helper.getSecurityStore();

        ess.add(SecurityInfo.newPreSharedKeyInfo(GOOD_ENDPOINT, BAD_PSK_ID, BAD_PSK_KEY));
        // Change PSK id for endpoint
        ess.add(SecurityInfo.newPreSharedKeyInfo(GOOD_ENDPOINT, GOOD_PSK_ID, GOOD_PSK_KEY));
        // Original/old PSK id should not be reserved any more
        try {
            ess.add(SecurityInfo.newPreSharedKeyInfo(BAD_ENDPOINT, BAD_PSK_ID, BAD_PSK_KEY));
        } catch (NonUniqueSecurityInfoException e) {
            fail("PSK identity change for existing endpoint should have cleaned up old PSK identity");
        }
    }

    @Test
    public void registered_device_with_rpk_to_server_with_rpk()
            throws NonUniqueSecurityInfoException, InterruptedException {
        helper.createServerWithRPK();
        helper.server.start();

        helper.createRPKClient();

        helper.getSecurityStore()
                .add(SecurityInfo.newRawPublicKeyInfo(helper.getCurrentEndpoint(), helper.clientPublicKey));

        helper.assertClientNotRegisterered();
        helper.client.start();
        helper.waitForRegistrationAtServerSide(1);

        assertNotNull(helper.getCurrentRegistration());

        // check we can send request to client.
        ReadResponse response = helper.server.send(helper.getCurrentRegistration(), new ReadRequest(3, 0, 1), 500);
        assertTrue(response.isSuccess());
    }

    @Test
    public void registered_device_with_bad_rpk_to_server_with_rpk_() throws NonUniqueSecurityInfoException {
        helper.createServerWithRPK();
        helper.server.start();

        helper.createRPKClient();

        // as it is complex to create a public key, I use the server one :p as bad client public key
        PublicKey bad_client_public_key = helper.getServerPublicKey();
        helper.getSecurityStore()
                .add(SecurityInfo.newRawPublicKeyInfo(helper.getCurrentEndpoint(), bad_client_public_key));

        helper.client.start();
        helper.ensureNoRegistration(1);
    }

    @Test
    public void registered_device_with_rpk_to_server_with_rpk_then_remove_security_info()
            throws NonUniqueSecurityInfoException {
        helper.createServerWithRPK();
        helper.server.start();

        helper.createRPKClient();

        helper.getSecurityStore()
                .add(SecurityInfo.newRawPublicKeyInfo(helper.getCurrentEndpoint(), helper.clientPublicKey));

        helper.assertClientNotRegisterered();
        helper.client.start();
        helper.waitForRegistrationAtClientSide(1);

        // Check client is well registered
        helper.assertClientRegisterered();

        // remove compromised credentials
        helper.getSecurityStore().remove(helper.getCurrentEndpoint(), true);

        // try to update
        helper.client.triggerRegistrationUpdate();
        helper.ensureNoUpdate(1);
    }

    @Test
    public void registered_device_with_rpk_and_bad_endpoint_to_server_with_rpk() throws NonUniqueSecurityInfoException {
        helper.createServerWithRPK();
        helper.server.start();

        helper.createRPKClient();

        helper.getSecurityStore().add(SecurityInfo.newRawPublicKeyInfo(BAD_ENDPOINT, helper.clientPublicKey));

        helper.client.start();
        helper.ensureNoRegistration(1);
    }

    @Test
    public void registered_device_with_x509cert_to_server_with_x509cert_then_remove_security_info()
            throws NonUniqueSecurityInfoException, CertificateEncodingException {
        helper.createServerWithX509Cert();
        helper.server.start();

        helper.setEndpointNameFromX509(helper.clientX509Cert);

        helper.createX509CertClient();

        helper.getSecurityStore().add(SecurityInfo.newX509CertInfo(helper.getCurrentEndpoint()));

        helper.assertClientNotRegisterered();
        helper.client.start();
        helper.waitForRegistrationAtClientSide(1);

        // Check client is well registered
        helper.assertClientRegisterered();

        // remove compromised credentials
        helper.getSecurityStore().remove(helper.getCurrentEndpoint(), true);

        // try to update
        helper.client.triggerRegistrationUpdate();
        helper.ensureNoUpdate(1);
    }

    @Test
    public void registered_device_with_x509cert_to_server_with_x509cert()
            throws NonUniqueSecurityInfoException, CertificateEncodingException, InterruptedException {
        helper.createServerWithX509Cert();
        helper.server.start();

        helper.setEndpointNameFromX509(helper.clientX509Cert);

        helper.createX509CertClient();

        helper.getSecurityStore().add(SecurityInfo.newX509CertInfo(helper.getCurrentEndpoint()));

        helper.assertClientNotRegisterered();
        helper.client.start();
        helper.waitForRegistrationAtServerSide(1);

        assertNotNull(helper.getCurrentRegistration());

        // check we can send request to client.
        ReadResponse response = helper.server.send(helper.getCurrentRegistration(), new ReadRequest(3, 0, 1), 500);
        assertTrue(response.isSuccess());
    }

    @Test
    public void registered_device_with_x509cert_to_server_with_self_signed_x509cert()
            throws NonUniqueSecurityInfoException, CertificateEncodingException, InterruptedException {
        helper.createServerWithX509Cert(helper.serverX509CertSelfSigned, helper.serverPrivateKeyFromCert, true);
        helper.server.start();

        helper.setEndpointNameFromX509(helper.clientX509Cert);

        helper.createX509CertClient(helper.clientX509Cert, helper.clientPrivateKeyFromCert,
                helper.serverX509CertSelfSigned);

        helper.getSecurityStore().add(SecurityInfo.newX509CertInfo(helper.getCurrentEndpoint()));

        helper.assertClientNotRegisterered();
        helper.client.start();
        helper.waitForRegistrationAtServerSide(1);

        assertNotNull(helper.getCurrentRegistration());

        // check we can send request to client.
        ReadResponse response = helper.server.send(helper.getCurrentRegistration(), new ReadRequest(3, 0, 1), 500);
        assertTrue(response.isSuccess());

    }

    @Test
    public void registered_device_with_x509cert_and_bad_endpoint_to_server_with_x509cert()
            throws NonUniqueSecurityInfoException, CertificateEncodingException {
        helper.createServerWithX509Cert();
        helper.server.start();

        helper.setEndpointNameFromX509(helper.clientX509Cert);

        helper.createX509CertClient();

        helper.getSecurityStore().add(SecurityInfo.newX509CertInfo(BAD_ENDPOINT));

        helper.assertClientNotRegisterered();
        helper.client.start();
        helper.ensureNoRegistration(1);
    }

    @Test
    public void registered_device_with_x509cert_and_bad_cn_certificate_to_server_with_x509cert()
            throws NonUniqueSecurityInfoException, CertificateEncodingException {
        helper.createServerWithX509Cert();
        helper.server.start();

        helper.setEndpointNameFromX509(helper.clientX509CertWithBadCN);

        helper.createX509CertClient(helper.clientX509CertWithBadCN);

        helper.getSecurityStore().add(SecurityInfo.newX509CertInfo(GOOD_ENDPOINT));

        helper.assertClientNotRegisterered();
        helper.client.start();
        helper.ensureNoRegistration(1);
    }

    @Test
    public void registered_device_with_x509cert_and_bad_private_key_to_server_with_x509cert()
            throws NonUniqueSecurityInfoException, CertificateEncodingException {
        helper.createServerWithX509Cert();
        helper.server.start();

        // we use the RPK private key as bad key, this key will not be compatible with the client certificate
        PrivateKey badPrivateKey = helper.clientPrivateKey;

        helper.setEndpointNameFromX509(helper.clientX509Cert);

        helper.createX509CertClient(helper.clientX509Cert, badPrivateKey);
        helper.getSecurityStore().add(SecurityInfo.newX509CertInfo(helper.getCurrentEndpoint()));

        helper.assertClientNotRegisterered();
        helper.client.start();
        helper.ensureNoRegistration(1);
    }

    @Test
    public void registered_device_with_untrusted_x509cert_to_server_with_x509cert()
            throws NonUniqueSecurityInfoException, CertificateEncodingException {
        // the server will not trust the client Certificate authority
        helper.createServerWithX509Cert();
        helper.server.start();

        helper.setEndpointNameFromX509(helper.clientX509CertNotTrusted);

        helper.createX509CertClient(helper.clientX509CertNotTrusted);

        helper.getSecurityStore().add(SecurityInfo.newX509CertInfo(helper.getCurrentEndpoint()));

        helper.assertClientNotRegisterered();
        helper.client.start();
        helper.ensureNoRegistration(1);
    }

    @Test
    public void registered_device_with_selfsigned_x509cert_to_server_with_x509cert()
            throws NonUniqueSecurityInfoException, CertificateEncodingException {
        // the server will not trust the client Certificate authority
        helper.createServerWithX509Cert();
        helper.server.start();

        helper.setEndpointNameFromX509(helper.clientX509CertSelfSigned);

        helper.createX509CertClient(helper.clientX509CertSelfSigned);

        helper.getSecurityStore().add(SecurityInfo.newX509CertInfo(helper.getCurrentEndpoint()));

        helper.assertClientNotRegisterered();
        helper.client.start();
        helper.ensureNoRegistration(1);
    }

    /* ---- CA_CONSTRAINT ---- */

    /**
     * <pre>
     * Test scenario:
     * - Certificate Usage = CA constraint
     * - Server Certificate = server certificate
     * - Server's TLS Server Certificate = intermediate signed certificate (with SAN DNS entry)
     * - Server accepts client
     * - Client Trust Store = root CA
     *
     * Expected outcome:
     * - Client denied the connection  (direct trust is not allowed with "CA constraint" usage)
     * </pre>
     */
    @Test
    public void registered_device_with_x509cert_to_server_with_x509cert_rootca_certificate_usage_ca()
            throws NonUniqueSecurityInfoException, CertificateEncodingException {
        helper.createServerWithX509Cert(helper.serverIntX509CertChain, helper.serverIntPrivateKeyFromCert,
                helper.trustedCertificates, true);

        helper.server.start();

        helper.setEndpointNameFromX509(helper.clientX509Cert);

        helper.createX509CertClient(new X509Certificate[] { helper.clientX509Cert }, helper.clientPrivateKeyFromCert,
                helper.clientTrustStore, helper.serverIntX509CertChain[0], CertificateUsage.CA_CONSTRAINT);

        helper.getSecurityStore().add(SecurityInfo.newX509CertInfo(helper.getCurrentEndpoint()));

        helper.assertClientNotRegisterered();
        helper.client.start();
        helper.ensureNoRegistration(1);
    }

    /**
     * <pre>
     * Test scenario:
     * - Certificate Usage = CA constraint
     * - Server Certificate = intermediate CA certificate
     * - Server's TLS Server Certificate = intermediate signed certificate (with SAN DNS entry)
     * - Server accepts client
     * - Client Trust Store = root CA
     *
     * Expected outcome:
     * - Client is able to connect (intermediate CA cert is part of the chain)
     * </pre>
     */
    @Test
    public void registered_device_with_x509cert_to_server_with_x509cert_rootca_certificate_usage_ca_intca_given()
            throws NonUniqueSecurityInfoException, CertificateEncodingException, InterruptedException {
        helper.createServerWithX509Cert(helper.serverIntX509CertChain, helper.serverIntPrivateKeyFromCert,
                helper.trustedCertificates, true);

        helper.server.start();

        helper.setEndpointNameFromX509(helper.clientX509Cert);

        helper.createX509CertClient(new X509Certificate[] { helper.clientX509Cert }, helper.clientPrivateKeyFromCert,
                helper.clientTrustStore, helper.serverIntX509CertChain[1], CertificateUsage.CA_CONSTRAINT);

        helper.getSecurityStore().add(SecurityInfo.newX509CertInfo(helper.getCurrentEndpoint()));

        helper.assertClientNotRegisterered();
        helper.client.start();
        helper.waitForRegistrationAtServerSide(1);

        assertNotNull(helper.getCurrentRegistration());

        // check we can send request to client.
        ReadResponse response = helper.server.send(helper.getCurrentRegistration(), new ReadRequest(3, 0, 1), 500);
        assertTrue(response.isSuccess());

    }

    /**
     * <pre>
     * Test scenario:
     * - Certificate Usage = CA constraint
     * - Server Certificate = intermediate CA certificate
     * - Server's TLS Server Certificate = intermediate signed certificate (with SAN DNS entry)
     * - Server accepts client
     * - Client Trust Store = does not contain server root CA.
     *
     * Expected outcome:
     * - Client is not able to connect as our CaConstraintCertificateVerifier does not support trust anchor mode.
     * </pre>
     */
    @Test
    public void registered_device_with_empty_truststore_with_x509cert_to_server_with_x509cert_rootca_certificate_usage_ca_intca_given()
            throws NonUniqueSecurityInfoException, CertificateEncodingException, InterruptedException {
        helper.createServerWithX509Cert(helper.serverIntX509CertChain, helper.serverIntPrivateKeyFromCert,
                helper.trustedCertificates, true);

        helper.server.start();

        helper.setEndpointNameFromX509(helper.clientX509Cert);

        // create a not empty trustore which does not contains any certificate of server certchain.
        List<Certificate> truststore = new ArrayList<>();
        truststore.add(helper.serverIntX509CertSelfSigned); // e.g. we use a selfsigned certificate not used in
                                                            // certchain of this test.

        helper.createX509CertClient(new X509Certificate[] { helper.clientX509Cert }, helper.clientPrivateKeyFromCert,
                truststore, helper.serverIntX509CertChain[1], CertificateUsage.CA_CONSTRAINT);

        helper.getSecurityStore().add(SecurityInfo.newX509CertInfo(helper.getCurrentEndpoint()));

        helper.assertClientNotRegisterered();
        helper.client.start();
        helper.ensureNoRegistration(1);

    }

    /**
     * <pre>
     * Test scenario:
     * - Certificate Usage = CA constraint
     * - Server Certificate = root CA certificate (not end-entity certificate)
     * - Server's TLS Server Certificate = intermediate signed certificate (with SAN DNS entry)
     * - Server accepts client
     * - Client Trust Store = root CA
     *
     * Expected outcome:
     * - Client is able to connect (root CA cert is part of the chain)
     * </pre>
     */
    @Test
    public void registered_device_with_x509cert_to_server_with_x509cert_rootca_certificate_ca_domain_root_ca_given()
            throws NonUniqueSecurityInfoException, CertificateEncodingException, InterruptedException {
        helper.createServerWithX509Cert(helper.serverIntX509CertChain, helper.serverIntPrivateKeyFromCert,
                helper.trustedCertificates, true);

        helper.server.start();

        helper.setEndpointNameFromX509(helper.clientX509Cert);

        helper.createX509CertClient(new X509Certificate[] { helper.clientX509Cert }, helper.clientPrivateKeyFromCert,
                helper.clientTrustStore, helper.rootCAX509Cert, CertificateUsage.CA_CONSTRAINT);

        helper.getSecurityStore().add(SecurityInfo.newX509CertInfo(helper.getCurrentEndpoint()));

        helper.assertClientNotRegisterered();
        helper.client.start();
        helper.waitForRegistrationAtServerSide(1);

        assertNotNull(helper.getCurrentRegistration());

        // check we can send request to client.
        ReadResponse response = helper.server.send(helper.getCurrentRegistration(), new ReadRequest(3, 0, 1), 500);
        assertTrue(response.isSuccess());
    }

    /**
     * <pre>
     * Test scenario:
     * - Certificate Usage = CA constraint
     * - Server Certificate = other end-entity certificate with same dns name signed by same root ca
     * - Server's TLS Server Certificate = intermediate signed certificate (with SAN DNS entry)
     * - Server accepts client
     * - Client Trust Store = root CA
     *
     * Expected outcome:
     * - Client denied the connection
     * </pre>
     */
    @Test
    public void registered_device_with_x509cert_to_server_with_x509cert_rootca_certificate_usage_ca_other_server_cert_given()
            throws NonUniqueSecurityInfoException, CertificateEncodingException {
        helper.createServerWithX509Cert(helper.serverIntX509CertChain, helper.serverIntPrivateKeyFromCert,
                helper.trustedCertificates, true);

        helper.server.start();

        helper.setEndpointNameFromX509(helper.clientX509Cert);

        helper.createX509CertClient(new X509Certificate[] { helper.clientX509Cert }, helper.clientPrivateKeyFromCert,
                helper.clientTrustStore, helper.serverX509Cert, CertificateUsage.CA_CONSTRAINT);

        helper.getSecurityStore().add(SecurityInfo.newX509CertInfo(helper.getCurrentEndpoint()));

        helper.assertClientNotRegisterered();
        helper.client.start();
        helper.ensureNoRegistration(1);
    }

    /**
     * <pre>
     * Test scenario:
     * - Certificate Usage = CA constraint
     * - Server Certificate = self signed certificate given
     * - Server's TLS Server Certificate = intermediate signed certificate (with SAN DNS entry)
     * - Server accepts client
     * - Client Trust Store = root CA
     *
     * Expected outcome:
     * - Client denied the connection
     * </pre>
     */
    @Test
    public void registered_device_with_x509cert_to_server_with_x509cert_rootca_certificate_usage_ca_selfsigned_server_cert_given()
            throws NonUniqueSecurityInfoException, CertificateEncodingException {
        helper.createServerWithX509Cert(helper.serverIntX509CertChain, helper.serverIntPrivateKeyFromCert,
                helper.trustedCertificates, true);

        helper.server.start();

        helper.setEndpointNameFromX509(helper.clientX509Cert);

        helper.createX509CertClient(new X509Certificate[] { helper.clientX509Cert }, helper.clientPrivateKeyFromCert,
                helper.clientTrustStore, helper.serverIntX509CertSelfSigned, CertificateUsage.CA_CONSTRAINT);

        helper.getSecurityStore().add(SecurityInfo.newX509CertInfo(helper.getCurrentEndpoint()));

        helper.assertClientNotRegisterered();
        helper.client.start();
        helper.ensureNoRegistration(1);
    }

    /**
     * <pre>
     * Test scenario:
     * - Certificate Usage = CA constraint
     * - Server Certificate = self signed certificate
     * - Server's TLS Server Certificate = self signed certificate (with SAN DNS entry)
     * - Server accepts client
     * - Client Trust Store = root CA
     *
     * Expected outcome:
     * - Client denied the connection  (direct trust is not allowed with "CA constraint" usage)
     * </pre>
     */
    @Test
    public void registered_device_with_x509cert_to_server_with_x509cert_selfsigned_certificate_usage_ca_selfsigned_server_cert_given()
            throws NonUniqueSecurityInfoException, CertificateEncodingException {
        helper.createServerWithX509Cert(new X509Certificate[] { helper.serverIntX509CertSelfSigned },
                helper.serverIntPrivateKeyFromCert, helper.trustedCertificates, true);

        helper.server.start();

        helper.setEndpointNameFromX509(helper.clientX509Cert);

        helper.createX509CertClient(new X509Certificate[] { helper.clientX509Cert }, helper.clientPrivateKeyFromCert,
                helper.clientTrustStore, helper.serverIntX509CertSelfSigned, CertificateUsage.CA_CONSTRAINT);

        helper.getSecurityStore().add(SecurityInfo.newX509CertInfo(helper.getCurrentEndpoint()));

        helper.assertClientNotRegisterered();
        helper.client.start();
        helper.ensureNoRegistration(1);
    }

    /**
     * <pre>
     * Test scenario:
     * - Certificate Usage = CA constraint
     * - Server Certificate = intermediate signed certificate/wo chain
     * - Server's TLS Server Certificate = intermediate signed certificate/wo chain (with SAN DNS entry)
     * - Server accepts client
     * - Client Trust Store = root CA
     *
     * Expected outcome:
     * - Client denied the connection  (direct trust is not allowed with "CA constraint" usage)
     * </pre>
     */
    @Test
    public void registered_device_with_x509cert_to_server_with_x509cert_server_certificate_usage_ca_server_cert_wo_chain_given()
            throws NonUniqueSecurityInfoException, CertificateEncodingException {
        helper.createServerWithX509Cert(new X509Certificate[] { helper.serverIntX509CertChain[0] },
                helper.serverIntPrivateKeyFromCert, helper.trustedCertificates, true);

        helper.server.start();

        helper.setEndpointNameFromX509(helper.clientX509Cert);

        helper.createX509CertClient(new X509Certificate[] { helper.clientX509Cert }, helper.clientPrivateKeyFromCert,
                helper.clientTrustStore, helper.serverIntX509CertChain[0], CertificateUsage.CA_CONSTRAINT);

        helper.getSecurityStore().add(SecurityInfo.newX509CertInfo(helper.getCurrentEndpoint()));

        helper.assertClientNotRegisterered();
        helper.client.start();
        helper.ensureNoRegistration(1);
    }

    /* ---- SERVICE_CERTIFICATE_CONSTRAINT ---- */

    /**
     * <pre>
     * Test scenario:
     * - Certificate Usage = service certificate constraint
     * - Server Certificate = server certificate
     * - Server's TLS Server Certificate = intermediate signed certificate (with SAN DNS entry)
     * - Server accepts client
     * - Client Trust Store = root CA
     *
     * Expected outcome:
     * - Client is able to connect
     * </pre>
     */
    @Test
    public void registered_device_with_x509cert_to_server_with_x509cert_rootca_certificate_usage_service()
            throws NonUniqueSecurityInfoException, CertificateEncodingException, InterruptedException {
        helper.createServerWithX509Cert(helper.serverIntX509CertChain, helper.serverIntPrivateKeyFromCert,
                helper.trustedCertificates, true);

        helper.server.start();

        helper.setEndpointNameFromX509(helper.clientX509Cert);

        helper.createX509CertClient(new X509Certificate[] { helper.clientX509Cert }, helper.clientPrivateKeyFromCert,
                helper.clientTrustStore, helper.serverIntX509CertChain[0],
                CertificateUsage.SERVICE_CERTIFICATE_CONSTRAINT);

        helper.getSecurityStore().add(SecurityInfo.newX509CertInfo(helper.getCurrentEndpoint()));

        helper.assertClientNotRegisterered();
        helper.client.start();
        helper.waitForRegistrationAtServerSide(1);

        assertNotNull(helper.getCurrentRegistration());

        // check we can send request to client.
        ReadResponse response = helper.server.send(helper.getCurrentRegistration(), new ReadRequest(3, 0, 1), 500);
        assertTrue(response.isSuccess());

    }

    /**
     * <pre>
     * Test scenario:
     * - Certificate Usage = service certificate constraint
     * - Server Certificate = root CA certificate (not end-entity certificate)
     * - Server's TLS Server Certificate = intermediate signed certificate (with SAN DNS entry)
     * - Server accepts client
     * - Client Trust Store = root CA
     *
     * Expected outcome:
     * - Client denied the connection
     * </pre>
     */
    @Test
    public void registered_device_with_x509cert_to_server_with_x509cert_rootca_certificate_service_domain_root_ca_given()
            throws NonUniqueSecurityInfoException, CertificateEncodingException {
        helper.createServerWithX509Cert(helper.serverIntX509CertChain, helper.serverIntPrivateKeyFromCert,
                helper.trustedCertificates, true);

        helper.server.start();

        helper.setEndpointNameFromX509(helper.clientX509Cert);

        helper.createX509CertClient(new X509Certificate[] { helper.clientX509Cert }, helper.clientPrivateKeyFromCert,
                helper.clientTrustStore, helper.rootCAX509Cert, CertificateUsage.SERVICE_CERTIFICATE_CONSTRAINT);

        helper.getSecurityStore().add(SecurityInfo.newX509CertInfo(helper.getCurrentEndpoint()));

        helper.assertClientNotRegisterered();
        helper.client.start();
        helper.ensureNoRegistration(1);
    }

    /**
     * <pre>
     * Test scenario:
     * - Certificate Usage = service certificate constraint
     * - Server Certificate = other end-entity certificate with same dns name signed by same root ca
     * - Server's TLS Server Certificate = intermediate signed certificate (with SAN DNS entry)
     * - Server accepts client
     * - Client Trust Store = root CA
     *
     * Expected outcome:
     * - Client denied the connection
     * </pre>
     */
    @Test
    public void registered_device_with_x509cert_to_server_with_x509cert_rootca_certificate_usage_service_other_server_cert_given()
            throws NonUniqueSecurityInfoException, CertificateEncodingException {
        helper.createServerWithX509Cert(helper.serverIntX509CertChain, helper.serverIntPrivateKeyFromCert,
                helper.trustedCertificates, true);

        helper.server.start();

        helper.setEndpointNameFromX509(helper.clientX509Cert);

        helper.createX509CertClient(new X509Certificate[] { helper.clientX509Cert }, helper.clientPrivateKeyFromCert,
                helper.clientTrustStore, helper.serverX509Cert, CertificateUsage.SERVICE_CERTIFICATE_CONSTRAINT);

        helper.getSecurityStore().add(SecurityInfo.newX509CertInfo(helper.getCurrentEndpoint()));

        helper.assertClientNotRegisterered();
        helper.client.start();
        helper.ensureNoRegistration(1);
    }

    /**
     * <pre>
     * Test scenario:
     * - Certificate Usage = service certificate constraint
     * - Server Certificate = self signed certificate given
     * - Server's TLS Server Certificate = intermediate signed certificate (with SAN DNS entry)
     * - Server accepts client
     * - Client Trust Store = root CA
     *
     * Expected outcome:
     * - Client denied the connection
     * </pre>
     */
    @Test
    public void registered_device_with_x509cert_to_server_with_x509cert_rootca_certificate_usage_service_selfsigned_server_cert_given()
            throws NonUniqueSecurityInfoException, CertificateEncodingException {
        helper.createServerWithX509Cert(helper.serverIntX509CertChain, helper.serverIntPrivateKeyFromCert,
                helper.trustedCertificates, true);

        helper.server.start();

        helper.setEndpointNameFromX509(helper.clientX509Cert);

        helper.createX509CertClient(new X509Certificate[] { helper.clientX509Cert }, helper.clientPrivateKeyFromCert,
                helper.clientTrustStore, helper.serverIntX509CertSelfSigned,
                CertificateUsage.SERVICE_CERTIFICATE_CONSTRAINT);

        helper.getSecurityStore().add(SecurityInfo.newX509CertInfo(helper.getCurrentEndpoint()));

        helper.assertClientNotRegisterered();
        helper.client.start();
        helper.ensureNoRegistration(1);
    }

    /**
     * <pre>
     * Test scenario:
     * - Certificate Usage = service certificate constraint
     * - Server Certificate = self signed certificate
     * - Server's TLS Server Certificate = self signed certificate (with SAN DNS entry)
     * - Server accepts client
     * - Client Trust Store = root CA
     *
     * Expected outcome:
     * - Client denied the connection (self-signed is not PKIX chainable)
     * </pre>
     */
    @Test
    public void registered_device_with_x509cert_to_server_with_x509cert_selfsigned_certificate_usage_service_selfsigned_server_cert_given()
            throws NonUniqueSecurityInfoException, CertificateEncodingException {
        helper.createServerWithX509Cert(new X509Certificate[] { helper.serverIntX509CertSelfSigned },
                helper.serverIntPrivateKeyFromCert, helper.trustedCertificates, true);

        helper.server.start();

        helper.setEndpointNameFromX509(helper.clientX509Cert);

        helper.createX509CertClient(new X509Certificate[] { helper.clientX509Cert }, helper.clientPrivateKeyFromCert,
                helper.clientTrustStore, helper.serverIntX509CertSelfSigned,
                CertificateUsage.SERVICE_CERTIFICATE_CONSTRAINT);

        helper.getSecurityStore().add(SecurityInfo.newX509CertInfo(helper.getCurrentEndpoint()));

        helper.assertClientNotRegisterered();
        helper.client.start();
        helper.ensureNoRegistration(1);
    }

    /**
     * <pre>
     * Test scenario:
     * - Certificate Usage = service certificate constraint
     * - Server Certificate = intermediate signed certificate/wo chain
     * - Server's TLS Server Certificate = intermediate signed certificate/wo chain (with SAN DNS entry)
     * - Server accepts client
     * - Client Trust Store = root CA
     *
     * Expected outcome:
     * - Client denied the connection (missing intermediate CA aka. "server chain configuration problem")
     * </pre>
     */
    @Test
    public void registered_device_with_x509cert_to_server_with_x509cert_server_certificate_usage_service_server_cert_wo_chain_given()
            throws NonUniqueSecurityInfoException, CertificateEncodingException {
        helper.createServerWithX509Cert(new X509Certificate[] { helper.serverIntX509CertChain[0] },
                helper.serverIntPrivateKeyFromCert, helper.trustedCertificates, true);

        helper.server.start();

        helper.setEndpointNameFromX509(helper.clientX509Cert);

        helper.createX509CertClient(new X509Certificate[] { helper.clientX509Cert }, helper.clientPrivateKeyFromCert,
                helper.clientTrustStore, helper.serverIntX509CertChain[0],
                CertificateUsage.SERVICE_CERTIFICATE_CONSTRAINT);

        helper.getSecurityStore().add(SecurityInfo.newX509CertInfo(helper.getCurrentEndpoint()));

        helper.assertClientNotRegisterered();
        helper.client.start();
        helper.ensureNoRegistration(1);
    }

    /* ---- TRUST_ANCHOR_ASSERTION ---- */

    /**
     * <pre>
     * Test scenario:
     * - Certificate Usage = trust anchor assertion
     * - Server Certificate = server certificate
     * - Server's TLS Server Certificate = intermediate signed certificate (with SAN DNS entry)
     * - Server accepts client
     * - Client Trust Store = root CA
     *
     * Expected outcome:
     * - Client denied the connection  (direct trust is not allowed with "trust constraint" usage)
     * </pre>
     */
    @Test
    public void registered_device_with_x509cert_to_server_with_x509cert_rootca_certificate_usage_taa()
            throws NonUniqueSecurityInfoException, CertificateEncodingException {
        helper.createServerWithX509Cert(helper.serverIntX509CertChain, helper.serverIntPrivateKeyFromCert,
                helper.trustedCertificates, true);

        helper.server.start();

        helper.setEndpointNameFromX509(helper.clientX509Cert);

        helper.createX509CertClient(new X509Certificate[] { helper.clientX509Cert }, helper.clientPrivateKeyFromCert,
                helper.clientTrustStore, helper.serverIntX509CertChain[0], CertificateUsage.TRUST_ANCHOR_ASSERTION);

        helper.getSecurityStore().add(SecurityInfo.newX509CertInfo(helper.getCurrentEndpoint()));

        helper.assertClientNotRegisterered();
        helper.client.start();
        helper.ensureNoRegistration(1);
    }

    /**
     * <pre>
     * Test scenario:
     * - Certificate Usage = trust anchor assertion
     * - Server Certificate = intermediate CA certificate
     * - Server's TLS Server Certificate = intermediate signed certificate (with SAN DNS entry)
     * - Server accepts client
     * - Client Trust Store = root CA
     *
     * Expected outcome:
     * - Client is able to connect (pkix path terminates in intermediate CA (TA), root CA is not available as client trust store not in use)
     * </pre>
     */
    @Test
    public void registered_device_with_x509cert_to_server_with_x509cert_rootca_certificate_usage_taa_intca_given()
            throws NonUniqueSecurityInfoException, CertificateEncodingException, InterruptedException {
        helper.createServerWithX509Cert(helper.serverIntX509CertChain, helper.serverIntPrivateKeyFromCert,
                helper.trustedCertificates, true);

        helper.server.start();

        helper.setEndpointNameFromX509(helper.clientX509Cert);

        helper.createX509CertClient(new X509Certificate[] { helper.clientX509Cert }, helper.clientPrivateKeyFromCert,
                helper.clientTrustStore, helper.serverIntX509CertChain[1], CertificateUsage.TRUST_ANCHOR_ASSERTION);

        helper.getSecurityStore().add(SecurityInfo.newX509CertInfo(helper.getCurrentEndpoint()));

        helper.assertClientNotRegisterered();
        helper.client.start();
        helper.waitForRegistrationAtServerSide(1);

        assertNotNull(helper.getCurrentRegistration());

        // check we can send request to client.
        ReadResponse response = helper.server.send(helper.getCurrentRegistration(), new ReadRequest(3, 0, 1), 500);
        assertTrue(response.isSuccess());
    }

    /**
     * <pre>
     * Test scenario:
     * - Certificate Usage = trust anchor assertion
     * - Server Certificate = root CA certificate
     * - Server's TLS Server Certificate = intermediate signed certificate (with SAN DNS entry)
     * - Server accepts client
     * - Client Trust Store = root CA
     *
     * Expected outcome:
     * - Client is able to connect (root CA cert is part of the chain)
     * </pre>
     */
    @Test
    public void registered_device_with_x509cert_to_server_with_x509cert_rootca_certificate_taa_domain_root_ca_given()
            throws NonUniqueSecurityInfoException, CertificateEncodingException, InterruptedException {
        helper.createServerWithX509Cert(helper.serverIntX509CertChain, helper.serverIntPrivateKeyFromCert,
                helper.trustedCertificates, true);

        helper.server.start();

        helper.setEndpointNameFromX509(helper.clientX509Cert);

        helper.createX509CertClient(new X509Certificate[] { helper.clientX509Cert }, helper.clientPrivateKeyFromCert,
                helper.clientTrustStore, helper.rootCAX509Cert, CertificateUsage.TRUST_ANCHOR_ASSERTION);

        helper.getSecurityStore().add(SecurityInfo.newX509CertInfo(helper.getCurrentEndpoint()));

        helper.assertClientNotRegisterered();
        helper.client.start();
        helper.waitForRegistrationAtServerSide(1);

        assertNotNull(helper.getCurrentRegistration());

        // check we can send request to client.
        ReadResponse response = helper.server.send(helper.getCurrentRegistration(), new ReadRequest(3, 0, 1), 500);
        assertTrue(response.isSuccess());
    }

    /**
     * <pre>
     * Test scenario:
     * - Certificate Usage = trust anchor assertion
     * - Server Certificate = other end-entity certificate with same dns name signed by same root ca
     * - Server's TLS Server Certificate = intermediate signed certificate (with SAN DNS entry)
     * - Server accepts client
     * - Client Trust Store = root CA
     *
     * Expected outcome:
     * - Client denied the connection
     * </pre>
     */
    @Test
    public void registered_device_with_x509cert_to_server_with_x509cert_rootca_certificate_usage_taa_other_server_cert_given()
            throws NonUniqueSecurityInfoException, CertificateEncodingException {
        helper.createServerWithX509Cert(helper.serverIntX509CertChain, helper.serverIntPrivateKeyFromCert,
                helper.trustedCertificates, true);

        helper.server.start();

        helper.setEndpointNameFromX509(helper.clientX509Cert);

        helper.createX509CertClient(new X509Certificate[] { helper.clientX509Cert }, helper.clientPrivateKeyFromCert,
                helper.clientTrustStore, helper.serverX509Cert, CertificateUsage.TRUST_ANCHOR_ASSERTION);

        helper.getSecurityStore().add(SecurityInfo.newX509CertInfo(helper.getCurrentEndpoint()));

        helper.assertClientNotRegisterered();
        helper.client.start();
        helper.ensureNoRegistration(1);
    }

    /**
     * <pre>
     * Test scenario:
     * - Certificate Usage = trust anchor assertion
     * - Server Certificate = self signed certificate given
     * - Server's TLS Server Certificate = intermediate signed certificate (with SAN DNS entry)
     * - Server accepts client
     * - Client Trust Store = root CA
     *
     * Expected outcome:
     * - Client denied the connection
     * </pre>
     */
    @Test
    public void registered_device_with_x509cert_to_server_with_x509cert_rootca_certificate_usage_taa_selfsigned_server_cert_given()
            throws NonUniqueSecurityInfoException, CertificateEncodingException {
        helper.createServerWithX509Cert(helper.serverIntX509CertChain, helper.serverIntPrivateKeyFromCert,
                helper.trustedCertificates, true);

        helper.server.start();

        helper.setEndpointNameFromX509(helper.clientX509Cert);

        helper.createX509CertClient(new X509Certificate[] { helper.clientX509Cert }, helper.clientPrivateKeyFromCert,
                helper.clientTrustStore, helper.serverIntX509CertSelfSigned, CertificateUsage.TRUST_ANCHOR_ASSERTION);

        helper.getSecurityStore().add(SecurityInfo.newX509CertInfo(helper.getCurrentEndpoint()));

        helper.assertClientNotRegisterered();
        helper.client.start();
        helper.ensureNoRegistration(1);
    }

    /**
     * <pre>
     * Test scenario:
     * - Certificate Usage = trust anchor assertion
     * - Server Certificate = self signed certificate
     * - Server's TLS Server Certificate = self signed certificate (with SAN DNS entry)
     * - Server accepts client
     * - Client Trust Store = root CA
     *
     * Expected outcome:
     * - Client denied the connection  (direct trust is not allowed with "trust anchor" usage)
     * </pre>
     */
    @Test
    public void registered_device_with_x509cert_to_server_with_x509cert_selfsigned_certificate_usage_taa_selfsigned_server_cert_given()
            throws NonUniqueSecurityInfoException, CertificateEncodingException {
        helper.createServerWithX509Cert(new X509Certificate[] { helper.serverIntX509CertSelfSigned },
                helper.serverIntPrivateKeyFromCert, helper.trustedCertificates, true);

        helper.server.start();

        helper.setEndpointNameFromX509(helper.clientX509Cert);

        helper.createX509CertClient(new X509Certificate[] { helper.clientX509Cert }, helper.clientPrivateKeyFromCert,
                helper.clientTrustStore, helper.serverIntX509CertSelfSigned, CertificateUsage.TRUST_ANCHOR_ASSERTION);

        helper.getSecurityStore().add(SecurityInfo.newX509CertInfo(helper.getCurrentEndpoint()));

        helper.assertClientNotRegisterered();
        helper.client.start();
        helper.ensureNoRegistration(1);
    }

    /**
     * <pre>
     * Test scenario:
     * - Certificate Usage = trust anchor assertion
     * - Server Certificate = intermediate signed certificate/wo chain
     * - Server's TLS Server Certificate = intermediate signed certificate/wo chain (with SAN DNS entry)
     * - Server accepts client
     * - Client Trust Store = root CA
     *
     * Expected outcome:
     * - Client denied the connection  (direct trust is not allowed with "trust anchor" usage)
     * </pre>
     */
    @Test
    public void registered_device_with_x509cert_to_server_with_x509cert_server_certificate_usage_taa_server_cert_wo_chain_given()
            throws NonUniqueSecurityInfoException, CertificateEncodingException {
        helper.createServerWithX509Cert(new X509Certificate[] { helper.serverIntX509CertChain[0] },
                helper.serverIntPrivateKeyFromCert, helper.trustedCertificates, true);

        helper.server.start();

        helper.setEndpointNameFromX509(helper.clientX509Cert);

        helper.createX509CertClient(new X509Certificate[] { helper.clientX509Cert }, helper.clientPrivateKeyFromCert,
                helper.clientTrustStore, helper.serverIntX509CertChain[0], CertificateUsage.TRUST_ANCHOR_ASSERTION);

        helper.getSecurityStore().add(SecurityInfo.newX509CertInfo(helper.getCurrentEndpoint()));

        helper.assertClientNotRegisterered();
        helper.client.start();
        helper.ensureNoRegistration(1);
    }

    /* ---- DOMAIN_ISSUER_CERTIFICATE ---- */

    /**
     * <pre>
     * Test scenario:
     * - Certificate Usage = domain issuer certificate
     * - Server Certificate = server certificate
     * - Server's TLS Server Certificate = intermediate signed certificate (with SAN DNS entry)
     * - Server accepts client
     * - Client Trust Store = root CA
     *
     * Expected outcome:
     * - Client is able to connect
     * </pre>
     */
    @Test
    public void registered_device_with_x509cert_to_server_with_x509cert_rootca_certificate_usage_domain()
            throws NonUniqueSecurityInfoException, CertificateEncodingException, InterruptedException {
        helper.createServerWithX509Cert(helper.serverIntX509CertChain, helper.serverIntPrivateKeyFromCert,
                helper.trustedCertificates, true);

        helper.server.start();

        helper.setEndpointNameFromX509(helper.clientX509Cert);

        helper.createX509CertClient(new X509Certificate[] { helper.clientX509Cert }, helper.clientPrivateKeyFromCert,
                helper.clientTrustStore, helper.serverIntX509CertChain[0], CertificateUsage.DOMAIN_ISSUER_CERTIFICATE);

        helper.getSecurityStore().add(SecurityInfo.newX509CertInfo(helper.getCurrentEndpoint()));

        helper.assertClientNotRegisterered();
        helper.client.start();
        helper.waitForRegistrationAtServerSide(1);

        assertNotNull(helper.getCurrentRegistration());

        // check we can send request to client.
        ReadResponse response = helper.server.send(helper.getCurrentRegistration(), new ReadRequest(3, 0, 1), 500);
        assertTrue(response.isSuccess());
    }

    /**
     * <pre>
     * Test scenario:
     * - Certificate Usage = domain issuer certificate
     * - Server Certificate = root CA certificate (not end-entity certificate)
     * - Server's TLS Server Certificate = intermediate signed certificate (with SAN DNS entry)
     * - Server accepts client
     * - Client Trust Store = root CA
     *
     * Expected outcome:
     * - Client denied the connection (no end-entity certificate given)
     * </pre>
     */
    @Test
    public void registered_device_with_x509cert_to_server_with_x509cert_rootca_certificate_usage_domain_root_ca_given()
            throws NonUniqueSecurityInfoException, CertificateEncodingException {
        helper.createServerWithX509Cert(helper.serverIntX509CertChain, helper.serverIntPrivateKeyFromCert,
                helper.trustedCertificates, true);

        helper.server.start();

        helper.setEndpointNameFromX509(helper.clientX509Cert);

        helper.createX509CertClient(new X509Certificate[] { helper.clientX509Cert }, helper.clientPrivateKeyFromCert,
                helper.clientTrustStore, helper.rootCAX509Cert, CertificateUsage.DOMAIN_ISSUER_CERTIFICATE);

        helper.getSecurityStore().add(SecurityInfo.newX509CertInfo(helper.getCurrentEndpoint()));

        helper.assertClientNotRegisterered();
        helper.client.start();
        helper.ensureNoRegistration(1);
    }

    /**
     * <pre>
     * Test scenario:
     * - Certificate Usage = domain issuer certificate
     * - Server Certificate = other end-entity certificate with same dns name signed by same root ca
     * - Server's TLS Server Certificate = intermediate signed certificate (with SAN DNS entry)
     * - Server accepts client
     * - Client Trust Store = root CA
     *
     * Expected outcome:
     * - Client denied the connection (different server cert given even thou hostname matches)
     * </pre>
     */
    @Test
    public void registered_device_with_x509cert_to_server_with_x509cert_rootca_certificate_usage_domain_other_server_cert_given()
            throws NonUniqueSecurityInfoException, CertificateEncodingException {
        helper.createServerWithX509Cert(helper.serverIntX509CertChain, helper.serverIntPrivateKeyFromCert,
                helper.trustedCertificates, true);

        helper.server.start();

        helper.setEndpointNameFromX509(helper.clientX509Cert);

        helper.createX509CertClient(new X509Certificate[] { helper.clientX509Cert }, helper.clientPrivateKeyFromCert,
                helper.clientTrustStore, helper.serverX509Cert, CertificateUsage.DOMAIN_ISSUER_CERTIFICATE);

        helper.getSecurityStore().add(SecurityInfo.newX509CertInfo(helper.getCurrentEndpoint()));

        helper.assertClientNotRegisterered();
        helper.client.start();
        helper.ensureNoRegistration(1);
    }

    /**
     * <pre>
     * Test scenario:
     * - Certificate Usage = domain issuer certificate
     * - Server Certificate = self signed certificate given
     * - Server's TLS Server Certificate = intermediate signed certificate (with SAN DNS entry)
     * - Server accepts client
     * - Client Trust Store = root CA
     *
     * Expected outcome:
     * - Client denied the connection (different certificate self-signed vs. signed -- even thou the public key is same)
     * </pre>
     */
    @Test
    public void registered_device_with_x509cert_to_server_with_x509cert_rootca_certificate_usage_domain_selfsigned_server_cert_given()
            throws NonUniqueSecurityInfoException, CertificateEncodingException {
        helper.createServerWithX509Cert(helper.serverIntX509CertChain, helper.serverIntPrivateKeyFromCert,
                helper.trustedCertificates, true);

        helper.server.start();

        helper.setEndpointNameFromX509(helper.clientX509Cert);

        helper.createX509CertClient(new X509Certificate[] { helper.clientX509Cert }, helper.clientPrivateKeyFromCert,
                helper.clientTrustStore, helper.serverIntX509CertSelfSigned,
                CertificateUsage.DOMAIN_ISSUER_CERTIFICATE);

        helper.getSecurityStore().add(SecurityInfo.newX509CertInfo(helper.getCurrentEndpoint()));

        helper.assertClientNotRegisterered();
        helper.client.start();
        helper.ensureNoRegistration(1);
    }

    /**
     * <pre>
     * Test scenario:
     * - Certificate Usage = domain issuer certificate
     * - Server Certificate = self signed certificate
     * - Server's TLS Server Certificate = self signed certificate (with SAN DNS entry)
     * - Server accepts client
     * - Client Trust Store = root CA
     *
     * Expected outcome:
     * - Client is able to connect
     * </pre>
     */
    @Test
    public void registered_device_with_x509cert_to_server_with_x509cert_selfsigned_certificate_usage_domain_selfsigned_server_cert_given()
            throws NonUniqueSecurityInfoException, CertificateEncodingException, InterruptedException {
        helper.createServerWithX509Cert(new X509Certificate[] { helper.serverIntX509CertSelfSigned },
                helper.serverIntPrivateKeyFromCert, helper.trustedCertificates, true);

        helper.server.start();

        helper.setEndpointNameFromX509(helper.clientX509Cert);

        helper.createX509CertClient(new X509Certificate[] { helper.clientX509Cert }, helper.clientPrivateKeyFromCert,
                helper.clientTrustStore, helper.serverIntX509CertSelfSigned,
                CertificateUsage.DOMAIN_ISSUER_CERTIFICATE);

        helper.getSecurityStore().add(SecurityInfo.newX509CertInfo(helper.getCurrentEndpoint()));

        helper.assertClientNotRegisterered();
        helper.client.start();
        helper.waitForRegistrationAtServerSide(1);

        assertNotNull(helper.getCurrentRegistration());

        // check we can send request to client.
        ReadResponse response = helper.server.send(helper.getCurrentRegistration(), new ReadRequest(3, 0, 1), 500);
        assertTrue(response.isSuccess());

    }

    /**
     * <pre>
     * Test scenario:
     * - Certificate Usage = service certificate constraint
     * - Server Certificate = intermediate signed certificate/wo chain
     * - Server's TLS Server Certificate = intermediate signed certificate/wo chain (with SAN DNS entry)
     * - Server accepts client
     * - Client Trust Store = root CA
     *
     * Expected outcome:
     * - Client is able to connect
     * </pre>
     */
    @Test
    public void registered_device_with_x509cert_to_server_with_x509cert_server_certificate_usage_domain_server_cert_wo_chain_given()
            throws NonUniqueSecurityInfoException, CertificateEncodingException, InterruptedException {
        helper.createServerWithX509Cert(new X509Certificate[] { helper.serverIntX509CertChain[0] },
                helper.serverIntPrivateKeyFromCert, helper.trustedCertificates, true);

        helper.server.start();

        helper.setEndpointNameFromX509(helper.clientX509Cert);

        helper.createX509CertClient(new X509Certificate[] { helper.clientX509Cert }, helper.clientPrivateKeyFromCert,
                helper.clientTrustStore, helper.serverIntX509CertChain[0], CertificateUsage.DOMAIN_ISSUER_CERTIFICATE);

        helper.getSecurityStore().add(SecurityInfo.newX509CertInfo(helper.getCurrentEndpoint()));

        helper.assertClientNotRegisterered();
        helper.client.start();
        helper.waitForRegistrationAtServerSide(1);

        assertNotNull(helper.getCurrentRegistration());

        // check we can send request to client.
        ReadResponse response = helper.server.send(helper.getCurrentRegistration(), new ReadRequest(3, 0, 1), 500);
        assertTrue(response.isSuccess());
    }

    /* ---- */

    @Test
    public void registered_device_with_x509cert_to_server_with_rpk()
            throws NonUniqueSecurityInfoException, CertificateEncodingException {
        helper.createServerWithRPK();
        helper.server.start();

        helper.setEndpointNameFromX509(helper.clientX509Cert);

        helper.createX509CertClient(helper.clientX509Cert);

        helper.getSecurityStore().add(
                SecurityInfo.newRawPublicKeyInfo(helper.getCurrentEndpoint(), helper.clientX509Cert.getPublicKey()));

        helper.assertClientNotRegisterered();
        helper.client.start();
        helper.ensureNoRegistration(1);
    }

    @Test
    public void registered_device_with_rpk_to_server_with_x509cert()
            throws NonUniqueSecurityInfoException, InterruptedException {
        helper.createServerWithX509Cert();
        helper.server.start();

        boolean useServerCertifcatePublicKey = true;
        helper.createRPKClient(useServerCertifcatePublicKey);

        helper.getSecurityStore()
                .add(SecurityInfo.newRawPublicKeyInfo(helper.getCurrentEndpoint(), helper.clientPublicKey));

        helper.client.start();
        helper.waitForRegistrationAtServerSide(1);

        assertNotNull(helper.getCurrentRegistration());

        // check we can send request to client.
        ReadResponse response = helper.server.send(helper.getCurrentRegistration(), new ReadRequest(3, 0, 1), 500);
        assertTrue(response.isSuccess());

    }
}
