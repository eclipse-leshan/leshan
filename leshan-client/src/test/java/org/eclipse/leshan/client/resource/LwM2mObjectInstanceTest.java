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
package org.eclipse.leshan.client.resource;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.leshan.client.exchange.LwM2mCallbackExchange;
import org.eclipse.leshan.client.exchange.LwM2mExchange;
import org.eclipse.leshan.client.resource.LwM2mClientObject;
import org.eclipse.leshan.client.resource.LwM2mClientObjectDefinition;
import org.eclipse.leshan.client.resource.LwM2mClientObjectInstance;
import org.eclipse.leshan.client.resource.MultipleResourceDefinition;
import org.eclipse.leshan.client.resource.SingleResourceDefinition;
import org.eclipse.leshan.client.resource.multiple.MultipleLwM2mExchange;
import org.eclipse.leshan.client.resource.multiple.MultipleLwM2mResource;
import org.eclipse.leshan.client.resource.string.StringLwM2mExchange;
import org.eclipse.leshan.client.resource.string.StringLwM2mResource;
import org.eclipse.leshan.client.response.ReadResponse;
import org.eclipse.leshan.tlv.Tlv;
import org.eclipse.leshan.tlv.TlvEncoder;
import org.eclipse.leshan.tlv.Tlv.TlvType;
import org.junit.Test;

public class LwM2mObjectInstanceTest {

    private static final boolean REQUIRED = true;
    private static final boolean MANDATORY = true;
    private static final boolean SINGLE = true;
    private LwM2mClientObjectDefinition definition;

    @Test
    public void testSingleResource() {
        final int resourceId = 12;
        initializeObjectWithSingleResource(resourceId, "hello");

        assertCorrectRead(createInstance(definition, new byte[0]),
                new Tlv(TlvType.RESOURCE_VALUE, null, "hello".getBytes(), resourceId));
    }

    @Test
    public void testMultipleResourceWithOneInstance() {
        final int resourceId = 65;
        initializeObjectWithMultipleResource(resourceId, Collections.singletonMap(94, "ninety-four".getBytes()));

        assertCorrectRead(createInstance(definition, new byte[0]), new Tlv(TlvType.MULTIPLE_RESOURCE,
                new Tlv[] { new Tlv(TlvType.RESOURCE_INSTANCE, null, "ninety-four".getBytes(), 94) }, null, resourceId));
    }

    @Test
    public void testMultipleResourceWithThreeInstances() {
        final int resourceId = 65;
        final Map<Integer, byte[]> values = new HashMap<>();
        values.put(1100, "eleven-hundred".getBytes());
        values.put(10, "ten".getBytes());
        values.put(3, "three".getBytes());
        initializeObjectWithMultipleResource(resourceId, values);

        assertCorrectRead(createInstance(definition, new byte[0]), new Tlv(TlvType.MULTIPLE_RESOURCE, new Tlv[] {
                                new Tlv(TlvType.RESOURCE_INSTANCE, null, "three".getBytes(), 3),
                                new Tlv(TlvType.RESOURCE_INSTANCE, null, "ten".getBytes(), 10),
                                new Tlv(TlvType.RESOURCE_INSTANCE, null, "eleven-hundred".getBytes(), 1100) }, null,
                resourceId));
    }

    private void initializeObjectWithSingleResource(final int resourceId, final String value) {
        definition = new LwM2mClientObjectDefinition(100, MANDATORY, SINGLE, new SingleResourceDefinition(resourceId,
                new SampleSingleResource(value), !REQUIRED));
    }

    private void initializeObjectWithMultipleResource(final int resourceId, final Map<Integer, byte[]> values) {
        definition = new LwM2mClientObjectDefinition(101, MANDATORY, SINGLE, new MultipleResourceDefinition(resourceId,
                new SampleMultipleResource(values), !REQUIRED));
    }

    private void assertCorrectRead(final LwM2mClientObjectInstance instance, final Tlv... tlvs) {
        final LwM2mExchange exchange = mock(LwM2mExchange.class);
        instance.read(exchange);
        final byte[] bytes = TlvEncoder.encode(tlvs).array();
        verify(exchange).respond(ReadResponse.success(bytes));
    }

    private LwM2mClientObjectInstance createInstance(final LwM2mClientObjectDefinition definition, final byte[] payload) {
        final LwM2mClientObject obj = mock(LwM2mClientObject.class);
        final LwM2mClientObjectInstance instance = new LwM2mClientObjectInstance(0, obj, definition);
        @SuppressWarnings("unchecked")
        final LwM2mCallbackExchange<LwM2mClientObjectInstance> createExchange = mock(LwM2mCallbackExchange.class);
        when(createExchange.getRequestPayload()).thenReturn(payload);
        instance.createInstance(createExchange);
        return instance;
    }

    private class SampleSingleResource extends StringLwM2mResource {

        private final String value;

        public SampleSingleResource(final String value) {
            this.value = value;
        }

        @Override
        public void handleRead(final StringLwM2mExchange exchange) {
            exchange.respondContent(value);
        }

    }

    private class SampleMultipleResource extends MultipleLwM2mResource {

        private final Map<Integer, byte[]> values;

        public SampleMultipleResource(final Map<Integer, byte[]> values) {
            this.values = values;
        }

        @Override
        public void handleRead(final MultipleLwM2mExchange exchange) {
            exchange.respondContent(values);
        }

    }

}
