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

import java.security.Principal;

import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.SSLSession;

import org.eclipse.leshan.transport.javacoap.identity.TlsTransportContextKeys;

import com.mbed.coap.packet.Opaque;
import com.mbed.coap.transport.TransportContext;

import io.netty.channel.Channel;
import io.netty.handler.ssl.SslHandler;

public class CoapsTcpTransportResolver extends CoapTcpTransportResolver {

    @Override
    public TransportContext apply(Channel channel) {
        // Get Session
        SslHandler sslHandler = channel.pipeline().get(SslHandler.class);
        if (sslHandler == null) {
            throw new IllegalStateException("Missing SslHandler");
        }
        SSLEngine sslEngine = sslHandler.engine();
        SSLSession sslSession = sslEngine.getSession();
        if (sslSession == null) {
            throw new IllegalStateException("Missing Session");
        }

        // Get Principal
        Principal principal;
        try {
            principal = sslSession.getPeerPrincipal();
        } catch (SSLPeerUnverifiedException e) {
            throw new IllegalStateException("Unable to get Principal", e);
        }
        if (principal == null) {
            throw new IllegalStateException("Missing Principal");
        }

        // Get Cipher Suite
        String cipherSuite = sslSession.getCipherSuite();
        if (cipherSuite == null) {
            throw new IllegalStateException("Missing Cipher Suite");

        }

        return super.apply(channel) //
                .with(TlsTransportContextKeys.TLS_SESSION_ID, new Opaque(sslSession.getId()).toHex()) //
                .with(TlsTransportContextKeys.PRINCIPAL, principal) //
                .with(TlsTransportContextKeys.CIPHER_SUITE, cipherSuite);
    }
}
