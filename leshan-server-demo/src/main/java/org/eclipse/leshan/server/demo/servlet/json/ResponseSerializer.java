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
package org.eclipse.leshan.server.demo.servlet.json;

import java.lang.reflect.Type;

import org.eclipse.leshan.core.response.DiscoverResponse;
import org.eclipse.leshan.core.response.LwM2mResponse;
import org.eclipse.leshan.core.response.ReadResponse;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

public class ResponseSerializer implements JsonSerializer<LwM2mResponse> {

    @Override
    public JsonElement serialize(LwM2mResponse src, Type typeOfSrc, JsonSerializationContext context) {
        JsonObject element = new JsonObject();

        element.addProperty("status", src.getCode().toString());
        element.addProperty("valid", src.isValid());
        element.addProperty("success", src.isSuccess());
        element.addProperty("failure", src.isFailure());

        if (typeOfSrc instanceof Class<?>) {
            if (ReadResponse.class.isAssignableFrom((Class<?>) typeOfSrc)) {
                element.add("content", context.serialize(((ReadResponse) src).getContent()));
            } else if (DiscoverResponse.class.isAssignableFrom((Class<?>) typeOfSrc)) {
                element.add("objectLinks", context.serialize(((DiscoverResponse) src).getObjectLinks()));
            }
        }

        return element;
    }
}
