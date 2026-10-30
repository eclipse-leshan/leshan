/*******************************************************************************
 * Copyright (c) 2026    Sierra Wireless and others.
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
import java.security.cert.CertPath;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
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
import org.eclipse.californium.scandium.dtls.x509.CertificateConfigurationHelper;
import org.eclipse.californium.scandium.dtls.x509.CertificateVerifier;
import org.eclipse.californium.scandium.dtls.x509.ConfigurationHelperSetup;
import org.eclipse.californium.scandium.util.ServerNames;
import org.eclipse.leshan.core.security.certificate.util.X509CertUtil;

/**
 * A {@link CertificateVerifier} which refuses the certificates of the clients which can not identify them.
 * <p>
 * To use at server side, for the DTLS endpoints which authenticate the clients with a certificate.
 * <p>
 * The identity of a LwM2M client is the common name of its certificate, so the certificate must have exactly one. The
 * verification of the certificate itself is done by the decorated verifier, which must give its result directly (as
 * {@link org.eclipse.californium.scandium.dtls.x509.StaticCertificateVerifier} does) : this is not able to check a
 * result given later to the {@link HandshakeResultHandler}.
 */
public final class ClientCommonNameCertificateVerifier implements CertificateVerifier, ConfigurationHelperSetup {

    private final CertificateVerifier delegate;

    public ClientCommonNameCertificateVerifier(CertificateVerifier delegate) {
        this.delegate = delegate;
    }

    @Override
    public CertificateVerificationResult verifyCertificate(ConnectionId cid, ServerNames serverName,
            InetSocketAddress remotePeer, boolean clientUsage, boolean verifySubject, boolean truncateCertificatePath,
            CertificateMessage message) {
        CertificateVerificationResult result = delegate.verifyCertificate(cid, serverName, remotePeer, clientUsage,
                verifySubject, truncateCertificatePath, message);
        if (clientUsage && result != null && result.getException() == null) {
            X509Certificate clientCertificate = getFirstCertificate(result.getCertificatePath());
            if (clientCertificate != null) {
                try {
                    X509CertUtil.extractCN(clientCertificate);
                } catch (IllegalStateException e) {
                    AlertMessage alert = new AlertMessage(AlertLevel.FATAL, AlertDescription.BAD_CERTIFICATE);
                    return new CertificateVerificationResult(cid, new HandshakeException(
                            "Client certificate has no usable common name: " + e.getMessage(), alert));
                }
            }
        }
        return result;
    }

    /**
     * @return the first certificate of the path, which is the one of the client, or null if there is none.
     */
    private static X509Certificate getFirstCertificate(CertPath path) {
        if (path == null) {
            return null;
        }
        List<? extends Certificate> certificates = path.getCertificates();
        if (!certificates.isEmpty() && certificates.get(0) instanceof X509Certificate) {
            return (X509Certificate) certificates.get(0);
        }
        return null;
    }

    @Override
    public List<CertificateType> getSupportedCertificateTypes() {
        return delegate.getSupportedCertificateTypes();
    }

    @Override
    public List<X500Principal> getAcceptedIssuers() {
        return delegate.getAcceptedIssuers();
    }

    @Override
    public void setResultHandler(HandshakeResultHandler resultHandler) {
        delegate.setResultHandler(resultHandler);
    }

    @Override
    public void setupConfigurationHelper(CertificateConfigurationHelper helper) {
        // the decorated verifier tells which trust anchors Scandium has to take into account
        if (delegate instanceof ConfigurationHelperSetup) {
            ((ConfigurationHelperSetup) delegate).setupConfigurationHelper(helper);
        }
    }
}
