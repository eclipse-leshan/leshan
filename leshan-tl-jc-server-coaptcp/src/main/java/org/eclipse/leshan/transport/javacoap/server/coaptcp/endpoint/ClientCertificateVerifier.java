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
package org.eclipse.leshan.transport.javacoap.server.coaptcp.endpoint;

import java.net.InetSocketAddress;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.List;

import org.eclipse.leshan.core.security.certificate.util.X509CertUtil;
import org.eclipse.leshan.core.security.certificate.verifier.DefaultCertificateVerifier;

/**
 * Verifies the certificates which the clients present to a LwM2M server over TLS.
 */
public class ClientCertificateVerifier extends DefaultCertificateVerifier {

    ClientCertificateVerifier(List<X509Certificate> trustedCertificates) {
        super(trustedCertificates);
    }

    @Override
    protected void validateSubject(InetSocketAddress peerSocket, X509Certificate receivedClientCertificate)
            throws CertificateException {
        // At server side the subject is not matched against the address of the client. But its common name is the
        // identity of the client, so the certificate must have exactly one.
        try {
            X509CertUtil.extractCN(receivedClientCertificate);
        } catch (IllegalStateException e) {
            throw new CertificateException(e.getMessage(), e);
        }
    }
}
