/*******************************************************************************
 * Copyright (c) 2016 Sierra Wireless and others.
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
package org.eclipse.leshan.server.redis.serialization;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.leshan.core.Link;
import org.eclipse.leshan.core.request.BindingMode;
import org.eclipse.leshan.server.registration.Registration;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;

/**
 * Functions for serialize and deserialize a Client in JSON.
 */
public class RegistrationSerDes {

    public static JsonObject jSerialize(Registration r) {
        JsonObject o = Json.object();
        o.add("regDate", r.getRegistrationDate().getTime());
        o.add("identity", IdentitySerDes.serialize(r.getIdentity()));
        o.add("lt", r.getLifeTimeInSec());
        if (r.getSmsNumber() != null) {
            o.add("sms", r.getSmsNumber());
        }
        o.add("ver", r.getLwM2mVersion());
        o.add("bnd", r.getBindingMode().name());
        o.add("ep", r.getEndpoint());
        o.add("regId", r.getId());

        JsonArray links = new JsonArray();
        for (Link l : r.getObjectLinks()) {
            JsonObject ol = Json.object();
            ol.add("url", l.getUrl());
            JsonObject at = Json.object();
            for (Map.Entry<String, String> e : l.getAttributes().entrySet()) {
                if (e.getValue() == null) {
                    at.add(e.getKey(), Json.NULL);
                } else {
                    at.add(e.getKey(), e.getValue().toString());
                }
            }
            ol.add("at", at);
            links.add(ol);
        }
        o.add("objLink", links);
        JsonObject addAttr = Json.object();
        for (Map.Entry<String, String> e : r.getAdditionalRegistrationAttributes().entrySet()) {
            addAttr.add(e.getKey(), e.getValue());
        }
        o.add("addAttr", addAttr);
        o.add("root", r.getRootPath());
        o.add("lastUp", r.getLastUpdate().getTime());
        return o;
    }

    public static String sSerialize(Registration r) {
        return jSerialize(r).toString();
    }

    public static byte[] bSerialize(Registration r) {
        return jSerialize(r).toString().getBytes();
    }

    public static Registration deserialize(JsonObject jObj) {
        Registration.Builder b = new Registration.Builder(jObj.getString("regId", null), jObj.getString("ep", null),
                IdentitySerDes.deserialize(jObj.get("identity").asObject()));
        b.bindingMode(BindingMode.valueOf(jObj.getString("bnd", null)));
        b.lastUpdate(new Date(jObj.getLong("lastUp", 0)));
        b.lifeTimeInSec(jObj.getLong("lt", 0));
        b.lwM2mVersion(jObj.getString("ver", "1.0"));
        b.registrationDate(new Date(jObj.getLong("regDate", 0)));
        if (jObj.get("sms") != null) {
            b.smsNumber(jObj.getString("sms", ""));
        }

        JsonArray links = (JsonArray) jObj.get("objLink");
        Link[] linkObjs = new Link[links.size()];
        for (int i = 0; i < links.size(); i++) {
            JsonObject ol = (JsonObject) links.get(i);

            Map<String, String> attMap = new HashMap<>();
            JsonObject att = (JsonObject) ol.get("at");
            for (String k : att.names()) {
                JsonValue jsonValue = att.get(k);
                if (jsonValue.isNull()) {
                    attMap.put(k, null);
                } else if (jsonValue.isNumber()) {
                    // This else block is just needed for retro-compatibility
                    attMap.put(k, Integer.toString(jsonValue.asInt()));
                } else {
                    attMap.put(k, jsonValue.asString());
                }
            }
            Link o = new Link(ol.getString("url", null), attMap);
            linkObjs[i] = o;
        }
        b.objectLinks(linkObjs);
        Map<String, String> addAttr = new HashMap<>();
        JsonObject o = (JsonObject) jObj.get("addAttr");
        for (String k : o.names()) {
            addAttr.put(k, o.getString(k, ""));
        }
        b.additionalRegistrationAttributes(addAttr);

        return b.build();
    }

    public static Registration deserialize(byte[] data) {
        return deserialize((JsonObject) Json.parse(new String(data)));
    }
}