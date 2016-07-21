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

import java.util.Date;

import org.eclipse.leshan.core.model.ResourceModel.Type;
import org.eclipse.leshan.util.Base64;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonValue;

/**
 * Functions for serialize and deserialize a value of LWM2M resource in JSON.
 */
public class ValueSerDes {

    public static JsonValue jSerialize(Object value, Type type) {
        switch (type) {
        case INTEGER:
            return Json.value((long) value);
        case FLOAT:
            return Json.value((double) value);
        case BOOLEAN:
            return Json.value((boolean) value);
        case OPAQUE:
            return Json.value(Base64.encodeBase64String((byte[]) value));
        case STRING:
            return Json.value((String) value);
        case TIME:
            return Json.value(((Date) value).getTime());
        default:
            throw new IllegalArgumentException(String.format("Type %s is not supported", type.name()));
        }
    }

    public static String sSerialize(Object value, Type type) {
        return jSerialize(value, type).toString();
    }

    public static byte[] bSerialize(Object value, Type type) {
        return jSerialize(value, type).toString().getBytes();
    }

    public static Object deserialize(JsonValue v, Type type) {
        switch (type) {
        case INTEGER:
            return v.asLong();
        case FLOAT:
            return v.asDouble();
        case BOOLEAN:
            return v.asBoolean();
        case OPAQUE:
            return Base64.decodeBase64(v.asString());
        case STRING:
            return v.asString();
        case TIME:
            return new Date(v.asLong());
        default:
            throw new IllegalArgumentException(String.format("Type %s is not supported", type.name()));
        }
    }
}
