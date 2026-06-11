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
import java.net.InetSocketAddress;
import java.security.PublicKey;
import java.security.cert.CertPath;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Vector;

import org.bouncycastle.crypto.params.AsymmetricKeyParameter;
import org.bouncycastle.tls.AlertDescription;
import org.bouncycastle.tls.Certificate;
import org.bouncycastle.tls.CertificateRequest;
import org.bouncycastle.tls.CertificateType;
import org.bouncycastle.tls.CipherSuite;
import org.bouncycastle.tls.ClientCertificateType;
import org.bouncycastle.tls.KeyExchangeAlgorithm;
import org.bouncycastle.tls.PSKTlsServer;
import org.bouncycastle.tls.ProtocolVersion;
import org.bouncycastle.tls.SecurityParameters;
import org.bouncycastle.tls.SignatureAndHashAlgorithm;
import org.bouncycastle.tls.TlsCredentials;
import org.bouncycastle.tls.TlsFatalAlert;
import org.bouncycastle.tls.TlsPSKIdentityManager;
import org.bouncycastle.tls.TlsUtils;
import org.bouncycastle.tls.crypto.TlsCryptoParameters;
import org.bouncycastle.tls.crypto.impl.bc.BcDefaultTlsCredentialedSigner;
import org.bouncycastle.tls.crypto.impl.bc.BcTlsCrypto;
import org.eclipse.leshan.core.security.certificate.util.X509CertUtil;
import org.eclipse.leshan.core.security.certificate.verifier.DefaultCertificateVerifier;
import org.eclipse.leshan.core.security.certificate.verifier.X509CertificateVerifier.Role;
import org.eclipse.leshan.servers.security.ServerSecurityInfo;
import org.eclipse.leshan.transport.javacoap.server.coaps.bc.endpoint.authentication.TlsAuthenticationUtil;

public class LwM2mTlsServer extends PSKTlsServer {

    private final ServerSecurityInfo serverSecurityInfo;
    private final BcTlsCrypto crypto;
    private final DefaultCertificateVerifier certificateVerifier;
    private final InetSocketAddress clientAddr;

    public LwM2mTlsServer(BcTlsCrypto crypto, TlsPSKIdentityManager pskIdentityManager,
            ServerSecurityInfo serverSecurityInfo, InetSocketAddress clientAddr) {
        super(crypto, pskIdentityManager);
        this.serverSecurityInfo = serverSecurityInfo;
        this.crypto = crypto;
        if (serverSecurityInfo.getTrustedCertificates() != null) {
            this.certificateVerifier = new DefaultCertificateVerifier(
                    X509CertUtil.toX509CertificatesList(Arrays.asList(serverSecurityInfo.getTrustedCertificates()))) {
                @Override
                protected void validateSubject(InetSocketAddress peerSocket, X509Certificate receivedServerCertificate)
                        throws CertificateException {
                    // Do not validate subject at server side.
                }
            };
        } else {
            this.certificateVerifier = null;
        }
        this.clientAddr = clientAddr;
    }

    @Override
    protected ProtocolVersion[] getSupportedVersions() {
        return ProtocolVersion.DTLSv12.only();
    }

    @Override
    public int getHandshakeTimeoutMillis() {
        // limit global handshake time to 15s
        return 15000;
    }

    /**
     * @return {@link SecurityParameters} of established connection or <code>null</code> if peer not connected.
     */
    public SecurityParameters getSecurityParametersConnection() {
        if (context == null) {
            return null;
        }
        return context.getSecurityParametersConnection();
    }

    @SuppressWarnings("java:S1168")
    @Override
    protected short[] getAllowedClientCertificateTypes() {
        if (serverSecurityInfo.getCertificateChain() != null) {
            return new short[] { CertificateType.X509, CertificateType.RawPublicKey };
        } else if (serverSecurityInfo.getPublicKey() != null) {
            return new short[] { CertificateType.RawPublicKey };
        } else {
            return null;
        }
    }

    @Override
    public TlsCredentials getCredentials() throws IOException {
        // we should not return credentials when PSK is used : I didn't find better way to check that...
        int keyExchangeAlgorithm = context.getSecurityParametersHandshake().getKeyExchangeAlgorithm();
        switch (keyExchangeAlgorithm) {
        case KeyExchangeAlgorithm.DHE_PSK:
        case KeyExchangeAlgorithm.ECDHE_PSK:
        case KeyExchangeAlgorithm.PSK:
        case KeyExchangeAlgorithm.RSA_PSK:
            return null;
        default:
            break;
        }

        Certificate cert = null;
        PublicKey publicKey = null;
        if (serverSecurityInfo.getPublicKey() != null) {
            publicKey = serverSecurityInfo.getPublicKey();
            cert = TlsAuthenticationUtil.createRawPublicKeyCertificate(getCrypto(), publicKey);
        } else if (serverSecurityInfo.getCertificateChain() != null
                && serverSecurityInfo.getCertificateChain().length > 0) {
            publicKey = serverSecurityInfo.getCertificateChain()[0].getPublicKey();
            cert = TlsAuthenticationUtil.createCertChain(getCrypto(), serverSecurityInfo.getCertificateChain());
        }

        if (publicKey != null && cert != null) {

            AsymmetricKeyParameter privateKey = TlsAuthenticationUtil
                    .createAsymmetricPrivateKey(serverSecurityInfo.getPrivateKey());

            @SuppressWarnings("java:S1149")
            Vector<?> clientSigAlgsSupported = context.getSecurityParametersHandshake().getClientSigAlgs();

            SignatureAndHashAlgorithm signatureAndHashAlgorithm = TlsAuthenticationUtil
                    .selectSignatureAndHashAlgorithm(crypto, publicKey, clientSigAlgsSupported);

            return new BcDefaultTlsCredentialedSigner(new TlsCryptoParameters(context), crypto, privateKey, cert,
                    signatureAndHashAlgorithm);
        }
        return null;

    }

    @Override
    public CertificateRequest getCertificateRequest() throws IOException {
        short[] certificateTypes = new short[] { ClientCertificateType.ecdsa_sign };
        @SuppressWarnings("java:S1149")
        Vector<?> supportedSigAlgs = TlsUtils.getDefaultSupportedSignatureAlgorithms(context);
        return new CertificateRequest(certificateTypes, supportedSigAlgs, null);
    }

    @Override
    public void notifyClientCertificate(Certificate clientCertificate) throws IOException {
        if (clientCertificate == null || clientCertificate.isEmpty()) {
            throw new TlsFatalAlert(AlertDescription.bad_certificate);
        }
        if (clientCertificate.getCertificateType() == CertificateType.RawPublicKey) {
            return;
        }
        // validate certificate chain
        if (certificateVerifier == null) {
            throw new TlsFatalAlert(AlertDescription.bad_certificate);
        }
        try {
            CertPath certPath = TlsAuthenticationUtil.convertToCertPath(clientCertificate);
            certificateVerifier.verifyCertificate(certPath, clientAddr, Role.CLIENT);
        } catch (CertificateException | IOException e) {
            throw new TlsFatalAlert(AlertDescription.bad_certificate, e);
        }
    }

    @Override
    public int[] getSupportedCipherSuites() {
        int[] supportedCiphers = TlsUtils.getSupportedCipherSuites(getCrypto(), getPskCipherSuites());
        if (serverSecurityInfo.getCertificateChain() != null || serverSecurityInfo.getPublicKey() != null) {
            supportedCiphers = merge(supportedCiphers,
                    TlsUtils.getSupportedCipherSuites(getCrypto(), getEcdheCipherSuites()));
        }
        return supportedCiphers;
    }

    protected int[] merge(int[] a, int[] b) {
        int[] merged = new int[a.length + b.length];

        System.arraycopy(a, 0, merged, 0, a.length);
        System.arraycopy(b, 0, merged, a.length, b.length);
        return merged;
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
}
