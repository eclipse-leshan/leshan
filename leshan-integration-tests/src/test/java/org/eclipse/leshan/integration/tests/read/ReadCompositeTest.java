/*******************************************************************************
 * Copyright (c) 2021 Sierra Wireless and others.
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
package org.eclipse.leshan.integration.tests.read;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.leshan.core.ResponseCode.CONTENT;
import static org.eclipse.leshan.integration.tests.util.LeshanTestClientBuilder.givenClientUsing;
import static org.eclipse.leshan.integration.tests.util.LeshanTestServerBuilder.givenServerUsing;
import static org.eclipse.leshan.integration.tests.util.assertion.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.net.InetSocketAddress;
import java.util.*;
import java.util.stream.Stream;

import org.eclipse.leshan.core.LwM2m;
import org.eclipse.leshan.core.endpoint.EndpointUriUtil;
import org.eclipse.leshan.core.endpoint.Protocol;
import org.eclipse.leshan.core.link.Link;
import org.eclipse.leshan.core.link.LinkParseException;
import org.eclipse.leshan.core.link.LinkParser;
import org.eclipse.leshan.core.link.lwm2m.DefaultLwM2mLinkParser;
import org.eclipse.leshan.core.model.ResourceModel.Type;
import org.eclipse.leshan.core.node.*;
import org.eclipse.leshan.core.peer.IpPeer;
import org.eclipse.leshan.core.request.ContentFormat;
import org.eclipse.leshan.core.request.ReadCompositeRequest;
import org.eclipse.leshan.core.response.ReadCompositeResponse;
import org.eclipse.leshan.core.util.TestLwM2mId;
import org.eclipse.leshan.integration.tests.util.LeshanTestClient;
import org.eclipse.leshan.integration.tests.util.LeshanTestServer;
import org.eclipse.leshan.integration.tests.util.junit5.extensions.ArgumentsUtil;
import org.eclipse.leshan.integration.tests.util.junit5.extensions.BeforeEachParameterizedResolver;
import org.eclipse.leshan.server.registration.DefaultRegistrationDataExtractor;
import org.eclipse.leshan.server.registration.Registration;
import org.eclipse.leshan.server.registration.RegistrationDataExtractor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@ExtendWith(BeforeEachParameterizedResolver.class)
public class ReadCompositeTest {

    private final LinkParser linkParser = new DefaultLwM2mLinkParser();

    /*---------------------------------/
     *  Parameterized Tests
     * -------------------------------*/
    @ParameterizedTest(name = "requested {0} response {1} over {2} - Client using {3} - Server using {4}")
    @MethodSource("transports")
    @Retention(RetentionPolicy.RUNTIME)
    private @interface TestAllCases {
    }

    static Stream<Arguments> transports() {

        Object[][] transports = new Object[][] {
                // ProtocolUsed - Client Endpoint Provider - Server Endpoint Provider
                { Protocol.COAP, "Californium", "Californium" }, //
                { Protocol.COAP, "Californium", "java-coap" }, //
                { Protocol.COAP, "java-coap", "Californium" }, //
                { Protocol.COAP, "java-coap", "java-coap" } };

        Object[][] contentFormats = new Object[][] { //
                // {request content format, response content format}
                { ContentFormat.SENML_JSON, ContentFormat.SENML_JSON }, //
                { ContentFormat.SENML_CBOR, ContentFormat.SENML_CBOR }, //
                { ContentFormat.SENML_CBOR, ContentFormat.SENML_JSON }, //
                { ContentFormat.SENML_JSON, ContentFormat.SENML_CBOR }, //
        };

        // for each transport, create 1 test by format.
        return Stream.of(ArgumentsUtil.combine(contentFormats, transports));
    }

    /*---------------------------------/
     *  Set-up and Tear-down Tests
     * -------------------------------*/

    LeshanTestServer server;
    LeshanTestClient client;
    Registration currentRegistration;

    @BeforeEach
    public void start(ContentFormat requestContentFormat, ContentFormat responseContentFormat, Protocol givenProtocol,
            String givenClientEndpointProvider, String givenServerEndpointProvider) {
        server = givenServerUsing(givenProtocol).with(givenServerEndpointProvider).build();
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

    @TestAllCases
    public void can_read_root(ContentFormat requestContentFormat, ContentFormat responseContentFormat,
            Protocol givenProtocol, String givenClientEndpointProvider, String givenServerEndpointProvider)
            throws InterruptedException {
        // read root object
        ReadCompositeResponse response = server.send(currentRegistration,
                new ReadCompositeRequest(requestContentFormat, responseContentFormat, "/"));

        // verify result
        // assertEquals(CONTENT, response.getCode());
        // assertContentFormat(responseContentFormat, response);

        assertThat(response) //
                .hasCode(CONTENT) //
                .hasCode(response.getCode()).hasContentFormat(responseContentFormat, givenServerEndpointProvider);

        LwM2mRoot root = (LwM2mRoot) response.getContent("/");
        Set<Integer> objectIdsFromRootObject = root.getObjects().keySet();
        Set<Integer> objectIdsFromObjectTree = new HashSet<>(Arrays.asList(1, 3, 3442));

        // assertEquals(objectIdsFromObjectTree, objectIdsFromRootObject);
        assertThat(objectIdsFromObjectTree).isEqualTo(objectIdsFromRootObject);

    }

    @TestAllCases
    public void can_read_root_object(ContentFormat requestContentFormat, ContentFormat responseContentFormat,
            Protocol givenProtocol, String givenClientEndpointProvider, String givenServerEndpointProvider)
            throws InterruptedException, LinkParseException {

        Registration reg = registration("</1/0>,</3/0>");

        Map<Integer, LwM2m.Version> supportedObject = reg.getSupportedObject();
        assertEquals(2, supportedObject.size());
        assertEquals(LwM2m.Version.getDefault(), supportedObject.get(1));
        assertEquals(LwM2m.Version.getDefault(), supportedObject.get(3));

        // ensure available instances are correct
        Set<LwM2mPath> availableInstances = reg.getAvailableInstances();
        assertEquals(2, availableInstances.size());
        assertTrue(availableInstances.containsAll(Arrays.asList(new LwM2mPath(1, 0), new LwM2mPath(3, 0))));

        ReadCompositeResponse response = server.send(currentRegistration,
                new ReadCompositeRequest(requestContentFormat, responseContentFormat, "/1/0", "/3/0"));

        response.getContent().forEach((lwM2mPath, lwM2mNode) -> {
            assertThat(lwM2mPath.getObjectInstanceId()).isEqualTo(0);
        });
    }

    @TestAllCases
    public void can_read_resources(ContentFormat requestContentFormat, ContentFormat responseContentFormat,
            Protocol givenProtocol, String givenClientEndpointProvider, String givenServerEndpointProvider)
            throws InterruptedException {
        // read device model number
        ReadCompositeResponse response = server.send(currentRegistration,
                new ReadCompositeRequest(requestContentFormat, responseContentFormat, "/3/0/0", "/1/0/1"));

        // verify result
        assertThat(response) //
                .hasCode(CONTENT) //
                .hasContentFormat(responseContentFormat, givenServerEndpointProvider);

        LwM2mSingleResource resource = (LwM2mSingleResource) response.getContent("/3/0/0");
        assertThat(resource.getId()).isEqualTo(0);
        assertThat(resource.getType()).isEqualTo(Type.STRING);

        resource = (LwM2mSingleResource) response.getContent("/1/0/1");
        assertThat(resource.getId()).isEqualTo(1);
        assertThat(resource.getType()).isEqualTo(Type.INTEGER);
    }

    @TestAllCases
    public void can_read_resource_instance(ContentFormat requestContentFormat, ContentFormat responseContentFormat,
            Protocol givenProtocol, String givenClientEndpointProvider, String givenServerEndpointProvider)
            throws InterruptedException {
        // read resource instance
        String path = "/" + TestLwM2mId.TEST_OBJECT + "/0/" + TestLwM2mId.MULTIPLE_STRING_VALUE + "/0";
        ReadCompositeResponse response = server.send(currentRegistration,
                new ReadCompositeRequest(requestContentFormat, responseContentFormat, path));

        // verify result
        assertThat(response) //
                .hasCode(CONTENT) //
                .hasContentFormat(responseContentFormat, givenServerEndpointProvider);

        LwM2mResourceInstance resource = (LwM2mResourceInstance) response.getContent(path);
        assertThat(resource.getId()).isEqualTo(0);
        assertThat(resource.getType()).isEqualTo(Type.STRING);

    }

    @TestAllCases
    public void can_read_resource_and_instance(ContentFormat requestContentFormat, ContentFormat responseContentFormat,
            Protocol givenProtocol, String givenClientEndpointProvider, String givenServerEndpointProvider)
            throws InterruptedException {
        // read device model number
        ReadCompositeResponse response = server.send(currentRegistration,
                new ReadCompositeRequest(requestContentFormat, responseContentFormat, "/3/0/0", "/1"));

        // verify result
        assertThat(response) //
                .hasCode(CONTENT) //
                .hasContentFormat(responseContentFormat, givenServerEndpointProvider);

        LwM2mSingleResource resource = (LwM2mSingleResource) response.getContent("/3/0/0");
        assertThat(resource.getId()).isEqualTo(0);
        assertThat(resource.getType()).isEqualTo(Type.STRING);

        LwM2mObject object = (LwM2mObject) response.getContent("/1");
        assertThat(object.getId()).isEqualTo(1);
        assertThat(object.getInstances()).hasSize(1);
    }

    private Registration registration(String objectLinks) throws LinkParseException {
        Registration.Builder builder = new Registration.Builder("id", "endpoint",
                new IpPeer(InetSocketAddress.createUnresolved("localhost", 0)),
                EndpointUriUtil.createUri("coap://localhost:5683"));

        Link[] links = linkParser.parseCoreLinkFormat(objectLinks.getBytes());
        builder.objectLinks(links);

        RegistrationDataExtractor.RegistrationData dataFromObjectLinks = new DefaultRegistrationDataExtractor()
                .extractDataFromObjectLinks(links, LwM2m.LwM2mVersion.V1_0);
        builder.rootPath(dataFromObjectLinks.getAlternatePath());
        builder.supportedContentFormats(dataFromObjectLinks.getSupportedContentFormats());
        builder.supportedObjects(dataFromObjectLinks.getSupportedObjects());
        builder.availableInstances(dataFromObjectLinks.getAvailableInstances());

        return builder.build();
    }
}
