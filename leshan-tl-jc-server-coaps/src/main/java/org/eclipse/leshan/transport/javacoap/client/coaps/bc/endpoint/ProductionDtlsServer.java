package org.eclipse.leshan.transport.javacoap.client.coaps.bc.endpoint;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;

import org.bouncycastle.tls.Certificate;
import org.bouncycastle.tls.DTLSServerProtocol;
import org.bouncycastle.tls.DTLSTransport;
import org.bouncycastle.tls.DatagramTransport;
import org.bouncycastle.tls.PSKTlsServer;
import org.bouncycastle.tls.TlsContext;
import org.bouncycastle.tls.TlsCredentialedSigner;
import org.bouncycastle.tls.TlsPSKIdentityManager;
import org.bouncycastle.tls.TlsServer;
import org.bouncycastle.tls.crypto.TlsCryptoParameters;
import org.bouncycastle.tls.crypto.impl.bc.BcTlsCrypto;

public class ProductionDtlsServer {
    private final DatagramSocket socket;
    private final Map<InetSocketAddress, DtlsClientHandler> clients = new ConcurrentHashMap<>();
    private final ExecutorService handshakeExecutor = Executors.newCachedThreadPool();
    private final SecureRandom secureRandom = new SecureRandom();
    private final BcTlsCrypto crypto = new BcTlsCrypto(secureRandom);

    public ProductionDtlsServer(int port) throws SocketException {
        this.socket = new DatagramSocket(port);
        socket.setSoTimeout(10);
    }

    public void run() throws IOException {
        byte[] buf = new byte[1500];

        while (!Thread.interrupted()) {
            try {
                DatagramPacket packet = new DatagramPacket(buf, buf.length);
                socket.receive(packet);
                InetSocketAddress clientAddr = new InetSocketAddress(packet.getAddress(), packet.getPort());
                clients.computeIfAbsent(clientAddr, addr -> new DtlsClientHandler(addr))
                        .receiveDatagram(Arrays.copyOf(packet.getData(), packet.getLength()));
            } catch (SocketTimeoutException ignored) {
            }

            for (DtlsClientHandler handler : clients.values()) {
                handler.process();
            }
        }
    }

    class DtlsClientHandler {
        private final InetSocketAddress remote;
        private final ByteArrayOutputStream incomingBuffer = new ByteArrayOutputStream();
        private final BlockingQueue<byte[]> outboundQueue = new LinkedBlockingQueue<>();
        private DTLSTransport dtlsTransport;
        private boolean handshakeComplete = false;
        private boolean handshakeStarted = false;

        public DtlsClientHandler(InetSocketAddress remote) {
            this.remote = remote;
        }

        public void receiveDatagram(byte[] data) {
            synchronized (incomingBuffer) {
                try {
                    incomingBuffer.write(data);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        public void process() {
            if (!handshakeComplete && !handshakeStarted) {
                handshakeStarted = true;
                startHandshake();
            } else if (handshakeComplete) {
                try {
                    byte[] appBuf = new byte[1500];
                    int len = dtlsTransport.receive(appBuf, 0, appBuf.length, 100);
                    if (len > 0) {
                        String msg = new String(appBuf, 0, len);
                        System.out.println("Received from " + remote + ": " + msg);
                        dtlsTransport.send(appBuf, 0, len);
                    }
                } catch (IOException ignored) {
                }
            }
        }

        public void startHandshake() {
            handshakeExecutor.submit(() -> {
                try {
                    TlsServer server = new PSKTlsServer(crypto, new TlsPSKIdentityManager() {

                        @Override
                        public byte[] getPSK(byte[] identity) {
                            // TODO Auto-generated method stub
                            return null;
                        }

                        @Override
                        public byte[] getHint() {
                            // TODO Auto-generated method stub
                            return null;
                        }
                    });

                    DatagramTransport transport = new DatagramTransport() {
                        @Override
                        public int getReceiveLimit() {
                            return 1500;
                        }

                        @Override
                        public int getSendLimit() {
                            return 1500;
                        }

                        @Override
                        public int receive(byte[] buf, int off, int len, int timeout) throws IOException {
                            synchronized (incomingBuffer) {
                                byte[] data = incomingBuffer.toByteArray();
                                if (data.length == 0)
                                    throw new SocketTimeoutException();
                                System.arraycopy(data, 0, buf, off, data.length);
                                incomingBuffer.reset();
                                return data.length;
                            }
                        }

                        @Override
                        public void send(byte[] buf, int off, int len) throws IOException {
                            byte[] data = Arrays.copyOfRange(buf, off, off + len);
                            outboundQueue.offer(data);
                            socket.send(new DatagramPacket(data, data.length, remote));
                        }

                        @Override
                        public void close() {
                        }
                    };

                    DTLSServerProtocol protocol = new DTLSServerProtocol();
                    dtlsTransport = protocol.accept(server, transport);
                    handshakeComplete = true;
                    System.out.println("Handshake complete with " + remote);

                } catch (IOException e) {
                    System.err.println("Handshake failed with " + remote);
                    e.printStackTrace();
                    clients.remove(remote);
                }
            });
        }
    }

    public static void main(String[] args) throws Exception {
        new ProductionDtlsServer(4444).run();
    }
}

class CertificateUtils {
    public static Certificate loadCertificateChain(String pemPath) throws IOException {
        throw new UnsupportedOperationException("Implement certificate loading");
    }

    public static TlsCryptoParameters createTlsCryptoParameters(TlsContext context) {
        return new TlsCryptoParameters(context);
    }

    public static TlsCredentialedSigner loadPrivateKey(String keyPath) throws IOException {
        throw new UnsupportedOperationException("Implement key loading");
    }
}
