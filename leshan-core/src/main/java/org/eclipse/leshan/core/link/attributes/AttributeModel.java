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

import org.eclipse.leshan.core.parser.StringParser;

/**
 * An {@link Attribute} model.
 */
public abstract class AttributeModel<T extends Attribute> {

    private String name;

    public AttributeModel(String name) {
        this.name = name;
    }

    /**
     * @return the name of the attribute
     */
    public String getName() {
        return name;
    }

    /**
     * Consume an attribute value in a CoRE Link Format for this model.
     */
    public abstract <E extends Throwable> T consumeAttributeValue(StringParser<E> parser) throws E;

    /**
     * Create an Empty Attribute for this Model.
     *
     * @throws UnsupportedOperationException if this attribute can not be created without value.
     */
    public T createEmptyAttribute() throws UnsupportedOperationException {
        throw new UnsupportedOperationException(String.format("Attribute %s must have a value", getName()));
    }

    /**
     * @return <code>true</code> if the Attribute can be used without value in a CoRE Link Format.
     */
    public boolean linkAttributeCanBeValueless() {
        return false;
    }
}
