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
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.function.Predicate;

import org.bouncycastle.util.Arrays;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mbed.coap.exception.CoapException;
import com.mbed.coap.packet.CoapPacket;
import com.mbed.coap.packet.CoapSerializer;
import com.mbed.coap.transport.CoapTransport;
import com.mbed.coap.utils.ExecutorHelpers;

public class BouncyCastleDtlsTransport implements CoapTransport {

    private static final Logger LOG = LoggerFactory.getLogger(BouncyCastleDtlsTransport.class);

    private final InetSocketAddress localAddress;
    private final LwM2mTlsServerFactory tlsServerFactory;

    private final ExecutorService readingWorker;
    private final boolean internalReadingWorker;
    private final ExecutorService packetHandlerWorker;
    private final boolean internalPacketHandlerWorker;

    // all this field can be accessed by different thread
    private volatile boolean isRunning = false;
    private DatagramSocket socket;
    private DtlsConnectionsHandler connectionsManager;

    public BouncyCastleDtlsTransport(LwM2mTlsServerFactory tlsServerFactory, InetSocketAddress localAddress,
            ExecutorService readingWorker, ExecutorService packetHandlerWorker) {
        this.localAddress = localAddress;
        this.tlsServerFactory = tlsServerFactory;
        if (readingWorker != null) {
            this.readingWorker = readingWorker;
            this.internalReadingWorker = false;
        } else {
            this.readingWorker = ExecutorHelpers.newSingleThreadExecutor("dtls-reader");
            this.internalReadingWorker = true;
        }
        if (packetHandlerWorker != null) {
            this.packetHandlerWorker = packetHandlerWorker;
            this.internalPacketHandlerWorker = false;
        } else {
            this.packetHandlerWorker = ExecutorHelpers.newSingleThreadExecutor("packet-handler");
            this.internalPacketHandlerWorker = false;
        }

    }

    @Override
    public synchronized void start() throws IOException {
        isRunning = true;
        // Create local socket
        socket = new DatagramSocket(localAddress);
        connectionsManager = new DtlsConnectionsHandler(tlsServerFactory, packetHandlerWorker, socket);
        DatagramReadTask readerTask = new DatagramReadTask(socket);
        readingWorker.execute(readerTask);
    }

    public class DatagramReadTask implements Runnable {
        private static final int BUFFER_SIZE = 1500;

        private final DatagramSocket socket;

        public DatagramReadTask(DatagramSocket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            byte[] buffer = new byte[BUFFER_SIZE];
            DatagramPacket datagramPacket = new DatagramPacket(buffer, buffer.length);

            while (isRunning) {
                try {
                    LOG.trace("Wait for datagram on socket ...");
                    socket.receive(datagramPacket);
                    LOG.trace("Datagram ({} bytes) received from {}", datagramPacket.getLength(),
                            datagramPacket.getSocketAddress());
                    connectionsManager.datagramPacketReceived(
                            new InetSocketAddress(datagramPacket.getAddress(), datagramPacket.getPort()),
                            Arrays.copyOf(datagramPacket.getData(), datagramPacket.getLength()));
                } catch (IOException e) {
                    isRunning = false;
                    LOG.warn("DTLS Socket Reader Task interrupted by unexpected IO exception", e);
                }
            }
        }
    }

    @Override
    public synchronized InetSocketAddress getLocalSocketAddress() {
        return (InetSocketAddress) socket.getLocalSocketAddress();
    }

    @Override
    public synchronized void stop() {
        if (isRunning) {
            socket.close();
            if (internalReadingWorker)
                readingWorker.shutdown();
            if (internalPacketHandlerWorker)
                packetHandlerWorker.shutdown();
        }
        isRunning = false;
    }

    @Override
    public CompletableFuture<CoapPacket> receive() {
        CompletableFuture<ApplicationData> applicationDataReceived = new CompletableFuture<>();
        CompletableFuture<CoapPacket> coapPacketReceived = applicationDataReceived.thenApply(p -> {
            try {
                CoapPacket packet = CoapSerializer.deserialize(p.getAddr(), p.getData(), p.getData().length);
                if (packet != null && p.getContext() != null) {
                    packet.setTransportContext(p.getContext());
                }
                return packet;
            } catch (CoapException e) {
                throw new IllegalStateException(
                        String.format("unable to deserialize coap packet from %s", p.getAddr()));
            }
        });
        connectionsManager.queuePacketConsumer(applicationDataReceived);
        return coapPacketReceived;
    }

    @Override
    public final CompletableFuture<Boolean> sendPacket(CoapPacket coapPacket) {
        return connectionsManager.queueApplicationDataToSend(new ApplicationData(CoapSerializer.serialize(coapPacket),
                coapPacket.getRemoteAddress(), coapPacket.getTransportContext()));
    }

    public void removeConnection(Predicate<? super Entry<InetSocketAddress, DtlsConnectionHandler>> filter) {
        connectionsManager.removeConnection(filter);
    }

    public void removeAllConnections() {
        connectionsManager.removeAllConnection();
    }
}
