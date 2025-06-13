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
import java.security.GeneralSecurityException;
import java.security.PublicKey;
import java.util.Vector;

import org.bouncycastle.crypto.util.PrivateKeyFactory;
import org.bouncycastle.tls.AlertDescription;
import org.bouncycastle.tls.BasicTlsPSKIdentity;
import org.bouncycastle.tls.Certificate;
import org.bouncycastle.tls.CertificateEntry;
import org.bouncycastle.tls.CertificateRequest;
import org.bouncycastle.tls.CertificateType;
import org.bouncycastle.tls.CipherSuite;
import org.bouncycastle.tls.DefaultTlsClient;
import org.bouncycastle.tls.HashAlgorithm;
import org.bouncycastle.tls.NameType;
import org.bouncycastle.tls.ProtocolVersion;
import org.bouncycastle.tls.ServerName;
import org.bouncycastle.tls.SignatureAlgorithm;
import org.bouncycastle.tls.SignatureAndHashAlgorithm;
import org.bouncycastle.tls.TlsAuthentication;
import org.bouncycastle.tls.TlsCredentials;
import org.bouncycastle.tls.TlsFatalAlert;
import org.bouncycastle.tls.TlsPSKIdentity;
import org.bouncycastle.tls.TlsServerCertificate;
import org.bouncycastle.tls.TlsUtils;
import org.bouncycastle.tls.crypto.TlsCertificate;
import org.bouncycastle.tls.crypto.TlsCrypto;
import org.bouncycastle.tls.crypto.TlsCryptoParameters;
import org.bouncycastle.tls.crypto.impl.bc.BcDefaultTlsCredentialedSigner;
import org.bouncycastle.tls.crypto.impl.bc.BcTlsCrypto;
import org.bouncycastle.tls.crypto.impl.bc.BcTlsRawKeyCertificate;
import org.eclipse.leshan.client.servers.ServerInfo;
import org.eclipse.leshan.core.SecurityMode;
import org.eclipse.leshan.core.security.util.SecurityUtil;

public class LwM2mTlsClient extends DefaultTlsClient {

    private final ServerInfo serverInfo;

    public LwM2mTlsClient(TlsCrypto crypto, ServerInfo serverInfo) {
        super(crypto);
        this.serverInfo = serverInfo;
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

    @SuppressWarnings("rawtypes")
    @Override
    protected Vector getSNIServerNames() {
        if (serverInfo.sni != null) {
            Vector<ServerName> serverNames = new Vector<>();
            serverNames.add(new ServerName(NameType.host_name, serverInfo.sni.getBytes()));
            return serverNames;
        }
        return null;
    }

//    @Override
//    protected Vector getSupportedSignatureAlgorithms() {
//        return TlsUtils.getSupportedSignatureAlgorithms(context,getAllowedSignatureAlgorithms()):
//    }
//
//    protected Vector getAllowedSignatureAlgorithms( Vector candidates) {
//
//    }

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
            return new TlsAuthentication() {
                @Override
                public void notifyServerCertificate(TlsServerCertificate serverCertificate) throws IOException {
                    // Validate server RPK manually
                    byte[] encoded = serverCertificate.getCertificate().getCertificateAt(0).getEncoded();
                    PublicKey receivedKey;
                    try {
                        receivedKey = SecurityUtil.publicKey.decode(encoded);
                    } catch (IOException | GeneralSecurityException e) {
                        throw new IOException("Unable to decode server public key");
                    }

                    if (!receivedKey.equals(serverInfo.serverPublicKey)) {
                        throw new IOException("Server public key mismatch");
                    }
                }

                @Override
                public TlsCredentials getClientCredentials(CertificateRequest certificateRequest) throws IOException {
                    TlsCertificate rawKeyCert = new BcTlsRawKeyCertificate((BcTlsCrypto) getCrypto(),
                            serverInfo.publicKey.getEncoded());
                    Certificate cert = new Certificate(CertificateType.RawPublicKey, null,
                            new CertificateEntry[] { new CertificateEntry(rawKeyCert, null) });
                    return new BcDefaultTlsCredentialedSigner(new TlsCryptoParameters(context),
                            (BcTlsCrypto) getCrypto(), PrivateKeyFactory.createKey(serverInfo.privateKey.getEncoded()),
                            cert,
                            SignatureAndHashAlgorithm.getInstance(HashAlgorithm.sha256, SignatureAlgorithm.ecdsa));
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
