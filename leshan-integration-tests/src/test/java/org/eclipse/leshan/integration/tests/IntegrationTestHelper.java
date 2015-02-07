/*******************************************************************************
 * Copyright (c) 2013-2015 Sierra Wireless and others.
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v1.0 which accompany this distribution.
 * 
 * The Eclipse Public License is available at
 *    http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 *    http://www.eclipse.org/org/documents/edl-v10.html.
 * 
 * Contributors:
 *     Zebra Technologies - initial API and implementation
 *******************************************************************************/

package org.eclipse.leshan.integration.tests;

import static org.junit.Assert.assertEquals;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.eclipse.californium.core.WebLink;
import org.eclipse.californium.core.coap.LinkFormat;
import org.eclipse.leshan.ObserveSpec;
import org.eclipse.leshan.ResponseCode;
import org.eclipse.leshan.bootstrap.BootstrapStoreImpl;
import org.eclipse.leshan.client.LwM2mClient;
import org.eclipse.leshan.client.californium.LeshanClient;
import org.eclipse.leshan.client.exchange.LwM2mExchange;
import org.eclipse.leshan.client.request.AbstractLwM2mClientRequest;
import org.eclipse.leshan.client.request.BootstrapRequest;
import org.eclipse.leshan.client.request.DeregisterRequest;
import org.eclipse.leshan.client.request.LwM2mClientRequest;
import org.eclipse.leshan.client.request.RegisterRequest;
import org.eclipse.leshan.client.request.UpdateRequest;
import org.eclipse.leshan.client.request.identifier.ClientIdentifier;
import org.eclipse.leshan.client.resource.LwM2mClientObjectDefinition;
import org.eclipse.leshan.client.resource.SingleResourceDefinition;
import org.eclipse.leshan.client.resource.integer.IntegerLwM2mExchange;
import org.eclipse.leshan.client.resource.integer.IntegerLwM2mResource;
import org.eclipse.leshan.client.resource.multiple.MultipleLwM2mExchange;
import org.eclipse.leshan.client.resource.multiple.MultipleLwM2mResource;
import org.eclipse.leshan.client.resource.string.StringLwM2mExchange;
import org.eclipse.leshan.client.resource.string.StringLwM2mResource;
import org.eclipse.leshan.client.response.ExecuteResponse;
import org.eclipse.leshan.client.response.OperationResponse;
import org.eclipse.leshan.client.util.ResponseCallback;
import org.eclipse.leshan.core.node.LwM2mNode;
import org.eclipse.leshan.core.node.LwM2mObjectInstance;
import org.eclipse.leshan.core.node.LwM2mResource;
import org.eclipse.leshan.core.node.Value;
import org.eclipse.leshan.core.request.ContentFormat;
import org.eclipse.leshan.core.request.CreateRequest;
import org.eclipse.leshan.core.request.DeleteRequest;
import org.eclipse.leshan.core.request.DiscoverRequest;
import org.eclipse.leshan.core.request.ObserveRequest;
import org.eclipse.leshan.core.request.ReadRequest;
import org.eclipse.leshan.core.request.WriteAttributesRequest;
import org.eclipse.leshan.core.request.WriteRequest;
import org.eclipse.leshan.core.response.CreateResponse;
import org.eclipse.leshan.core.response.DiscoverResponse;
import org.eclipse.leshan.core.response.LwM2mResponse;
import org.eclipse.leshan.core.response.ValueResponse;
import org.eclipse.leshan.server.LwM2mServer;
import org.eclipse.leshan.server.californium.LeshanServerBuilder;
import org.eclipse.leshan.server.californium.impl.LwM2mBootstrapServerImpl;
import org.eclipse.leshan.server.client.Client;
import org.eclipse.leshan.server.client.ClientRegistry;
import org.eclipse.leshan.server.impl.SecurityRegistryImpl;
import org.eclipse.leshan.server.observation.ObservationRegistry;

/**
 * Helper for running a server and executing a client against it.
 * 
 */
public final class IntegrationTestHelper {

    static final int GOOD_OBJECT_ID = 100;
    static final int GOOD_OBJECT_INSTANCE_ID = 0;
    static final int FIRST_RESOURCE_ID = 4;
    static final int SECOND_RESOURCE_ID = 5;
    static final int EXECUTABLE_RESOURCE_ID = 6;
    static final int INVALID_RESOURCE_ID = 9;

    static final int BROKEN_OBJECT_ID = GOOD_OBJECT_ID + 1;
    static final int BROKEN_RESOURCE_ID = 7;

    static final int MULTIPLE_OBJECT_ID = GOOD_OBJECT_ID + 2;
    static final int MULTIPLE_RESOURCE_ID = 0;

    static final int INT_OBJECT_ID = GOOD_OBJECT_ID + 3;
    static final int INT_RESOURCE_ID = 0;

    static final int MANDATORY_MULTIPLE_OBJECT_ID = GOOD_OBJECT_ID + 4;
    static final int MANDATORY_MULTIPLE_RESOURCE_ID = 0;

    static final int MANDATORY_SINGLE_OBJECT_ID = GOOD_OBJECT_ID + 5;
    static final int MANDATORY_SINGLE_RESOURCE_ID = 0;

    static final int OPTIONAL_SINGLE_OBJECT_ID = GOOD_OBJECT_ID + 6;
    static final int OPTIONAL_SINGLE_RESOURCE_ID = 0;

    static final int BAD_OBJECT_ID = 1000;
    static final String ENDPOINT_IDENTIFIER = "kdfflwmtm";
    static final int CLIENT_PORT = 44022;
    static final int TIMEOUT_MS = 5000;
    private final String clientDataModel = "</lwm2m>;rt=\"oma.lwm2m\", </lwm2m/1/101>, </lwm2m/1/102>, </lwm2m/2/0>, </lwm2m/2/1>, </lwm2m/2/2>, </lwm2m/3/0>, </lwm2m/4/0>, </lwm2m/5>";

    LwM2mServer server;
    private final ClientRegistry clientRegistry;

    Map<String, String> clientParameters = new HashMap<>();
    LwM2mClient client;
    ExecutableResource executableResource;
    ValueResource firstResource;
    ValueResource secondResource;
    MultipleResource multipleResource;
    IntValueResource intResource;
    ObservationRegistry observationRegistry;

    Set<WebLink> objectsAndInstances = LinkFormat.parse(clientDataModel);

    final InetSocketAddress serverAddress = new InetSocketAddress(InetAddress.getLoopbackAddress(), 5683);
    final InetSocketAddress clientAddress = new InetSocketAddress(InetAddress.getLoopbackAddress(), CLIENT_PORT);
    private LwM2mBootstrapServerImpl bootstrapServer;

    public IntegrationTestHelper() {
        this(false);
    }

    public IntegrationTestHelper(final boolean startBootstrap) {
        final InetSocketAddress serverAddressSecure = new InetSocketAddress(InetAddress.getLoopbackAddress(), 5684);
        server = new LeshanServerBuilder().setLocalAddress(serverAddress).setLocalAddressSecure(serverAddressSecure)
                .build();
        clientRegistry = server.getClientRegistry();
        observationRegistry = server.getObservationRegistry();

        firstResource = new ValueResource();
        secondResource = new ValueResource();
        executableResource = new ExecutableResource();
        multipleResource = new MultipleResource();
        intResource = new IntValueResource();

        if (startBootstrap) {
            startBootstrapServer();
            client = createClient(serverAddressSecure);
        } else {
            server.start();
            client = createClient(serverAddress);
        }
    }

    private void startBootstrapServer() {
        bootstrapServer = new LwM2mBootstrapServerImpl(new BootstrapStoreImpl(), new SecurityRegistryImpl());
        bootstrapServer.start();
    }

    LwM2mClient createClient(final InetSocketAddress serverAddress) {
        final ReadWriteListenerWithBrokenWrite brokenResourceListener = new ReadWriteListenerWithBrokenWrite();

        final boolean single = true;
        final boolean mandatory = true;

        final LwM2mClientObjectDefinition objectOne = new LwM2mClientObjectDefinition(GOOD_OBJECT_ID, !mandatory,
                !single, new SingleResourceDefinition(FIRST_RESOURCE_ID, firstResource, mandatory),
                new SingleResourceDefinition(SECOND_RESOURCE_ID, secondResource, mandatory),
                new SingleResourceDefinition(EXECUTABLE_RESOURCE_ID, executableResource, !mandatory));
        final LwM2mClientObjectDefinition objectTwo = new LwM2mClientObjectDefinition(BROKEN_OBJECT_ID, !mandatory,
                !single, new SingleResourceDefinition(BROKEN_RESOURCE_ID, brokenResourceListener, mandatory));
        final LwM2mClientObjectDefinition objectThree = new LwM2mClientObjectDefinition(MULTIPLE_OBJECT_ID, !mandatory,
                !single, new SingleResourceDefinition(MULTIPLE_RESOURCE_ID, multipleResource, !mandatory));
        final LwM2mClientObjectDefinition objectFour = new LwM2mClientObjectDefinition(INT_OBJECT_ID, !mandatory,
                !single, new SingleResourceDefinition(INT_RESOURCE_ID, intResource, !mandatory));
        final LwM2mClientObjectDefinition mandatoryMultipleObject = new LwM2mClientObjectDefinition(
                MANDATORY_MULTIPLE_OBJECT_ID, mandatory, !single, new SingleResourceDefinition(
                        MANDATORY_MULTIPLE_RESOURCE_ID, intResource, !mandatory));
        final LwM2mClientObjectDefinition mandatorySingleObject = new LwM2mClientObjectDefinition(
                MANDATORY_SINGLE_OBJECT_ID, mandatory, single, new SingleResourceDefinition(
                        MANDATORY_SINGLE_RESOURCE_ID, intResource, mandatory));
        final LwM2mClientObjectDefinition optionalSingleObject = new LwM2mClientObjectDefinition(
                OPTIONAL_SINGLE_OBJECT_ID, !mandatory, single, new SingleResourceDefinition(
                        OPTIONAL_SINGLE_RESOURCE_ID, intResource, !mandatory));
        return new LeshanClient(clientAddress, serverAddress, objectOne, objectTwo, objectThree, objectFour,
                mandatoryMultipleObject, mandatorySingleObject, optionalSingleObject);
    }

    public void stop() {
        client.stop();
        server.stop();
        if (bootstrapServer != null) {
            bootstrapServer.stop();
        }
    }

    private LwM2mClientRequest createRegisterRequest() {
        final RegisterRequest registerRequest = new RegisterRequest(ENDPOINT_IDENTIFIER, clientParameters);

        return registerRequest;
    }

    private LwM2mClientRequest createDeregisterRequest(final ClientIdentifier clientIdentifier) {
        final DeregisterRequest deregisterRequest = new DeregisterRequest(clientIdentifier);

        return deregisterRequest;
    }

    private LwM2mClientRequest createBoostrapRequest() {
        return new BootstrapRequest(ENDPOINT_IDENTIFIER);
    }

    public OperationResponse bootstrap() {
        client.start();

        final LwM2mClientRequest boostrapRequest = createBoostrapRequest();
        final OperationResponse response = client.send(boostrapRequest);
        return response;
    }

    public OperationResponse register() {
        client.start();
        final LwM2mClientRequest registerRequest = createRegisterRequest();
        final OperationResponse response = client.send(registerRequest);
        return response;
    }

    public void register(final ResponseCallback callback) {
        client.start();
        final LwM2mClientRequest registerRequest = createRegisterRequest();
        client.send(registerRequest, callback);
    }

    public OperationResponse update(final ClientIdentifier clientIdentifier, final Map<String, String> updatedParameters) {
        final AbstractLwM2mClientRequest updaterRequest = new UpdateRequest(clientIdentifier, updatedParameters);

        return client.send(updaterRequest);
    }

    public void deregister(final ClientIdentifier clientIdentifier, final ResponseCallback callback) {
        final LwM2mClientRequest deregisterRequest = createDeregisterRequest(clientIdentifier);
        client.send(deregisterRequest, callback);
    }

    static LwM2mObjectInstance createGoodObjectInstance(final String value0, final String value1) {
        return new LwM2mObjectInstance(GOOD_OBJECT_INSTANCE_ID, new LwM2mResource[] {
                                new LwM2mResource(FIRST_RESOURCE_ID, Value.newStringValue(value0)),
                                new LwM2mResource(SECOND_RESOURCE_ID, Value.newStringValue(value1)) });
    }

    ValueResponse sendRead(final int objectId) {
        return server.send(getClient(), new ReadRequest(objectId));
    }

    ValueResponse sendRead(final int objectId, final int objectInstanceId) {
        return server.send(getClient(), new ReadRequest(objectId, objectInstanceId));
    }

    ValueResponse sendRead(final int objectId, final int objectInstanceId, final int resourceId) {
        return server.send(getClient(), new ReadRequest(objectId, objectInstanceId, resourceId));
    }

    ValueResponse sendObserve(final int objectId) {
        return server.send(getClient(), new ObserveRequest(objectId));
    }

    ValueResponse sendObserve(final int objectId, final int objectInstanceId) {
        return server.send(getClient(), new ObserveRequest(objectId, objectInstanceId));
    }

    ValueResponse sendObserve(final int objectId, final int objectInstanceId, final int resourceId) {
        return server.send(getClient(), new ObserveRequest(objectId, objectInstanceId, resourceId));
    }

    DiscoverResponse sendDiscover(final int objectId) {
        return server.send(getClient(), new DiscoverRequest(objectId));
    }

    DiscoverResponse sendDiscover(final int objectId, final int objectInstanceId) {
        return server.send(getClient(), new DiscoverRequest(objectId, objectInstanceId));
    }

    DiscoverResponse sendDiscover(final int objectId, final int objectInstanceId, final int resourceId) {
        return server.send(getClient(), new DiscoverRequest(objectId, objectInstanceId, resourceId));
    }

    CreateResponse sendCreate(final LwM2mObjectInstance instance, final int objectId) {
        return server.send(getClient(), new CreateRequest(objectId, instance, ContentFormat.TLV));
    }

    CreateResponse sendCreate(final LwM2mObjectInstance instance, final int objectId, final int objectInstanceId) {
        return server.send(getClient(), new CreateRequest(objectId, objectInstanceId, instance, ContentFormat.TLV));
    }

    LwM2mResponse sendDelete(final int objectId, final int objectInstanceId) {
        return server.send(getClient(), new DeleteRequest(objectId, objectInstanceId));
    }

    LwM2mResponse sendUpdate(final LwM2mResource resource, final int objectId, final int objectInstanceId,
            final int resourceId) {
        final boolean isReplace = true;
        return server.send(getClient(), new WriteRequest(objectId, objectInstanceId, resourceId, resource,
                ContentFormat.TEXT, !isReplace));
    }

    LwM2mResponse sendUpdate(final String payload, final int objectId, final int objectInstanceId, final int resourceId) {
        final boolean isReplace = true;
        final LwM2mNode resource = new LwM2mResource(resourceId, Value.newStringValue(payload));
        return server.send(getClient(), new WriteRequest(objectId, objectInstanceId, resourceId, resource,
                ContentFormat.TEXT, !isReplace));
    }

    LwM2mResponse sendReplace(final LwM2mResource resource, final int objectId, final int objectInstanceId,
            final int resourceId) {
        final boolean isReplace = true;
        return server.send(getClient(), new WriteRequest(objectId, objectInstanceId, resourceId, resource,
                ContentFormat.TEXT, isReplace));
    }

    LwM2mResponse sendReplace(final String payload, final int objectId, final int objectInstanceId, final int resourceId) {
        final boolean isReplace = true;
        final LwM2mNode resource = new LwM2mResource(resourceId, Value.newStringValue(payload));
        return server.send(getClient(), new WriteRequest(objectId, objectInstanceId, resourceId, resource,
                ContentFormat.TEXT, isReplace));
    }

    LwM2mResponse sendWriteAttributes(final ObserveSpec observeSpec, final int objectId) {
        return server.send(getClient(), new WriteAttributesRequest(objectId, observeSpec));
    }

    LwM2mResponse sendWriteAttributes(final ObserveSpec observeSpec, final int objectId, final int objectInstanceId) {
        return server.send(getClient(), new WriteAttributesRequest(objectId, objectInstanceId, observeSpec));
    }

    LwM2mResponse sendWriteAttributes(final ObserveSpec observeSpec, final int objectId, final int objectInstanceId,
            final int resourceId) {
        return server
                .send(getClient(), new WriteAttributesRequest(objectId, objectInstanceId, resourceId, observeSpec));
    }

    Client getClient() {
        return clientRegistry.get(ENDPOINT_IDENTIFIER);
    }

    static void assertResponse(final ValueResponse response, final ResponseCode expectedCode,
            final LwM2mNode expectedContent) {
        assertEquals(expectedCode, response.getCode());
        assertEquals(expectedContent, response.getContent());
    }

    static void assertEmptyResponse(final LwM2mResponse response, final ResponseCode responseCode) {
        assertEquals(responseCode, response.getCode());
    }

    public class ValueResource extends StringLwM2mResource {

        private String value = "blergs";

        public void setValue(final String newValue) {
            value = newValue;
            notifyResourceUpdated();
        }

        public String getValue() {
            return value;
        }

        @Override
        public void handleWrite(final StringLwM2mExchange exchange) {
            setValue(exchange.getRequestPayload());

            exchange.respondSuccess();
        }

        @Override
        public void handleRead(final StringLwM2mExchange exchange) {
            exchange.respondContent(value);
        }

    }

    public class IntValueResource extends IntegerLwM2mResource {

        private int value = 0;

        public void setValue(final int newValue) {
            value = newValue;
            notifyResourceUpdated();
        }

        public int getValue() {
            return value;
        }

        @Override
        public void handleWrite(final IntegerLwM2mExchange exchange) {
            setValue(exchange.getRequestPayload());

            exchange.respondSuccess();
        }

        @Override
        public void handleRead(final IntegerLwM2mExchange exchange) {
            exchange.respondContent(value);
        }

    }

    public class ReadWriteListenerWithBrokenWrite extends StringLwM2mResource {

        private String value;

        @Override
        public void handleWrite(final StringLwM2mExchange exchange) {
            if (value == null) {
                value = exchange.getRequestPayload();
                exchange.respondSuccess();
            } else {
                exchange.respondFailure();
            }
        }

        @Override
        public void handleRead(final StringLwM2mExchange exchange) {
            exchange.respondContent(value);
        }

    }

    public class ExecutableResource extends StringLwM2mResource {

        @Override
        public void handleExecute(final LwM2mExchange exchange) {
            exchange.respond(ExecuteResponse.success());
        }

    }

    public class MultipleResource extends MultipleLwM2mResource {

        private Map<Integer, byte[]> value;

        public void setValue(final Map<Integer, byte[]> initialValue) {
            this.value = initialValue;
        }

        @Override
        public void handleRead(final MultipleLwM2mExchange exchange) {
            exchange.respondContent(value);
        }

        @Override
        public void handleWrite(final MultipleLwM2mExchange exchange) {
            this.value = exchange.getRequestPayload();
            exchange.respondSuccess();
        }

    }

}
