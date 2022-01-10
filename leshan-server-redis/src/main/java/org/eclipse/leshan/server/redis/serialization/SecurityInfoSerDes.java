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

import org.eclipse.californium.cose.AlgorithmID;
import org.eclipse.californium.cose.CoseException;
import org.eclipse.californium.oscore.OSCoreCtx;
import org.eclipse.californium.oscore.OSException;
import org.eclipse.leshan.core.OscoreObject;
import org.eclipse.leshan.core.util.Hex;
import org.eclipse.leshan.server.security.SecurityInfo;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;
import com.upokecenter.cbor.CBORObject;

/**
 * Functions for serialize and deserialize security information in JSON for storage.
 */
public class SecurityInfoSerDes {

    public static byte[] serialize(SecurityInfo s) {
        JsonObject o = Json.object();
        o.set("ep", s.getEndpoint());
        if (s.getIdentity() != null) {
            o.set("id", s.getIdentity());
        }
        if (s.getPreSharedKey() != null) {
            o.set("psk", Hex.encodeHexString(s.getPreSharedKey()));
        }
        if (s.getRawPublicKey() != null) {
            JsonObject rpk = new JsonObject();
            ECPublicKey ecPublicKey = (ECPublicKey) s.getRawPublicKey();
            // Get x coordinate
            byte[] x = ecPublicKey.getW().getAffineX().toByteArray();
            if (x[0] == 0)
                x = Arrays.copyOfRange(x, 1, x.length);
            rpk.add("x", Hex.encodeHexString(x));

            // Get Y coordinate
            byte[] y = ecPublicKey.getW().getAffineY().toByteArray();
            if (y[0] == 0)
                y = Arrays.copyOfRange(y, 1, y.length);
            rpk.add("y", Hex.encodeHexString(y));

            // Get Curves params
            ecPublicKey.getParams();

            // use only the first part as the curve name
            rpk.add("params", ecPublicKey.getParams().toString().split(" ")[0]);
            o.set("rpk", rpk);
        }

        if (s.useX509Cert()) {
            o.set("x509", true);
        }

        if (s.useOSCORE()) {
            JsonObject oscore = new JsonObject();

            OscoreObject oscoreObject = s.getOscoreObject();
            oscore.add("senderId", Hex.encodeHexString(oscoreObject.getSenderId()));
            oscore.add("recipientId", Hex.encodeHexString(oscoreObject.getRecipientId()));
            oscore.add("masterSecret", Hex.encodeHexString(oscoreObject.getMasterSecret()));
            oscore.add("aeadAlgorithm", oscoreObject.getAeadAlgorithm());
            oscore.add("hmacAlgorithm", oscoreObject.getHmacAlgorithm());
            oscore.add("masterSalt", Hex.encodeHexString(oscoreObject.getMasterSalt()));

            o.set("oscore", oscore);
        }

        return o.toString().getBytes();
    }

    public static SecurityInfo deserialize(byte[] data) {
        JsonObject o = (JsonObject) Json.parse(new String(data));

        SecurityInfo i;
        String ep = o.getString("ep", null);
        if (o.get("psk") != null) {
            i = SecurityInfo.newPreSharedKeyInfo(ep, o.getString("id", null),
                    Hex.decodeHex(o.getString("psk", null).toCharArray()));
        } else if (o.get("x509") != null) {
            i = SecurityInfo.newX509CertInfo(ep);
        } else if (o.get("oscore") != null) {
            JsonObject oscore = (JsonObject)o.get("oscore");

            byte[] senderId = Hex.decodeHex(oscore.getString("senderId", null).toCharArray());
            byte[] recipientId = Hex.decodeHex(oscore.getString("recipientId", null).toCharArray());
            byte[] masterSecret = Hex.decodeHex(oscore.getString("masterSecret", null).toCharArray());
            byte[] masterSalt = Hex.decodeHex(oscore.getString("masterSalt", null).toCharArray());

            try {
                AlgorithmID aeadAlgorithm = AlgorithmID.FromCBOR(CBORObject.FromObject(oscore.getInt("aeadAlgorithm", 0)));
                AlgorithmID hmacAlgorithm = AlgorithmID.FromCBOR(CBORObject.FromObject(oscore.getInt("hmacAlgorithm", 0)));

                OSCoreCtx oscoreCtx = new OSCoreCtx(masterSecret, false, aeadAlgorithm, senderId, recipientId,
                        hmacAlgorithm, null, masterSalt, null);
                i = SecurityInfo.newOSCoreInfo(ep, oscoreCtx);

            } catch (CoseException | OSException e) {
                throw new IllegalStateException("Invalid security info content", e);
            }
        } else {
            JsonObject rpk = (JsonObject) o.get("rpk");
            PublicKey key;
            try {
                byte[] x = Hex.decodeHex(rpk.getString("x", null).toCharArray());
                byte[] y = Hex.decodeHex(rpk.getString("y", null).toCharArray());
                String params = rpk.getString("params", null);
                AlgorithmParameters algoParameters = AlgorithmParameters.getInstance("EC");
                algoParameters.init(new ECGenParameterSpec(params));
                ECParameterSpec parameterSpec = algoParameters.getParameterSpec(ECParameterSpec.class);

                KeySpec keySpec = new ECPublicKeySpec(new ECPoint(new BigInteger(x), new BigInteger(y)), parameterSpec);

                key = KeyFactory.getInstance("EC").generatePublic(keySpec);
            } catch (IllegalArgumentException | InvalidKeySpecException | NoSuchAlgorithmException
                    | InvalidParameterSpecException e) {
                throw new IllegalStateException("Invalid security info content", e);
            }
            i = SecurityInfo.newRawPublicKeyInfo(ep, key);
        }
        return i;
    }

}
