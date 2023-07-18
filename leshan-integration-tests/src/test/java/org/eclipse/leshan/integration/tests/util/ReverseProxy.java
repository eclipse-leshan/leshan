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
package org.eclipse.leshan.integration.tests.util;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility to test client/server use case in Dynamic IP environment (E.g. NAT)
 * <p>
 * It supports only 1 client and server.
 *
 * <pre>
 * ┌──────┐
 * │client│
 * └─┬─▲──┘
 *   │ │  clientAddress
 *   │ │
 *   │ │  clientSideProxyAddress
 * ┌─▼─┴──┐
 * │Proxy │
 * └─┬─▲──┘
 *   │ │  serverSideProxyAddress
 *   │ │
 *   │ │  serverAddress
 * ┌─▼─┴──┐
 * │Server│
 * └──────┘
 * </pre>
 */
public class ReverseProxy {

    private static final Logger LOGGER = LoggerFactory.getLogger(ReverseProxy.class);

    private static final int BUFFER_SIZE = 2048;

    private final InetSocketAddress clientSideProxyAddress;
    private final InetSocketAddress serverAddress;
    private InetSocketAddress clientAddress;

    private DatagramChannel clientToProxyChannel;
    private DatagramChannel proxyToServerChannel;
    private Selector selector;

    private final ExecutorService executor = Executors.newFixedThreadPool(1);
    private volatile boolean running = false; // true if reserver proxy is running
    private volatile boolean stop = false; // true if we asked to stop reserve proxy
    private volatile boolean changeServerSideProxyAddress = false; // true if we ask to change server side proxy address

    public ReverseProxy(InetSocketAddress clientSideProxyAddress, InetSocketAddress serverAddress) {
        this.clientSideProxyAddress = clientSideProxyAddress;
        this.serverAddress = serverAddress;
    }

    public void start() {
        executor.execute(() -> {
            try {
                LOGGER.trace("Starting Reverse Proxy");

                selector = Selector.open();

                clientToProxyChannel = DatagramChannel.open();
                clientToProxyChannel.bind(clientSideProxyAddress);
                clientToProxyChannel.configureBlocking(false);
                clientToProxyChannel.register(selector, SelectionKey.OP_READ);

                proxyToServerChannel = DatagramChannel.open();
                proxyToServerChannel.configureBlocking(false);
                proxyToServerChannel.register(selector, SelectionKey.OP_READ);

                running = true;

                LOGGER.debug("Reverse Proxy Started");
                while (!stop) {
                    selector.select();
                    // Handle events if any
                    Set<SelectionKey> selecteds = selector.selectedKeys();
                    for (SelectionKey selected : selecteds) {
                        if (selected.channel() == clientToProxyChannel) {
                            handleClientPackets();
                        } else if (selected.channel() == proxyToServerChannel) {
                            handlerServerPackets();
                        } else {
                            logAndRaiseException("Unexpected selected channel");
                        }
                    }
                    // Reassign address if needed
                    if (changeServerSideProxyAddress) {
                        reassignServerSideProxyAddress();
                    }
                }
                // proxy is stopped
                LOGGER.trace("Stopping Reverse Proxy");
                clientToProxyChannel.close();
                proxyToServerChannel.close();
                running = false;
                LOGGER.debug("Reverse Proxy stopped");

            } catch (IOException e) {
                logAndRaiseException("Unexpected IO Exception when running proxy", e);
            }
        });

        // Wait until proxy is really running
        for (int i = 0; i < 10 && !running; i++) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                LOGGER.debug("Start was interrupted", e);
            }
        }
        if (!running) {
            stop = true;
            executor.shutdownNow();
            LOGGER.error("Unable to start ReverseProxy");
            throw new IllegalStateException("Unable to start ReserveProxy");
        }
    }

    private void handleClientPackets() throws IOException {
        LOGGER.trace("Handling Packet received from Client");

        // Get received Data
        ByteBuffer buffer = ByteBuffer.allocate(BUFFER_SIZE);
        InetSocketAddress sourceAddress = (InetSocketAddress) clientToProxyChannel.receive(buffer);
        if (sourceAddress == null) {
            return;
        } else {
            // maybe better to store this on connect event ?
            clientAddress = sourceAddress;
        }

        // Transfer Data
        buffer.flip();
        proxyToServerChannel.send(buffer, serverAddress);
        buffer.clear();
        LOGGER.trace("{} bytes transfer to Server", buffer.remaining(), serverAddress);
    }

    private void handlerServerPackets() throws IOException {
        LOGGER.trace("Handling Packet received from Server");

        // Get received Data
        ByteBuffer buffer = ByteBuffer.allocate(BUFFER_SIZE);
        InetSocketAddress sourceAddress;
        sourceAddress = (InetSocketAddress) proxyToServerChannel.receive(buffer);
        if (sourceAddress == null) {
            return;
        }
        if (!sourceAddress.equals(serverAddress)) {
            logAndRaiseException(String.format("We should only receive data from server %s", serverAddress));
        }
        if (clientAddress == null) {
            logAndRaiseException("Client should send data first before sever send data");
        }

        // Transfer Data
        buffer.flip();
        clientToProxyChannel.send(buffer, clientAddress);
        buffer.clear();
        LOGGER.debug("{} bytes transfered to Client", buffer.remaining(), clientAddress);
    }

    private void reassignServerSideProxyAddress() {
        LOGGER.trace("Changing Server Side Proxy Address");
        DatagramChannel previousChannel = proxyToServerChannel;
        try {
            proxyToServerChannel = DatagramChannel.open();
            proxyToServerChannel.configureBlocking(false);
            proxyToServerChannel.register(selector, SelectionKey.OP_READ);
            proxyToServerChannel.connect(serverAddress);
            selector.wakeup();
            LOGGER.debug("Server Side Proxy Address Changed from {} to {}", previousChannel.getLocalAddress(),
                    proxyToServerChannel.getLocalAddress());
        } catch (IOException e) {
            logAndRaiseException("Unable to create new channel when trying to get change Server Side Proxy Address", e);
        } finally {
            changeServerSideProxyAddress = false;
            try {
                previousChannel.close();
            } catch (IOException e) {
                logAndRaiseException(
                        "Unable to close previous channel when trying to get change Server Side Proxy Address", e);
            }
        }
    }

    private void logAndRaiseException(String errorMessage) {
        LOGGER.error(errorMessage);
        throw new IllegalStateException(errorMessage);
    }

    private void logAndRaiseException(String errorMessage, Exception exp) {
        LOGGER.error(errorMessage, exp);
        throw new IllegalStateException(errorMessage, exp);
    }

    /**
     * @return effective public address which should be exposed to client.
     */
    public InetSocketAddress getClientSideProxyAddress() {
        try {
            return (InetSocketAddress) clientToProxyChannel.getLocalAddress();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * @return address of server to proxified
     */
    public InetSocketAddress getServerAddress() {
        try {
            return (InetSocketAddress) clientToProxyChannel.getLocalAddress();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * @return will assign a new server side proxy address which can be used to simulate client address change.
     */
    public void changeServerSideProxyAddress() {
        changeServerSideProxyAddress = true;
        selector.wakeup();
        // Wait address effectively changed (we wait 10x100 ms max)
        for (int i = 0; i < 10 && changeServerSideProxyAddress; i++) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                LOGGER.debug("Change Server Side Proxy Address was interrupted", e);
            }
        }
    }

    public void stop() {
        stop = true;
        executor.shutdown();
    }
}
