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
package org.eclipse.leshan.integration.tests.gateway;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import org.eclipse.californium.core.coap.CoAP.Code;
import org.eclipse.californium.core.coap.CoAP.ResponseCode;
import org.eclipse.californium.core.coap.CoAP.Type;
import org.eclipse.californium.core.coap.Request;
import org.eclipse.californium.core.coap.Token;
import org.eclipse.californium.core.config.CoapConfig;
import org.eclipse.californium.elements.config.Configuration;
import org.eclipse.leshan.core.endpoint.Protocol;
import org.eclipse.leshan.core.link.lwm2m.DefaultLwM2mLinkParser;
import org.eclipse.leshan.core.link.lwm2m.LwM2mLinkParser;
import org.eclipse.leshan.core.node.LwM2mNode;
import org.eclipse.leshan.core.node.LwM2mObject;
import org.eclipse.leshan.core.node.LwM2mObjectInstance;
import org.eclipse.leshan.core.node.LwM2mPath;
import org.eclipse.leshan.core.node.LwM2mSingleResource;
import org.eclipse.leshan.core.node.PrefixedLwM2mPath;
import org.eclipse.leshan.core.node.TimestampedLwM2mNodes;
import org.eclipse.leshan.core.node.codec.DefaultLwM2mEncoder;
import org.eclipse.leshan.core.node.codec.senml.LwM2mNodeSenMLDecoder;
import org.eclipse.leshan.core.request.BindingMode;
import org.eclipse.leshan.core.request.CancelCompositeObservationRequest;
import org.eclipse.leshan.core.request.ContentFormat;
import org.eclipse.leshan.core.request.ObserveCompositeRequest;
import org.eclipse.leshan.core.request.ReadCompositeRequest;
import org.eclipse.leshan.core.request.RegisterRequest;
import org.eclipse.leshan.core.request.WriteCompositeRequest;
import org.eclipse.leshan.core.response.CancelCompositeObservationResponse;
import org.eclipse.leshan.core.response.ErrorCallback;
import org.eclipse.leshan.core.response.ObserveCompositeResponse;
import org.eclipse.leshan.core.response.ReadCompositeResponse;
import org.eclipse.leshan.core.response.ResponseCallback;
import org.eclipse.leshan.core.response.WriteCompositeResponse;
import org.eclipse.leshan.integration.tests.lockstep.LockStepLwM2mClient;
import org.eclipse.leshan.integration.tests.util.LeshanTestServer;
import org.eclipse.leshan.integration.tests.util.LeshanTestServerBuilder;
import org.eclipse.leshan.integration.tests.util.TestObjectLoader;
import org.eclipse.leshan.integration.tests.util.junit5.extensions.BeforeEachParameterizedResolver;
import org.eclipse.leshan.senml.json.jackson.SenMLJsonJacksonEncoderDecoder;
import org.eclipse.leshan.server.LeshanServerBuilder;
import org.eclipse.leshan.server.endpoint.LwM2mServerEndpointsProvider;
import org.eclipse.leshan.server.model.LwM2mModelProvider;
import org.eclipse.leshan.server.model.VersionedModelProvider;
import org.eclipse.leshan.server.registration.IRegistration;
import org.eclipse.leshan.transport.californium.server.endpoint.ServerProtocolProvider;
import org.eclipse.leshan.transport.californium.server.endpoint.coap.CoapServerProtocolProvider;
import org.eclipse.leshan.transport.javacoap.server.endpoint.JavaCoapServerEndpointsProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;

import com.mbed.coap.server.CoapServerBuilder;
import com.mbed.coap.transmission.RetransmissionBackOff;

@ExtendWith(BeforeEachParameterizedResolver.class)
public class LockStepGatewayTest {
    public static final LwM2mLinkParser linkParser = new DefaultLwM2mLinkParser();

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

            @Override
            public LeshanServerBuilder setObjectModelProvider(LwM2mModelProvider objectModelProvider) {
                return super.setObjectModelProvider(
                        new VersionedModelProvider(TestObjectLoader.loadDefaultObjectWithGateway()));
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
    public void register_send(String givenServerEndpointProvider) throws Exception {

        // Register client
        LockStepLwM2mClient client = new LockStepLwM2mClient(server.getEndpoint(Protocol.COAP).getURI());
        Token token = client.sendLwM2mRequest(
                new RegisterRequest(client.getEndpointName(), 60l, "1.1", EnumSet.of(BindingMode.U), null, null,
                        linkParser.parseCoreLinkFormat("</1>,</2>,</3>,</25>;ver=2.0,</25/0>".getBytes()), null));
        client.startMultiExpectation();
        client.expectResponse().token(token).code(ResponseCode.CREATED).go();

        // wait for object 25 read
        client.expectRequest().storeBoth("A").code(Code.GET).go();
        client.goMultiExpectation();
        LwM2mObject object = new LwM2mObject(25, //
                new LwM2mObjectInstance(0, //
                        LwM2mSingleResource.newStringResource(0, "enddevice1"), //
                        LwM2mSingleResource.newStringResource(1, "d01"), //
                        LwM2mSingleResource.newCoreLinkResource(3,
                                new DefaultLwM2mLinkParser()
                                        .parseLwM2mLinkFromCoreLinkFormat("</3>;ver=1.2,</3/0>".getBytes(), null))),
                new LwM2mObjectInstance(1, //
                        LwM2mSingleResource.newStringResource(0, "enddevice2"), //
                        LwM2mSingleResource.newStringResource(1, "d02"), //
                        LwM2mSingleResource.newCoreLinkResource(3, new DefaultLwM2mLinkParser()
                                .parseLwM2mLinkFromCoreLinkFormat("</3>;ver=1.2,</3/0>".getBytes(), null))));
        byte[] payload = new DefaultLwM2mEncoder().encode(object, ContentFormat.SENML_JSON, null, new LwM2mPath(25),
                client.getLwM2mModel());
        client.sendResponse(Type.ACK, ResponseCode.CONTENT).loadBoth("A")
                .payload(payload, ContentFormat.SENML_JSON_CODE).go();

        // TODO Gateway : This sleep is needed because SEND Request could be received before we handle the Read Response
        // about Gateway object and so registration is not ready.
        // Is it something we should handle in real use case (sightly delay Send Handling waiting we get child data?)
        Thread.sleep(200);

        // Send "Send Request" with invalid payload
        StringBuilder json = new StringBuilder();
        json.append("[\n");
        json.append("  {\"bn\":\"/d01/3/0/\",\"n\":\"0\", \"vs\":\"Manufacturer1\"},\n");
        json.append("  {\"n\":\"1\",\"vs\":\"device1\"},\n");
        json.append("  {\"bn\":\"/d02/3/0/\",\"n\":\"0\", \"vs\":\"Manufacturer2\"},\n");
        json.append("  {\"n\":\"1\",\"vs\":\"device2\"}\n");
        json.append("]");

        token = new Token(Token.createBytes(new Random(), 8));
        int mid = 200;
        client.sendRequest(Type.CON, Code.POST, token, mid).path("/dp")
                .payload(json.toString(), ContentFormat.SENML_JSON_CODE).go();
        TimestampedLwM2mNodes data = server.waitForData(client.getEndpointName(), 1000, TimeUnit.MILLISECONDS);

        assertThat(data.getTimestamps()).hasSize(1);
        assertThat(data.getTimestamps()).containsExactlyInAnyOrder((Instant) null);
        assertThat(data.getPrefixedMostRecentNodes()).hasSize(4);

        assertThat(data.getPrefixedMostRecentNodes()).containsEntry( //
                PrefixedLwM2mPath.fromString("/d01/3/0/0"), LwM2mSingleResource.newStringResource(0, "Manufacturer1"));
        assertThat(data.getPrefixedMostRecentNodes()).containsEntry( //
                PrefixedLwM2mPath.fromString("/d01/3/0/1"), LwM2mSingleResource.newStringResource(1, "device1"));
        assertThat(data.getPrefixedMostRecentNodes()).containsEntry( //
                PrefixedLwM2mPath.fromString("/d02/3/0/0"), LwM2mSingleResource.newStringResource(0, "Manufacturer2"));
        assertThat(data.getPrefixedMostRecentNodes()).containsEntry( //
                PrefixedLwM2mPath.fromString("/d02/3/0/1"), LwM2mSingleResource.newStringResource(1, "device2"));
    }

    @TestAllTransportLayer
    public void register_composite_read(String givenServerEndpointProvider) throws Exception {

        // Register client
        LockStepLwM2mClient client = new LockStepLwM2mClient(server.getEndpoint(Protocol.COAP).getURI());
        Token token = client.sendLwM2mRequest(
                new RegisterRequest(client.getEndpointName(), 60l, "1.1", EnumSet.of(BindingMode.U), null, null,
                        linkParser.parseCoreLinkFormat("</1>,</2>,</3>,</25>;ver=2.0,</25/0>".getBytes()), null));
        client.startMultiExpectation();
        client.expectResponse().token(token).code(ResponseCode.CREATED).go();

        // wait for object 25 read
        client.expectRequest().storeBoth("A").code(Code.GET).go();
        client.goMultiExpectation();
        LwM2mObject object = new LwM2mObject(25, //
                new LwM2mObjectInstance(0, //
                        LwM2mSingleResource.newStringResource(0, "enddevice1"), //
                        LwM2mSingleResource.newStringResource(1, "d01"), //
                        LwM2mSingleResource.newCoreLinkResource(3,
                                new DefaultLwM2mLinkParser()
                                        .parseLwM2mLinkFromCoreLinkFormat("</3>;ver=1.2,</3/0>".getBytes(), null))),
                new LwM2mObjectInstance(1, //
                        LwM2mSingleResource.newStringResource(0, "enddevice2"), //
                        LwM2mSingleResource.newStringResource(1, "d02"), //
                        LwM2mSingleResource.newCoreLinkResource(3, new DefaultLwM2mLinkParser()
                                .parseLwM2mLinkFromCoreLinkFormat("</3>;ver=1.2,</3/0>".getBytes(), null))));
        byte[] payload = new DefaultLwM2mEncoder().encode(object, ContentFormat.SENML_JSON, null, new LwM2mPath(25),
                client.getLwM2mModel());
        client.sendResponse(Type.ACK, ResponseCode.CONTENT).loadBoth("A")
                .payload(payload, ContentFormat.SENML_JSON_CODE).go();

        // TODO Gateway : This sleep is needed because SEND Request could be received before we handle the Read Response
        // about Gateway object and so registration is not ready.
        // Is it something we should handle in real use case (sightly delay Send Handling waiting we get child data?)
        Thread.sleep(200);

        // Send Read request
        IRegistration registration = server.getRegistrationService().getByEndpoint(client.getEndpointName());
        ErrorCallback errorCallback = mock(ErrorCallback.class);
        @SuppressWarnings("unchecked")
        ResponseCallback<ReadCompositeResponse> reponseCallback = mock(ResponseCallback.class);

        server.send(registration,
                new ReadCompositeRequest(ContentFormat.SENML_JSON, ContentFormat.SENML_JSON,
                        Arrays.asList("/d01/3/0/0", "/d01/3/0/1", "/d02/3/0/0", "/d02/3/0/1")),
                reponseCallback, errorCallback);

        client.expectRequest().storeBoth("A").code(Code.FETCH).go();

        // Send Response
        StringBuilder json = new StringBuilder();
        json.append("[\n");
        json.append("  {\"bn\":\"/d01/3/0/\",\"n\":\"0\", \"vs\":\"Manufacturer1\"},\n");
        json.append("  {\"n\":\"1\",\"vs\":\"device1\"},\n");
        json.append("  {\"bn\":\"/d02/3/0/\",\"n\":\"0\", \"vs\":\"Manufacturer2\"},\n");
        json.append("  {\"n\":\"1\",\"vs\":\"device2\"}\n");
        json.append("]");

        token = new Token(Token.createBytes(new Random(), 8));
        int mid = 200;
        client.sendResponse(Type.ACK, ResponseCode.CONTENT).payload(json.toString(), ContentFormat.SENML_JSON_CODE)
                .loadBoth("A").go();

        // check result
        @SuppressWarnings("unchecked")
        ArgumentCaptor<ReadCompositeResponse> responseCaptor = ArgumentCaptor.forClass(ReadCompositeResponse.class);
        verify(reponseCallback, timeout((500000000)).times(1)).onResponse(responseCaptor.capture());

        Map<PrefixedLwM2mPath, LwM2mNode> data = responseCaptor.getValue().getContent();
        assertThat(data).hasSize(4);

        assertThat(data).containsEntry( //
                PrefixedLwM2mPath.fromString("/d01/3/0/0"), LwM2mSingleResource.newStringResource(0, "Manufacturer1"));
        assertThat(data).containsEntry( //
                PrefixedLwM2mPath.fromString("/d01/3/0/1"), LwM2mSingleResource.newStringResource(1, "device1"));
        assertThat(data).containsEntry( //
                PrefixedLwM2mPath.fromString("/d02/3/0/0"), LwM2mSingleResource.newStringResource(0, "Manufacturer2"));
        assertThat(data).containsEntry( //
                PrefixedLwM2mPath.fromString("/d02/3/0/1"), LwM2mSingleResource.newStringResource(1, "device2"));
    }

    @TestAllTransportLayer
    public void register_composite_write(String givenServerEndpointProvider) throws Exception {

        // Register client
        LockStepLwM2mClient client = new LockStepLwM2mClient(server.getEndpoint(Protocol.COAP).getURI());
        Token token = client.sendLwM2mRequest(
                new RegisterRequest(client.getEndpointName(), 60l, "1.1", EnumSet.of(BindingMode.U), null, null,
                        linkParser.parseCoreLinkFormat("</1>,</2>,</3>,</25>;ver=2.0,</25/0>".getBytes()), null));
        client.startMultiExpectation();
        client.expectResponse().token(token).code(ResponseCode.CREATED).go();

        // wait for object 25 read
        client.expectRequest().storeBoth("A").code(Code.GET).go();
        client.goMultiExpectation();
        LwM2mObject object = new LwM2mObject(25, //
                new LwM2mObjectInstance(0, //
                        LwM2mSingleResource.newStringResource(0, "enddevice1"), //
                        LwM2mSingleResource.newStringResource(1, "d01"), //
                        LwM2mSingleResource.newCoreLinkResource(3,
                                new DefaultLwM2mLinkParser()
                                        .parseLwM2mLinkFromCoreLinkFormat("</3>;ver=1.2,</3/0>".getBytes(), null))),
                new LwM2mObjectInstance(1, //
                        LwM2mSingleResource.newStringResource(0, "enddevice2"), //
                        LwM2mSingleResource.newStringResource(1, "d02"), //
                        LwM2mSingleResource.newCoreLinkResource(3, new DefaultLwM2mLinkParser()
                                .parseLwM2mLinkFromCoreLinkFormat("</3>;ver=1.2,</3/0>".getBytes(), null))));
        byte[] payload = new DefaultLwM2mEncoder().encode(object, ContentFormat.SENML_JSON, null, new LwM2mPath(25),
                client.getLwM2mModel());
        client.sendResponse(Type.ACK, ResponseCode.CONTENT).loadBoth("A")
                .payload(payload, ContentFormat.SENML_JSON_CODE).go();

        // TODO Gateway : This sleep is needed because SEND Request could be received before we handle the Read Response
        // about Gateway object and so registration is not ready.
        // Is it something we should handle in real use case (sightly delay Send Handling waiting we get child data?)
        Thread.sleep(200);

        // Send Read request
        IRegistration registration = server.getRegistrationService().getByEndpoint(client.getEndpointName());
        ErrorCallback errorCallback = mock(ErrorCallback.class);
        @SuppressWarnings("unchecked")
        ResponseCallback<WriteCompositeResponse> reponseCallback = mock(ResponseCallback.class);

        HashMap<PrefixedLwM2mPath, LwM2mNode> dataToWrite = new HashMap<>(); //
        dataToWrite.put(PrefixedLwM2mPath.fromString("/d01/3/0/0"),
                LwM2mSingleResource.newStringResource(0, "Manufacturer1"));
        dataToWrite.put(PrefixedLwM2mPath.fromString("/d01/3/0/1"),
                LwM2mSingleResource.newStringResource(1, "device1"));
        dataToWrite.put(PrefixedLwM2mPath.fromString("/d02/3/0/0"),
                LwM2mSingleResource.newStringResource(0, "Manufacturer2"));
        dataToWrite.put(PrefixedLwM2mPath.fromString("/d02/3/0/1"),
                LwM2mSingleResource.newStringResource(1, "device2"));

        server.send(registration, new WriteCompositeRequest(ContentFormat.SENML_JSON, dataToWrite, null),
                reponseCallback, errorCallback);

        Request request = client.expectRequest().storeBoth("A").code(Code.IPATCH).go();

        LwM2mNodeSenMLDecoder decoder = new LwM2mNodeSenMLDecoder(new SenMLJsonJacksonEncoderDecoder(), false);
        TimestampedLwM2mNodes nodes = decoder.decodeTimestampedNodes(request.getPayload(), null, null,
                server.getModelProvider().getObjectModel(registration, server.getRegistrationStore()));

        assertThat(nodes.getPrefixedMostRecentNodes()).containsExactlyEntriesOf(dataToWrite);

        // Send Response
        StringBuilder json = new StringBuilder();
        json.append("[\n");
        json.append("  {\"bn\":\"/d01/3/0/\",\"n\":\"0\", \"vs\":\"Manufacturer1\"},\n");
        json.append("  {\"n\":\"1\",\"vs\":\"device1\"},\n");
        json.append("  {\"bn\":\"/d02/3/0/\",\"n\":\"0\", \"vs\":\"Manufacturer2\"},\n");
        json.append("  {\"n\":\"1\",\"vs\":\"device2\"}\n");
        json.append("]");

        int mid = 200;
        client.sendResponse(Type.ACK, ResponseCode.CHANGED).loadBoth("A").go();

        // check result
        @SuppressWarnings("unchecked")
        ArgumentCaptor<WriteCompositeResponse> responseCaptor = ArgumentCaptor.forClass(WriteCompositeResponse.class);
        verify(reponseCallback, timeout((500000000)).times(1)).onResponse(responseCaptor.capture());
    }

    @TestAllTransportLayer
    public void register_composite_observe(String givenServerEndpointProvider) throws Exception {

        // Register client
        LockStepLwM2mClient client = new LockStepLwM2mClient(server.getEndpoint(Protocol.COAP).getURI());
        Token token = client.sendLwM2mRequest(
                new RegisterRequest(client.getEndpointName(), 600l, "1.1", EnumSet.of(BindingMode.U), null, null,
                        linkParser.parseCoreLinkFormat("</1>,</2>,</3>,</25>;ver=2.0,</25/0>".getBytes()), null));
        client.startMultiExpectation();
        client.expectResponse().token(token).code(ResponseCode.CREATED).go();

        // wait for object 25 read
        client.expectRequest().storeBoth("A").code(Code.GET).go();
        client.goMultiExpectation();
        LwM2mObject object = new LwM2mObject(25, //
                new LwM2mObjectInstance(0, //
                        LwM2mSingleResource.newStringResource(0, "enddevice1"), //
                        LwM2mSingleResource.newStringResource(1, "d01"), //
                        LwM2mSingleResource.newCoreLinkResource(3,
                                new DefaultLwM2mLinkParser()
                                        .parseLwM2mLinkFromCoreLinkFormat("</3>;ver=1.2,</3/0>".getBytes(), null))),
                new LwM2mObjectInstance(1, //
                        LwM2mSingleResource.newStringResource(0, "enddevice2"), //
                        LwM2mSingleResource.newStringResource(1, "d02"), //
                        LwM2mSingleResource.newCoreLinkResource(3, new DefaultLwM2mLinkParser()
                                .parseLwM2mLinkFromCoreLinkFormat("</3>;ver=1.2,</3/0>".getBytes(), null))));
        byte[] payload = new DefaultLwM2mEncoder().encode(object, ContentFormat.SENML_JSON, null, new LwM2mPath(25),
                client.getLwM2mModel());
        client.sendResponse(Type.ACK, ResponseCode.CONTENT).loadBoth("A")
                .payload(payload, ContentFormat.SENML_JSON_CODE).go();

        // TODO Gateway : This sleep is needed because SEND Request could be received before we handle the Read Response
        // about Gateway object and so registration is not ready.
        // Is it something we should handle in real use case (sightly delay Send Handling waiting we get child data?)
        Thread.sleep(200);

        // Send Observe request
        IRegistration registration = server.getRegistrationService().getByEndpoint(client.getEndpointName());
        ErrorCallback errorCallback = mock(ErrorCallback.class);
        @SuppressWarnings("unchecked")
        ResponseCallback<ObserveCompositeResponse> reponseCallback = mock(ResponseCallback.class);

        server.send(registration, new ObserveCompositeRequest(ContentFormat.SENML_JSON, ContentFormat.SENML_JSON,
                "/d01/3/0/0", "/d01/3/0/1", "/d02/3/0/0", "/d02/3/0/1"), reponseCallback, errorCallback);

        client.expectRequest().storeBoth("A").code(Code.FETCH).go();

        // Send Response
        StringBuilder json = new StringBuilder();
        json.append("[\n");
        json.append("  {\"bn\":\"/d01/3/0/\",\"n\":\"0\", \"vs\":\"Manufacturer1\"},\n");
        json.append("  {\"n\":\"1\",\"vs\":\"device1\"},\n");
        json.append("  {\"bn\":\"/d02/3/0/\",\"n\":\"0\", \"vs\":\"Manufacturer2\"},\n");
        json.append("  {\"n\":\"1\",\"vs\":\"device2\"}\n");
        json.append("]");

        client.sendResponse(Type.ACK, ResponseCode.CONTENT).payload(json.toString(), ContentFormat.SENML_JSON_CODE)
                .loadBoth("A").observe(0).go();

        // check result
        @SuppressWarnings("unchecked")
        ArgumentCaptor<ObserveCompositeResponse> responseCaptor = ArgumentCaptor
                .forClass(ObserveCompositeResponse.class);
        verify(reponseCallback, timeout((500000000)).times(1)).onResponse(responseCaptor.capture());

        Map<PrefixedLwM2mPath, LwM2mNode> data = responseCaptor.getValue().getContent();
        assertThat(data).hasSize(4);

        assertThat(data).containsEntry( //
                PrefixedLwM2mPath.fromString("/d01/3/0/0"), LwM2mSingleResource.newStringResource(0, "Manufacturer1"));
        assertThat(data).containsEntry( //
                PrefixedLwM2mPath.fromString("/d01/3/0/1"), LwM2mSingleResource.newStringResource(1, "device1"));
        assertThat(data).containsEntry( //
                PrefixedLwM2mPath.fromString("/d02/3/0/0"), LwM2mSingleResource.newStringResource(0, "Manufacturer2"));
        assertThat(data).containsEntry( //
                PrefixedLwM2mPath.fromString("/d02/3/0/1"), LwM2mSingleResource.newStringResource(1, "device2"));

        server.waitForNewObservation(responseCaptor.getValue().getObservation());

        // send notification
        int mid = 200;
        client.sendResponse(Type.CON, ResponseCode.CONTENT).payload(json.toString(), ContentFormat.SENML_JSON_CODE)
                .loadToken("A").mid(mid).observe(2).go();
        ObserveCompositeResponse notif = server.waitForNotificationOf(responseCaptor.getValue().getObservation());

        data = notif.getContent();
        assertThat(data).hasSize(4);
        assertThat(data).containsEntry( //
                PrefixedLwM2mPath.fromString("/d01/3/0/0"), LwM2mSingleResource.newStringResource(0, "Manufacturer1"));
        assertThat(data).containsEntry( //
                PrefixedLwM2mPath.fromString("/d01/3/0/1"), LwM2mSingleResource.newStringResource(1, "device1"));
        assertThat(data).containsEntry( //
                PrefixedLwM2mPath.fromString("/d02/3/0/0"), LwM2mSingleResource.newStringResource(0, "Manufacturer2"));
        assertThat(data).containsEntry( //
                PrefixedLwM2mPath.fromString("/d02/3/0/1"), LwM2mSingleResource.newStringResource(1, "device2"));

        client.expectEmpty(Type.ACK, mid).go();

        // send cancel notification
        @SuppressWarnings("unchecked")
        ResponseCallback<CancelCompositeObservationResponse> cancelReponseCallback = mock(ResponseCallback.class);

        server.send(registration, new CancelCompositeObservationRequest(responseCaptor.getValue().getObservation()),
                cancelReponseCallback, errorCallback);

        client.expectRequest().storeBoth("C").code(Code.FETCH).go();

        // Send Response
        client.sendResponse(Type.ACK, ResponseCode.CONTENT).payload(json.toString(), ContentFormat.SENML_JSON_CODE)
                .loadBoth("C").go();

        // check result
        @SuppressWarnings("unchecked")
        ArgumentCaptor<CancelCompositeObservationResponse> cancelResponseCaptor = ArgumentCaptor
                .forClass(CancelCompositeObservationResponse.class);
        verify(cancelReponseCallback, timeout((500000000)).times(1)).onResponse(cancelResponseCaptor.capture());

        data = cancelResponseCaptor.getValue().getContent();
        assertThat(data).hasSize(4);

        assertThat(data).containsEntry( //
                PrefixedLwM2mPath.fromString("/d01/3/0/0"), LwM2mSingleResource.newStringResource(0, "Manufacturer1"));
        assertThat(data).containsEntry( //
                PrefixedLwM2mPath.fromString("/d01/3/0/1"), LwM2mSingleResource.newStringResource(1, "device1"));
        assertThat(data).containsEntry( //
                PrefixedLwM2mPath.fromString("/d02/3/0/0"), LwM2mSingleResource.newStringResource(0, "Manufacturer2"));
        assertThat(data).containsEntry( //
                PrefixedLwM2mPath.fromString("/d02/3/0/1"), LwM2mSingleResource.newStringResource(1, "device2"));
    }
}
