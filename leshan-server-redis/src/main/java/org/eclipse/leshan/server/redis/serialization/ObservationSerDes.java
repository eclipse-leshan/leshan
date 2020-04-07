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
 *     Achim Kraus (Bosch Software Innovations GmbH) - add support for californium
 *                                                     endpoint context
 *******************************************************************************/
package org.eclipse.leshan.server.redis.serialization;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.californium.core.coap.Request;
import org.eclipse.californium.core.network.serialization.DataParser;
import org.eclipse.californium.core.network.serialization.DataSerializer;
import org.eclipse.californium.core.network.serialization.UdpDataParser;
import org.eclipse.californium.core.network.serialization.UdpDataSerializer;
import org.eclipse.californium.core.observe.Observation;
import org.eclipse.californium.elements.EndpointContext;
import org.eclipse.californium.elements.RawData;
import org.eclipse.leshan.core.util.Hex;

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
        if (obs.getContext() != null)
            o.set("peer", EndpointContextSerDes.serialize(obs.getContext()));
        else
            o.set("peer", EndpointContextSerDes.serialize(obs.getRequest().getDestinationContext()));

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

        EndpointContext endpointContext = EndpointContextSerDes.deserialize(v.get("peer").asObject());
        byte[] req = Hex.decodeHex(v.getString("request", null).toCharArray());

        RawData rawData = RawData.outbound(req, endpointContext, null, false);
        Request request = (Request) parser.parseMessage(rawData);
        request.setDestinationContext(endpointContext);

        JsonValue ctxValue = v.get("context");
        if (ctxValue != null) {
            Map<String, String> context = new HashMap<>();
            JsonObject ctxObject = (JsonObject) ctxValue;
            for (String name : ctxObject.names()) {
                context.put(name, ctxObject.getString(name, null));
            }
            request.setUserContext(context);
        }

        return new Observation(request, endpointContext);
    }

}
