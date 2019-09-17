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
package org.eclipse.leshan.server.demo.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.eclipse.leshan.core.model.ObjectModel;
import org.eclipse.leshan.core.model.ResourceModel;
import org.eclipse.leshan.core.model.json.JsonSerDes;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;

public class ObjectModelSerDes extends JsonSerDes<ObjectModel> {

    ResourceModelSerDes resourceModelSerDes = new ResourceModelSerDes();

    @Override
    public JsonObject jSerialize(ObjectModel m) {
        final JsonObject o = Json.object();
        o.add("name", m.name);
        o.add("id", m.id);
        o.add("instancetype", m.multiple ? "multiple" : "single");
        o.add("mandatory", m.mandatory);
        if (!ObjectModel.DEFAULT_VERSION.equals(m.version))
            o.add("version", m.version);
        o.add("description", m.description);

        // sort resources value
        List<ResourceModel> resourceSpecs = new ArrayList<>(m.resources.values());
        Collections.sort(resourceSpecs, new Comparator<ResourceModel>() {
            @Override
            public int compare(ResourceModel r1, ResourceModel r2) {
                return r1.id - r2.id;
            }
        });

        JsonArray rs = new JsonArray();
        for (ResourceModel rm : resourceSpecs) {
            rs.add(resourceModelSerDes.jSerialize(rm));
        }
        o.add("resourcedefs", rs);

        return o;
    }

    @Override
    public ObjectModel deserialize(JsonObject o) {
        if (o == null)
            return null;

        if (!o.isObject())
            return null;

        int id = o.getInt("id", -1);
        if (id < 0)
            return null;

        String name = o.getString("name", null);
        String instancetype = o.getString("instancetype", null);
        boolean mandatory = o.getBoolean("mandatory", false);
        String description = o.getString("description", null);
        String version = o.getString("version", ObjectModel.DEFAULT_VERSION);
        List<ResourceModel> resourceSpecs = resourceModelSerDes.deserialize(o.get("resourcedefs").asArray());

        return new ObjectModel(id, name, description, version, "multiple".equals(instancetype), mandatory,
                resourceSpecs);
    }
}
