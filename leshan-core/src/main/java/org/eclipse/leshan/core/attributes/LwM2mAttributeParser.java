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
package org.eclipse.leshan.core.attributes;

import java.util.Collection;

import org.eclipse.leshan.core.link.attributes.AttributeParser;
import org.eclipse.leshan.core.link.attributes.InvalidAttributeException;

public interface LwM2mAttributeParser extends AttributeParser {

    /**
     * Create an AttributeSet from a uri queries string.
     * 
     * @param uriQueries the URI queries to parse. e.g. {@literal pmin=10&pmax=60}
     */
    Collection<LwM2mAttribute<?>> parseUriQueries(String uriQueries) throws InvalidAttributeException;

    /**
     * Create an AttributeSet from an array of string. Each elements is an attribute with its value.
     * 
     * <pre>
     * queryParams[0] = "pmin=10";
     * queryParams[1] = "pmax=10";
     * </pre>
     */
    Collection<LwM2mAttribute<?>> parseQueryParams(String... queryParams) throws InvalidAttributeException;;

    /**
     * Create an AttributeSet from a collection of string. Each elements is an attribute with its value.
     * 
     * <pre>
     * queryParams.get(0) = "pmin=10";
     * queryParams.get(1) = "pmax=10";
     * </pre>
     */
    Collection<LwM2mAttribute<?>> parseQueryParams(Collection<String> queryParams) throws InvalidAttributeException;;
}
