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
import static org.mockito.Mockito.verifyNoMoreInteractions;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.EnumSet;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import org.eclipse.californium.core.coap.CoAP.Code;
import org.eclipse.californium.core.coap.CoAP.ResponseCode;
import org.eclipse.californium.core.coap.CoAP.Type;
import org.eclipse.californium.core.coap.Request;
import org.eclipse.californium.core.coap.Token;
import org.eclipse.californium.core.config.CoapConfig;
import org.eclipse.californium.elements.AddressEndpointContext;
import org.eclipse.californium.elements.config.Configuration;
import org.eclipse.leshan.core.endpoint.Protocol;
import org.eclipse.leshan.core.link.LinkParser;
import org.eclipse.leshan.core.link.lwm2m.DefaultLwM2mLinkParser;
import org.eclipse.leshan.core.node.LwM2mPath;
import org.eclipse.leshan.core.node.LwM2mSingleResource;
import org.eclipse.leshan.core.node.TimestampedLwM2mNodes;
import org.eclipse.leshan.core.node.codec.DefaultLwM2mEncoder;
import org.eclipse.leshan.core.node.codec.LwM2mEncoder;
import org.eclipse.leshan.core.observation.Observation;
import org.eclipse.leshan.core.request.BindingMode;
import org.eclipse.leshan.core.request.ContentFormat;
import org.eclipse.leshan.core.request.DeregisterRequest;
import org.eclipse.leshan.core.request.ObserveRequest;
import org.eclipse.leshan.core.request.ReadRequest;
import org.eclipse.leshan.core.request.RegisterRequest;
import org.eclipse.leshan.core.request.UpdateRequest;
import org.eclipse.leshan.core.request.exception.SendFailedException;
import org.eclipse.leshan.core.request.exception.TimeoutException;
import org.eclipse.leshan.core.response.*;
import org.eclipse.leshan.integration.tests.util.LeshanTestServer;
import org.eclipse.leshan.integration.tests.util.LeshanTestServerBuilder;
import org.eclipse.leshan.integration.tests.util.junit5.extensions.BeforeEachParameterizedResolver;
import org.eclipse.leshan.server.californium.endpoint.ServerProtocolProvider;
import org.eclipse.leshan.server.californium.endpoint.coap.CoapServerProtocolProvider;
import org.eclipse.leshan.server.californium.request.LwM2mResponseBuilder;
import org.eclipse.leshan.server.endpoint.LwM2mServerEndpointsProvider;
import org.eclipse.leshan.server.registration.Registration;
import org.eclipse.leshan.transport.javacoap.server.endpoint.JavaCoapServerEndpointsProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import com.mbed.coap.server.CoapServerBuilder;
import com.mbed.coap.transmission.RetransmissionBackOff;

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
    public void register_with_invalid_request(String givenServerEndpointProvider) throws Exception {

        LockStepLwM2mClient client = new LockStepLwM2mClient(server.getEndpoint(Protocol.COAP).getURI());

        // create a register request without the list of supported object
        Request invalidRegisterRequest = new Request(Code.POST);
        URI destinationURI = server.getEndpoint(Protocol.COAP).getURI();
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
        client.sendEmpty(Type.ACK, ContentFormat.TEXT).loadMID("R").go();

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
        client.sendEmpty(Type.ACK, ContentFormat.TEXT).loadMID("R").go();

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

    @TestAllTransportLayer
    public void register_deregister_observe(String givenServerEndpointProvider) throws Exception {
        // register client
        LockStepLwM2mClient client = new LockStepLwM2mClient(server.getEndpoint(Protocol.COAP).getURI());
        Token token = client
                .sendLwM2mRequest(new RegisterRequest(client.getEndpointName(), 60l, "1.1", EnumSet.of(BindingMode.U),
                        null, null, linkParser.parseCoreLinkFormat("</1>,</2>,</3>".getBytes()), null));
        client.expectResponse().token(token).go();
        server.waitForNewRegistrationOf(client.getEndpointName());
        Registration registration = server.getRegistrationService().getByEndpoint(client.getEndpointName());

        // deregister client
        token = client.sendLwM2mRequest(new DeregisterRequest("rd/" + registration.getId()));
        client.expectResponse().token(token).go();
        server.waitForDeregistrationOf(registration);

        // send read
        @SuppressWarnings("unchecked")
        ResponseCallback<ObserveResponse> responseCallback = mock(ResponseCallback.class);
        ErrorCallback errorCallback = mock(ErrorCallback.class);

        server.send(registration, new ObserveRequest(ContentFormat.TEXT_CODE, 3, 0), 500l, responseCallback,
                errorCallback);

        if (givenServerEndpointProvider.equals("Californium")) {
            // with californium endpoint provider "SendFailedException" is raised because,
            // we try to add the relation in store before to send the request and registration doesn't exist anymorev
            verify(errorCallback, timeout(200).times(1)) //
                    .onError(assertArg(e -> {
                        assertThat(e).isInstanceOf(SendFailedException.class);
                    }));
        } else {
            // with java-coap it failed transparently at response reception.
            // TODO I don't know if this is the right behavior.
            client.expectRequest().storeMID("R").storeToken("T").go();
            client.sendResponse(Type.ACK, ResponseCode.CONTENT, ContentFormat.TEXT).payload("aaa").observe(2)
                    .loadMID("R").loadToken("T").go();
        }

        // ensure we don't get answer and there is no observation in store.
        verifyNoMoreInteractions(responseCallback, errorCallback);
        assertThat(server.getRegistrationService().getAllRegistrations().hasNext() == false);
        Set<Observation> observations = server.getObservationService().getObservations(registration);
        assertThat(observations).isEmpty();
    }

    @TestAllTransportLayer
    public void read_timestamped(String givenServerEndpointProvider) throws Exception {

        // -------------------------------------------REGISTER
        // CLIENT----------------------------------------------------
        LockStepLwM2mClient client = new LockStepLwM2mClient(server.getEndpoint(Protocol.COAP).getURI());
        Token token = client
                .sendLwM2mRequest(new RegisterRequest(client.getEndpointName(), 60l, "1.1", EnumSet.of(BindingMode.U),
                        null, null, linkParser.parseCoreLinkFormat("</1>,</2>,</3>".getBytes()), null));
        client.expectResponse().token(token).go();
        server.waitForNewRegistrationOf(client.getEndpointName());
        Registration registration = server.getRegistrationService().getByEndpoint(client.getEndpointName());
        // --------------------------------------------------------------------------------------------------------------

        // ----------------------------------------------TIMESTAMP-------------------------------------------------------
        LwM2mEncoder encoder = new DefaultLwM2mEncoder();
        TimestampedLwM2mNodes.Builder builder = new TimestampedLwM2mNodes.Builder();
        Instant t1 = Instant.now();
        builder.put(t1, new LwM2mPath("/1/0/1"), LwM2mSingleResource.newIntegerResource(1, 3600));
        TimestampedLwM2mNodes timestampedNodes = builder.build();
        byte[] payload = encoder.encodeTimestampedNodes(timestampedNodes, ContentFormat.SENML_JSON,
                client.getLwM2mModel());
        // --------------------------------------------------------------------------------------------------------------

        // Send read REQUEST
        Future<ReadResponse> future = Executors.newSingleThreadExecutor().submit(new Callable<ReadResponse>() {
            @Override
            public ReadResponse call() throws Exception {
                // send a request with 3 seconds timeout
                return server.send(registration, new ReadRequest(ContentFormat.SENML_JSON, 1), 2000);
            }
        });

        client.expectRequest().storeToken("TKN").storeMID("MID").go();

        client.sendResponse(Type.ACK, ResponseCode.CONTENT, ContentFormat.SENML_JSON).loadMID("MID").loadToken("TKN")
                .payload(payload).go();

        ReadResponse response = future.get(2000, TimeUnit.MILLISECONDS);
        assertThat(response.getTimestampedLwM2mNode()).isNotNull();
    }

}
