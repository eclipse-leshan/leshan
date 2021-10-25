/*******************************************************************************
 * Copyright (c) 2021 Orange.
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
 *     Orange - Make LinkParser extensible.
 *******************************************************************************/
package org.eclipse.leshan.core.link;

/**
 * Generic parser interface grouping common behavior for parsing different CoRE Link elements with specific type.
 */
public interface Parser<T> {

    /**
     * Check if content is valid. Returns true if content is valid else returns false. Should not throw any exception
     * here.
     */
    boolean isValid(String content);

    /**
     * If content has some validation errors should return user-friendly message else returns null.
     */
    String getValidationErrorMessage(String content);

    /**
     * Parses the content and returns object with type {@link T}. If content is invalid should throw
     * {@link LinkParseException} exception.
     *
     * @param content content to parse.
     * @return parsed object with specific type {@link T}
     * @throws LinkParseException if any validation errors.
     */
    T parse(String content) throws LinkParseException;
}
