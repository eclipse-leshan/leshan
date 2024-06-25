/*******************************************************************************
 * Copyright (c) 2020 Sierra Wireless and others.
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
import java.security.cert.CertPath;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import org.eclipse.leshan.core.security.certificate.util.X509CertUtil;

public abstract class BaseCertificateVerifier implements X509CertificateVerifier {

    /**
     * Ensure that chain is not empty
     */
    protected void validateCertificateChainNotEmpty(CertPath certChain) throws CertificateException {
        if (certChain.getCertificates().size() == 0) {
            throw new CertificateException("Certificate chain could not be validated : server cert chain is empty");
        }
    }

    /**
     * Ensure that received certificate is x509 certificate
     */
    protected X509Certificate validateReceivedCertificateIsSupported(CertPath certChain) throws CertificateException {
        Certificate receivedServerCertificate = certChain.getCertificates().get(0);
        if (!(receivedServerCertificate instanceof X509Certificate)) {
            throw new CertificateException("Certificate chain could not be validated - unknown certificate type");
        }
        return (X509Certificate) receivedServerCertificate;
    }

    protected void validateSNI(String serverName, X509Certificate receivedServerCertificate)
            throws CertificateException {
        if (X509CertUtil.matchSubjectDnsName(receivedServerCertificate, serverName))
            return;

        throw new CertificateException(
                "Certificate chain could not be validated - server identity (sni) does not match certificate");
    }

    protected void validateSubject(InetSocketAddress peerSocket, X509Certificate receivedServerCertificate)
            throws CertificateException {

        if (X509CertUtil.matchSubjectDnsName(receivedServerCertificate, peerSocket.getHostName()))
            return;

        if (X509CertUtil.matchSubjectInetAddress(receivedServerCertificate, peerSocket.getAddress()))
            return;

        throw new CertificateException(
                "Certificate chain could not be validated - server identity does not match certificate");
    }

    protected void validateCertificateCanBeUsedForAuthentication(X509Certificate certificate,
            Role certificateOwnerRole) {
        switch (certificateOwnerRole) {
        case CLIENT:
            X509CertUtil.canBeUsedForAuthentication(certificate, true);
            break;
        case SERVER:
            X509CertUtil.canBeUsedForAuthentication(certificate, false);
            break;
        default:
            throw new IllegalStateException("Unsupported role " + certificateOwnerRole);
        }
    }
}
