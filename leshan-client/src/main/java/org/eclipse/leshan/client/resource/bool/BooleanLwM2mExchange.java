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
package org.eclipse.leshan.client.resource.bool;

import org.eclipse.leshan.client.exchange.LwM2mExchange;
import org.eclipse.leshan.client.resource.TypedLwM2mExchange;

public class BooleanLwM2mExchange extends TypedLwM2mExchange<Boolean> {

    private static String ZERO = Integer.toString(0);
    private static String ONE = Integer.toString(1);

    public BooleanLwM2mExchange(final LwM2mExchange exchange) {
        super(exchange);
    }

    @Override
    protected Boolean convertFromBytes(final byte[] value) {
        final String parsedValue = new String(value);
        if (!parsedValue.equals(ZERO) && !parsedValue.equals(ONE)) {
            throw new IllegalArgumentException();
        }

        return parsedValue.equals(ONE);
    }

    @Override
    protected byte[] convertToBytes(final Boolean value) {
        final int numericalValue = value ? 1 : 0;
        return Integer.toString(numericalValue).getBytes();
    }

}
