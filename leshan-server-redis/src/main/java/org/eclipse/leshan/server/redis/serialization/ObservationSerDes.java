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
package org.eclipse.leshan.server.redis.serialization;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.leshan.core.node.LwM2mPath;
import org.eclipse.leshan.core.observation.CompositeObservation;
import org.eclipse.leshan.core.observation.Observation;
import org.eclipse.leshan.core.observation.ObservationIdentifier;
import org.eclipse.leshan.core.observation.SingleObservation;
import org.eclipse.leshan.core.request.ContentFormat;
import org.eclipse.leshan.core.util.Hex;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Functions for serializing and deserializing a {@link Observation} in JSON.
 */
public class ObservationSerDes {

    private static final String OBS_ID = "id";
    private static final String OBS_REGID = "regid";
    private static final String OBS_USER_CONTEXT = "userContext";
    private static final String OBS_PROTOCOL_DATA = "protocolData";
    private static final String OBS_KIND = "kind";

    private static final String SOBS_CONTENT_FORMAT = "ct";
    private static final String SOBS_PATH = "path";

    private static final String COBS_RESP_CONTENT_FORMAT = "resCt";
    private static final String COBS_REQ_CONTENT_FORMAT = "reqCt";
    private static final String COBS_PATHS = "paths";

    private static final String KIND_SINGLE = "single";
    private static final String KIND_COMPOSITE = "composite";

    public static byte[] serialize(Observation obs) {
        ObjectNode n = JsonNodeFactory.instance.objectNode();
        n.put(OBS_ID, obs.getId().getAsHexString());
        n.put(OBS_REGID, obs.getRegistrationId());

        ObjectNode userContext = JsonNodeFactory.instance.objectNode();
        for (Map.Entry<String, String> e : obs.getContext().entrySet()) {
            userContext.put(e.getKey(), e.getValue());
        }
        n.set(OBS_USER_CONTEXT, userContext);

        ObjectNode protocolData = JsonNodeFactory.instance.objectNode();
        for (Map.Entry<String, String> e : obs.getProtocolData().entrySet()) {
            protocolData.put(e.getKey(), e.getValue());
        }
        n.set(OBS_PROTOCOL_DATA, protocolData);

        if (obs instanceof SingleObservation) {
            SingleObservation sobs = (SingleObservation) obs;
            n.put(OBS_KIND, KIND_SINGLE);
            if (sobs.getContentFormat() != null) {
                n.put(SOBS_CONTENT_FORMAT, sobs.getContentFormat().getCode());
            }
            n.put(SOBS_PATH, sobs.getPath().toString());
        } else if (obs instanceof CompositeObservation) {
            CompositeObservation cobs = (CompositeObservation) obs;
            n.put(OBS_KIND, KIND_COMPOSITE);
            if (cobs.getRequestContentFormat() != null) {
                n.put(COBS_REQ_CONTENT_FORMAT, cobs.getRequestContentFormat().getCode());
            }
            if (cobs.getResponseContentFormat() != null) {
                n.put(COBS_RESP_CONTENT_FORMAT, cobs.getResponseContentFormat().getCode());
            }

            ArrayNode paths = JsonNodeFactory.instance.arrayNode();
            for (LwM2mPath path : cobs.getPaths()) {
                paths.add(path.toString());
            }
            n.set(COBS_PATHS, paths);
        } else {
            throw new IllegalArgumentException(String.format("Unsupported kind of Observation : %s", obs));
        }

        return n.toString().getBytes();
    }

    public static Observation deserialize(byte[] data) {
        String json = new String(data);
        try {
            JsonNode n = new ObjectMapper().readTree(json);
            String id = n.get(OBS_ID).asText();
            ObservationIdentifier obsId = new ObservationIdentifier(Hex.decodeHex(id.toCharArray()));
            String regid = n.get(OBS_REGID).asText();
            String kind = n.get(OBS_KIND).asText();

            Map<String, String> context = null;
            ObjectNode jUserContext = (ObjectNode) n.get(OBS_USER_CONTEXT);
            if (jUserContext != null) {
                context = new HashMap<>();
                for (Iterator<String> it = jUserContext.fieldNames(); it.hasNext();) {
                    String k = it.next();
                    context.put(k, jUserContext.get(k).asText());
                }
            }

            Map<String, String> protocolData = null;
            ObjectNode jProtocolData = (ObjectNode) n.get(OBS_PROTOCOL_DATA);
            if (jProtocolData != null) {
                protocolData = new HashMap<>();
                for (Iterator<String> it = jProtocolData.fieldNames(); it.hasNext();) {
                    String k = it.next();
                    protocolData.put(k, jProtocolData.get(k).asText());
                }
            }

            if (KIND_SINGLE.equals(kind)) {
                ContentFormat contentFormat = null;
                if (n.has(SOBS_CONTENT_FORMAT)) {
                    contentFormat = ContentFormat.fromCode(n.get(SOBS_CONTENT_FORMAT).asInt());
                }
                LwM2mPath path = new LwM2mPath(n.get(SOBS_PATH).asText());

                return new SingleObservation(obsId, regid, path, contentFormat, context, protocolData);
            } else if (KIND_COMPOSITE.equals(kind)) {
                ContentFormat reqContentFormat = null;
                if (n.has(COBS_REQ_CONTENT_FORMAT)) {
                    reqContentFormat = ContentFormat.fromCode(n.get(COBS_REQ_CONTENT_FORMAT).asInt());
                }
                ContentFormat respcontentFormat = null;
                if (n.has(COBS_RESP_CONTENT_FORMAT)) {
                    respcontentFormat = ContentFormat.fromCode(n.get(COBS_RESP_CONTENT_FORMAT).asInt());
                }

                List<LwM2mPath> paths = null;
                ArrayNode jPaths = (ArrayNode) n.get(COBS_PATHS);
                if (jPaths != null) {
                    paths = new ArrayList<>();
                    for (JsonNode jPath : jPaths) {
                        paths.add(new LwM2mPath(jPath.asText()));
                    }
                }
                return new CompositeObservation(obsId, regid, paths, reqContentFormat, respcontentFormat, context,
                        protocolData);

            } else {
                throw new IllegalArgumentException(
                        String.format("Unsupported kind of Observation : %s in %s", kind, json));
            }
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException(String.format("Unable to deserialize Observation %s", json), e);
        }
    }
}
