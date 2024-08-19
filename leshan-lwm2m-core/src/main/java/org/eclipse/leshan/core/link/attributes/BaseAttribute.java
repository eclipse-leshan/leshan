/*******************************************************************************
 * Copyright (c) 2022 Sierra Wireless and others.
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
package org.eclipse.leshan.core.link.attributes;

import java.util.Objects;
import java.util.regex.Pattern;

import org.eclipse.leshan.core.util.Validate;

/**
 * A base class to implement an {@link Attribute}
 */
public abstract class BaseAttribute implements Attribute {

    private static final Pattern parnamePattern = Pattern.compile("[!#$&+\\-.^_`|~a-zA-Z0-9]+");

    private final String name;
    private final Object value;

    public BaseAttribute(String name, Object value, boolean validate) {
        this.name = name;
        this.value = value;
        if (validate) {
            validate();
        }
    }

    protected void validate() {
        Validate.notEmpty(name);
        // see org.eclipse.leshan.core.link.DefaultLinkParser#consumeParmName(StringParser<LinkParseException>)
        if (!parnamePattern.matcher(name).matches()) {
            throw new IllegalArgumentException(String.format("%s is not a valid name for Attribute", name));
        }
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Object getValue() {
        return value;
    }

    @Override
    public boolean hasValue() {
        return value != null;
    }

    @Override
    public String toCoreLinkFormat() {
        return name + "=" + getCoreLinkValue();
    }

    @Override
    public String toString() {
        return String.format("%s=%s", name, value);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof BaseAttribute))
            return false;
        BaseAttribute that = (BaseAttribute) o;
        return that.canEqual(this) && Objects.equals(name, that.name) && Objects.equals(value, that.value);
    }

    public boolean canEqual(Object o) {
        return (o instanceof BaseAttribute);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, value);
    }
}
