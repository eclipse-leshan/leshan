/*******************************************************************************
 * Copyright (c) 2013-2018 Sierra Wireless and others.
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
 *     Daniel Persson (Husqvarna Group) - Attribute support
 *******************************************************************************/
package org.eclipse.leshan.core.attributes;

import org.eclipse.leshan.core.util.Validate;

/**
 * Represents an LwM2m attribute that can be attached to an object, instance or resource.
 * 
 * The {@link Attachment} level of the attribute indicates where it can be applied, e.g. the 'pmin' attribute is only
 * applicable to resources, but it can be assigned on all levels and then inherited by underlying resources.
 */
public class LwM2mAttribute {
    private final LwM2mAttributeModel<?> model;
    private final Object value;

    public LwM2mAttribute(String coRELinkParam) {
        Validate.notEmpty(coRELinkParam);
        this.model = LwM2mAttributeModel.modelMap.get(coRELinkParam);
        if (model == null) {
            throw new IllegalArgumentException(String.format("Unsupported attribute '%s'", coRELinkParam));
        }
        this.value = null;
    }

    public LwM2mAttribute(String coRELinkParam, Object value) {
        Validate.notEmpty(coRELinkParam);
        this.model = LwM2mAttributeModel.modelMap.get(coRELinkParam);
        if (model == null) {
            throw new IllegalArgumentException(String.format("Unsupported attribute '%s'", coRELinkParam));
        }
        this.value = ensureMatchingValue(model, value);
    }

    /**
     * Ensures that a provided attribute value matches the attribute value type, including trying to perform a correct
     * conversion if the value is a string, e.g.
     * 
     * @return the converted or original value
     */
    private Object ensureMatchingValue(LwM2mAttributeModel<?> model, Object value) {
        // Ensure that the attribute value has the correct type
        // If the value is a string, we make an attempt to convert it
        Class<?> expectedClass = model.valueClass;
        if (!expectedClass.equals(value.getClass()) && value instanceof String) {
            if (expectedClass.equals(Long.class)) {
                return Long.parseLong(value.toString());
            } else if (expectedClass.equals(Double.class)) {
                return Double.parseDouble(value.toString());
            }
        } else if (!this.model.valueClass.equals(value.getClass())) {
            throw new IllegalArgumentException(String.format("Attribute '%s' must have a value of type %s",
                    model.coRELinkParam, model.valueClass.getSimpleName()));
        }
        return value;
    }

    public String getCoRELinkParam() {
        return model.coRELinkParam;
    }

    public Object getValue() {
        return value;
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

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((model == null) ? 0 : model.hashCode());
        result = prime * result + ((value == null) ? 0 : value.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        LwM2mAttribute other = (LwM2mAttribute) obj;
        if (model == null) {
            if (other.model != null)
                return false;
        } else if (!model.equals(other.model))
            return false;
        if (value == null) {
            if (other.value != null)
                return false;
        } else if (!value.equals(other.value))
            return false;
        return true;
    }
}
