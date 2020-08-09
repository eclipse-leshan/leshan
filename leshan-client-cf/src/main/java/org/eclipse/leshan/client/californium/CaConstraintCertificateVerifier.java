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
package org.eclipse.leshan.client.californium;

import org.eclipse.californium.elements.util.CertPathUtil;
import org.eclipse.californium.scandium.dtls.AlertMessage;
import org.eclipse.californium.scandium.dtls.AlertMessage.AlertDescription;
import org.eclipse.californium.scandium.dtls.AlertMessage.AlertLevel;
import org.eclipse.californium.scandium.dtls.CertificateMessage;
import org.eclipse.californium.scandium.dtls.DTLSSession;
import org.eclipse.californium.scandium.dtls.HandshakeException;

import java.security.GeneralSecurityException;
import java.security.cert.CertPath;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.List;

/**
 * This class implements Certificate Usage (0) - CA Constraint
 *
 * From RFC 6698:
 *
 * 0 -- Certificate usage 0 is used to specify a CA certificate, or
 *       the public key of such a certificate, that MUST be found in any of
 *       the PKIX certification paths for the end entity certificate given
 *       by the server in TLS.  This certificate usage is sometimes
 *       referred to as "CA constraint" because it limits which CA can be
 *       used to issue certificates for a given service on a host.  The
 *       presented certificate MUST pass PKIX certification path
 *       validation, and a CA certificate that matches the TLSA record MUST
 *       be included as part of a valid certification path.  Because this
 *       certificate usage allows both trust anchors and CA certificates,
 *       the certificate might or might not have the basicConstraints
 *       extension present.
 *
 * For details about Certificate Usage please see:
 * <a href="https://tools.ietf.org/html/rfc6698#section-2.1.1">rfc6698#section-2.1.1</a> - The Certificate Usage Field
 */
public class CaConstraintCertificateVerifier extends LeshanCertificateVerifierBase {

    public CaConstraintCertificateVerifier(Certificate expectedServerCertificate,
            X509Certificate[] trustedCertificates) {
        super(expectedServerCertificate, trustedCertificates);
    }

    @Override
    public void verifyCertificate(CertificateMessage message, DTLSSession session) throws HandshakeException {
        CertPath messageChain = message.getCertificateChain();

        if (messageChain.getCertificates().size() == 0) {
            AlertMessage alert = new AlertMessage(AlertLevel.FATAL, AlertDescription.BAD_CERTIFICATE,
                    session.getPeer());
            throw new HandshakeException("Certificate chain could not be validated : server cert chain is empty",
                    alert);
        }

        Certificate receivedServerCertificate = messageChain.getCertificates().get(0);
        if (!(receivedServerCertificate instanceof X509Certificate)) {
            AlertMessage alert = new AlertMessage(AlertLevel.FATAL, AlertDescription.UNSUPPORTED_CERTIFICATE,
                    session.getPeer());
            throw new HandshakeException("Certificate chain could not be validated - unknown certificate type", alert);
        }
        X509Certificate serverCertificate = (X509Certificate)receivedServerCertificate;

        CertPath certPath = null;
        if (trustedCertificates != null) {
            try {
                // - must do PKIX validation with trustStore
                certPath = CertPathUtil.validateCertificatePath(false, messageChain, trustedCertificates);
            } catch (GeneralSecurityException e) {
                AlertMessage alert = new AlertMessage(AlertLevel.FATAL, AlertDescription.BAD_CERTIFICATE,
                        session.getPeer());
                throw new HandshakeException("Certificate chain could not be validated", alert, e);
            }
        }

        boolean found = false;

        // - must check that given certificate is part of certPath
        List<? extends Certificate> certificates = certPath.getCertificates();
        for (Certificate certificate : certificates) {
            if (certificate.equals(expectedServerCertificate)) {
                X509Certificate x509Certificate = (X509Certificate) certificate;
                // Make sure that certificate is a CA certificate
                if (x509Certificate.getBasicConstraints() != -1) {
                    found = true;
                    break;
                }
            }
        }

        if (!found) {
            // Np match found -> throw exception about it
            AlertMessage alert = new AlertMessage(AlertLevel.FATAL, AlertDescription.BAD_CERTIFICATE,
                    session.getPeer());
            throw new HandshakeException("Certificate chain could not be validated", alert);
        }

        // - validate server name
        validateSubject(session, serverCertificate);
    }
}
