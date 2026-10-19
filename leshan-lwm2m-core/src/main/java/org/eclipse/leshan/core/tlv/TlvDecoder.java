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

import java.math.BigInteger;
import java.nio.Buffer;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.eclipse.leshan.core.node.ObjectLink;
import org.eclipse.leshan.core.tlv.Tlv.TlvType;
import org.eclipse.leshan.core.util.Hex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TlvDecoder {

    private static final Logger LOG = LoggerFactory.getLogger(TlvDecoder.class);

    private TlvDecoder() {
        // this is an utility static class
    }

    public static Tlv[] decode(ByteBuffer input) throws TlvException {
        try {
            return decodeTlvs(input, null);
        } catch (TlvException ex) {
            if (LOG.isTraceEnabled()) {
                LOG.trace("Impossible to parse TLV: {}", getBufferHex(input), ex);
            }
            throw new TlvException("Impossible to parse TLV: " + getBufferState(input), ex);
        } catch (RuntimeException ex) {
            if (LOG.isTraceEnabled()) {
                LOG.trace("Unexpected TLV parse error: {}", getBufferHex(input), ex);
            }
            throw new TlvException("Unexpected TLV parse error: " + getBufferState(input), ex);
        }
    }

    /**
     * Decodes all the TLVs contained in the given buffer.
     * <p>
     * This method is called recursively for nested TLVs and so must not wrap the raised {@link TlvException} : doing it
     * at each level would create a chain of causes as deep as the payload is nested. Exceptions are wrapped once for
     * all by {@link #decode(ByteBuffer)}.
     *
     * @param input the buffer to decode
     * @param parentType the type of the enclosing TLV or <code>null</code> if this is the top level
     */
    private static Tlv[] decodeTlvs(ByteBuffer input, TlvType parentType) throws TlvException {
        List<Tlv> tlvs = new ArrayList<>();

        while (input.remaining() > 0) {
            input.order(ByteOrder.BIG_ENDIAN);

            // decode type
            int typeByte = input.get() & 0xFF;
            TlvType type = consumeType(typeByte);

            // validate that this type is allowed at this place of the hierarchy
            validateType(type, parentType);

            // decode identifier
            int identifier = consumeIdentifier(input, typeByte);
            LOG.trace("decoding {} {}", type, identifier);

            // decode length
            int lengthType = typeByte & 0b0001_1000;
            int length = consumeLength(input, lengthType, typeByte);
            LOG.trace("length: {} (length type: {})", length, lengthType);

            // validate length
            validateLength(input, length);

            // decode value
            if (type == TlvType.RESOURCE_VALUE || type == TlvType.RESOURCE_INSTANCE) {
                byte[] payload = consumeValue(input, length);
                tlvs.add(new Tlv(type, null, payload, identifier));
                if (LOG.isTraceEnabled()) {
                    LOG.trace("payload value: {}", Hex.encodeHexString(payload));
                }
            } else {
                Tlv[] children = consumeChildren(input, length, type);
                Tlv tlv = new Tlv(type, children, null, identifier);
                tlvs.add(tlv);
            }
        }

        return tlvs.toArray(new Tlv[] {});
    }

    /**
     * Validates that a TLV of the given type is allowed inside a TLV of the given parent type.
     * <p>
     * The LWM2M TLV hierarchy is fixed : an OBJECT_INSTANCE contains only resources (RESOURCE_VALUE or
     * MULTIPLE_RESOURCE), a MULTIPLE_RESOURCE contains only RESOURCE_INSTANCE and both RESOURCE_VALUE and
     * RESOURCE_INSTANCE are leaves. Enforcing it here makes the nesting depth of a valid payload constant and so bounds
     * the recursion depth of the decoder, whatever the payload size is.
     *
     * @param type the type of the TLV being decoded
     * @param parentType the type of the enclosing TLV or <code>null</code> if this is the top level
     */
    protected static void validateType(TlvType type, TlvType parentType) throws TlvException {
        if (parentType == null) {
            // at top level, the expected type depends on the path of the request, so we can not check it here.
            return;
        }
        switch (parentType) {
        case OBJECT_INSTANCE:
            if (type != TlvType.RESOURCE_VALUE && type != TlvType.MULTIPLE_RESOURCE) {
                throw new TlvException(String.format("Invalid TLV: %s is not allowed inside a %s, expected %s or %s",
                        type, parentType, TlvType.RESOURCE_VALUE, TlvType.MULTIPLE_RESOURCE));
            }
            break;
        case MULTIPLE_RESOURCE:
            if (type != TlvType.RESOURCE_INSTANCE) {
                throw new TlvException(String.format("Invalid TLV: %s is not allowed inside a %s, expected %s", type,
                        parentType, TlvType.RESOURCE_INSTANCE));
            }
            break;
        default:
            // should not happen as we only decode children of OBJECT_INSTANCE and MULTIPLE_RESOURCE
            throw new TlvException(String.format("Invalid TLV: a %s can not contain other TLV", parentType));
        }
    }

    private static void validateLength(ByteBuffer buffer, int length) throws TlvException {
        if (length > buffer.remaining()) {
            throw new TlvException(String.format("Invalid length: %d bytes declared but %d bytes remaining in buffer",
                    length, buffer.remaining()));
        }
    }

    private static String getBufferState(ByteBuffer input) {
        return String.format("buffer state position=%d, remaining=%d, limit=%d", input.position(), input.remaining(),
                input.limit());
    }

    private static String getBufferHex(ByteBuffer input) {
        ByteBuffer copy = input.duplicate();
        copy.position(0);
        byte[] bytes = new byte[copy.limit()];
        copy.get(bytes);
        return Hex.encodeHexString(bytes);
    }

    private static TlvType consumeType(int typeByte) throws TlvException {
        TlvType type;
        switch (typeByte & 0b1100_0000) {
        case 0b0000_0000:
            type = TlvType.OBJECT_INSTANCE;
            break;
        case 0b0100_0000:
            type = TlvType.RESOURCE_INSTANCE;
            break;
        case 0b1000_0000:
            type = TlvType.MULTIPLE_RESOURCE;
            break;
        case 0b1100_0000:
            type = TlvType.RESOURCE_VALUE;
            break;
        default:
            throw new TlvException("unknown type: " + (typeByte & 0b1100_0000));
        }
        return type;
    }

    private static int consumeIdentifier(ByteBuffer input, int typeByte) throws TlvException {
        int identifier;
        try {
            if ((typeByte & 0b0010_0000) == 0) {
                identifier = input.get() & 0xFF;
            } else {
                identifier = input.getShort() & 0xFFFF;
            }
        } catch (BufferUnderflowException e) {
            throw new TlvException("Invalid 'identifier' length", e);
        }
        return identifier;
    }

    private static int consumeLength(ByteBuffer input, int lengthType, int typeByte) throws TlvException {
        try {
            switch (lengthType) {
            case 0b0000_0000:
                // 2 bit length
                return typeByte & 0b0000_0111;
            case 0b0000_1000:
                // 8 bit length
                return input.get() & 0xFF;
            case 0b0001_0000:
                // 16 bit length
                return input.getShort() & 0xFFFF;
            case 0b0001_1000:
                // 24 bit length
                int b = input.get() & 0x000000FF;
                int s = input.getShort() & 0x0000FFFF;
                return (b << 16) | s;
            default:
                throw new TlvException("unknown length type: " + (typeByte & 0b0001_1000));
            }
        } catch (BufferUnderflowException e) {
            throw new TlvException("Invalid 'length' length", e);
        }
    }

    private static byte[] consumeValue(ByteBuffer input, int length) throws TlvException {
        try {
            byte[] payload = new byte[length];
            input.get(payload);
            return payload;
        } catch (BufferUnderflowException e) {
            // should not happened anymore because length is check before to avoid amplification attack
            throw new TlvException("Invalid 'value' length", e);
        }
    }

    @SuppressWarnings("java:S1905")
    private static Tlv[] consumeChildren(ByteBuffer input, int length, TlvType parentType) throws TlvException {
        // create a view of the contained TLVs
        ByteBuffer slice = input.slice();
        // HACK the cast is necessary for binary backward compatibility bug introduce in Java 9
        // https://github.com/apache/felix/pull/114
        ((Buffer) slice).limit(length);

        Tlv[] children = decodeTlvs(slice, parentType);

        // skip the children, it will be decoded by the view
        // HACK the cast is necessary for binary backward compatibility bug introduce in Java 9
        // https://github.com/apache/felix/pull/114
        ((Buffer) input).position(input.position() + length);

        return children;
    }

    /**
     * Decodes a byte array into string value.
     */
    public static String decodeString(byte[] value) {
        return new String(value, StandardCharsets.UTF_8);
    }

    /**
     * Decodes a byte array into a boolean value.
     */
    public static boolean decodeBoolean(byte[] value) throws TlvException {
        if (value.length == 1) {
            if (value[0] == 0) {
                return false;
            } else if (value[0] == 1) {
                return true;
            } else {
                LOG.warn("Boolean value should be encoded as integer with value 0 or 1, not {}", value[0]);
                return false;
            }
        }
        throw new TlvException("Invalid length for a boolean value: " + value.length);
    }

    /**
     * Decodes a byte array into a date value.
     */
    public static Date decodeDate(byte[] value) throws TlvException {
        BigInteger bi = new BigInteger(value);
        if (value.length <= 8) {
            return new Date(bi.longValue() * 1000L);
        } else {
            throw new TlvException("Invalid length for a time value: " + value.length);
        }
    }

    /**
     * Decodes a byte array into a objlnk value.
     */
    public static ObjectLink decodeObjlnk(byte[] value) throws TlvException {
        if (value.length != 4) {
            throw new TlvException("Invalid length for an object link value, 4 bytes array expected but get "
                    + value.length + " bytes");
        }
        ByteBuffer bff = ByteBuffer.allocate(4).order(ByteOrder.BIG_ENDIAN);
        bff.put(value);
        int val1 = bff.getShort(0) & 0xFFFF;
        int val2 = bff.getShort(2) & 0xFFFF;
        return new ObjectLink(val1, val2);
    }

    /**
     * Decodes a byte array into an integer value.
     */
    public static Number decodeInteger(byte[] value) throws TlvException {
        BigInteger bi = new BigInteger(value);
        if (value.length == 1) {
            return bi.byteValue();
        } else if (value.length <= 2) {
            return bi.shortValue();
        } else if (value.length <= 4) {
            return bi.intValue();
        } else if (value.length <= 8) {
            return bi.longValue();
        } else {
            throw new TlvException("Invalid length for an integer value: " + value.length);
        }
    }

    /**
     * Decodes a byte array into a float value.
     */
    public static Number decodeFloat(byte[] value) throws TlvException {
        ByteBuffer floatBb = ByteBuffer.wrap(value);
        if (value.length == 4) {
            return floatBb.getFloat();
        } else if (value.length == 8) {
            return floatBb.getDouble();
        } else {
            throw new TlvException("Invalid length for a float value: " + value.length);
        }
    }
}
