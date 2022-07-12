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

import java.util.regex.Pattern;

import org.eclipse.leshan.core.util.Validate;

/**
 * A base class to implement an {@link Attribute}
 */
public abstract class BaseAttribute implements Attribute {

    private static final Pattern parnamePattern = Pattern.compile("[!#$&+\\-.^_`|~a-zA-Z0-9]+");

    private String name;
    private Object value;

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
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((name == null) ? 0 : name.hashCode());
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
        BaseAttribute other = (BaseAttribute) obj;
        if (name == null) {
            if (other.name != null)
                return false;
        } else if (!name.equals(other.name))
            return false;
        if (value == null) {
            if (other.value != null)
                return false;
        } else if (!value.equals(other.value))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return String.format("%s=%s", name, value);
    }
}
