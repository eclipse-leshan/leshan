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
package org.eclipse.leshan.client.coap.californium;

import org.eclipse.californium.core.server.resources.CoapExchange;
import org.eclipse.leshan.client.exchange.LwM2mCallbackExchange;
import org.eclipse.leshan.client.resource.LwM2mClientNode;
import org.eclipse.leshan.client.response.LwM2mResponse;

public class CaliforniumBasedLwM2mCallbackExchange<T extends LwM2mClientNode> extends CaliforniumBasedLwM2mExchange
        implements LwM2mCallbackExchange<T> {

    private final Callback<T> callback;
    private T node;

    public CaliforniumBasedLwM2mCallbackExchange(final CoapExchange exchange, final Callback<T> callback) {
        super(exchange);
        this.callback = callback;
    }

    @Override
    public void respond(final LwM2mResponse response) {
        if (response.isSuccess()) {
            callback.onSuccess(node);
        } else {
            callback.onFailure();
        }
        super.respond(response);
    }

    @Override
    public void setNode(final T node) {
        this.node = node;
    }

}
