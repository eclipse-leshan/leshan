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
import static org.eclipse.leshan.integration.tests.util.LeshanTestClientBuilder.givenClientUsing;
import static org.eclipse.leshan.integration.tests.util.LeshanTestServerBuilder.givenServerUsing;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.stream.Stream;

import org.eclipse.californium.core.coap.Request;
import org.eclipse.californium.core.coap.Response;
import org.eclipse.californium.elements.AddressEndpointContext;
import org.eclipse.leshan.core.endpoint.Protocol;
import org.eclipse.leshan.core.node.LwM2mObjectInstance;
import org.eclipse.leshan.core.node.LwM2mSingleResource;
import org.eclipse.leshan.core.request.CreateRequest;
import org.eclipse.leshan.integration.tests.util.LeshanTestClient;
import org.eclipse.leshan.integration.tests.util.LeshanTestServer;
import org.eclipse.leshan.integration.tests.util.junit5.extensions.BeforeEachParameterizedResolver;
import org.eclipse.leshan.server.registration.IRegistration;
import org.eclipse.leshan.transport.californium.CoapSyncRequestObserver;
import org.eclipse.leshan.transport.californium.DefaultExceptionTranslator;
import org.eclipse.leshan.transport.californium.server.endpoint.CaliforniumServerEndpoint;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

@ExtendWith(BeforeEachParameterizedResolver.class)
public class DeleteClientOnlyTest {

    /*---------------------------------/
     *  Parameterized Tests
     * -------------------------------*/
    @ParameterizedTest(name = "{0} - Client using {1}")
    @MethodSource("transports")
    @Retention(RetentionPolicy.RUNTIME)
    private @interface TestAllTransportLayer {
    }

    static Stream<org.junit.jupiter.params.provider.Arguments> transports() {
        return Stream.of(//
                // ProtocolUsed - Client Endpoint Provider
                arguments(Protocol.COAP, "Californium"), //
                arguments(Protocol.COAP, "java-coap"));
    }

    /*---------------------------------/
     *  Set-up and Tear-down Tests
     * -------------------------------*/

    LeshanTestServer server;
    LeshanTestClient client;
    IRegistration currentRegistration;

    @BeforeEach
    public void start(Protocol givenProtocol, String givenClientEndpointProvider) {
        server = givenServerUsing(givenProtocol).with("Californium").build();
        server.start();
        client = givenClientUsing(givenProtocol).with(givenClientEndpointProvider).connectingTo(server).build();
        client.start();
        server.waitForNewRegistrationOf(client);
        client.waitForRegistrationTo(server);

        currentRegistration = server.getRegistrationFor(client);

    }

    @AfterEach
    public void stop() throws InterruptedException {
        if (client != null)
            client.destroy(false);
        if (server != null)
            server.destroy();
    }

    /*---------------------------------/
     *  Tests
     * -------------------------------*/
    @TestAllTransportLayer
    public void cannot_delete_resource(Protocol givenProtocol, String givenClientEndpointProvider)
            throws InterruptedException {

        // create ACL instance
        server.send(currentRegistration,
                new CreateRequest(2, new LwM2mObjectInstance(0, LwM2mSingleResource.newIntegerResource(0, 123))));

        // try to delete this resource using coap API as lwm2m API does not allow it.
        Request delete = Request.newDelete();
        delete.getOptions().addUriPath("2").addUriPath("0").addUriPath("0");

        // TODO TL add Coap API again ?
        delete.setDestinationContext(new AddressEndpointContext(currentRegistration.getSocketAddress()));
        CoapSyncRequestObserver syncMessageObserver = new CoapSyncRequestObserver(delete, 2000,
                new DefaultExceptionTranslator());
        delete.addMessageObserver(syncMessageObserver);

        CaliforniumServerEndpoint endpoint = (CaliforniumServerEndpoint) server.getEndpoint(Protocol.COAP);
        endpoint.getCoapEndpoint().sendRequest(delete);
        Response response = syncMessageObserver.waitForCoapResponse();

        // verify result
        assertThat(response.getCode()).isEqualTo(org.eclipse.californium.core.coap.CoAP.ResponseCode.BAD_REQUEST);
    }
}
