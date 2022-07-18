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

import java.util.Collection;

import org.eclipse.leshan.core.link.attributes.Attribute;
import org.eclipse.leshan.core.link.attributes.AttributeSet;
import org.eclipse.leshan.core.util.Validate;

/**
 * A Link as defined in http://tools.ietf.org/html/rfc6690.
 */
public class Link {

    private final String uriReference;

    private final AttributeSet attributes;

    /**
     * Creates a new link and with its attributes.
     *
     * @param uriReference the link URL
     * @param attributes the object link attributes or <code>null</code> if the link has no attributes
     */
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
     * @param attributes the object link attributes if the link has no attributes
     */
    public Link(String uriReference, Attribute... attributes) {
        this(uriReference, new AttributeSet(attributes));
    }

    /**
     * Creates a new link and with its attributes.
     *
     * @param uriReference the link URL
     * @param attributes the object link attributes if the link has no attributes
     */
    public Link(String uriReference, Collection<Attribute> attributes) {
        this(uriReference, new AttributeSet(attributes));
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
    public AttributeSet getAttributes() {
        return attributes;
    }

    /**
     * @return true if this link has some attributes.
     */
    public boolean hasAttribute() {
        return !attributes.isEmpty();
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append('<');
        builder.append(getUriReference());
        builder.append('>');

        for (Attribute attr : getAttributes()) {
            builder.append(";");
            builder.append(attr.getName());
            if (attr.hasValue()) {
                builder.append("=");
                builder.append(attr.getValue());
            }
        }
        return builder.toString();
    }

    public String toCoreLinkFormat() {
        StringBuilder builder = new StringBuilder();
        builder.append('<');
        builder.append(getUriReference());
        builder.append('>');
        if (hasAttribute()) {
            builder.append(getAttributes().toCoreLinkFormat());
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
