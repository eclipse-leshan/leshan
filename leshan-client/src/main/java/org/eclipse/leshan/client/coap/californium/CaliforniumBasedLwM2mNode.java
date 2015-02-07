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

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import org.eclipse.californium.core.CoapResource;
import org.eclipse.californium.core.coap.CoAP.ResponseCode;
import org.eclipse.californium.core.coap.MediaTypeRegistry;
import org.eclipse.californium.core.server.resources.CoapExchange;
import org.eclipse.leshan.ObserveSpec;
import org.eclipse.leshan.client.exchange.LwM2mExchange;
import org.eclipse.leshan.client.resource.LinkFormattable;
import org.eclipse.leshan.client.resource.LwM2mClientNode;

public abstract class CaliforniumBasedLwM2mNode<T extends LwM2mClientNode> extends CoapResource implements
        LinkFormattable {

    private static final ScheduledExecutorService service = Executors.newSingleThreadScheduledExecutor();
    protected T node;

    public CaliforniumBasedLwM2mNode(int id, T node) {
        super(Integer.toString(id));
        setObservable(true);
        this.node = node;
    }
    
    public T getLwM2mClientObject() {
        return node;
    }

    @Override
    public void handleGET(final CoapExchange coapExchange) {
        if (coapExchange.getRequestOptions().getAccept() == MediaTypeRegistry.APPLICATION_LINK_FORMAT) {
            handleDiscover(coapExchange);
        } else {
            LwM2mExchange exchange = new CaliforniumBasedLwM2mExchange(coapExchange);
            if (exchange.isObserve()) {
                node.observe(exchange, service);
            }
            node.read(exchange);
        }
    }

    @Override
    public void handlePUT(final CoapExchange coapExchange) {
        LwM2mExchange exchange = new CaliforniumBasedLwM2mExchange(coapExchange);
        final ObserveSpec spec = exchange.getObserveSpec();
        if (spec != null) {
            node.writeAttributes(exchange, spec);
        } else {
            node.write(exchange);
        }
    }

    protected void handleDiscover(final CoapExchange exchange) {
        exchange.respond(ResponseCode.CONTENT, asLinkFormat(), MediaTypeRegistry.APPLICATION_LINK_FORMAT);
    }

}
