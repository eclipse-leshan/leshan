/*******************************************************************************
 * Copyright (c) 2017 Bosch Software Innovations GmbH and others.
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
 *    Achim Kraus (Bosch Software Innovations GmbH) - initial implementation.
 ******************************************************************************/
package org.eclipse.leshan.server.redis.serialization;

import java.net.InetSocketAddress;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;

import org.eclipse.californium.elements.EndpointContext;
import org.eclipse.leshan.core.request.Identity;
import org.eclipse.leshan.core.util.Hex;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;

/**
 * Functions for serializing and deserializing a Californium {@link EndpointContext} in JSON.
 */
public class IdentitySerDes {

    private static final String KEY_ADDRESS = "address";
    private static final String KEY_PORT = "port";
    private static final String KEY_ID = "id";
    private static final String KEY_CN = "cn";
    private static final String KEY_RPK = "rpk";

    public static JsonObject serialize(Identity identity) {
        JsonObject o = Json.object();
        o.set(KEY_ADDRESS, identity.getPeerAddress().getHostString());
        o.set(KEY_PORT, identity.getPeerAddress().getPort());
        if (identity.isPSK()) {
            o.set(KEY_ID, identity.getPskIdentity());
        } else if (identity.isRPK()) {
            PublicKey publicKey = identity.getRawPublicKey();
            o.set(KEY_RPK, Hex.encodeHexString(publicKey.getEncoded()));
        } else if (identity.isX509()) {
            o.set(KEY_CN, identity.getX509CommonName());
        }
        return o;
    }

    public static Identity deserialize(JsonObject peer) {
        String address = peer.get(KEY_ADDRESS).asString();
        int port = peer.get(KEY_PORT).asInt();

        JsonValue jpsk = peer.get(KEY_ID);
        if (jpsk != null) {
            return Identity.psk(new InetSocketAddress(address, port), jpsk.asString());
        }

        JsonValue jrpk = peer.get(KEY_RPK);
        if (jrpk != null) {
            try {
                byte[] rpk = Hex.decodeHex(jrpk.asString().toCharArray());
                X509EncodedKeySpec spec = new X509EncodedKeySpec(rpk);
                PublicKey publicKey = KeyFactory.getInstance("EC").generatePublic(spec);
                return Identity.rpk(new InetSocketAddress(address, port), publicKey);
            } catch (InvalidKeySpecException | NoSuchAlgorithmException e) {
                throw new IllegalStateException("Invalid security info content", e);
            }
        }

        JsonValue jcn = peer.get(KEY_CN);
        if (jcn != null) {
            return Identity.x509(new InetSocketAddress(address, port), jcn.asString());
        }

        return Identity.unsecure(new InetSocketAddress(address, port));
    }
}
