/*******************************************************************************
 * Copyright (c) 2013-2015 Sierra Wireless and others.
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
 *     Gemalto M2M GmbH
 *******************************************************************************/

package org.eclipse.leshan.core.json;

import org.eclipse.leshan.core.util.json.JsonException;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.ParseException;

/**
 * Helper for encoding/decoding LWM2M JSON format
 */
public class LwM2mJson {

    private static final JsonRootObjectSerDes serDes = new JsonRootObjectSerDes();

    public static String toJsonLwM2m(JsonRootObject jro) throws LwM2mJsonException {
        try {
            return serDes.sSerialize(jro);
        } catch (JsonException e) {
            throw new LwM2mJsonException("Unable to serialize LWM2M JSON.", e);
        }
    }

    public static JsonRootObject fromJsonLwM2m(String jsonString) throws LwM2mJsonException {
        try {
            return serDes.deserialize(Json.parse(jsonString).asObject());
        } catch (JsonException | ParseException e) {
            throw new LwM2mJsonException("Unable to parse LWM2M JSON.", e);
        }
    }
}
