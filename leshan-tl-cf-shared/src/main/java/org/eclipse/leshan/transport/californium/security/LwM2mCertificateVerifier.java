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

package org.eclipse.leshan.transport.californium.security;

import java.net.InetSocketAddress;
import java.security.cert.CertificateException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.security.auth.x500.X500Principal;

import org.eclipse.californium.scandium.dtls.AlertMessage;
import org.eclipse.californium.scandium.dtls.AlertMessage.AlertDescription;
import org.eclipse.californium.scandium.dtls.AlertMessage.AlertLevel;
import org.eclipse.californium.scandium.dtls.CertificateMessage;
import org.eclipse.californium.scandium.dtls.CertificateType;
import org.eclipse.californium.scandium.dtls.CertificateVerificationResult;
import org.eclipse.californium.scandium.dtls.ConnectionId;
import org.eclipse.californium.scandium.dtls.HandshakeException;
import org.eclipse.californium.scandium.dtls.HandshakeResultHandler;
import org.eclipse.californium.scandium.dtls.x509.CertificateVerifier;
import org.eclipse.californium.scandium.util.ServerNames;
import org.eclipse.leshan.core.security.certificate.verifier.X509CertificateVerifier;
import org.eclipse.leshan.core.security.certificate.verifier.X509CertificateVerifier.Role;

public class LwM2mCertificateVerifier implements CertificateVerifier {

    private final List<CertificateType> supportedCertificateType = Arrays.asList(CertificateType.X_509);

    private final X509CertificateVerifier certificateVerifier;

    public LwM2mCertificateVerifier(X509CertificateVerifier certificateVerifier) {
        this.certificateVerifier = certificateVerifier;
    }

    @Override
    public List<CertificateType> getSupportedCertificateTypes() {
        return supportedCertificateType;
    }

    @Override
    public void setResultHandler(HandshakeResultHandler resultHandler) {
        // we don't use async mode.
    }

    @Override
    public List<X500Principal> getAcceptedIssuers() {
        return Collections.emptyList();
    }

    @Override
    public CertificateVerificationResult verifyCertificate(ConnectionId cid, ServerNames serverName,
            InetSocketAddress remotePeer, boolean clientUsage, boolean verifyDestination,
            boolean truncateCertificatePath, CertificateMessage message) {
        try {
            // verifyDestination is currently not used.
            // The DTLS_VERIFY_SERVER_CERTIFICATES_SUBJECT is therefore set to transient.
            certificateVerifier.verifyCertificate(message.getCertificateChain(), remotePeer,
                    clientUsage ? Role.CLIENT : Role.SERVER);
            return new CertificateVerificationResult(cid, message.getCertificateChain(), null);
        } catch (CertificateException exception) {
            return new CertificateVerificationResult(cid, new HandshakeException("Unable to verify Certificate",
                    new AlertMessage(AlertLevel.FATAL, AlertDescription.BAD_CERTIFICATE), exception));
        }
    }

}
