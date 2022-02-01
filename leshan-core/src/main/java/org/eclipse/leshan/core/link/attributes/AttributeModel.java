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
     * Parse an attribute in a CoreLinkFormat.
     */
    public abstract <E extends Throwable> T consumeAttribute(StringParser<E> parser) throws E;

    /**
     * Create an Empty Attribute for this Model
     */
    public abstract T createEmptyAttribute() throws InvalidAttributeException;
}
