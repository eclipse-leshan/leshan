/*******************************************************************************
 * Copyright (c) 2016 Sierra Wireless and others.
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v1.0 which accompany this distribution.
 * 
 * The Eclipse Public License is available at
 *    http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 *    http://www.eclipse.org/org/documents/edl-v10.html.
 * 
 * Contributors:
 *     Sierra Wireless - initial API and implementation
 *******************************************************************************/
package org.eclipse.leshan.server.cluster.serialization;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.californium.core.coap.Request;
import org.eclipse.californium.core.network.serialization.DataParser;
import org.eclipse.californium.core.network.serialization.DataSerializer;
import org.eclipse.californium.core.network.serialization.UdpDataParser;
import org.eclipse.californium.core.network.serialization.UdpDataSerializer;
import org.eclipse.californium.core.observe.Observation;
import org.eclipse.californium.elements.RawData;
import org.eclipse.leshan.util.Hex;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;

/**
 * Functions for serializing and deserializing a Californium {@link Observation} in JSON.
 * 
 * The embedded CoAP request is serialized using the Californium network serialization (see {@link UdpDataParser} and
 * {@link UdpDataSerializer}).
 */
public class ObservationSerDes {

    private static final DataSerializer serializer = new UdpDataSerializer();
    private static final DataParser parser = new UdpDataParser();

    public static byte[] serialize(Observation obs) {
        JsonObject o = Json.object();

        o.set("request", Hex.encodeHexString(serializer.serializeRequest(obs.getRequest()).bytes));

        if (obs.getRequest().getUserContext() != null) {
            JsonObject ctxObject = Json.object();
            for (Entry<String, String> e : obs.getRequest().getUserContext().entrySet()) {
                ctxObject.set(e.getKey(), e.getValue());
            }
            o.set("context", ctxObject);
        }
        return o.toString().getBytes();
    }

    public static Observation deserialize(byte[] data) {
        JsonObject v = (JsonObject) Json.parse(new String(data));

        byte[] req = Hex.decodeHex(v.getString("request", null).toCharArray());
        Request request = (Request) parser.parseMessage(new RawData(req, null, 0));

        JsonValue ctxValue = v.get("context");
        if (ctxValue != null) {
            Map<String, String> context = new HashMap<>();
            JsonObject ctxObject = (JsonObject) ctxValue;
            for (String name : ctxObject.names()) {
                context.put(name, ctxObject.getString(name, null));
            }
            request.setUserContext(context);
        }

        // TODO handle security context
        return new Observation(request, null);
    }

}
