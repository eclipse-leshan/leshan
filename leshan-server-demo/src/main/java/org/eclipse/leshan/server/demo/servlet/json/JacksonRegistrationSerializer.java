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
 *     Orange - keep one JSON dependency
 *******************************************************************************/
package org.eclipse.leshan.server.demo.servlet.json;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

import org.eclipse.leshan.core.request.BindingMode;
import org.eclipse.leshan.server.queue.PresenceService;
import org.eclipse.leshan.server.registration.Registration;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

public class JacksonRegistrationSerializer extends StdSerializer<Registration> {

    private static final long serialVersionUID = -2828961931685566265L;

    private final PresenceService presenceService;

    protected JacksonRegistrationSerializer(Class<Registration> t, PresenceService presenceService) {
        super(t);
        this.presenceService = presenceService;
    }

    public JacksonRegistrationSerializer(PresenceService presenceService) {
        this(null, presenceService);
    }

    @Override
    public void serialize(Registration src, JsonGenerator gen, SerializerProvider provider) throws IOException {
        Map<String, Object> map = new LinkedHashMap<>();

        map.put("endpoint", src.getEndpoint());
        map.put("registrationId", src.getId());

        map.put("registrationDate", src.getRegistrationDate());

        map.put("lastUpdate", src.getLastUpdate());

        map.put("address", src.getAddress().getHostAddress() + ":" + src.getPort());
        map.put("smsNumber", src.getSmsNumber());
        map.put("lwM2mVersion", src.getLwM2mVersion().toString());
        map.put("lifetime", src.getLifeTimeInSec());
        map.put("bindingMode", BindingMode.toString(src.getBindingMode()));

        map.put("rootPath", src.getRootPath());
        map.put("objectLinks", src.getSortedObjectLinks());
        map.put("secure", src.getIdentity().isSecure());
        map.put("additionalRegistrationAttributes", src.getAdditionalRegistrationAttributes());
        map.put("queuemode", src.usesQueueMode());

        if (src.usesQueueMode()) {
            map.put("sleeping", !presenceService.isClientAwake(src));
        }

        gen.writeObject(map);
    }
}
