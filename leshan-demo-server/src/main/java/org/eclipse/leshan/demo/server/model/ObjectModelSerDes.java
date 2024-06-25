/*******************************************************************************
 * Copyright (c) 2017 Sierra Wireless and others.
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
package org.eclipse.leshan.server.demo.model;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.leshan.core.model.ObjectModel;
import org.eclipse.leshan.core.model.ResourceModel;
import org.eclipse.leshan.core.util.json.JacksonJsonSerDes;
import org.eclipse.leshan.core.util.json.JsonException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class ObjectModelSerDes extends JacksonJsonSerDes<ObjectModel> {

    ResourceModelSerDes resourceModelSerDes = new ResourceModelSerDes();

    @Override
    public JsonNode jSerialize(ObjectModel m) {
        final ObjectNode o = JsonNodeFactory.instance.objectNode();
        o.put("id", m.id);
        o.put("name", m.name);
        o.put("instancetype", m.multiple ? "multiple" : "single");
        o.put("mandatory", m.mandatory);
        o.put("urn", m.urn);
        o.put("version", m.version);
        o.put("lwm2mversion", m.lwm2mVersion);
        o.put("description", m.description);
        o.put("description2", m.description2);

        List<ResourceModel> resourceSpecs = new ArrayList<>(m.resources.values());
        ArrayNode arrayNode = JsonNodeFactory.instance.arrayNode();
        for (ResourceModel rm : resourceSpecs) {
            arrayNode.add(resourceModelSerDes.jSerialize(rm));
        }
        o.set("resourcedefs", arrayNode);

        return o;
    }

    @Override
    public ObjectModel deserialize(JsonNode o) throws JsonException {
        if (o == null)
            return null;

        if (!o.isObject())
            return null;

        int id = o.get("id").asInt(-1);
        if (id < 0)
            return null;

        String name = o.get("name").asText();
        String instancetype = o.get("instancetype").asText();
        if (!instancetype.equals("multiple") && !instancetype.equals("single")) {
            throw new JsonException("Invalid value for 'instancetype' : must be multiple or single");
        }
        Boolean mandatory = null;
        if (o.get("mandatory") != null) {
            mandatory = o.asBoolean();
        }
        String urn = o.get("urn").asText();
        String version = o.get("version").asText();
        String lwm2mVersion = o.get("lwm2mversion").asText();
        String description = o.get("description").asText();
        String description2 = o.get("description2").asText();

        List<ResourceModel> resourceSpecs = resourceModelSerDes.deserialize(o.get("resourcedefs").iterator());

        return new ObjectModel(id, name, description, version, "multiple".equals(instancetype), mandatory,
                resourceSpecs, urn, lwm2mVersion, description2);
    }
}
