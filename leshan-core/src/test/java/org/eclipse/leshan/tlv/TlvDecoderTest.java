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

import java.nio.ByteBuffer;
import java.util.Arrays;

import org.eclipse.leshan.tlv.Tlv;
import org.eclipse.leshan.tlv.TlvDecoder;
import org.eclipse.leshan.tlv.TlvEncoder;
import org.eclipse.leshan.tlv.TlvException;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Unit test for {@link TlvDecoder}
 */
public class TlvDecoderTest {

    private Logger log = LoggerFactory.getLogger(getClass());

    @Test
    public void decode_device_object() throws TlvException {
        // // the /3// from liwblwm2m
        String dataStr = "C800144F70656E204D6F62696C6520416C6C69616E6365C801164C69676874776569676874204D324D20436C69656E74C80209333435303030313233C303312E30860641000141010588070842000ED842011388870841007D42010384C10964C10A0F830B410000C40D5182428FC60E2B30323A3030C10F55";
        byte[] bytes = hexStringToByteArray(dataStr);
        ByteBuffer b = ByteBuffer.wrap(bytes);
        Tlv[] tlv = TlvDecoder.decode(b);
        log.debug(Arrays.toString(tlv));

        ByteBuffer buff = TlvEncoder.encode(tlv);
        Assert.assertTrue(Arrays.equals(bytes, buff.array()));
    }

    protected byte[] hexStringToByteArray(String hexString) {

        if (hexString.length() % 2 > 0) {
            throw new IllegalArgumentException("Hex String must have even number of chars");
        }

        byte[] bytes = new byte[hexString.length() / 2];

        for (int i = 0; i < bytes.length; i++) {
            int currentByteIdx = i * 2;
            bytes[i] = (byte) Integer.parseInt(hexString.substring(currentByteIdx, currentByteIdx + 2), 16);
            ;
        }
        return bytes;
    }
    
    @Test
    public void testHexStringToByteArray()
    {
        String   dataStr = "C800144F70656E204D6F62696C6520416C6C69616E6365C801164C69676874776569676874204D324D20436C69656E74C80209333435303030313233C303312E30860641000141010588070842000ED842011388870841007D42010384C10964C10A0F830B410000C40D5182428FC60E2B30323A3030C10F55";
        byte[] dataBytes1 = com.sun.org.apache.xerces.internal.impl.dv.util.HexBin.decode((String) dataStr);
        byte[] dataBytes2 = javax.xml.bind.DatatypeConverter.parseHexBinary(dataStr);
        byte[] dataBytes3 = hexStringToByteArray(dataStr);
        Assert.assertArrayEquals(dataBytes1, dataBytes3);
        Assert.assertArrayEquals(dataBytes1, dataBytes2);
    }
}
