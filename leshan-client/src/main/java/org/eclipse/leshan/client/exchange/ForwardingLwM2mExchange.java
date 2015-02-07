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
package org.eclipse.leshan.client.exchange;

import org.eclipse.leshan.ObserveSpec;
import org.eclipse.leshan.client.response.LwM2mResponse;

public class ForwardingLwM2mExchange implements LwM2mExchange {

    protected final LwM2mExchange exchange;

    public ForwardingLwM2mExchange(final LwM2mExchange exchange) {
        this.exchange = exchange;
    }

    @Override
    public void respond(final LwM2mResponse response) {
        exchange.respond(response);
    }

    @Override
    public byte[] getRequestPayload() {
        return exchange.getRequestPayload();
    }

    @Override
    public boolean hasObjectInstanceId() {
        return exchange.hasObjectInstanceId();
    }

    @Override
    public int getObjectInstanceId() {
        return exchange.getObjectInstanceId();
    }

    @Override
    public boolean isObserve() {
        return exchange.isObserve();
    }

    @Override
    public ObserveSpec getObserveSpec() {
        return exchange.getObserveSpec();
    }

}
