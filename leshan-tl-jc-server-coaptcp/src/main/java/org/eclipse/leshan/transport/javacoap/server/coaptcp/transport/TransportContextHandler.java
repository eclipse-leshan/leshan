/*******************************************************************************
 * Copyright (c) 2024 Sierra Wireless and others.
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
 *******************************************************************************/
package org.eclipse.leshan.transport.javacoap.server.coaptcp.transport;

import static java.util.Objects.requireNonNull;

import java.util.function.Function;

import com.mbed.coap.transport.TransportContext;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.AttributeKey;

public class TransportContextHandler extends ChannelInboundHandlerAdapter {

    public static final AttributeKey<TransportContext> TRANSPORT_CONTEXT_ATTR = AttributeKey.newInstance("transport");

    private final Function<Channel, TransportContext> contextResolver;

    public TransportContextHandler(Function<Channel, TransportContext> contextResolver) {
        this.contextResolver = requireNonNull(contextResolver);
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        attachContextToChannel(ctx);
        super.channelActive(ctx);
    }

    protected void attachContextToChannel(ChannelHandlerContext ctx) {
        // create context
        TransportContext transportContext = contextResolver.apply(ctx.channel());
        if (transportContext == null) {
            throw new IllegalStateException("transport context must not be null");
        }

        // add it to the channel
        TransportContext oldTransportContext = ctx.channel().attr(TRANSPORT_CONTEXT_ATTR).setIfAbsent(transportContext);
        if (oldTransportContext != null) {
            throw new IllegalStateException(
                    String.format("Can not create new endpoint context %s as %s already exists.", transportContext,
                            oldTransportContext));
        }
    }
}
