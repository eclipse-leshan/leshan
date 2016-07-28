/*******************************************************************************
 * Copyright (c) 2016 Sierra Wireless and others.
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
package org.eclipse.leshan.server.cluster.serialization;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.leshan.core.model.ResourceModel.Type;
import org.eclipse.leshan.core.node.LwM2mMultipleResource;
import org.eclipse.leshan.core.node.LwM2mNode;
import org.eclipse.leshan.core.node.LwM2mObject;
import org.eclipse.leshan.core.node.LwM2mObjectInstance;
import org.eclipse.leshan.core.node.LwM2mResource;
import org.eclipse.leshan.core.node.LwM2mSingleResource;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonObject.Member;
import com.eclipsesource.json.JsonValue;

/**
 * Functions for serialize and deserialize a LWM2M node in JSON.
 */
public class LwM2mNodeSerDes {
    public static JsonObject jSerialize(LwM2mNode n) {
        final JsonObject o = Json.object();
        o.add("id", n.getId());

        if (n instanceof LwM2mObject) {
            o.add("kind", "object");
            JsonObject instances = Json.object();
            for (LwM2mObjectInstance instance : ((LwM2mObject) n).getInstances().values()) {
                instances.add(String.valueOf(instance.getId()), jSerialize(instance));
            }
            o.add("instances", instances);
        } else if (n instanceof LwM2mObjectInstance) {
            o.add("kind", "instance");
            JsonObject resources = Json.object();
            for (LwM2mResource resource : ((LwM2mObjectInstance) n).getResources().values()) {
                resources.add(String.valueOf(resource.getId()), jSerialize(resource));
            }
            o.add("resources", resources);
        } else if (n instanceof LwM2mResource) {
            LwM2mResource r = (LwM2mResource) n;
            o.add("type", r.getType().toString());
            if (r.isMultiInstances()) {
                o.add("kind", "multipleResource");
                JsonObject values = Json.object();
                for (Entry<Integer, ?> value : r.getValues().entrySet()) {
                    values.add(value.getKey().toString(), ValueSerDes.jSerialize(value.getValue(), r.getType()));
                }
                o.add("values", values);
            } else {
                o.add("kind", "singleResource");
                o.add("value", ValueSerDes.jSerialize(r.getValue(), r.getType()));
            }
        }
        return o;
    }

    public static String sSerialize(LwM2mNode n) {
        return jSerialize(n).toString();
    }

    public static byte[] bSerialize(LwM2mNode n) {
        return jSerialize(n).toString().getBytes();
    }

    public static LwM2mNode deserialize(JsonObject o) {
        String kind = o.getString("kind", null);
        int id = o.getInt("id", LwM2mObjectInstance.UNDEFINED);

        switch (kind) {
        case "object": {
            Collection<LwM2mObjectInstance> instances = new ArrayList<>();
            JsonArray jInstances = (JsonArray) o.get("instances");
            for (JsonValue jInstance : jInstances) {
                LwM2mObjectInstance instance = (LwM2mObjectInstance) deserialize((JsonObject) jInstance);
                instances.add(instance);
            }
            return new LwM2mObject(id, instances);
        }
        case "instance": {
            Collection<LwM2mResource> resources = new ArrayList<>();
            JsonObject jResources = (JsonObject) o.get("resources");
            for (Member jResource : jResources) {
                LwM2mResource resource = (LwM2mResource) deserialize((JsonObject) jResource.getValue());
                resources.add(resource);
            }
            return new LwM2mObjectInstance(id, resources);
        }
        case "singleResource": {
            String jType = o.getString("type", null);
            if (jType == null)
                throw new IllegalStateException("Invalid LwM2mNode missing type attribute");
            Type type = Enum.valueOf(Type.class, jType);
            Object value = ValueSerDes.deserialize(o.get("value"), type);
            return LwM2mSingleResource.newResource(id, value, type);
        }
        case "multipleResource": {
            String jType = o.getString("type", null);
            if (jType == null)
                throw new IllegalStateException("Invalid LwM2mNode missing type attribute");
            Type type = Enum.valueOf(Type.class, jType);

            Map<Integer, Object> values = new HashMap<>();
            JsonObject jValues = (JsonObject) o.get("values");
            for (Member jValue : jValues) {
                Integer valueId = Integer.valueOf(jValue.getName());
                Object value = ValueSerDes.deserialize(jValue.getValue(), type);
                values.put(valueId, value);
            }

            return LwM2mMultipleResource.newResource(id, values, type);
        }
        default:
            throw new IllegalStateException("Invalid LwM2mNode missing kind attribute");
        }
    }
}
