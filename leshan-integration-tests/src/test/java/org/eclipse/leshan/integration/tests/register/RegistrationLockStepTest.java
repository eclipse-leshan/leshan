/*******************************************************************************
 * Copyright (c) 2017 Sierra Wireless and others.
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
package org.eclipse.leshan.integration.tests.register;

import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.EnumSet;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import org.eclipse.californium.core.coap.CoAP.Code;
import org.eclipse.californium.core.coap.CoAP.ResponseCode;
import org.eclipse.californium.core.coap.Request;
import org.eclipse.californium.core.coap.Token;
import org.eclipse.californium.core.config.CoapConfig;
import org.eclipse.californium.elements.AddressEndpointContext;
import org.eclipse.californium.elements.config.Configuration;
import org.eclipse.leshan.core.endpoint.EndpointUri;
import org.eclipse.leshan.core.endpoint.Protocol;
import org.eclipse.leshan.core.link.LinkParser;
import org.eclipse.leshan.core.link.lwm2m.DefaultLwM2mLinkParser;
import org.eclipse.leshan.core.request.BindingMode;
import org.eclipse.leshan.core.request.ContentFormat;
import org.eclipse.leshan.core.request.RegisterRequest;
import org.eclipse.leshan.core.request.UpdateRequest;
import org.eclipse.leshan.integration.tests.lockstep.LockStepLwM2mClient;
import org.eclipse.leshan.integration.tests.util.LeshanTestServer;
import org.eclipse.leshan.integration.tests.util.LeshanTestServerBuilder;
import org.eclipse.leshan.integration.tests.util.junit5.extensions.BeforeEachParameterizedResolver;
import org.eclipse.leshan.server.endpoint.LwM2mServerEndpointsProvider;
import org.eclipse.leshan.server.registration.Registration;
import org.eclipse.leshan.transport.californium.server.endpoint.ServerProtocolProvider;
import org.eclipse.leshan.transport.californium.server.endpoint.coap.CoapServerProtocolProvider;
import org.eclipse.leshan.transport.javacoap.server.endpoint.JavaCoapServerEndpointsProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import com.mbed.coap.server.CoapServerBuilder;
import com.mbed.coap.transmission.RetransmissionBackOff;

@ExtendWith(BeforeEachParameterizedResolver.class)
public class RegistrationLockStepTest {
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
                // Server Endpoint Provider
                arguments("Californium"), //
                arguments("java-coap"));
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
                        // ~200*5ms = 1s
                        configuration.set(CoapConfig.ACK_TIMEOUT, 200, TimeUnit.MILLISECONDS) //
                                .set(CoapConfig.ACK_INIT_RANDOM, 1f) //
                                .set(CoapConfig.ACK_TIMEOUT_SCALE, 1f) //
                                .set(CoapConfig.MAX_RETRANSMIT, 4);
                    }
                };
            }

            @Override
            protected LwM2mServerEndpointsProvider getJavaCoapProtocolProvider(Protocol protocol) {
                return new JavaCoapServerEndpointsProvider(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0)) {
                    @Override
                    protected CoapServerBuilder createCoapServer() {
                        return super.createCoapServer()
                                // configure retransmission, with this configuration a request without ACK should
                                // timeout in
                                // 140 + 2*140 + 4*140 = ~1s
                                .retransmission(RetransmissionBackOff.ofExponential(Duration.ofMillis(140), 2, 1));
                    }
                };
            }
        };
    }

    LeshanTestServer server;

    @BeforeEach
    void start(String givenServerEndpointProvider) {
        server = givenServer().using(Protocol.COAP).with(givenServerEndpointProvider).build();
        server.start();
    }

    @AfterEach
    void stop() {
        if (server != null)
            server.destroy();
    }

    /*---------------------------------/
     *  Tests
     * -------------------------------*/
    @TestAllTransportLayer
    public void register_with_invalid_request(String givenServerEndpointProvider) throws Exception {

        LockStepLwM2mClient client = new LockStepLwM2mClient(server.getEndpoint(Protocol.COAP).getURI());

        // create a register request without the list of supported object
        Request invalidRegisterRequest = new Request(Code.POST);
        EndpointUri destinationURI = server.getEndpoint(Protocol.COAP).getURI();
        invalidRegisterRequest
                .setDestinationContext(new AddressEndpointContext(destinationURI.getHost(), destinationURI.getPort()));
        invalidRegisterRequest.getOptions().setContentFormat(ContentFormat.LINK.getCode());
        invalidRegisterRequest.getOptions().addUriPath("rd");
        invalidRegisterRequest.getOptions().addUriQuery("ep=" + client.getEndpointName());

        // send request and check it is rejected
        Token token = client.sendCoapRequest(invalidRegisterRequest);
        client.expectResponse().token(token).code(ResponseCode.BAD_REQUEST).go();
    }

    @TestAllTransportLayer
    public void register_with_uq_binding_in_lw_1_0(String givenServerEndpointProvider) throws Exception {
        // Register client
        LockStepLwM2mClient client = new LockStepLwM2mClient(server.getEndpoint(Protocol.COAP).getURI());
        Token token = client.sendLwM2mRequest(
                new RegisterRequest(client.getEndpointName(), 60l, "1.0", EnumSet.of(BindingMode.U, BindingMode.Q),
                        null, null, linkParser.parseCoreLinkFormat("</1>,</2>,</3>".getBytes()), null));
        client.expectResponse().token(token).code(ResponseCode.CREATED).go();
        server.waitForNewRegistrationOf(client.getEndpointName());
    }

    @TestAllTransportLayer
    public void register_with_ut_binding_in_lw_1_1(String givenServerEndpointProvider) throws Exception {
        // Register client
        LockStepLwM2mClient client = new LockStepLwM2mClient(server.getEndpoint(Protocol.COAP).getURI());
        Token token = client.sendLwM2mRequest(
                new RegisterRequest(client.getEndpointName(), 60l, "1.1", EnumSet.of(BindingMode.U, BindingMode.T),
                        null, null, linkParser.parseCoreLinkFormat("</1>,</2>,</3>".getBytes()), null));
        client.expectResponse().token(token).code(ResponseCode.CREATED).go();
        server.waitForNewRegistrationOf(client.getEndpointName());
    }

    @TestAllTransportLayer
    public void register_update_with_invalid_binding_for_lw_1_1(String givenServerEndpointProvider) throws Exception {
        LockStepLwM2mClient client = new LockStepLwM2mClient(server.getEndpoint(Protocol.COAP).getURI());

        // register with valid binding for 1.1
        RegisterRequest validRegisterRequest = new RegisterRequest(client.getEndpointName(), 60l, "1.1",
                EnumSet.of(BindingMode.U), null, null, linkParser.parseCoreLinkFormat("</1>,</2>,</3>".getBytes()),
                null);
        Token token = client.sendLwM2mRequest(validRegisterRequest);
        client.expectResponse().token(token).code(ResponseCode.CREATED).go();
        server.waitForNewRegistrationOf(client.getEndpointName());

        // update with valid binding for 1.1
        Registration registration = server.getRegistrationService().getByEndpoint(client.getEndpointName());
        UpdateRequest validUpdateRequest = new UpdateRequest("/rd/" + registration.getId(), 60l, null,
                EnumSet.of(BindingMode.U), null, null);
        token = client.sendLwM2mRequest(validUpdateRequest);
        client.expectResponse().token(token).code(ResponseCode.CHANGED).go();

        // register with invalid binding for 1.1
        Request invalidRegisterRequest = client.createCoapRequest(validRegisterRequest);
        invalidRegisterRequest.getOptions().removeUriQuery("b=U");
        invalidRegisterRequest.getOptions().addUriQuery("b=UQ");
        token = client.sendCoapRequest(invalidRegisterRequest);
        client.expectResponse().token(token).code(ResponseCode.BAD_REQUEST).go();

        // update with invalid binding for 1.1
        Request invalidUpdateRequest = client.createCoapRequest(validUpdateRequest);
        invalidUpdateRequest.getOptions().removeUriQuery("b=U");
        invalidUpdateRequest.getOptions().addUriQuery("b=UQ");
        token = client.sendCoapRequest(invalidUpdateRequest);
        client.expectResponse().token(token).code(ResponseCode.BAD_REQUEST).go();
    }

    @TestAllTransportLayer
    public void register_update_with_invalid_binding_for_lw_1_0(String givenServerEndpointProvider) throws Exception {
        LockStepLwM2mClient client = new LockStepLwM2mClient(server.getEndpoint(Protocol.COAP).getURI());

        // register with valid binding for 1.0
        RegisterRequest validRegisterRequest = new RegisterRequest(client.getEndpointName(), 60l, "1.0",
                EnumSet.of(BindingMode.U), null, null, linkParser.parseCoreLinkFormat("</1>,</2>,</3>".getBytes()),
                null);
        Token token = client.sendLwM2mRequest(validRegisterRequest);
        client.expectResponse().token(token).code(ResponseCode.CREATED).go();
        server.waitForNewRegistrationOf(client.getEndpointName());

        // update with valid binding for 1.0
        Registration registration = server.getRegistrationService().getByEndpoint(client.getEndpointName());
        UpdateRequest validUpdateRequest = new UpdateRequest("/rd/" + registration.getId(), 60l, null,
                EnumSet.of(BindingMode.U), null, null);
        token = client.sendLwM2mRequest(validUpdateRequest);
        client.expectResponse().token(token).code(ResponseCode.CHANGED).go();

        // register with invalid binding for 1.0
        Request invalidRegisterRequest = client.createCoapRequest(validRegisterRequest);
        invalidRegisterRequest.getOptions().removeUriQuery("b=U");
        invalidRegisterRequest.getOptions().addUriQuery("b=UT");
        token = client.sendCoapRequest(invalidRegisterRequest);
        client.expectResponse().token(token).code(ResponseCode.BAD_REQUEST).go();

        // update with invalid binding for 1.0
        Request invalidUpdateRequest = client.createCoapRequest(validUpdateRequest);
        invalidUpdateRequest.getOptions().removeUriQuery("b=U");
        invalidUpdateRequest.getOptions().addUriQuery("b=UT");
        token = client.sendCoapRequest(invalidUpdateRequest);
        client.expectResponse().token(token).code(ResponseCode.BAD_REQUEST).go();
    }
}
