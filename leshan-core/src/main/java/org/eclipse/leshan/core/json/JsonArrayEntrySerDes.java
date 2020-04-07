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
 *******************************************************************************/
package org.eclipse.leshan.core.json;

import org.eclipse.leshan.core.model.ResourceModel.Type;
import org.eclipse.leshan.core.util.json.JsonException;
import org.eclipse.leshan.core.util.json.JsonSerDes;

import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;

public class JsonArrayEntrySerDes extends JsonSerDes<JsonArrayEntry> {

    @Override
    public JsonObject jSerialize(JsonArrayEntry jae) throws JsonException {
        JsonObject o = new JsonObject();
        if (jae.getName() != null)
            o.add("n", jae.getName());
        Type type = jae.getType();
        if (type != null) {
            switch (jae.getType()) {
            case FLOAT:
                o.add("v", jae.getFloatValue().doubleValue());
                break;
            case BOOLEAN:
                o.add("bv", jae.getBooleanValue());
                break;
            case OBJLNK:
                o.add("ov", jae.getObjectLinkValue());
                break;
            case STRING:
                o.add("sv", jae.getStringValue());
                break;
            default:
                throw new JsonException("JsonArrayEntry MUST have a value : %s", jae);
            }
        }
        if (jae.getTime() != null)
            o.add("t", jae.getTime());
        return o;
    };

    @Override
    public JsonArrayEntry deserialize(JsonObject o) throws JsonException {
        if (o == null)
            return null;

        JsonArrayEntry jae = new JsonArrayEntry();
        jae.setName(o.getString("n", null));

        JsonValue t = o.get("t");
        if (t != null && t.isNumber())
            jae.setTime(t.asLong());

        JsonValue v = o.get("v");
        if (v != null && v.isNumber())
            jae.setFloatValue(v.asDouble());

        JsonValue bv = o.get("bv");
        if (bv != null && bv.isBoolean())
            jae.setBooleanValue(bv.asBoolean());

        JsonValue sv = o.get("sv");
        if (sv != null && sv.isString())
            jae.setStringValue(sv.asString());

        JsonValue ov = o.get("ov");
        if (ov != null && ov.isString())
            jae.setObjectLinkValue(ov.asString());

        if (jae.getType() == null) {
            throw new JsonException("Missing value(v,bv,ov,sv) field for entry %s", o.toString());
        }

        return jae;
    }
}
