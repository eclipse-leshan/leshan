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

    @Test
    public void encode_resource() throws TlvException {
        Tlv[] expectedTlv = new Tlv[] { new Tlv(TlvType.RESOURCE_VALUE, null, TlvEncoder.encodeInteger(100), 500) };

        ByteBuffer encoded = TlvEncoder.encode(expectedTlv);
        Tlv[] decodedTlv = TlvDecoder.decode(encoded);

        assertArrayEquals(expectedTlv, decodedTlv);
    }
}
