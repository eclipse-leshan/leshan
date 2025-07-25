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
package org.eclipse.leshan.transport.javacoap.transport.context;

import java.util.function.BiFunction;

import org.eclipse.leshan.transport.javacoap.transport.context.keys.IpTransportContextKeys;
import org.eclipse.leshan.transport.javacoap.transport.context.keys.TcpTransportContextKeys;
import org.eclipse.leshan.transport.javacoap.transport.context.keys.TlsTransportContextKeys;

import com.mbed.coap.transport.TransportContext;
import com.mbed.coap.transport.TransportContext.Key;

public class DefaultTransportContextMatcher implements BiFunction<TransportContext, TransportContext, Boolean> {

    private final Key<?>[] knownKeys;

    public DefaultTransportContextMatcher() {
        this(IpTransportContextKeys.REMOTE_ADDRESS, //
                TcpTransportContextKeys.CONNECTION_ID, //
                TcpTransportContextKeys.CONNECTION_START_TIMESTAMP, //
                TlsTransportContextKeys.TLS_SESSION_ID, //
                TlsTransportContextKeys.PRINCIPAL, //
                TlsTransportContextKeys.CIPHER_SUITE);
    }

    public DefaultTransportContextMatcher(Key<?>... knownKeys) {
        this.knownKeys = knownKeys;
    }

    @Override
    public Boolean apply(TransportContext packetTransport, TransportContext channelTransport) {
        // TODO we should be able to iterate on all keys ...
        // As we can not workaround is to test all known key
        for (Key<?> key : knownKeys) {

            Object packetValue = packetTransport.get(key);
            if (packetValue != null) {
                Object channelValue = channelTransport.get(key);

                if (!matches(key, packetValue, channelValue))
                    return false;
            }
        }
        return true;
    }

    protected boolean matches(Key<?> key, Object packetValue, Object channelValue) {
        return packetValue.equals(channelValue);
    }
}
