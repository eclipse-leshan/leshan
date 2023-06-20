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

import org.eclipse.leshan.core.peer.IpPeer;
import org.eclipse.leshan.core.peer.LwM2mPeer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class LwM2mPeerSerDes {

    // LwM2mPeer keys
    protected static final String KEY_IDENTITY = "identity";
    protected static final String KEY_LWM2MPEERTYPE = "type";
    protected static final String DEFAULT_LWM2MPEERTYPE = "ippeer";
    // IpPeer keys
    protected static final String KEY_ADDRESS = "address";
    protected static final String KEY_PORT = "port";

    private final LwM2mIdentitySerDes identitySerDes = new LwM2mIdentitySerDes();

    public JsonNode serialize(LwM2mPeer peer) {
        ObjectNode o = JsonNodeFactory.instance.objectNode();
        if (peer.getClass() == IpPeer.class) {
            IpPeer ipPeer = (IpPeer) peer;
            o.put(KEY_ADDRESS, ipPeer.getSocketAddress().getHostString());
            o.put(KEY_PORT, ipPeer.getSocketAddress().getPort());
            o.put(KEY_LWM2MPEERTYPE, DEFAULT_LWM2MPEERTYPE);
        } else {
            throw new IllegalStateException(String.format("Can not serialize %s", peer.getClass().getSimpleName()));
        }
        o.set(KEY_IDENTITY, identitySerDes.serialize(peer.getIdentity()));
        return o;
    }

    public LwM2mPeer deserialize(JsonNode jObj) throws IllegalStateException {
        if ((jObj.get(KEY_LWM2MPEERTYPE).asText()).equals(DEFAULT_LWM2MPEERTYPE)) {
            String address = jObj.get(KEY_ADDRESS).asText();
            int port = jObj.get(KEY_PORT).asInt();

            return new IpPeer(new InetSocketAddress(address, port), identitySerDes.deserialize(jObj.get("identity")));
        } else {
            throw new IllegalStateException(String.format("Invalid object type, expected type for this object is: %s",
                    jObj.get(KEY_LWM2MPEERTYPE)));
        }
    }

}
