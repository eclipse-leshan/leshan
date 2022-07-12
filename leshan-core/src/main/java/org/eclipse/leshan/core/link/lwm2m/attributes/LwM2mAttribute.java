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
package org.eclipse.leshan.core.link.lwm2m.attributes;

import org.eclipse.leshan.core.link.attributes.Attribute;
import org.eclipse.leshan.core.util.Validate;

/**
 * Represents an LwM2m Attribute that can be attached to an object, instance or resource.
 *
 * The {@link Attachment} level of the attribute indicates where it can be applied, e.g. the 'pmin' attribute is only
 * applicable to resources, but it can be assigned on all levels and then inherited by underlying resources.
 */
public class LwM2mAttribute<T> implements Attribute {
    private final LwM2mAttributeModel<T> model;
    private final Object value;

    /**
     * Some constants about model are available at {@link LwM2mAttributes}
     *
     * @see LwM2mAttributes
     */
    public LwM2mAttribute(LwM2mAttributeModel<T> model) {
        Validate.notNull(model);
        this.model = model;
        this.value = null;
    }

    /**
     * Some constants about model are available at {@link LwM2mAttributes}
     * <p>
     * {@link LwM2mAttributes#create(LwM2mAttributeModel, Object)} is more convenient way to create LwM2mAttribute
     *
     * @see LwM2mAttributes
     * @see LwM2mAttributes#create(LwM2mAttributeModel, Object)
     */
    public LwM2mAttribute(LwM2mAttributeModel<T> model, T value) {
        Validate.notNull(model);
        this.model = model;
        this.value = value;
        String errorMessage = model.getInvalidValueCause(value);
        if (errorMessage != null) {
            throw new IllegalArgumentException(errorMessage);
        }
    }

    @Override
    public String getName() {
        return model.getName();
    }

    @Override
    public boolean hasValue() {
        return value != null;
    }

    @SuppressWarnings("unchecked")
    @Override
    public T getValue() {
        return (T) value;
    }

    public LwM2mAttributeModel<T> getModel() {
        return model;
    }

    @Override
    public String getCoreLinkValue() {
        return model.toCoreLinkValue(this);
    }

    @Override
    public String toCoreLinkFormat() {
        if (hasValue()) {
            return getName() + "=" + getCoreLinkValue();
        } else {
            return getName();
        }
    }

    public Attachment getAttachment() {
        return model.getAttachment();
    }

    public boolean isWritable() {
        return model.getAccessMode() == AccessMode.W || model.getAccessMode() == AccessMode.RW;
    }

    public boolean canBeAssignedTo(AssignationLevel assignationLevel) {
        return model.canBeAssignedTo(assignationLevel);
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
        LwM2mAttribute<?> other = (LwM2mAttribute<?>) obj;
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
