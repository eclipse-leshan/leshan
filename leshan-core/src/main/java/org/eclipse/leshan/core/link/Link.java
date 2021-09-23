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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.leshan.core.util.Validate;

/**
 * A Link as defined in http://tools.ietf.org/html/rfc6690.
 */
public class Link {

    private final String uriReference;

    private final Map<String, LinkParamValue> linkParams;

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
    public Link(String uriReference, Map<String, LinkParamValue> linkParams) {
        Validate.notNull(uriReference);
        this.uriReference = uriReference;
        if (linkParams != null) {
            this.linkParams = Collections.unmodifiableMap(new HashMap<>(linkParams));
        } else {
            this.linkParams = Collections.emptyMap();
        }
    }

    /**
     * Creates a new link and with its attributes.
     * 
     * @param uriReference the link URL
     * @param linkParams the object link attributes or <code>null</code> if the link has no attributes
     */
    @SuppressWarnings("unchecked")
    public <T> Link(String uriReference, Map<String, T> linkParams, Class<T> clazz) {
        Validate.notNull(uriReference);
        this.uriReference = uriReference;
        if (linkParams == null || linkParams.isEmpty()) {
            this.linkParams = Collections.emptyMap();
        } else {
            if (String.class.equals(clazz)) {
                this.linkParams = Collections.unmodifiableMap((Map<String, LinkParamValue>) new HashMap<>(linkParams));
            } else {
                HashMap<String, LinkParamValue> attributesMap = new HashMap<>();
                for (Entry<String, T> attr : linkParams.entrySet()) {
                    if (attr.getValue() == null) {
                        attributesMap.put(attr.getKey(), null);
                    } else {
                        attributesMap.put(attr.getKey(), new LinkParamValue(attr.getValue().toString()));
                    }

                }
                this.linkParams = Collections.unmodifiableMap(attributesMap);
            }
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
            this.linkParams = Collections.emptyMap();
        } else {
            if (linkParams.length % 2 != 0) {
                throw new IllegalArgumentException("Each attributes key must have a value");
            }

            HashMap<String, LinkParamValue> attributesMap = new HashMap<>();
            for (int i = 0; i < linkParams.length; i = i + 2) {
                String value = linkParams[i + 1];
                attributesMap.put(linkParams[i], value != null ? new LinkParamValue(value) : null);
            }
            this.linkParams = Collections.unmodifiableMap(attributesMap);
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
    public Map<String, LinkParamValue> getLinkParams() {
        return linkParams;
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
        result = prime * result + ((linkParams == null) ? 0 : linkParams.hashCode());
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
        if (linkParams == null) {
            if (other.linkParams != null)
                return false;
        } else if (!linkParams.equals(other.linkParams))
            return false;
        if (uriReference == null) {
            if (other.uriReference != null)
                return false;
        } else if (!uriReference.equals(other.uriReference))
            return false;
        return true;
    }

}