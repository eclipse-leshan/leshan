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
import java.util.function.Function;
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
import org.eclipse.leshan.core.request.UplinkRequest;
import org.eclipse.leshan.core.response.LwM2mResponse;
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
    LockStepLwM2mClient client;

    @BeforeEach
    void start(String givenServerEndpointProvider) {
        server = givenServer().using(Protocol.COAP).with(givenServerEndpointProvider).build();
        server.start();

        client = new LockStepLwM2mClient(server.getEndpoint(Protocol.COAP).getURI());
    }

    @AfterEach
    void stop() {
        if (server != null)
            server.destroy();
        if (client != null)
            client.destroy();
    }

    /*---------------------------------/
     *  Tests Utils
     * -------------------------------*/
    private <T extends UplinkRequest<? extends LwM2mResponse>> void sendInvalidRequestExpectingGivenResponseCode(
            T lwm2mReq, Function<Request, Request> requestModifier, ResponseCode expectedCode) throws Exception {
        // create request and modify it to make it invalid
        Request validRequest = client.createCoapRequest(lwm2mReq);
        Request invalidRequest = requestModifier.apply(validRequest);

        // send invalid request
        Token token = client.sendCoapRequest(invalidRequest);

        // assert that we get right failure code
        client.expectResponse().token(token).code(expectedCode).go();
    }

    private RegisterRequest createValidRegisterRequestForV11() throws Exception {
        return new RegisterRequest( //
                client.getEndpointName(), //
                60l, // lifetime
                "1.1", // lwm2m version
                EnumSet.of(BindingMode.U), // binding
                null, // queue mode (since v1.1)
                null, // SMS number
                linkParser.parseCoreLinkFormat("</1>,</2>,</3>".getBytes()), // linked object
                null); // additional attributes
    }

    private Registration registerClientv11Successfully() throws Exception {
        return registerClientSuccessfullyWith(createValidRegisterRequestForV11());
    }

    private RegisterRequest createValidRegisterRequestForV10() throws Exception {
        return new RegisterRequest( //
                client.getEndpointName(), //
                60l, // lifetime
                "1.0", // lwm2m version
                EnumSet.of(BindingMode.U), // binding
                null, // queue mode (since v1.1)
                null, // SMS number
                linkParser.parseCoreLinkFormat("</1>,</2>,</3>".getBytes()), // linked object
                null); // additional attributes
    }

    private Registration registerClientv10Successfully() throws Exception {
        return registerClientSuccessfullyWith(createValidRegisterRequestForV10());
    }

    private Registration registerClientSuccessfullyWith(RegisterRequest registerRequest) throws Exception {
        return registerClientSuccessfullyWith(registerRequest, Function.identity());
    }

    private Registration registerClientSuccessfullyWith(RegisterRequest registerRequest,
            Function<Request, Request> requestModifier) throws Exception {
        // create request and modify it
        Request request = requestModifier.apply(client.createCoapRequest(registerRequest));

        // send request
        Token token = client.sendCoapRequest(request);

        // assert client is register
        client.expectResponse().token(token).code(ResponseCode.CREATED).go();
        server.waitForNewRegistrationOf(client.getEndpointName());
        return server.getRegistrationService().getByEndpoint(client.getEndpointName());
    }

    private UpdateRequest createValidUpdateRequest(Registration registration) {
        return new UpdateRequest("/rd/" + registration.getId(), 60l, null, EnumSet.of(BindingMode.U), null, null);
    }

    private Registration updateClientSuccessfullyWith(Registration registration) throws Exception {
        return updateClientSuccessfullyWith(registration, createValidUpdateRequest(registration));
    }

    private Registration updateClientSuccessfullyWith(Registration registration, UpdateRequest updateRequest)
            throws Exception {
        return updateClientSuccessfullyWith(registration, updateRequest, Function.identity());
    }

    private Registration updateClientSuccessfullyWith(Registration registration, UpdateRequest updateRequest,
            Function<Request, Request> requestModifier) throws Exception {
        // create request and modify it
        Request request = requestModifier.apply(client.createCoapRequest(updateRequest));

        // send request
        Token token = client.sendCoapRequest(request);

        // assert client is updated
        client.expectResponse().token(token).code(ResponseCode.CHANGED).go();
        server.waitForUpdateOf(registration);
        return server.getRegistrationService().getByEndpoint(client.getEndpointName());
    }

    /*---------------------------------/
     *  Tests
     * -------------------------------*/
    @TestAllTransportLayer
    public void register_with_invalid_request(String givenServerEndpointProvider) throws Exception {

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
        server.ensureNoRegistration();
    }

    @TestAllTransportLayer
    public void register_with_uq_binding_in_lw_1_0(String givenServerEndpointProvider) throws Exception {

        registerClientSuccessfullyWith(new RegisterRequest( //
                client.getEndpointName(), //
                60l, // lifetime
                "1.0", // lwm2m version
                EnumSet.of(BindingMode.U, BindingMode.Q), // binding
                null, // queue mode (since v1.1)
                null, // SMS number
                linkParser.parseCoreLinkFormat("</1>,</2>,</3>".getBytes()), // linked object
                null)); // additional attributes
    }

    @TestAllTransportLayer
    public void register_with_ut_binding_in_lw_1_1(String givenServerEndpointProvider) throws Exception {

        registerClientSuccessfullyWith(new RegisterRequest( //
                client.getEndpointName(), //
                60l, // lifetime
                "1.1", // lwm2m version
                EnumSet.of(BindingMode.U, BindingMode.T), // binding
                null, // queue mode (since v1.1)
                null, // SMS number
                linkParser.parseCoreLinkFormat("</1>,</2>,</3>".getBytes()), // linked object
                null)); // additional attributes

    }

    @TestAllTransportLayer
    public void register_update_with_invalid_binding_for_lw_1_1(String givenServerEndpointProvider) throws Exception {

        Registration registration = registerClientv11Successfully();

        updateClientSuccessfullyWith(registration);

        // register with invalid binding for 1.1
        sendInvalidRequestExpectingGivenResponseCode(createValidRegisterRequestForV11(), invalidRegisterRequest -> {
            invalidRegisterRequest.getOptions().removeUriQuery("b=U");
            invalidRegisterRequest.getOptions().addUriQuery("b=UQ");
            return invalidRegisterRequest;
        }, ResponseCode.BAD_REQUEST);

        // update with invalid binding for 1.1
        sendInvalidRequestExpectingGivenResponseCode(createValidUpdateRequest(registration), invalidUpdateRequest -> {
            invalidUpdateRequest.getOptions().removeUriQuery("b=U");
            invalidUpdateRequest.getOptions().addUriQuery("b=UQ");
            return invalidUpdateRequest;
        }, ResponseCode.BAD_REQUEST);

    }

    @TestAllTransportLayer
    public void register_update_with_invalid_binding_for_lw_1_0(String givenServerEndpointProvider) throws Exception {

        Registration registration = registerClientv10Successfully();

        updateClientSuccessfullyWith(registration);

        // register with invalid binding for 1.0
        sendInvalidRequestExpectingGivenResponseCode(createValidRegisterRequestForV10(), invalidRegisterRequest -> {
            invalidRegisterRequest.getOptions().removeUriQuery("b=U");
            invalidRegisterRequest.getOptions().addUriQuery("b=UT");
            return invalidRegisterRequest;
        }, ResponseCode.BAD_REQUEST);

        // update with invalid binding for 1.0
        sendInvalidRequestExpectingGivenResponseCode(createValidUpdateRequest(registration), invalidUpdateRequest -> {
            invalidUpdateRequest.getOptions().removeUriQuery("b=U");
            invalidUpdateRequest.getOptions().addUriQuery("b=UT");
            return invalidUpdateRequest;
        }, ResponseCode.BAD_REQUEST);
    }
}
