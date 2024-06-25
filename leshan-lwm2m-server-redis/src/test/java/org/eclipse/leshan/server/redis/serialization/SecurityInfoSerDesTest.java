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

import java.math.BigInteger;
import java.security.AlgorithmParameters;
import java.security.KeyFactory;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.ECParameterSpec;
import java.security.spec.ECPoint;
import java.security.spec.ECPublicKeySpec;
import java.security.spec.KeySpec;

import org.eclipse.leshan.core.util.Hex;
import org.eclipse.leshan.server.security.SecurityInfo;
import org.junit.jupiter.api.Test;

public class SecurityInfoSerDesTest {

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
}
