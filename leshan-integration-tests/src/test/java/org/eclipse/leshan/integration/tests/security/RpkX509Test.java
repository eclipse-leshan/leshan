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
import static org.eclipse.leshan.integration.tests.util.Credentials.clientPrivateKey;
import static org.eclipse.leshan.integration.tests.util.Credentials.clientPrivateKeyFromCert;
import static org.eclipse.leshan.integration.tests.util.Credentials.clientPublicKey;
import static org.eclipse.leshan.integration.tests.util.Credentials.clientX509CertSignedByRoot;
import static org.eclipse.leshan.integration.tests.util.Credentials.serverPrivateKeyFromCert;
import static org.eclipse.leshan.integration.tests.util.Credentials.serverX509CertSignedByRoot;
import static org.eclipse.leshan.integration.tests.util.Credentials.trustedCertificatesByServer;
import static org.eclipse.leshan.integration.tests.util.LeshanTestClientBuilder.givenClientUsing;
import static org.eclipse.leshan.integration.tests.util.assertion.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.security.cert.CertificateEncodingException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import org.eclipse.leshan.core.endpoint.Protocol;
import org.eclipse.leshan.core.request.ReadRequest;
import org.eclipse.leshan.core.response.ReadResponse;
import org.eclipse.leshan.integration.tests.util.LeshanTestClient;
import org.eclipse.leshan.integration.tests.util.LeshanTestClientBuilder;
import org.eclipse.leshan.integration.tests.util.LeshanTestServer;
import org.eclipse.leshan.integration.tests.util.LeshanTestServerBuilder;
import org.eclipse.leshan.integration.tests.util.junit5.extensions.BeforeEachParameterizedResolver;
import org.eclipse.leshan.server.registration.Registration;
import org.eclipse.leshan.servers.security.InMemorySecurityStore;
import org.eclipse.leshan.servers.security.NonUniqueSecurityInfoException;
import org.eclipse.leshan.servers.security.SecurityInfo;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

@ExtendWith(BeforeEachParameterizedResolver.class)
public class RpkX509Test {
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
    public void registered_device_with_x509cert_to_server_with_rpk(Protocol givenProtocol,
            String givenClientEndpointProvider, String givenServerEndpointProvider)
            throws NonUniqueSecurityInfoException, CertificateEncodingException {
        server = givenServer.using(serverX509CertSignedByRoot.getPublicKey(), serverPrivateKeyFromCert).build();
        server.start();

        client = givenClient.connectingTo(server) //
                .using(clientX509CertSignedByRoot, clientPrivateKeyFromCert)//
                .trusting(serverX509CertSignedByRoot).build();

        server.getSecurityStore().add(
                SecurityInfo.newRawPublicKeyInfo(client.getEndpointName(), clientX509CertSignedByRoot.getPublicKey()));

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
                .using(serverX509CertSignedByRoot, serverPrivateKeyFromCert)//
                .trusting(trustedCertificatesByServer).build();
        server.start();

        client = givenClient.connectingTo(server) //
                .using(clientPublicKey, clientPrivateKey)//
                .trusting(serverX509CertSignedByRoot.getPublicKey()).build();

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
