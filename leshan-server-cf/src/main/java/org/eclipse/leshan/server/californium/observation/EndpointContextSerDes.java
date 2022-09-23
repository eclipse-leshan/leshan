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
package org.eclipse.leshan.server.californium.observation;

import java.net.InetSocketAddress;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.Principal;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.Iterator;
import java.util.Map;

import javax.security.auth.x500.X500Principal;

import org.eclipse.californium.elements.AddressEndpointContext;
import org.eclipse.californium.elements.Definition;
import org.eclipse.californium.elements.EndpointContext;
import org.eclipse.californium.elements.MapBasedEndpointContext;
import org.eclipse.californium.elements.MapBasedEndpointContext.Attributes;
import org.eclipse.californium.elements.auth.PreSharedKeyIdentity;
import org.eclipse.californium.elements.auth.RawPublicKeyIdentity;
import org.eclipse.californium.elements.auth.X509CertPath;
import org.eclipse.californium.elements.util.Bytes;
import org.eclipse.californium.elements.util.StringUtil;
import org.eclipse.leshan.core.util.Hex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Functions for serializing and deserializing a Californium {@link EndpointContext} in JSON.
 */
public class EndpointContextSerDes {

    private static final Logger LOG = LoggerFactory.getLogger(EndpointContextSerDes.class);

    private static final String KEY_ADDRESS = "address";
    private static final String KEY_PORT = "port";
    private static final String KEY_ID = "id";
    private static final String KEY_DN = "dn";
    private static final String KEY_RPK = "rpk";
    private static final String KEY_ATTRIBUTES = "attributes";

    public static ObjectNode serialize(EndpointContext context) {
        ObjectNode peer = JsonNodeFactory.instance.objectNode();
        addAddress(peer, context.getPeerAddress());
        Principal principal = context.getPeerIdentity();
        if (principal != null) {
            if (principal instanceof PreSharedKeyIdentity) {
                peer.put(KEY_ID, ((PreSharedKeyIdentity) principal).getIdentity());
            } else if (principal instanceof RawPublicKeyIdentity) {
                PublicKey publicKey = ((RawPublicKeyIdentity) principal).getKey();
                peer.put(KEY_RPK, Hex.encodeHexString(publicKey.getEncoded()));
            } else if (principal instanceof X500Principal || principal instanceof X509CertPath) {
                peer.put(KEY_DN, principal.getName());
            }
        }
        /** copy the attributes **/
        Map<Definition<?>, Object> attributes = context.entries();
        if (!attributes.isEmpty()) {
            ObjectNode attContext = JsonNodeFactory.instance.objectNode();
            for (Definition<?> key : attributes.keySet()) {
                // write all values as string
                Object value = attributes.get(key);
                if (value instanceof InetSocketAddress) {
                    ObjectNode address = JsonNodeFactory.instance.objectNode();
                    addAddress(address, (InetSocketAddress) value);
                    attContext.set(key.getKey(), address);
                } else {
                    if (value instanceof Bytes) {
                        value = ((Bytes) value).getAsString();
                    }
                    attContext.put(key.getKey(), value.toString());
                }
            }
            peer.set(KEY_ATTRIBUTES, attContext);
        }
        return peer;
    }

    @SuppressWarnings("unchecked")
    public static EndpointContext deserialize(JsonNode peer) {

        final InetSocketAddress socketAddress = getAddress(peer);

        Principal principal = null;
        JsonNode value = peer.get(KEY_ID);
        if (value != null) {
            principal = new PreSharedKeyIdentity(value.asText());
        } else if ((value = peer.get(KEY_RPK)) != null) {
            try {
                byte[] rpk = Hex.decodeHex(value.asText().toCharArray());
                X509EncodedKeySpec spec = new X509EncodedKeySpec(rpk);
                PublicKey publicKey = KeyFactory.getInstance("EC").generatePublic(spec);
                principal = new RawPublicKeyIdentity(publicKey);
            } catch (InvalidKeySpecException | NoSuchAlgorithmException e) {
                throw new IllegalStateException("Invalid security info content", e);
            }
        } else if ((value = peer.get(KEY_DN)) != null) {
            principal = new X500Principal(value.asText());
        }

        EndpointContext endpointContext;
        value = peer.get(KEY_ATTRIBUTES);
        if (value == null) {
            endpointContext = new AddressEndpointContext(socketAddress, principal);
        } else {
            Attributes attributes = new Attributes();
            for (Iterator<String> it = value.fieldNames(); it.hasNext();) {
                String name = it.next();
                Definition<?> key = MapBasedEndpointContext.ATTRIBUTE_DEFINITIONS.get(name);
                if (key != null) {
                    if (key.getValueType().equals(InetSocketAddress.class)) {
                        InetSocketAddress address = getAddress(value.get(name));
                        attributes.add((Definition<InetSocketAddress>) key, address);
                    } else {
                        String attributeValue = value.get(name).asText();
                        // convert the text values into typed values according their type
                        if (key.getValueType().equals(String.class)) {
                            attributes.add((Definition<String>) key, attributeValue);
                        } else if (key.getValueType().equals(Bytes.class)) {
                            attributes.add((Definition<Bytes>) key,
                                    new Bytes(StringUtil.hex2ByteArray(attributeValue)));
                        } else if (key.getValueType().equals(Integer.class)) {
                            attributes.add((Definition<Integer>) key, Integer.parseInt(attributeValue));
                        } else if (key.getValueType().equals(Long.class)) {
                            attributes.add((Definition<Long>) key, Long.parseLong(attributeValue));
                        } else if (key.getValueType().equals(Boolean.class)) {
                            attributes.add((Definition<Boolean>) key, Boolean.parseBoolean(attributeValue));
                        } else {
                            LOG.warn("Unsupported type" + key.getValueType() + " for endpoint-context-attribute '{}'.",
                                    name);
                        }
                    }
                } else {
                    LOG.warn("missing definition for endpoint-context-attribute '{}'.", name);
                }
            }
            endpointContext = new MapBasedEndpointContext(socketAddress, principal, attributes);
        }
        return endpointContext;
    }

    private static void addAddress(ObjectNode object, InetSocketAddress address) {
        object.put(KEY_ADDRESS, address.getHostString());
        object.put(KEY_PORT, address.getPort());
    }

    private static InetSocketAddress getAddress(JsonNode object) {
        String address = object.get(KEY_ADDRESS).asText();
        int port = object.get(KEY_PORT).asInt();
        return new InetSocketAddress(address, port);
    }
}
