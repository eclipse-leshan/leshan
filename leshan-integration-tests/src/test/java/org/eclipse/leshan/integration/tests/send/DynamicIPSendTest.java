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
package org.eclipse.leshan.integration.tests.send;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.leshan.core.ResponseCode.BAD_REQUEST;
import static org.eclipse.leshan.integration.tests.util.Credentials.GOOD_PSK_ID;
import static org.eclipse.leshan.integration.tests.util.Credentials.GOOD_PSK_KEY;
import static org.eclipse.leshan.integration.tests.util.Credentials.clientPrivateKey;
import static org.eclipse.leshan.integration.tests.util.Credentials.clientPrivateKeyFromCert;
import static org.eclipse.leshan.integration.tests.util.Credentials.clientPublicKey;
import static org.eclipse.leshan.integration.tests.util.Credentials.clientX509Cert;
import static org.eclipse.leshan.integration.tests.util.Credentials.serverPrivateKey;
import static org.eclipse.leshan.integration.tests.util.Credentials.serverPrivateKeyFromCert;
import static org.eclipse.leshan.integration.tests.util.Credentials.serverPublicKey;
import static org.eclipse.leshan.integration.tests.util.Credentials.serverX509Cert;
import static org.eclipse.leshan.integration.tests.util.Credentials.trustedCertificates;
import static org.eclipse.leshan.integration.tests.util.LeshanProxyBuilder.givenReverseProxyFor;
import static org.eclipse.leshan.integration.tests.util.LeshanTestClientBuilder.givenClientUsing;
import static org.eclipse.leshan.integration.tests.util.assertion.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Stream;

import org.eclipse.leshan.client.servers.LwM2mServer;
import org.eclipse.leshan.core.endpoint.Protocol;
import org.eclipse.leshan.core.node.LwM2mNode;
import org.eclipse.leshan.core.node.LwM2mPath;
import org.eclipse.leshan.core.node.LwM2mResource;
import org.eclipse.leshan.core.node.TimestampedLwM2mNodes;
import org.eclipse.leshan.core.request.ContentFormat;
import org.eclipse.leshan.core.response.SendResponse;
import org.eclipse.leshan.integration.tests.util.LeshanTestClient;
import org.eclipse.leshan.integration.tests.util.LeshanTestServer;
import org.eclipse.leshan.integration.tests.util.LeshanTestServerBuilder;
import org.eclipse.leshan.integration.tests.util.ReverseProxy;
import org.eclipse.leshan.server.registration.Registration;
import org.eclipse.leshan.server.security.InMemorySecurityStore;
import org.eclipse.leshan.server.security.NonUniqueSecurityInfoException;
import org.eclipse.leshan.server.security.SecurityInfo;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class DynamicIPSendTest {

    /*---------------------------------/
     *  Parameterized Tests
     * -------------------------------*/
    @ParameterizedTest(name = "{0} - Client using {1} - Server using {2}")
    @MethodSource("noTlsTransports")
    @Retention(RetentionPolicy.RUNTIME)
    private @interface TestNoTlsTransport {
    }

    static Stream<Arguments> noTlsTransports() {
        return Stream.of(//
                // ProtocolUsed - Client Endpoint Provider - Server Endpoint Provider
                arguments(Protocol.COAP, "Californium", "Californium"));
    }

    @ParameterizedTest(name = "{0} - Client using {1} - Server using {2}")
    @MethodSource("tlsTransports")
    @Retention(RetentionPolicy.RUNTIME)
    private @interface TestTlsTransport {
    }

    static Stream<Arguments> tlsTransports() {
        return Stream.of(//
                // ProtocolUsed - Client Endpoint Provider - Server Endpoint Provider
                arguments(Protocol.COAPS, "Californium", "Californium"));
    }

    /*---------------------------------/
     *  Set-up and Tear-down Tests
     * -------------------------------*/
    LeshanTestServer server;
    LeshanTestClient client;
    ReverseProxy proxy;

    protected LeshanTestServerBuilder givenServerUsing(Protocol givenProtocol) {
        return new LeshanTestServerBuilder(givenProtocol).with(new InMemorySecurityStore());
    }

    @AfterEach
    public void stop() throws InterruptedException {
        if (client != null)
            client.destroy(false);
        if (proxy != null)
            proxy.stop();
        if (server != null)
            server.destroy();
    }

    /*---------------------------------/
     *  Tests
     * -------------------------------*/
    @TestNoTlsTransport
    public void can_not_send_if_client_ip_changes(Protocol givenProtocol, String givenClientEndpointProvider,
            String givenServerEndpointProvider) throws InterruptedException, TimeoutException {

        // Start Client and Server
        server = givenServerUsing(givenProtocol).with(givenServerEndpointProvider).build();
        server.start();

        proxy = givenReverseProxyFor(server, givenProtocol);
        proxy.start();

        client = givenClientUsing(givenProtocol).with(givenClientEndpointProvider).connectingTo(server).behind(proxy)
                .build();
        client.start();

        server.waitForNewRegistrationOf(client);
        client.waitForRegistrationTo(server);

        // Simulate new IP for client
        proxy.changeServerSideProxyAddress();

        // Send Data
        LwM2mServer registeredServer = client.getRegisteredServers().values().iterator().next();
        SendResponse response = client.getSendService().sendData(registeredServer, ContentFormat.SENML_JSON,
                Arrays.asList("/3/0/1", "/3/0/2"), 1000);

        // it should failed !
        assertThat(response).hasCode(BAD_REQUEST);
    }

    // TODO OSCORE implement a test with OSCORE
    @Disabled
    @TestTlsTransport
    public void can_send_if_client_ip_changes_using_oscore(Protocol givenProtocol, String givenClientEndpointProvider,
            String givenServerEndpointProvider)
            throws InterruptedException, TimeoutException, NonUniqueSecurityInfoException {
    }

    @TestTlsTransport
    public void can_send_if_client_ip_changes_using_psk(Protocol givenProtocol, String givenClientEndpointProvider,
            String givenServerEndpointProvider)
            throws InterruptedException, TimeoutException, NonUniqueSecurityInfoException {

        // Start Client and Server
        server = givenServerUsing(givenProtocol).with(givenServerEndpointProvider).build();
        server.start();

        proxy = givenReverseProxyFor(server, givenProtocol);
        proxy.start();

        client = givenClientUsing(givenProtocol).with(givenClientEndpointProvider).connectingTo(server).behind(proxy)
                .usingPsk(GOOD_PSK_ID, GOOD_PSK_KEY).build();

        server.getSecurityStore()
                .add(SecurityInfo.newPreSharedKeyInfo(client.getEndpointName(), GOOD_PSK_ID, GOOD_PSK_KEY));

        client.start();
        server.waitForNewRegistrationOf(client);
        client.waitForRegistrationTo(server);

        // Send Data should works
        Registration registrationBeforeSend = server.getRegistrationFor(client);
        assertSuccessfulSendAfterAddressChanged();

        // check that client registration is not updated.
        Registration registrationAfterObserve = server.getRegistrationFor(client);
        assertThat(registrationAfterObserve).isEqualTo(registrationBeforeSend);
    }

    @TestTlsTransport
    public void update_registration_on_send_using_psk(Protocol givenProtocol, String givenClientEndpointProvider,
            String givenServerEndpointProvider)
            throws InterruptedException, TimeoutException, NonUniqueSecurityInfoException {

        // Start Client and Server
        server = givenServerUsing(givenProtocol).with(givenServerEndpointProvider).withUpdateOnSendOperation().build();
        server.start();

        proxy = givenReverseProxyFor(server, givenProtocol);
        proxy.start();

        client = givenClientUsing(givenProtocol).with(givenClientEndpointProvider).connectingTo(server).behind(proxy)
                .usingPsk(GOOD_PSK_ID, GOOD_PSK_KEY).build();

        server.getSecurityStore()
                .add(SecurityInfo.newPreSharedKeyInfo(client.getEndpointName(), GOOD_PSK_ID, GOOD_PSK_KEY));

        client.start();
        server.waitForNewRegistrationOf(client);
        client.waitForRegistrationTo(server);

        // Send Data should works
        Registration registrationBeforeSend = server.getRegistrationFor(client);
        assertSuccessfulSendAfterAddressChanged();

        // check that client registration is updated.
        Registration registrationAfterSend = server.getRegistrationFor(client);
        assertThat(registrationAfterSend.getSocketAddress()).isNotEqualTo(registrationBeforeSend.getSocketAddress());
    }

    @TestTlsTransport
    public void can_send_if_client_ip_changes_using_rpk(Protocol givenProtocol, String givenClientEndpointProvider,
            String givenServerEndpointProvider)
            throws InterruptedException, TimeoutException, NonUniqueSecurityInfoException {

        // Start Client and Server
        server = givenServerUsing(givenProtocol).with(givenServerEndpointProvider)
                .using(serverPublicKey, serverPrivateKey).build();
        server.start();

        proxy = givenReverseProxyFor(server, givenProtocol);
        proxy.start();

        client = givenClientUsing(givenProtocol).with(givenClientEndpointProvider).connectingTo(server).behind(proxy)
                .using(clientPublicKey, clientPrivateKey)//
                .trusting(serverPublicKey).build();

        server.getSecurityStore().add(SecurityInfo.newRawPublicKeyInfo(client.getEndpointName(), clientPublicKey));

        client.start();
        server.waitForNewRegistrationOf(client);
        client.waitForRegistrationTo(server);

        // Send Data should works
        assertSuccessfulSendAfterAddressChanged();
    }

    @TestTlsTransport
    public void can_send_if_client_ip_changes_using_x509(Protocol givenProtocol, String givenClientEndpointProvider,
            String givenServerEndpointProvider)
            throws InterruptedException, TimeoutException, NonUniqueSecurityInfoException {

        // Start Client and Server
        server = givenServerUsing(givenProtocol).with(givenServerEndpointProvider)//
                .actingAsServerOnly()//
                .using(serverX509Cert, serverPrivateKeyFromCert)//
                .trusting(trustedCertificates).build();
        server.start();

        proxy = givenReverseProxyFor(server, givenProtocol);
        proxy.start();

        client = givenClientUsing(givenProtocol).with(givenClientEndpointProvider).connectingTo(server).behind(proxy)
                .using(clientX509Cert, clientPrivateKeyFromCert)//
                .trusting(serverX509Cert).build();

        server.getSecurityStore().add(SecurityInfo.newX509CertInfo(client.getEndpointName()));

        client.start();
        server.waitForNewRegistrationOf(client);
        client.waitForRegistrationTo(server);

        // Send Data should works
        assertSuccessfulSendAfterAddressChanged();
    }

    private void assertSuccessfulSendAfterAddressChanged() throws InterruptedException {
        // Simulate new IP for client
        proxy.changeServerSideProxyAddress();

        // Force new Handshake
        LwM2mServer registeredServer = client.getRegisteredServers().values().iterator().next();
        client.clearSecurityContextFor(registeredServer);

        // Send Data
        SendResponse response = client.getSendService().sendData(registeredServer, ContentFormat.SENML_JSON,
                Arrays.asList("/3/0/1", "/3/0/2"), 1000);
        assertThat(response.isSuccess()).isTrue();

        // wait for data and check result
        TimestampedLwM2mNodes data = server.waitForData(client.getEndpointName(), 1, TimeUnit.SECONDS);
        Map<LwM2mPath, LwM2mNode> nodes = data.getNodes();
        LwM2mResource modelnumber = (LwM2mResource) nodes.get(new LwM2mPath("/3/0/1"));
        assertThat(modelnumber.getId()).isEqualTo(1);
        assertThat(modelnumber.getValue()).isEqualTo("IT-TEST-123");

        LwM2mResource serialnumber = (LwM2mResource) nodes.get(new LwM2mPath("/3/0/2"));
        assertThat(serialnumber.getId()).isEqualTo(2);
        assertThat(serialnumber.getValue()).isEqualTo("12345");
    }
}
