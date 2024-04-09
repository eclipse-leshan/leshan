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
package org.eclipse.leshan.client.security;

import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.List;

import org.eclipse.leshan.client.servers.ServerInfo;
import org.eclipse.leshan.core.CertificateUsage;
import org.eclipse.leshan.core.security.certificate.util.X509CertUtil;
import org.eclipse.leshan.core.security.certificate.verifier.CaConstraintCertificateVerifier;
import org.eclipse.leshan.core.security.certificate.verifier.DomainIssuerCertificateVerifier;
import org.eclipse.leshan.core.security.certificate.verifier.ServiceCertificateConstraintCertificateVerifier;
import org.eclipse.leshan.core.security.certificate.verifier.TrustAnchorAssertionCertificateVerifier;
import org.eclipse.leshan.core.security.certificate.verifier.X509CertificateVerifier;

/**
 * Create {@link X509CertificateVerifier} from {@link ServerInfo} and a list of trusted {@link Certificate}.
 */
public class CertificateVerifierFactory {

    public X509CertificateVerifier create(ServerInfo serverInfo, List<Certificate> trustStore) {

        // LWM2M v1.1.1 - 5.2.8.7. Certificate Usage Field
        //
        // 0: Certificate usage 0 ("CA constraint")
        // - trustStore is client's configured trust store
        // - must do PKIX validation with trustStore to build certPath
        // - must check that given certificate is part of certPath
        // - validate server name
        //
        // 1: Certificate usage 1 ("service certificate constraint")
        // - trustStore is client's configured trust store
        // - must do PKIX validation with trustStore
        // - target certificate must match what is provided certificate in server info
        // - validate server name
        //
        // 2: Certificate usage 2 ("trust anchor assertion")
        // - trustStore is only the provided certificate in server info
        // - must do PKIX validation with trustStore
        // - validate server name
        //
        // 3: Certificate usage 3 ("domain-issued certificate") (default mode if missing)
        // - no trustStore used in this mode
        // - target certificate must match what is provided certificate in server info
        // - validate server name

        CertificateUsage certificateUsage = serverInfo.certificateUsage != null ? serverInfo.certificateUsage
                : CertificateUsage.DOMAIN_ISSUER_CERTIFICATE;

        if (certificateUsage == CertificateUsage.CA_CONSTRAINT) {
            X509Certificate[] trustedCertificates = null;
            if (trustStore != null) {
                trustedCertificates = X509CertUtil.toX509CertificatesList(trustStore)
                        .toArray(new X509Certificate[trustStore.size()]);
            }
            return new CaConstraintCertificateVerifier(serverInfo.serverCertificate, trustedCertificates,
                    serverInfo.sni);
        } else if (certificateUsage == CertificateUsage.SERVICE_CERTIFICATE_CONSTRAINT) {
            X509Certificate[] trustedCertificates = null;

            // - trustStore is client's configured trust store
            if (trustStore != null) {
                trustedCertificates = X509CertUtil.toX509CertificatesList(trustStore)
                        .toArray(new X509Certificate[trustStore.size()]);
            }

            return new ServiceCertificateConstraintCertificateVerifier(serverInfo.serverCertificate,
                    trustedCertificates, serverInfo.sni);
        } else if (certificateUsage == CertificateUsage.TRUST_ANCHOR_ASSERTION) {
            return new TrustAnchorAssertionCertificateVerifier((X509Certificate) serverInfo.serverCertificate,
                    serverInfo.sni);
        } else if (certificateUsage == CertificateUsage.DOMAIN_ISSUER_CERTIFICATE) {
            return new DomainIssuerCertificateVerifier(serverInfo.serverCertificate);
        }
        throw new IllegalStateException(String.format("Unsupported Certificate Usage %s", certificateUsage));
    }
}
