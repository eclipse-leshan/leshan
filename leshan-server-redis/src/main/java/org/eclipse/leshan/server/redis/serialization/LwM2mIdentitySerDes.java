/*******************************************************************************
 * Copyright (c) 2023 Sierra Wireless and others.
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

import java.net.InetSocketAddress;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;

import org.eclipse.leshan.core.peer.LwM2mIdentity;
import org.eclipse.leshan.core.peer.PskIdentity;
import org.eclipse.leshan.core.peer.RpkIdentity;
import org.eclipse.leshan.core.peer.SocketIdentity;
import org.eclipse.leshan.core.peer.X509Identity;
import org.eclipse.leshan.core.util.Hex;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class LwM2mIdentitySerDes {

    protected static final String KEY_ADDRESS = "address";
    protected static final String KEY_PORT = "port";
    protected static final String KEY_ID = "pskid";
    protected static final String KEY_CN = "cn";
    protected static final String KEY_RPK = "rpk";
    // identity field key and types
    protected static final String KEY_LWM2MIDENTITY_TYPE = "type";
    protected static final String LWM2MIDENTITY_TYPE_UNSECURE = "unsecure";
    protected static final String LWM2MIDENTITY_TYPE_PSK = "psk";
    protected static final String LWM2MIDENTITY_TYPE_X509 = "cn";
    protected static final String LWM2MIDENTITY_TYPE_RPK = "rpk";
    protected static final String LWM2MIDENTITY_TYPE_RID = "rid";

    public JsonNode serialize(LwM2mIdentity identity) {
        ObjectNode o = JsonNodeFactory.instance.objectNode();

        if (identity.getClass() == SocketIdentity.class) {
            o.put(KEY_LWM2MIDENTITY_TYPE, LWM2MIDENTITY_TYPE_UNSECURE);
            o.put(KEY_ADDRESS, ((SocketIdentity) identity).getSocketAddress().getHostString());
            o.put(KEY_PORT, ((SocketIdentity) identity).getSocketAddress().getPort());
        } else if (identity.getClass() == PskIdentity.class) {
            o.put(KEY_LWM2MIDENTITY_TYPE, LWM2MIDENTITY_TYPE_PSK);
            o.put(KEY_ID, ((PskIdentity) identity).getPskIdentity());
        } else if (identity.getClass() == RpkIdentity.class) {
            o.put(KEY_LWM2MIDENTITY_TYPE, LWM2MIDENTITY_TYPE_RPK);
            PublicKey publicKey = ((RpkIdentity) identity).getPublicKey();
            o.put(KEY_RPK, Hex.encodeHexString(publicKey.getEncoded()));
        } else if (identity.getClass() == X509Identity.class) {
            o.put(KEY_LWM2MIDENTITY_TYPE, LWM2MIDENTITY_TYPE_X509);
            o.put(KEY_CN, ((X509Identity) identity).getX509CommonName());
        } else {
            throw new IllegalStateException(String.format("Can not serialize %s", identity.getClass().getSimpleName()));
        }
        return o;
    }

    public LwM2mIdentity deserialize(JsonNode jObj) {
        if ((jObj.get(KEY_LWM2MIDENTITY_TYPE).asText()).equals(LWM2MIDENTITY_TYPE_UNSECURE)) {
            JsonNode jaddress = jObj.get(KEY_ADDRESS);
            JsonNode jport = jObj.get(KEY_PORT);
            if (jaddress != null && jport != null) {
                return new SocketIdentity(new InetSocketAddress(jaddress.asText(), jport.asInt()));
            }
        }

        if ((jObj.get(KEY_LWM2MIDENTITY_TYPE).asText()).equals(LWM2MIDENTITY_TYPE_PSK)) {
            JsonNode jpsk = jObj.get(KEY_ID);
            if (jpsk != null) {
                return new PskIdentity(jpsk.asText());
            }
        }

        if ((jObj.get(KEY_LWM2MIDENTITY_TYPE).asText()).equals(LWM2MIDENTITY_TYPE_RPK)) {
            JsonNode jrpk = jObj.get(KEY_RPK);
            if (jrpk != null) {
                try {
                    byte[] rpk = Hex.decodeHex(jrpk.asText().toCharArray());
                    X509EncodedKeySpec spec = new X509EncodedKeySpec(rpk);
                    PublicKey publicKey = KeyFactory.getInstance("EC").generatePublic(spec);
                    return new RpkIdentity(publicKey);
                } catch (InvalidKeySpecException | NoSuchAlgorithmException e) {
                    throw new IllegalStateException("Invalid security info content", e);
                }
            }
        }

        if ((jObj.get(KEY_LWM2MIDENTITY_TYPE).asText()).equals(LWM2MIDENTITY_TYPE_X509)) {
            JsonNode jcn = jObj.get(KEY_CN);
            if (jcn != null) {
                return new X509Identity(jcn.asText());
            }
        }
        throw new IllegalStateException(String.format("Can not deserialize %s", jObj.toPrettyString()));

    }
}
