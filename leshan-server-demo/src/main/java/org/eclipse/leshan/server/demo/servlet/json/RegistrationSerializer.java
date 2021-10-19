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
 *******************************************************************************/
package org.eclipse.leshan.server.demo.servlet.json;

import java.lang.reflect.Type;
import java.util.Map.Entry;

import org.eclipse.leshan.core.link.Link;
import org.eclipse.leshan.core.link.LinkParamValue;
import org.eclipse.leshan.core.request.BindingMode;
import org.eclipse.leshan.server.queue.PresenceService;
import org.eclipse.leshan.server.registration.Registration;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

public class RegistrationSerializer implements JsonSerializer<Registration> {

    private final PresenceService presenceService;

    public RegistrationSerializer(PresenceService presenceService) {
        this.presenceService = presenceService;
    }

    @Override
    public JsonElement serialize(Registration src, Type typeOfSrc, JsonSerializationContext context) {
        JsonObject element = new JsonObject();

        element.addProperty("endpoint", src.getEndpoint());
        element.addProperty("registrationId", src.getId());
        element.add("registrationDate", context.serialize(src.getRegistrationDate()));
        element.add("lastUpdate", context.serialize(src.getLastUpdate()));
        element.addProperty("address", src.getAddress().getHostAddress() + ":" + src.getPort());
        element.addProperty("smsNumber", src.getSmsNumber());
        element.addProperty("lwM2mVersion", src.getLwM2mVersion().toString());
        element.addProperty("lifetime", src.getLifeTimeInSec());
        element.addProperty("bindingMode", BindingMode.toString(src.getBindingMode()));
        element.add("rootPath", context.serialize(src.getRootPath()));
        element.add("objectLinks", serializeLinks(src.getSortedObjectLinks()));
        element.add("secure", context.serialize(src.getIdentity().isSecure()));
        element.add("additionalRegistrationAttributes", context.serialize(src.getAdditionalRegistrationAttributes()));
        element.add("queuemode", context.serialize(src.usesQueueMode()));

        if (src.usesQueueMode()) {
            element.add("sleeping", context.serialize(!presenceService.isClientAwake(src)));
        }

        return element;
    }

    private JsonArray serializeLinks(Link[] links) {
        JsonArray jlinks = new JsonArray(links.length);
        for (int i = 0; i < links.length; i++) {
            Link link = links[i];
            JsonObject jlink = new JsonObject();

            // add url
            jlink.add("url", new JsonPrimitive(link.getUriReference()));

            // add attributes
            JsonObject attributes = new JsonObject();
            for (Entry<String, LinkParamValue> linkParam : link.getLinkParams().entrySet()) {
                attributes.add(linkParam.getKey(), new JsonPrimitive(linkParam.getValue().toString()));
            }
            jlink.add("attributes", attributes);
            jlinks.add(jlink);
        }
        return jlinks;
    }
}
