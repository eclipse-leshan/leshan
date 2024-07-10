/*******************************************************************************
 * Copyright (c) 2013-2015 Sierra Wireless and others.
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
package org.eclipse.leshan.core.tlv;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.ByteBuffer;
import java.util.Calendar;
import java.util.Date;

import org.eclipse.leshan.core.tlv.Tlv.TlvType;
import org.junit.jupiter.api.Test;

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
    public void encode_long() {
        long value = System.currentTimeMillis();
        byte[] encoded = TlvEncoder.encodeInteger(value);

        // check value
        ByteBuffer bb = ByteBuffer.wrap(encoded);
        assertEquals(value, bb.getLong());
        assertEquals(0, bb.remaining());
    }

    @Test
    public void encode_date_4_bytes() throws TlvException {
        Calendar cal = Calendar.getInstance();
        cal.set(2024, 1, 1, 0, 0, 0);
        cal.set(Calendar.MILLISECOND, 534);
        long timestamp = cal.getTimeInMillis();
        byte[] encoded = TlvEncoder.encodeDate(new Date(timestamp));

        // check value
        ByteBuffer bb = ByteBuffer.wrap(encoded);
        assertEquals((int) (timestamp / 1000), bb.getInt());
        assertEquals(0, bb.remaining());
        cal.set(Calendar.MILLISECOND, 0);
        Date date = TlvDecoder.decodeDate(encoded);
        assertEquals(cal.getTimeInMillis(), date.getTime());
    }

    @Test
    public void encode_date_8_bytes() throws TlvException {
        Calendar cal = Calendar.getInstance();
        cal.set(2700, 1, 1, 0, 0, 0);
        cal.set(Calendar.MILLISECOND, 523);
        long timestamp = cal.getTimeInMillis();
        byte[] encoded = TlvEncoder.encodeDate(new Date(timestamp));

        // check value
        ByteBuffer bb = ByteBuffer.wrap(encoded);
        assertEquals(timestamp / 1000, bb.getLong());
        assertEquals(0, bb.remaining());
        cal.set(Calendar.MILLISECOND, 0);
        Date date = TlvDecoder.decodeDate(encoded);
        assertEquals(cal.getTimeInMillis(), date.getTime());
    }

    @Test
    public void encode_boolean() {
        byte[] encoded = TlvEncoder.encodeBoolean(true);

        // check value
        assertEquals(1, encoded.length);
        assertEquals(1, encoded[0]);
    }

    @Test
    public void encode_resource() throws TlvException {
        Tlv[] expectedTlv = new Tlv[] { new Tlv(TlvType.RESOURCE_VALUE, null, TlvEncoder.encodeInteger(100), 500) };

        ByteBuffer encoded = TlvEncoder.encode(expectedTlv);
        Tlv[] decodedTlv = TlvDecoder.decode(encoded);

        assertArrayEquals(expectedTlv, decodedTlv);
    }
}
