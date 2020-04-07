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

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.Date;

import org.eclipse.leshan.core.node.ObjectLink;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TlvEncoder {

    private static final Logger LOG = LoggerFactory.getLogger(TlvEncoder.class);

    private static final int MAX_LENGTH_8BIT = 256;
    private static final int MAX_LENGTH_16BIT = 65_536;
    private static final int MAX_LENGTH_24BIT = 16_777_216;

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
        // HACK the cast is necessary for binary backward compatibility bug introduce in Java 9
        // https://github.com/apache/felix/pull/114
        ((Buffer) b).flip();
        return b;
    }

    /**
     * Encodes an integer value.
     */
    public static byte[] encodeInteger(Number number) {
        ByteBuffer iBuf;
        long lValue = number.longValue();
        if (lValue >= Byte.MIN_VALUE && lValue <= Byte.MAX_VALUE) {
            iBuf = ByteBuffer.allocate(1);
            iBuf.put((byte) lValue);
        } else if (lValue >= Short.MIN_VALUE && lValue <= Short.MAX_VALUE) {
            iBuf = ByteBuffer.allocate(2);
            iBuf.putShort((short) lValue);
        } else if (lValue >= Integer.MIN_VALUE && lValue <= Integer.MAX_VALUE) {
            iBuf = ByteBuffer.allocate(4);
            iBuf.putInt((int) lValue);
        } else {
            iBuf = ByteBuffer.allocate(8);
            iBuf.putLong(lValue);
        }
        return iBuf.array();
    }

    /**
     * Encodes a floating point value.
     */
    public static byte[] encodeFloat(Number number) {
        ByteBuffer fBuf;
        if (number instanceof Float) {
            fBuf = ByteBuffer.allocate(4);
            fBuf.putFloat(number.floatValue());
        } else {
            fBuf = ByteBuffer.allocate(8);
            fBuf.putDouble(number.doubleValue());
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
        return value.getBytes(StandardCharsets.UTF_8);
    }

    /**
     * Encodes a date value.
     */
    public static byte[] encodeDate(Date value) {
        ByteBuffer tBuf = ByteBuffer.allocate(4);
        tBuf.putInt((int) (value.getTime() / 1000L));
        return tBuf.array();
    }

    /**
     * Encodes a Objlnk value.
     */
    public static byte[] encodeObjlnk(ObjectLink value) {
        ByteBuffer objlnkBuffer = ByteBuffer.allocate(4).order(ByteOrder.BIG_ENDIAN);
        objlnkBuffer.putShort(0, (short) value.getObjectId());
        objlnkBuffer.putShort(2, (short) value.getObjectInstanceId());
        return objlnkBuffer.array();
    }

    private static int tlvEncodedSize(Tlv tlv, int length) {
        int size = 1 /* HEADER */;
        size += (tlv.getIdentifier() < MAX_LENGTH_8BIT) ? 1 : 2; /* 8 bits or 16 bits identifiers */

        if (length < 8) {
            size += 0;
        } else if (length < MAX_LENGTH_8BIT) {
            size += 1;
        } else if (length < MAX_LENGTH_16BIT) {
            size += 2;
        } else if (length < MAX_LENGTH_24BIT) {
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
        typeByte |= (tlv.getIdentifier() < MAX_LENGTH_8BIT) ? 0b00_0000 : 0b10_0000;

        // type of length
        if (length < 8) {
            typeByte |= length;
        } else if (length < MAX_LENGTH_8BIT) {
            typeByte |= 0b0000_1000;
        } else if (length < MAX_LENGTH_16BIT) {
            typeByte |= 0b0001_0000;
        } else {
            typeByte |= 0b0001_1000;
        }

        // fill the buffer
        b.put((byte) typeByte);
        if (tlv.getIdentifier() < MAX_LENGTH_8BIT) {
            b.put((byte) tlv.getIdentifier());
        } else {
            b.putShort((short) tlv.getIdentifier());
        }

        // write length

        if (length >= 8) {
            if (length < MAX_LENGTH_8BIT) {
                b.put((byte) length);
            } else if (length < MAX_LENGTH_16BIT) {
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
