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
package org.eclipse.leshan.transport.javacoap.client.coaptcp.endpoint;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.security.cert.X509Certificate;

import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mbed.coap.transport.javassl.SocketClientTransport;

public class SSLSocketClientTransport extends SocketClientTransport {
    private static final Logger LOGGER = LoggerFactory.getLogger(SSLSocketClientTransport.class);

    public SSLSocketClientTransport(InetSocketAddress destination, SSLSocketFactory socketFactory,
            boolean autoReconnect) {
        super(destination, socketFactory, autoReconnect);
    }

    @Override
    protected void connect() throws IOException {
        SSLSocket sslSocket = (SSLSocket) socketFactory.createSocket(destination.getAddress(), destination.getPort());

        sslSocket.addHandshakeCompletedListener(handshakeCompletedEvent -> {
            try {
                LOGGER.debug("Connected [{}, {}]", handshakeCompletedEvent.getSource(),
                        ((X509Certificate) sslSocket.getSession().getPeerCertificates()[0]).getSubjectX500Principal());
            } catch (SSLPeerUnverifiedException e) {
                LOGGER.warn(e.getMessage(), e);
            }
            listener.onConnected((InetSocketAddress) socket.getRemoteSocketAddress());
        });

        this.socket = sslSocket;

        synchronized (this) {
            outputStream = new BufferedOutputStream(socket.getOutputStream());
        }
        inputStream = new BufferedInputStream(socket.getInputStream(), 1024);
    }

    public SSLSocket getSslSocket() {
        return (SSLSocket) socket;
    }
}
