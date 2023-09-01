/*******************************************************************************
 * Copyright (c) 2023 Sierra Wireless and others.
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

import java.io.IOException;

import org.eclipse.leshan.core.LwM2m.Version;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

public class JacksonVersionSerializer extends StdSerializer<Version> {

    private static final long serialVersionUID = 1L;

    public JacksonVersionSerializer() {
        this(null);
    }

    protected JacksonVersionSerializer(Class<Version> t) {
        super(t);
    }

    @Override
    public void serialize(Version value, JsonGenerator gen, SerializerProvider provider) throws IOException {
        gen.writeObject(value.toString());
    }
}
