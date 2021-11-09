/*******************************************************************************
 * Copyright (c) 2013-2015 Sierra Wireless and others.
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
 *     Michał Wadowski (Orange) - Improved compliance with rfc6690.
 *******************************************************************************/
package org.eclipse.leshan.core.link;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.leshan.core.link.attributes.AttributeSet;
import org.eclipse.leshan.core.util.Validate;

/**
 * A Link as defined in http://tools.ietf.org/html/rfc6690.
 */
public class Link {

    private final String uriReference;

    private final AttributeSet attributes;

    /**
     * Creates a new Link without attributes.
     * 
     * @param uriReference the object link URL
     */
    public Link(String uriReference) {
        this(uriReference, (Map<String, LinkParamValue>) null);
    }

    /**
     * Creates a new link and with its attributes.
     * 
     * @param uriReference the link URL
     * @param linkParams the object link attributes or <code>null</code> if the link has no attributes
     */
    // TODO remove this contructor
    public Link(String uriReference, Map<String, LinkParamValue> linkParams) {
        Validate.notNull(uriReference);
        this.uriReference = uriReference;
        if (linkParams != null) {
            this.attributes = new AttributeSet(linkParams);
        } else {
            this.attributes = new AttributeSet();
        }
    }

    public Link(String uriReference, AttributeSet attributes) {
        Validate.notNull(uriReference);
        Validate.notNull(attributes);
        this.uriReference = uriReference;
        this.attributes = attributes;
    }

    /**
     * Creates a new link and with its attributes.
     * 
     * @param uriReference the link URL
     * @param linkParams the object link attributes or <code>null</code> if the link has no attributes
     */
    public <T> Link(String uriReference, Map<String, T> linkParams, Class<T> clazz) {
        Validate.notNull(uriReference);
        this.uriReference = uriReference;
        if (linkParams == null || linkParams.isEmpty()) {
            this.attributes = new AttributeSet();
        } else {
            HashMap<String, LinkParamValue> attributesMap = new HashMap<>();
            for (Entry<String, T> attr : linkParams.entrySet()) {
                if (attr.getValue() == null) {
                    attributesMap.put(attr.getKey(), null);
                } else {
                    attributesMap.put(attr.getKey(), new LinkParamValue(attr.getValue().toString()));
                }

            }
            this.attributes = new AttributeSet(attributesMap);
        }
    }

    /**
     * Creates a new link and with its attributes.
     * 
     * @param uriReference the link URL
     * @param linkParams the object link attributes. The format is attributeKey1, attributeValue1, attributeKey2,
     *        attributeValue2. For empty attributes null value should be used.
     */
    public Link(String uriReference, String... linkParams) {
        Validate.notNull(uriReference);
        this.uriReference = uriReference;
        if (linkParams == null || linkParams.length == 0) {
            this.attributes = new AttributeSet();
        } else {
            if (linkParams.length % 2 != 0) {
                throw new IllegalArgumentException("Each attributes key must have a value");
            }

            HashMap<String, LinkParamValue> attributesMap = new HashMap<>();
            for (int i = 0; i < linkParams.length; i = i + 2) {
                String value = linkParams[i + 1];
                attributesMap.put(linkParams[i], value != null ? new LinkParamValue(value) : null);
            }
            this.attributes = new AttributeSet(attributesMap);
        }
    }

    /**
     * Gets the URI-Reference
     *
     * @return String with URI-Reference
     */
    public String getUriReference() {
        return uriReference;
    }

    /**
     * Gets the link attributes
     * 
     * @return an unmodifiable map containing the link attributes
     */
    // TODO remove it
    public Map<String, LinkParamValue> getLinkParams() {
        return attributes.toLinkParams();
    }

    public AttributeSet getAttributes() {
        return attributes;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append('<');
        builder.append(getUriReference());
        builder.append('>');

        Map<String, LinkParamValue> attributes = getLinkParams();
        if (attributes != null && !attributes.isEmpty()) {
            for (Entry<String, LinkParamValue> entry : attributes.entrySet()) {
                builder.append(";");
                builder.append(entry.getKey());
                if (entry.getValue() != null) {
                    builder.append("=");
                    builder.append(entry.getValue());
                }
            }
        }
        return builder.toString();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((attributes == null) ? 0 : attributes.hashCode());
        result = prime * result + ((uriReference == null) ? 0 : uriReference.hashCode());
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
        Link other = (Link) obj;
        if (attributes == null) {
            if (other.attributes != null)
                return false;
        } else if (!attributes.equals(other.attributes))
            return false;
        if (uriReference == null) {
            if (other.uriReference != null)
                return false;
        } else if (!uriReference.equals(other.uriReference))
            return false;
        return true;
    }
}