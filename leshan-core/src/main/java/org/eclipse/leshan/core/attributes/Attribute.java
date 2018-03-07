/*******************************************************************************
 * Copyright (c) 2013-2018 Sierra Wireless and others.
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
 *     Daniel Persson (Husqvarna Group) - Attribute support
 *******************************************************************************/
package org.eclipse.leshan.core.attributes;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 * Represents an LwM2m attribute that can be attached to an object, instance or resource.
 * 
 * The {@link Attachment} level of the attribute indicates where it can be applied, e.g.
 * the 'pmin' attribute is only applicable to resources, but it can be assigned on all levels
 * and then inherited by underlying resources.
 */
public class Attribute {
    
    /**
     * Metadata container for LwM2m attributes
     */
    private static class AttributeModel {
        private final String coRELinkParam;
        private final Attachment attachment;
        private final Set<AssignationLevel> assignationLevels;
        private AccessMode accessMode;
        private Class<?> valueClass;

        private AttributeModel(String coRELinkParam, Attachment attachment, Set<AssignationLevel> assignationLevels,
                AccessMode accessMode, Class<?> valueClass) {
            this.coRELinkParam = coRELinkParam;
            this.attachment = attachment;
            this.assignationLevels = assignationLevels;
            this.accessMode = accessMode;
            this.valueClass = valueClass;
        }
    }
    
    private static Map<AttributeName, AttributeModel> modelMap;
    private static Map<String, AttributeName> coRELinkParamMap;
    
    static {
        modelMap = new HashMap<>();
        modelMap.put(AttributeName.DIMENSION, new Attribute.AttributeModel("dim", Attachment.RESOURCE, EnumSet.of(AssignationLevel.RESOURCE),
                AccessMode.R, Long.class));
        modelMap.put(AttributeName.OBJECT_VERSION, new Attribute.AttributeModel("ver", Attachment.OBJECT, EnumSet.of(AssignationLevel.OBJECT),
                AccessMode.R, String.class));
        modelMap.put(AttributeName.MINIMUM_PERIOD, new Attribute.AttributeModel("pmin", Attachment.RESOURCE,
                EnumSet.of(AssignationLevel.OBJECT, AssignationLevel.INSTANCE, AssignationLevel.RESOURCE),
                AccessMode.RW, Long.class));
        modelMap.put(AttributeName.MAXIMUM_PERIOD, new Attribute.AttributeModel("pmax", Attachment.RESOURCE,
                EnumSet.of(AssignationLevel.OBJECT, AssignationLevel.INSTANCE, AssignationLevel.RESOURCE),
                AccessMode.RW, Long.class));
        modelMap.put(AttributeName.GREATER_THAN, new AttributeModel("gt", Attachment.RESOURCE,
                EnumSet.of(AssignationLevel.RESOURCE),  AccessMode.RW,  Double.class));
        modelMap.put(AttributeName.LESS_THAN, new AttributeModel("lt", Attachment.RESOURCE,
                EnumSet.of(AssignationLevel.RESOURCE),  AccessMode.RW,  Double.class));
        modelMap.put(AttributeName.STEP, new AttributeModel("st", Attachment.RESOURCE,
                EnumSet.of(AssignationLevel.RESOURCE),  AccessMode.RW,  Double.class));
        
        // Create a map from coRELinkParam to AttributeName as well
        coRELinkParamMap = new HashMap<>();
        for (Entry<AttributeName, AttributeModel> entry  : modelMap.entrySet()) {
            coRELinkParamMap.put(modelMap.get(entry.getKey()).coRELinkParam, entry.getKey());
        }
    }

    private final AttributeModel model;
    private final AttributeName name;
    private final Object value;
    
    public Attribute(AttributeName name, Object value) {
        this.model = modelMap.get(name);
        // Ensure that the attribute value has the correct type
        if (!this.model.valueClass.equals(value.getClass())) {
            throw new IllegalArgumentException(String.format("Attribute %s must have a value of type %s",
                    name.name(), model.valueClass.getSimpleName()));
        }
        this.name = name;
        this.value = value;
    }

    public static Attribute create(AttributeName name, Object value) {
        return new Attribute(name, value);
    }

    public static Attribute create(String coRELinkParam, Object value) {
        if (!coRELinkParamMap.containsKey(coRELinkParam)) {
            throw new IllegalArgumentException(String.format("Unsupported attribute '%s'", coRELinkParam));
        }
        AttributeName attributeName = coRELinkParamMap.get(coRELinkParam);
        // If the value is a string, we make an attempt to convert it
        Class<?> expectedClass = modelMap.get(attributeName).valueClass;
        if (!expectedClass.equals(value.getClass()) && value instanceof String) {
            if (expectedClass.equals(Long.class)) {
                return new Attribute(attributeName, Long.parseLong(value.toString()));
            } else if (expectedClass.equals(Double.class)) {
                return new Attribute(attributeName, Double.parseDouble(value.toString()));
            }
        }
        return new Attribute(attributeName, value);
    }
    
    public AttributeName getName() {
        return name;
    }

    public Object getValue() {
        return value;
    }
    
    public String getCoRELinkParam() {
        return model.coRELinkParam;
    }

    public Attachment getAttachment() {
        return model.attachment;
    }
    
    public boolean isWritable() {
        return model.accessMode == AccessMode.W || model.accessMode == AccessMode.RW;
    }
    
    public boolean canBeAssignedTo(AssignationLevel assignationLevel) {
        return model.assignationLevels.contains(assignationLevel);
    }
}
