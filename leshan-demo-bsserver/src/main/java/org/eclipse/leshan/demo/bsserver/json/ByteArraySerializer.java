/*******************************************************************************
 * Copyright (c) 2021 Orange.
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
 *     Orange - keep one JSON dependency
 *******************************************************************************/
package org.eclipse.leshan.server.bootstrap.demo.json;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

public class ByteArraySerializer extends StdSerializer<byte[]> {

    private static final long serialVersionUID = 1166117057647546937L;

    private final ByteMode byteMode;

    public enum ByteMode {
        SIGNED, UNSIGNED
    }

    protected ByteArraySerializer(Class<byte[]> t, ByteMode byteMode) {
        super(t);
        this.byteMode = byteMode;
    }

    public ByteArraySerializer(ByteMode byteMode) {
        this(byte[].class, byteMode);
    }

    @Override
    public void serialize(byte[] value, JsonGenerator gen, SerializerProvider provider) throws IOException {
        int[] output = new int[value.length];

        int mask = byteMode == ByteMode.SIGNED ? -1 : 0xff;
        for (int i = 0; i < value.length; i++) {
            output[i] = value[i] & mask;
        }
        gen.writeArray(output, 0, output.length);
    }
}
