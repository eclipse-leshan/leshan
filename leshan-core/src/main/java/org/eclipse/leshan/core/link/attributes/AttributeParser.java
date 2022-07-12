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

import org.eclipse.leshan.core.link.Link;
import org.eclipse.leshan.core.parser.StringParser;

/**
 * Parse {@link Attribute} of {@link Link}.
 */
public interface AttributeParser {

    /**
     * Parse the value of an {@link Attribute} in a CoRE link Format.
     */
    Attribute parseCoreLinkValue(String attributeName, String attributeValue) throws InvalidAttributeException;

    /**
     * Consume an attribute value in a CoRE Link Format.
     */
    <T extends Throwable> Attribute consumeAttributeValue(String attributeName, StringParser<T> parser) throws T;

    /**
     * Create an Empty Attribute. It could raise an {@link InvalidAttributeException} if this attribute must have a
     * value.
     */
    Attribute createEmptyAttribute(String attributeName) throws InvalidAttributeException;
}
