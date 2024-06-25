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

import org.eclipse.leshan.core.model.ResourceModel;
import org.eclipse.leshan.core.model.ResourceModel.Operations;
import org.eclipse.leshan.core.model.ResourceModel.Type;
import org.eclipse.leshan.core.util.json.JacksonJsonSerDes;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class ResourceModelSerDes extends JacksonJsonSerDes<ResourceModel> {

    @Override
    public JsonNode jSerialize(ResourceModel m) {
        final ObjectNode o = JsonNodeFactory.instance.objectNode();
        o.put("id", m.id);
        o.put("name", m.name);
        o.put("operations", m.operations.toString());
        o.put("instancetype", m.multiple ? "multiple" : "single");
        o.put("mandatory", m.mandatory);
        o.put("type", m.type == null ? "none" : m.type.toString().toLowerCase());
        o.put("range", m.rangeEnumeration);
        o.put("units", m.units);
        o.put("description", m.description);
        return o;
    }

    @Override
    public ResourceModel deserialize(JsonNode o) {
        if (o == null)
            return null;

        if (!o.isObject())
            return null;

        int id = o.get("id").asInt(-1);
        if (id < 0)
            return null;

        String name = o.get("name").asText();
        Operations operations = Operations.valueOf(o.get("operations").asText());
        String instancetype = o.get("instancetype").asText();
        boolean mandatory = o.get("mandatory").asBoolean(false);
        Type type = Type.valueOf(o.get("type").asText().toUpperCase());
        String range = o.get("range").asText();
        String units = o.get("units").asText();
        String description = o.get("description").asText();

        return new ResourceModel(id, name, operations, "multiple".equals(instancetype), mandatory, type, range, units,
                description);
    }
}
