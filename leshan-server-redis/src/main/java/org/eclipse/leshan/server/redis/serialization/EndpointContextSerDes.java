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
import java.security.Principal;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.Map;

import javax.security.auth.x500.X500Principal;

import org.eclipse.californium.elements.AddressEndpointContext;
import org.eclipse.californium.elements.DtlsEndpointContext;
import org.eclipse.californium.elements.EndpointContext;
import org.eclipse.californium.elements.MapBasedEndpointContext;
import org.eclipse.californium.elements.MapBasedEndpointContext.Attributes;
import org.eclipse.californium.elements.UdpEndpointContext;
import org.eclipse.californium.elements.auth.PreSharedKeyIdentity;
import org.eclipse.californium.elements.auth.RawPublicKeyIdentity;
import org.eclipse.californium.elements.auth.X509CertPath;
import org.eclipse.californium.elements.util.Bytes;
import org.eclipse.californium.elements.util.StringUtil;
import org.eclipse.californium.scandium.dtls.ConnectionId;
import org.eclipse.californium.scandium.dtls.SessionId;
import org.eclipse.leshan.core.util.Hex;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonObject.Member;
import com.eclipsesource.json.JsonValue;

/**
 * Functions for serializing and deserializing a Californium {@link EndpointContext} in JSON.
 */
public class EndpointContextSerDes {

    private static final String KEY_ADDRESS = "address";
    private static final String KEY_PORT = "port";
    private static final String KEY_ID = "id";
    private static final String KEY_DN = "dn";
    private static final String KEY_RPK = "rpk";
    private static final String KEY_ATTRIBUTES = "attributes";

    public static JsonObject serialize(EndpointContext context) {
        JsonObject peer = Json.object();
        peer.set(KEY_ADDRESS, context.getPeerAddress().getHostString());
        peer.set(KEY_PORT, context.getPeerAddress().getPort());
        Principal principal = context.getPeerIdentity();
        if (principal != null) {
            if (principal instanceof PreSharedKeyIdentity) {
                peer.set(KEY_ID, ((PreSharedKeyIdentity) principal).getIdentity());
            } else if (principal instanceof RawPublicKeyIdentity) {
                PublicKey publicKey = ((RawPublicKeyIdentity) principal).getKey();
                peer.set(KEY_RPK, Hex.encodeHexString(publicKey.getEncoded()));
            } else if (principal instanceof X500Principal || principal instanceof X509CertPath) {
                peer.set(KEY_DN, principal.getName());
            }
        }
        /** copy the attributes **/
        Map<String, Object> attributes = context.entries();
        if (!attributes.isEmpty()) {
            JsonObject attContext = Json.object();
            for (String key : attributes.keySet()) {
                // write all values as string
                Object value = attributes.get(key);
                if (value instanceof Bytes) {
                    value = ((Bytes) value).getAsString();
                }
                attContext.set(key, value.toString());
            }
            peer.set(KEY_ATTRIBUTES, attContext);
        }
        return peer;
    }

    public static EndpointContext deserialize(JsonObject peer) {

        String address = peer.get(KEY_ADDRESS).asString();
        int port = peer.get(KEY_PORT).asInt();
        InetSocketAddress socketAddress = new InetSocketAddress(address, port);

        Principal principal = null;
        JsonValue value = peer.get(KEY_ID);
        if (value != null) {
            principal = new PreSharedKeyIdentity(value.asString());
        } else if ((value = peer.get(KEY_RPK)) != null) {
            try {
                byte[] rpk = Hex.decodeHex(value.asString().toCharArray());
                X509EncodedKeySpec spec = new X509EncodedKeySpec(rpk);
                PublicKey publicKey = KeyFactory.getInstance("EC").generatePublic(spec);
                principal = new RawPublicKeyIdentity(publicKey);
            } catch (InvalidKeySpecException | NoSuchAlgorithmException e) {
                throw new IllegalStateException("Invalid security info content", e);
            }
        } else if ((value = peer.get(KEY_DN)) != null) {
            principal = new X500Principal(value.asString());
        }

        EndpointContext endpointContext;
        value = peer.get(KEY_ATTRIBUTES);
        if (value == null) {
            endpointContext = new AddressEndpointContext(socketAddress, principal);
        } else {
            Attributes attributes = new Attributes();
            for (Member member : value.asObject()) {
                String name = member.getName();
                String attributeValue = member.getValue().asString();
                // convert the text values into typed values according their name
                if (name.equals(UdpEndpointContext.KEY_PLAIN)) {
                    attributes.add(name, attributeValue);
                } else if (name.equals(DtlsEndpointContext.KEY_SESSION_ID)) {
                    attributes.add(name, new SessionId(StringUtil.hex2ByteArray(attributeValue)));
                } else if (name.equals(DtlsEndpointContext.KEY_EPOCH)) {
                    attributes.add(name, Integer.parseInt(attributeValue));
                } else if (name.equals(DtlsEndpointContext.KEY_CIPHER)) {
                    attributes.add(name, attributeValue);
                } else if (name.equals(DtlsEndpointContext.KEY_HANDSHAKE_TIMESTAMP)) {
                    attributes.add(name, Long.parseLong(attributeValue));
                } else if (name.equals(DtlsEndpointContext.KEY_READ_CONNECTION_ID)) {
                    attributes.add(name, new ConnectionId(StringUtil.hex2ByteArray(attributeValue)));
                } else if (name.equals(DtlsEndpointContext.KEY_WRITE_CONNECTION_ID)) {
                    attributes.add(name, new ConnectionId(StringUtil.hex2ByteArray(attributeValue)));
                }
                attributes.add(name, attributeValue);
            }
            endpointContext = new MapBasedEndpointContext(socketAddress, principal, attributes);
        }
        return endpointContext;
    }

}
