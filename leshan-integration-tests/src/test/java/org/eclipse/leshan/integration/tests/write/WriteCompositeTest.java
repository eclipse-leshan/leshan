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
package org.eclipse.leshan.integration.tests.write;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import org.eclipse.californium.core.coap.Response;
import org.eclipse.leshan.core.ResponseCode;
import org.eclipse.leshan.core.node.LwM2mMultipleResource;
import org.eclipse.leshan.core.node.LwM2mNode;
import org.eclipse.leshan.core.node.LwM2mObjectInstance;
import org.eclipse.leshan.core.node.LwM2mPath;
import org.eclipse.leshan.core.node.LwM2mResourceInstance;
import org.eclipse.leshan.core.node.LwM2mSingleResource;
import org.eclipse.leshan.core.observation.SingleObservation;
import org.eclipse.leshan.core.request.ContentFormat;
import org.eclipse.leshan.core.request.ObserveRequest;
import org.eclipse.leshan.core.request.ReadRequest;
import org.eclipse.leshan.core.request.WriteCompositeRequest;
import org.eclipse.leshan.core.response.LwM2mResponse;
import org.eclipse.leshan.core.response.ObserveResponse;
import org.eclipse.leshan.core.response.ReadResponse;
import org.eclipse.leshan.core.response.WriteCompositeResponse;
import org.eclipse.leshan.core.util.TestLwM2mId;
import org.eclipse.leshan.integration.tests.observe.TestObservationListener;
import org.eclipse.leshan.integration.tests.util.IntegrationTestHelper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

public class WriteCompositeTest {

    @ParameterizedTest(name = "{0}")
    @MethodSource("contentFormats")
    @Retention(RetentionPolicy.RUNTIME)
    private @interface TestAllContentFormat {
    }

    static Stream<ContentFormat> contentFormats() {
        return Stream.of(//
                ContentFormat.SENML_JSON, //
                ContentFormat.SENML_CBOR);
    }

    protected IntegrationTestHelper helper = new IntegrationTestHelper();

    @BeforeEach
    public void start() {
        helper.initialize();
        helper.createServer();
        helper.server.start();
        helper.createClient();
        helper.client.start();
        helper.waitForRegistrationAtServerSide(1);
    }

    @AfterEach
    public void stop() {
        helper.client.destroy(false);
        helper.server.destroy();
        helper.dispose();
    }

    @TestAllContentFormat
    public void can_write_resources(ContentFormat contentFormat) throws InterruptedException {
        // write device timezone and offset
        Map<String, Object> nodes = new HashMap<>();
        nodes.put("/3/0/14", "+02");
        nodes.put("/1/0/2", 100);

        WriteCompositeResponse response = helper.server.send(helper.getCurrentRegistration(),
                new WriteCompositeRequest(contentFormat, nodes));

        // verify result
        assertEquals(ResponseCode.CHANGED, response.getCode());
        assertNotNull(response.getCoapResponse());
        assertThat(response.getCoapResponse(), is(instanceOf(Response.class)));

        // read resource to check the value changed
        ReadResponse readResponse = helper.server.send(helper.getCurrentRegistration(), new ReadRequest(3, 0, 14));
        assertEquals("+02", ((LwM2mSingleResource) readResponse.getContent()).getValue());
        readResponse = helper.server.send(helper.getCurrentRegistration(), new ReadRequest(1, 0, 2));
        assertEquals(100l, ((LwM2mSingleResource) readResponse.getContent()).getValue());
    }

    @TestAllContentFormat
    public void can_write_resource_and_instance(ContentFormat contentFormat) throws InterruptedException {
        // create value
        LwM2mSingleResource utcOffset = LwM2mSingleResource.newStringResource(14, "+02");
        LwM2mPath resourceInstancePath = new LwM2mPath(TestLwM2mId.TEST_OBJECT, 0, TestLwM2mId.MULTIPLE_STRING_VALUE,
                0);
        LwM2mResourceInstance testStringResourceInstance = LwM2mResourceInstance
                .newStringInstance(resourceInstancePath.getResourceInstanceId(), "test_string_instance");

        // add it to the map
        Map<LwM2mPath, LwM2mNode> nodes = new HashMap<>();
        nodes.put(new LwM2mPath("/3/0/14"), utcOffset);
        nodes.put(resourceInstancePath, testStringResourceInstance);

        WriteCompositeResponse response = helper.server.send(helper.getCurrentRegistration(),
                new WriteCompositeRequest(contentFormat, nodes, null));

        // verify result
        assertEquals(ResponseCode.CHANGED, response.getCode());
        assertNotNull(response.getCoapResponse());
        assertThat(response.getCoapResponse(), is(instanceOf(Response.class)));

        // read resource to check the value changed
        ReadResponse readResponse = helper.server.send(helper.getCurrentRegistration(), new ReadRequest(3, 0, 14));
        assertEquals(utcOffset, readResponse.getContent());

        readResponse = helper.server.send(helper.getCurrentRegistration(),
                new ReadRequest(contentFormat, resourceInstancePath, null));
        assertEquals(testStringResourceInstance, readResponse.getContent());
    }

    @TestAllContentFormat
    public void can_add_resource_instances(ContentFormat contentFormat) throws InterruptedException {
        // Prepare node
        LwM2mPath resourceInstancePath = new LwM2mPath(TestLwM2mId.TEST_OBJECT, 0, TestLwM2mId.MULTIPLE_STRING_VALUE,
                1);
        LwM2mResourceInstance testStringResourceInstance = LwM2mResourceInstance
                .newStringInstance(resourceInstancePath.getResourceInstanceId(), "test_string_instance");
        Map<LwM2mPath, LwM2mNode> nodes = new HashMap<>();
        nodes.put(resourceInstancePath, testStringResourceInstance);

        // Write it
        WriteCompositeResponse response = helper.server.send(helper.getCurrentRegistration(),
                new WriteCompositeRequest(contentFormat, nodes, null));

        // verify result
        assertEquals(ResponseCode.CHANGED, response.getCode());
        assertNotNull(response.getCoapResponse());
        assertThat(response.getCoapResponse(), is(instanceOf(Response.class)));

        // read resource to check the value changed
        ReadResponse readResponse = helper.server.send(helper.getCurrentRegistration(),
                new ReadRequest(contentFormat, resourceInstancePath.toResourcePath(), null));
        LwM2mMultipleResource multiResource = (LwM2mMultipleResource) readResponse.getContent();
        assertEquals(2, multiResource.getInstances().size());
        assertEquals(testStringResourceInstance,
                multiResource.getInstance(resourceInstancePath.getResourceInstanceId()));
    }

    @TestAllContentFormat
    public void can_observe_instance_with_composite_write(ContentFormat contentFormat) throws InterruptedException {
        TestObservationListener listener = new TestObservationListener();
        helper.server.getObservationService().addListener(listener);

        // observe device instance
        ObserveResponse observeResponse = helper.server.send(helper.getCurrentRegistration(), new ObserveRequest(3, 0));
        assertEquals(ResponseCode.CONTENT, observeResponse.getCode());
        assertNotNull(observeResponse.getCoapResponse());
        assertThat(observeResponse.getCoapResponse(), is(instanceOf(Response.class)));

        // an observation response should have been sent
        SingleObservation observation = observeResponse.getObservation();
        assertEquals("/3/0", observation.getPath().toString());
        assertEquals(helper.getCurrentRegistration().getId(), observation.getRegistrationId());

        // write device timezone
        Map<String, Object> nodes = new HashMap<>();
        nodes.put("/3/0/14", "+11");
        nodes.put("/3/0/15", "Moon");

        LwM2mResponse writeResponse = helper.server.send(helper.getCurrentRegistration(),
                new WriteCompositeRequest(ContentFormat.SENML_CBOR, nodes));

        // verify result both resource must have new value
        listener.waitForNotification(1000);
        assertEquals(ResponseCode.CHANGED, writeResponse.getCode());
        assertTrue(listener.receivedNotify().get());
        assertTrue(listener.getObserveResponse().getContent() instanceof LwM2mObjectInstance);
        assertNotNull(listener.getObserveResponse().getCoapResponse());
        assertThat(listener.getObserveResponse().getCoapResponse(), is(instanceOf(Response.class)));

        LwM2mObjectInstance instance = (LwM2mObjectInstance) listener.getObserveResponse().getContent();
        assertEquals("+11", instance.getResource(14).getValue());
        assertEquals("Moon", instance.getResource(15).getValue());

        // Ensure we received only one notification.
        Thread.sleep(1000);// wait 1 second more to catch more notification ?
        assertEquals(1, listener.getNotificationCount());
    }
}
