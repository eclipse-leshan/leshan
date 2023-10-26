/*******************************************************************************
 * Copyright (c) 2023 Sierra Wireless and others.
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

import java.util.function.BiFunction;

import com.mbed.coap.packet.CoapPacket;
import com.mbed.coap.packet.CoapTcpPacketSerializer;
import com.mbed.coap.transport.TransportContext;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

public class CoapTcpEncoder extends MessageToByteEncoder<CoapPacket> {

    private final BiFunction<TransportContext, TransportContext, Boolean> contextMatcher;

    public CoapTcpEncoder(BiFunction<TransportContext, TransportContext, Boolean> contextMatcher) {
        this.contextMatcher = contextMatcher;
    }

    @Override
    protected void encode(ChannelHandlerContext ctx, CoapPacket msg, ByteBuf out) throws Exception {
        // Get attached transport context
        TransportContext transportContext = ctx.channel().attr(TransportContextHandler.TRANSPORT_CONTEXT_ATTR).get();
        if (transportContext == null)
            throw new IllegalStateException("transport context should not be null");

        // Check if "destination transport context" packet matches transport context of current channel / "connection".
        // TODO java-coap doesn't set context on response.
        if (msg.getMethod() != null) { // do msg is a request
            if (!contextMatcher.apply(msg.getTransportContext(), transportContext)) {
                throw new UnconnectedPeerException(
                        String.format("transport context expected doesn't match current one at %s",
                                msg.getRemoteAddress().getHostString()));
            }
        }

        byte[] bytes = CoapTcpPacketSerializer.serialize(msg);
        out.writeBytes(bytes);
    }
}
