/*******************************************************************************
 * Copyright (c) 2016 Sierra Wireless and others.
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
 *     Bartosz Stolarczyk
 *     Orange Polska S.A. - initial API and implementation
 *******************************************************************************/
package org.eclipse.leshan.server.bootstrap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;

import org.eclipse.leshan.core.util.Hex;
import org.eclipse.leshan.core.util.datatype.ULong;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class BootstrapConfigTest {

    @ParameterizedTest
    @CsvSource(value = { // given 2 bytes, expected ToString()
            "C0A8 | 0xC0,0xA8", // TLS_PSK_WITH_AES_128_CCM_8
            "0003 | 0x00,0x03", // TLS_RSA_EXPORT_WITH_RC4_40_MD5
    }, delimiter = '|')
    public void test_toString(String input, String expectedResult) {
        // Given 2 bytes
        byte[] decoded = Hex.decodeHex(input.toCharArray());

        // Create CipherSuiteId
        BootstrapConfig.CipherSuiteId cipherSuiteId = new BootstrapConfig.CipherSuiteId(decoded[0], decoded[1]);

        // Assert if bytes were correctly phrased
        assertEquals(expectedResult, cipherSuiteId.toString());
    }

    @Test
    public void test_create_new_CipherSuiteId_from_ULong() {
        // Create CipherSuiteId with ULong
        BootstrapConfig.CipherSuiteId cipherSuiteId = new BootstrapConfig.CipherSuiteId(ULong.valueOf(49320));

        // Assert if ULong was correctly phrased
        assertEquals("0xC0,0xA8", cipherSuiteId.toString());
    }

    @Test
    public void test_getValueForSecurityObject() {
        // Create example ULong
        ULong testValue = ULong.valueOf(49320);

        // Create cipherSuiteId from ULong
        BootstrapConfig.CipherSuiteId cipherSuiteId = new BootstrapConfig.CipherSuiteId(testValue);

        // Check if getValueForSecurityObject() returns input ULong
        assertEquals(testValue, cipherSuiteId.getValueForSecurityObject());
    }

    @ParameterizedTest
    @ValueSource(strings = { //
            "2147483647", // max 32-bit signed Integer
            "2147483648", // max 32-bit signed Integer + 1
            "65536"// max 16-bit signed Integer +1
    })
    public void test_error_thrown_for_invalid_ulong(String invalidULong) {
        // Create ULong from String
        ULong testValue = ULong.valueOf(invalidULong);

        // Try to create CipherSuiteId with ULong outside of range
        assertThrowsExactly(IllegalArgumentException.class, () -> new BootstrapConfig.CipherSuiteId(testValue));
    }
}
