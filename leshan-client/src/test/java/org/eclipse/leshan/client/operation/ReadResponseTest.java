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
package org.eclipse.leshan.client.operation;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.leshan.client.response.ReadResponse;
import org.eclipse.leshan.tlv.Tlv;
import org.eclipse.leshan.tlv.TlvEncoder;
import org.eclipse.leshan.tlv.Tlv.TlvType;
import org.junit.Test;

public class ReadResponseTest {

    @Test
    public void testEqualityRobustnessForSuccesses() {
        assertEquals(ReadResponse.success("hello".getBytes()), ReadResponse.success("hello".getBytes()));
        assertNotEquals(ReadResponse.success("hello".getBytes()), ReadResponse.success("goodbye".getBytes()));
        assertNotEquals(ReadResponse.success("hello".getBytes()), ReadResponse.failure());
        assertNotEquals(ReadResponse.success("hello".getBytes()), null);
    }

    @Test
    public void testHashCodeRobustnessForSuccesses() {
        assertEquals(ReadResponse.success("hello".getBytes()).hashCode(), ReadResponse.success("hello".getBytes())
                .hashCode());
        assertNotEquals(ReadResponse.success("hello".getBytes()).hashCode(), ReadResponse.success("goodbye".getBytes())
                .hashCode());
        assertNotEquals(ReadResponse.success("hello".getBytes()).hashCode(), ReadResponse.failure().hashCode());
    }

    @Test
    public void testEqualityRobustnessForFailures() {
        assertEquals(ReadResponse.failure(), ReadResponse.failure());
        assertNotEquals(ReadResponse.failure(), ReadResponse.success("goodbye".getBytes()));
        assertNotEquals(ReadResponse.failure(), null);
    }

    @Test
    public void testHashCodeRobustnessForFailures() {
        assertEquals(ReadResponse.failure().hashCode(), ReadResponse.failure().hashCode());
        assertNotEquals(ReadResponse.failure(), ReadResponse.success("goodbye".getBytes()).hashCode());
    }

    @Test
    public void testSuccessSinglePayload() {
        final ReadResponse response = ReadResponse.success("value".getBytes());
        assertArrayEquals("value".getBytes(), response.getResponsePayload());
    }

    @Test
    public void testSuccessSingleTlv() {
        final ReadResponse response = ReadResponse.success("value".getBytes());
        assertEquals(new Tlv(TlvType.RESOURCE_VALUE, null, "value".getBytes(), 0), response.getResponsePayloadAsTlv());
    }

    @Test
    public void testSuccessMultiplePayload() {
        final Map<Integer, byte[]> readValues = new HashMap<>();
        readValues.put(55, "value".getBytes());
        final ReadResponse response = ReadResponse.successMultiple(readValues);
        final Tlv[] instances = new Tlv[] { new Tlv(TlvType.RESOURCE_INSTANCE, null, "value".getBytes(), 55) };
        assertArrayEquals(TlvEncoder.encode(instances).array(), response.getResponsePayload());
    }

    @Test
    public void testSuccessMultipleTlv() {
        final Map<Integer, byte[]> readValues = new HashMap<>();
        readValues.put(55, "value".getBytes());
        final ReadResponse response = ReadResponse.successMultiple(readValues);
        final Tlv[] instances = new Tlv[] { new Tlv(TlvType.RESOURCE_INSTANCE, null, "value".getBytes(), 55) };
        assertEquals(new Tlv(TlvType.MULTIPLE_RESOURCE, instances, null, 0), response.getResponsePayloadAsTlv());
    }

}
