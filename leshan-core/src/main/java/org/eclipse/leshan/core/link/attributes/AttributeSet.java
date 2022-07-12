/*******************************************************************************
 * Copyright (c) 2021 Sierra Wireless and others.
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

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A set of {@link Attribute}
 */
public class AttributeSet implements Iterable<Attribute> {

    private final Map<String, Attribute> attributes = new LinkedHashMap<>();

    public AttributeSet(Attribute... attributes) {
        this(Arrays.asList(attributes));
    }

    public AttributeSet(Collection<? extends Attribute> attributes) {
        if (attributes != null && !attributes.isEmpty()) {
            for (Attribute attr : attributes) {
                // Check for duplicates
                if (this.attributes.containsKey(attr.getName())) {
                    throw new IllegalArgumentException(
                            String.format("Cannot create attribute set with duplicates (attr: '%s')", attr.getName()));
                }
                this.attributes.put(attr.getName(), attr);
            }
        }
    }

    public Attribute get(String attrName) {
        return attributes.get(attrName);
    }

    @SuppressWarnings("unchecked")
    public <T extends Attribute> T get(AttributeModel<T> attrName) {
        // In case where cast is unsuccessful should we raise an exception or return null ?
        return (T) attributes.get(attrName.getName());
    }

    public boolean contains(String attrName) {
        return attributes.containsKey(attrName);
    }

    public Collection<Attribute> asCollection() {
        return attributes.values();
    }

    public boolean isEmpty() {
        return attributes.isEmpty();
    }

    @Override
    public Iterator<Attribute> iterator() {
        return attributes.values().iterator();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((attributes == null) ? 0 : attributes.hashCode());
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
        AttributeSet other = (AttributeSet) obj;
        if (attributes == null) {
            if (other.attributes != null)
                return false;
        } else if (!attributes.equals(other.attributes))
            return false;
        return true;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        for (Attribute attr : attributes.values()) {
            builder.append(attr);
            builder.append(",");
        }
        if (builder.length() != 0) {
            builder.deleteCharAt(builder.length() - 1);
        }
        return builder.toString();
    }

    public String toCoreLinkFormat() {
        StringBuilder builder = new StringBuilder();
        for (Attribute attr : attributes.values()) {
            builder.append(";");
            builder.append(attr.toCoreLinkFormat());

        }
        return builder.toString();
    }
}
