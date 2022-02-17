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
package org.eclipse.leshan.server.core.demo.json;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.PublicKey;

import org.eclipse.leshan.core.util.Hex;
import org.eclipse.leshan.core.util.SecurityUtil;
import org.eclipse.leshan.server.security.SecurityInfo;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeType;

public class JacksonSecurityDeserializer extends JsonDeserializer<SecurityInfo> {

    @Override
    public SecurityInfo deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        JsonNode node = p.getCodec().readTree(p);
        if (!node.isObject()) {
            throw new JsonParseException(p, "Security info should be a json object");
        }

        // Get endpoint
        String endpoint;
        if (node.has("endpoint")) {
            endpoint = node.get("endpoint").asText();
        } else {
            throw new JsonParseException(p, "Missing endpoint");
        }

        // handle dtls
        if (node.has("tls")) {
            JsonNode oTls = node.get("tls");
            if (oTls.getNodeType() != JsonNodeType.OBJECT) {
                throw new JsonParseException(p, "tls field should be a json object");
            }
            String mode = oTls.get("mode").asText();
            if (mode.equals("psk")) {
                // handle PSK
                JsonNode oPsk = oTls.get("details");
                if (oPsk.getNodeType() != JsonNodeType.OBJECT) {
                    throw new JsonParseException(p, "details field should be a json object");
                }

                // get identity
                String identity;
                if (oPsk.has("identity")) {
                    identity = oPsk.get("identity").asText();
                } else {
                    throw new JsonParseException(p, "Missing PSK identity");
                }

                // get key
                byte[] key;
                try {
                    key = Hex.decodeHex(oPsk.get("key").asText().toCharArray());
                } catch (IllegalArgumentException e) {
                    throw new JsonParseException(p, "key parameter must be a valid hex string", e);
                }
                return SecurityInfo.newPreSharedKeyInfo(endpoint, identity, key);
            } else if (mode.equals("rpk")) {
                // handle RPK
                JsonNode oRpk = oTls.get("details");
                if (oRpk.getNodeType() != JsonNodeType.OBJECT) {
                    throw new JsonParseException(p, "details field should be a json object");
                }

                // get public key
                PublicKey key;
                try {
                    if (oRpk.has("key")) {
                        byte[] bytekey = Hex.decodeHex(oRpk.get("key").asText().toCharArray());
                        key = SecurityUtil.publicKey.decode(bytekey);
                        return SecurityInfo.newRawPublicKeyInfo(endpoint, key);
                    }
                } catch (IllegalArgumentException | GeneralSecurityException e) {
                    throw new JsonParseException(p, "Invalid security info content", e);
                }
            } else if (mode.equals("x509")) {
                // handle x509
                return SecurityInfo.newX509CertInfo(endpoint);
            } else {
                throw new JsonParseException(p, "Invalid security info content");
            }
        }
        return null;
    }
}
