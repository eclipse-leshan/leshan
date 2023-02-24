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

import org.eclipse.leshan.core.util.Hex;
import org.eclipse.leshan.core.util.datatype.ULong;
import org.junit.jupiter.api.Test;

class BootstrapConfigTest {

    @Test
    public void CipherSuiteId_encode_from_two_bytes() {
        // Create 2 bytes
        byte[] decoded = Hex.decodeHex("C0A8".toCharArray());
        Byte firstByte = decoded[0];
        Byte secoundByte = decoded[1];

        // Create CipherSuiteId with two bytes
        BootstrapConfig.CipherSuiteId cipherSuiteId = new BootstrapConfig.CipherSuiteId(firstByte, secoundByte);

        // Assert if bytes were correctly phrased
        assertEquals("c0,a8", cipherSuiteId.toString());
    }

    @Test
    public void CipherSuiteId_encode_from_ULong() {
        // Create CipherSuiteId with ULong
        BootstrapConfig.CipherSuiteId cipherSuiteId = new BootstrapConfig.CipherSuiteId(ULong.valueOf(49320));

        // Assert if ULong was correctly phrased
        assertEquals("c0,a8", cipherSuiteId.toString());
    }

    @Test
    public void getValueForSecurityObject() {
        // Create example ULong
        ULong testValue = ULong.valueOf(49320);

        // Create cipherSuiteId from ULong
        BootstrapConfig.CipherSuiteId cipherSuiteId = new BootstrapConfig.CipherSuiteId(testValue);

        // Check if getValueForSecurityObject() returns input ULong
        assertEquals(testValue, cipherSuiteId.getValueForSecurityObject());
    }

    @Test
    public void is_error_thrown_for_too_big_values() {
        // Create ULong with value bigger than 65535
        ULong testValue = ULong.valueOf(65536);

        // Try to create CipherSuiteId with ULong outside of range
        try {
            new BootstrapConfig.CipherSuiteId(testValue);
            assert (false);
        } catch (IllegalArgumentException e) {
            // If IllegalArgumentException is caught pass the test
            assert (true);
        }
    }
}
