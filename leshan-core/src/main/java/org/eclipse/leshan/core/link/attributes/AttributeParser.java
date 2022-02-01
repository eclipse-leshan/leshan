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
 * Parse {@link Attribute} of {@link Link} i
 */
public interface AttributeParser {
    Attribute parse(String name, String attributeValue) throws InvalidAttributeException;

    <T extends Throwable> Attribute consumeAttributeValue(String name, StringParser<T> attributeValue) throws T;

    Attribute createValuelessAttribute(String name) throws InvalidAttributeException;
}
