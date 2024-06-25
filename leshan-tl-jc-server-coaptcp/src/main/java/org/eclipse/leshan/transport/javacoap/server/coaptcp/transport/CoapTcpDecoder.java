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

import java.net.InetSocketAddress;
import java.util.List;

import com.mbed.coap.packet.CoapPacket;
import com.mbed.coap.packet.CoapTcpPacketSerializer;
import com.mbed.coap.transport.TransportContext;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ReplayingDecoder;

public class CoapTcpDecoder extends ReplayingDecoder<CoapPacket> {

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {

        // Maybe we should use CHECKPOINT to improve performance :
        // see : https://docs.jboss.org/netty/3.2/api/org/jboss/netty/handler/codec/replay/ReplayingDecoder.html
        CoapPacket coap = CoapTcpPacketSerializer.deserialize((InetSocketAddress) ctx.channel().remoteAddress(),
                new ByteBufInputStream(in));

        // Attach transport context to packet
        TransportContext transportContext = ctx.channel().attr(TransportContextHandler.TRANSPORT_CONTEXT_ATTR).get();
        if (transportContext == null)
            throw new IllegalStateException("transport context should not be null");
        coap.setTransportContext(transportContext);

        // Push decoded packet
        out.add(coap);
    }
}
