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

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

import org.eclipse.leshan.core.model.ResourceModel;
import org.eclipse.leshan.core.model.ResourceModel.Type;
import org.eclipse.leshan.core.node.LwM2mMultipleResource;
import org.eclipse.leshan.core.node.LwM2mNode;
import org.eclipse.leshan.core.node.LwM2mObject;
import org.eclipse.leshan.core.node.LwM2mObjectInstance;
import org.eclipse.leshan.core.node.LwM2mResource;
import org.eclipse.leshan.core.node.LwM2mResourceInstance;
import org.eclipse.leshan.core.node.LwM2mSingleResource;
import org.eclipse.leshan.core.node.codec.CodecException;
import org.eclipse.leshan.core.util.Hex;

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

            String kind = null;
            if (object.has("kind")) {
                kind = object.get("kind").asText();
            }

            if ("obj".equals(kind) || object.has("instances")) {
                if (id == null) {
                    throw new JsonParseException(p, "Missing id");
                }

                JsonNode array = object.get("instances");
                LwM2mObjectInstance[] instances = new LwM2mObjectInstance[object.get("instances").size()];

                for (int i = 0; i < array.size(); i++) {
                    instances[i] = (LwM2mObjectInstance) om.treeToValue(array.get(i), LwM2mNode.class);
                }
                node = new LwM2mObject(id, instances);

            } else if ("instance".equals(kind) || object.has("resources")) {
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
            } else if ("multiResource".equals(kind) || object.has("values")) {
                if (id == null) {
                    throw new JsonParseException(p, "Missing id");
                }
                // multi-instances resource
                Map<Integer, Object> values = new HashMap<>();
                Type type = Type.valueOf(object.get("type").asText().toUpperCase());

                JsonNode valuesNode = object.get("values");
                if (!valuesNode.isObject()) {
                    throw new JsonParseException(p, "Values element is not an object");
                }

                for (Iterator<String> it = valuesNode.fieldNames(); it.hasNext();) {
                    String nodeName = it.next();
                    JsonNode nodeValue = valuesNode.get(nodeName);
                    values.put(Integer.valueOf(nodeName), deserializeValue(nodeValue, type));
                }
                node = LwM2mMultipleResource.newResource(id, values, type);
            } else if (object.has("value")) {
                if (id == null) {
                    throw new JsonParseException(p, "Missing id");
                }

                if ("resourceInstance".equals(kind)) {
                    // resource instance
                    JsonNode val = object.get("value");
                    Type type = Type.valueOf(object.get("type").asText().toUpperCase());
                    node = LwM2mResourceInstance.newInstance(id, deserializeValue(val, type), type);
                } else {
                    // single value resource
                    JsonNode val = object.get("value");
                    Type type = Type.valueOf(object.get("type").asText().toUpperCase());
                    node = LwM2mSingleResource.newResource(id, deserializeValue(val, type), type);
                }
            } else {
                throw new JsonParseException(p, "Invalid node element");
            }
        } else {
            throw new JsonParseException(p, "Invalid node element");
        }

        return node;
    }

    private Object deserializeValue(JsonNode val, ResourceModel.Type type) {
        switch (type) {
        case BOOLEAN:
            return val.asBoolean();
        case STRING:
            return val.asText();
        case INTEGER:
            return val.asLong();
        case FLOAT:
            return val.asDouble();
        case TIME:
            try {
                DatatypeFactory datatypeFactory = DatatypeFactory.newInstance();
                XMLGregorianCalendar cal = datatypeFactory.newXMLGregorianCalendar(val.asText());
                return cal.toGregorianCalendar().getTime();
            } catch (DatatypeConfigurationException | IllegalArgumentException e) {
                throw new CodecException("Unable to convert string (%s) to date", val.asText(), e);
            }
        case OPAQUE:
            return Hex.decodeHex((val.asText()).toCharArray());
        default:
            throw new UnsupportedOperationException(String.format("Type %s is not supported for now", type));
        }
    }
}
