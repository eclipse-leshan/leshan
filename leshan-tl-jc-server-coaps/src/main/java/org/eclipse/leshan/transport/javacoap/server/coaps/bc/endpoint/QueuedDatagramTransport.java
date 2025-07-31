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
import java.net.SocketTimeoutException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import org.bouncycastle.tls.DatagramTransport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is implementation of {@link DatagramTransport} to send/receive data from/to 1 particular peer
 */
public class QueuedDatagramTransport implements DatagramTransport {

    private static final Logger LOG = LoggerFactory.getLogger(QueuedDatagramTransport.class);

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
            LOG.trace("Waiting dequeue data for {} ({} offset, {} length)", peer, off, len);
            byte[] data = queue.poll(waitMillis, TimeUnit.MILLISECONDS);
            if (data == null) {
                LOG.trace("No data to dequeue for {} : timeout", peer);
                throw new SocketTimeoutException();
            }
            if (data.length > len) {
                throw new IllegalStateException(String
                        .format("data received (%d bytes) is too big for given buffer (%d bytes)", data.length, len));
            }
            System.arraycopy(data, 0, buf, off, data.length);
            LOG.trace("Data ({} bytes) dequeue for {}", data.length, peer);
            return data.length;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            LOG.trace("Received method interrupted", e);
            return -1;
        }
    }

    @Override
    public void send(byte[] buf, int off, int len) throws IOException {
        LOG.trace("Send data to {} ({} offset {} length)", peer, off, len);
        socket.send(new DatagramPacket(buf, off, len, peer));
    }

    @Override
    public void close() throws IOException {
        // nothing to close socket is shared.
    }
}
