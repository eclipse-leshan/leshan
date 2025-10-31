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

import org.eclipse.leshan.core.oscore.OscoreSetting;
import org.eclipse.leshan.core.util.Hex;
import org.eclipse.leshan.servers.security.SecurityInfo;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Functions for serialize and deserialize security information in JSON for storage.
 */
public class SecurityInfoSerDes {

    private static final String KEY_ID = "id";
    private static final String KEY_EP = "ep";
    // PSK
    private static final String KEY_PSK = "psk";
    // RPK
    private static final String KEY_RPK = "rpk";
    private static final String KEY_RPK_X = "x";
    private static final String KEY_RPK_Y = "y";
    private static final String KEY_RPK_PARAMS = "params";
    // X509
    private static final String KEY_X509 = "x509";
    // OSCORE
    private static final String KEY_OSCORE = "oscore";
    private static final String KEY_OSCORE_SENDER_ID = "senderId";
    private static final String KEY_OSCORE_RECIPIENT_ID = "recipientId";
    private static final String KEY_OSCORE_MASTER_SECRET = "masterSecret";
    private static final String KEY_OSCORE_MASTER_SALT = "masterSalt";
    private static final String KEY_OSCORE_AEAD_ALGORITHM = "aeadAlgorithm";
    private static final String KEY_OSCORE_HMAC_ALGORITHM = "hmacAlgorithm";

    public static byte[] serialize(SecurityInfo s) {
        ObjectNode o = JsonNodeFactory.instance.objectNode();
        o.put(KEY_EP, s.getEndpoint());
        if (s.getPskIdentity() != null) {
            o.put(KEY_ID, s.getPskIdentity());
        }
        if (s.getPreSharedKey() != null) {
            o.put(KEY_PSK, Hex.encodeHexString(s.getPreSharedKey()));
        }
        if (s.getRawPublicKey() != null) {
            ObjectNode rpk = JsonNodeFactory.instance.objectNode();
            ECPublicKey ecPublicKey = (ECPublicKey) s.getRawPublicKey();
            // Get x coordinate
            byte[] x = ecPublicKey.getW().getAffineX().toByteArray();
            if (x[0] == 0)
                x = Arrays.copyOfRange(x, 1, x.length);
            rpk.put(KEY_RPK_X, Hex.encodeHexString(x));

            // Get Y coordinate
            byte[] y = ecPublicKey.getW().getAffineY().toByteArray();
            if (y[0] == 0)
                y = Arrays.copyOfRange(y, 1, y.length);
            rpk.put(KEY_RPK_Y, Hex.encodeHexString(y));

            // Get Curves params
            ecPublicKey.getParams();

            // use only the first part as the curve name
            rpk.put(KEY_RPK_PARAMS, ecPublicKey.getParams().toString().split(" ")[0]);
            o.set(KEY_RPK, rpk);
        }

        if (s.useX509Cert()) {
            o.put(KEY_X509, true);
        }

        if (s.useOSCORE()) {

            ObjectNode oscore = JsonNodeFactory.instance.objectNode();
            OscoreSetting oscoreObject = s.getOscoreSetting();

            oscore.put(KEY_OSCORE_SENDER_ID, Hex.encodeHexString(oscoreObject.getSenderId()));
            oscore.put(KEY_OSCORE_RECIPIENT_ID, Hex.encodeHexString(oscoreObject.getRecipientId()));
            oscore.put(KEY_OSCORE_MASTER_SECRET, Hex.encodeHexString(oscoreObject.getMasterSecret()));
            oscore.put(KEY_OSCORE_AEAD_ALGORITHM, oscoreObject.getAeadAlgorithm().getValue());
            oscore.put(KEY_OSCORE_HMAC_ALGORITHM, oscoreObject.getHkdfAlgorithm().getValue());
            byte[] masterSalt = oscoreObject.getMasterSalt();
            if (masterSalt.length > 0) {
                oscore.put(KEY_OSCORE_MASTER_SALT, Hex.encodeHexString(masterSalt));
            }

            o.set(KEY_OSCORE, oscore);
        }
        return o.toString().getBytes();
    }

    public static SecurityInfo deserialize(byte[] data) {
        SecurityInfo i;
        try {
            JsonNode o = new ObjectMapper().readTree(new String(data));

            String ep = o.get(KEY_EP).asText();
            if (o.get(KEY_PSK) != null) {
                i = SecurityInfo.newPreSharedKeyInfo(ep, o.get(KEY_ID).asText(),
                        Hex.decodeHex(o.get(KEY_PSK).asText().toCharArray()));
            } else if (o.get(KEY_X509) != null) {
                i = SecurityInfo.newX509CertInfo(ep);
            } else if (o.get(KEY_OSCORE) != null) {
                JsonNode oscore = o.get(KEY_OSCORE);

                byte[] senderId = Hex.decodeHex(oscore.get(KEY_OSCORE_SENDER_ID).asText().toCharArray());
                byte[] recipientId = Hex.decodeHex(oscore.get(KEY_OSCORE_RECIPIENT_ID).asText().toCharArray());
                byte[] masterSecret = Hex.decodeHex(oscore.get(KEY_OSCORE_MASTER_SECRET).asText().toCharArray());
                byte[] masterSalt = null;
                if (oscore.has(KEY_OSCORE_MASTER_SALT)) {
                    masterSalt = Hex.decodeHex(oscore.get(KEY_OSCORE_MASTER_SALT).asText().toCharArray());
                }

                int aeadAlgId = oscore.get(KEY_OSCORE_AEAD_ALGORITHM).asInt();
                int hmacAlgId = oscore.get(KEY_OSCORE_HMAC_ALGORITHM).asInt();

                OscoreSetting oscoreSetting = new OscoreSetting(senderId, recipientId, masterSecret, aeadAlgId,
                        hmacAlgId, masterSalt);
                i = SecurityInfo.newOscoreInfo(ep, oscoreSetting);

            } else {
                JsonNode rpk = o.get(KEY_RPK);
                PublicKey key;

                byte[] x = Hex.decodeHex(rpk.get(KEY_RPK_X).asText().toCharArray());
                byte[] y = Hex.decodeHex(rpk.get(KEY_RPK_Y).asText().toCharArray());
                String params = rpk.get(KEY_RPK_PARAMS).asText();
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
