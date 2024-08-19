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

import org.eclipse.leshan.core.util.Validate;

/**
 * An attribute without value
 */
public class ValuelessAttribute implements Attribute {

    private final String name;

    public ValuelessAttribute(String name) {
        Validate.notNull(name);
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Object getValue() {
        return null;
    }

    @Override
    public String getCoreLinkValue() {
        return null;
    }

    @Override
    public boolean hasValue() {
        return false;
    }

    @Override
    public String toString() {
        return name;
    }

    @Override
    public String toCoreLinkFormat() {
        return name;
    }

    @Override
    public final boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof ValuelessAttribute))
            return false;
        ValuelessAttribute that = (ValuelessAttribute) o;
        return Objects.equals(name, that.name);
    }

    @Override
    public final int hashCode() {
        return Objects.hashCode(name);
    }
}
