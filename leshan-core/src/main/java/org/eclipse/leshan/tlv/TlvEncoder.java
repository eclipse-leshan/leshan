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
import java.nio.ByteOrder;
import java.util.Date;

import org.eclipse.leshan.util.Charsets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TlvEncoder {

    private static final Logger LOG = LoggerFactory.getLogger(TlvEncoder.class);

    /**
     * Encodes an array of TLV.
     */
    public static ByteBuffer encode(Tlv[] tlvs) {
        int size = 0;

        LOG.trace("start");
        for (Tlv tlv : tlvs) {

            int length = tlvEncodedLength(tlv);
            size += tlvEncodedSize(tlv, length);
            LOG.trace("tlv size : {}", size);
        }
        LOG.trace("done, size : {}", size);
        ByteBuffer b = ByteBuffer.allocate(size);
        b.order(ByteOrder.BIG_ENDIAN);
        for (Tlv tlv : tlvs) {
            encode(tlv, b);
        }
        b.flip();
        return b;
    }

    /**
     * Encodes an integer value (signed magnitude representation)
     */
    public static byte[] encodeInteger(Number number) {

        ByteBuffer iBuf = null;
        long longValue = number.longValue();
        if (longValue == Long.MIN_VALUE) {
            throw new IllegalArgumentException(
                    "Could not encode Long.MIN_VALUE, because of signed magnitude representation.");
        }

        long positiveValue = longValue < 0 ? -longValue : longValue;
        if (positiveValue <= Byte.MAX_VALUE) {
            iBuf = ByteBuffer.allocate(1);
            iBuf.put((byte) positiveValue);
        } else if (positiveValue <= Short.MAX_VALUE) {
            iBuf = ByteBuffer.allocate(2);
            iBuf.putShort((short) positiveValue);
        } else if (positiveValue <= Integer.MAX_VALUE) {
            iBuf = ByteBuffer.allocate(4);
            iBuf.putInt((int) positiveValue);
        } else if (positiveValue <= Long.MAX_VALUE) {
            iBuf = ByteBuffer.allocate(8);
            iBuf.putLong(positiveValue);
        }

        byte[] bytes = iBuf.array();
        // set the most significant bit to 1 if negative value
        if (number.longValue() < 0) {
            bytes[0] |= 0b1000_0000;
        }
        return bytes;
    }

    /**
     * Encodes a floating point value.
     */
    public static byte[] encodeFloat(Number number) {
        ByteBuffer fBuf = null;
        double dValue = number.doubleValue();
        if (Float.MIN_VALUE <= dValue && dValue <= Float.MAX_VALUE) {
            fBuf = ByteBuffer.allocate(4);
            fBuf.putFloat((float) dValue);
        } else {
            fBuf = ByteBuffer.allocate(8);
            fBuf.putDouble(dValue);
        }
        return fBuf.array();
    }

    /**
     * Encodes a boolean value.
     */
    public static byte[] encodeBoolean(boolean value) {
        return value ? new byte[] { 1 } : new byte[] { 0 };
    }

    /**
     * Encodes a string value.
     */
    public static byte[] encodeString(String value) {
        return value.getBytes(Charsets.UTF_8);
    }

    /**
     * Encodes a date value.
     */
    public static byte[] encodeDate(Date value) {
        ByteBuffer tBuf = ByteBuffer.allocate(4);
        tBuf.putInt((int) (value.getTime() / 1000L));
        return tBuf.array();
    }

    private static int tlvEncodedSize(Tlv tlv, int length) {
        int size = 1 /* HEADER */;
        size += (tlv.getIdentifier() < 65_536) ? 1 : 2; /* 8 bits or 16 bits identifiers */

        if (length < 8) {
            size += 0;
        } else if (length < 256) {
            size += 1;
        } else if (length < 65_536) {
            size += 2;
        } else if (length < 16_777_216) {
            size += 3;
        } else {
            throw new IllegalArgumentException("length should fit in max 24bits");
        }

        size += length;
        return size;
    }

    private static int tlvEncodedLength(Tlv tlv) {
        int length;
        switch (tlv.getType()) {
        case RESOURCE_VALUE:
        case RESOURCE_INSTANCE:
            length = tlv.getValue().length;
            break;
        default:
            length = 0;
            for (Tlv child : tlv.getChildren()) {
                int subLength = tlvEncodedLength(child);
                length += tlvEncodedSize(child, subLength);
            }
        }

        return length;
    }

    private static void encode(Tlv tlv, ByteBuffer b) {
        int length;
        length = tlvEncodedLength(tlv);
        int typeByte;

        switch (tlv.getType()) {
        case OBJECT_INSTANCE:
            typeByte = 0b00_000000;
            break;
        case RESOURCE_INSTANCE:
            typeByte = 0b01_000000;
            break;
        case MULTIPLE_RESOURCE:
            typeByte = 0b10_000000;
            break;
        case RESOURCE_VALUE:
            // encode the value
            typeByte = 0b11_000000;
            break;
        default:
            throw new IllegalArgumentException("unknown TLV type : '" + tlv.getType() + "'");
        }

        // encode identifier length
        typeByte |= (tlv.getIdentifier() < 65_536) ? 0b00_0000 : 0b10_0000;

        // type of length
        if (length < 8) {
            typeByte |= length;
        } else if (length < 256) {
            typeByte |= 0b0000_1000;
        } else if (length < 65_536) {
            typeByte |= 0b0001_0000;
        } else {
            typeByte |= 0b0001_1000;
        }

        // fill the buffer
        b.put((byte) typeByte);
        if (tlv.getIdentifier() < 65_536) {
            b.put((byte) tlv.getIdentifier());
        } else {
            b.putShort((short) tlv.getIdentifier());
        }

        // write length

        if (length >= 8) {
            if (length < 256) {
                b.put((byte) length);
            } else if (length < 65_536) {
                b.putShort((short) length);
            } else {
                int msb = (length & 0xFF_00_00) >> 16;
                b.put((byte) msb);
                b.putShort((short) (length & 0xFF_FF));
                typeByte |= 0b0001_1000;
            }
        }

        switch (tlv.getType()) {
        case RESOURCE_VALUE:
        case RESOURCE_INSTANCE:
            b.put(tlv.getValue());
            break;
        default:
            for (Tlv child : tlv.getChildren()) {
                encode(child, b);
            }
            break;
        }
    }
}
