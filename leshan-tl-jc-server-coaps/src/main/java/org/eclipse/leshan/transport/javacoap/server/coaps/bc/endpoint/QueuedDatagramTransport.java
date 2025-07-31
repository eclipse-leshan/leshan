/*******************************************************************************
 * Copyright (c) 2025 Sierra Wireless and others.
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
package org.eclipse.leshan.transport.javacoap.server.coaps.bc.endpoint;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import org.bouncycastle.tls.DatagramTransport;

/**
 * This is implementation of {@link DatagramTransport} to send/receive data from/to 1 particular peer
 */
public class QueuedDatagramTransport implements DatagramTransport {
    private final DatagramSocket socket;
    private final int bufferSize;
    private final InetSocketAddress peer;
    private final BlockingQueue<byte[]> queue;

    QueuedDatagramTransport(int bufferSize, DatagramSocket socket, InetSocketAddress peer,
            BlockingQueue<byte[]> queue) {
        this.bufferSize = bufferSize;
        this.socket = socket;
        this.peer = peer;
        this.queue = queue;
    }

    @Override
    public int getReceiveLimit() {
        return bufferSize;
    }

    @Override
    public int getSendLimit() {
        return getReceiveLimit();
    }

    @Override
    public int receive(byte[] buf, int off, int len, int waitMillis) throws IOException {
        try {
            byte[] data = queue.poll(waitMillis, TimeUnit.MILLISECONDS);
            if (data == null)
                return -1;
            int copyLen = Math.min(len, data.length);
            System.arraycopy(data, 0, buf, off, copyLen);
            return copyLen;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return -1;
        }
    }

    @Override
    public void send(byte[] buf, int off, int len) throws IOException {
        socket.send(new DatagramPacket(buf, off, len, peer));
    }

    @Override
    public void close() throws IOException {
        // nothing to close socket is shared.
    }
}
