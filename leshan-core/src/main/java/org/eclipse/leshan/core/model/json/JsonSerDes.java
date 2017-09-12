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
package org.eclipse.leshan.core.model.json;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;

public abstract class JsonSerDes<T> {

    public abstract JsonObject jSerialize(T t);

    public String sSerialize(T t) {
        return jSerialize(t).toString();
    }

    public byte[] bSerialize(T t) {
        return jSerialize(t).toString().getBytes();
    }

    public JsonArray jSerialize(Collection<T> ts) {
        JsonArray o = new JsonArray();
        if (ts != null) {
            for (T t : ts) {
                JsonObject jo = jSerialize(t);
                if (jo != null)
                    o.add(jo);
            }
        }
        return o;
    }

    public String sSerialize(Collection<T> ts) {
        return jSerialize(ts).toString();
    }

    public byte[] bSerialize(Collection<T> ts) {
        return jSerialize(ts).toString().getBytes();
    }

    public abstract T deserialize(JsonObject o);

    public List<T> deserialize(JsonArray a) {
        ArrayList<T> res = new ArrayList<>(a.size());
        for (JsonValue v : a) {
            res.add(deserialize(v.asObject()));
        }
        return res;
    }
}
