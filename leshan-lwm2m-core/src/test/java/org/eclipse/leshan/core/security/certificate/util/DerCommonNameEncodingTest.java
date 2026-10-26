/*******************************************************************************
 * Copyright (c) 2026    Sierra Wireless and others.
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
package org.eclipse.leshan.core.security.certificate.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.util.Arrays;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Checks the bytes produced by the encoder of {@link DerCommonName}. {@link javax.security.auth.x500.X500Principal}
 * would hide a non minimal encoding, as the JDK re-encodes what it parses, so this test does not use it.
 */
class DerCommonNameEncodingTest {

    @Test
    @SuppressWarnings("java:S125")
    void encode_name_of_a_short_common_name() {
        // SEQUENCE { SET { SEQUENCE { OID 2.5.4.3, UTF8String "a" } } }
        assertArrayEquals(hex("300c310a300806035504030c0161"), DerCommonName.encodeName("a"));
    }

    @Test
    void encode_name_of_a_common_name_of_128_characters() {
        // 128 is the first length which needs the long form, and it makes all the enclosing lengths use it too
        byte[] expected = concat(hex("30818e3181 8b308188 0603550403 0c8180"), repeat('a', 128));

        assertArrayEquals(expected, DerCommonName.encodeName(new String(repeat('a', 128))));
    }

    static Stream<Arguments> lengths() {
        return Stream.of(//
                arguments(0, "00"), //
                arguments(127, "7f"), //
                arguments(128, "8180"), //
                arguments(255, "81ff"), //
                arguments(256, "820100"), //
                arguments(65_535, "82ffff"), //
                arguments(65_536, "83010000"), //
                arguments(0xFFFFFF, "83ffffff"));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("lengths")
    void encode_length_in_its_minimal_form(int length, String expected) {
        assertArrayEquals(hex(expected), DerCommonName.encodeLength(length));
    }

    @Test
    void encode_length_refuses_what_the_reader_does_not_accept() {
        // the reader accepts at most 3 length octets
        assertThrows(IllegalArgumentException.class, () -> DerCommonName.encodeLength(0x1000000));
        assertThrows(IllegalArgumentException.class, () -> DerCommonName.encodeLength(Integer.MAX_VALUE));
    }

    @Test
    void reader_reads_back_the_encoding_around_the_boundaries_of_the_length_encoding() {
        int[] lengths = IntStream.concat(IntStream.concat(IntStream.rangeClosed(100, 140), //
                IntStream.rangeClosed(250, 260)), IntStream.rangeClosed(65_530, 65_540)).toArray();

        for (int length : lengths) {
            String commonName = new String(repeat('a', length));

            assertEquals(commonName, DerCommonName.extractCN(DerCommonName.encodeName(commonName)),
                    "common name of " + length + " characters");
        }
    }

    private static byte[] hex(String hex) {
        String digits = hex.replace(" ", "");
        byte[] bytes = new byte[digits.length() / 2];
        for (int i = 0; i < bytes.length; i++) {
            bytes[i] = (byte) Integer.parseInt(digits.substring(2 * i, 2 * i + 2), 16);
        }
        return bytes;
    }

    private static byte[] repeat(char c, int count) {
        byte[] bytes = new byte[count];
        Arrays.fill(bytes, (byte) c);
        return bytes;
    }

    private static byte[] concat(byte[] first, byte[] second) {
        byte[] result = Arrays.copyOf(first, first.length + second.length);
        System.arraycopy(second, 0, result, first.length, second.length);
        return result;
    }
}
