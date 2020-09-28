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

/**
 * This class implements Certificate Usage (1) - Service Certificate Constraint
 *
 * From RFC 6698:
 *
 * 1 -- Certificate usage 1 is used to specify an end entity
 *       certificate, or the public key of such a certificate, that MUST be
 *       matched with the end entity certificate given by the server in
 *       TLS.  This certificate usage is sometimes referred to as "service
 *       certificate constraint" because it limits which end entity
 *       certificate can be used by a given service on a host.  The target
 *       certificate MUST pass PKIX certification path validation and MUST
 *       match the TLSA record.
 *
 * For details about Certificate Usage please see:
 * <a href="https://tools.ietf.org/html/rfc6698#section-2.1.1">rfc6698#section-2.1.1</a> - The Certificate Usage Field
 */
public class ServiceCertificateConstraintCertificateVerifier extends LeshanCertificateVerifierBase {
    public ServiceCertificateConstraintCertificateVerifier(Certificate expectedServerCertificate,
            X509Certificate[] trustedCertificates) {
        super(expectedServerCertificate, trustedCertificates);
    }

    @Override
    public CertPath verifyCertificate(Boolean clientUsage, boolean truncateCertificatePath, CertificateMessage message,
            DTLSSession session) throws HandshakeException {
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
        X509Certificate serverCertificate = (X509Certificate) receivedServerCertificate;

        // If clientUsage is defined then check key usage
        if (clientUsage) {
            if (!CertPathUtil.canBeUsedForAuthentication(serverCertificate, true)) {
                AlertMessage alert = new AlertMessage(AlertLevel.FATAL, AlertDescription.BAD_CERTIFICATE,
                        session.getPeer());
                throw new HandshakeException("Certificate chain could not be validated - Key Usage doesn't match!",
                        alert);
            }
        }

        CertPath certPath = expandCertPath(messageChain);
        if (trustedCertificates != null) {
            try {
                // - must do PKIX validation with trustStore
                CertPathUtil.validateCertificatePath(truncateCertificatePath, certPath, trustedCertificates);
            } catch (GeneralSecurityException e) {
                AlertMessage alert = new AlertMessage(AlertLevel.FATAL, AlertDescription.BAD_CERTIFICATE,
                        session.getPeer());
                throw new HandshakeException("Certificate chain could not be validated", alert, e);
            }
        }

        // - target certificate must match what is provided certificate in server info
        if (!expectedServerCertificate.equals(serverCertificate)) {
            AlertMessage alert = new AlertMessage(AlertLevel.FATAL, AlertDescription.BAD_CERTIFICATE,
                    session.getPeer());
            throw new HandshakeException("Certificate chain could not be validated", alert);
        }

        // - validate server name
        validateSubject(session, serverCertificate);

        return messageChain;
    }
}
