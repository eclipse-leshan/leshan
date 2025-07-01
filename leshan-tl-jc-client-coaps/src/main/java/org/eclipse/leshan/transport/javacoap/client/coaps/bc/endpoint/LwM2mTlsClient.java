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
import java.net.InetSocketAddress;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.Vector;

import org.bouncycastle.tls.AlertDescription;
import org.bouncycastle.tls.BasicTlsPSKIdentity;
import org.bouncycastle.tls.CertificateType;
import org.bouncycastle.tls.CipherSuite;
import org.bouncycastle.tls.DefaultTlsClient;
import org.bouncycastle.tls.NameType;
import org.bouncycastle.tls.ProtocolVersion;
import org.bouncycastle.tls.ServerName;
import org.bouncycastle.tls.TlsAuthentication;
import org.bouncycastle.tls.TlsFatalAlert;
import org.bouncycastle.tls.TlsPSKIdentity;
import org.bouncycastle.tls.TlsUtils;
import org.bouncycastle.tls.crypto.TlsCrypto;
import org.bouncycastle.tls.crypto.impl.bc.BcTlsCrypto;
import org.eclipse.leshan.client.security.CertificateVerifierFactory;
import org.eclipse.leshan.client.servers.ServerInfo;
import org.eclipse.leshan.core.SecurityMode;
import org.eclipse.leshan.core.security.certificate.verifier.X509CertificateVerifier;
import org.eclipse.leshan.transport.javacoap.client.coaps.bc.endpoint.authentication.RpkAuthentication;
import org.eclipse.leshan.transport.javacoap.client.coaps.bc.endpoint.authentication.X509Authentication;

public class LwM2mTlsClient extends DefaultTlsClient {

    private final CertificateVerifierFactory certificateVerifierFactory = new CertificateVerifierFactory();
    private final ServerInfo serverInfo;
    private final List<Certificate> trustStore;

    public LwM2mTlsClient(TlsCrypto crypto, ServerInfo serverInfo, List<Certificate> trustStore) {
        super(crypto);
        this.serverInfo = serverInfo;
        this.trustStore = trustStore;
    }

    @Override
    public ProtocolVersion[] getProtocolVersions() {
        return ProtocolVersion.DTLSv12.only();
    }

    @Override
    public int getHandshakeTimeoutMillis() {
        // limit global handshake time to 15s
        return 15000;
    }

    @SuppressWarnings({ "rawtypes", "java:S1168" })
    @Override
    protected Vector getSNIServerNames() {
        if (serverInfo.sni != null) {
            Vector<ServerName> serverNames = new Vector<>();
            serverNames.add(new ServerName(NameType.host_name, serverInfo.sni.getBytes()));
            return serverNames;
        }
        return null;
    }

    @SuppressWarnings("java:S1168")
    @Override
    protected short[] getAllowedClientCertificateTypes() {
        switch (serverInfo.secureMode) {
        case RPK:
            return new short[] { CertificateType.RawPublicKey };
        case X509:
            return new short[] { CertificateType.X509 };
        default:
            return null;
        }
    }

    @SuppressWarnings("java:S1168")
    @Override
    protected short[] getAllowedServerCertificateTypes() {
        switch (serverInfo.secureMode) {
        case RPK:
            return new short[] { CertificateType.RawPublicKey };
        case X509:
            return new short[] { CertificateType.X509 };
        default:
            return null;
        }
    }

    @Override
    public TlsAuthentication getAuthentication() throws IOException {
        if (serverInfo.secureMode == SecurityMode.RPK) {
            return new RpkAuthentication((BcTlsCrypto) getCrypto(), context) {

                @Override
                public PrivateKey getClientPrivateKey() {
                    return serverInfo.privateKey;
                }

                @Override
                public PublicKey getClientPublicKey() {
                    return serverInfo.publicKey;
                }

                @Override
                public PublicKey getServerPublicKey() {
                    return serverInfo.serverPublicKey;
                }

            };
        } else if (serverInfo.secureMode == SecurityMode.X509) {
            final X509CertificateVerifier x509CertificateVerifier = certificateVerifierFactory.create(serverInfo,
                    trustStore);
            return new X509Authentication((BcTlsCrypto) getCrypto(), context) {

                @Override
                public PrivateKey getClientPrivateKey() {
                    return serverInfo.privateKey;
                }

                @Override
                public X509Certificate getClientCertificate() {
                    return (X509Certificate) serverInfo.clientCertificate;
                }

                @Override
                public InetSocketAddress getRemotePeerAddress() {
                    return serverInfo.getAddress();
                }

                @Override
                public X509CertificateVerifier getCertificateVerifier() {
                    return x509CertificateVerifier;
                }

            };
        }
        throw new TlsFatalAlert(AlertDescription.internal_error);

    }

    @Override
    public int[] getSupportedCipherSuites() {
        switch (serverInfo.secureMode) {
        case PSK:
            return TlsUtils.getSupportedCipherSuites(getCrypto(), getPskCipherSuites());
        case RPK:
        case X509:
            return TlsUtils.getSupportedCipherSuites(getCrypto(), getEcdheCipherSuites());
        default:
            throw new IllegalStateException(String.format("Unexpected security mode used %s", serverInfo.secureMode));
        }
    }

    public int[] getPskCipherSuites() {
        // maybe more could be added by default ?
        return new int[] { //
                CipherSuite.TLS_PSK_WITH_AES_128_CCM, //
                CipherSuite.TLS_PSK_WITH_AES_256_CCM, //
                CipherSuite.TLS_DHE_PSK_WITH_AES_128_CCM, //
                CipherSuite.TLS_DHE_PSK_WITH_AES_256_CCM, //
                CipherSuite.TLS_PSK_WITH_AES_128_CCM_8, //
                CipherSuite.TLS_PSK_WITH_AES_256_CCM_8, //
                CipherSuite.TLS_PSK_DHE_WITH_AES_128_CCM_8, //
                CipherSuite.TLS_PSK_DHE_WITH_AES_256_CCM_8 };

    }

    public int[] getEcdheCipherSuites() {
        // maybe more could be added by default ?
        return new int[] { //
                CipherSuite.TLS_ECDHE_ECDSA_WITH_AES_128_CCM, //
                CipherSuite.TLS_ECDHE_ECDSA_WITH_AES_256_CCM, //
                CipherSuite.TLS_ECDHE_ECDSA_WITH_AES_128_CCM_8, //
                CipherSuite.TLS_ECDHE_ECDSA_WITH_AES_256_CCM_8 };
    }

    @Override
    public TlsPSKIdentity getPSKIdentity() throws IOException {
        if (serverInfo.secureMode == SecurityMode.PSK)
            return new BasicTlsPSKIdentity(serverInfo.pskId, serverInfo.pskKey);
        return null;
    }
}
