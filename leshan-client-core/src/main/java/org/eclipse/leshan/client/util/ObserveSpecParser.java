/*******************************************************************************
 * Copyright (c) 2013-2015 Sierra Wireless and others.
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v1.0 which accompany this distribution.
 * 
 * The Eclipse Public License is available at
 *    http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 *    http://www.eclipse.org/org/documents/edl-v10.html.
 * 
 * Contributors:
 *     Zebra Technologies - initial API and implementation
 *******************************************************************************/
package org.eclipse.leshan.client.util;

import java.util.Arrays;
import java.util.List;

import org.eclipse.leshan.ObserveSpec;
import org.eclipse.leshan.ObserveSpec.Builder;

public class ObserveSpecParser {

    private static final String CANCEL = "cancel";

    private static final String GREATER_THAN = "gt";
    private static final String LESS_THAN = "lt";
    private static final String MAX_PERIOD = "pmax";
    private static final String MIN_PERIOD = "pmin";
    private static final String STEP = "st";

    public static ObserveSpec parse(final List<String> uriQueries) {
        ObserveSpec.Builder builder = new ObserveSpec.Builder();
        if (uriQueries.equals(Arrays.asList(CANCEL))) {
            return builder.cancel().build();
        }
        for (final String query : uriQueries) {
            builder = process(builder, query);
        }
        return builder.build();
    }

    private static Builder process(final ObserveSpec.Builder bob, final String query) {
        final String[] split = query.split("=");
        if (split.length != 2) {
            throw new IllegalArgumentException();
        }

        final String key = split[0];
        final String value = split[1];

        switch (key) {
        case GREATER_THAN:
            return bob.greaterThan(Float.parseFloat(value));
        case LESS_THAN:
            return bob.lessThan(Float.parseFloat(value));
        case STEP:
            return bob.step(Float.parseFloat(value));
        case MIN_PERIOD:
            return bob.minPeriod(Integer.parseInt(value));
        case MAX_PERIOD:
            return bob.maxPeriod(Integer.parseInt(value));
        default:
            throw new IllegalArgumentException();
        }
    }

}
