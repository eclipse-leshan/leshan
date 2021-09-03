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
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.eclipse.leshan.core.Link;
import org.eclipse.leshan.core.LwM2m.LwM2mVersion;
import org.eclipse.leshan.core.request.BindingMode;
import org.eclipse.leshan.core.request.ContentFormat;
import org.eclipse.leshan.server.registration.Registration;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonObject.Member;
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
        o.add("ver", r.getLwM2mVersion().toString());
        o.add("bnd", BindingMode.toString(r.getBindingMode()));
        if (r.getQueueMode() != null)
            o.add("qm", r.getQueueMode());
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

        // add supported content format
        Set<ContentFormat> supportedContentFormat = r.getSupportedContentFormats();
        JsonArray ct = Json.array();
        for (ContentFormat contentFormat : supportedContentFormat) {
            ct.add(contentFormat.getCode());
        }
        o.add("ct", ct);

        // handle supported object
        JsonObject so = Json.object();
        for (Entry<Integer, String> supportedObject : r.getSupportedObject().entrySet()) {
            so.add(supportedObject.getKey().toString(), supportedObject.getValue());
        }
        o.add("suppObjs", so);

        // handle application data
        JsonObject ad = Json.object();
        for (Entry<String, String> appData : r.getApplicationData().entrySet()) {
            ad.add(appData.getKey(), appData.getValue());
        }
        o.add("appdata", ad);
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
        b.bindingMode(BindingMode.parse(jObj.getString("bnd", null)));
        if (jObj.get("qm") != null)
            b.queueMode(jObj.getBoolean("qm", false));
        b.lastUpdate(new Date(jObj.getLong("lastUp", 0)));
        b.lifeTimeInSec(jObj.getLong("lt", 0));
        String versionAsString = jObj.getString("ver", null);
        if (versionAsString == null) {
            b.lwM2mVersion(LwM2mVersion.getDefault());
        } else {
            b.lwM2mVersion(LwM2mVersion.get(versionAsString));
        }
        b.registrationDate(new Date(jObj.getLong("regDate", 0)));
        if (jObj.get("sms") != null) {
            b.smsNumber(jObj.getString("sms", ""));
        }

        b.rootPath(jObj.getString("root", "/"));

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

        // additional attributes
        Map<String, String> addAttr = new HashMap<>();
        JsonObject o = (JsonObject) jObj.get("addAttr");
        for (String k : o.names()) {
            addAttr.put(k, o.getString(k, ""));
        }
        b.additionalRegistrationAttributes(addAttr);

        // add supported content format
        JsonValue ct = jObj.get("ct");
        if (ct == null) {
            // Backward compatibility : if suppObjs doesn't exist we extract supported object from object link
            b.extractDataFromObjectLink(true);
        } else {
            Set<ContentFormat> supportedContentFormat = new HashSet<>();
            for (JsonValue ctCode : ct.asArray()) {
                supportedContentFormat.add(ContentFormat.fromCode(ctCode.asInt()));
            }
            b.supportedContentFormats(supportedContentFormat);
        }
        // parse supported object
        JsonValue so = jObj.get("suppObjs");
        if (so == null) {
            // Backward compatibility : if suppObjs doesn't exist we extract supported object from object link
            b.extractDataFromObjectLink(true);
        } else {
            Map<Integer, String> supportedObject = new HashMap<>();
            for (Member member : so.asObject()) {
                supportedObject.put(Integer.parseInt(member.getName()), member.getValue().asString());
            }
            b.supportedObjects(supportedObject);
        }

        // app data
        Map<String, String> appData = new HashMap<>();
        JsonObject oap = (JsonObject) jObj.get("appdata");
        for (String k : oap.names()) {
            JsonValue jv = oap.get(k);
            if (jv.isNull()) {
                appData.put(k, null);
            } else {
                appData.put(k, jv.asString());
            }
        }
        b.applicationData(appData);

        return b.build();
    }

    public static Registration deserialize(byte[] data) {
        return deserialize((JsonObject) Json.parse(new String(data)));
    }
}