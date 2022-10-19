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
 *    Orange - keep one JSON dependency
 ******************************************************************************/
package org.eclipse.leshan.server.redis.serialization;

import java.net.InetSocketAddress;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;

import org.eclipse.leshan.core.request.Identity;
import org.eclipse.leshan.core.util.Hex;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class IdentitySerDes {

    private static final String KEY_ADDRESS = "address";
    private static final String KEY_PORT = "port";
    private static final String KEY_ID = "id";
    private static final String KEY_CN = "cn";
    private static final String KEY_RPK = "rpk";

    public static JsonNode serialize(Identity identity) {
        ObjectNode o = JsonNodeFactory.instance.objectNode();
        o.put(KEY_ADDRESS, identity.getPeerAddress().getHostString());
        o.put(KEY_PORT, identity.getPeerAddress().getPort());
        if (identity.isPSK()) {
            o.put(KEY_ID, identity.getPskIdentity());
        } else if (identity.isRPK()) {
            PublicKey publicKey = identity.getRawPublicKey();
            o.put(KEY_RPK, Hex.encodeHexString(publicKey.getEncoded()));
        } else if (identity.isX509()) {
            o.put(KEY_CN, identity.getX509CommonName());
        }
        return o;
    }

    public static Identity deserialize(JsonNode peer) {
        String address = peer.get(KEY_ADDRESS).asText();
        int port = peer.get(KEY_PORT).asInt();

        JsonNode jpsk = peer.get(KEY_ID);
        if (jpsk != null) {
            return Identity.psk(new InetSocketAddress(address, port), jpsk.asText());
        }

        JsonNode jrpk = peer.get(KEY_RPK);
        if (jrpk != null) {
            try {
                byte[] rpk = Hex.decodeHex(jrpk.asText().toCharArray());
                X509EncodedKeySpec spec = new X509EncodedKeySpec(rpk);
                PublicKey publicKey = KeyFactory.getInstance("EC").generatePublic(spec);
                return Identity.rpk(new InetSocketAddress(address, port), publicKey);
            } catch (InvalidKeySpecException | NoSuchAlgorithmException e) {
                throw new IllegalStateException("Invalid security info content", e);
            }
        }

        JsonNode jcn = peer.get(KEY_CN);
        if (jcn != null) {
            return Identity.x509(new InetSocketAddress(address, port), jcn.asText());
        }

        return Identity.unsecure(new InetSocketAddress(address, port));
    }
}
