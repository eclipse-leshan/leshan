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
package org.eclipse.leshan.integration.tests.observe;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.leshan.core.ResponseCode.CHANGED;
import static org.eclipse.leshan.core.ResponseCode.CONTENT;
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
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Stream;

import org.eclipse.leshan.client.servers.LwM2mServer;
import org.eclipse.leshan.core.endpoint.Protocol;
import org.eclipse.leshan.core.node.LwM2mSingleResource;
import org.eclipse.leshan.core.observation.Observation;
import org.eclipse.leshan.core.observation.SingleObservation;
import org.eclipse.leshan.core.request.ObserveRequest;
import org.eclipse.leshan.core.request.WriteRequest;
import org.eclipse.leshan.core.response.ObserveResponse;
import org.eclipse.leshan.core.response.WriteResponse;
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

public class DynamicIPObserveTest {

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
                arguments(Protocol.COAP, "Californium", "Californium"),
                arguments(Protocol.COAP, "Californium", "java-coap"));
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
    public void can_not_send_notification_if_client_ip_changes(Protocol givenProtocol,
            String givenClientEndpointProvider, String givenServerEndpointProvider)
            throws InterruptedException, TimeoutException {

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
        Registration currentRegistration = server.getRegistrationFor(client);

        // observe device timezone
        ObserveResponse observeResponse = server.send(currentRegistration, new ObserveRequest(3, 0, 15));
        assertThat(observeResponse) //
                .hasCode(CONTENT) //
                .hasValidUnderlyingResponseFor(givenServerEndpointProvider);

        // an observation response should have been sent
        SingleObservation observation = observeResponse.getObservation();
        server.waitForNewObservation(observation);
        assertThat(observation.getPath()).asString().isEqualTo("/3/0/15");
        assertThat(observation.getRegistrationId()).isEqualTo(currentRegistration.getId());
        Set<Observation> observations = server.getObservationService().getObservations(currentRegistration);
        assertThat(observations).containsExactly(observation);

        // Simulate new IP for client
        proxy.changeServerSideProxyAddress();

        // write device timezone
        WriteResponse writeResponse = client.getObjectTree().getObjectEnabler(3).write(LwM2mServer.SYSTEM,
                new WriteRequest(3, 0, 15, "Europe/Paris"));
        assertThat(writeResponse).hasCode(CHANGED);

        // notification should be ignored !
        server.ensureNoNotification(observation, 1, TimeUnit.SECONDS);
    }

    // TODO OSCORE implement a test with OSCORE
    @Disabled
    @TestTlsTransport
    public void can_send_notification_if_ip_changes_using_oscore(Protocol givenProtocol,
            String givenClientEndpointProvider, String givenServerEndpointProvider)
            throws InterruptedException, TimeoutException, NonUniqueSecurityInfoException {
    }

    @TestTlsTransport
    public void can_send_notification_if_ip_changes_using_psk(Protocol givenProtocol,
            String givenClientEndpointProvider, String givenServerEndpointProvider)
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
        Registration registrationBeforeObserve = server.getRegistrationFor(client);
        assertSuccessfulNotificationSendingAfterAddressChanged(registrationBeforeObserve);

        // check that client registration is not updated.
        Registration registrationAfterObserve = server.getRegistrationFor(client);
        assertThat(registrationAfterObserve).isEqualTo(registrationBeforeObserve);
    }

    @TestTlsTransport
    public void update_registration_on_notification_using_psk(Protocol givenProtocol,
            String givenClientEndpointProvider, String givenServerEndpointProvider)
            throws InterruptedException, TimeoutException, NonUniqueSecurityInfoException {

        // Start Client and Server
        server = givenServerUsing(givenProtocol).with(givenServerEndpointProvider).withUpdateOnNotification().build();
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
        Registration registrationBeforeObserve = server.getRegistrationFor(client);
        assertSuccessfulNotificationSendingAfterAddressChanged(registrationBeforeObserve);

        // check that client registration is updated.
        Registration registrationAfterObserve = server.getRegistrationFor(client);
        assertThat(registrationAfterObserve.getSocketAddress())
                .isNotEqualTo(registrationBeforeObserve.getSocketAddress());
    }

    @TestTlsTransport
    public void can_send_notification_if_ip_changes_using_rpk(Protocol givenProtocol,
            String givenClientEndpointProvider, String givenServerEndpointProvider)
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
        Registration registrationBeforeObserve = server.getRegistrationFor(client);
        assertSuccessfulNotificationSendingAfterAddressChanged(registrationBeforeObserve);

        // check that client registration is not updated.
        Registration registrationAfterObserve = server.getRegistrationFor(client);
        assertThat(registrationAfterObserve).isEqualTo(registrationBeforeObserve);
    }

    @TestTlsTransport
    public void can_send_notification_if_ip_changes_using_x509(Protocol givenProtocol,
            String givenClientEndpointProvider, String givenServerEndpointProvider)
            throws InterruptedException, TimeoutException, NonUniqueSecurityInfoException {

        // Start Client and Server
        server = givenServerUsing(givenProtocol).with(givenServerEndpointProvider)//
                .actingAsServerOnly()//
                .using(serverX509Cert, serverPrivateKeyFromCert)//
                .trusting(trustedCertificates).build(); // default server support
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
        Registration registrationBeforeObserve = server.getRegistrationFor(client);
        assertSuccessfulNotificationSendingAfterAddressChanged(registrationBeforeObserve);

        // check that client registration is not updated.
        Registration registrationAfterObserve = server.getRegistrationFor(client);
        assertThat(registrationAfterObserve).isEqualTo(registrationBeforeObserve);
    }

    private void assertSuccessfulNotificationSendingAfterAddressChanged(Registration currentRegistration)
            throws InterruptedException {

        // observe device timezone
        ObserveResponse observeResponse = server.send(currentRegistration, new ObserveRequest(3, 0, 15));
        assertThat(observeResponse) //
                .hasCode(CONTENT);

        // an observation response should have been sent
        SingleObservation observation = observeResponse.getObservation();
        assertThat(observation.getPath()).asString().isEqualTo("/3/0/15");
        assertThat(observation.getRegistrationId()).isEqualTo(currentRegistration.getId());
        Set<Observation> observations = server.getObservationService().getObservations(currentRegistration);
        assertThat(observations).containsExactly(observation);

        // Simulate new IP for client
        proxy.changeServerSideProxyAddress();

        // Force new Handshake
        LwM2mServer registeredServer = client.getRegisteredServers().values().iterator().next();
        client.clearSecurityContextFor(registeredServer);

        // write device timezone
        WriteResponse writeResponse = client.getObjectTree().getObjectEnabler(3).write(LwM2mServer.SYSTEM,
                new WriteRequest(3, 0, 15, "Europe/Paris"));
        assertThat(writeResponse).hasCode(CHANGED);

        // notification succeed !
        server.waitForNewObservation(observation);
        ObserveResponse response = server.waitForNotificationOf(observation);
        assertThat(response.getContent()).isEqualTo(LwM2mSingleResource.newStringResource(15, "Europe/Paris"));
    }
}
