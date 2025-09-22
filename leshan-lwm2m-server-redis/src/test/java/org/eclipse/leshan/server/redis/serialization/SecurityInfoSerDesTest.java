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
 *     Sierra Wireless - initial API and implementation
 *******************************************************************************/
package org.eclipse.leshan.server.redis.serialization;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.AlgorithmParameters;
import java.security.KeyFactory;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.ECParameterSpec;
import java.security.spec.ECPoint;
import java.security.spec.ECPublicKeySpec;
import java.security.spec.KeySpec;

import org.eclipse.leshan.core.oscore.AeadAlgorithm;
import org.eclipse.leshan.core.oscore.HkdfAlgorithm;
import org.eclipse.leshan.core.oscore.OscoreSetting;
import org.eclipse.leshan.core.util.Hex;
import org.eclipse.leshan.servers.security.SecurityInfo;
import org.junit.jupiter.api.Test;

public class SecurityInfoSerDesTest {

    private static final String OSCORE_MASTER_SECRET = "1234567890";
    private static final String OSCORE_MASTER_SALT = "0987654321";
    private static final String OSCORE_SENDER_ID = "ABCDEF";
    private static final String OSCORE_RECIPIENT_ID = "FEDCBA";

    @Test
    public void security_info_psk_ser_des_then_equal() {

        SecurityInfo si = SecurityInfo.newPreSharedKeyInfo("myendPoint", "pskIdentity",
                Hex.decodeHex("deadbeef".toCharArray()));

        byte[] data = SecurityInfoSerDes.serialize(si);
        assertEquals("{\"ep\":\"myendPoint\",\"id\":\"pskIdentity\",\"psk\":\"deadbeef\"}", new String(data));
        assertEquals(si, SecurityInfoSerDes.deserialize(data));
    }

    @Test
    public void security_info_rpk_ser_des_then_equal() throws Exception {
        byte[] publicX = Hex
                .decodeHex("89c048261979208666f2bfb188be1968fc9021c416ce12828c06f4e314c167b5".toCharArray());
        byte[] publicY = Hex
                .decodeHex("cbf1eb7587f08e01688d9ada4be859137ca49f79394bad9179326b3090967b68".toCharArray());
        // Get Elliptic Curve Parameter spec for secp256r1
        AlgorithmParameters algoParameters = AlgorithmParameters.getInstance("EC");
        algoParameters.init(new ECGenParameterSpec("secp256r1"));
        ECParameterSpec parameterSpec = algoParameters.getParameterSpec(ECParameterSpec.class);

        // Create key specs
        KeySpec publicKeySpec = new ECPublicKeySpec(new ECPoint(new BigInteger(1, publicX), new BigInteger(1, publicY)),
                parameterSpec);

        SecurityInfo si = SecurityInfo.newRawPublicKeyInfo("myendpoint",
                KeyFactory.getInstance("EC").generatePublic(publicKeySpec));

        byte[] data = SecurityInfoSerDes.serialize(si);

        assertEquals(
                "{\"ep\":\"myendpoint\",\"rpk\":{\"x\":\"89c048261979208666f2bfb188be1968fc9021c416ce12828c06f4e314c167b5\",\"y\":\"cbf1eb7587f08e01688d9ada4be859137ca49f79394bad9179326b3090967b68\",\"params\":\"secp256r1\"}}",
                new String(data));
        assertEquals(si, SecurityInfoSerDes.deserialize(data));
    }

    @Test

    void security_info_oscore_ser_des_then_equal() {

        OscoreSetting oscoreSetting = new OscoreSetting(OSCORE_SENDER_ID.getBytes(StandardCharsets.UTF_8),
                OSCORE_RECIPIENT_ID.getBytes(StandardCharsets.UTF_8),
                OSCORE_MASTER_SECRET.getBytes(StandardCharsets.UTF_8), AeadAlgorithm.AES_CCM_16_64_128,
                HkdfAlgorithm.HKDF_HMAC_SHA_256, OSCORE_MASTER_SALT.getBytes(StandardCharsets.UTF_8));

        SecurityInfo si = SecurityInfo.newOscoreInfo("myendPoint", oscoreSetting);
        byte[] data = SecurityInfoSerDes.serialize(si);

        assertEquals(si, SecurityInfoSerDes.deserialize(data));
    }

    @Test
    public void testOscoreMasterSalt_NullValue() {

        OscoreSetting oscoreSetting = new OscoreSetting(OSCORE_SENDER_ID.getBytes(StandardCharsets.UTF_8),
                OSCORE_RECIPIENT_ID.getBytes(StandardCharsets.UTF_8),
                OSCORE_MASTER_SECRET.getBytes(StandardCharsets.UTF_8), AeadAlgorithm.AES_CCM_16_64_128,
                HkdfAlgorithm.HKDF_HMAC_SHA_256, null);

        SecurityInfo si = SecurityInfo.newOscoreInfo("myendPoint", oscoreSetting);
        byte[] dataserialized = SecurityInfoSerDes.serialize(si);

        SecurityInfo datarestored = SecurityInfoSerDes.deserialize(dataserialized);

        assertNotNull(datarestored);
        assertNotNull(datarestored.getOscoreSetting());
        assertNotNull(datarestored.getOscoreSetting().getMasterSalt(),
                "Expected masterSalt not to be null after serialization/deserialization");
        assertEquals(0, datarestored.getOscoreSetting().getMasterSalt().length,
                "Expected masterSalt to be an empty array after serialization/deserialization");
    }

    @Test
    public void testOscoreMasterSalt_EmptyArray() {

        OscoreSetting oscoreSetting = new OscoreSetting(OSCORE_SENDER_ID.getBytes(StandardCharsets.UTF_8),
                OSCORE_RECIPIENT_ID.getBytes(StandardCharsets.UTF_8),
                OSCORE_MASTER_SECRET.getBytes(StandardCharsets.UTF_8), AeadAlgorithm.AES_CCM_16_64_128,
                HkdfAlgorithm.HKDF_HMAC_SHA_256, new byte[0]);

        SecurityInfo si = SecurityInfo.newOscoreInfo("myendPoint", oscoreSetting);
        byte[] serialized = SecurityInfoSerDes.serialize(si);

        SecurityInfo datarestored = SecurityInfoSerDes.deserialize(serialized);

        assertNotNull(datarestored);
        assertNotNull(datarestored.getOscoreSetting());
        assertEquals(0, datarestored.getOscoreSetting().getMasterSalt().length,
                "Expected masterSalt to be an empty byte array");
    }
}
