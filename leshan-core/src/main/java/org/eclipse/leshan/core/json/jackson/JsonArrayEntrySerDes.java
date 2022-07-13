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

import java.math.BigDecimal;
import java.math.BigInteger;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import org.eclipse.leshan.core.json.JsonArrayEntry;
import org.eclipse.leshan.core.model.ResourceModel.Type;
import org.eclipse.leshan.core.util.datatype.ULong;
import org.eclipse.leshan.core.util.json.JacksonJsonSerDes;
import org.eclipse.leshan.core.util.json.JsonException;

public class JsonArrayEntrySerDes extends JacksonJsonSerDes<JsonArrayEntry> {

    @Override
    public JsonNode jSerialize(JsonArrayEntry jae) throws JsonException {
        ObjectNode o = JsonNodeFactory.instance.objectNode();
        if (jae.getName() != null)
            o.put("n", jae.getName());
        Type type = jae.getType();
        if (type != null) {
            switch (jae.getType()) {
            case FLOAT:
                Number value = jae.getFloatValue();
                // integer
                if (value instanceof Byte) {
                    o.put("v", value.byteValue());
                } else if (value instanceof Short) {
                    o.put("v", value.shortValue());
                } else if (value instanceof Integer) {
                    o.put("v", value.intValue());
                } else if (value instanceof Long) {
                    o.put("v", value.longValue());
                } else if (value instanceof BigInteger) {
                    o.put("v", (BigInteger) value);
                }
                // unsigned integer
                else if (value instanceof ULong) {
                    o.put("v", ((ULong) value).toBigInteger());
                }
                // floating-point
                else if (value instanceof Float) {
                    o.put("v", value.floatValue());
                } else if (value instanceof Double) {
                    o.put("v", value.doubleValue());
                } else if (value instanceof BigDecimal) {
                    o.put("v", (BigDecimal) value);
                }
                break;
            case BOOLEAN:
                o.put("bv", jae.getBooleanValue());
                break;
            case OBJLNK:
                o.put("ov", jae.getObjectLinkValue());
                break;
            case STRING:
                o.put("sv", jae.getStringValue());
                break;
            default:
                throw new JsonException("JsonArrayEntry MUST have a value : %s", jae);
            }
        }
        if (jae.getTime() != null)
            o.put("t", jae.getTime());
        return o;
    };

    @Override
    public JsonArrayEntry deserialize(JsonNode o) throws JsonException {
        if (o == null)
            return null;

        JsonArrayEntry jae = new JsonArrayEntry();
        JsonNode n = o.get("n");
        if (n != null && n.isTextual())
            jae.setName(n.asText());

        JsonNode t = o.get("t");
        if (t != null && t.isNumber())
            jae.setTime(t.asLong());

        JsonNode v = o.get("v");
        if (v != null && v.isNumber())
            jae.setFloatValue(v.numberValue());

        JsonNode bv = o.get("bv");
        if (bv != null && bv.isBoolean())
            jae.setBooleanValue(bv.asBoolean());

        JsonNode sv = o.get("sv");
        if (sv != null && sv.isTextual())
            jae.setStringValue(sv.asText());

        JsonNode ov = o.get("ov");
        if (ov != null && ov.isTextual())
            jae.setObjectLinkValue(ov.asText());

        if (jae.getType() == null) {
            throw new JsonException("Missing value(v,bv,ov,sv) field for entry %s", o.toString());
        }

        return jae;
    }
}
