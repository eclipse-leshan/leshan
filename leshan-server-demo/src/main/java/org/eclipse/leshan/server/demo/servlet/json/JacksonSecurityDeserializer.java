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
 *     Orange - keep one JSON dependency
 *******************************************************************************/
package org.eclipse.leshan.server.demo.servlet.json;

import java.io.IOException;
import java.math.BigInteger;
import java.security.AlgorithmParameters;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.ECParameterSpec;
import java.security.spec.ECPoint;
import java.security.spec.ECPublicKeySpec;
import java.security.spec.KeySpec;

import org.eclipse.leshan.core.util.Hex;
import org.eclipse.leshan.core.util.SecurityUtil;
import org.eclipse.leshan.server.security.SecurityInfo;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;

public class JacksonSecurityDeserializer extends JsonDeserializer<SecurityInfo> {

    @Override
    public SecurityInfo deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {

        JsonNode node = p.getCodec().readTree(p);

        SecurityInfo info = null;

        if (node.isObject()) {

            String endpoint;
            if (node.has("endpoint")) {
                endpoint = node.get("endpoint").asText();
            } else {
                throw new JsonParseException(p, "Missing endpoint");
            }

            JsonNode psk = node.get("psk");
            JsonNode rpk = node.get("rpk");
            JsonNode x509 = node.get("x509");
            if (psk != null) {
                // PSK Deserialization
                String identity;
                if (psk.has("identity")) {
                    identity = psk.get("identity").asText();
                } else {
                    throw new JsonParseException(p, "Missing PSK identity");
                }
                byte[] key;
                try {
                    key = Hex.decodeHex(psk.get("key").asText().toCharArray());
                } catch (IllegalArgumentException e) {
                    throw new JsonParseException(p, "key parameter must be a valid hex string", e);
                }

                info = SecurityInfo.newPreSharedKeyInfo(endpoint, identity, key);
            } else if (rpk != null) {
                PublicKey key;
                try {
                    if (rpk.has("key")) {
                        byte[] bytekey = Hex.decodeHex(rpk.get("key").asText().toCharArray());
                        key = SecurityUtil.publicKey.decode(bytekey);
                    } else {
                        // This is just needed to keep API backward compatibility.
                        // TODO as this is not used anymore by the UI, we should maybe remove it.
                        byte[] x = Hex.decodeHex(rpk.get("x").asText().toCharArray());
                        byte[] y = Hex.decodeHex(rpk.get("y").asText().toCharArray());
                        String params = rpk.get("params").asText();

                        AlgorithmParameters algoParameters = AlgorithmParameters.getInstance("EC");
                        algoParameters.init(new ECGenParameterSpec(params));
                        ECParameterSpec parameterSpec = algoParameters.getParameterSpec(ECParameterSpec.class);

                        KeySpec keySpec = new ECPublicKeySpec(new ECPoint(new BigInteger(1, x), new BigInteger(1, y)),
                                parameterSpec);

                        key = KeyFactory.getInstance("EC").generatePublic(keySpec);
                    }
                } catch (IllegalArgumentException | IOException | GeneralSecurityException e) {
                    throw new JsonParseException(p, "Invalid security info content", e);
                }
                info = SecurityInfo.newRawPublicKeyInfo(endpoint, key);
            } else if (x509 != null && x509.asBoolean()) {
                info = SecurityInfo.newX509CertInfo(endpoint);
            } else {
                throw new JsonParseException(p, "Invalid security info content");
            }
        }

        return info;
    }
}
