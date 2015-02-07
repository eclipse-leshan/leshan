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

import java.util.concurrent.ScheduledExecutorService;

import org.eclipse.leshan.ObserveSpec;
import org.eclipse.leshan.client.exchange.LwM2mExchange;
import org.eclipse.leshan.client.exchange.ObserveNotifyExchange;
import org.eclipse.leshan.client.response.WriteResponse;

public abstract class LwM2mClientNode {

    protected ObserveSpec observeSpec;
    protected ObserveNotifyExchange observer;

    public LwM2mClientNode() {
        this.observeSpec = new ObserveSpec.Builder().build();
    }

    public abstract void read(LwM2mExchange exchange);

    public void observe(final LwM2mExchange exchange, final ScheduledExecutorService service) {
        observer = new ObserveNotifyExchange(exchange, this, observeSpec, service);
    }

    public void write(LwM2mExchange exchange) {
        exchange.respond(WriteResponse.notAllowed());
    }

    public void writeAttributes(LwM2mExchange exchange, ObserveSpec spec) {
        this.observeSpec = spec;
        exchange.respond(WriteResponse.success());
    }

}
