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

import static org.eclipse.leshan.integration.tests.IntegrationTestHelper.*;
import static org.junit.Assert.*;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.eclipse.leshan.ObserveSpec;
import org.eclipse.leshan.ResponseCode;
import org.eclipse.leshan.core.node.LwM2mNode;
import org.eclipse.leshan.core.node.LwM2mObject;
import org.eclipse.leshan.core.node.LwM2mObjectInstance;
import org.eclipse.leshan.core.node.LwM2mResource;
import org.eclipse.leshan.core.node.Value;
import org.eclipse.leshan.core.response.ValueResponse;
import org.eclipse.leshan.server.observation.Observation;
import org.eclipse.leshan.server.observation.ObservationRegistryListener;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import com.jayway.awaitility.Awaitility;

public class ObserveTest {

    private SampleObservation observer;

    private IntegrationTestHelper helper = new IntegrationTestHelper();

    @After
    public void stop() {
        helper.stop();
    }

    @Before
    public void setupObservation() {
        observer = new SampleObservation();
        helper.observationRegistry.addListener(observer);

        helper.register();
        create();
    }

    @Test
    public void can_observe_resource() {
        observeResource();

        helper.intResource.setValue(2);
        assertObservedResource("2");
    }

    @Ignore
    @Test
    public void can_observe_resource_with_gt_with_notify() {
        observeResource(attributes().greaterThan(6));

        helper.intResource.setValue(20);
        assertObservedResource("20");
    }

    @Ignore
    @Test
    public void can_observe_resource_with_gt_no_notify() {
        observeResource(attributes().greaterThan(6));

        helper.intResource.setValue(2);
        assertNoObservation(500);
    }

    @Ignore
    @Test
    public void can_observe_resource_with_lt_with_notify() {
        observeResource(attributes().lessThan(6));

        helper.intResource.setValue(2);
        assertObservedResource("2");
    }

    @Ignore
    @Test
    public void can_observe_resource_with_lt_no_notify() {
        observeResource(attributes().lessThan(6));

        helper.intResource.setValue(20);
        assertNoObservation(500);
    }

    @Ignore
    @Test
    public void can_observe_resource_with_gt_and_lt_with_notify() {
        observeResource(attributes().greaterThan(10).lessThan(6));

        helper.intResource.setValue(20);
        assertObservedResource("20");
    }

    @Test
    public void can_observe_resource_with_pmax_with_notify() {
        observeResource(attributes().maxPeriod(1));

        assertObservedResource(2000, "0");
    }

    @Test
    public void can_observe_resource_with_pmax_no_notify() {
        observeResource(attributes().maxPeriod(1));

        assertNoObservation(500);
    }

    @Test
    public void can_observe_object_instance_with_pmax_with_notify() {
        observeObjectInstance(attributes().maxPeriod(1));

        helper.intResource.setValue(2);
        assertObservedObjectInstance(2000, "2");
    }

    @Test
    public void can_observe_object_with_pmax_with_notify() {
        observeObject(attributes().maxPeriod(1));

        helper.intResource.setValue(2);
        assertObservedObject(2000, "2");
    }

    private void create() {
        helper.sendCreate(new LwM2mObjectInstance(GOOD_OBJECT_INSTANCE_ID, new LwM2mResource[0]), INT_OBJECT_ID);
    }

    private ObserveSpec.Builder attributes() {
        return new ObserveSpec.Builder();
    }

    private void observeResource() {
        final ValueResponse response = helper.sendObserve(INT_OBJECT_ID, GOOD_OBJECT_INSTANCE_ID, INT_RESOURCE_ID);
        assertResponse(response, ResponseCode.CONTENT, new LwM2mResource(INT_RESOURCE_ID, Value.newStringValue("0")));
    }

    private void observeResource(final ObserveSpec.Builder observeSpecBuilder) {
        helper.sendWriteAttributes(observeSpecBuilder.build(), INT_OBJECT_ID, GOOD_OBJECT_INSTANCE_ID, INT_RESOURCE_ID);
        observeResource();
    }

    private void observeObjectInstance() {
        final ValueResponse response = helper.sendObserve(INT_OBJECT_ID, GOOD_OBJECT_INSTANCE_ID);
        assertResponse(response, ResponseCode.CONTENT, new LwM2mObjectInstance(GOOD_OBJECT_INSTANCE_ID,
                new LwM2mResource[] { new LwM2mResource(INT_RESOURCE_ID, Value.newBinaryValue("0".getBytes())) }));
    }

    private void observeObjectInstance(final ObserveSpec.Builder observeSpecBuilder) {
        helper.sendWriteAttributes(observeSpecBuilder.build(), INT_OBJECT_ID, GOOD_OBJECT_INSTANCE_ID);
        observeObjectInstance();
    }

    private void observeObject() {
        final ValueResponse response = helper.sendObserve(INT_OBJECT_ID);
        assertResponse(
                response,
                ResponseCode.CONTENT,
                new LwM2mObject(INT_OBJECT_ID, new LwM2mObjectInstance[] { new LwM2mObjectInstance(
                        GOOD_OBJECT_INSTANCE_ID, new LwM2mResource[] { new LwM2mResource(INT_RESOURCE_ID, Value
                                .newBinaryValue("0".getBytes())) }) }));
    }

    private void observeObject(final ObserveSpec.Builder observeSpecBuilder) {
        helper.sendWriteAttributes(observeSpecBuilder.build(), INT_OBJECT_ID);
        observeObject();
    }

    private void assertObservedResource(final String value) {
        assertObservedResource(500, value);
    }

    private void assertObservedResource(final long timeoutInSeconds, final String value) {
        Awaitility.await().atMost(timeoutInSeconds, TimeUnit.MILLISECONDS).untilTrue(observer.receievedNotify());
        assertEquals(new LwM2mResource(INT_RESOURCE_ID, Value.newStringValue(value)), observer.getContent());
    }

    private void assertObservedObjectInstance(final long timeoutInSeconds, final String resourceValue) {
        Awaitility.await().atMost(timeoutInSeconds, TimeUnit.MILLISECONDS).untilTrue(observer.receievedNotify());
        assertEquals(new LwM2mObjectInstance(GOOD_OBJECT_INSTANCE_ID, new LwM2mResource[] { new LwM2mResource(
                INT_RESOURCE_ID, Value.newBinaryValue(resourceValue.getBytes())) }), observer.getContent());
    }

    private void assertObservedObject(final long timeoutInSeconds, final String resourceValue) {
        Awaitility.await().atMost(timeoutInSeconds, TimeUnit.MILLISECONDS).untilTrue(observer.receievedNotify());
        assertEquals(
                new LwM2mObject(INT_OBJECT_ID, new LwM2mObjectInstance[] { new LwM2mObjectInstance(
                        GOOD_OBJECT_INSTANCE_ID, new LwM2mResource[] { new LwM2mResource(INT_RESOURCE_ID,
                                Value.newBinaryValue(resourceValue.getBytes())) }) }), observer.getContent());
    }

    private void assertNoObservation(final long time) {
        sleep(time);
        assertFalse(observer.receievedNotify().get());
    }

    private void sleep(final long time) {
        try {
            Thread.sleep(time);
        } catch (final InterruptedException e) {
        }
    }

    private final class SampleObservation implements ObservationRegistryListener {
        private final AtomicBoolean receivedNotify = new AtomicBoolean();
        private LwM2mNode content;

        @Override
        public void newValue(final Observation observation, final LwM2mNode value) {
            receivedNotify.set(true);
            content = value;
        }

        @Override
        public void cancelled(final Observation observation) {

        }

        @Override
        public void newObservation(final Observation observation) {

        }

        public AtomicBoolean receievedNotify() {
            return receivedNotify;
        }

        public LwM2mNode getContent() {
            return content;
        }
    }

}
