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
package org.eclipse.leshan.standalone.servlet.json;

import java.lang.reflect.Type;

import org.eclipse.leshan.core.node.LwM2mNode;
import org.eclipse.leshan.core.node.LwM2mObject;
import org.eclipse.leshan.core.node.LwM2mObjectInstance;
import org.eclipse.leshan.core.node.LwM2mResource;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

public class LwM2mNodeSerializer implements JsonSerializer<LwM2mNode> {

    @Override
    public JsonElement serialize(LwM2mNode src, Type typeOfSrc, JsonSerializationContext context) {
        JsonObject element = new JsonObject();

        element.addProperty("id", src.getId());

        if (typeOfSrc == LwM2mObject.class) {
            element.add("instances", context.serialize(((LwM2mObject) src).getInstances().values()));
        } else if (typeOfSrc == LwM2mObjectInstance.class) {
            element.add("resources", context.serialize(((LwM2mObjectInstance) src).getResources().values()));
        } else if (typeOfSrc == LwM2mResource.class) {
            LwM2mResource rsc = (LwM2mResource) src;
            if (rsc.isMultiInstances()) {
                Object[] values = new Object[rsc.getValues().length];
                for (int i = 0; i < rsc.getValues().length; i++) {
                    values[i] = rsc.getValues()[i].value;
                }
                element.add("values", context.serialize(values));
            } else {
                element.add("value", context.serialize(rsc.getValue().value));
            }
        }

        return element;
    }

}
