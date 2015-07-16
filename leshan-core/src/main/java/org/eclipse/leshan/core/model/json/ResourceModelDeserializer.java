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
package org.eclipse.leshan.core.model.json;

import org.eclipse.leshan.core.model.ResourceModel;
import org.eclipse.leshan.core.model.ResourceModel.Operations;
import org.eclipse.leshan.core.model.ResourceModel.Type;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

public class ResourceModelDeserializer implements JsonDeserializer<ResourceModel> {

    @Override
    public ResourceModel deserialize(JsonElement json, java.lang.reflect.Type typeOfT,
            JsonDeserializationContext context) throws JsonParseException {
        if (json == null)
            return null;

        if (!json.isJsonObject())
            return null;

        JsonObject jsonObject = json.getAsJsonObject();
        if (!jsonObject.has("id"))
            return null;

        int id = jsonObject.get("id").getAsInt();
        String name = jsonObject.get("name").getAsString();
        Operations operations = Operations.valueOf(jsonObject.get("operations").getAsString());
        String instancetype = jsonObject.get("instancetype").getAsString();
        boolean mandatory = jsonObject.get("mandatory").getAsBoolean();
        Type type = Type.valueOf(jsonObject.get("type").getAsString().toUpperCase());
        String range = jsonObject.get("range").getAsString();
        String units = jsonObject.get("units").getAsString();
        String description = jsonObject.get("description").getAsString();

        return new ResourceModel(id, name, operations, "multiple".equals(instancetype), mandatory, type, range, units,
                description);
    }
}
