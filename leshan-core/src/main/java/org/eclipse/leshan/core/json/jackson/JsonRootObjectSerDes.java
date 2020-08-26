/*******************************************************************************
 * Copyright (c) 2020 Sierra Wireless and others.
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
package org.eclipse.leshan.core.json.jackson;

import org.eclipse.leshan.core.json.JsonRootObject;
import org.eclipse.leshan.core.util.json.JacksonJsonSerDes;
import org.eclipse.leshan.core.util.json.JsonException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class JsonRootObjectSerDes extends JacksonJsonSerDes<JsonRootObject> {

    private JsonArrayEntrySerDes serDes = new JsonArrayEntrySerDes();

    @Override
    public JsonNode jSerialize(JsonRootObject jro) throws JsonException {
        ObjectNode o = JsonNodeFactory.instance.objectNode();

        if (jro.getBaseName() != null)
            o.put("bn", jro.getBaseName());

        ArrayNode ja = serDes.jSerialize(jro.getResourceList());
        if (ja != null)
            o.set("e", ja);

        if (jro.getBaseTime() != null)
            o.put("bt", jro.getBaseTime());

        return o;
    };

    @Override
    public JsonRootObject deserialize(JsonNode jsonNode) throws JsonException {
        if (jsonNode == null)
            return null;

        JsonRootObject jro = new JsonRootObject();

        JsonNode e = jsonNode.get("e");
        if (e != null && e.isArray())
            jro.setResourceList(serDes.deserialize(e.elements()));
        else
            throw new JsonException("'e' field is missing for %s", jsonNode.toString());

        JsonNode bn = jsonNode.get("bn");
        if (bn != null && bn.isTextual())
            jro.setBaseName(bn.asText());

        JsonNode bt = jsonNode.get("bt");
        if (bt != null && bt.isNumber())
            jro.setBaseTime(bt.asLong());

        return jro;
    }
}
