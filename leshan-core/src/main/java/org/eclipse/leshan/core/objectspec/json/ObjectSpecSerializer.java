/*******************************************************************************
 * Copyright (c) 2013-2015 Sierra Wireless and others.
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
package org.eclipse.leshan.core.objectspec.json;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.eclipse.leshan.core.objectspec.ObjectSpec;
import org.eclipse.leshan.core.objectspec.ResourceSpec;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

public class ObjectSpecSerializer implements JsonSerializer<ObjectSpec> {

    @Override
    public JsonElement serialize(ObjectSpec object, Type typeOfSrc, JsonSerializationContext context) {
        JsonObject element = new JsonObject();

        // sort resources value
        List<ResourceSpec> resourceSpecs = new ArrayList<ResourceSpec>(object.resources.values());
        Collections.sort(resourceSpecs, new Comparator<ResourceSpec>() {
            @Override
            public int compare(ResourceSpec r1, ResourceSpec r2) {
                return r1.id - r2.id;
            }
        });

        // serialize fields
        element.addProperty("name", object.name);
        element.addProperty("id", object.id);
        element.addProperty("instancetype", object.multiple ? "multiple" : "single");
        element.addProperty("mandatory", object.mandatory);
        element.addProperty("description", object.description);
        element.add("resourcedefs", context.serialize(resourceSpecs));

        return element;
    }

}
