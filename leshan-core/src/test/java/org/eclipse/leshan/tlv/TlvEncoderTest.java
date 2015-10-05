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
 *     Sierra Wireless - initial API and implementation
 *******************************************************************************/
package org.eclipse.leshan.tlv;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.nio.ByteBuffer;
import java.util.Date;

import org.junit.Test;

public class TlvEncoderTest {

    @Test
    public void encode_short() {
        byte[] encoded = TlvEncoder.encodeInteger(1234);

        // check value
        ByteBuffer bb = ByteBuffer.wrap(encoded);
        assertEquals(1234, bb.getShort());
        assertEquals(0, bb.remaining());
    }

    @Test
    public void encode_integer() {
        byte[] encoded = TlvEncoder.encodeInteger(1245823);

        // check value
        ByteBuffer bb = ByteBuffer.wrap(encoded);
        assertEquals(1245823, bb.getInt());
        assertEquals(0, bb.remaining());
    }

    @Test
    public void encode_negative_integer() {
        byte[] encoded = TlvEncoder.encodeInteger(-1245823);

        // check sign
        assertFalse((encoded[0] & (1 << 7)) == 0);

        // convert to positive and check the value
        encoded[0] &= ~(1 << 7);
        ByteBuffer bb = ByteBuffer.wrap(encoded);
        assertEquals(1245823, bb.getInt());
        assertEquals(0, bb.remaining());
    }

    @Test
    public void encode_long() {
        long value = System.currentTimeMillis();
        byte[] encoded = TlvEncoder.encodeInteger(value);

        // check value
        ByteBuffer bb = ByteBuffer.wrap(encoded);
        assertEquals(value, bb.getLong());
        assertEquals(0, bb.remaining());
    }

    @Test(expected = IllegalArgumentException.class)
    public void can_not_encode_min_long() {
        long value = Long.MIN_VALUE;
        TlvEncoder.encodeInteger(value);
    }

    @Test
    public void encode_date() {
        long timestamp = System.currentTimeMillis();
        byte[] encoded = TlvEncoder.encodeDate(new Date(timestamp));

        // check value
        ByteBuffer bb = ByteBuffer.wrap(encoded);
        assertEquals((int) (timestamp / 1000), bb.getInt());
        assertEquals(0, bb.remaining());
    }

    @Test
    public void encode_boolean() {
        byte[] encoded = TlvEncoder.encodeBoolean(true);

        // check value
        assertEquals(1, encoded.length);
        assertEquals(1, encoded[0]);
    }

}
