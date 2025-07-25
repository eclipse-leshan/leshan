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
package org.eclipse.leshan.transport.javacoap.client.coaps.bc.endpoint;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;

import org.bouncycastle.tls.DTLSClientProtocol;
import org.bouncycastle.tls.DTLSTransport;
import org.bouncycastle.tls.TlsClient;
import org.bouncycastle.tls.UDPTransport;

import com.mbed.coap.exception.CoapException;
import com.mbed.coap.packet.CoapPacket;
import com.mbed.coap.packet.CoapSerializer;
import com.mbed.coap.transport.CoapTransport;
import com.mbed.coap.utils.ExecutorHelpers;

public class BouncyCastleDtlsTransport implements CoapTransport {

    private final InetSocketAddress destination;
    private final TlsClient client;
    private final ExecutorService readingWorker;
    private final boolean internalReadingWorker;
    private final ExecutorService sendingWorker;
    private final boolean internalSendingWorker;

    // all this field can be accessed by different thread
    private volatile boolean isRunning = false;
    private CompletableFuture<DTLSTransport> whenConnected;
    private DatagramSocket socket;
    private DTLSTransport dtlsTransport;

    public BouncyCastleDtlsTransport(TlsClient client, InetSocketAddress destination, ExecutorService readingWorker,
            ExecutorService sendingWorker) {
        this.destination = destination;
        this.client = client;
        if (readingWorker != null) {
            this.readingWorker = readingWorker;
            this.internalReadingWorker = false;
        } else {
            this.readingWorker = ExecutorHelpers.newSingleThreadExecutor("dtls-reader");
            this.internalReadingWorker = true;
        }
        if (sendingWorker != null) {
            this.sendingWorker = sendingWorker;
            this.internalSendingWorker = false;
        } else {
            this.sendingWorker = ExecutorHelpers.newSingleThreadExecutor("dtls-sender");
            this.internalSendingWorker = false;
        }
    }

    @Override
    public synchronized void start() throws IOException {
        isRunning = true;
        // Create local socket
        socket = new DatagramSocket();
        socket.connect(destination.getAddress(), destination.getPort());
    }

    @Override
    public synchronized InetSocketAddress getLocalSocketAddress() {
        return (InetSocketAddress) socket.getLocalSocketAddress();
    }

    @Override
    public synchronized void stop() {
        if (isRunning) {
            if (dtlsTransport != null)
                try {
                    dtlsTransport.close();
                } catch (IOException e) {
                    throw new IllegalStateException("unable to close dtls transport", e);
                }
            socket.close();
            if (internalReadingWorker)
                readingWorker.shutdown();
            if (internalSendingWorker)
                sendingWorker.shutdown();
        }
        isRunning = false;
    }

    protected synchronized CompletableFuture<DTLSTransport> connectIfNeeded() {

        if (whenConnected == null || whenConnected.isCompletedExceptionally()) {

            // create connecting future
            whenConnected = CompletableFuture.supplyAsync(() -> {

                // connect to foreign peer
                DTLSTransport newTransport;
                try {
                    newTransport = new DTLSClientProtocol().connect(client, new UDPTransport(socket, 1500));
                } catch (IOException e) {
                    throw new CompletionException(String.format("unable to connect to %s", destination), e);
                }

                // store current transport
                synchronized (this) {
                    dtlsTransport = newTransport;
                }
                return newTransport;
            }, readingWorker);
        }
        return whenConnected;
    }

    @Override
    public CompletableFuture<CoapPacket> receive() {
        return connectIfNeeded() //
                .thenApplyAsync(this::blockingReceive, readingWorker) //
                .thenCompose(it -> (it == null) ? receive() : CompletableFuture.completedFuture(it));
    }

    private CoapPacket blockingReceive(DTLSTransport transport) {
        byte[] readBuffer = new byte[1500];
        CoapPacket packet = null;
        try {
            // wait to receive new data
            int receive = transport.receive(readBuffer, 0, readBuffer.length, 100);
            if (receive > 0) {
                packet = CoapSerializer.deserialize(destination, readBuffer, receive);
                LOGGER.trace("Coap packet received {}", packet);
            }
        } catch (CoapException e) {
            LOGGER.warn(e.toString(), e);
        } catch (IOException e) {
            throw new CompletionException(e);
        }
        return packet;
    }

    @Override
    public final CompletableFuture<Boolean> sendPacket(CoapPacket coapPacket) {
        return connectIfNeeded() //
                .thenApplyAsync(transport -> sendData(transport, coapPacket), sendingWorker);
    }

    public synchronized boolean sendData(DTLSTransport transport, CoapPacket coapPacket) {
        try {
            InetSocketAddress adr = coapPacket.getRemoteAddress();
            if (!adr.equals(this.destination)) {
                throw new IllegalStateException("No connection with: " + adr);
            }
            byte[] bytes = CoapSerializer.serialize(coapPacket);
            LOGGER.trace("Try to send coap packet {} ...", coapPacket);
            transport.send(bytes, 0, bytes.length);
            LOGGER.trace("... coap packet {} sent ", coapPacket.getMessageId());
            return true;
        } catch (IOException e) {
            throw new CompletionException(String.format("unable to send data to %s", destination), e);
        }
    }

    public synchronized void forceReconnection() {
        whenConnected = null;
    }
}
