/*******************************************************************************
 * Copyright (c) 2017 Sierra Wireless and others.
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v1.0 which accompany this distribution.
 * 
 * The Eclipse Public License is available at
 *    http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 *    http://www.eclipse.org/org/documents/edl-v10.html.
 * 
 * Contributors:
 *     Sierra Wireless - initial API and implementation
 *******************************************************************************/
package org.eclipse.leshan.json;

import org.eclipse.leshan.core.model.json.JsonSerDes;

import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;

public class JsonRootObjectSerDes extends JsonSerDes<JsonRootObject> {

    private JsonArrayEntrySerDes serDes = new JsonArrayEntrySerDes();

    @Override
    public JsonObject jSerialize(JsonRootObject jro) {
        JsonObject o = new JsonObject();

        if (jro.getBaseName() != null)
            o.add("bn", jro.getBaseName());

        JsonArray ja = serDes.jSerialize(jro.getResourceList());
        if (ja != null)
            o.add("e", ja);

        if (jro.getBaseTime() != null)
            o.add("bt", jro.getBaseTime());

        return o;
    };

    @Override
    public JsonRootObject deserialize(JsonObject o) {
        if (o == null)
            return null;

        JsonRootObject jro = new JsonRootObject();

        JsonValue e = o.get("e");
        if (e != null)
            jro.setResourceList(serDes.deserialize(e.asArray()));
        else
            throw new LwM2mJsonException("'e' field is missing for %s", o.toString());

        JsonValue bn = o.get("bn");
        if (bn != null && bn.isString())
            jro.setBaseName(bn.asString());

        JsonValue bt = o.get("bt");
        if (bt != null && bt.isNumber())
            jro.setBaseTime(bt.asLong());

        return jro;
    }
}
