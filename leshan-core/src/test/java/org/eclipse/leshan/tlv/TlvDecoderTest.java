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
import static org.junit.Assert.assertTrue;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Date;

import javax.xml.bind.DatatypeConverter;

import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Unit test for {@link TlvDecoder}
 */
public class TlvDecoderTest {

    private static Logger LOG = LoggerFactory.getLogger(TlvDecoderTest.class);

    @Test
    public void decode_device_object() throws TlvException {
        // // the /3// from liwblwm2m
        String dataStr = "C800144F70656E204D6F62696C6520416C6C69616E6365C801164C69676874776569676874204D324D20436C69656E74C80209333435303030313233C303312E30860641000141010588070842000ED842011388870841007D42010384C10964C10A0F830B410000C40D5182428FC60E2B30323A3030C10F55";
        byte[] bytes = DatatypeConverter.parseHexBinary(dataStr);
        ByteBuffer b = ByteBuffer.wrap(bytes);
        Tlv[] tlv = TlvDecoder.decode(b);
        LOG.debug(Arrays.toString(tlv));

        ByteBuffer buff = TlvEncoder.encode(tlv);
        assertTrue(Arrays.equals(bytes, buff.array()));
    }

    @Test
    public void decode_broken_tlv() throws TlvException {
        String dataStr = "0011223344556677889900";
        byte[] bytes = DatatypeConverter.parseHexBinary(dataStr);
        ByteBuffer b = ByteBuffer.wrap(bytes);

        try {
            TlvDecoder.decode(b);
            Assert.fail();
        } catch (TlvException ex) {
            assertEquals("Impossible to parse TLV: \n0011223344556677889900", ex.getMessage());
        }
    }

    @Test
    public void decode_uncomplete_integer() throws TlvException {

        // byte representation of 4194304 (2^22) integer on only 3 bytes instead of 4.
        ByteBuffer bb = ByteBuffer.allocate(3);
        bb.put((byte) 0b01000000);
        bb.put((byte) 0b00000000);
        bb.put((byte) 0b00000000);
        byte[] val = bb.array();

        Integer intVal = (Integer) TlvDecoder.decodeInteger(val);
        assertEquals(4194304, intVal.intValue());
    }

    @Test
    public void decode_negative_integer() throws TlvException {

        // signed magnitude representation for value -123456
        ByteBuffer bb = ByteBuffer.allocate(4);
        bb.putInt(123456);
        byte[] val = bb.array();

        // last bit to 1 for negative number
        val[0] |= (1 << 7);

        Integer intVal = (Integer) TlvDecoder.decodeInteger(val);
        assertEquals(-123456, intVal.intValue());
    }

    @Test
    public void decode_date() throws TlvException {

        long tsInSecond = 100000;
        ByteBuffer bb = ByteBuffer.allocate(8);
        bb.putLong(tsInSecond);

        assertEquals(new Date(tsInSecond * 1000L), TlvDecoder.decodeDate(bb.array()));
    }
}
