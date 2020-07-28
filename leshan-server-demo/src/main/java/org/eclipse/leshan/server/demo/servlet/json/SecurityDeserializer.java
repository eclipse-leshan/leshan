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
 *     Rikard Höglund (RISE SICS) - Additions to support OSCORE
 *******************************************************************************/
package org.eclipse.leshan.server.demo.servlet.json;

import java.io.IOException;
import java.lang.reflect.Type;
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

import org.eclipse.californium.cose.AlgorithmID;
import org.eclipse.californium.oscore.OSCoreCtx;
import org.eclipse.californium.oscore.OSException;
import org.eclipse.leshan.core.util.Hex;
import org.eclipse.leshan.core.util.SecurityUtil;
import org.eclipse.leshan.server.security.SecurityInfo;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;

public class SecurityDeserializer implements JsonDeserializer<SecurityInfo> {

    @Override
    public SecurityInfo deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
            throws JsonParseException {

        if (json == null) {
            return null;
        }

        SecurityInfo info = null;

        if (json.isJsonObject()) {
            JsonObject object = (JsonObject) json;

            String endpoint;
            if (object.has("endpoint")) {
                endpoint = object.get("endpoint").getAsString();
            } else {
                throw new JsonParseException("Missing endpoint");
            }

            JsonObject psk = (JsonObject) object.get("psk");
            JsonObject rpk = (JsonObject) object.get("rpk");
            JsonObject oscore = (JsonObject) object.get("oscore");
            JsonPrimitive x509 = object.getAsJsonPrimitive("x509");
            if (psk != null) {
                // PSK Deserialization
                String identity;
                if (psk.has("identity")) {
                    identity = psk.get("identity").getAsString();
                } else {
                    throw new JsonParseException("Missing PSK identity");
                }
                byte[] key;
                try {
                    key = Hex.decodeHex(psk.get("key").getAsString().toCharArray());
                } catch (IllegalArgumentException e) {
                    throw new JsonParseException("key parameter must be a valid hex string", e);
                }

                info = SecurityInfo.newPreSharedKeyInfo(endpoint, identity, key);
            } else if (rpk != null) {
                PublicKey key;
                try {
                    if (rpk.has("key")) {
                        byte[] bytekey = Hex.decodeHex(rpk.get("key").getAsString().toCharArray());
                        key = SecurityUtil.publicKey.decode(bytekey);
                    } else {
                        // This is just needed to keep API backward compatibility.
                        // TODO as this is not used anymore by the UI, we should maybe remove it.
                        byte[] x = Hex.decodeHex(rpk.get("x").getAsString().toCharArray());
                        byte[] y = Hex.decodeHex(rpk.get("y").getAsString().toCharArray());
                        String params = rpk.get("params").getAsString();

                        AlgorithmParameters algoParameters = AlgorithmParameters.getInstance("EC");
                        algoParameters.init(new ECGenParameterSpec(params));
                        ECParameterSpec parameterSpec = algoParameters.getParameterSpec(ECParameterSpec.class);

                        KeySpec keySpec = new ECPublicKeySpec(new ECPoint(new BigInteger(x), new BigInteger(y)),
                                parameterSpec);

                        key = KeyFactory.getInstance("EC").generatePublic(keySpec);
                    }
                } catch (IllegalArgumentException | IOException | GeneralSecurityException e) {
                    throw new JsonParseException("Invalid security info content", e);
                }
                info = SecurityInfo.newRawPublicKeyInfo(endpoint, key);
            } else if (x509 != null && x509.getAsBoolean()) {
                info = SecurityInfo.newX509CertInfo(endpoint);
            } else if (oscore != null) {
                // OSCORE Deserialization

                // Parse hexadecimal context parameters
                byte[] masterSecret = Hex.decodeHex(oscore.get("masterSecret").getAsString().toCharArray());
                byte[] senderId = Hex.decodeHex(oscore.get("senderId").getAsString().toCharArray());
                byte[] recipientId = Hex.decodeHex(oscore.get("recipientId").getAsString().toCharArray());

                // Check parameters that are allowed to be empty
                byte[] masterSalt = null;
                if (oscore.get("masterSalt") != null) {
                    masterSalt = Hex.decodeHex(oscore.get("masterSalt").getAsString().toCharArray());

                    if (masterSalt.length == 0) {
                        masterSalt = null;
                    }
                }

                // ID Context not supported
                byte[] idContext = null;

                // Parse AEAD Algorithm
                AlgorithmID aeadAlgorithm = null;
                try {
                    String aeadAlgorithmStr = oscore.get("aeadAlgorithm").getAsString();
                    aeadAlgorithm = AlgorithmID.valueOf(aeadAlgorithmStr);
                } catch (IllegalArgumentException e) {
                    throw new JsonParseException("Invalid AEAD algorithm", e);
                }
                if (aeadAlgorithm != AlgorithmID.AES_CCM_16_64_128) {
                    throw new JsonParseException("Unsupported AEAD algorithm");
                }

                // Parse HKDF Algorithm
                AlgorithmID hkdfAlgorithm = null;
                try {
                    String hkdfAlgorithmStr = oscore.get("hkdfAlgorithm").getAsString();
                    hkdfAlgorithm = AlgorithmID.valueOf(hkdfAlgorithmStr);
                } catch (IllegalArgumentException e) {
                    throw new JsonParseException("Invalid HKDF algorithm", e);
                }
                if (hkdfAlgorithm != AlgorithmID.HKDF_HMAC_SHA_256) {
                    throw new JsonParseException("Unsupported HKDF algorithm");
                }

                OSCoreCtx ctx = null;
                // Attempt to generate OSCORE Context from parsed parameters
                // Note that the sender and recipient IDs are inverted here
                try {
                    ctx = new OSCoreCtx(masterSecret, true, aeadAlgorithm, recipientId, senderId, hkdfAlgorithm, 32,
                            masterSalt, idContext);
                } catch (OSException e) {
                    throw new JsonParseException("Failed to generate OSCORE context", e);
                }

                info = SecurityInfo.newOSCoreInfo(endpoint, ctx);
            } else {
                throw new JsonParseException("Invalid security info content");
            }
        }

        return info;
    }
}
