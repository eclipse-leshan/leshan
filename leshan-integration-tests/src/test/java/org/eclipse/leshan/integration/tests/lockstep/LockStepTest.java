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
package org.eclipse.leshan.integration.tests.lockstep;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.leshan.integration.tests.util.assertion.Assertions.assertArg;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.EnumSet;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import org.eclipse.californium.core.coap.CoAP.ResponseCode;
import org.eclipse.californium.core.coap.CoAP.Type;
import org.eclipse.californium.core.coap.Request;
import org.eclipse.californium.core.coap.Token;
import org.eclipse.californium.core.config.CoapConfig;
import org.eclipse.californium.elements.config.Configuration;
import org.eclipse.leshan.core.endpoint.Protocol;
import org.eclipse.leshan.core.link.LinkParser;
import org.eclipse.leshan.core.link.lwm2m.DefaultLwM2mLinkParser;
import org.eclipse.leshan.core.request.BindingMode;
import org.eclipse.leshan.core.request.ReadRequest;
import org.eclipse.leshan.core.request.RegisterRequest;
import org.eclipse.leshan.core.request.UpdateRequest;
import org.eclipse.leshan.core.request.exception.TimeoutException;
import org.eclipse.leshan.core.response.ErrorCallback;
import org.eclipse.leshan.core.response.ReadResponse;
import org.eclipse.leshan.core.response.ResponseCallback;
import org.eclipse.leshan.integration.tests.util.LeshanTestServer;
import org.eclipse.leshan.integration.tests.util.LeshanTestServerBuilder;
import org.eclipse.leshan.integration.tests.util.junit5.extensions.BeforeEachParameterizedResolver;
import org.eclipse.leshan.server.californium.endpoint.ServerProtocolProvider;
import org.eclipse.leshan.server.californium.endpoint.coap.CoapServerProtocolProvider;
import org.eclipse.leshan.server.registration.Registration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

@ExtendWith(BeforeEachParameterizedResolver.class)
public class LockStepTest {
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
        Request invalidUpdateRequest = client.createCoapRequest(validRegisterRequest);
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
        Request invalidUpdateRequest = client.createCoapRequest(validRegisterRequest);
        invalidUpdateRequest.getOptions().removeUriQuery("b=U");
        invalidUpdateRequest.getOptions().addUriQuery("b=UT");
        token = client.sendCoapRequest(invalidUpdateRequest);
        client.expectResponse().token(token).code(ResponseCode.BAD_REQUEST).go();
    }

    @TestAllTransportLayer
    public void sync_send_without_acknowleged(String givenServerEndpointProvider) throws Exception {
        // Register client
        LockStepLwM2mClient client = new LockStepLwM2mClient(server.getEndpoint(Protocol.COAP).getURI());
        Token token = client
                .sendLwM2mRequest(new RegisterRequest(client.getEndpointName(), 60l, "1.1", EnumSet.of(BindingMode.U),
                        null, null, linkParser.parseCoreLinkFormat("</1>,</2>,</3>".getBytes()), null));
        client.expectResponse().token(token).go();
        server.waitForNewRegistrationOf(client.getEndpointName());

        // Send read
        final Registration registration = server.getRegistrationService().getByEndpoint(client.getEndpointName());
        Future<ReadResponse> future = Executors.newSingleThreadExecutor().submit(new Callable<ReadResponse>() {
            @Override
            public ReadResponse call() throws Exception {
                // send a request with 3 seconds timeout
                return server.send(registration, new ReadRequest(3), 3000);
            }
        });
        // Request should timedout in ~1s we don't send ACK
        ReadResponse response = future.get(1500, TimeUnit.MILLISECONDS);
        assertNull(response, "we should timeout");
    }

    @TestAllTransportLayer
    public void sync_send_with_acknowleged_request_without_response(String givenServerEndpointProvider)
            throws Exception {
        // Register client
        LockStepLwM2mClient client = new LockStepLwM2mClient(server.getEndpoint(Protocol.COAP).getURI());
        Token token = client
                .sendLwM2mRequest(new RegisterRequest(client.getEndpointName(), 60l, "1.1", EnumSet.of(BindingMode.U),
                        null, null, linkParser.parseCoreLinkFormat("</1>,</2>,</3>".getBytes()), null));
        client.expectResponse().token(token).go();
        server.waitForNewRegistrationOf(client.getEndpointName());

        // Send read
        final Registration registration = server.getRegistrationService().getByEndpoint(client.getEndpointName());
        Future<ReadResponse> future = Executors.newSingleThreadExecutor().submit(new Callable<ReadResponse>() {
            @Override
            public ReadResponse call() throws Exception {
                // send a request with 3 seconds timeout
                return server.send(registration, new ReadRequest(3), 3000);
            }
        });

        // Acknowledge the response
        client.expectRequest().storeMID("R").go();
        client.sendEmpty(Type.ACK).loadMID("R").go();

        // Request should timedout in ~3s as we send the ACK
        Thread.sleep(1500);
        assertFalse(future.isDone(), "we should still wait for response");
        ReadResponse response = future.get(2000, TimeUnit.MILLISECONDS);
        assertNull(response, "we should timeout");
    }

    @TestAllTransportLayer
    public void async_send_without_acknowleged(String givenServerEndpointProvider) throws Exception {
        // register client
        LockStepLwM2mClient client = new LockStepLwM2mClient(server.getEndpoint(Protocol.COAP).getURI());
        Token token = client
                .sendLwM2mRequest(new RegisterRequest(client.getEndpointName(), 60l, "1.1", EnumSet.of(BindingMode.U),
                        null, null, linkParser.parseCoreLinkFormat("</1>,</2>,</3>".getBytes()), null));
        client.expectResponse().token(token).go();
        server.waitForNewRegistrationOf(client.getEndpointName());

        // send read
        final Registration registration = server.getRegistrationService().getByEndpoint(client.getEndpointName());
        @SuppressWarnings("unchecked")
        ResponseCallback<ReadResponse> responseCallback = mock(ResponseCallback.class);
        ErrorCallback errorCallback = mock(ErrorCallback.class);
        server.send(registration, new ReadRequest(3), 3000l, responseCallback, errorCallback);

        // Request should timedout in ~1s we don't send ACK
        verify(errorCallback, timeout(1500).times(1)) //
                .onError(assertArg(e -> {
                    assertThat(e).isInstanceOfSatisfying(TimeoutException.class,
                            ex -> assertThat(ex.getType()).isEqualTo(TimeoutException.Type.COAP_TIMEOUT));
                }));
        verify(responseCallback, never()).onResponse(any());
    }

    @TestAllTransportLayer
    public void async_send_with_acknowleged_request_without_response(String givenServerEndpointProvider)
            throws Exception {
        // register client
        LockStepLwM2mClient client = new LockStepLwM2mClient(server.getEndpoint(Protocol.COAP).getURI());
        Token token = client
                .sendLwM2mRequest(new RegisterRequest(client.getEndpointName(), 60l, "1.1", EnumSet.of(BindingMode.U),
                        null, null, linkParser.parseCoreLinkFormat("</1>,</2>,</3>".getBytes()), null));
        client.expectResponse().token(token).go();
        server.waitForNewRegistrationOf(client.getEndpointName());

        // send read
        final Registration registration = server.getRegistrationService().getByEndpoint(client.getEndpointName());
        @SuppressWarnings("unchecked")
        ResponseCallback<ReadResponse> responseCallback = mock(ResponseCallback.class);
        ErrorCallback errorCallback = mock(ErrorCallback.class);
        server.send(registration, new ReadRequest(3), 3000l, responseCallback, errorCallback);

        // Acknowledge the response
        client.expectRequest().storeMID("R").go();
        client.sendEmpty(Type.ACK).loadMID("R").go();

        // Request should timedout in ~3s as we send a ack
        Thread.sleep(1500);
        verifyNoInteractions(responseCallback, errorCallback);
        verify(errorCallback, timeout(2000).times(1)) //
                .onError(assertArg(e -> {
                    assertThat(e).isInstanceOfSatisfying(TimeoutException.class,
                            ex -> assertThat(ex.getType()).isEqualTo(TimeoutException.Type.RESPONSE_TIMEOUT));
                }));
        verify(responseCallback, never()).onResponse(any());
    }
}
