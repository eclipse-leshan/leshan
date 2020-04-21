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
package org.eclipse.leshan.server.demo.model;

import org.eclipse.leshan.core.model.ResourceModel;
import org.eclipse.leshan.core.model.ResourceModel.Operations;
import org.eclipse.leshan.core.model.ResourceModel.Type;
import org.eclipse.leshan.core.util.json.JsonSerDes;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;

public class ResourceModelSerDes extends JsonSerDes<ResourceModel> {

    @Override
    public JsonObject jSerialize(ResourceModel m) {
        final JsonObject o = Json.object();
        o.add("id", m.id);
        o.add("name", m.name);
        o.add("operations", m.operations.toString());
        o.add("instancetype", m.multiple ? "multiple" : "single");
        o.add("mandatory", m.mandatory);
        o.add("type", m.type == null ? "none" : m.type.toString().toLowerCase());
        o.add("range", m.rangeEnumeration);
        o.add("units", m.units);
        o.add("description", m.description);
        return o;
    }

    @Override
    public ResourceModel deserialize(JsonObject o) {
        if (o == null)
            return null;

        if (!o.isObject())
            return null;

        int id = o.getInt("id", -1);
        if (id < 0)
            return null;

        String name = o.getString("name", null);
        Operations operations = Operations.valueOf(o.getString("operations", null));
        String instancetype = o.getString("instancetype", null);
        boolean mandatory = o.getBoolean("mandatory", false);
        Type type = Type.valueOf(o.getString("type", "").toUpperCase());
        String range = o.getString("range", null);
        String units = o.getString("units", null);
        String description = o.getString("description", null);

        return new ResourceModel(id, name, operations, "multiple".equals(instancetype), mandatory, type, range, units,
                description);
    }
}
