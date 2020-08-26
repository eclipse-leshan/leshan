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
package org.eclipse.leshan.core.util.json;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;

/**
 * An abstract class to easily create serializer/deserializer class based on "minimal-json".
 * 
 * @param <T> the type of the objects to serialize or deserialize.
 */
public abstract class JacksonJsonSerDes<T> {

    public abstract JsonNode jSerialize(T t) throws JsonException;

    public String sSerialize(T t) throws JsonException {
        return jSerialize(t).toString();
    }

    public byte[] bSerialize(T t) throws JsonException {
        return jSerialize(t).toString().getBytes();
    }

    public ArrayNode jSerialize(Collection<T> jaes) throws JsonException {
        ArrayNode array = JsonNodeFactory.instance.arrayNode();
        for (T jae : jaes) {
            array.add(jSerialize(jae));
        }
        return array;
    }

    public String sSerialize(Collection<T> ts) throws JsonException {
        return jSerialize(ts).toString();
    }

    public byte[] bSerialize(Collection<T> ts) throws JsonException {
        return jSerialize(ts).toString().getBytes();
    }

    public abstract T deserialize(JsonNode o) throws JsonException;

    public List<T> deserialize(Iterator<JsonNode> nodes) throws JsonException {
        ArrayList<T> res = new ArrayList<>();
        while (nodes.hasNext()) {
            res.add(deserialize(nodes.next()));
        }
        return res;
    }
}