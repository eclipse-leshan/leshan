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
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.leshan.core.link.LinkParamValue;

/**
 * A set of {@link Attribute}
 */
public class AttributeSet {

    private final Map<String, Attribute> attributes = new LinkedHashMap<>();

    public AttributeSet(Attribute... attributes) {
        this(Arrays.asList(attributes));
    }

    // TODO constructor to make migration from LinkParamValue to AttributeSet easier
    // we need to remove it
    public AttributeSet(Map<String, LinkParamValue> linkParams) {
        if (linkParams != null && !linkParams.isEmpty()) {
            for (Entry<String, LinkParamValue> attr : linkParams.entrySet()) {
                this.attributes.put(attr.getKey(), new LinkParamValueAttribute(attr.getKey(), attr.getValue()));
            }
        }
    }

    // TODO method to make migration from LinkParamValue to AttributeSet easier
    // we need to remove it
    public Map<String, LinkParamValue> toLinkParams() {
        HashMap<String, LinkParamValue> params = new HashMap<>();
        for (Attribute attr : attributes.values()) {
            if (!attr.hasValue()) {
                params.put(attr.getName(), null);
            } else {
                if (attr instanceof LinkParamValueAttribute) {
                    params.put(attr.getName(), new LinkParamValue((String) attr.getValue()));
                } else {
                    throw new IllegalStateException(
                            attr.getClass().getSimpleName() + " is not supported by AttributeSet");
                }
            }
        }
        return params;
    }

    public AttributeSet(Collection<Attribute> attributes) {
        if (attributes != null && !attributes.isEmpty()) {

            for (Attribute attr : attributes) {
                if (!(attr instanceof LinkParamValueAttribute)) {
                    throw new IllegalArgumentException(
                            attr.getClass().getSimpleName() + " is not supported by AttributeSet");
                }

                // TODO we must check if duplicates are allowed in CoRE Link Format
//                // Check for duplicates
//                if (this.attributes.containsKey(attr.getName())) {
//                    throw new IllegalArgumentException(
//                            String.format("Cannot create attribute set with duplicates (attr: '%s')", attr.getName()));
//                }
                this.attributes.put(attr.getName(), attr);
            }
        }
    }

    public Attribute getAttribute(String attrName) {
        return attributes.get(attrName);
    }

    public Collection<Attribute> getAttributes() {
        return attributes.values();
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
}
