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

import static org.eclipse.leshan.core.tlv.Tlv.TlvType.MULTIPLE_RESOURCE;
import static org.eclipse.leshan.core.tlv.Tlv.TlvType.OBJECT_INSTANCE;
import static org.eclipse.leshan.core.tlv.Tlv.TlvType.RESOURCE_INSTANCE;
import static org.eclipse.leshan.core.tlv.Tlv.TlvType.RESOURCE_VALUE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.ByteBuffer;
import java.util.Arrays;

import org.eclipse.leshan.core.node.ObjectLink;
import org.eclipse.leshan.core.tlv.Tlv.TlvType;
import org.eclipse.leshan.core.util.Hex;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Unit test for {@link TlvDecoder}
 */
class TlvDecoderTest {

    private static final Logger LOG = LoggerFactory.getLogger(TlvDecoderTest.class);

    @Test
    void decode_device_object() throws TlvException {
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
    void decode_broken_tlv() {
        String dataStr = "0011223344556677889900";
        byte[] bytes = Hex.decodeHex(dataStr.toCharArray());
        ByteBuffer b = ByteBuffer.wrap(bytes);

        TlvException exception = assertThrows(TlvException.class, () -> TlvDecoder.decode(b));
        // this is very weak assertion since the format of the exception's message could
        // be changed any time
        assertEquals("Impossible to parse TLV: buffer state position=5, remaining=6, limit=11", exception.getMessage());
        assertFalse(exception.getMessage().contains(dataStr));
    }

    @Test
    void decode_broken_tlv_from_direct_byte_buffer() {
        String dataStr = "0011223344556677889900";
        byte[] bytes = Hex.decodeHex(dataStr.toCharArray());
        ByteBuffer b = ByteBuffer.allocateDirect(bytes.length);
        b.put(bytes);
        b.flip();

        TlvException exception = assertThrows(TlvException.class, () -> TlvDecoder.decode(b));
        assertEquals("Impossible to parse TLV: buffer state position=5, remaining=6, limit=11", exception.getMessage());
        assertFalse(exception.getMessage().contains(dataStr));
    }

    @Test
    void decode_object_link() throws TlvException {
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

    @Test
    void decode_object_link_reject_invalid_length() {
        TlvException exception = assertThrows(TlvException.class,
                () -> TlvDecoder.decodeObjlnk(new byte[] { 1, 2, 3 }));
        assertEquals("Invalid length for an object link value, 4 bytes array expected but get 3 bytes",
                exception.getMessage());
    }

    @Test
    void decode_tlv_with_truncated_value_throws_tlv_exception() {
        // Type=0xC8: resource with value,
        // ID=0x00,
        // declared length=0xC8 (200), but no value bytes follow
        String dataStr = "C800C8";
        ByteBuffer b = ByteBuffer.wrap(Hex.decodeHex(dataStr.toCharArray()));

        TlvException exception = assertThrows(TlvException.class, () -> TlvDecoder.decode(b));
        assertTrue(exception.getCause().getMessage().startsWith("Invalid length"));
        assertTrue(exception.getCause() instanceof TlvException);
    }

    // ---------------------------------------------------------------------------------------------------------------
    // valid hierarchies : must still be decoded
    // ---------------------------------------------------------------------------------------------------------------

    @Test
    void decode_object_instance_containing_multiple_resource() throws TlvException {
        byte[] payload = encode( //
                node(OBJECT_INSTANCE, 0, //
                        node(MULTIPLE_RESOURCE, 1, //
                                leaf(RESOURCE_INSTANCE, 0, 0x42))));

        Tlv[] tlvs = TlvDecoder.decode(ByteBuffer.wrap(payload));

        assertEquals(1, tlvs.length);
        assertEquals(OBJECT_INSTANCE, tlvs[0].getType());
        assertEquals(MULTIPLE_RESOURCE, tlvs[0].getChildren()[0].getType());
        assertEquals(RESOURCE_INSTANCE, tlvs[0].getChildren()[0].getChildren()[0].getType());
    }

    @Test
    void decode_object_instance_containing_single_value_resources() throws TlvException {
        byte[] payload = encode( //
                node(OBJECT_INSTANCE, 0, //
                        leaf(RESOURCE_VALUE, 0, 0x01), //
                        leaf(RESOURCE_VALUE, 1, 0x02)));

        Tlv[] tlvs = TlvDecoder.decode(ByteBuffer.wrap(payload));

        assertEquals(2, tlvs[0].getChildren().length);
    }

    @Test
    void decode_accepts_any_type_at_top_level() throws TlvException {
        // What is expected at top level depends on the path of the request (/3/0/6 gives RESOURCE_INSTANCEs for
        // instance), so the decoder can not restrict it.
        for (TlvType type : new TlvType[] { RESOURCE_INSTANCE, RESOURCE_VALUE }) {
            byte[] payload = encode(leaf(type, 0, 0x42));
            assertEquals(type, TlvDecoder.decode(ByteBuffer.wrap(payload))[0].getType());
        }
        byte[] payload = encode(node(MULTIPLE_RESOURCE, 0, leaf(RESOURCE_INSTANCE, 0, 0x42)));
        assertEquals(MULTIPLE_RESOURCE, TlvDecoder.decode(ByteBuffer.wrap(payload))[0].getType());
    }

    // ---------------------------------------------------------------------------------------------------------------
    // invalid hierarchies : must be rejected by the TLV decoder itself
    // ---------------------------------------------------------------------------------------------------------------

    @Test
    void decode_fails_on_resource_instance_directly_in_object_instance() {
        // a MULTIPLE_RESOURCE should be between them
        byte[] payload = encode( //
                node(OBJECT_INSTANCE, 0, //
                        leaf(RESOURCE_INSTANCE, 0, 0x42)));

        assertDecodeFails(payload);
    }

    @Test
    void decode_fails_on_object_instance_in_object_instance() {
        byte[] payload = encode( //
                node(OBJECT_INSTANCE, 0, //
                        node(OBJECT_INSTANCE, 0, //
                                leaf(RESOURCE_VALUE, 0, 0x42))));

        assertDecodeFails(payload);
    }

    @Test
    void decode_fails_on_object_instance_in_multiple_resource() {
        byte[] payload = encode( //
                node(MULTIPLE_RESOURCE, 0, //
                        node(OBJECT_INSTANCE, 0, //
                                leaf(RESOURCE_VALUE, 0, 0x42))));

        assertDecodeFails(payload);
    }

    @Test
    void decode_fails_on_multiple_resource_in_multiple_resource() {
        // this is the shape which allows to build an arbitrarily deep tree
        byte[] payload = encode( //
                node(MULTIPLE_RESOURCE, 0, //
                        node(MULTIPLE_RESOURCE, 0, //
                                leaf(RESOURCE_INSTANCE, 0, 0x42))));

        assertDecodeFails(payload);
    }

    @Test
    void decode_fails_on_resource_value_in_multiple_resource() {
        // only RESOURCE_INSTANCE are allowed inside a MULTIPLE_RESOURCE
        byte[] payload = encode( //
                node(MULTIPLE_RESOURCE, 0, //
                        leaf(RESOURCE_VALUE, 0, 0x42)));

        assertDecodeFails(payload);
    }

    // ---------------------------------------------------------------------------------------------------------------
    // payloads which can not be produced by the encoder : raw bytes
    // ---------------------------------------------------------------------------------------------------------------

    @Test
    void decode_fails_fast_on_deeply_nested_payload() {
        // 8192 bytes of nested MULTIPLE_RESOURCE, each frame is 4 bytes of header (type byte 0x90 = 8-bit identifier +
        // 16-bit length) and declares everything which remains below it : 2048 levels.
        // Without the hierarchy check this built the whole tree and raised a StackOverflowError on a small stack.
        byte[] payload = new byte[8192];
        for (int offset = 0; offset + 4 <= payload.length; offset += 4) {
            int length = payload.length - offset - 4;
            payload[offset] = (byte) 0x90;
            payload[offset + 2] = (byte) (length >> 8);
            payload[offset + 3] = (byte) length;
        }

        assertDecodeFails(payload);
    }

    @Test
    void decode_wraps_error_only_once_whatever_the_nesting_level() {
        // OBJECT_INSTANCE > MULTIPLE_RESOURCE > RESOURCE_INSTANCE whose declared length (7) is bigger than what remains
        byte[] payload = hex("05 00  83 01  47 00 42");

        TlvException ex = assertDecodeFails(payload);

        // the error is raised at depth 3 but must be wrapped once, by the public decode() : ex + 1 cause
        assertEquals(2, causeChainLength(ex));
    }

    // ---------------------------------------------------------------------------------------------------------------
    // helpers
    // ---------------------------------------------------------------------------------------------------------------

    /** A TLV containing other TLVs (OBJECT_INSTANCE or MULTIPLE_RESOURCE) */
    private static Tlv node(TlvType type, int identifier, Tlv... children) {
        return new Tlv(type, children, null, identifier);
    }

    /** A TLV containing a value (RESOURCE_VALUE or RESOURCE_INSTANCE) */
    private static Tlv leaf(TlvType type, int identifier, int... value) {
        byte[] bytes = new byte[value.length];
        for (int i = 0; i < value.length; i++) {
            bytes[i] = (byte) value[i];
        }
        return new Tlv(type, null, bytes, identifier);
    }

    private static byte[] encode(Tlv... tlvs) {
        return TlvEncoder.encode(tlvs).array();
    }

    /** Hex string with optional spaces so each TLV frame can be separated visually. */
    private static byte[] hex(String hex) {
        return Hex.decodeHex(hex.replace(" ", "").toCharArray());
    }

    private static TlvException assertDecodeFails(byte[] payload) {
        return assertThrows(TlvException.class, () -> TlvDecoder.decode(ByteBuffer.wrap(payload)));
    }

    private static int causeChainLength(Throwable throwable) {
        int length = 0;
        for (Throwable cause = throwable; cause != null; cause = cause.getCause()) {
            length++;
        }
        return length;
    }
}
