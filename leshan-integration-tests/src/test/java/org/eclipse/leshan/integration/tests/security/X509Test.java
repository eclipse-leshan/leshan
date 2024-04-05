/*******************************************************************************
 * Copyright (c) 2024 Sierra Wireless and others.
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
package org.eclipse.leshan.integration.tests.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.leshan.core.CertificateUsage.CA_CONSTRAINT;
import static org.eclipse.leshan.core.CertificateUsage.DOMAIN_ISSUER_CERTIFICATE;
import static org.eclipse.leshan.core.CertificateUsage.SERVICE_CERTIFICATE_CONSTRAINT;
import static org.eclipse.leshan.core.CertificateUsage.TRUST_ANCHOR_ASSERTION;
import static org.eclipse.leshan.integration.tests.util.Credentials.BAD_ENDPOINT;
import static org.eclipse.leshan.integration.tests.util.Credentials.clientPrivateKey;
import static org.eclipse.leshan.integration.tests.util.Credentials.clientPrivateKeyFromCert;
import static org.eclipse.leshan.integration.tests.util.Credentials.clientTrustStore;
import static org.eclipse.leshan.integration.tests.util.Credentials.clientX509CertNotTrusted;
import static org.eclipse.leshan.integration.tests.util.Credentials.clientX509CertSignedByRoot;
import static org.eclipse.leshan.integration.tests.util.Credentials.clientX509CertWithBadCN;
import static org.eclipse.leshan.integration.tests.util.Credentials.rootCAX509Cert;
import static org.eclipse.leshan.integration.tests.util.Credentials.serverPrivateKeyFromCert;
import static org.eclipse.leshan.integration.tests.util.Credentials.serverX509CertChainWithIntermediateCa;
import static org.eclipse.leshan.integration.tests.util.Credentials.serverX509CertSelfSigned;
import static org.eclipse.leshan.integration.tests.util.Credentials.serverX509CertSignedByRoot;
import static org.eclipse.leshan.integration.tests.util.Credentials.trustedCertificatesByServer;
import static org.eclipse.leshan.integration.tests.util.LeshanTestClientBuilder.givenClientUsing;
import static org.eclipse.leshan.integration.tests.util.assertion.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.security.PrivateKey;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import org.eclipse.leshan.core.endpoint.Protocol;
import org.eclipse.leshan.core.request.ReadRequest;
import org.eclipse.leshan.core.response.ReadResponse;
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
public class X509Test {

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
    public void registered_device_with_x509cert_to_server_with_x509cert_then_remove_security_info(
            Protocol givenProtocol, String givenClientEndpointProvider, String givenServerEndpointProvider)
            throws NonUniqueSecurityInfoException, CertificateEncodingException, InterruptedException {

        // Create X509 server & start it
        server = givenServer //
                .actingAsServerOnly()//
                .using(serverX509CertSignedByRoot, serverPrivateKeyFromCert)//
                .trusting(trustedCertificatesByServer).build();
        server.start();

        // Create X509 Client
        client = givenClient.connectingTo(server) //
                .using(clientX509CertSignedByRoot, clientPrivateKeyFromCert)//
                .trusting(serverX509CertSignedByRoot).build();

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
        Thread.sleep(100);
        if (givenProtocol.equals(Protocol.COAPS)) {
            // For DTLS, Client doesn't know that connection is removed at server side.
            // So request will first timeout.
            client.triggerRegistrationUpdate();
            client.waitForUpdateTimeoutTo(server);
        }

        // try to update
        client.triggerRegistrationUpdate();
        client.waitForUpdateFailureTo(server, 2, TimeUnit.SECONDS);

    }

    @TestAllTransportLayer
    public void registered_device_with_x509cert_to_server_with_x509cert(Protocol givenProtocol,
            String givenClientEndpointProvider, String givenServerEndpointProvider)
            throws NonUniqueSecurityInfoException, CertificateEncodingException, InterruptedException {

        // Create X509 server & start it
        server = givenServer //
                .actingAsServerOnly()//
                .using(serverX509CertSignedByRoot, serverPrivateKeyFromCert)//
                .trusting(trustedCertificatesByServer).build();
        server.start();

        // Create X509 Client
        client = givenClient.connectingTo(server) //
                .using(clientX509CertSignedByRoot, clientPrivateKeyFromCert)//
                .trusting(serverX509CertSignedByRoot).build();

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
                .trusting(trustedCertificatesByServer).build();
        server.start();

        // Create X509 Client
        client = givenClient.connectingTo(server) //
                .using(clientX509CertSignedByRoot, clientPrivateKeyFromCert)//
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
                .using(serverX509CertSignedByRoot, serverPrivateKeyFromCert)//
                .trusting(trustedCertificatesByServer).build();
        server.start();

        // Create X509 Client
        client = givenClient.connectingTo(server) //
                .using(clientX509CertSignedByRoot, clientPrivateKeyFromCert)//
                .trusting(serverX509CertSignedByRoot).build();

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
                .using(serverX509CertSignedByRoot, serverPrivateKeyFromCert)//
                .trusting(trustedCertificatesByServer).build();
        server.start();

        // Create X509 Client
        client = givenClient.connectingTo(server) //
                .named(BAD_ENDPOINT)//
                .using(clientX509CertWithBadCN, clientPrivateKeyFromCert)//
                .trusting(serverX509CertSignedByRoot).build();

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
                .using(serverX509CertSignedByRoot, serverPrivateKeyFromCert)//
                .trusting(trustedCertificatesByServer).build();
        server.start();

        // Create X509 Client
        // we use the RPK private key as bad key, this key will not be compatible with the client certificate
        PrivateKey badPrivateKey = clientPrivateKey;
        client = givenClient.connectingTo(server) //
                .using(clientX509CertSignedByRoot, badPrivateKey)//
                .trusting(serverX509CertSignedByRoot).build();

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
                .using(serverX509CertSignedByRoot, serverPrivateKeyFromCert)//
                .trusting(trustedCertificatesByServer).build();
        server.start();

        // Create X509 Client
        client = givenClient.connectingTo(server) //
                .using(clientX509CertNotTrusted, clientPrivateKeyFromCert)//
                .trusting(serverX509CertSignedByRoot).build();

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
                .using(serverX509CertSignedByRoot, serverPrivateKeyFromCert)//
                .trusting(trustedCertificatesByServer).build();
        server.start();

        // Create X509 Client
        client = givenClient.connectingTo(server) //
                .using(Credentials.clientX509CertSelfSigned, clientPrivateKeyFromCert)//
                .trusting(serverX509CertSignedByRoot).build();

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
    public void registered_device_with_x509cert_using_ca_constraint_with_direct_trust(Protocol givenProtocol,
            String givenClientEndpointProvider, String givenServerEndpointProvider)
            throws NonUniqueSecurityInfoException, CertificateEncodingException {
        server = givenServer //
                .actingAsServerOnly()//
                .using(serverPrivateKeyFromCert, serverX509CertChainWithIntermediateCa)//
                .trusting(trustedCertificatesByServer).build();
        server.start();

        client = givenClient.connectingTo(server) //
                .using(clientX509CertSignedByRoot, clientPrivateKeyFromCert)//
                .trusting(serverX509CertChainWithIntermediateCa[0], CA_CONSTRAINT, clientTrustStore).build();

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
    public void registered_device_with_x509cert_with_intermediate_ca_as_ca_contraint(Protocol givenProtocol,
            String givenClientEndpointProvider, String givenServerEndpointProvider)
            throws NonUniqueSecurityInfoException, CertificateEncodingException, InterruptedException {

        server = givenServer //
                .actingAsServerOnly()//
                .using(serverPrivateKeyFromCert, serverX509CertChainWithIntermediateCa)//
                .trusting(trustedCertificatesByServer).build();
        server.start();

        client = givenClient.connectingTo(server) //
                .using(clientX509CertSignedByRoot, clientPrivateKeyFromCert)//
                .trusting(serverX509CertChainWithIntermediateCa[1], CA_CONSTRAINT, clientTrustStore).build();

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
    public void registered_device_with_x509cert_using_ca_constraint_like_trust_anchor(Protocol givenProtocol,
            String givenClientEndpointProvider, String givenServerEndpointProvider)
            throws NonUniqueSecurityInfoException, CertificateEncodingException, InterruptedException {
        server = givenServer //
                .actingAsServerOnly()//
                .using(serverPrivateKeyFromCert, serverX509CertChainWithIntermediateCa)//
                .trusting(trustedCertificatesByServer).build();
        server.start();

        // create a not empty trustore which does not contains any certificate of server certchain.
        X509Certificate[] truststore = new X509Certificate[] { serverX509CertSelfSigned };
        // e.g. we use a selfsigned certificate not used in certchain of this test.

        client = givenClient.connectingTo(server) //
                .using(clientX509CertSignedByRoot, clientPrivateKeyFromCert)//
                .trusting(serverX509CertChainWithIntermediateCa[1], CA_CONSTRAINT, truststore).build();

        server.getSecurityStore().add(SecurityInfo.newX509CertInfo(client.getEndpointName()));

        assertThat(client).isNotRegisteredAt(server);
        client.start();
        assertThat(client).after(1, TimeUnit.SECONDS).isNotRegisteredAt(server);
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
     *
     * </pre>
     *
     */
    @TestAllTransportLayer
    public void registered_device_with_x509cert_with_root_as_ca_contraint(Protocol givenProtocol,
            String givenClientEndpointProvider, String givenServerEndpointProvider)
            throws NonUniqueSecurityInfoException, CertificateEncodingException, InterruptedException {
        server = givenServer //
                .actingAsServerOnly()//
                .using(serverPrivateKeyFromCert, serverX509CertChainWithIntermediateCa)//
                .trusting(trustedCertificatesByServer).build();
        server.start();

        client = givenClient.connectingTo(server) //
                .using(clientX509CertSignedByRoot, clientPrivateKeyFromCert)//
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
    public void registered_device_with_x509cert_with_server_self_signed_certificate_as_ca_contraint_which_is_not_in_certhchain(
            Protocol givenProtocol, String givenClientEndpointProvider, String givenServerEndpointProvider)
            throws NonUniqueSecurityInfoException, CertificateEncodingException {
        server = givenServer //
                .actingAsServerOnly()//
                .using(serverPrivateKeyFromCert, serverX509CertChainWithIntermediateCa)//
                .trusting(trustedCertificatesByServer).build();
        server.start();

        client = givenClient.connectingTo(server) //
                .using(clientX509CertSignedByRoot, clientPrivateKeyFromCert)//
                .trusting(serverX509CertSelfSigned, CA_CONSTRAINT, clientTrustStore).build();

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
     * - Server's TLS Server Certificate = self signed certificate
     * - Server accepts client
     * - Client Trust Store = root CA
     *
     * Expected outcome:
     * - Client denied the connection  (direct trust is not allowed with "CA constraint" usage)
     * </pre>
     */
    @TestAllTransportLayer
    public void registered_device_with_x509cert_with_server_self_signed_certificate_as_ca_contraint(
            Protocol givenProtocol, String givenClientEndpointProvider, String givenServerEndpointProvider)
            throws NonUniqueSecurityInfoException, CertificateEncodingException {
        server = givenServer //
                .actingAsServerOnly()//
                .using(serverPrivateKeyFromCert, serverX509CertSelfSigned)//
                .trusting(trustedCertificatesByServer).build();
        server.start();

        client = givenClient.connectingTo(server) //
                .using(clientX509CertSignedByRoot, clientPrivateKeyFromCert)//
                .trusting(serverX509CertSelfSigned, CA_CONSTRAINT, clientTrustStore).build();

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
    public void registered_device_with_x509cert_using_service_certificate_constraint(Protocol givenProtocol,
            String givenClientEndpointProvider, String givenServerEndpointProvider)
            throws NonUniqueSecurityInfoException, CertificateEncodingException, InterruptedException {
        server = givenServer //
                .actingAsServerOnly()//
                .using(serverPrivateKeyFromCert, serverX509CertChainWithIntermediateCa)//
                .trusting(trustedCertificatesByServer).build();
        server.start();

        client = givenClient.connectingTo(server) //
                .using(clientX509CertSignedByRoot, clientPrivateKeyFromCert)//
                .trusting(serverX509CertChainWithIntermediateCa[0], SERVICE_CERTIFICATE_CONSTRAINT, clientTrustStore)
                .build();

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
    public void registered_device_with_x509cert_using_root_ca_as_service_certificate_constraint(Protocol givenProtocol,
            String givenClientEndpointProvider, String givenServerEndpointProvider)
            throws NonUniqueSecurityInfoException, CertificateEncodingException {
        server = givenServer //
                .actingAsServerOnly()//
                .using(serverPrivateKeyFromCert, serverX509CertChainWithIntermediateCa)//
                .trusting(trustedCertificatesByServer).build();
        server.start();

        client = givenClient.connectingTo(server) //
                .using(clientX509CertSignedByRoot, clientPrivateKeyFromCert)//
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
    public void registered_device_with_x509cert_using_another_certificate_as_service_certificate_constraint(
            Protocol givenProtocol, String givenClientEndpointProvider, String givenServerEndpointProvider)
            throws NonUniqueSecurityInfoException, CertificateEncodingException {
        server = givenServer //
                .actingAsServerOnly()//
                .using(serverPrivateKeyFromCert, serverX509CertChainWithIntermediateCa)//
                .trusting(trustedCertificatesByServer).build();
        server.start();

        client = givenClient.connectingTo(server) //
                .using(clientX509CertSignedByRoot, clientPrivateKeyFromCert)//
                .trusting(serverX509CertSignedByRoot, SERVICE_CERTIFICATE_CONSTRAINT, clientTrustStore).build();

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
    public void registered_device_with_x509cert_using_server_self_signed_cert_as_service_certificate_constraint_which_is_not_in_certhchain(
            Protocol givenProtocol, String givenClientEndpointProvider, String givenServerEndpointProvider)
            throws NonUniqueSecurityInfoException, CertificateEncodingException {
        server = givenServer //
                .actingAsServerOnly()//
                .using(serverPrivateKeyFromCert, serverX509CertChainWithIntermediateCa)//
                .trusting(trustedCertificatesByServer).build();
        server.start();

        client = givenClient.connectingTo(server) //
                .using(clientX509CertSignedByRoot, clientPrivateKeyFromCert)//
                .trusting(serverX509CertSelfSigned, SERVICE_CERTIFICATE_CONSTRAINT, clientTrustStore).build();

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
     * - Server's TLS Server Certificate = self signed certificate
     * - Server accepts client
     * - Client Trust Store = root CA
     *
     * Expected outcome:
     * - Client denied the connection (self-signed is not PKIX chainable)
     * </pre>
     */
    @TestAllTransportLayer
    public void registered_device_with_x509cert_using_server_self_signed_cert_as_service_certificate_constraint(
            Protocol givenProtocol, String givenClientEndpointProvider, String givenServerEndpointProvider)
            throws NonUniqueSecurityInfoException, CertificateEncodingException {
        server = givenServer //
                .actingAsServerOnly()//
                .using(serverPrivateKeyFromCert, serverX509CertSelfSigned)//
                .trusting(trustedCertificatesByServer).build();
        server.start();

        client = givenClient.connectingTo(server) //
                .using(clientX509CertSignedByRoot, clientPrivateKeyFromCert)//
                .trusting(serverX509CertSelfSigned, SERVICE_CERTIFICATE_CONSTRAINT, clientTrustStore).build();

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
    public void registered_device_with_x509cert_using_service_certificate_constraint_with_missing_intermediate_certificate_in_chain(
            Protocol givenProtocol, String givenClientEndpointProvider, String givenServerEndpointProvider)
            throws NonUniqueSecurityInfoException, CertificateEncodingException {
        server = givenServer //
                .actingAsServerOnly()//
                .using(serverPrivateKeyFromCert, serverX509CertChainWithIntermediateCa[0])//
                .trusting(trustedCertificatesByServer).build();
        server.start();

        client = givenClient.connectingTo(server) //
                .using(clientX509CertSignedByRoot, clientPrivateKeyFromCert)//
                .trusting(serverX509CertChainWithIntermediateCa[0], SERVICE_CERTIFICATE_CONSTRAINT, clientTrustStore)
                .build();

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
    public void registered_device_with_x509cert_using_trust_anchor_assertion_with_direct_trust(Protocol givenProtocol,
            String givenClientEndpointProvider, String givenServerEndpointProvider)
            throws NonUniqueSecurityInfoException, CertificateEncodingException {
        server = givenServer //
                .actingAsServerOnly()//
                .using(serverPrivateKeyFromCert, serverX509CertChainWithIntermediateCa)//
                .trusting(trustedCertificatesByServer).build();
        server.start();

        client = givenClient.connectingTo(server) //
                .using(clientX509CertSignedByRoot, clientPrivateKeyFromCert)//
                .trusting(serverX509CertChainWithIntermediateCa[0], TRUST_ANCHOR_ASSERTION, clientTrustStore).build();

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
    public void registered_device_with_x509cert_with_intermediate_cert_as_trust_anchor_assertion(Protocol givenProtocol,
            String givenClientEndpointProvider, String givenServerEndpointProvider)
            throws NonUniqueSecurityInfoException, CertificateEncodingException, InterruptedException {
        server = givenServer //
                .actingAsServerOnly()//
                .using(serverPrivateKeyFromCert, serverX509CertChainWithIntermediateCa)//
                .trusting(trustedCertificatesByServer).build();
        server.start();

        client = givenClient.connectingTo(server) //
                .using(clientX509CertSignedByRoot, clientPrivateKeyFromCert)//
                .trusting(serverX509CertChainWithIntermediateCa[1], TRUST_ANCHOR_ASSERTION).build();

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
    public void registered_device_with_x509cert_with_root_ca_cert_as_trust_anchor_assertion(Protocol givenProtocol,
            String givenClientEndpointProvider, String givenServerEndpointProvider)
            throws NonUniqueSecurityInfoException, CertificateEncodingException, InterruptedException {
        server = givenServer //
                .actingAsServerOnly()//
                .using(serverPrivateKeyFromCert, serverX509CertChainWithIntermediateCa)//
                .trusting(trustedCertificatesByServer).build();
        server.start();

        client = givenClient.connectingTo(server) //
                .using(clientX509CertSignedByRoot, clientPrivateKeyFromCert)//
                .trusting(rootCAX509Cert, TRUST_ANCHOR_ASSERTION).build();

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
    public void registered_device_with_x509cert_with_trust_anchor_assertion_which_is_not_in_certchain(
            Protocol givenProtocol, String givenClientEndpointProvider, String givenServerEndpointProvider)
            throws NonUniqueSecurityInfoException, CertificateEncodingException {
        server = givenServer //
                .actingAsServerOnly()//
                .using(serverPrivateKeyFromCert, serverX509CertChainWithIntermediateCa)//
                .trusting(trustedCertificatesByServer).build();
        server.start();

        client = givenClient.connectingTo(server) //
                .using(clientX509CertSignedByRoot, clientPrivateKeyFromCert)//
                .trusting(serverX509CertSignedByRoot, TRUST_ANCHOR_ASSERTION, clientTrustStore).build();

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
    public void registered_device_with_x509cert_with_server_self_signed__cert_as_trust_anchor_assertion(
            Protocol givenProtocol, String givenClientEndpointProvider, String givenServerEndpointProvider)
            throws NonUniqueSecurityInfoException, CertificateEncodingException {
        server = givenServer //
                .actingAsServerOnly()//
                .using(serverPrivateKeyFromCert, serverX509CertChainWithIntermediateCa)//
                .trusting(trustedCertificatesByServer).build();
        server.start();

        client = givenClient.connectingTo(server) //
                .using(clientX509CertSignedByRoot, clientPrivateKeyFromCert)//
                .trusting(serverX509CertSelfSigned, TRUST_ANCHOR_ASSERTION, clientTrustStore).build();

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
    public void registered_device_with_x509cert_using_direct_trust_with_self_signed_certificate_as_trust_anchor_assertion(
            Protocol givenProtocol, String givenClientEndpointProvider, String givenServerEndpointProvider)
            throws NonUniqueSecurityInfoException, CertificateEncodingException {
        server = givenServer //
                .actingAsServerOnly()//
                .using(serverPrivateKeyFromCert, serverX509CertSelfSigned)//
                .trusting(trustedCertificatesByServer).build();
        server.start();

        client = givenClient.connectingTo(server) //
                .using(clientX509CertSignedByRoot, clientPrivateKeyFromCert)//
                .trusting(serverX509CertSelfSigned, TRUST_ANCHOR_ASSERTION, clientTrustStore).build();

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
    public void registered_device_with_x509cert_using_direct_trust_with_certificate_signed_by_ca_as_trust_anchor_assertion(
            Protocol givenProtocol, String givenClientEndpointProvider, String givenServerEndpointProvider)
            throws NonUniqueSecurityInfoException, CertificateEncodingException {
        server = givenServer //
                .actingAsServerOnly()//
                .using(serverPrivateKeyFromCert, serverX509CertChainWithIntermediateCa[0])//
                .trusting(trustedCertificatesByServer).build();
        server.start();

        client = givenClient.connectingTo(server) //
                .using(clientX509CertSignedByRoot, clientPrivateKeyFromCert)//
                .trusting(serverX509CertChainWithIntermediateCa[0], TRUST_ANCHOR_ASSERTION, clientTrustStore).build();

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
    public void registered_device_with_x509cert_with_server_certificate_signed_by_ca_as_domain_issuer_certificate(
            Protocol givenProtocol, String givenClientEndpointProvider, String givenServerEndpointProvider)
            throws NonUniqueSecurityInfoException, CertificateEncodingException, InterruptedException {
        server = givenServer //
                .actingAsServerOnly()//
                .using(serverPrivateKeyFromCert, serverX509CertChainWithIntermediateCa)//
                .trusting(trustedCertificatesByServer).build();
        server.start();

        client = givenClient.connectingTo(server) //
                .using(clientX509CertSignedByRoot, clientPrivateKeyFromCert)//
                .trusting(serverX509CertChainWithIntermediateCa[0], DOMAIN_ISSUER_CERTIFICATE).build();

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
    public void registered_device_with_x509cert_using_no_end_entity_certificate_as_domain_issuer_certificate(
            Protocol givenProtocol, String givenClientEndpointProvider, String givenServerEndpointProvider)
            throws NonUniqueSecurityInfoException, CertificateEncodingException {
        server = givenServer //
                .actingAsServerOnly()//
                .using(serverPrivateKeyFromCert, serverX509CertChainWithIntermediateCa)//
                .trusting(trustedCertificatesByServer).build();
        server.start();

        client = givenClient.connectingTo(server) //
                .using(clientX509CertSignedByRoot, clientPrivateKeyFromCert)//
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
    public void registered_device_with_x509cert_using_another_end_entity_certificate_as_domain_issuer_certificate(
            Protocol givenProtocol, String givenClientEndpointProvider, String givenServerEndpointProvider)
            throws NonUniqueSecurityInfoException, CertificateEncodingException {
        server = givenServer //
                .actingAsServerOnly()//
                .using(serverPrivateKeyFromCert, serverX509CertChainWithIntermediateCa)//
                .trusting(trustedCertificatesByServer).build();
        server.start();

        client = givenClient.connectingTo(server) //
                .using(clientX509CertSignedByRoot, clientPrivateKeyFromCert)//
                .trusting(serverX509CertSignedByRoot, DOMAIN_ISSUER_CERTIFICATE, clientTrustStore).build();

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
    public void registered_device_with_x509cert_using_unexpected_self_signed_certificate_as_domain_issuer_certificate(
            Protocol givenProtocol, String givenClientEndpointProvider, String givenServerEndpointProvider)
            throws NonUniqueSecurityInfoException, CertificateEncodingException {
        server = givenServer //
                .actingAsServerOnly()//
                .using(serverPrivateKeyFromCert, serverX509CertChainWithIntermediateCa)//
                .trusting(trustedCertificatesByServer).build();
        server.start();

        client = givenClient.connectingTo(server) //
                .using(clientX509CertSignedByRoot, clientPrivateKeyFromCert)//
                .trusting(serverX509CertSelfSigned, DOMAIN_ISSUER_CERTIFICATE, clientTrustStore).build();

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
    public void registered_device_with_x509cert_using_expected_self_signed_certificate_as_domain_issuer_certificate(
            Protocol givenProtocol, String givenClientEndpointProvider, String givenServerEndpointProvider)
            throws NonUniqueSecurityInfoException, CertificateEncodingException, InterruptedException {
        server = givenServer //
                .actingAsServerOnly()//
                .using(serverPrivateKeyFromCert, serverX509CertSelfSigned)//
                .trusting(trustedCertificatesByServer).build();
        server.start();

        client = givenClient.connectingTo(server) //
                .using(clientX509CertSignedByRoot, clientPrivateKeyFromCert)//
                .trusting(serverX509CertSelfSigned, DOMAIN_ISSUER_CERTIFICATE).build();

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
    public void registered_device_with_x509cert_using_server_certificate_signed_by_ca_as_domain_issuer_certificate(
            Protocol givenProtocol, String givenClientEndpointProvider, String givenServerEndpointProvider)
            throws NonUniqueSecurityInfoException, CertificateEncodingException, InterruptedException {
        server = givenServer //
                .actingAsServerOnly()//
                .using(serverPrivateKeyFromCert, serverX509CertChainWithIntermediateCa[0])//
                .trusting(trustedCertificatesByServer).build();
        server.start();

        client = givenClient.connectingTo(server) //
                .using(clientX509CertSignedByRoot, clientPrivateKeyFromCert)//
                .trusting(serverX509CertChainWithIntermediateCa[0], DOMAIN_ISSUER_CERTIFICATE).build();

        server.getSecurityStore().add(SecurityInfo.newX509CertInfo(client.getEndpointName()));

        assertThat(client).isNotRegisteredAt(server);
        client.start();
        server.waitForNewRegistrationOf(client);
        assertThat(client).isRegisteredAt(server);
        Registration registration = server.getRegistrationFor(client);

        ReadResponse response = server.send(registration, new ReadRequest(3, 0, 1), 500);
        assertThat(response.isSuccess()).isTrue();
    }
}
