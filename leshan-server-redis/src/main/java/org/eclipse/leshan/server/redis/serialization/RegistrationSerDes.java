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
 *     Orange - keep one JSON dependency
 *******************************************************************************/
package org.eclipse.leshan.server.redis.serialization;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.eclipse.leshan.core.LwM2m.LwM2mVersion;
import org.eclipse.leshan.core.LwM2m.Version;
import org.eclipse.leshan.core.link.Link;
import org.eclipse.leshan.core.link.attributes.Attribute;
import org.eclipse.leshan.core.link.attributes.AttributeModel;
import org.eclipse.leshan.core.link.attributes.AttributeParser;
import org.eclipse.leshan.core.link.attributes.Attributes;
import org.eclipse.leshan.core.link.attributes.DefaultAttributeParser;
import org.eclipse.leshan.core.link.attributes.InvalidAttributeException;
import org.eclipse.leshan.core.link.lwm2m.MixedLwM2mLink;
import org.eclipse.leshan.core.link.lwm2m.attributes.LwM2mAttributes;
import org.eclipse.leshan.core.link.lwm2m.attributes.MixedLwM2mAttributeSet;
import org.eclipse.leshan.core.node.LwM2mPath;
import org.eclipse.leshan.core.request.BindingMode;
import org.eclipse.leshan.core.request.ContentFormat;
import org.eclipse.leshan.server.registration.Registration;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Functions for serialize and deserialize a Client in JSON.
 */
public class RegistrationSerDes {

    private final AttributeParser attributeParser;

    public RegistrationSerDes() {
        // Define all supported Attributes
        Collection<AttributeModel<?>> suppportedAttributes = new ArrayList<AttributeModel<?>>();
        suppportedAttributes.addAll(Attributes.ALL);
        suppportedAttributes.addAll(LwM2mAttributes.ALL);

        // Create default link Parser
        this.attributeParser = new DefaultAttributeParser(suppportedAttributes);
    }

    public RegistrationSerDes(AttributeParser attributeParser) {
        this.attributeParser = attributeParser;
    }

    public JsonNode jSerialize(Registration r) {
        ObjectNode o = JsonNodeFactory.instance.objectNode();
        o.put("regDate", r.getRegistrationDate().getTime());
        o.set("identity", IdentitySerDes.serialize(r.getIdentity()));
        o.put("lt", r.getLifeTimeInSec());
        if (r.getSmsNumber() != null) {
            o.put("sms", r.getSmsNumber());
        }
        o.put("ver", r.getLwM2mVersion().toString());
        o.put("bnd", BindingMode.toString(r.getBindingMode()));
        if (r.getQueueMode() != null)
            o.put("qm", r.getQueueMode());
        o.put("ep", r.getEndpoint());
        o.put("regId", r.getId());
        o.put("epUri", r.getLastEndpointUsed().toString());

        ArrayNode links = JsonNodeFactory.instance.arrayNode();
        for (Link l : r.getObjectLinks()) {
            ObjectNode ol = JsonNodeFactory.instance.objectNode();
            ol.put("url", l.getUriReference());
            ObjectNode at = JsonNodeFactory.instance.objectNode();
            for (Attribute a : l.getAttributes()) {
                if (a.hasValue()) {
                    at.put(a.getName(), a.getCoreLinkValue());
                } else {
                    at.set(a.getName(), null);

                }
            }
            ol.set("at", at);
            links.add(ol);
        }
        o.set("objLink", links);
        ObjectNode addAttr = JsonNodeFactory.instance.objectNode();
        for (Map.Entry<String, String> e : r.getAdditionalRegistrationAttributes().entrySet()) {
            addAttr.put(e.getKey(), e.getValue());
        }
        o.set("addAttr", addAttr);
        o.put("root", r.getRootPath());
        o.put("lastUp", r.getLastUpdate().getTime());

        // add supported content format
        Set<ContentFormat> supportedContentFormat = r.getSupportedContentFormats();
        ArrayNode ct = JsonNodeFactory.instance.arrayNode();
        for (ContentFormat contentFormat : supportedContentFormat) {
            ct.add(contentFormat.getCode());
        }
        o.set("ct", ct);

        // handle supported object
        ObjectNode so = JsonNodeFactory.instance.objectNode();
        for (Entry<Integer, Version> supportedObject : r.getSupportedObject().entrySet()) {
            so.put(supportedObject.getKey().toString(), supportedObject.getValue().toString());
        }
        o.set("suppObjs", so);

        // handle available instances
        ArrayNode ai = JsonNodeFactory.instance.arrayNode();
        for (LwM2mPath instance : r.getAvailableInstances()) {
            ai.add(instance.toString());
        }
        o.set("objInstances", ai);

        // handle application data
        ObjectNode ad = JsonNodeFactory.instance.objectNode();
        for (Entry<String, String> appData : r.getApplicationData().entrySet()) {
            ad.put(appData.getKey(), appData.getValue());
        }
        o.set("appdata", ad);
        return o;
    }

    public String sSerialize(Registration r) {
        return jSerialize(r).toString();
    }

    public byte[] bSerialize(Registration r) {
        return jSerialize(r).toString().getBytes();
    }

    public Registration deserialize(JsonNode jObj) {
        Registration.Builder b = new Registration.Builder(jObj.get("regId").asText(), jObj.get("ep").asText(),
                IdentitySerDes.deserialize(jObj.get("identity")));

        try {
            b.lastEndpointUsed(new URI(jObj.get("epUri").asText()));
        } catch (URISyntaxException e1) {
            throw new IllegalStateException(
                    String.format("Unable to deserialize last endpoint used URI %s of registration %s/%s",
                            jObj.get("epUri").asText(), jObj.get("regId").asText(), jObj.get("ep").asText()));
        }

        b.bindingMode(BindingMode.parse(jObj.get("bnd").asText()));
        if (jObj.get("qm") != null)
            b.queueMode(jObj.get("qm").asBoolean());
        b.lastUpdate(new Date(jObj.get("lastUp").asLong(0)));
        b.lifeTimeInSec(jObj.get("lt").asLong(0));
        String versionAsString = jObj.get("ver").asText();
        if (versionAsString == null) {
            b.lwM2mVersion(LwM2mVersion.getDefault());
        } else {
            b.lwM2mVersion(LwM2mVersion.get(versionAsString));
        }
        b.registrationDate(new Date(jObj.get("regDate").asLong(0)));
        if (jObj.get("sms") != null) {
            b.smsNumber(jObj.get("sms").asText(""));
        }

        String rootPath = jObj.get("root").asText("/");
        b.rootPath(rootPath);

        ArrayNode links = (ArrayNode) jObj.get("objLink");
        Link[] linkObjs = new Link[links.size()];
        for (int i = 0; i < links.size(); i++) {
            ObjectNode ol = (ObjectNode) links.get(i);

            List<Attribute> atts = new ArrayList<>();
            JsonNode att = ol.get("at");
            for (Iterator<String> it = att.fieldNames(); it.hasNext();) {
                String k = it.next();
                JsonNode jsonValue = att.get(k);
                String attValue;
                if (jsonValue.isNull()) {
                    attValue = null;
                } else {
                    if (jsonValue.isNumber()) {
                        // This else block is just needed for retro-compatibility
                        attValue = Integer.toString(jsonValue.asInt());
                    } else {
                        attValue = jsonValue.asText();
                    }
                }
                try {
                    atts.add(attributeParser.parseCoreLinkValue(k, attValue));
                } catch (InvalidAttributeException e) {
                    throw new IllegalStateException(
                            String.format("Unable to deserialize attribute value from links of registraiton %s/%s",
                                    jObj.get("regId").asText(), jObj.get("ep").asText()));
                }
            }
            // handle lwm2m path
            String path = ol.get("url").asText();
            Link o;
            if (path.startsWith(rootPath)) {
                LwM2mPath lwm2mPath = LwM2mPath.parse(path, rootPath);
                o = new MixedLwM2mLink(rootPath, lwm2mPath, new MixedLwM2mAttributeSet(atts));
            } else {
                o = new Link(path, atts);
            }
            linkObjs[i] = o;
        }
        b.objectLinks(linkObjs);

        // parse additional attributes
        Map<String, String> addAttr = new HashMap<>();
        ObjectNode o = (ObjectNode) jObj.get("addAttr");
        for (Iterator<String> it = o.fieldNames(); it.hasNext();) {
            String k = it.next();
            addAttr.put(k, o.get(k).asText(""));
        }
        b.additionalRegistrationAttributes(addAttr);

        // parse supported content format
        JsonNode ct = jObj.get("ct");
        if (ct == null) {
            // Backward compatibility : if ct doesn't exist we extract supported content format from object link
            b.extractDataFromObjectLink(true);
        } else {
            Set<ContentFormat> supportedContentFormat = new HashSet<>();
            for (JsonNode ctCode : ct) {
                supportedContentFormat.add(ContentFormat.fromCode(ctCode.asInt()));
            }
            b.supportedContentFormats(supportedContentFormat);
        }
        // parse supported object
        JsonNode so = jObj.get("suppObjs");
        if (so == null) {
            // Backward compatibility : if suppObjs doesn't exist we extract supported object from object link
            b.extractDataFromObjectLink(true);
        } else {
            Map<Integer, Version> supportedObject = new HashMap<>();
            for (Iterator<String> it = so.fieldNames(); it.hasNext();) {
                String key = it.next();
                supportedObject.put(Integer.parseInt(key), new Version(so.get(key).asText()));
            }
            b.supportedObjects(supportedObject);
        }

        // parse available instances
        JsonNode ai = jObj.get("objInstances");
        if (ai == null) {
            // Backward compatibility : if objInstances doesn't exist we extract available instances from object link
            b.extractDataFromObjectLink(true);
        } else {
            Set<LwM2mPath> availableInstances = new HashSet<>();
            for (JsonNode aiPath : ai) {
                availableInstances.add(new LwM2mPath(aiPath.asText()));
            }
            b.availableInstances(availableInstances);
        }

        // parse app data
        Map<String, String> appData = new HashMap<>();
        ObjectNode oap = (ObjectNode) jObj.get("appdata");
        for (Iterator<String> it = oap.fieldNames(); it.hasNext();) {
            String key = it.next();
            JsonNode jv = oap.get(key);
            if (jv.isNull()) {
                appData.put(key, null);
            } else {
                appData.put(key, jv.asText());
            }
        }
        b.applicationData(appData);

        return b.build();
    }

    public Registration deserialize(byte[] data) {
        String json = new String(data);
        try {
            return deserialize(new ObjectMapper().readTree(json));
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException(String.format("Unable to deserialize Registration %s", json), e);
        }
    }
}
