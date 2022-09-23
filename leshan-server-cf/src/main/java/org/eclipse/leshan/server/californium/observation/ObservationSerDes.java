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
 *     Orange - keep one JSON dependency
 *******************************************************************************/
package org.eclipse.leshan.server.californium.observation;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.californium.core.coap.Request;
import org.eclipse.californium.core.network.serialization.DataParser;
import org.eclipse.californium.core.network.serialization.DataSerializer;
import org.eclipse.californium.core.network.serialization.UdpDataParser;
import org.eclipse.californium.core.network.serialization.UdpDataSerializer;
import org.eclipse.californium.core.observe.Observation;
import org.eclipse.californium.elements.EndpointContext;
import org.eclipse.leshan.core.util.Hex;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Functions for serializing and deserializing a Californium {@link Observation} in JSON.
 *
 * The embedded CoAP request is serialized using the Californium network serialization (see {@link UdpDataParser} and
 * {@link UdpDataSerializer}).
 */
public class ObservationSerDes {

    private final DataSerializer serializer;
    private final DataParser parser;

    public ObservationSerDes(DataParser parser, DataSerializer serializer) {
        this.parser = parser;
        this.serializer = serializer;
    }

    public String serialize(Observation obs) {
        ObjectNode o = JsonNodeFactory.instance.objectNode();

        o.put("request", Hex.encodeHexString(serializer.serializeRequest(obs.getRequest()).bytes));
        if (obs.getContext() != null)
            o.set("peer", EndpointContextSerDes.serialize(obs.getContext()));
        else
            o.set("peer", EndpointContextSerDes.serialize(obs.getRequest().getDestinationContext()));

        if (obs.getRequest().getUserContext() != null) {
            ObjectNode ctxObject = JsonNodeFactory.instance.objectNode();
            for (Entry<String, String> e : obs.getRequest().getUserContext().entrySet()) {
                ctxObject.put(e.getKey(), e.getValue());
            }
            o.set("context", ctxObject);
        }
        return o.toString();
    }

    public Observation deserialize(String data) {
        try {
            JsonNode v = new ObjectMapper().readTree(data);

            EndpointContext endpointContext = EndpointContextSerDes.deserialize(v.get("peer"));
            byte[] req = Hex.decodeHex(v.get("request").asText().toCharArray());

            Request request = (Request) parser.parseMessage(req);
            request.setDestinationContext(endpointContext);

            JsonNode ctxValue = v.get("context");
            if (ctxValue != null) {
                Map<String, String> context = new HashMap<>();
                for (Iterator<String> it = ctxValue.fieldNames(); it.hasNext();) {
                    String name = it.next();
                    context.put(name, ctxValue.get(name).asText());
                }
                request.setUserContext(context);
            }

            return new Observation(request, endpointContext);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException(String.format("Unable to deserialize Observation %s", data), e);
        }
    }

}
