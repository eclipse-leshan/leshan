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
import java.util.Objects;

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
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof Link))
            return false;
        Link that = (Link) o;
        return that.canEqual(this) && Objects.equals(uriReference, that.uriReference)
                && Objects.equals(attributes, that.attributes);
    }

    public boolean canEqual(Object o) {
        return (o instanceof Link);
    }

    @Override
    public int hashCode() {
        return Objects.hash(uriReference, attributes);
    }
}
