package org.eclipse.leshan.transport.javacoap.client.coaps.bc.endpoint;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;

import org.bouncycastle.tls.DTLSServerProtocol;
import org.bouncycastle.tls.DTLSTransport;
import org.bouncycastle.tls.DatagramTransport;
import org.bouncycastle.tls.PSKTlsServer;
import org.bouncycastle.tls.TlsPSKIdentityManager;
import org.bouncycastle.tls.crypto.TlsCrypto;
import org.bouncycastle.tls.crypto.impl.bc.BcTlsCrypto;

public class BouncyCastleDtlsServer {
    private static final int PORT = 4444;
    private static final int BUFFER_SIZE = 1500;

    private final Map<InetSocketAddress, ClientSession> sessions = new ConcurrentHashMap<>();
    private final DatagramSocket socket;
    private final BcTlsCrypto crypto;
    private BiConsumer<InetSocketAddress, byte[]> onAppDataReceived;

    public BouncyCastleDtlsServer() throws SocketException {
        this.socket = new DatagramSocket(PORT);
        this.crypto = new BcTlsCrypto(new SecureRandom());
    }

    public void start() throws IOException {
        byte[] buffer = new byte[BUFFER_SIZE];

        while (true) {
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
            socket.receive(packet);

            InetSocketAddress clientAddress = new InetSocketAddress(packet.getAddress(), packet.getPort());
            ClientSession session = sessions.computeIfAbsent(clientAddress, addr -> new ClientSession(addr));

            session.handleIncomingPacket(Arrays.copyOf(packet.getData(), packet.getLength()));
        }
    }

    public void sendToClient(InetSocketAddress address, byte[] data) {
        ClientSession session = sessions.get(address);
        if (session != null && session.isReady()) {
            session.send(data);
        } else {
            System.out.println("Client session not ready for: " + address);
        }
    }

    public void setOnAppDataReceived(BiConsumer<InetSocketAddress, byte[]> callback) {
        this.onAppDataReceived = callback;
    }

    private class ClientSession {
        private final InetSocketAddress address;
        private final BlockingQueue<byte[]> packetQueue = new LinkedBlockingQueue<>();
        private final ExecutorService executor = Executors.newSingleThreadExecutor();

        private volatile DTLSTransport tlsSession;
        private volatile boolean ready = false;

        ClientSession(InetSocketAddress address) {
            this.address = address;
            executor.submit(this::run);
        }

        void run() {
            try {
                DatagramTransport transport = new QueuedDatagramTransport(socket, address, packetQueue);
                DTLSServerProtocol protocol = new DTLSServerProtocol();
                tlsSession = protocol.accept(new SimplePskTlsServer(crypto), transport);
                ready = true;

                byte[] buf = new byte[BUFFER_SIZE];
                while (true) {
                    int len = tlsSession.readApplicationData(buf, 0, buf.length);
                    if (len > 0) {
                        byte[] received = Arrays.copyOf(buf, len);
                        System.out.println("Received from " + address + ": " + new String(received));
                        if (onAppDataReceived != null) {
                            onAppDataReceived.accept(address, received);
                        }
                        tlsSession.writeApplicationData(received, 0, received.length); // echo
                    }
                }
            } catch (IOException e) {
                System.err.println("Session error with " + address + ": " + e.getMessage());
            }
        }

        void handleIncomingPacket(byte[] data) {
            packetQueue.offer(data);
        }

        boolean isReady() {
            return ready && tlsSession != null;
        }

        void send(byte[] data) {
            try {
                tlsSession.writeApplicationData(data, 0, data.length);
            } catch (IOException e) {
                System.err.println("Failed to send data to " + address + ": " + e.getMessage());
            }
        }
    }

    private static class QueuedDatagramTransport implements DatagramTransport {
        private final DatagramSocket socket;
        private final InetSocketAddress peer;
        private final BlockingQueue<byte[]> queue;

        QueuedDatagramTransport(DatagramSocket socket, InetSocketAddress peer, BlockingQueue<byte[]> queue) {
            this.socket = socket;
            this.peer = peer;
            this.queue = queue;
        }

        @Override
        public int getReceiveLimit() {
            return BUFFER_SIZE;
        }

        @Override
        public int getSendLimit() {
            return BUFFER_SIZE;
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
        }
    }

    private static class SimplePskTlsServer extends PSKTlsServer {
        public SimplePskTlsServer(TlsCrypto crypto) {
            super(crypto, null);
        }

        @Override
        public TlsPSKIdentityManager getPSKIdentityManager() {
            return new TlsPSKIdentityManager() {
                @Override
                public byte[] getHint() {
                    return "psk_hint".getBytes();
                }

                @Override
                public byte[] getPSK(byte[] identity) {
                    String id = new String(identity);
                    if ("client1".equals(id)) {
                        return "secretPSK".getBytes();
                    }
                    return null;
                }
            };
        }
    }

    public static void main(String[] args) throws Exception {
        BouncyCastleDtlsServer server = new BouncyCastleDtlsServer();
        server.setOnAppDataReceived((addr, data) -> {
            System.out.println("Callback - AppData from " + addr + ": " + new String(data));
        });

        new Thread(() -> {
            try {
                server.start();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }
}
