/*******************************************************************************
 * Copyright (c) 2024 Sierra Wireless and others.
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
package org.eclipse.leshan.core.security.certificate.verifier;

import java.net.InetSocketAddress;
import java.security.GeneralSecurityException;
import java.security.cert.CertPath;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.List;

import org.eclipse.leshan.core.security.certificate.util.PKIValidator;

/**
 * Will do PKIX validation using given certificate as trust anchor.
 */
public class DefaultCertificateVerifier extends BaseCertificateVerifier {

    private final List<X509Certificate> trustedCertificates;

    /**
     * Will apply default certificate validation using given <code>trustedCertificates</code> as trust anchor.
     * <p>
     * An empty list means we trust anything.
     *
     * @param trustedCertificates the trusted certificate, empty list means we trust anything, <code>null</code> is not
     *        allowed.
     */
    public DefaultCertificateVerifier(List<X509Certificate> trustedCertificates) {
        this.trustedCertificates = trustedCertificates;
    }

    @Override
    public CertPath verifyCertificate(CertPath remotePeerCertChain, InetSocketAddress remotePeerAddress,
            Role remotePeerRole) throws CertificateException {

        if (trustedCertificates.isEmpty()) {
            // In this case we trust anything.
            return remotePeerCertChain;
        }

        validateCertificateChainNotEmpty(remotePeerCertChain);
        X509Certificate receivedServerCertificate = validateReceivedCertificateIsSupported(remotePeerCertChain);
        validateCertificateCanBeUsedForAuthentication(receivedServerCertificate, remotePeerRole);

        // - must do PKIX validation with trustStore
        CertPath certPath;
        try {
            certPath = PKIValidator.applyPKIXValidation(remotePeerCertChain,
                    trustedCertificates.toArray(new X509Certificate[trustedCertificates.size()]));
        } catch (GeneralSecurityException e) {
            throw new CertificateException("Certificate chain could not be validated");
        }

        validateSubject(remotePeerAddress, receivedServerCertificate);

        return certPath;
    }
}
