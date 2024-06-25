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
import java.util.EnumSet;

import org.eclipse.leshan.core.request.BindingMode;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.PropertyName;
import com.fasterxml.jackson.databind.SerializationConfig;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.introspect.BasicBeanDescription;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

public class EnumSetSerializer extends StdSerializer<EnumSet<?>> {

    private static final long serialVersionUID = 7456682518094775441L;

    public EnumSetSerializer(JavaType type) {
        super(type);
    }

    @SuppressWarnings("unchecked")
    @Override
    public void serialize(EnumSet<?> value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        if (value == null) {
            gen.writeNull();
        } else {
            if (isBindingMode(gen, serializers)) {
                gen.writeString(BindingMode.toString((EnumSet<BindingMode>) value));
            } else {
                defaultEnumSetSerializer(value, gen, serializers);
            }
        }
    }

    private void defaultEnumSetSerializer(EnumSet<?> value, JsonGenerator gen, SerializerProvider serializers)
            throws IOException {
        JavaType valueType = serializers.getConfig().constructType(value.getClass());
        com.fasterxml.jackson.databind.ser.std.EnumSetSerializer enumSetSerializer = new com.fasterxml.jackson.databind.ser.std.EnumSetSerializer(
                valueType);
        enumSetSerializer.serialize(value, gen, serializers);
    }

    private boolean isBindingMode(JsonGenerator gen, SerializerProvider serializers) {
        SerializationConfig config = serializers.getConfig();

        JavaType currValue = config.constructType(gen.getCurrentValue().getClass());
        BeanDescription beanDesc = config.introspect(currValue);

        String currentName = gen.getOutputContext().getCurrentName();
        JavaType primaryType = ((BasicBeanDescription) beanDesc).findProperty(new PropertyName(currentName))
                .getPrimaryType();

        return primaryType.hasContentType() && primaryType.getContentType().getRawClass() == BindingMode.class;
    }
}
