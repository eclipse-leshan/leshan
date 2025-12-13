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
package org.eclipse.leshan.transport.javacoap.client.coaps.bc.endpoint.authentication;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.security.PrivateKey;
import java.security.cert.CertPath;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import org.bouncycastle.crypto.params.AsymmetricKeyParameter;
import org.bouncycastle.tls.Certificate;
import org.bouncycastle.tls.CertificateRequest;
import org.bouncycastle.tls.SignatureAndHashAlgorithm;
import org.bouncycastle.tls.TlsContext;
import org.bouncycastle.tls.TlsCredentials;
import org.bouncycastle.tls.TlsServerCertificate;
import org.bouncycastle.tls.crypto.TlsCryptoParameters;
import org.bouncycastle.tls.crypto.impl.bc.BcDefaultTlsCredentialedSigner;
import org.bouncycastle.tls.crypto.impl.bc.BcTlsCrypto;
import org.eclipse.leshan.core.security.certificate.verifier.X509CertificateVerifier;
import org.eclipse.leshan.core.security.certificate.verifier.X509CertificateVerifier.Role;

public abstract class X509Authentication extends AbstractTlsAuthentication {

    protected X509Authentication(BcTlsCrypto crypto, TlsContext context) {
        super(crypto, context);
    }

    public abstract PrivateKey getClientPrivateKey();

    public abstract X509Certificate getClientCertificate();

    public abstract X509CertificateVerifier getCertificateVerifier();

    public abstract InetSocketAddress getRemotePeerAddress();

    @Override
    public void notifyServerCertificate(TlsServerCertificate serverCertificate) throws IOException {
        try {
            CertPath certPath = TlsAuthenticationUtil.convertToCertPath(serverCertificate);
            getCertificateVerifier().verifyCertificate(certPath, getRemotePeerAddress(), Role.CLIENT);
        } catch (CertificateException e) {
            throw new IOException("Certificate doesn't pass validation !", e);
        }
    }

    @Override
    public TlsCredentials getClientCredentials(CertificateRequest certificateRequest) throws IOException {
        Certificate cert = TlsAuthenticationUtil.createX509Certificate(getCrypto(), getClientCertificate());
        AsymmetricKeyParameter privateKey = TlsAuthenticationUtil.createAsymmetricPrivateKey(getClientPrivateKey());

        SignatureAndHashAlgorithm selectedSignatureAndHashAlgorithm = TlsAuthenticationUtil
                .selectSignatureAndHashAlgorithm(getCrypto(), getClientCertificate().getPublicKey(),
                        certificateRequest.getSupportedSignatureAlgorithms());

        return new BcDefaultTlsCredentialedSigner(new TlsCryptoParameters(getContext()), getCrypto(), privateKey, cert,
                selectedSignatureAndHashAlgorithm);
    }

}
