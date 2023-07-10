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

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.leshan.core.CertificateUsage.DOMAIN_ISSUER_CERTIFICATE;
import static org.eclipse.leshan.core.CertificateUsage.SERVICE_CERTIFICATE_CONSTRAINT;
import static org.eclipse.leshan.integration.tests.util.Credentials.anotherServerPrivateKey;
import static org.eclipse.leshan.integration.tests.util.Credentials.anotherServerPublicKey;
import static org.eclipse.leshan.integration.tests.util.Credentials.clientPrivateKey;
import static org.eclipse.leshan.integration.tests.util.Credentials.clientPrivateKeyFromCert;
import static org.eclipse.leshan.integration.tests.util.Credentials.clientPublicKey;
import static org.eclipse.leshan.integration.tests.util.Credentials.clientTrustStore;
import static org.eclipse.leshan.integration.tests.util.Credentials.clientX509CertSignedByRoot;
import static org.eclipse.leshan.integration.tests.util.Credentials.rootCAX509Cert;
import static org.eclipse.leshan.integration.tests.util.Credentials.serverPrivateKey;
import static org.eclipse.leshan.integration.tests.util.Credentials.serverPrivateKeyFromCert;
import static org.eclipse.leshan.integration.tests.util.Credentials.serverPublicKey;
import static org.eclipse.leshan.integration.tests.util.Credentials.serverX509CertChainWithIntermediateCa;
import static org.eclipse.leshan.integration.tests.util.Credentials.serverX509CertSignedByRoot;
import static org.eclipse.leshan.integration.tests.util.Credentials.trustedCertificatesByServer;
import static org.eclipse.leshan.integration.tests.util.Credentials.virtualHostX509CertChainWithIntermediateCa;
import static org.eclipse.leshan.integration.tests.util.Credentials.virtualHostX509CertSignedByRoot;
import static org.eclipse.leshan.integration.tests.util.assertion.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.security.cert.CertificateEncodingException;
import java.util.stream.Stream;

import org.eclipse.leshan.core.CertificateUsage;
import org.eclipse.leshan.core.endpoint.Protocol;
import org.eclipse.leshan.core.request.ReadRequest;
import org.eclipse.leshan.core.response.ReadResponse;
import org.eclipse.leshan.integration.tests.util.LeshanTestClient;
import org.eclipse.leshan.integration.tests.util.LeshanTestClientBuilder;
import org.eclipse.leshan.integration.tests.util.LeshanTestServer;
import org.eclipse.leshan.integration.tests.util.LeshanTestServerBuilder;
import org.eclipse.leshan.server.registration.Registration;
import org.eclipse.leshan.server.security.InMemorySecurityStore;
import org.eclipse.leshan.server.security.NonUniqueSecurityInfoException;
import org.eclipse.leshan.server.security.SecurityInfo;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

public class SniTest {

    /*---------------------------------/
     *  Parameterized Tests
     * -------------------------------*/
    @ParameterizedTest(name = "{0} - Client using {1} - Server using Californium")
    @MethodSource("transports")
    @Retention(RetentionPolicy.RUNTIME)
    private @interface TestAllTransportLayer {
    }

    static Stream<org.junit.jupiter.params.provider.Arguments> transports() {
        return Stream.of(//
                // ProtocolUsed - Client Endpoint Provider
                arguments(Protocol.COAPS, "Californium"));
    }

    /*---------------------------------/
     *  Set-up and Tear-down Tests
     * -------------------------------*/
    LeshanTestServer server;
    LeshanTestClient client;

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

    protected LeshanTestClientBuilder givenClientUsing(Protocol givenProtocol) {
        return new LeshanTestClientBuilder(givenProtocol);
    }

    /*---------------------------------/
     *  Tests
     * -------------------------------*/
    @TestAllTransportLayer
    public void registered_device_with_rpk(Protocol givenProtocol, String givenClientEndpointProvider)
            throws NonUniqueSecurityInfoException, CertificateEncodingException, InterruptedException {

        // Create server & start it
        server = givenServerUsing(givenProtocol).with("Californium") //
                .actingAsServerOnly()//
                .usingSni() //
                .using("localhost", serverPublicKey, serverPrivateKey)//
                .using("virtualhost.org", anotherServerPublicKey, anotherServerPrivateKey)//
                .trusting(trustedCertificatesByServer).build();
        server.start();

        // Create Client
        client = givenClientUsing(givenProtocol).with(givenClientEndpointProvider) //
                .connectingTo(server) //
                .usingSniVirtualHost("virtualhost.org") //
                .using(clientPublicKey, clientPrivateKey)//
                .trusting(anotherServerPublicKey).build();

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
    public void registered_device_with_x509_using_domain_issuer_certificate_usage(Protocol givenProtocol,
            String givenClientEndpointProvider)
            throws NonUniqueSecurityInfoException, CertificateEncodingException, InterruptedException {

        // Create server & start it
        server = givenServerUsing(givenProtocol).with("Californium") //
                .actingAsServerOnly()//
                .usingSni() //
                .using("localhost", serverPrivateKeyFromCert, serverX509CertSignedByRoot) //
                .using("virtualhost-with-different-cn.org", serverPrivateKeyFromCert, virtualHostX509CertSignedByRoot)//
                .trusting(trustedCertificatesByServer).build();
        server.start();

        // Create Client
        client = givenClientUsing(givenProtocol).with(givenClientEndpointProvider) //
                .connectingTo(server) //
                .usingSniVirtualHost("virtualhost-with-different-cn.org") // because domain issuer certificate usage
                                                                          // don't not check subject/CN value.
                .using(clientX509CertSignedByRoot, clientPrivateKeyFromCert)//
                .trusting(virtualHostX509CertSignedByRoot, DOMAIN_ISSUER_CERTIFICATE).build();

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
    public void registered_device_with_x509_using_trust_anchor_assertion_certificate_usage(Protocol givenProtocol,
            String givenClientEndpointProvider)
            throws NonUniqueSecurityInfoException, CertificateEncodingException, InterruptedException {

        // Create server & start it
        server = givenServerUsing(givenProtocol).with("Californium") //
                .actingAsServerOnly()//
                .usingSni() //
                .using("localhost", serverPrivateKeyFromCert, serverX509CertSignedByRoot) //
                .using("virtualhost.org", serverPrivateKeyFromCert, virtualHostX509CertSignedByRoot)//
                .trusting(trustedCertificatesByServer).build();
        server.start();

        // Create Client
        client = givenClientUsing(givenProtocol).with(givenClientEndpointProvider) //
                .connectingTo(server) //
                .usingSniVirtualHost("virtualhost.org") //
                .using(clientX509CertSignedByRoot, clientPrivateKeyFromCert)//
                .trusting(rootCAX509Cert, CertificateUsage.TRUST_ANCHOR_ASSERTION).build();

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
    public void registered_device_with_x509_using_service_certificate_constraint_certificate_usage(
            Protocol givenProtocol, String givenClientEndpointProvider)
            throws NonUniqueSecurityInfoException, CertificateEncodingException, InterruptedException {

        // Create server & start it
        server = givenServerUsing(givenProtocol).with("Californium") //
                .actingAsServerOnly()//
                .usingSni() //
                .using("localhost", serverPrivateKeyFromCert, serverX509CertSignedByRoot) //
                .using("virtualhost.org", serverPrivateKeyFromCert, virtualHostX509CertSignedByRoot)//
                .trusting(trustedCertificatesByServer).build();
        server.start();

        // Create Client
        client = givenClientUsing(givenProtocol).with(givenClientEndpointProvider) //
                .connectingTo(server) //
                .usingSniVirtualHost("virtualhost.org") //
                .using(clientX509CertSignedByRoot, clientPrivateKeyFromCert)//
                .trusting(virtualHostX509CertSignedByRoot, SERVICE_CERTIFICATE_CONSTRAINT, clientTrustStore).build();

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
    public void registered_device_with_x509_using_ca_constraint_certificate_usage(Protocol givenProtocol,
            String givenClientEndpointProvider)
            throws NonUniqueSecurityInfoException, CertificateEncodingException, InterruptedException {

        // Create server & start it
        server = givenServerUsing(givenProtocol).with("Californium") //
                .actingAsServerOnly()//
                .usingSni() //
                .using("localhost", serverPrivateKeyFromCert, serverX509CertChainWithIntermediateCa) //
                .using("virtualhost.org", serverPrivateKeyFromCert, virtualHostX509CertChainWithIntermediateCa)//
                .trusting(trustedCertificatesByServer).build();
        server.start();

        // Create Client
        client = givenClientUsing(givenProtocol).with(givenClientEndpointProvider) //
                .connectingTo(server) //
                .usingSniVirtualHost("virtualhost.org") //
                .using(clientX509CertSignedByRoot, clientPrivateKeyFromCert)//
                .trusting(virtualHostX509CertChainWithIntermediateCa[1], CertificateUsage.CA_CONSTRAINT,
                        clientTrustStore)
                .build();

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
}
