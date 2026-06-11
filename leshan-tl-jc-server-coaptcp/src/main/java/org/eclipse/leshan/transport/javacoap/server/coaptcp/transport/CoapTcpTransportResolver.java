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

import java.net.InetSocketAddress;
import java.time.Instant;
import java.util.function.Function;

import org.eclipse.leshan.transport.javacoap.transport.context.keys.IpTransportContextKeys;
import org.eclipse.leshan.transport.javacoap.transport.context.keys.TcpTransportContextKeys;

import com.mbed.coap.transport.TransportContext;

import io.netty.channel.Channel;

public class CoapTcpTransportResolver implements Function<Channel, TransportContext> {

    @Override
    public TransportContext apply(Channel channel) {
        return TransportContext.of(IpTransportContextKeys.REMOTE_ADDRESS, (InetSocketAddress) channel.remoteAddress()) //
                .with(TcpTransportContextKeys.CONNECTION_ID, channel.id().asShortText()) //
                .with(TcpTransportContextKeys.CONNECTION_START_TIMESTAMP, Instant.now());
    }
}
