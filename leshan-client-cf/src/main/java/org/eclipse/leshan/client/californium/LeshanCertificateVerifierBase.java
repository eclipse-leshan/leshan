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

import org.eclipse.californium.scandium.dtls.CertificateMessage;
import org.eclipse.californium.scandium.dtls.DTLSSession;
import org.eclipse.californium.scandium.dtls.HandshakeException;
import org.eclipse.californium.scandium.dtls.x509.CertificateVerifier;
import org.eclipse.leshan.core.CertificateUsage;

import java.security.cert.Certificate;
import java.security.cert.X509Certificate;

public abstract class LeshanCertificateVerifierBase implements CertificateVerifier {
    protected final Certificate expectedServerCertificate;
    protected final X509Certificate[] trustedCertificates;
    protected final CertificateUsage certificateUsage;

    public LeshanCertificateVerifierBase(Certificate expectedServerCertificate,
            X509Certificate[] trustedCertificates) {
        this.expectedServerCertificate = expectedServerCertificate;
        this.trustedCertificates = trustedCertificates;
        this.certificateUsage = CertificateUsage.DOMAIN_ISSUER_CERTIFICATE;
    }

    @Override
    public abstract void verifyCertificate(CertificateMessage message, DTLSSession session) throws HandshakeException;

    @Override
    public X509Certificate[] getAcceptedIssuers() {
        return null;
    }
}
