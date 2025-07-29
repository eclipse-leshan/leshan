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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.CertPath;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import org.bouncycastle.crypto.params.AsymmetricKeyParameter;
import org.bouncycastle.crypto.util.PrivateKeyFactory;
import org.bouncycastle.tls.Certificate;
import org.bouncycastle.tls.CertificateEntry;
import org.bouncycastle.tls.CertificateType;
import org.bouncycastle.tls.SignatureAlgorithm;
import org.bouncycastle.tls.SignatureAndHashAlgorithm;
import org.bouncycastle.tls.TlsServerCertificate;
import org.bouncycastle.tls.crypto.TlsCertificate;
import org.bouncycastle.tls.crypto.TlsCrypto;

public class TlsAuthenticationUtil {

    private TlsAuthenticationUtil() {
    }

    public static Certificate createRawPublicKeyCertificate(TlsCrypto crypto, PublicKey publicKey) throws IOException {
        TlsCertificate clientPubKeyCert = crypto.createCertificate(CertificateType.RawPublicKey,
                publicKey.getEncoded());
        return new Certificate(CertificateType.RawPublicKey, null,
                new CertificateEntry[] { new CertificateEntry(clientPubKeyCert, null) });
    }

    public static Certificate createX509Certificate(TlsCrypto crypto, X509Certificate certificate) throws IOException {
        TlsCertificate clientPubKeyCert;
        try {
            clientPubKeyCert = crypto.createCertificate(CertificateType.X509, certificate.getEncoded());
            return new Certificate(CertificateType.X509, null,
                    new CertificateEntry[] { new CertificateEntry(clientPubKeyCert, null) });
        } catch (CertificateEncodingException e) {
            throw new IOException("Unable to convert certificate", e);
        }

    }

    public static AsymmetricKeyParameter createAsymmetricPrivateKey(PrivateKey privateKey) throws IOException {
        return PrivateKeyFactory.createKey(privateKey.getEncoded());
    }

    public static CertPath convertToCertPath(TlsServerCertificate tlsCert) throws CertificateException, IOException {
        CertificateFactory certFactory = CertificateFactory.getInstance("X.509");
        List<X509Certificate> certs = new ArrayList<>();
        for (TlsCertificate bcCert : tlsCert.getCertificate().getCertificateList()) {
            certs.add((X509Certificate) certFactory.generateCertificate(new ByteArrayInputStream(bcCert.getEncoded())));
        }
        return certFactory.generateCertPath(certs);
    }

    public static SignatureAndHashAlgorithm selectSignatureAndHashAlgorithm(TlsCrypto crypto, PublicKey publicKey,
            @SuppressWarnings({ "rawtypes", "java:S1149" }) //
            Vector /* <SignatureAndHashAlgorithm> */ signatureAndHashAlgorithms) {

        short signature = getPublicKeySignatureAlgorithm(publicKey);
        if (signature == -1) {
            throw new IllegalStateException(
                    String.format("Client public key use unsupported signature algorithm : %s", publicKey.toString()));
        }

        for (Object object : signatureAndHashAlgorithms) {
            SignatureAndHashAlgorithm signatureAndHashAlgorithm = (SignatureAndHashAlgorithm) object;
            if ( // match key algorithm
            signatureAndHashAlgorithm.getSignature() == signature
                    // signature and hash algorithm is supported
                    && crypto.hasSignatureAndHashAlgorithm(signatureAndHashAlgorithm)) {
                return signatureAndHashAlgorithm;
            }
        }
        return null;
    }

    private static short getPublicKeySignatureAlgorithm(PublicKey pubKey) {
        String algorithm = pubKey.getAlgorithm();
        if ("RSA".equals(algorithm)) {
            return SignatureAlgorithm.rsa;
        } else if ("EC".equals(algorithm)) {
            return SignatureAlgorithm.ecdsa;
        } else if ("DSA".equals(algorithm)) {
            return SignatureAlgorithm.dsa;
        } else {
            return -1; // Unknown/unsupported
        }
    }
}
