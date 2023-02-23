/*******************************************************************************
 * Copyright (c) 2022 Sierra Wireless and others.
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
import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import org.eclipse.californium.core.coap.CoAP.ResponseCode;
import org.eclipse.californium.core.coap.Request;
import org.eclipse.californium.core.coap.Token;
import org.eclipse.californium.core.config.CoapConfig;
import org.eclipse.californium.elements.config.Configuration;
import org.eclipse.leshan.core.endpoint.Protocol;
import org.eclipse.leshan.core.link.LinkParser;
import org.eclipse.leshan.core.link.lwm2m.DefaultLwM2mLinkParser;
import org.eclipse.leshan.core.node.LwM2mNode;
import org.eclipse.leshan.core.node.LwM2mPath;
import org.eclipse.leshan.core.node.LwM2mSingleResource;
import org.eclipse.leshan.core.request.BindingMode;
import org.eclipse.leshan.core.request.ContentFormat;
import org.eclipse.leshan.core.request.RegisterRequest;
import org.eclipse.leshan.core.request.SendRequest;
import org.eclipse.leshan.integration.tests.lockstep.LockStepLwM2mClient;
import org.eclipse.leshan.integration.tests.util.LeshanTestServer;
import org.eclipse.leshan.integration.tests.util.LeshanTestServerBuilder;
import org.eclipse.leshan.integration.tests.util.junit5.extensions.BeforeEachParameterizedResolver;
import org.eclipse.leshan.server.californium.endpoint.ServerProtocolProvider;
import org.eclipse.leshan.server.californium.endpoint.coap.CoapServerProtocolProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

@ExtendWith(BeforeEachParameterizedResolver.class)
public class LockStepSendTest {
    public static final LinkParser linkParser = new DefaultLwM2mLinkParser();

    /*---------------------------------/
     *  Parameterized Tests
     * -------------------------------*/
    @ParameterizedTest(name = "COAP - Server using {0}")
    @MethodSource("transports")
    @Retention(RetentionPolicy.RUNTIME)
    private @interface TestAllTransportLayer {
    }

    static Stream<org.junit.jupiter.params.provider.Arguments> transports() {
        return Stream.of(//
                // ProtocolUsed - Server Endpoint Provider
                arguments("Californium"));
    }

    /*---------------------------------/
     *  Set-up and Tear-down Tests
     * -------------------------------*/
    public LeshanTestServerBuilder givenServer() {
        return new LeshanTestServerBuilder() {
            @Override
            protected ServerProtocolProvider getCaliforniumProtocolProvider(Protocol protocol) {
                return new CoapServerProtocolProvider() {
                    @Override
                    public void applyDefaultValue(Configuration configuration) {
                        super.applyDefaultValue(configuration);
                        // configure retransmission, with this configuration a request without ACK should timeout in
                        // ~200*5ms
                        configuration.set(CoapConfig.ACK_TIMEOUT, 200, TimeUnit.MILLISECONDS) //
                                .set(CoapConfig.ACK_INIT_RANDOM, 1f) //
                                .set(CoapConfig.ACK_TIMEOUT_SCALE, 1f) //
                                .set(CoapConfig.MAX_RETRANSMIT, 4);
                    }
                };
            }
        };
    }

    LeshanTestServer server;

    @BeforeEach
    public void start(String givenServerEndpointProvider) {
        server = givenServer().using(Protocol.COAP).with(givenServerEndpointProvider).build();
        server.start();
    }

    @AfterEach
    public void stop() throws InterruptedException {
        if (server != null)
            server.destroy();
    }

    /*---------------------------------/
     *  Tests
     * -------------------------------*/
    @TestAllTransportLayer
    public void register_send_with_invalid_payload(String givenServerEndpointProvider) throws Exception {
        // Register client
        LockStepLwM2mClient client = new LockStepLwM2mClient(server.getEndpoint(Protocol.COAP).getURI());
        Token token = client
                .sendLwM2mRequest(new RegisterRequest(client.getEndpointName(), 60l, "1.1", EnumSet.of(BindingMode.U),
                        null, null, linkParser.parseCoreLinkFormat("</1>,</2>,</3>".getBytes()), null));
        client.expectResponse().token(token).code(ResponseCode.CREATED).go();
        server.waitForNewRegistrationOf(client.getEndpointName());

        // Send "Send Request" with invalid payload
        Map<LwM2mPath, LwM2mNode> nodes = new HashMap<>();
        nodes.put(new LwM2mPath("/3/0/1"), LwM2mSingleResource.newStringResource(1, "data"));
        Request sendRequest = client.createCoapRequest(new SendRequest(ContentFormat.SENML_CBOR, nodes));
        sendRequest.setPayload(new byte[] { 0x00, 0x10 });
        client.sendCoapRequest(sendRequest);

        // wait for error
        Exception exception = server.waitForSendDataError(client.getEndpointName(), 1, TimeUnit.SECONDS);
        assertThat(exception).isNotNull();
    }
}
