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

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.leshan.core.CertificateUsage.CA_CONSTRAINT;
import static org.eclipse.leshan.core.CertificateUsage.DOMAIN_ISSUER_CERTIFICATE;
import static org.eclipse.leshan.core.CertificateUsage.SERVICE_CERTIFICATE_CONSTRAINT;
import static org.eclipse.leshan.core.CertificateUsage.TRUST_ANCHOR_ASSERTION;
import static org.eclipse.leshan.integration.tests.util.Credentials.BAD_ENDPOINT;
import static org.eclipse.leshan.integration.tests.util.Credentials.BAD_PSK_ID;
import static org.eclipse.leshan.integration.tests.util.Credentials.BAD_PSK_KEY;
import static org.eclipse.leshan.integration.tests.util.Credentials.GOOD_PSK_ID;
import static org.eclipse.leshan.integration.tests.util.Credentials.GOOD_PSK_KEY;
import static org.eclipse.leshan.integration.tests.util.Credentials.clientPrivateKey;
import static org.eclipse.leshan.integration.tests.util.Credentials.clientPrivateKeyFromCert;
import static org.eclipse.leshan.integration.tests.util.Credentials.clientPublicKey;
import static org.eclipse.leshan.integration.tests.util.Credentials.clientTrustStore;
import static org.eclipse.leshan.integration.tests.util.Credentials.clientX509Cert;
import static org.eclipse.leshan.integration.tests.util.Credentials.clientX509CertNotTrusted;
import static org.eclipse.leshan.integration.tests.util.Credentials.clientX509CertWithBadCN;
import static org.eclipse.leshan.integration.tests.util.Credentials.rootCAX509Cert;
import static org.eclipse.leshan.integration.tests.util.Credentials.serverIntPrivateKeyFromCert;
import static org.eclipse.leshan.integration.tests.util.Credentials.serverIntX509CertChain;
import static org.eclipse.leshan.integration.tests.util.Credentials.serverIntX509CertSelfSigned;
import static org.eclipse.leshan.integration.tests.util.Credentials.serverPrivateKey;
import static org.eclipse.leshan.integration.tests.util.Credentials.serverPrivateKeyFromCert;
import static org.eclipse.leshan.integration.tests.util.Credentials.serverPublicKey;
import static org.eclipse.leshan.integration.tests.util.Credentials.serverX509Cert;
import static org.eclipse.leshan.integration.tests.util.Credentials.serverX509CertSelfSigned;
import static org.eclipse.leshan.integration.tests.util.Credentials.trustedCertificates;
import static org.eclipse.leshan.integration.tests.util.LeshanTestClientBuilder.givenClientUsing;
import static org.eclipse.leshan.integration.tests.util.assertion.Assertions.assertArg;
import static org.eclipse.leshan.integration.tests.util.assertion.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import org.eclipse.leshan.core.endpoint.Protocol;
import org.eclipse.leshan.core.request.ReadRequest;
import org.eclipse.leshan.core.request.exception.TimeoutException;
import org.eclipse.leshan.core.request.exception.TimeoutException.Type;
import org.eclipse.leshan.core.request.exception.UnconnectedPeerException;
import org.eclipse.leshan.core.response.ErrorCallback;
import org.eclipse.leshan.core.response.ReadResponse;
import org.eclipse.leshan.core.response.ResponseCallback;
import org.eclipse.leshan.integration.tests.util.Credentials;
import org.eclipse.leshan.integration.tests.util.LeshanTestClient;
import org.eclipse.leshan.integration.tests.util.LeshanTestClientBuilder;
import org.eclipse.leshan.integration.tests.util.LeshanTestServer;
import org.eclipse.leshan.integration.tests.util.LeshanTestServerBuilder;
import org.eclipse.leshan.integration.tests.util.junit5.extensions.BeforeEachParameterizedResolver;
import org.eclipse.leshan.server.registration.Registration;
import org.eclipse.leshan.server.security.InMemorySecurityStore;
import org.eclipse.leshan.server.security.NonUniqueSecurityInfoException;
import org.eclipse.leshan.server.security.SecurityInfo;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

@ExtendWith(BeforeEachParameterizedResolver.class)
public class SecurityTest {

    private static final long SHORT_LIFETIME = 2; // seconds

    /*---------------------------------/
     *  Parameterized Tests
     * -------------------------------*/
    @ParameterizedTest(name = "{0} - Client using {1} - Server using {2}")
    @MethodSource("transports")
    @Retention(RetentionPolicy.RUNTIME)
    private @interface TestAllTransportLayer {
    }

    static Stream<org.junit.jupiter.params.provider.Arguments> transports() {
        return Stream.of(//
                // ProtocolUsed - Client Endpoint Provider - Server Endpoint Provider
                arguments(Protocol.COAPS, "Californium", "Californium"));
    }

    /*---------------------------------/
     *  Set-up and Tear-down Tests
     * -------------------------------*/
    LeshanTestServerBuilder givenServer;
    LeshanTestServer server;
    LeshanTestClientBuilder givenClient;
    LeshanTestClient client;

    @BeforeEach
    public void start(Protocol givenProtocol, String givenClientEndpointProvider, String givenServerEndpointProvider) {
        givenServer = givenServerUsing(givenProtocol).with(givenServerEndpointProvider);
        givenClient = givenClientUsing(givenProtocol).with(givenClientEndpointProvider);
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

    /*---------------------------------/
     *  Tests
     * -------------------------------*/
    @TestAllTransportLayer
    public void registered_device_with_psk_to_server_with_psk(Protocol givenProtocol,
            String givenClientEndpointProvider, String givenServerEndpointProvider)
            throws NonUniqueSecurityInfoException, InterruptedException {
        // Create PSK server & start it
        server = givenServer.build(); // default server support PSK
        server.start();

        // Create PSK Client
        client = givenClient.connectingTo(server).usingPsk(GOOD_PSK_ID, GOOD_PSK_KEY).build();

        // Add client credentials to the server
        server.getSecurityStore()
                .add(SecurityInfo.newPreSharedKeyInfo(client.getEndpointName(), GOOD_PSK_ID, GOOD_PSK_KEY));

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

    @TestAllTransportLayer
    public void register_update_deregister_reregister_device_with_psk_to_server_with_psk(Protocol givenProtocol,
            String givenClientEndpointProvider, String givenServerEndpointProvider)
            throws NonUniqueSecurityInfoException {
        // Create PSK server & start it
        server = givenServer.build(); // default server support PSK
        server.start();

        // Create PSK Client
        client = givenClient.connectingTo(server) //
                .usingPsk(GOOD_PSK_ID, GOOD_PSK_KEY) //
                .usingLifeTimeOf(SHORT_LIFETIME, TimeUnit.SECONDS).build();

        // Add client credentials to the server
        server.getSecurityStore()
                .add(SecurityInfo.newPreSharedKeyInfo(client.getEndpointName(), GOOD_PSK_ID, GOOD_PSK_KEY));

        // Check client is not registered
        assertThat(client).isNotRegisteredAt(server);

        // Start it.
        client.start();

        // Check for register
        server.waitForNewRegistrationOf(client);
        client.waitForRegistrationTo(server);
        assertThat(client).isRegisteredAt(server);
        Registration registration = server.getRegistrationFor(client);

        // Check for update
        client.waitForUpdateTo(server, SHORT_LIFETIME, TimeUnit.SECONDS);
        server.waitForUpdateOf(registration);
        assertThat(client).isRegisteredAt(server);

        // Check de-registration
        client.stop(true);
        server.waitForDeregistrationOf(registration);
        client.waitForDeregistrationTo(server);
        assertThat(client).isNotRegisteredAt(server);

        // check new registration
        client.start();
        server.waitForNewRegistrationOf(client);
        client.waitForRegistrationTo(server);
        assertThat(client).isRegisteredAt(server);
    }

    @TestAllTransportLayer
    public void register_update_reregister_device_with_psk_to_server_with_psk(Protocol givenProtocol,
            String givenClientEndpointProvider, String givenServerEndpointProvider)
            throws NonUniqueSecurityInfoException {
        // Create PSK server & start it
        server = givenServer.build(); // default server support PSK
        server.start();

        // Create PSK Client
        client = givenClient.connectingTo(server) //
                .usingPsk(GOOD_PSK_ID, GOOD_PSK_KEY) //
                .usingLifeTimeOf(SHORT_LIFETIME, TimeUnit.SECONDS).build();

        // Add client credentials to the server
        server.getSecurityStore()
                .add(SecurityInfo.newPreSharedKeyInfo(client.getEndpointName(), GOOD_PSK_ID, GOOD_PSK_KEY));

        // Check client is not registered
        assertThat(client).isNotRegisteredAt(server);

        // Start it.
        client.start();

        // Check for register
        server.waitForNewRegistrationOf(client);
        client.waitForRegistrationTo(server);
        assertThat(client).isRegisteredAt(server);
        Registration registration = server.getRegistrationFor(client);

        // Check for update
        client.waitForUpdateTo(server, SHORT_LIFETIME, TimeUnit.SECONDS);
        server.waitForUpdateOf(registration);
        assertThat(client).isRegisteredAt(server);

        // Check de-registration
        client.stop(false);
        assertThat(client).after(500, TimeUnit.MILLISECONDS).isRegisteredAt(server);

        // check new registration
        client.start();
        server.waitForReRegistrationOf(registration);
        client.waitForRegistrationTo(server);
        assertThat(client).isRegisteredAt(server);
    }

    @TestAllTransportLayer
    public void server_initiates_dtls_handshake(Protocol givenProtocol, String givenClientEndpointProvider,
            String givenServerEndpointProvider) throws NonUniqueSecurityInfoException, InterruptedException {
        // Create PSK server & start it
        server = givenServer.build(); // default server support PSK
        server.start();

        // Create PSK Client
        client = givenClient.connectingTo(server).usingPsk(GOOD_PSK_ID, GOOD_PSK_KEY).build();

        // Add client credentials to the server
        server.getSecurityStore()
                .add(SecurityInfo.newPreSharedKeyInfo(client.getEndpointName(), GOOD_PSK_ID, GOOD_PSK_KEY));

        // Check client is not registered
        assertThat(client).isNotRegisteredAt(server);

        // Start it.
        client.start();

        // Check for register
        server.waitForNewRegistrationOf(client);
        client.waitForRegistrationTo(server);
        assertThat(client).isRegisteredAt(server);
        Registration registration = server.getRegistrationFor(client);

        // Remove Client Security info
        server.clearSecurityContextFor(givenProtocol);

        // try to send request
        ReadResponse readResponse = server.send(registration, new ReadRequest(3), 1000);
        assertThat(readResponse.isSuccess()).isTrue();
    }

    @TestAllTransportLayer
    public void server_initiates_dtls_handshake_timeout(Protocol givenProtocol, String givenClientEndpointProvider,
            String givenServerEndpointProvider) throws NonUniqueSecurityInfoException, InterruptedException {
        // Create PSK server & start it
        server = givenServer.build(); // default server support PSK
        server.start();

        // Create PSK Client
        client = givenClient.connectingTo(server).usingPsk(GOOD_PSK_ID, GOOD_PSK_KEY).build();

        // Add client credentials to the server
        server.getSecurityStore()
                .add(SecurityInfo.newPreSharedKeyInfo(client.getEndpointName(), GOOD_PSK_ID, GOOD_PSK_KEY));

        // Check client is not registered
        assertThat(client).isNotRegisteredAt(server);

        // Start it.
        client.start();

        // Check for register
        server.waitForNewRegistrationOf(client);
        client.waitForRegistrationTo(server);
        assertThat(client).isRegisteredAt(server);
        Registration registration = server.getRegistrationFor(client);

        // Remove Client Security info
        server.clearSecurityContextFor(givenProtocol);

        // stop client
        client.stop(false);

        // try to send request asynchronously, it should fail with security layer timeout
        @SuppressWarnings("unchecked")
        ResponseCallback<ReadResponse> responseCallback = mock(ResponseCallback.class);
        ErrorCallback errorCallback = mock(ErrorCallback.class);
        server.send(registration, new ReadRequest(3), 1000, responseCallback, errorCallback);

        verify(errorCallback, timeout(1100).times(1)) //
                .onError(assertArg(e -> {
                    assertThat(e).isInstanceOfSatisfying(TimeoutException.class, timeout -> {
                        assertThat(timeout.getType()).isEqualTo(Type.DTLS_HANDSHAKE_TIMEOUT);
                    });
                }));
        verify(responseCallback, never()).onResponse(any());
    }

    @TestAllTransportLayer
    public void server_does_not_initiate_dtls_handshake_with_queue_mode(Protocol givenProtocol,
            String givenClientEndpointProvider, String givenServerEndpointProvider)
            throws NonUniqueSecurityInfoException, InterruptedException {

        // Create PSK server & start it
        server = givenServer.build(); // default server support PSK
        server.start();

        // Create PSK Client
        client = givenClient.connectingTo(server) //
                .usingPsk(GOOD_PSK_ID, GOOD_PSK_KEY) //
                .usingQueueMode() //
                .build();

        // Add client credentials to the server
        server.getSecurityStore()
                .add(SecurityInfo.newPreSharedKeyInfo(client.getEndpointName(), GOOD_PSK_ID, GOOD_PSK_KEY));

        // Check client is not registered
        assertThat(client).isNotRegisteredAt(server);

        // Start it.
        client.start();

        // Check for register
        server.waitForNewRegistrationOf(client);
        client.waitForRegistrationTo(server);
        assertThat(client).isRegisteredAt(server);
        Registration registration = server.getRegistrationFor(client);

        // Remove Client Security info
        server.clearSecurityContextFor(givenProtocol);

        // try to send request
        assertThrowsExactly(UnconnectedPeerException.class, () -> {
            server.send(registration, new ReadRequest(3), 1000);
        });
        assertThat(client).isSleepingOn(server);
    }

    @TestAllTransportLayer
    public void registered_device_with_bad_psk_identity_to_server_with_psk(Protocol givenProtocol,
            String givenClientEndpointProvider, String givenServerEndpointProvider)
            throws NonUniqueSecurityInfoException {
        // Create PSK server & start it
        server = givenServer.build(); // default server support PSK
        server.start();

        // Create PSK Client
        client = givenClient.connectingTo(server).usingPsk(GOOD_PSK_ID, GOOD_PSK_KEY).build();

        // Add client credentials with BAD PSK ID to the server
        server.getSecurityStore()
                .add(SecurityInfo.newPreSharedKeyInfo(client.getEndpointName(), BAD_PSK_ID, GOOD_PSK_KEY));

        // Check client is not registered
        assertThat(client).isNotRegisteredAt(server);

        // Start it.
        client.start();

        // Check client can not register
        assertThat(client).after(1, TimeUnit.SECONDS).isNotRegisteredAt(server);
    }

    @TestAllTransportLayer
    public void registered_device_with_bad_psk_key_to_server_with_psk(Protocol givenProtocol,
            String givenClientEndpointProvider, String givenServerEndpointProvider)
            throws NonUniqueSecurityInfoException {
        // Create PSK server & start it
        server = givenServer.build(); // default server support PSK
        server.start();

        // Create PSK Client
        client = givenClient.connectingTo(server).usingPsk(GOOD_PSK_ID, GOOD_PSK_KEY).build();

        // Add client credentials with BAD PSK KEY to the server
        server.getSecurityStore()
                .add(SecurityInfo.newPreSharedKeyInfo(client.getEndpointName(), BAD_PSK_ID, BAD_PSK_KEY));

        // Check client is not registered
        assertThat(client).isNotRegisteredAt(server);

        // Start it.
        client.start();

        // Check client can not register
        assertThat(client).after(1, TimeUnit.SECONDS).isNotRegisteredAt(server);
    }

    @TestAllTransportLayer
    public void registered_device_with_psk_and_bad_endpoint_to_server_with_psk(Protocol givenProtocol,
            String givenClientEndpointProvider, String givenServerEndpointProvider)
            throws NonUniqueSecurityInfoException {

        // Create PSK server & start it
        server = givenServer.build(); // default server support PSK
        server.start();

        // Create PSK Client
        client = givenClient.connectingTo(server).usingPsk(GOOD_PSK_ID, GOOD_PSK_KEY).build();

        // Add client credentials for another endpoint to the server
        server.getSecurityStore().add(SecurityInfo.newPreSharedKeyInfo(BAD_ENDPOINT, GOOD_PSK_ID, GOOD_PSK_KEY));

        // Check client is not registered
        assertThat(client).isNotRegisteredAt(server);

        // Start it.
        client.start();

        // Check client can not register
        assertThat(client).after(1, TimeUnit.SECONDS).isNotRegisteredAt(server);
    }

    @TestAllTransportLayer
    public void registered_device_with_psk_identity_to_server_with_psk_then_remove_security_info(Protocol givenProtocol,
            String givenClientEndpointProvider, String givenServerEndpointProvider)
            throws NonUniqueSecurityInfoException, InterruptedException {
        // Create PSK server & start it
        server = givenServer.build(); // default server support PSK
        server.start();

        // Create PSK Client
        client = givenClient.connectingTo(server).usingPsk(GOOD_PSK_ID, GOOD_PSK_KEY).build();

        // Add client credentials to the server
        server.getSecurityStore()
                .add(SecurityInfo.newPreSharedKeyInfo(client.getEndpointName(), GOOD_PSK_ID, GOOD_PSK_KEY));

        // Check client is not registered
        assertThat(client).isNotRegisteredAt(server);

        // Start it.
        client.start();
        client.waitForRegistrationTo(server);

        // Check client is well registered
        assertThat(client).isRegisteredAt(server);

        // remove compromised credentials
        boolean credentialsCompromised = true;
        server.getSecurityStore().remove(client.getEndpointName(), credentialsCompromised);

        // try to update
        client.triggerRegistrationUpdate();
        client.waitForUpdateTimeoutTo(server);
    }

    @TestAllTransportLayer
    public void registered_device_with_rpk_to_server_with_rpk(Protocol givenProtocol,
            String givenClientEndpointProvider, String givenServerEndpointProvider)
            throws NonUniqueSecurityInfoException, InterruptedException {
        // Create RPK server & start it
        server = givenServer.using(serverPublicKey, serverPrivateKey).build();
        server.start();

        // Create RPK Client
        client = givenClient.connectingTo(server) //
                .using(clientPublicKey, clientPrivateKey)//
                .trusting(serverPublicKey).build();

        // Add client credentials to the server
        server.getSecurityStore().add(SecurityInfo.newRawPublicKeyInfo(client.getEndpointName(), clientPublicKey));

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

    @TestAllTransportLayer
    public void registered_device_with_bad_rpk_to_server_with_rpk(Protocol givenProtocol,
            String givenClientEndpointProvider, String givenServerEndpointProvider)
            throws NonUniqueSecurityInfoException {
        // Create RPK server & start it
        server = givenServer.using(serverPublicKey, serverPrivateKey).build();
        server.start();

        // Create RPK Client
        client = givenClient.connectingTo(server) //
                .using(clientPublicKey, clientPrivateKey)//
                .trusting(serverPublicKey).build();

        // We use the server public key as bad client public key
        PublicKey bad_client_public_key = serverPublicKey;
        server.getSecurityStore()
                .add(SecurityInfo.newRawPublicKeyInfo(client.getEndpointName(), bad_client_public_key));

        // Check client is not registered
        assertThat(client).isNotRegisteredAt(server);

        // Start it and wait for registration
        client.start();
        assertThat(client).after(1, TimeUnit.SECONDS).isNotRegisteredAt(server);
    }

    @TestAllTransportLayer
    public void registered_device_with_rpk_to_server_with_rpk_then_remove_security_info(Protocol givenProtocol,
            String givenClientEndpointProvider, String givenServerEndpointProvider)
            throws NonUniqueSecurityInfoException {
        // Create RPK server & start it
        server = givenServer.using(serverPublicKey, serverPrivateKey).build();
        server.start();

        // Create RPK Client
        client = givenClient.connectingTo(server) //
                .using(clientPublicKey, clientPrivateKey)//
                .trusting(serverPublicKey).build();

        // Add client credentials to the server
        server.getSecurityStore().add(SecurityInfo.newRawPublicKeyInfo(client.getEndpointName(), clientPublicKey));

        // Check client is not registered
        assertThat(client).isNotRegisteredAt(server);

        // Start it and wait for registration
        client.start();
        server.waitForNewRegistrationOf(client);

        // Check client is well registered
        assertThat(client).isRegisteredAt(server);

        // remove compromised credentials
        server.getSecurityStore().remove(client.getEndpointName(), true);

        // try to update
        client.triggerRegistrationUpdate();
        client.waitForUpdateTimeoutTo(server);
    }

    @TestAllTransportLayer
    public void registered_device_with_rpk_and_bad_endpoint_to_server_with_rpk(Protocol givenProtocol,
            String givenClientEndpointProvider, String givenServerEndpointProvider)
            throws NonUniqueSecurityInfoException {

        // Create RPK server & start it
        server = givenServer.using(serverPublicKey, serverPrivateKey).build();
        server.start();

        // Create RPK Client
        client = givenClient.connectingTo(server) //
                .using(clientPublicKey, clientPrivateKey)//
                .trusting(serverPublicKey).build();

        // We use the server public key as bad client public key
        server.getSecurityStore().add(SecurityInfo.newRawPublicKeyInfo(BAD_ENDPOINT, clientPublicKey));

        // Check client is not registered
        assertThat(client).isNotRegisteredAt(server);

        // Start it and wait for registration
        client.start();
        assertThat(client).after(1, TimeUnit.SECONDS).isNotRegisteredAt(server);
    }

    @TestAllTransportLayer
    public void registered_device_with_x509cert_to_server_with_x509cert_then_remove_security_info(
            Protocol givenProtocol, String givenClientEndpointProvider, String givenServerEndpointProvider)
            throws NonUniqueSecurityInfoException, CertificateEncodingException {

        // Create X509 server & start it
        server = givenServer //
                .actingAsServerOnly()//
                .using(serverX509Cert, serverPrivateKeyFromCert)//
                .trusting(trustedCertificates).build();
        server.start();

        // Create X509 Client
        client = givenClient.connectingTo(server) //
                .using(clientX509Cert, clientPrivateKeyFromCert)//
                .trusting(serverX509Cert).build();

        // Add client credentials to the server
        server.getSecurityStore().add(SecurityInfo.newX509CertInfo(client.getEndpointName()));

        // Check client is not registered
        assertThat(client).isNotRegisteredAt(server);

        // Start it and wait for registration
        client.start();
        server.waitForNewRegistrationOf(client);

        // Check client is well registered
        assertThat(client).isRegisteredAt(server);

        // remove compromised credentials
        server.getSecurityStore().remove(client.getEndpointName(), true);

        // try to update
        client.triggerRegistrationUpdate();
        client.waitForUpdateTimeoutTo(server);
    }

    @TestAllTransportLayer
    public void registered_device_with_x509cert_to_server_with_x509cert(Protocol givenProtocol,
            String givenClientEndpointProvider, String givenServerEndpointProvider)
            throws NonUniqueSecurityInfoException, CertificateEncodingException, InterruptedException {

        // Create X509 server & start it
        server = givenServer //
                .actingAsServerOnly()//
                .using(serverX509Cert, serverPrivateKeyFromCert)//
                .trusting(trustedCertificates).build();
        server.start();

        // Create X509 Client
        client = givenClient.connectingTo(server) //
                .using(clientX509Cert, clientPrivateKeyFromCert)//
                .trusting(serverX509Cert).build();

        // Add client credentials to the server
        server.getSecurityStore().add(SecurityInfo.newX509CertInfo(client.getEndpointName()));

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

    @TestAllTransportLayer
    public void registered_device_with_x509cert_to_server_with_self_signed_x509cert(Protocol givenProtocol,
            String givenClientEndpointProvider, String givenServerEndpointProvider)
            throws NonUniqueSecurityInfoException, CertificateEncodingException, InterruptedException {
        // Create X509 server & start it
        server = givenServer //
                .actingAsServerOnly()//
                .using(serverX509CertSelfSigned, serverPrivateKeyFromCert)//
                .trusting(trustedCertificates).build();
        server.start();

        // Create X509 Client
        client = givenClient.connectingTo(server) //
                .using(clientX509Cert, clientPrivateKeyFromCert)//
                .trusting(serverX509CertSelfSigned).build();

        // Add client credentials to the server
        server.getSecurityStore().add(SecurityInfo.newX509CertInfo(client.getEndpointName()));

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

    @TestAllTransportLayer
    public void registered_device_with_x509cert_and_bad_endpoint_to_server_with_x509cert(Protocol givenProtocol,
            String givenClientEndpointProvider, String givenServerEndpointProvider)
            throws NonUniqueSecurityInfoException, CertificateEncodingException {

        // Create X509 server & start it
        server = givenServer //
                .actingAsServerOnly()//
                .using(serverX509Cert, serverPrivateKeyFromCert)//
                .trusting(trustedCertificates).build();
        server.start();

        // Create X509 Client
        client = givenClient.connectingTo(server) //
                .using(clientX509Cert, clientPrivateKeyFromCert)//
                .trusting(serverX509Cert).build();

        // Add client credentials to the server
        server.getSecurityStore().add(SecurityInfo.newX509CertInfo(BAD_ENDPOINT));

        // Check client is not registered
        assertThat(client).isNotRegisteredAt(server);

        // Start it and check we can not register
        client.start();
        assertThat(client).after(1, TimeUnit.SECONDS).isNotRegisteredAt(server);
    }

    @TestAllTransportLayer
    public void registered_device_with_x509cert_and_bad_cn_certificate_to_server_with_x509cert(Protocol givenProtocol,
            String givenClientEndpointProvider, String givenServerEndpointProvider)
            throws NonUniqueSecurityInfoException, CertificateEncodingException {
        // Create X509 server & start it
        server = givenServer //
                .actingAsServerOnly()//
                .using(serverX509Cert, serverPrivateKeyFromCert)//
                .trusting(trustedCertificates).build();
        server.start();

        // Create X509 Client
        client = givenClient.connectingTo(server) //
                .named(BAD_ENDPOINT)//
                .using(clientX509CertWithBadCN, clientPrivateKeyFromCert)//
                .trusting(serverX509Cert).build();

        // Add client credentials to the server
        server.getSecurityStore().add(SecurityInfo.newX509CertInfo(client.getEndpointName()));

        // Check client is not registered
        assertThat(client).isNotRegisteredAt(server);

        // Start it and check we can not register
        client.start();
        assertThat(client).after(1, TimeUnit.SECONDS).isNotRegisteredAt(server);
    }

    @TestAllTransportLayer
    public void registered_device_with_x509cert_and_bad_private_key_to_server_with_x509cert(Protocol givenProtocol,
            String givenClientEndpointProvider, String givenServerEndpointProvider)
            throws NonUniqueSecurityInfoException, CertificateEncodingException {

        // Create X509 server & start it
        server = givenServer //
                .actingAsServerOnly()//
                .using(serverX509Cert, serverPrivateKeyFromCert)//
                .trusting(trustedCertificates).build();
        server.start();

        // Create X509 Client
        // we use the RPK private key as bad key, this key will not be compatible with the client certificate
        PrivateKey badPrivateKey = clientPrivateKey;
        client = givenClient.connectingTo(server) //
                .using(clientX509Cert, badPrivateKey)//
                .trusting(serverX509Cert).build();

        // Add client credentials to the server
        server.getSecurityStore().add(SecurityInfo.newX509CertInfo(client.getEndpointName()));

        // Check client is not registered
        assertThat(client).isNotRegisteredAt(server);

        // Start it and check we can not register
        client.start();
        assertThat(client).after(1, TimeUnit.SECONDS).isNotRegisteredAt(server);
    }

    @TestAllTransportLayer
    public void registered_device_with_untrusted_x509cert_to_server_with_x509cert(Protocol givenProtocol,
            String givenClientEndpointProvider, String givenServerEndpointProvider)
            throws NonUniqueSecurityInfoException, CertificateEncodingException {

        // Create X509 server & start it
        server = givenServer //
                .actingAsServerOnly()//
                .using(serverX509Cert, serverPrivateKeyFromCert)//
                .trusting(trustedCertificates).build();
        server.start();

        // Create X509 Client
        client = givenClient.connectingTo(server) //
                .using(clientX509CertNotTrusted, clientPrivateKeyFromCert)//
                .trusting(serverX509Cert).build();

        // Add client credentials to the server
        server.getSecurityStore().add(SecurityInfo.newX509CertInfo(client.getEndpointName()));

        // Check client is not registered
        assertThat(client).isNotRegisteredAt(server);

        // Start it and check we can not register
        client.start();
        assertThat(client).after(1, TimeUnit.SECONDS).isNotRegisteredAt(server);
    }

    @TestAllTransportLayer
    public void registered_device_with_selfsigned_x509cert_to_server_with_x509cert(Protocol givenProtocol,
            String givenClientEndpointProvider, String givenServerEndpointProvider)
            throws NonUniqueSecurityInfoException, CertificateEncodingException {
        // Create X509 server & start it
        server = givenServer //
                .actingAsServerOnly()//
                .using(serverX509Cert, serverPrivateKeyFromCert)//
                .trusting(trustedCertificates).build();
        server.start();

        // Create X509 Client
        client = givenClient.connectingTo(server) //
                .using(Credentials.clientX509CertSelfSigned, clientPrivateKeyFromCert)//
                .trusting(serverX509Cert).build();

        // Add client credentials to the server
        server.getSecurityStore().add(SecurityInfo.newX509CertInfo(client.getEndpointName()));

        // Check client is not registered
        assertThat(client).isNotRegisteredAt(server);

        // Start it and check we can not register
        client.start();
        assertThat(client).after(1, TimeUnit.SECONDS).isNotRegisteredAt(server);
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
    @TestAllTransportLayer
    public void registered_device_with_x509cert_to_server_with_x509cert_rootca_certificate_usage_ca(
            Protocol givenProtocol, String givenClientEndpointProvider, String givenServerEndpointProvider)
            throws NonUniqueSecurityInfoException, CertificateEncodingException {
        server = givenServer //
                .actingAsServerOnly()//
                .using(serverIntPrivateKeyFromCert, serverIntX509CertChain)//
                .trusting(trustedCertificates).build();
        server.start();

        client = givenClient.connectingTo(server) //
                .using(clientX509Cert, clientPrivateKeyFromCert)//
                .trusting(serverIntX509CertChain[0], CA_CONSTRAINT, clientTrustStore).build();

        server.getSecurityStore().add(SecurityInfo.newX509CertInfo(client.getEndpointName()));

        assertThat(client).isNotRegisteredAt(server);
        client.start();
        assertThat(client).after(1, TimeUnit.SECONDS).isNotRegisteredAt(server);
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
    @TestAllTransportLayer
    public void registered_device_with_x509cert_to_server_with_x509cert_rootca_certificate_usage_ca_intca_given(
            Protocol givenProtocol, String givenClientEndpointProvider, String givenServerEndpointProvider)
            throws NonUniqueSecurityInfoException, CertificateEncodingException, InterruptedException {

        server = givenServer //
                .actingAsServerOnly()//
                .using(serverIntPrivateKeyFromCert, serverIntX509CertChain)//
                .trusting(trustedCertificates).build();
        server.start();

        client = givenClient.connectingTo(server) //
                .using(clientX509Cert, clientPrivateKeyFromCert)//
                .trusting(serverIntX509CertChain[1], CA_CONSTRAINT, clientTrustStore).build();

        server.getSecurityStore().add(SecurityInfo.newX509CertInfo(client.getEndpointName()));

        assertThat(client).isNotRegisteredAt(server);
        client.start();
        server.waitForNewRegistrationOf(client);
        assertThat(client).isRegisteredAt(server);
        Registration registration = server.getRegistrationFor(client);

        ReadResponse response = server.send(registration, new ReadRequest(3, 0, 1), 500);
        assertThat(response.isSuccess()).isTrue();
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
    @TestAllTransportLayer
    public void registered_device_with_empty_truststore_with_x509cert_to_server_with_x509cert_rootca_certificate_usage_ca_intca_given(
            Protocol givenProtocol, String givenClientEndpointProvider, String givenServerEndpointProvider)
            throws NonUniqueSecurityInfoException, CertificateEncodingException, InterruptedException {
        server = givenServer //
                .actingAsServerOnly()//
                .using(serverIntPrivateKeyFromCert, serverIntX509CertChain)//
                .trusting(trustedCertificates).build();
        server.start();

        // create a not empty trustore which does not contains any certificate of server certchain.
        X509Certificate[] truststore = new X509Certificate[] { serverIntX509CertSelfSigned };
        // e.g. we use a selfsigned certificate not used in certchain of this test.

        client = givenClient.connectingTo(server) //
                .using(clientX509Cert, clientPrivateKeyFromCert)//
                .trusting(serverIntX509CertChain[1], CA_CONSTRAINT, truststore).build();

        server.getSecurityStore().add(SecurityInfo.newX509CertInfo(client.getEndpointName()));

        assertThat(client).isNotRegisteredAt(server);
        client.start();
        assertThat(client).after(1, TimeUnit.SECONDS).isNotRegisteredAt(server);
    }

    /**
     * // *
     *
     * <pre>
     * //     * Test scenario:
     * //     * - Certificate Usage = CA constraint
     * //     * - Server Certificate = root CA certificate (not end-entity certificate)
     * //     * - Server's TLS Server Certificate = intermediate signed certificate (with SAN DNS entry)
     * //     * - Server accepts client
     * //     * - Client Trust Store = root CA
     * //     *
     * //     * Expected outcome:
     * //     * - Client is able to connect (root CA cert is part of the chain)
     * //     *
     * </pre>
     *
     * //
     */
    @TestAllTransportLayer
    public void registered_device_with_x509cert_to_server_with_x509cert_rootca_certificate_ca_domain_root_ca_given(
            Protocol givenProtocol, String givenClientEndpointProvider, String givenServerEndpointProvider)
            throws NonUniqueSecurityInfoException, CertificateEncodingException, InterruptedException {
        server = givenServer //
                .actingAsServerOnly()//
                .using(serverIntPrivateKeyFromCert, serverIntX509CertChain)//
                .trusting(trustedCertificates).build();
        server.start();

        client = givenClient.connectingTo(server) //
                .using(clientX509Cert, clientPrivateKeyFromCert)//
                .trusting(rootCAX509Cert, CA_CONSTRAINT, clientTrustStore).build();

        server.getSecurityStore().add(SecurityInfo.newX509CertInfo(client.getEndpointName()));

        assertThat(client).isNotRegisteredAt(server);
        client.start();
        server.waitForNewRegistrationOf(client);
        assertThat(client).isRegisteredAt(server);
        Registration registration = server.getRegistrationFor(client);

        ReadResponse response = server.send(registration, new ReadRequest(3, 0, 1), 500);
        assertThat(response.isSuccess()).isTrue();
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
    @TestAllTransportLayer
    public void registered_device_with_x509cert_to_server_with_x509cert_rootca_certificate_usage_ca_other_server_cert_given(
            Protocol givenProtocol, String givenClientEndpointProvider, String givenServerEndpointProvider)
            throws NonUniqueSecurityInfoException, CertificateEncodingException {
        server = givenServer //
                .actingAsServerOnly()//
                .using(serverIntPrivateKeyFromCert, serverIntX509CertChain)//
                .trusting(trustedCertificates).build();
        server.start();

        client = givenClient.connectingTo(server) //
                .using(clientX509Cert, clientPrivateKeyFromCert)//
                .trusting(serverX509Cert, CA_CONSTRAINT, clientTrustStore).build();

        server.getSecurityStore().add(SecurityInfo.newX509CertInfo(client.getEndpointName()));

        assertThat(client).isNotRegisteredAt(server);
        client.start();
        assertThat(client).after(1, TimeUnit.SECONDS).isNotRegisteredAt(server);
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
    @TestAllTransportLayer
    public void registered_device_with_x509cert_to_server_with_x509cert_rootca_certificate_usage_ca_selfsigned_server_cert_given(
            Protocol givenProtocol, String givenClientEndpointProvider, String givenServerEndpointProvider)
            throws NonUniqueSecurityInfoException, CertificateEncodingException {
        server = givenServer //
                .actingAsServerOnly()//
                .using(serverIntPrivateKeyFromCert, serverIntX509CertChain)//
                .trusting(trustedCertificates).build();
        server.start();

        client = givenClient.connectingTo(server) //
                .using(clientX509Cert, clientPrivateKeyFromCert)//
                .trusting(serverIntX509CertSelfSigned, CA_CONSTRAINT, clientTrustStore).build();

        server.getSecurityStore().add(SecurityInfo.newX509CertInfo(client.getEndpointName()));

        assertThat(client).isNotRegisteredAt(server);
        client.start();
        assertThat(client).after(1, TimeUnit.SECONDS).isNotRegisteredAt(server);
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
    @TestAllTransportLayer
    public void registered_device_with_x509cert_to_server_with_x509cert_selfsigned_certificate_usage_ca_selfsigned_server_cert_given(
            Protocol givenProtocol, String givenClientEndpointProvider, String givenServerEndpointProvider)
            throws NonUniqueSecurityInfoException, CertificateEncodingException {
        server = givenServer //
                .actingAsServerOnly()//
                .using(serverIntPrivateKeyFromCert, serverIntX509CertSelfSigned)//
                .trusting(trustedCertificates).build();
        server.start();

        client = givenClient.connectingTo(server) //
                .using(clientX509Cert, clientPrivateKeyFromCert)//
                .trusting(serverIntX509CertSelfSigned, CA_CONSTRAINT, clientTrustStore).build();

        server.getSecurityStore().add(SecurityInfo.newX509CertInfo(client.getEndpointName()));

        assertThat(client).isNotRegisteredAt(server);
        client.start();
        assertThat(client).after(1, TimeUnit.SECONDS).isNotRegisteredAt(server);
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
    @TestAllTransportLayer
    public void registered_device_with_x509cert_to_server_with_x509cert_server_certificate_usage_ca_server_cert_wo_chain_given(
            Protocol givenProtocol, String givenClientEndpointProvider, String givenServerEndpointProvider)
            throws NonUniqueSecurityInfoException, CertificateEncodingException {
        server = givenServer //
                .actingAsServerOnly()//
                .using(serverIntPrivateKeyFromCert, serverIntX509CertChain[0])//
                .trusting(trustedCertificates).build();
        server.start();

        client = givenClient.connectingTo(server) //
                .using(clientX509Cert, clientPrivateKeyFromCert)//
                .trusting(serverIntX509CertChain[0], CA_CONSTRAINT, clientTrustStore).build();

        server.getSecurityStore().add(SecurityInfo.newX509CertInfo(client.getEndpointName()));

        assertThat(client).isNotRegisteredAt(server);
        client.start();
        assertThat(client).after(1, TimeUnit.SECONDS).isNotRegisteredAt(server);
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
    @TestAllTransportLayer
    public void registered_device_with_x509cert_to_server_with_x509cert_rootca_certificate_usage_service(
            Protocol givenProtocol, String givenClientEndpointProvider, String givenServerEndpointProvider)
            throws NonUniqueSecurityInfoException, CertificateEncodingException, InterruptedException {
        server = givenServer //
                .actingAsServerOnly()//
                .using(serverIntPrivateKeyFromCert, serverIntX509CertChain)//
                .trusting(trustedCertificates).build();
        server.start();

        client = givenClient.connectingTo(server) //
                .using(clientX509Cert, clientPrivateKeyFromCert)//
                .trusting(serverIntX509CertChain[0], SERVICE_CERTIFICATE_CONSTRAINT, clientTrustStore).build();

        server.getSecurityStore().add(SecurityInfo.newX509CertInfo(client.getEndpointName()));

        assertThat(client).isNotRegisteredAt(server);
        client.start();
        server.waitForNewRegistrationOf(client);
        assertThat(client).isRegisteredAt(server);
        Registration registration = server.getRegistrationFor(client);

        ReadResponse response = server.send(registration, new ReadRequest(3, 0, 1), 500);
        assertThat(response.isSuccess()).isTrue();
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
    @TestAllTransportLayer
    public void registered_device_with_x509cert_to_server_with_x509cert_rootca_certificate_service_domain_root_ca_given(
            Protocol givenProtocol, String givenClientEndpointProvider, String givenServerEndpointProvider)
            throws NonUniqueSecurityInfoException, CertificateEncodingException {
        server = givenServer //
                .actingAsServerOnly()//
                .using(serverIntPrivateKeyFromCert, serverIntX509CertChain)//
                .trusting(trustedCertificates).build();
        server.start();

        client = givenClient.connectingTo(server) //
                .using(clientX509Cert, clientPrivateKeyFromCert)//
                .trusting(rootCAX509Cert, SERVICE_CERTIFICATE_CONSTRAINT, clientTrustStore).build();

        server.getSecurityStore().add(SecurityInfo.newX509CertInfo(client.getEndpointName()));

        assertThat(client).isNotRegisteredAt(server);
        client.start();
        assertThat(client).after(1, TimeUnit.SECONDS).isNotRegisteredAt(server);
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
    @TestAllTransportLayer
    public void registered_device_with_x509cert_to_server_with_x509cert_rootca_certificate_usage_service_other_server_cert_given(
            Protocol givenProtocol, String givenClientEndpointProvider, String givenServerEndpointProvider)
            throws NonUniqueSecurityInfoException, CertificateEncodingException {
        server = givenServer //
                .actingAsServerOnly()//
                .using(serverIntPrivateKeyFromCert, serverIntX509CertChain)//
                .trusting(trustedCertificates).build();
        server.start();

        client = givenClient.connectingTo(server) //
                .using(clientX509Cert, clientPrivateKeyFromCert)//
                .trusting(serverX509Cert, SERVICE_CERTIFICATE_CONSTRAINT, clientTrustStore).build();

        server.getSecurityStore().add(SecurityInfo.newX509CertInfo(client.getEndpointName()));

        assertThat(client).isNotRegisteredAt(server);
        client.start();
        assertThat(client).after(1, TimeUnit.SECONDS).isNotRegisteredAt(server);
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
    @TestAllTransportLayer
    public void registered_device_with_x509cert_to_server_with_x509cert_rootca_certificate_usage_service_selfsigned_server_cert_given(
            Protocol givenProtocol, String givenClientEndpointProvider, String givenServerEndpointProvider)
            throws NonUniqueSecurityInfoException, CertificateEncodingException {
        server = givenServer //
                .actingAsServerOnly()//
                .using(serverIntPrivateKeyFromCert, serverIntX509CertChain)//
                .trusting(trustedCertificates).build();
        server.start();

        client = givenClient.connectingTo(server) //
                .using(clientX509Cert, clientPrivateKeyFromCert)//
                .trusting(serverIntX509CertSelfSigned, SERVICE_CERTIFICATE_CONSTRAINT, clientTrustStore).build();

        server.getSecurityStore().add(SecurityInfo.newX509CertInfo(client.getEndpointName()));

        assertThat(client).isNotRegisteredAt(server);
        client.start();
        assertThat(client).after(1, TimeUnit.SECONDS).isNotRegisteredAt(server);
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
    @TestAllTransportLayer
    public void registered_device_with_x509cert_to_server_with_x509cert_selfsigned_certificate_usage_service_selfsigned_server_cert_given(
            Protocol givenProtocol, String givenClientEndpointProvider, String givenServerEndpointProvider)
            throws NonUniqueSecurityInfoException, CertificateEncodingException {
        server = givenServer //
                .actingAsServerOnly()//
                .using(serverIntPrivateKeyFromCert, serverIntX509CertSelfSigned)//
                .trusting(trustedCertificates).build();
        server.start();

        client = givenClient.connectingTo(server) //
                .using(clientX509Cert, clientPrivateKeyFromCert)//
                .trusting(serverIntX509CertSelfSigned, SERVICE_CERTIFICATE_CONSTRAINT, clientTrustStore).build();

        server.getSecurityStore().add(SecurityInfo.newX509CertInfo(client.getEndpointName()));

        assertThat(client).isNotRegisteredAt(server);
        client.start();
        assertThat(client).after(1, TimeUnit.SECONDS).isNotRegisteredAt(server);
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
    @TestAllTransportLayer
    public void registered_device_with_x509cert_to_server_with_x509cert_server_certificate_usage_service_server_cert_wo_chain_given(
            Protocol givenProtocol, String givenClientEndpointProvider, String givenServerEndpointProvider)
            throws NonUniqueSecurityInfoException, CertificateEncodingException {
        server = givenServer //
                .actingAsServerOnly()//
                .using(serverIntPrivateKeyFromCert, serverIntX509CertChain[0])//
                .trusting(trustedCertificates).build();
        server.start();

        client = givenClient.connectingTo(server) //
                .using(clientX509Cert, clientPrivateKeyFromCert)//
                .trusting(serverIntX509CertChain[0], SERVICE_CERTIFICATE_CONSTRAINT, clientTrustStore).build();

        server.getSecurityStore().add(SecurityInfo.newX509CertInfo(client.getEndpointName()));

        assertThat(client).isNotRegisteredAt(server);
        client.start();
        assertThat(client).after(1, TimeUnit.SECONDS).isNotRegisteredAt(server);

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
    @TestAllTransportLayer
    public void registered_device_with_x509cert_to_server_with_x509cert_rootca_certificate_usage_taa(
            Protocol givenProtocol, String givenClientEndpointProvider, String givenServerEndpointProvider)
            throws NonUniqueSecurityInfoException, CertificateEncodingException {
        server = givenServer //
                .actingAsServerOnly()//
                .using(serverIntPrivateKeyFromCert, serverIntX509CertChain)//
                .trusting(trustedCertificates).build();
        server.start();

        client = givenClient.connectingTo(server) //
                .using(clientX509Cert, clientPrivateKeyFromCert)//
                .trusting(serverIntX509CertChain[0], TRUST_ANCHOR_ASSERTION, clientTrustStore).build();

        server.getSecurityStore().add(SecurityInfo.newX509CertInfo(client.getEndpointName()));

        assertThat(client).isNotRegisteredAt(server);
        client.start();
        assertThat(client).after(1, TimeUnit.SECONDS).isNotRegisteredAt(server);
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
    @TestAllTransportLayer
    public void registered_device_with_x509cert_to_server_with_x509cert_rootca_certificate_usage_taa_intca_given(
            Protocol givenProtocol, String givenClientEndpointProvider, String givenServerEndpointProvider)
            throws NonUniqueSecurityInfoException, CertificateEncodingException, InterruptedException {
        server = givenServer //
                .actingAsServerOnly()//
                .using(serverIntPrivateKeyFromCert, serverIntX509CertChain)//
                .trusting(trustedCertificates).build();
        server.start();

        client = givenClient.connectingTo(server) //
                .using(clientX509Cert, clientPrivateKeyFromCert)//
                .trusting(serverIntX509CertChain[1], TRUST_ANCHOR_ASSERTION, clientTrustStore).build();

        server.getSecurityStore().add(SecurityInfo.newX509CertInfo(client.getEndpointName()));

        assertThat(client).isNotRegisteredAt(server);
        client.start();
        server.waitForNewRegistrationOf(client);
        assertThat(client).isRegisteredAt(server);
        Registration registration = server.getRegistrationFor(client);

        ReadResponse response = server.send(registration, new ReadRequest(3, 0, 1), 500);
        assertThat(response.isSuccess()).isTrue();
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
    @TestAllTransportLayer
    public void registered_device_with_x509cert_to_server_with_x509cert_rootca_certificate_taa_domain_root_ca_given(
            Protocol givenProtocol, String givenClientEndpointProvider, String givenServerEndpointProvider)
            throws NonUniqueSecurityInfoException, CertificateEncodingException, InterruptedException {
        server = givenServer //
                .actingAsServerOnly()//
                .using(serverIntPrivateKeyFromCert, serverIntX509CertChain)//
                .trusting(trustedCertificates).build();
        server.start();

        client = givenClient.connectingTo(server) //
                .using(clientX509Cert, clientPrivateKeyFromCert)//
                .trusting(rootCAX509Cert, TRUST_ANCHOR_ASSERTION, clientTrustStore).build();

        server.getSecurityStore().add(SecurityInfo.newX509CertInfo(client.getEndpointName()));

        assertThat(client).isNotRegisteredAt(server);
        client.start();
        server.waitForNewRegistrationOf(client);
        assertThat(client).isRegisteredAt(server);
        Registration registration = server.getRegistrationFor(client);

        ReadResponse response = server.send(registration, new ReadRequest(3, 0, 1), 500);
        assertThat(response.isSuccess()).isTrue();
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
    @TestAllTransportLayer
    public void registered_device_with_x509cert_to_server_with_x509cert_rootca_certificate_usage_taa_other_server_cert_given(
            Protocol givenProtocol, String givenClientEndpointProvider, String givenServerEndpointProvider)
            throws NonUniqueSecurityInfoException, CertificateEncodingException {
        server = givenServer //
                .actingAsServerOnly()//
                .using(serverIntPrivateKeyFromCert, serverIntX509CertChain)//
                .trusting(trustedCertificates).build();
        server.start();

        client = givenClient.connectingTo(server) //
                .using(clientX509Cert, clientPrivateKeyFromCert)//
                .trusting(serverX509Cert, TRUST_ANCHOR_ASSERTION, clientTrustStore).build();

        server.getSecurityStore().add(SecurityInfo.newX509CertInfo(client.getEndpointName()));

        assertThat(client).isNotRegisteredAt(server);
        client.start();
        assertThat(client).after(1, TimeUnit.SECONDS).isNotRegisteredAt(server);
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
    @TestAllTransportLayer
    public void registered_device_with_x509cert_to_server_with_x509cert_rootca_certificate_usage_taa_selfsigned_server_cert_given(
            Protocol givenProtocol, String givenClientEndpointProvider, String givenServerEndpointProvider)
            throws NonUniqueSecurityInfoException, CertificateEncodingException {
        server = givenServer //
                .actingAsServerOnly()//
                .using(serverIntPrivateKeyFromCert, serverIntX509CertChain)//
                .trusting(trustedCertificates).build();
        server.start();

        client = givenClient.connectingTo(server) //
                .using(clientX509Cert, clientPrivateKeyFromCert)//
                .trusting(serverIntX509CertSelfSigned, TRUST_ANCHOR_ASSERTION, clientTrustStore).build();

        server.getSecurityStore().add(SecurityInfo.newX509CertInfo(client.getEndpointName()));

        assertThat(client).isNotRegisteredAt(server);
        client.start();
        assertThat(client).after(1, TimeUnit.SECONDS).isNotRegisteredAt(server);
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
    @TestAllTransportLayer
    public void registered_device_with_x509cert_to_server_with_x509cert_selfsigned_certificate_usage_taa_selfsigned_server_cert_given(
            Protocol givenProtocol, String givenClientEndpointProvider, String givenServerEndpointProvider)
            throws NonUniqueSecurityInfoException, CertificateEncodingException {
        server = givenServer //
                .actingAsServerOnly()//
                .using(serverIntPrivateKeyFromCert, serverIntX509CertSelfSigned)//
                .trusting(trustedCertificates).build();
        server.start();

        client = givenClient.connectingTo(server) //
                .using(clientX509Cert, clientPrivateKeyFromCert)//
                .trusting(serverIntX509CertSelfSigned, TRUST_ANCHOR_ASSERTION, clientTrustStore).build();

        server.getSecurityStore().add(SecurityInfo.newX509CertInfo(client.getEndpointName()));

        assertThat(client).isNotRegisteredAt(server);
        client.start();
        assertThat(client).after(1, TimeUnit.SECONDS).isNotRegisteredAt(server);
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
    @TestAllTransportLayer
    public void registered_device_with_x509cert_to_server_with_x509cert_server_certificate_usage_taa_server_cert_wo_chain_given(
            Protocol givenProtocol, String givenClientEndpointProvider, String givenServerEndpointProvider)
            throws NonUniqueSecurityInfoException, CertificateEncodingException {
        server = givenServer //
                .actingAsServerOnly()//
                .using(serverIntPrivateKeyFromCert, serverIntX509CertChain[0])//
                .trusting(trustedCertificates).build();
        server.start();

        client = givenClient.connectingTo(server) //
                .using(clientX509Cert, clientPrivateKeyFromCert)//
                .trusting(serverIntX509CertChain[0], TRUST_ANCHOR_ASSERTION, clientTrustStore).build();

        server.getSecurityStore().add(SecurityInfo.newX509CertInfo(client.getEndpointName()));

        assertThat(client).isNotRegisteredAt(server);
        client.start();
        assertThat(client).after(1, TimeUnit.SECONDS).isNotRegisteredAt(server);
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
    @TestAllTransportLayer
    public void registered_device_with_x509cert_to_server_with_x509cert_rootca_certificate_usage_domain(
            Protocol givenProtocol, String givenClientEndpointProvider, String givenServerEndpointProvider)
            throws NonUniqueSecurityInfoException, CertificateEncodingException, InterruptedException {
        server = givenServer //
                .actingAsServerOnly()//
                .using(serverIntPrivateKeyFromCert, serverIntX509CertChain)//
                .trusting(trustedCertificates).build();
        server.start();

        client = givenClient.connectingTo(server) //
                .using(clientX509Cert, clientPrivateKeyFromCert)//
                .trusting(serverIntX509CertChain[0], DOMAIN_ISSUER_CERTIFICATE, clientTrustStore).build();

        server.getSecurityStore().add(SecurityInfo.newX509CertInfo(client.getEndpointName()));

        assertThat(client).isNotRegisteredAt(server);
        client.start();
        server.waitForNewRegistrationOf(client);
        assertThat(client).isRegisteredAt(server);
        Registration registration = server.getRegistrationFor(client);

        ReadResponse response = server.send(registration, new ReadRequest(3, 0, 1), 500);
        assertThat(response.isSuccess()).isTrue();
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
    @TestAllTransportLayer
    public void registered_device_with_x509cert_to_server_with_x509cert_rootca_certificate_usage_domain_root_ca_given(
            Protocol givenProtocol, String givenClientEndpointProvider, String givenServerEndpointProvider)
            throws NonUniqueSecurityInfoException, CertificateEncodingException {
        server = givenServer //
                .actingAsServerOnly()//
                .using(serverIntPrivateKeyFromCert, serverIntX509CertChain)//
                .trusting(trustedCertificates).build();
        server.start();

        client = givenClient.connectingTo(server) //
                .using(clientX509Cert, clientPrivateKeyFromCert)//
                .trusting(rootCAX509Cert, DOMAIN_ISSUER_CERTIFICATE, clientTrustStore).build();

        server.getSecurityStore().add(SecurityInfo.newX509CertInfo(client.getEndpointName()));

        assertThat(client).isNotRegisteredAt(server);
        client.start();
        assertThat(client).after(1, TimeUnit.SECONDS).isNotRegisteredAt(server);
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
    @TestAllTransportLayer
    public void registered_device_with_x509cert_to_server_with_x509cert_rootca_certificate_usage_domain_other_server_cert_given(
            Protocol givenProtocol, String givenClientEndpointProvider, String givenServerEndpointProvider)
            throws NonUniqueSecurityInfoException, CertificateEncodingException {
        server = givenServer //
                .actingAsServerOnly()//
                .using(serverIntPrivateKeyFromCert, serverIntX509CertChain)//
                .trusting(trustedCertificates).build();
        server.start();

        client = givenClient.connectingTo(server) //
                .using(clientX509Cert, clientPrivateKeyFromCert)//
                .trusting(serverX509Cert, DOMAIN_ISSUER_CERTIFICATE, clientTrustStore).build();

        server.getSecurityStore().add(SecurityInfo.newX509CertInfo(client.getEndpointName()));

        assertThat(client).isNotRegisteredAt(server);
        client.start();
        assertThat(client).after(1, TimeUnit.SECONDS).isNotRegisteredAt(server);
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
    @TestAllTransportLayer
    public void registered_device_with_x509cert_to_server_with_x509cert_rootca_certificate_usage_domain_selfsigned_server_cert_given(
            Protocol givenProtocol, String givenClientEndpointProvider, String givenServerEndpointProvider)
            throws NonUniqueSecurityInfoException, CertificateEncodingException {
        server = givenServer //
                .actingAsServerOnly()//
                .using(serverIntPrivateKeyFromCert, serverIntX509CertChain)//
                .trusting(trustedCertificates).build();
        server.start();

        client = givenClient.connectingTo(server) //
                .using(clientX509Cert, clientPrivateKeyFromCert)//
                .trusting(serverIntX509CertSelfSigned, DOMAIN_ISSUER_CERTIFICATE, clientTrustStore).build();

        server.getSecurityStore().add(SecurityInfo.newX509CertInfo(client.getEndpointName()));

        assertThat(client).isNotRegisteredAt(server);
        client.start();
        assertThat(client).after(1, TimeUnit.SECONDS).isNotRegisteredAt(server);
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
    @TestAllTransportLayer
    public void registered_device_with_x509cert_to_server_with_x509cert_selfsigned_certificate_usage_domain_selfsigned_server_cert_given(
            Protocol givenProtocol, String givenClientEndpointProvider, String givenServerEndpointProvider)
            throws NonUniqueSecurityInfoException, CertificateEncodingException, InterruptedException {
        server = givenServer //
                .actingAsServerOnly()//
                .using(serverIntPrivateKeyFromCert, serverIntX509CertSelfSigned)//
                .trusting(trustedCertificates).build();
        server.start();

        client = givenClient.connectingTo(server) //
                .using(clientX509Cert, clientPrivateKeyFromCert)//
                .trusting(serverIntX509CertSelfSigned, DOMAIN_ISSUER_CERTIFICATE, clientTrustStore).build();

        server.getSecurityStore().add(SecurityInfo.newX509CertInfo(client.getEndpointName()));

        assertThat(client).isNotRegisteredAt(server);
        client.start();
        server.waitForNewRegistrationOf(client);
        assertThat(client).isRegisteredAt(server);
        Registration registration = server.getRegistrationFor(client);

        ReadResponse response = server.send(registration, new ReadRequest(3, 0, 1), 500);
        assertThat(response.isSuccess()).isTrue();
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
    @TestAllTransportLayer
    public void registered_device_with_x509cert_to_server_with_x509cert_server_certificate_usage_domain_server_cert_wo_chain_given(
            Protocol givenProtocol, String givenClientEndpointProvider, String givenServerEndpointProvider)
            throws NonUniqueSecurityInfoException, CertificateEncodingException, InterruptedException {
        server = givenServer //
                .actingAsServerOnly()//
                .using(serverIntPrivateKeyFromCert, serverIntX509CertChain[0])//
                .trusting(trustedCertificates).build();
        server.start();

        client = givenClient.connectingTo(server) //
                .using(clientX509Cert, clientPrivateKeyFromCert)//
                .trusting(serverIntX509CertChain[0], DOMAIN_ISSUER_CERTIFICATE, clientTrustStore).build();

        server.getSecurityStore().add(SecurityInfo.newX509CertInfo(client.getEndpointName()));

        assertThat(client).isNotRegisteredAt(server);
        client.start();
        server.waitForNewRegistrationOf(client);
        assertThat(client).isRegisteredAt(server);
        Registration registration = server.getRegistrationFor(client);

        ReadResponse response = server.send(registration, new ReadRequest(3, 0, 1), 500);
        assertThat(response.isSuccess()).isTrue();
    }

    /* ---- */

    @TestAllTransportLayer
    public void registered_device_with_x509cert_to_server_with_rpk(Protocol givenProtocol,
            String givenClientEndpointProvider, String givenServerEndpointProvider)
            throws NonUniqueSecurityInfoException, CertificateEncodingException {
        server = givenServer.using(serverX509Cert.getPublicKey(), serverPrivateKeyFromCert).build();
        server.start();

        client = givenClient.connectingTo(server) //
                .using(clientX509Cert, clientPrivateKeyFromCert)//
                .trusting(serverX509Cert).build();

        server.getSecurityStore()
                .add(SecurityInfo.newRawPublicKeyInfo(client.getEndpointName(), clientX509Cert.getPublicKey()));

        assertThat(client).isNotRegisteredAt(server);
        client.start();
        assertThat(client).after(1, TimeUnit.SECONDS).isNotRegisteredAt(server);
    }

    @TestAllTransportLayer
    public void registered_device_with_rpk_to_server_with_x509cert(Protocol givenProtocol,
            String givenClientEndpointProvider, String givenServerEndpointProvider)
            throws NonUniqueSecurityInfoException, InterruptedException {
        server = givenServer //
                .actingAsServerOnly()//
                .using(serverX509Cert, serverPrivateKeyFromCert)//
                .trusting(trustedCertificates).build();
        server.start();

        client = givenClient.connectingTo(server) //
                .using(clientPublicKey, clientPrivateKey)//
                .trusting(serverX509Cert.getPublicKey()).build();

        server.getSecurityStore().add(SecurityInfo.newRawPublicKeyInfo(client.getEndpointName(), clientPublicKey));

        assertThat(client).isNotRegisteredAt(server);
        client.start();
        server.waitForNewRegistrationOf(client);
        assertThat(client).isRegisteredAt(server);
        Registration registration = server.getRegistrationFor(client);

        ReadResponse response = server.send(registration, new ReadRequest(3, 0, 1), 500);
        assertThat(response.isSuccess()).isTrue();

    }
}
