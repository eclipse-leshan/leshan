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

import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.function.Predicate;

import org.eclipse.leshan.core.request.exception.UnconnectedPeerException;
import org.eclipse.leshan.transport.javacoap.transport.context.DefaultTransportContextMatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DtlsConnectionsHandler {

    private static final Logger LOG = LoggerFactory.getLogger(DtlsConnectionsHandler.class);

    private final LwM2mTlsServerFactory tlsServerFactory;
    private final ExecutorService dtlsConnectionsHandlerWorkers;
    private final DatagramSocket socket;

    private final ApplicationDataReceiver applicationDataReceiver;
    private final DefaultTransportContextMatcher transportContextMather = new DefaultTransportContextMatcher();
    private final CoapsBouncyCastleTransportResolver transportContextResolver = new CoapsBouncyCastleTransportResolver();

    private final ConcurrentMap<InetSocketAddress, DtlsConnectionHandler> peers = new ConcurrentHashMap<>();
    private final ConcurrentLinkedQueue<CompletableFuture<ApplicationData>> consummers = new ConcurrentLinkedQueue<>();

    public DtlsConnectionsHandler(LwM2mTlsServerFactory tlsServerFactory, ExecutorService dtlsConnectionsHandlerWorkers,
            DatagramSocket socket) {
        this.tlsServerFactory = tlsServerFactory;
        this.dtlsConnectionsHandlerWorkers = dtlsConnectionsHandlerWorkers;
        this.socket = socket;
        this.applicationDataReceiver = appData -> {
            CompletableFuture<ApplicationData> consumer = consummers.poll();
            if (consumer != null) {
                consumer.complete(appData);
            } else {
                LOG.warn("Packet recevied from {} dropped because there is no consumer for it", appData.getAddr());
            }
        };
    }

    public void datagramPacketReceived(InetSocketAddress peer, byte[] data) {
        DtlsConnectionHandler connectionHandler = peers.computeIfAbsent(peer, c -> {
            LOG.trace("New Connection Handler for {}", peer);
            return new DtlsConnectionHandler(peer, dtlsConnectionsHandlerWorkers, applicationDataReceiver,
                    transportContextMather, transportContextResolver, tlsServerFactory, socket);
        });

        connectionHandler.datagramPacketReceived(data);
    }

    public void queuePacketConsumer(CompletableFuture<ApplicationData> consummer) {
        consummers.add(consummer);
    }

    public CompletableFuture<Boolean> queueApplicationDataToSend(ApplicationData appData) {
        CompletableFuture<Boolean> packetSent = new CompletableFuture<>();
        DtlsConnectionHandler connectionHandler = peers.get(appData.getAddr());
        if (connectionHandler == null) {
            LOG.warn("No connection for peer {} : unable to send packet ", appData.getAddr());
            packetSent.completeExceptionally(new UnconnectedPeerException("no connection for %s", appData.getAddr()));
            return packetSent;
        } else {
            connectionHandler.queuePacketToSend(appData, packetSent);
            return packetSent;
        }
    }

    public void removeConnection(Predicate<? super Entry<InetSocketAddress, DtlsConnectionHandler>> filter) {
        for (Map.Entry<InetSocketAddress, DtlsConnectionHandler> entry : peers.entrySet()) {
            InetSocketAddress key = entry.getKey();
            DtlsConnectionHandler value = entry.getValue();
            if (filter.test(entry) && peers.remove(key, value)) {
                return;
            }
        }
    }

    public void removeAllConnection() {
        peers.clear();
    }
}
