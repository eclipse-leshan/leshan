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
 *     Gemalto M2M GmbH
 *******************************************************************************/

package org.eclipse.leshan.json;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;

/**
 * Helper for encoding/decoding LWM2M JSON format
 */
public class LwM2mJson {

    private static final Gson gson = new GsonBuilder().create();

    public static String toJsonLwM2m(LwM2mJsonObject lwM2mJsonElement) {
        String json = gson.toJson(lwM2mJsonElement);
        return json;
    }

    public static LwM2mJsonObject fromJsonLwM2m(String jsonString) throws LwM2mJsonException {
        try {
            LwM2mJsonObject element = gson.fromJson(jsonString, LwM2mJsonObject.class);
            return element;
        } catch (JsonSyntaxException e) {
            throw new LwM2mJsonException("Unable to deserialize JSON String to LwM2mJsonObject ", e);
        }
    }

}
