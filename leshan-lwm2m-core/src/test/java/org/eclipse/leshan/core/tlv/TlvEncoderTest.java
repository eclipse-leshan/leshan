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

    private void shouldEncodeDateAsNumberOfBytes(long utcTimeInSeconds, int expectedNumberOfBytes) throws TlvException {
        Date dateToEncode = new Date(utcTimeInSeconds * 1000);

        // encode
        byte[] encoded = TlvEncoder.encodeDate(dateToEncode);

        // check byte array value
        ByteBuffer bb = ByteBuffer.wrap(encoded);
        if (expectedNumberOfBytes == 1) {
            assertEquals((byte) (dateToEncode.getTime() / 1000), bb.get());
            assertEquals(0, bb.remaining());
        } else if (expectedNumberOfBytes == 2) {
            assertEquals((short) (dateToEncode.getTime() / 1000), bb.getShort());
            assertEquals(0, bb.remaining());
        } else if (expectedNumberOfBytes == 4) {
            assertEquals((int) (dateToEncode.getTime() / 1000), bb.getInt());
            assertEquals(0, bb.remaining());
        } else {
            assertEquals(dateToEncode.getTime() / 1000, bb.getLong());
            assertEquals(0, bb.remaining());
        }

        // confirm decoded value
        Date date = TlvDecoder.decodeDate(encoded);
        assertEquals(dateToEncode.getTime(), date.getTime());
    }

    @Test
    public void encode_date_byte() throws TlvException {
        shouldEncodeDateAsNumberOfBytes(Byte.MAX_VALUE, 1);
        shouldEncodeDateAsNumberOfBytes(Byte.MIN_VALUE, 1);
        shouldEncodeDateAsNumberOfBytes(0, 1);
        shouldEncodeDateAsNumberOfBytes(100, 1);
        shouldEncodeDateAsNumberOfBytes(-100, 1);
    }

    @Test
    public void encode_date_short() throws TlvException {
        shouldEncodeDateAsNumberOfBytes(Byte.MAX_VALUE + 1, 2);
        shouldEncodeDateAsNumberOfBytes(Byte.MIN_VALUE - 1, 2);
        shouldEncodeDateAsNumberOfBytes(Short.MAX_VALUE, 2);
        shouldEncodeDateAsNumberOfBytes(Short.MIN_VALUE, 2);
        shouldEncodeDateAsNumberOfBytes(32000, 2);
        shouldEncodeDateAsNumberOfBytes(-32000, 2);
    }

    @Test
    public void encode_date_int() throws TlvException {
        shouldEncodeDateAsNumberOfBytes(Short.MAX_VALUE + 1, 4);
        shouldEncodeDateAsNumberOfBytes(Short.MIN_VALUE - 1, 4);
        shouldEncodeDateAsNumberOfBytes(Integer.MAX_VALUE, 4);
        shouldEncodeDateAsNumberOfBytes(Integer.MIN_VALUE, 4);
        shouldEncodeDateAsNumberOfBytes(Integer.MAX_VALUE - 100, 4);
        shouldEncodeDateAsNumberOfBytes(Integer.MIN_VALUE + 100, 4);
    }

    @Test
    public void encode_date_long() throws TlvException {
        shouldEncodeDateAsNumberOfBytes((long) Integer.MAX_VALUE + 1, 8);
        shouldEncodeDateAsNumberOfBytes((long) Integer.MIN_VALUE - 1, 8);
        shouldEncodeDateAsNumberOfBytes(Long.MAX_VALUE / 1000, 8);
        shouldEncodeDateAsNumberOfBytes(Long.MIN_VALUE / 1000, 8);
        shouldEncodeDateAsNumberOfBytes(Long.MAX_VALUE / 1000 - 100, 8);
        shouldEncodeDateAsNumberOfBytes(Long.MIN_VALUE / 1000 + 100, 8);
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
