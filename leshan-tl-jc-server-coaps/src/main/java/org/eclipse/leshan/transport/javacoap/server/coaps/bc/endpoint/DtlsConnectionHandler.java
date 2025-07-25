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
import java.util.Arrays;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import org.bouncycastle.tls.ContentType;
import org.bouncycastle.tls.DTLSRequest;
import org.bouncycastle.tls.DTLSServerProtocol;
import org.bouncycastle.tls.DTLSTransport;
import org.bouncycastle.tls.DTLSVerifier;
import org.bouncycastle.tls.DatagramSender;
import org.bouncycastle.tls.HandshakeType;
import org.bouncycastle.tls.ProtocolVersion;
import org.bouncycastle.tls.TlsUtils;
import org.eclipse.leshan.transport.javacoap.transport.context.DefaultTransportContextMatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mbed.coap.transport.TransportContext;

public class DtlsConnectionHandler {
    private static final int MTU = 1500;

    private static final Logger LOG = LoggerFactory.getLogger(DtlsConnectionHandler.class);

    private final InetSocketAddress remote;
    private final ExecutorService executor;
    private final ApplicationDataReceiver packetReceiver;
    private final DefaultTransportContextMatcher transportContextMather;
    private final CoapsBouncyCastleTransportResolver transportContextResolver;
    private final LwM2mTlsServerFactory tlsServerFactory;
    private final DatagramSocket socket;

    private final BlockingQueue<byte[]> incommingDatagramPacket = new LinkedBlockingQueue<>();

    private final AtomicBoolean processing = new AtomicBoolean(false);

    private boolean handshakeComplete = false;
    private boolean handshakeInProgress = false;
    private TransportContext transportContext;
    private DTLSTransport dtlsTransport;
    private final DTLSVerifier verifier;

    public DtlsConnectionHandler(InetSocketAddress remote, ExecutorService executor,
            ApplicationDataReceiver packetReceiver, DefaultTransportContextMatcher transportContextMather,
            CoapsBouncyCastleTransportResolver transportContextResolver, LwM2mTlsServerFactory tlsServerFactory,
            DatagramSocket socket) {
        this.remote = remote;
        this.executor = executor;
        this.packetReceiver = packetReceiver;
        this.transportContextMather = transportContextMather;
        this.transportContextResolver = transportContextResolver;
        this.tlsServerFactory = tlsServerFactory;
        this.socket = socket;
        this.verifier = new DTLSVerifier(tlsServerFactory.getCrypto());
    }

    public void datagramPacketReceived(byte[] datagramPacket) {
        incommingDatagramPacket.add(datagramPacket);
        tryProcess();
    }

    private void tryProcess() {
        if (processing.compareAndSet(false, true)) {
            LOG.trace("processing submitted for {} ...", remote);
            executor.submit(this::processLoop);
        } else {
            LOG.trace("prosssing already ongoing for {} ...", remote);
        }
    }

    private void processLoop() {
        LOG.trace("Start processing task for {} ...", remote);
        try {
            int packetLen = 0;
            do {
                try {
                    packetLen = handlePacket();
                } catch (Exception e) {
                    LOG.warn("unexpected issue raised during DTLS Connection job handling", e);
                }
            } while (packetLen != -1);
        } finally {
            processing.set(false);
            LOG.trace("... stopped task for {}", remote);
            if (!incommingDatagramPacket.isEmpty()) {
                tryProcess();
            }
        }
    }

    private int handlePacket() {
        // Look at next packet to handle
        byte[] nextPacket = incommingDatagramPacket.peek();
        if (nextPacket == null) {
            return -1;
        }

        // if this is client hello
        if (isClientHello(nextPacket)) {
            LOG.trace("CLIENT_HELLO received from {}", remote);
            // use HELLO_VERIFY_REQUEST to avoid IP spoofing
            incommingDatagramPacket.poll(); // unqueue client hello
            DTLSRequest verifiedRequest = verifier.verifyRequest(remote.getHostString().getBytes(), nextPacket, 0,
                    nextPacket.length, new DatagramSender() {
                        @Override
                        public void send(byte[] buf, int off, int len) throws IOException {
                            socket.send(new DatagramPacket(buf, off, len, remote));
                        }

                        @Override
                        public int getSendLimit() throws IOException {
                            return MTU;
                        }
                    });
            if (verifiedRequest == null) {
                LOG.trace("CLIENT_HELLO not verified from {}", remote);
                return -1;
            }
            LOG.trace("CLIENT_HELLO verified from {}", remote);

            // If CLIENT HELLO is verified start handshake
            if (!handshakeInProgress) {
                handshakeComplete = false;
                handshakeInProgress = true;
                try {
                    startHandshake(verifiedRequest);
                } finally {
                    handshakeInProgress = false;
                }
            }
        }
        // read encrypted APPLICATION_DATA
        try {
            if (dtlsTransport != null) {
                byte[] buffer = new byte[1500];
                int len = dtlsTransport.receive(buffer, 0, buffer.length, 100);
                if (len > 0) {
                    LOG.trace("Packet ({} bytes) received from {}", len, remote);
                    packetReceiver
                            .packetReceived(new ApplicationData(Arrays.copyOf(buffer, len), remote, transportContext));
                }
                LOG.trace("transport received len {}", len);
                return len;
            }
        } catch (IOException e) {
            LOG.warn("Unexpected IO exception when handling datagram from {}", remote, e);
        }
        LOG.trace("Ignore packet no dtlsTransport for {}", remote);
        incommingDatagramPacket.poll(); // remove ignore packet
        return -1;
    }

    public void startHandshake(DTLSRequest verifiedClientHello) {
        LwM2mTlsServer server = tlsServerFactory.createTlsServer(remote);
        DTLSServerProtocol protocol = new DTLSServerProtocol();
        try {
            LOG.trace("Handshake started for {}", remote);
            dtlsTransport = protocol.accept(server,
                    new QueuedDatagramTransport(MTU, socket, remote, incommingDatagramPacket), verifiedClientHello);
            LOG.trace("data decrypted from {}", remote);
            transportContext = transportContextResolver.getContext(server.getSecurityParametersConnection(), remote);
            LOG.trace("resolve transport context for {}", remote);
            handshakeComplete = true;
            LOG.trace("Handshake complete for {}", remote);
        } catch (IOException e) {
            LOG.warn("Unexpected IO exception when handshaking with {}", remote, e);
        }

    }

    public void queuePacketToSend(ApplicationData appData, CompletableFuture<Boolean> packetSent) {
        // TODO packet should be queued to be send when handshake is complete
        if (!handshakeComplete) {
            LOG.warn("Drop AppData because hanshake is not complete with {}", remote);
            packetSent.complete(false);
            return;
        }

        if (!Boolean.TRUE.equals(transportContextMather.apply(appData.getContext(), transportContext))) {
            LOG.warn("Drop AppData because expected transport context doesn't match current one for peer {}", remote);
            packetSent.completeExceptionally(new IllegalStateException(
                    String.format("connection at %s is not handle by peer with given transport context %s", remote,
                            appData.getContext())));
            return;
        }

        try {
            dtlsTransport.send(appData.getData(), 0, appData.getData().length);
            packetSent.complete(true);
        } catch (IOException e) {
            LOG.warn("Unexpected IO exception when sending to {}", remote, e);
            packetSent.completeExceptionally(e);
        }
    }

    public TransportContext getTransportContext() {
        return transportContext;
    }

    // =================================================== //
    // This code should be available in BouncyCastle
    private static final int RECORD_HEADER_LENGTH = 13;
    private static final int MAX_FRAGMENT_LENGTH = 1 << 14;

    protected boolean isClientHello(byte[] data) {
        if (data.length < RECORD_HEADER_LENGTH) {
            return false;
        }

        short contentType = TlsUtils.readUint8(data, 0);
        if (ContentType.handshake != contentType) {
            return false;
        }

        ProtocolVersion version = TlsUtils.readVersion(data, 1);
        if (!ProtocolVersion.DTLSv10.isEqualOrEarlierVersionOf(version)) {
            return false;
        }

        int epoch = TlsUtils.readUint16(data, 3);
        if (0 != epoch) {
            return false;
        }

        // long sequenceNumber = TlsUtils.readUint48(data, dataOff + 5)

        int length = TlsUtils.readUint16(data, 11);
        if (length < 1 || length > MAX_FRAGMENT_LENGTH) {
            return false;
        }

        if (data.length < RECORD_HEADER_LENGTH + length) {
            return false;
        }

        short msgType = TlsUtils.readUint8(data, RECORD_HEADER_LENGTH);
        return HandshakeType.client_hello == msgType;
    }
    // =================================================== //
}
