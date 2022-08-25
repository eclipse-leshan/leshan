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
 *     Orange - keep one JSON dependency
 *******************************************************************************/
package org.eclipse.leshan.server.redis.serialization;

import java.math.BigInteger;
import java.security.AlgorithmParameters;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.ECParameterSpec;
import java.security.spec.ECPoint;
import java.security.spec.ECPublicKeySpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.InvalidParameterSpecException;
import java.security.spec.KeySpec;
import java.util.Arrays;

import org.eclipse.leshan.core.util.Hex;
import org.eclipse.leshan.server.security.SecurityInfo;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Functions for serialize and deserialize security information in JSON for storage.
 */
public class SecurityInfoSerDes {

    public static byte[] serialize(SecurityInfo s) {
        ObjectNode o = JsonNodeFactory.instance.objectNode();
        o.put("ep", s.getEndpoint());
        if (s.getPskIdentity() != null) {
            o.put("id", s.getPskIdentity());
        }
        if (s.getPreSharedKey() != null) {
            o.put("psk", Hex.encodeHexString(s.getPreSharedKey()));
        }
        if (s.getRawPublicKey() != null) {
            ObjectNode rpk = JsonNodeFactory.instance.objectNode();
            ECPublicKey ecPublicKey = (ECPublicKey) s.getRawPublicKey();
            // Get x coordinate
            byte[] x = ecPublicKey.getW().getAffineX().toByteArray();
            if (x[0] == 0)
                x = Arrays.copyOfRange(x, 1, x.length);
            rpk.put("x", Hex.encodeHexString(x));

            // Get Y coordinate
            byte[] y = ecPublicKey.getW().getAffineY().toByteArray();
            if (y[0] == 0)
                y = Arrays.copyOfRange(y, 1, y.length);
            rpk.put("y", Hex.encodeHexString(y));

            // Get Curves params
            ecPublicKey.getParams();

            // use only the first part as the curve name
            rpk.put("params", ecPublicKey.getParams().toString().split(" ")[0]);
            o.set("rpk", rpk);
        }

        if (s.useX509Cert()) {
            o.put("x509", true);
        }

        return o.toString().getBytes();
    }

    public static SecurityInfo deserialize(byte[] data) {
        SecurityInfo i;
        try {
            JsonNode o = new ObjectMapper().readTree(new String(data));

            String ep = o.get("ep").asText();
            if (o.get("psk") != null) {
                i = SecurityInfo.newPreSharedKeyInfo(ep, o.get("id").asText(),
                        Hex.decodeHex(o.get("psk").asText().toCharArray()));
            } else if (o.get("x509") != null) {
                i = SecurityInfo.newX509CertInfo(ep);
            } else {
                JsonNode rpk = o.get("rpk");
                PublicKey key;

                byte[] x = Hex.decodeHex(rpk.get("x").asText().toCharArray());
                byte[] y = Hex.decodeHex(rpk.get("y").asText().toCharArray());
                String params = rpk.get("params").asText();
                AlgorithmParameters algoParameters = AlgorithmParameters.getInstance("EC");
                algoParameters.init(new ECGenParameterSpec(params));
                ECParameterSpec parameterSpec = algoParameters.getParameterSpec(ECParameterSpec.class);

                KeySpec keySpec = new ECPublicKeySpec(new ECPoint(new BigInteger(1, x), new BigInteger(1, y)),
                        parameterSpec);

                key = KeyFactory.getInstance("EC").generatePublic(keySpec);

                i = SecurityInfo.newRawPublicKeyInfo(ep, key);
            }

        } catch (IllegalArgumentException | InvalidKeySpecException | NoSuchAlgorithmException
                | InvalidParameterSpecException | JsonProcessingException e) {
            throw new IllegalStateException("Invalid security info content", e);
        }
        return i;
    }

}
