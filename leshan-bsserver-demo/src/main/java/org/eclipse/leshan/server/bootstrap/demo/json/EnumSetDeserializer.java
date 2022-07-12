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

import java.util.EnumSet;

import org.eclipse.leshan.core.request.BindingMode;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.DeserializationConfig;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.deser.ContextualDeserializer;
import com.fasterxml.jackson.databind.deser.std.EnumDeserializer;
import com.fasterxml.jackson.databind.util.EnumResolver;

public class EnumSetDeserializer extends JsonDeserializer<EnumSet<?>> implements ContextualDeserializer {

    @Override
    public EnumSet<BindingMode> deserialize(JsonParser p, DeserializationContext ctxt) {
        return null;
    }

    @Override
    public JsonDeserializer<?> createContextual(DeserializationContext ctxt, BeanProperty property) {
        if (ctxt.getContextualType().getContentType().getRawClass() == BindingMode.class) {
            return new EnumSetBindingModeDeserializer();
        } else {
            return buildDefaultEnumSetDeserializer(ctxt);
        }
    }

    private com.fasterxml.jackson.databind.deser.std.EnumSetDeserializer buildDefaultEnumSetDeserializer(
            DeserializationContext ctxt) {
        JavaType contextualType = ctxt.getContextualType();
        JavaType contentType = contextualType.getContentType();
        DeserializationConfig config = ctxt.getConfig();

        return new com.fasterxml.jackson.databind.deser.std.EnumSetDeserializer(contentType, new EnumDeserializer(
                buildDefaultEnumResolver(ctxt), config.isEnabled(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS)));
    }

    protected EnumResolver buildDefaultEnumResolver(DeserializationContext ctxt) {
        JavaType contextualType = ctxt.getContextualType();
        JavaType contentType = contextualType.getContentType();
        Class<?> rawClass = contentType.getRawClass();
        DeserializationConfig config = ctxt.getConfig();

        return EnumResolver.constructFor(config, rawClass);
    }
}
