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
import org.eclipse.leshan.client.response.ExecuteResponse;
import org.eclipse.leshan.client.response.ReadResponse;
import org.eclipse.leshan.client.response.WriteResponse;

public abstract class BaseTypedLwM2mResource<E extends TypedLwM2mExchange<?>> extends LwM2mClientResource {

    protected abstract E createSpecificExchange(final LwM2mExchange exchange);

    @Override
    public final void read(final LwM2mExchange exchange) {
        handleRead(createSpecificExchange(exchange));
    }

    protected void handleRead(final E exchange) {
        exchange.advanced().respond(ReadResponse.notAllowed());
    }

    @Override
    public final void write(final LwM2mExchange exchange) {
        try {
            handleWrite(createSpecificExchange(exchange));
        } catch (final Exception e) {
            exchange.respond(WriteResponse.badRequest());
        }
    }

    protected void handleWrite(final E exchange) {
        exchange.advanced().respond(WriteResponse.notAllowed());
    }

    @Override
    public void execute(final LwM2mExchange exchange) {
        handleExecute(exchange);
    }

    protected void handleExecute(final LwM2mExchange exchange) {
        exchange.respond(ExecuteResponse.notAllowed());
    }

    @Override
    public boolean isReadable() {
        return false;
    }

    @Override
    public final void notifyResourceUpdated() {
        if (observer != null) {
            observer.setObserveSpec(observeSpec);
            read(observer);
        }
    }

}
