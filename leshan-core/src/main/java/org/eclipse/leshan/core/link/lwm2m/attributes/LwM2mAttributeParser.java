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
package org.eclipse.leshan.core.link.lwm2m.attributes;

import java.util.Collection;

import org.eclipse.leshan.core.link.attributes.Attribute;
import org.eclipse.leshan.core.link.attributes.AttributeParser;
import org.eclipse.leshan.core.link.attributes.InvalidAttributeException;

/**
 * an {@link AttributeParser} with some LWM2M flavor.
 * <p>
 * It can parse {@link LwM2mAttribute} from CoRE link format but also from URI query.
 */
public interface LwM2mAttributeParser extends AttributeParser {

    /**
     * Create a collection of {@link LwM2mAttribute} from a uri query string.
     *
     * @param uriQueries the URI queries to parse. e.g. {@literal pmin=10&pmax=60}
     */
    Collection<LwM2mAttribute<?>> parseUriQuery(String uriQueries) throws InvalidAttributeException;

    /**
     * Create a list of {@link LwM2mAttribute} from an array of string. Each elements is an attribute with its value.
     *
     * <pre>
     * queryParams[0] = "pmin=10";
     * queryParams[1] = "pmax=10";
     * </pre>
     */
    Collection<LwM2mAttribute<?>> parseQueryParams(String... queryParams) throws InvalidAttributeException;

    /**
     * Create a collection of {@link LwM2mAttribute} from a collection of string. Each elements is an attribute with its
     * value.
     *
     * <pre>
     * queryParams.get(0) = "pmin=10";
     * queryParams.get(1) = "pmax=10";
     * </pre>
     */
    Collection<LwM2mAttribute<?>> parseQueryParams(Collection<String> queryParams) throws InvalidAttributeException;

    /**
     * Parse the value of an {@link LwM2mAttribute} in a query param format.
     */
    Attribute parseQueryParamValue(String attributeName, String attributeValue) throws InvalidAttributeException;
}
