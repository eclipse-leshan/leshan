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
package org.eclipse.leshan.client.resource;

import org.eclipse.leshan.client.exchange.LwM2mExchange;
import org.eclipse.leshan.client.response.ReadResponse;
import org.eclipse.leshan.client.response.WriteResponse;

public abstract class TypedLwM2mExchange<T> {

    private final LwM2mExchange exchange;

    public TypedLwM2mExchange(final LwM2mExchange exchange) {
        this.exchange = exchange;
    }

    public final LwM2mExchange advanced() {
        return exchange;
    }

    public final void respondSuccess() {
        exchange.respond(WriteResponse.success());
    }

    public final void respondFailure() {
        exchange.respond(WriteResponse.failure());
    }

    public final T getRequestPayload() {
        final byte[] requestPayload = exchange.getRequestPayload();
        return convertFromBytes(requestPayload);
    }

    public void respondContent(final T value) {
        exchange.respond(ReadResponse.success(convertToBytes(value)));
    }

    protected abstract T convertFromBytes(final byte[] value);

    protected abstract byte[] convertToBytes(final T value);

}
