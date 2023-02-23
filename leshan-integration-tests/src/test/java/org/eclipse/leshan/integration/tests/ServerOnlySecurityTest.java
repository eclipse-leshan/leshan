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
import static org.eclipse.leshan.integration.tests.util.Credentials.GOOD_PSK_ID;
import static org.eclipse.leshan.integration.tests.util.Credentials.GOOD_PSK_KEY;
import static org.eclipse.leshan.integration.tests.util.LeshanTestClientBuilder.givenClientUsing;
import static org.eclipse.leshan.integration.tests.util.assertion.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.io.IOException;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.net.InetSocketAddress;
import java.net.URI;
import java.security.cert.Certificate;
import java.util.List;
import java.util.stream.Stream;

import javax.crypto.SecretKey;

import org.eclipse.californium.core.coap.CoAP.Type;
import org.eclipse.californium.core.coap.Request;
import org.eclipse.californium.core.coap.Token;
import org.eclipse.californium.core.network.serialization.UdpDataSerializer;
import org.eclipse.californium.elements.AddressEndpointContext;
import org.eclipse.californium.elements.EndpointContext;
import org.eclipse.californium.elements.RawData;
import org.eclipse.californium.elements.auth.PreSharedKeyIdentity;
import org.eclipse.californium.elements.config.Configuration;
import org.eclipse.californium.elements.exception.EndpointMismatchException;
import org.eclipse.californium.scandium.DTLSConnector;
import org.eclipse.californium.scandium.config.DtlsConnectorConfig.Builder;
import org.eclipse.californium.scandium.dtls.ConnectionId;
import org.eclipse.californium.scandium.dtls.PskPublicInformation;
import org.eclipse.californium.scandium.dtls.pskstore.AdvancedPskStore;
import org.eclipse.leshan.client.californium.endpoint.CaliforniumClientEndpointFactory;
import org.eclipse.leshan.client.californium.endpoint.CaliforniumClientEndpointsProvider;
import org.eclipse.leshan.client.californium.endpoint.coaps.CoapsClientProtocolProvider;
import org.eclipse.leshan.client.servers.ServerInfo;
import org.eclipse.leshan.core.endpoint.Protocol;
import org.eclipse.leshan.core.request.ReadRequest;
import org.eclipse.leshan.core.request.exception.SendFailedException;
import org.eclipse.leshan.core.response.ReadResponse;
import org.eclipse.leshan.integration.tests.util.LeshanTestClient;
import org.eclipse.leshan.integration.tests.util.LeshanTestClientBuilder;
import org.eclipse.leshan.integration.tests.util.LeshanTestServer;
import org.eclipse.leshan.integration.tests.util.LeshanTestServerBuilder;
import org.eclipse.leshan.integration.tests.util.SinglePSKStore;
import org.eclipse.leshan.integration.tests.util.cf.SimpleMessageCallback;
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
public class ServerOnlySecurityTest {

    private SinglePSKStore singlePSKStore;

    public void setNewPsk(String identity, byte[] key) {
        if (identity != null)
            singlePSKStore.setIdentity(identity);
        if (key != null)
            singlePSKStore.setKey(key);
    }

    /*---------------------------------/
     *  Parameterized Tests
     * -------------------------------*/
    @ParameterizedTest(name = "COAPS - Server using {0}")
    @MethodSource("transports")
    @Retention(RetentionPolicy.RUNTIME)
    private @interface TestAllTransportLayer {
    }

    static Stream<org.junit.jupiter.params.provider.Arguments> transports() {
        return Stream.of(//
                // Server Endpoint Provider
                arguments("Californium"));
    }

    /*---------------------------------/
     *  Set-up and Tear-down Tests
     * -------------------------------*/
    LeshanTestServer server;
    LeshanTestClientBuilder givenClient;
    LeshanTestClient client;

    @BeforeEach
    public void start(String givenServerEndpointProvider) {
        server = givenServerUsing(Protocol.COAPS).with(givenServerEndpointProvider).build();
        server.start();
        givenClient = givenClientUsing(Protocol.COAPS);

        // HACK to be able to change PSK on the fly
        CoapsClientProtocolProvider coapsProtocolProvider = new CoapsClientProtocolProvider() {
            @Override
            public CaliforniumClientEndpointFactory createDefaultEndpointFactory() {
                return new org.eclipse.leshan.client.californium.endpoint.coaps.CoapsClientEndpointFactory() {

                    @Override
                    protected Builder createEffectiveDtlsConnectorConfigBuilder(InetSocketAddress addr,
                            ServerInfo serverInfo, Builder rootDtlsConfigBuilder, Configuration coapConfig,
                            boolean clientInitiatedOnly, List<Certificate> trustStore) {
                        // create config
                        Builder dtlsConfigBuilder = super.createEffectiveDtlsConnectorConfigBuilder(addr, serverInfo,
                                rootDtlsConfigBuilder, coapConfig, clientInitiatedOnly, trustStore);

                        // tricks to be able to change psk information on the fly
                        // DtlsConnectorConfig.Builder newBuilder = DtlsConnectorConfig.builder(dtlsConfig);
                        AdvancedPskStore pskStore = dtlsConfigBuilder.getIncompleteConfig().getAdvancedPskStore();
                        if (pskStore != null) {
                            PskPublicInformation identity = pskStore.getIdentity(null, null);
                            SecretKey key = pskStore
                                    .requestPskSecretResult(ConnectionId.EMPTY, null, identity, null, null, null, false)
                                    .getSecret();
                            singlePSKStore = new SinglePSKStore(identity, key);
                            dtlsConfigBuilder.setAdvancedPskStore(singlePSKStore);
                        }
                        return dtlsConfigBuilder;
                    }
                };
            }
        };
        CaliforniumClientEndpointsProvider endpointsProvider = new CaliforniumClientEndpointsProvider.Builder(
                coapsProtocolProvider).build();
        givenClient.setEndpointsProvider(endpointsProvider);
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

    // TODO We should really find a better way to test this. Maybe creating LockStepLwM2mClient which support DTLS.
    @TestAllTransportLayer
    public void dont_sent_request_if_identity_change(String givenServerEndpointProvider)
            throws NonUniqueSecurityInfoException, InterruptedException, IOException {

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

        // Add new credential to the server
        server.getSecurityStore()
                .add(SecurityInfo.newPreSharedKeyInfo(client.getEndpointName(), "anotherPSK", GOOD_PSK_KEY));

        // Create new session with new credentials at client side.
        // Get connector
        DTLSConnector connector = (DTLSConnector) client
                .getClientConnector(client.getServerIdForRegistrationId("/rd/" + registration.getId()));
        // Clear DTLS session to force new handshake
        connector.clearConnectionState();
        // Change PSK id
        setNewPsk("anotherPSK", GOOD_PSK_KEY);
        // send and empty message to force a new handshake with new credentials
        SimpleMessageCallback callback = new SimpleMessageCallback();
        // create a ping message
        Request request = new Request(null, Type.CON);
        request.setToken(Token.EMPTY);
        request.setMID(0);
        byte[] ping = new UdpDataSerializer().getByteArray(request);
        // sent it
        URI destinationUri = server.getEndpoint(Protocol.COAPS).getURI();
        connector.send(RawData.outbound(ping,
                new AddressEndpointContext(destinationUri.getHost(), destinationUri.getPort()), callback, false));
        // Wait until new handshake DTLS is done
        EndpointContext endpointContext = callback.getEndpointContext(1000);
        assertEquals(((PreSharedKeyIdentity) endpointContext.getPeerIdentity()).getIdentity(), "anotherPSK");

        // Try to send a read request this should failed with an SendFailedException.
        Exception e = assertThrowsExactly(SendFailedException.class, () -> {
            server.send(registration, new ReadRequest(3, 0, 1), 1000);
        });
        assertThat(e).cause().isExactlyInstanceOf(EndpointMismatchException.class);

        connector.stop();
        client.destroy(false);
        client = null;
    }
}
