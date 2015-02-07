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
package org.eclipse.leshan.client.resource.time;

import java.util.Date;

import org.eclipse.leshan.client.exchange.LwM2mExchange;
import org.eclipse.leshan.client.resource.TypedLwM2mExchange;

public class TimeLwM2mExchange extends TypedLwM2mExchange<Date> {

    public TimeLwM2mExchange(final LwM2mExchange exchange) {
        super(exchange);
    }

    @Override
    protected Date convertFromBytes(final byte[] value) {
        final int secondsSinceEpoch = Integer.parseInt(new String(value));
        final long millisSinceEpoch = secondsSinceEpoch * 1000L;
        return new Date(millisSinceEpoch);
    }

    @Override
    protected byte[] convertToBytes(final Date value) {
        final long secondsSinceEpoch = value.getTime() / 1000;
        return Long.toString(secondsSinceEpoch).getBytes();
    }

}
