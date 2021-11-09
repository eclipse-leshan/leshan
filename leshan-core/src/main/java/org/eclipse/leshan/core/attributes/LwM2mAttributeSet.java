/*******************************************************************************
 * Copyright (c) 2013-2018 Sierra Wireless and others.
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
 *     Daniel Persson (Husqvarna Group) - Attribute support
 *******************************************************************************/
package org.eclipse.leshan.core.attributes;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

/**
 * An {@link MixedLwM2mAttributeSet} but which allow only {@link LwM2mAttribute}
 */
public class LwM2mAttributeSet extends MixedLwM2mAttributeSet {

    public LwM2mAttributeSet(LwM2mAttribute... attributes) {
        super(attributes);
    }

    public LwM2mAttributeSet(Collection<LwM2mAttribute> attributes) {
        super(attributes);
    }

    /**
     * Create an AttributeSet from a uri queries string.
     * 
     * @param uriQueries the URI queries to parse. e.g. {@literal pmin=10&pmax=60}
     */
    // TODO Must probably be moved somewhere else
    public static LwM2mAttributeSet parse(String uriQueries) {
        if (uriQueries == null)
            return null;

        String[] queriesArray = uriQueries.split("&");
        return LwM2mAttributeSet.parse(queriesArray);
    }

    /**
     * Create an AttributeSet from an array of string. Each elements is an attribute with its value.
     * 
     * <pre>
     * queryParams[0] = "pmin=10";
     * queryParams[1] = "pmax=10";
     * </pre>
     */
    // TODO Must probably be moved somewhere else
    public static LwM2mAttributeSet parse(String... queryParams) {
        return LwM2mAttributeSet.parse(Arrays.asList(queryParams));
    }

    /**
     * Create an AttributeSet from a collection of string. Each elements is an attribute with its value.
     * 
     * <pre>
     * queryParams.get(0) = "pmin=10";
     * queryParams.get(1) = "pmax=10";
     * </pre>
     */
    // TODO Must probably be moved somewhere else
    public static LwM2mAttributeSet parse(Collection<String> queryParams) {
        ArrayList<LwM2mAttribute> attributes = new ArrayList<>();

        for (String param : queryParams) {
            String[] keyAndValue = param.split("=");
            if (keyAndValue.length == 1) {
                attributes.add(new LwM2mAttribute(keyAndValue[0]));
            } else if (keyAndValue.length == 2) {
                attributes.add(new LwM2mAttribute(keyAndValue[0], keyAndValue[1]));
            } else {
                throw new IllegalArgumentException(String.format("Cannot parse query param '%s'", param));
            }

        }
        return new LwM2mAttributeSet(attributes);
    }
}
