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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.nio.ByteBuffer;
import java.util.Arrays;

import org.eclipse.leshan.core.node.ObjectLink;
import org.eclipse.leshan.core.util.Hex;
import org.junit.jupiter.api.Test;
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
        byte[] bytes = Hex.decodeHex(dataStr.toCharArray());
        ByteBuffer b = ByteBuffer.wrap(bytes);
        Tlv[] tlv = TlvDecoder.decode(b);
        LOG.debug(Arrays.toString(tlv));

        ByteBuffer buff = TlvEncoder.encode(tlv);
        assertTrue(Arrays.equals(bytes, buff.array()));
    }

    @Test
    public void decode_broken_tlv() throws TlvException {
        String dataStr = "0011223344556677889900";
        byte[] bytes = Hex.decodeHex(dataStr.toCharArray());
        ByteBuffer b = ByteBuffer.wrap(bytes);

        try {
            TlvDecoder.decode(b);
            fail();
        } catch (TlvException ex) {
            // this is very weak assertion since the format of the exception's message could
            // be changed any time
            assertEquals("Impossible to parse TLV: \n0011223344556677889900", ex.getMessage());
        }
    }

    @Test
    public void decode_object_link() throws TlvException {
        String dataStr = "12345678";
        byte[] bytes = Hex.decodeHex(dataStr.toCharArray());
        ObjectLink objlnk = TlvDecoder.decodeObjlnk(bytes);
        assertEquals(0x1234, objlnk.getObjectId());
        assertEquals(0x5678, objlnk.getObjectInstanceId());

        dataStr = "ffffffff";
        bytes = Hex.decodeHex(dataStr.toCharArray());
        objlnk = TlvDecoder.decodeObjlnk(bytes);
        assertEquals(0xffff, objlnk.getObjectId());
        assertEquals(0xffff, objlnk.getObjectInstanceId());
    }
}
