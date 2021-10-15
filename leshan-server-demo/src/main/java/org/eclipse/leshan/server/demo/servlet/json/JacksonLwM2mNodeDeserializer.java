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
package org.eclipse.leshan.server.demo.servlet.json;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.eclipse.leshan.core.model.ResourceModel;
import org.eclipse.leshan.core.node.LwM2mMultipleResource;
import org.eclipse.leshan.core.node.LwM2mNode;
import org.eclipse.leshan.core.node.LwM2mObject;
import org.eclipse.leshan.core.node.LwM2mObjectInstance;
import org.eclipse.leshan.core.node.LwM2mResource;
import org.eclipse.leshan.core.node.LwM2mSingleResource;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class JacksonLwM2mNodeDeserializer extends JsonDeserializer<LwM2mNode> {

    @Override
    public LwM2mNode deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {

        ObjectMapper om = (ObjectMapper) p.getCodec();
        om.setConfig(ctxt.getConfig());

        JsonNode object = p.getCodec().readTree(p);

        LwM2mNode node = null;

        if (object.isObject()) {

            Integer id = null;
            if (object.has("id")) {
                id = object.get("id").asInt();
            }

            if (object.has("instances")) {
                if (id == null) {
                    throw new JsonParseException(p, "Missing id");
                }

                JsonNode array = object.get("instances");
                LwM2mObjectInstance[] instances = new LwM2mObjectInstance[object.get("instances").size()];

                for (int i = 0; i < array.size(); i++) {
                    instances[i] = (LwM2mObjectInstance) om.treeToValue(array.get(i), LwM2mNode.class);
                }
                node = new LwM2mObject(id, instances);

            } else if (object.has("resources")) {
                JsonNode array = object.get("resources");
                LwM2mResource[] resources = new LwM2mResource[array.size()];

                for (int i = 0; i < array.size(); i++) {
                    resources[i] = (LwM2mResource) om.treeToValue(array.get(i), LwM2mNode.class);
                }
                if (id == null) {
                    node = new LwM2mObjectInstance(Arrays.asList(resources));
                } else {
                    node = new LwM2mObjectInstance(id, resources);
                }
            } else if (object.has("value")) {
                if (id == null) {
                    throw new JsonParseException(p, "Missing id");
                }
                // single value resource
                JsonNode val = object.get("value");
                org.eclipse.leshan.core.model.ResourceModel.Type expectedType = getTypeFor(val);
                node = LwM2mSingleResource.newResource(id, deserializeValue(val, expectedType), expectedType);
            } else if (object.has("values")) {
                if (id == null) {
                    throw new JsonParseException(p, "Missing id");
                }
                // multi-instances resource
                Map<Integer, Object> values = new HashMap<>();
                org.eclipse.leshan.core.model.ResourceModel.Type expectedType = null;

                JsonNode valuesNode = object.get("values");
                if (!valuesNode.isObject()) {
                    throw new JsonParseException(p, "Values element is not an object");
                }

                for (Iterator<String> it = valuesNode.fieldNames(); it.hasNext();) {
                    String nodeName = it.next();
                    JsonNode nodeValue = valuesNode.get(nodeName);

                    expectedType = getTypeFor(nodeValue);
                    values.put(Integer.valueOf(nodeName), deserializeValue(nodeValue, expectedType));
                }

                // use string by default;
                if (expectedType == null)
                    expectedType = org.eclipse.leshan.core.model.ResourceModel.Type.STRING;
                node = LwM2mMultipleResource.newResource(id, values, expectedType);
            } else {
                throw new JsonParseException(p, "Invalid node element");
            }
        } else {
            throw new JsonParseException(p, "Invalid node element");
        }

        return node;
    }

    private org.eclipse.leshan.core.model.ResourceModel.Type getTypeFor(JsonNode val) {
        if (val.isBoolean())
            return org.eclipse.leshan.core.model.ResourceModel.Type.BOOLEAN;
        if (val.isTextual())
            return org.eclipse.leshan.core.model.ResourceModel.Type.STRING;
        if (val.isNumber()) {
            if (val.isDouble()) {
                return org.eclipse.leshan.core.model.ResourceModel.Type.FLOAT;
            } else {
                return org.eclipse.leshan.core.model.ResourceModel.Type.INTEGER;
            }
        }
        // use string as default value
        return org.eclipse.leshan.core.model.ResourceModel.Type.STRING;
    }

    private Object deserializeValue(JsonNode val, ResourceModel.Type expectedType) {
        switch (expectedType) {
        case BOOLEAN:
            return val.asBoolean();
        case STRING:
            return val.asText();
        case INTEGER:
            return val.asLong();
        case FLOAT:
            return val.asDouble();
        case TIME:
        case OPAQUE:
        default:
            // TODO we need to better handle this.
            return val.asText();
        }
    }
}
