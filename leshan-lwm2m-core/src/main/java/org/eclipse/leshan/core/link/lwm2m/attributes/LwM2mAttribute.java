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

import java.util.Objects;

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
        this(model, null);
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
        if (value == null) {
            if (!model.canBeValueless()) {
                throw new IllegalArgumentException(String.format("Attribute %s must have a value", model.getName()));
            }
            this.value = null;
        } else {
            this.value = model.initValue(value);
            String errorMessage = model.getInvalidValueCause(value);
            if (errorMessage != null) {
                throw new IllegalArgumentException(errorMessage);
            }
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

    /**
     * @return the value as it should be serialized for the query parameter format in URL.
     */
    public String getQueryParamValue() {
        return model.toQueryParemValue(this);
    }

    /**
     * @return a attribute String as it should be serialized for the query parameter format in URL.
     *         <p>
     *         It should looks like : <code>"getName()=getQueryParamValue()"</code>
     */
    public String toQueryParamFormat() {
        if (hasValue()) {
            return getName() + "=" + getQueryParamValue();
        } else {
            if (getModel().queryParamCanBeValueless()) {
                return getName();
            } else {
                throw new IllegalStateException(String
                        .format("Attribute %s can not have null value when serialized in query param", getName()));
            }
        }
    }

    @Override
    public String toCoreLinkFormat() {
        if (hasValue()) {
            return getName() + "=" + getCoreLinkValue();
        } else {
            if (getModel().linkAttributeCanBeValueless()) {
                return getName();
            } else {
                throw new IllegalStateException(
                        String.format("Attribute %s can not have null value when serialized in CoreLink", getName()));
            }
        }
    }

    public boolean isWritable() {
        return model.getAccessMode() == AccessMode.W || model.getAccessMode() == AccessMode.RW;
    }

    public boolean canBeAttachedTo(Attachment attachement) {
        return model.canBeAttachedTo(attachement);
    }

    @Override
    public final boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof LwM2mAttribute))
            return false;
        LwM2mAttribute<?> that = (LwM2mAttribute<?>) o;
        return Objects.equals(model, that.model) && Objects.equals(value, that.value);
    }

    @Override
    public final int hashCode() {
        return Objects.hash(model, value);
    }

    @Override
    public String toString() {
        if (hasValue()) {
            return getName() + "=" + value;
        } else {
            return getName();
        }
    }
}
