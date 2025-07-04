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
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Arrays;

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

public abstract class RpkAuthentication extends AbstractTlsAuthentication {

    protected RpkAuthentication(BcTlsCrypto crypto, TlsContext context) {
        super(crypto, context);
    }

    public abstract PrivateKey getClientPrivateKey();

    public abstract PublicKey getClientPublicKey();

    public abstract PublicKey getServerPublicKey();

    @Override
    public void notifyServerCertificate(TlsServerCertificate serverCertificate) throws IOException {

        if (serverCertificate.getCertificate() == null || serverCertificate.getCertificate().isEmpty()) {
            throw new IOException("Server certificate/publicKey expected !");
        }

        byte[] receivedKey = serverCertificate.getCertificate().getCertificateAt(0).getEncoded();
        if (!Arrays.equals(receivedKey, getServerPublicKey().getEncoded())) {
            throw new IOException("Server public key mismatch !");
        }
    }

    @Override
    public TlsCredentials getClientCredentials(CertificateRequest certificateRequest) throws IOException {

        Certificate rpkCert = TlsAuthenticationUtil.createRawPublicKeyCertificate(getCrypto(), getClientPublicKey());
        AsymmetricKeyParameter privateKey = TlsAuthenticationUtil.createAsymmetricPrivateKey(getClientPrivateKey());

        SignatureAndHashAlgorithm selectedSignatureAndHashAlgorithm = TlsAuthenticationUtil
                .selectSignatureAndHashAlgorithm(getCrypto(), getClientPublicKey(),
                        certificateRequest.getSupportedSignatureAlgorithms());

        return new BcDefaultTlsCredentialedSigner(new TlsCryptoParameters(getContext()), getCrypto(), privateKey,
                rpkCert, selectedSignatureAndHashAlgorithm);
    }
}
