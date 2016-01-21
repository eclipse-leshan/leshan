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
package org.eclipse.leshan.standalone.servlet.json;

import java.lang.reflect.Type;

import org.eclipse.leshan.server.client.Client;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

public class ClientSerializer implements JsonSerializer<Client> {

    private final int securePort;

    public ClientSerializer(int securePort) {
        this.securePort = securePort;
    }

    @Override
    public JsonElement serialize(Client src, Type typeOfSrc, JsonSerializationContext context) {
        JsonObject element = new JsonObject();

        element.addProperty("endpoint", src.getEndpoint());
        element.addProperty("registrationId", src.getRegistrationId());
        element.add("registrationDate", context.serialize(src.getRegistrationDate()));
        element.add("lastUpdate", context.serialize(src.getLastUpdate()));
        element.addProperty("address", src.getAddress().toString() + ":" + src.getPort());
        element.addProperty("smsNumber", src.getSmsNumber());
        element.addProperty("lwM2MmVersion", src.getLwM2mVersion());
        element.addProperty("lifetime", src.getLifeTimeInSec());
        element.addProperty("bindingMode", src.getBindingMode().toString());
        element.add("rootPath", context.serialize(src.getRootPath()));
        element.add("objectLinks", context.serialize(src.getSortedObjectLinks()));
        element.add("secure", context.serialize(src.getRegistrationEndpointAddress().getPort() == securePort));

        return element;
    }
}
