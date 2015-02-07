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

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.eclipse.leshan.client.exchange.LwM2mExchange;
import org.eclipse.leshan.client.resource.multiple.MultipleLwM2mExchange;
import org.eclipse.leshan.client.resource.multiple.MultipleLwM2mResource;
import org.eclipse.leshan.client.response.ReadResponse;
import org.eclipse.leshan.client.response.WriteResponse;
import org.eclipse.leshan.tlv.Tlv;
import org.eclipse.leshan.tlv.TlvEncoder;
import org.eclipse.leshan.tlv.Tlv.TlvType;
import org.junit.Test;

public class MultipleLwM2mResourceTest {

    @Test
    public void testWriteGoodValue() {
        final LwM2mExchange exchange = mock(LwM2mExchange.class);
        final Tlv[] tlvs = new Tlv[] { new Tlv(TlvType.RESOURCE_INSTANCE, null, "zero".getBytes(), 0),
                                new Tlv(TlvType.RESOURCE_INSTANCE, null, "one".getBytes(), 1),
                                new Tlv(TlvType.RESOURCE_INSTANCE, null, "two".getBytes(), 2) };
        when(exchange.getRequestPayload()).thenReturn(TlvEncoder.encode(tlvs).array());

        final ReadableWriteableTestResource testResource = new ReadableWriteableTestResource();
        testResource.write(exchange);

        final Map<Integer, byte[]> values = new HashMap<>();
        values.put(0, "zero".getBytes());
        values.put(2, "two".getBytes());
        values.put(1, "one".getBytes());
        assertDeepEquals(values, testResource.value);
        verify(exchange).respond(WriteResponse.success());
    }

    @Test
    public void testWriteNonTlvValue() {
        final LwM2mExchange exchange = mock(LwM2mExchange.class);
        when(exchange.getRequestPayload()).thenReturn("lol not a tlv".getBytes());

        final ReadableWriteableTestResource testResource = new ReadableWriteableTestResource();
        testResource.write(exchange);

        assertNull(testResource.value);
        verify(exchange).respond(WriteResponse.badRequest());
    }

    @Test
    public void testWriteIncorrectTlvValue() {
        final LwM2mExchange exchange = mock(LwM2mExchange.class);
        final Tlv[] tlvs = new Tlv[] { new Tlv(TlvType.RESOURCE_VALUE, null, "zero".getBytes(), 0),
                                new Tlv(TlvType.RESOURCE_INSTANCE, null, "one".getBytes(), 1),
                                new Tlv(TlvType.RESOURCE_INSTANCE, null, "two".getBytes(), 2) };
        when(exchange.getRequestPayload()).thenReturn(TlvEncoder.encode(tlvs).array());

        final ReadableWriteableTestResource testResource = new ReadableWriteableTestResource();
        testResource.write(exchange);

        assertNull(testResource.value);
        verify(exchange).respond(WriteResponse.badRequest());
    }

    @Test
    public void testRead() {
        final LwM2mExchange exchange = mock(LwM2mExchange.class);

        final Map<Integer, byte[]> initialValue = new HashMap<>();
        initialValue.put(0, "zero".getBytes());
        initialValue.put(2, "two".getBytes());
        initialValue.put(1, "one".getBytes());
        final ReadableWriteableTestResource testResource = new ReadableWriteableTestResource(initialValue);

        testResource.read(exchange);

        final Tlv[] tlvs = new Tlv[] { new Tlv(TlvType.RESOURCE_INSTANCE, null, "zero".getBytes(), 0),
                                new Tlv(TlvType.RESOURCE_INSTANCE, null, "one".getBytes(), 1),
                                new Tlv(TlvType.RESOURCE_INSTANCE, null, "two".getBytes(), 2) };

        assertEquals(initialValue, testResource.value);
        verify(exchange).respond(ReadResponse.success(TlvEncoder.encode(tlvs).array()));
    }

    @Test
    public void testDefaultPermissionsRead() {
        final LwM2mExchange exchange = mock(LwM2mExchange.class);

        final DefaultTestResource testResource = new DefaultTestResource();
        testResource.read(exchange);

        verify(exchange).respond(ReadResponse.notAllowed());
    }

    @Test
    public void testDefaultPermissionsWrite() {
        final LwM2mExchange exchange = mock(LwM2mExchange.class);
        when(exchange.getRequestPayload()).thenReturn("badwolf".getBytes());

        final DefaultTestResource testResource = new DefaultTestResource();
        testResource.write(exchange);

        verify(exchange).respond(WriteResponse.notAllowed());
    }

    private void assertDeepEquals(final Map<Integer, byte[]> left, final Map<Integer, byte[]> right) {
        assertEquals(left.size(), right.size());
        final Set<Entry<Integer, byte[]>> entrySet = left.entrySet();
        for (final Entry<Integer, byte[]> entry : entrySet) {
            assertArrayEquals(entry.getValue(), right.get(entry.getKey()));
        }
    }

    private class ReadableWriteableTestResource extends MultipleLwM2mResource {

        private Map<Integer, byte[]> value;

        public ReadableWriteableTestResource(final Map<Integer, byte[]> newValue) {
            value = newValue;
        }

        public ReadableWriteableTestResource() {
        }

        @Override
        protected void handleWrite(final MultipleLwM2mExchange exchange) {
            this.value = exchange.getRequestPayload();
            exchange.respondSuccess();
        }

        @Override
        protected void handleRead(final MultipleLwM2mExchange exchange) {
            exchange.respondContent(value);
        }

    }

    private class DefaultTestResource extends MultipleLwM2mResource {

    }

}
