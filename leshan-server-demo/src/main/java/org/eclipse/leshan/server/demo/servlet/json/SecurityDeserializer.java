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
            } else {
                throw new JsonParseException("Invalid security info content");
            }
        }

        return info;
    }
}
