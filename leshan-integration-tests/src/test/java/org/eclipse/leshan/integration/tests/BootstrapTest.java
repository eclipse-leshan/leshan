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
 *     Michał Wadowski (Orange) - Improved compliance with rfc6690
 *******************************************************************************/
package org.eclipse.leshan.integration.tests;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.leshan.integration.tests.BootstrapConfigTestBuilder.givenBootstrapConfig;
import static org.eclipse.leshan.integration.tests.util.LeshanTestBootstrapServerBuilder.givenBootstrapServerUsing;
import static org.eclipse.leshan.integration.tests.util.LeshanTestClientBuilder.givenClientUsing;
import static org.eclipse.leshan.integration.tests.util.LeshanTestServerBuilder.givenServerUsing;
import static org.eclipse.leshan.integration.tests.util.assertion.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import org.eclipse.leshan.client.bootstrap.InvalidStateException;
import org.eclipse.leshan.client.resource.LwM2mObjectEnabler;
import org.eclipse.leshan.client.servers.ServerIdentity;
import org.eclipse.leshan.core.LwM2mId;
import org.eclipse.leshan.core.ResponseCode;
import org.eclipse.leshan.core.endpoint.Protocol;
import org.eclipse.leshan.core.node.LwM2mObject;
import org.eclipse.leshan.core.node.LwM2mObjectInstance;
import org.eclipse.leshan.core.request.BootstrapDiscoverRequest;
import org.eclipse.leshan.core.request.ContentFormat;
import org.eclipse.leshan.core.request.ExecuteRequest;
import org.eclipse.leshan.core.request.ReadRequest;
import org.eclipse.leshan.core.request.exception.InvalidRequestException;
import org.eclipse.leshan.core.request.exception.RequestCanceledException;
import org.eclipse.leshan.core.response.BootstrapDiscoverResponse;
import org.eclipse.leshan.core.response.ExecuteResponse;
import org.eclipse.leshan.core.response.LwM2mResponse;
import org.eclipse.leshan.core.response.ReadResponse;
import org.eclipse.leshan.core.util.TestLwM2mId;
import org.eclipse.leshan.integration.tests.util.BootstrapRequestChecker;
import org.eclipse.leshan.integration.tests.util.LeshanTestBootstrapServer;
import org.eclipse.leshan.integration.tests.util.LeshanTestBootstrapServerBuilder;
import org.eclipse.leshan.integration.tests.util.LeshanTestClient;
import org.eclipse.leshan.integration.tests.util.LeshanTestClientBuilder;
import org.eclipse.leshan.integration.tests.util.LeshanTestServer;
import org.eclipse.leshan.integration.tests.util.LeshanTestServerBuilder;
import org.eclipse.leshan.integration.tests.util.junit5.extensions.BeforeEachParameterizedResolver;
import org.eclipse.leshan.server.bootstrap.BootstrapConfig.ACLConfig;
import org.eclipse.leshan.server.bootstrap.BootstrapFailureCause;
import org.eclipse.leshan.server.bootstrap.BootstrapSession;
import org.eclipse.leshan.server.bootstrap.InvalidConfigurationException;
import org.eclipse.leshan.server.registration.Registration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

@ExtendWith(BeforeEachParameterizedResolver.class)
public class BootstrapTest {

    /*---------------------------------/
     *  Parameterized Tests
     * -------------------------------*/
    @ParameterizedTest(name = "{0} - Client using {1} - Server using {2}- BS Server using {3}")
    @MethodSource("transports")
    @Retention(RetentionPolicy.RUNTIME)
    private @interface TestAllTransportLayer {
    }

    static Stream<org.junit.jupiter.params.provider.Arguments> transports() {
        return Stream.of(//
                // ProtocolUsed - Client Endpoint Provider - Server Endpoint Provider - BS Server Endpoint Provider
                arguments(Protocol.COAP, "Californium", "Californium", "Californium"));
    }

    /*---------------------------------/
     *  Set-up and Tear-down Tests
     * -------------------------------*/
    LeshanTestBootstrapServerBuilder givenBootstrapServer;
    LeshanTestBootstrapServer bootstrapServer;
    LeshanTestServerBuilder givenServer;
    LeshanTestServer server;
    LeshanTestClientBuilder givenClient;
    LeshanTestClient client;

    @BeforeEach
    public void start(Protocol givenProtocol, String givenClientEndpointProvider, String givenServerEndpointProvider,
            String givenBootstrapServerEndpointProvider) {
        givenServer = givenServerUsing(givenProtocol).with(givenServerEndpointProvider);
        givenBootstrapServer = givenBootstrapServerUsing(givenProtocol).with(givenBootstrapServerEndpointProvider);
        givenClient = givenClientUsing(givenProtocol).with(givenClientEndpointProvider);
    }

    @AfterEach
    public void stop() throws InterruptedException {
        if (client != null)
            client.destroy(false);
        if (server != null)
            server.destroy();
        if (bootstrapServer != null)
            bootstrapServer.destroy();
    }

    /*---------------------------------/
     *  Tests
     * -------------------------------*/

    @TestAllTransportLayer
    public void bootstrap(Protocol givenProtocol, String givenClientEndpointProvider,
            String givenServerEndpointProvider, String givenBootstrapServerEndpointProvider)
            throws InvalidConfigurationException {
        // Create DM Server without security & start it
        server = givenServer.build();
        server.start();

        // Create and start bootstrap server
        bootstrapServer = givenBootstrapServer.build();
        bootstrapServer.start();

        // Create Client and check it is not already registered
        client = givenClient.connectingTo(bootstrapServer).build();
        assertThat(client).isNotRegisteredAt(server);

        // Add config for this client
        bootstrapServer.getConfigStore().add(client.getEndpointName(), //
                givenBootstrapConfig() //
                        .adding(givenProtocol, bootstrapServer) //
                        .adding(givenProtocol, server) //
                        .build());

        // Start it and wait for registration
        client.start();
        server.waitForNewRegistrationOf(client);

        // check the client is registered
        assertThat(client).isRegisteredAt(server);
    }

    @TestAllTransportLayer
    public void bootstrap_tlv_only(Protocol givenProtocol, String givenClientEndpointProvider,
            String givenServerEndpointProvider, String givenBootstrapServerEndpointProvider)
            throws InvalidConfigurationException {
        // Create DM Server without security & start it
        server = givenServer.build();
        server.start();

        // Create and start bootstrap server
        bootstrapServer = givenBootstrapServer.build();
        bootstrapServer.start();

        // Create Client and check it is not already registered
        client = givenClient.connectingTo(bootstrapServer)//
                // if no preferred content format server should use TLV
                .preferring(null)//
                .supporting(ContentFormat.TLV) //
                .build();
        assertThat(client).isNotRegisteredAt(server);

        // TODO this should be replace by mockito ArgumentCaptor
        BootstrapRequestChecker contentFormatChecker = BootstrapRequestChecker.contentFormatChecker(ContentFormat.TLV);
        bootstrapServer.addListener(contentFormatChecker);

        // Add config for this client
        bootstrapServer.getConfigStore().add(client.getEndpointName(), //
                givenBootstrapConfig() //
                        .adding(givenProtocol, bootstrapServer) //
                        .adding(givenProtocol, server) //
                        .build());

        // Start it and wait for registration
        client.start();
        server.waitForNewRegistrationOf(client);

        // check the client is registered
        assertThat(client).isRegisteredAt(server);
        assertTrue(contentFormatChecker.isValid(), "not expected content format used");
    }

    @TestAllTransportLayer
    public void bootstrap_senmlcbor_only(Protocol givenProtocol, String givenClientEndpointProvider,
            String givenServerEndpointProvider, String givenBootstrapServerEndpointProvider)
            throws InvalidConfigurationException {
        // Create DM Server without security & start it
        server = givenServer.build();
        server.start();

        // Create and start bootstrap server
        bootstrapServer = givenBootstrapServer.build();
        bootstrapServer.start();

        // Create Client and check it is not already registered
        client = givenClient.connectingTo(bootstrapServer)//
                // if no preferred content format server should use TLV
                .preferring(ContentFormat.SENML_CBOR)//
                .supporting(ContentFormat.SENML_CBOR) //
                .build();
        assertThat(client).isNotRegisteredAt(server);

        // TODO this should be replace by mockito ArgumentCaptor
        BootstrapRequestChecker contentFormatChecker = BootstrapRequestChecker
                .contentFormatChecker(ContentFormat.SENML_CBOR);
        bootstrapServer.addListener(contentFormatChecker);

        // Add config for this client
        bootstrapServer.getConfigStore().add(client.getEndpointName(), //
                givenBootstrapConfig() //
                        .adding(givenProtocol, bootstrapServer) //
                        .adding(givenProtocol, server) //
                        .build());

        // Start it and wait for registration
        client.start();
        server.waitForNewRegistrationOf(client);

        // check the client is registered
        assertThat(client).isRegisteredAt(server);
        assertTrue(contentFormatChecker.isValid(), "not expected content format used");
    }

    @TestAllTransportLayer
    public void bootstrap_contentformat_from_config(Protocol givenProtocol, String givenClientEndpointProvider,
            String givenServerEndpointProvider, String givenBootstrapServerEndpointProvider)
            throws InvalidConfigurationException {
        // Create DM Server without security & start it
        server = givenServer.build();
        server.start();

        // Create and start bootstrap server
        bootstrapServer = givenBootstrapServer.build();
        bootstrapServer.start();

        // Create Client and check it is not already registered
        client = givenClient.connectingTo(bootstrapServer)//
                // if no preferred content format server should use TLV
                .preferring(ContentFormat.SENML_CBOR)//
                .build();
        assertThat(client).isNotRegisteredAt(server);

        // Add config for this client
        bootstrapServer.getConfigStore().add(client.getEndpointName(), //
                givenBootstrapConfig() //
                        .adding(givenProtocol, bootstrapServer) //
                        .adding(givenProtocol, server) //
                        .using(ContentFormat.SENML_JSON) //
                        .build());

        // TODO this should be replace by mockito ArgumentCaptor
        BootstrapRequestChecker contentFormatChecker = BootstrapRequestChecker
                .contentFormatChecker(ContentFormat.SENML_JSON);
        bootstrapServer.addListener(contentFormatChecker);

        // Start it and wait for registration
        client.start();
        server.waitForNewRegistrationOf(client);

        // check the client is registered
        assertThat(client).isRegisteredAt(server);
        assertTrue(contentFormatChecker.isValid(), "not expected content format used");
    }

    @TestAllTransportLayer
    public void bootstrapWithAdditionalAttributes(Protocol givenProtocol, String givenClientEndpointProvider,
            String givenServerEndpointProvider, String givenBootstrapServerEndpointProvider)
            throws InvalidConfigurationException {
        // Create DM Server without security & start it
        server = givenServer.build();
        server.start();

        // Create and start bootstrap server
        bootstrapServer = givenBootstrapServer.build();
        bootstrapServer.start();

        // Create Client with additional attributes and check it is not already registered
        Map<String, String> additionalAttributes = new HashMap<>();
        additionalAttributes.put("key1", "value1");
        additionalAttributes.put("imei", "2136872368");

        client = givenClient.connectingTo(bootstrapServer)//
                .withBootstrap(additionalAttributes)//
                .build();
        assertThat(client).isNotRegisteredAt(server);

        // Add config for this client
        bootstrapServer.getConfigStore().add(client.getEndpointName(), //
                givenBootstrapConfig() //
                        .adding(givenProtocol, bootstrapServer) //
                        .adding(givenProtocol, server) //
                        .build());

        // Start it and wait for registration
        client.start();
        server.waitForNewRegistrationOf(client);

        // check the client is registered
        assertThat(client).isRegisteredAt(server);

        // assert session contains additional attributes
        BootstrapSession bootstrapSession = bootstrapServer.verifyForSuccessfullBootstrap();

        assertThat(bootstrapSession.getBootstrapRequest().getAdditionalAttributes())
                .containsAllEntriesOf(additionalAttributes);
    }

    @TestAllTransportLayer
    public void bootstrapWithDiscoverOnRoot(Protocol givenProtocol, String givenClientEndpointProvider,
            String givenServerEndpointProvider, String givenBootstrapServerEndpointProvider)
            throws InvalidConfigurationException {
        // Create DM Server without security & start it
        server = givenServer.build();
        server.start();

        // Create and start bootstrap server
        BootstrapDiscoverRequest firstRequest = new BootstrapDiscoverRequest();
        bootstrapServer = givenBootstrapServer.startingSessionWith(firstRequest).build();
        bootstrapServer.start();

        // Create Client and check it is not already registered
        client = givenClient.connectingTo(bootstrapServer).build();
        assertThat(client).isNotRegisteredAt(server);

        // Add config for this client
        bootstrapServer.getConfigStore().add(client.getEndpointName(), //
                givenBootstrapConfig() //
                        .adding(givenProtocol, bootstrapServer) //
                        .adding(givenProtocol, server) //
                        .build());

        // Start it and wait for registration
        client.start();
        server.waitForNewRegistrationOf(client);

        // check the client is registered
        assertThat(client).isRegisteredAt(server);

        // check response
        BootstrapSession bootstrapSession = bootstrapServer.verifyForSuccessfullBootstrap();
        LwM2mResponse firstResponse = bootstrapServer.getFirstResponseFor(bootstrapSession, firstRequest);
        assertThat(firstResponse).isInstanceOfSatisfying(BootstrapDiscoverResponse.class, r -> {
            assertThat(r).hasCode(ResponseCode.CONTENT);
            assertThat(r.getObjectLinks()).isLikeLwM2mLinks(
                    String.format("</>;lwm2m=1.0,</0/0>;uri=\"coap://%s:%d\",</1>,</2>,</3442/0>,</3/0>",
                            bootstrapServer.getEndpoint(givenProtocol).getURI().getHost(),
                            bootstrapServer.getEndpoint(givenProtocol).getURI().getPort()));
        });
    }

    @TestAllTransportLayer
    public void bootstrapWithDiscoverOnRootThenRebootstrap(Protocol givenProtocol, String givenClientEndpointProvider,
            String givenServerEndpointProvider, String givenBootstrapServerEndpointProvider)
            throws InvalidRequestException, InterruptedException, InvalidConfigurationException {
        // Create DM Server without security & start it
        server = givenServer.build();
        server.start();

        // Create and start bootstrap server
        BootstrapDiscoverRequest firstRequest = new BootstrapDiscoverRequest();
        bootstrapServer = givenBootstrapServer.startingSessionWith(firstRequest).build();
        bootstrapServer.start();

        // Create Client and check it is not already registered
        client = givenClient.connectingTo(bootstrapServer).build();
        assertThat(client).isNotRegisteredAt(server);

        // Add config for this client
        bootstrapServer.getConfigStore().add(client.getEndpointName(), //
                givenBootstrapConfig() //
                        .adding(givenProtocol, bootstrapServer) //
                        .adding(givenProtocol, server) //
                        .build());

        // Start it and wait for registration
        client.start();
        server.waitForNewRegistrationOf(client);

        // check the client is registered
        assertThat(client).isRegisteredAt(server);

        // check response
        BootstrapSession bootstrapSession = bootstrapServer.verifyForSuccessfullBootstrap();
        LwM2mResponse firstResponse = bootstrapServer.getFirstResponseFor(bootstrapSession, firstRequest);
        assertThat(firstResponse).isInstanceOfSatisfying(BootstrapDiscoverResponse.class, r -> {
            assertThat(r).hasCode(ResponseCode.CONTENT);
            assertThat(r.getObjectLinks()).isLikeLwM2mLinks(
                    String.format("</>;lwm2m=1.0,</0/0>;uri=\"coap://%s:%d\",</1>,</2>,</3442/0>,</3/0>",
                            bootstrapServer.getEndpoint(givenProtocol).getURI().getHost(),
                            bootstrapServer.getEndpoint(givenProtocol).getURI().getPort()));
        });

        // re-bootstrap
        bootstrapServer.resetInvocations();
        try {
            Registration registration = server.getRegistrationFor(client);
            ExecuteResponse response = server.send(registration, new ExecuteRequest("/1/0/9"));
            assertTrue(response.isSuccess());
        } catch (RequestCanceledException e) {
            // request can be cancelled if server does not received the execute response before the de-registration
            // so we just ignore this error.
        }

        // wait bootstrap finished
        bootstrapSession = bootstrapServer.waitForSuccessfullBootstrap(1, TimeUnit.SECONDS);

        // check response
        LwM2mResponse secondResponse = bootstrapServer.getFirstResponseFor(bootstrapSession, firstRequest);
        assertThat(secondResponse).isInstanceOfSatisfying(BootstrapDiscoverResponse.class, r -> {
            assertThat(r).hasCode(ResponseCode.CONTENT);
            assertThat(r.getObjectLinks()).isLikeLwM2mLinks(String.format(
                    "</>;lwm2m=1.0,</0/0>;uri=\"coap://%s:%d\",</0/1>;ssid=2222;uri=\"coap://%s:%d\",</1/0>;ssid=2222,</2>,</3442/0>,</3/0>",
                    bootstrapServer.getEndpoint(givenProtocol).getURI().getHost(),
                    bootstrapServer.getEndpoint(givenProtocol).getURI().getPort(),
                    server.getEndpoint(givenProtocol).getURI().getHost(),
                    server.getEndpoint(givenProtocol).getURI().getPort()));
        });
    }

    @TestAllTransportLayer
    public void bootstrapWithDiscoverOnDevice(Protocol givenProtocol, String givenClientEndpointProvider,
            String givenServerEndpointProvider, String givenBootstrapServerEndpointProvider)
            throws InvalidConfigurationException {
        // Create DM Server without security & start it
        server = givenServer.build();
        server.start();

        // Create and start bootstrap server
        BootstrapDiscoverRequest firstRequest = new BootstrapDiscoverRequest(3);
        bootstrapServer = givenBootstrapServer.startingSessionWith(firstRequest).build();
        bootstrapServer.start();

        // Create Client and check it is not already registered
        client = givenClient.connectingTo(bootstrapServer).build();
        assertThat(client).isNotRegisteredAt(server);

        // Add config for this client
        bootstrapServer.getConfigStore().add(client.getEndpointName(), //
                givenBootstrapConfig() //
                        .adding(givenProtocol, bootstrapServer) //
                        .adding(givenProtocol, server) //
                        .build());

        // Start it and wait for registration
        client.start();
        server.waitForNewRegistrationOf(client);

        // check the client is registered
        assertThat(client).isRegisteredAt(server);

        // check response
        BootstrapSession bootstrapSession = bootstrapServer.verifyForSuccessfullBootstrap();
        LwM2mResponse firstResponse = bootstrapServer.getFirstResponseFor(bootstrapSession, firstRequest);
        assertThat(firstResponse).isInstanceOfSatisfying(BootstrapDiscoverResponse.class, r -> {
            assertThat(r).hasCode(ResponseCode.CONTENT);
            assertThat(r.getObjectLinks()).isLikeLwM2mLinks("</>;lwm2m=1.0,</3/0>");
        });
    }

    @TestAllTransportLayer
    public void bootstrap_create_2_bsserver(Protocol givenProtocol, String givenClientEndpointProvider,
            String givenServerEndpointProvider, String givenBootstrapServerEndpointProvider)
            throws InvalidConfigurationException {
        // Create DM Server without security & start it
        server = givenServer.build();
        server.start();

        // Create and start bootstrap server
        bootstrapServer = givenBootstrapServer.build();
        bootstrapServer.start();

        // Create Client with bootstrap server config at /0/10
        client = givenClient.connectingTo(bootstrapServer).usingBootstrapServerId(10).build();
        assertThat(client).isNotRegisteredAt(server);

        // Add config for this client
        bootstrapServer.getConfigStore().add(client.getEndpointName(), //
                givenBootstrapConfig() //
                        .adding(givenProtocol, bootstrapServer, 0) // with bootstrap server ID = 0
                        .adding(givenProtocol, server) //
                        .build());

        // Start it and wait for registration
        client.start();

        // ensure bootstrap session failed because of invalid state
        Exception cause = client.waitForBootstrapFailure(bootstrapServer, 2, TimeUnit.SECONDS);
        assertThat(cause).isExactlyInstanceOf(InvalidStateException.class);
        BootstrapFailureCause failure = bootstrapServer.waitForBootstrapFailure(1, TimeUnit.SECONDS);
        assertThat(failure).isEqualTo(BootstrapFailureCause.FINISH_FAILED);
    }

    @TestAllTransportLayer
    public void bootstrap_with_auto_id_for_security_object(Protocol givenProtocol, String givenClientEndpointProvider,
            String givenServerEndpointProvider, String givenBootstrapServerEndpointProvider)
            throws InvalidConfigurationException {
        // Create DM Server without security & start it
        server = givenServer.build();
        server.start();

        // Create and start bootstrap server
        bootstrapServer = givenBootstrapServer.build();
        bootstrapServer.start();

        // Create Client with bootstrap server config at /0/10
        client = givenClient.connectingTo(bootstrapServer).usingBootstrapServerId(10).build();
        assertThat(client).isNotRegisteredAt(server);

        // Add config for this client
        bootstrapServer.getConfigStore().add(client.getEndpointName(), //
                givenBootstrapConfig() //
                        .adding(givenProtocol, bootstrapServer, 0) // with bootstrap server ID = 0
                        .adding(givenProtocol, server) //
                        .usingAutoIdForSecurityObject() //
                        .build());

        // Start it and wait for registration
        client.start();

        // ensure bootstrap session succeed
        server.waitForNewRegistrationOf(client);
        client.waitForBootstrapSuccess(bootstrapServer, 1, TimeUnit.SECONDS);
        bootstrapServer.verifyForSuccessfullBootstrap();
        assertThat(client).isRegisteredAt(server);
    }

    @TestAllTransportLayer
    public void bootstrap_delete_access_control_and_connectivity_statistics(Protocol givenProtocol,
            String givenClientEndpointProvider, String givenServerEndpointProvider,
            String givenBootstrapServerEndpointProvider) throws InvalidConfigurationException {

        // Create DM Server without security & start it
        server = givenServer.build();
        server.start();

        // Create and start bootstrap server
        bootstrapServer = givenBootstrapServer.build();
        bootstrapServer.start();

        // Create Client
        client = givenClient.connectingTo(bootstrapServer) //
                .withOneSimpleInstancesForObjects(LwM2mId.LOCATION).build();

        assertThat(client).isNotRegisteredAt(server);

        // Add config for this client
        bootstrapServer.getConfigStore().add(client.getEndpointName(), //
                givenBootstrapConfig() //
                        .deleting(LwM2mId.ACCESS_CONTROL, TestLwM2mId.TEST_OBJECT) //
                        .adding(givenProtocol, bootstrapServer) //
                        .adding(givenProtocol, server) //
                        .build());

        // Start it and wait for registration
        client.start();

        // ensure bootstrap session succeed
        server.waitForNewRegistrationOf(client);
        client.waitForBootstrapSuccess(bootstrapServer, 1, TimeUnit.SECONDS);
        bootstrapServer.verifyForSuccessfullBootstrap();
        assertThat(client).isRegisteredAt(server);

        // ensure instances are deleted
        ReadResponse response = client.getObjectTree().getObjectEnabler(LwM2mId.ACCESS_CONTROL)
                .read(ServerIdentity.SYSTEM, new ReadRequest(LwM2mId.ACCESS_CONTROL));
        assertThat(((LwM2mObject) response.getContent()).getInstances()).as("ACL instances").isEmpty();

        response = client.getObjectTree().getObjectEnabler(TestLwM2mId.TEST_OBJECT).read(ServerIdentity.SYSTEM,
                new ReadRequest(TestLwM2mId.TEST_OBJECT));
        assertThat(((LwM2mObject) response.getContent()).getInstances()).as("Test Object instances").isEmpty();

        // ensure other instances are not deleted.
        response = client.getObjectTree().getObjectEnabler(LwM2mId.DEVICE).read(ServerIdentity.SYSTEM,
                new ReadRequest(LwM2mId.DEVICE));
        assertThat(((LwM2mObject) response.getContent()).getInstances()).as("DEVICE instances").isNotEmpty();

        response = client.getObjectTree().getObjectEnabler(LwM2mId.SECURITY).read(ServerIdentity.SYSTEM,
                new ReadRequest(LwM2mId.SECURITY));
        assertThat(((LwM2mObject) response.getContent()).getInstances()).as("SECURITY instances").isNotEmpty();

        response = client.getObjectTree().getObjectEnabler(LwM2mId.LOCATION).read(ServerIdentity.SYSTEM,
                new ReadRequest(LwM2mId.LOCATION));
        assertThat(((LwM2mObject) response.getContent()).getInstances()).as("LOCATION instances").isNotEmpty();
    }

    @TestAllTransportLayer
    public void bootstrapDeleteAll(Protocol givenProtocol, String givenClientEndpointProvider,
            String givenServerEndpointProvider, String givenBootstrapServerEndpointProvider)
            throws InvalidConfigurationException {
        // Create DM Server without security & start it
        server = givenServer.build();
        server.start();

        // Create and start bootstrap server
        bootstrapServer = givenBootstrapServer.build();
        bootstrapServer.start();

        // Create Client
        client = givenClient.connectingTo(bootstrapServer) //
                .withOneSimpleInstancesForObjects(LwM2mId.LOCATION).build();

        assertThat(client).isNotRegisteredAt(server);

        // Add config for this client
        bootstrapServer.getConfigStore().add(client.getEndpointName(), //
                givenBootstrapConfig() //
                        .deletingAll() //
                        .build());

        // Start it and wait for registration
        client.start();

        // ensure bootstrap session succeed
        client.waitForBootstrapSuccess(bootstrapServer, 1, TimeUnit.SECONDS);

        // ensure instances are deleted except device instance and bootstrap server
        for (LwM2mObjectEnabler enabler : client.getObjectTree().getObjectEnablers().values()) {
            ReadResponse response = enabler.read(ServerIdentity.SYSTEM, new ReadRequest(enabler.getId()));
            LwM2mObject responseValue = (LwM2mObject) response.getContent();
            if (enabler.getId() == LwM2mId.DEVICE) {
                assertThat(responseValue.getInstances()).as("Devices instances").hasSize(1);
            } else if (enabler.getId() == LwM2mId.SECURITY) {
                assertThat(responseValue.getInstances()).as("Security instances").hasSize(1);
                LwM2mObjectInstance securityInstance = responseValue.getInstances().values().iterator().next();
                assertThat(securityInstance.getResource(1).getValue()).isEqualTo(Boolean.TRUE);
            } else {
                assertThat(responseValue.getInstances()).as(enabler.getObjectModel().name + " instances").isEmpty();
            }
        }
    }

    @TestAllTransportLayer
    public void bootstrapWithAcl(Protocol givenProtocol, String givenClientEndpointProvider,
            String givenServerEndpointProvider, String givenBootstrapServerEndpointProvider)
            throws InvalidConfigurationException {
        // Create DM Server without security & start it
        server = givenServer.build();
        server.start();

        // Create and start bootstrap server
        bootstrapServer = givenBootstrapServer.build();
        bootstrapServer.start();

        // Create Client
        client = givenClient.connectingTo(bootstrapServer) //
                .withOneSimpleInstancesForObjects(LwM2mId.ACCESS_CONTROL).build();

        assertThat(client).isNotRegisteredAt(server);

        // Define ACL
        List<ACLConfig> acls = new ArrayList<>();
        ACLConfig aclConfig = new ACLConfig();
        aclConfig.objectId = 3;
        aclConfig.objectInstanceId = 0;
        HashMap<Integer, Long> acl = new HashMap<Integer, Long>();
        acl.put(3333, 1l); // server with short id 3333 has just read(1) right on device object (3/0)
        aclConfig.acls = acl;
        aclConfig.AccessControlOwner = 2222;
        acls.add(aclConfig);

        aclConfig = new ACLConfig();
        aclConfig.objectId = 4;
        aclConfig.objectInstanceId = 0;
        aclConfig.AccessControlOwner = 2222;
        acls.add(aclConfig);

        // Add config for this client
        bootstrapServer.getConfigStore().add(client.getEndpointName(), //
                givenBootstrapConfig() //
                        .adding(givenProtocol, bootstrapServer) // with bootstrap server ID = 0
                        .adding(givenProtocol, server) //
                        .adding(acls) //
                        .build());

        // Start it and wait for registration
        client.start();

        // ensure bootstrap session succeed
        client.waitForBootstrapSuccess(bootstrapServer, 1, TimeUnit.SECONDS);

        // ensure ACL is correctly set
        ReadResponse response = client.getObjectTree().getObjectEnabler(2).read(ServerIdentity.SYSTEM,
                new ReadRequest(2));
        LwM2mObject aclObject = (LwM2mObject) response.getContent();

        assertThat(aclObject.getInstances()).allSatisfy((i, instance) -> {
            ACLConfig expectedACL = acls.get(i);
            assertThat(instance.getResource(0).getValue()).isEqualTo((long) expectedACL.objectId);
            assertThat(instance.getResource(1).getValue()).isEqualTo((long) expectedACL.objectInstanceId);
            assertThat(instance.getResource(3).getValue()).isEqualTo((long) expectedACL.AccessControlOwner);

            if (expectedACL.acls != null) {
                for (Entry<Integer, Long> entry : expectedACL.acls.entrySet()) {
                    assertThat(instance.getResource(2).getValue(entry.getKey())).isEqualTo((long) entry.getValue());
                }
            }
        });
    }
}
