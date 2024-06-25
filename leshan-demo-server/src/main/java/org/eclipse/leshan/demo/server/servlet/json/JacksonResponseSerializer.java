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

import org.eclipse.leshan.core.response.CreateResponse;
import org.eclipse.leshan.core.response.DiscoverResponse;
import org.eclipse.leshan.core.response.LwM2mResponse;
import org.eclipse.leshan.core.response.ReadCompositeResponse;
import org.eclipse.leshan.core.response.ReadResponse;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

public class JacksonResponseSerializer extends StdSerializer<LwM2mResponse> {

    private static final long serialVersionUID = -1249267471664578631L;

    protected JacksonResponseSerializer(Class<LwM2mResponse> t) {
        super(t);
    }

    public JacksonResponseSerializer() {
        this(null);
    }

    @Override
    public void serialize(LwM2mResponse src, JsonGenerator gen, SerializerProvider provider) throws IOException {
        Map<String, Object> map = new LinkedHashMap<>();

        map.put("status", src.getCode().toString());
        map.put("valid", src.isValid());
        map.put("success", src.isSuccess());
        map.put("failure", src.isFailure());

        if (src instanceof ReadResponse) {
            map.put("content", ((ReadResponse) src).getContent());
        } else if (src instanceof DiscoverResponse) {
            map.put("objectLinks", ((DiscoverResponse) src).getObjectLinks());
        } else if (src instanceof CreateResponse) {
            map.put("location", ((CreateResponse) src).getLocation());
        } else if (src instanceof ReadCompositeResponse) {
            map.put("content", ((ReadCompositeResponse) src).getContent());
        }

        if (src.isFailure() && src.getErrorMessage() != null && !src.getErrorMessage().isEmpty()) {
            map.put("errormessage", src.getErrorMessage());
        }

        gen.writeObject(map);
    }
}
