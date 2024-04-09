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
package org.eclipse.leshan.core.security.certificate.util;

import java.security.GeneralSecurityException;
import java.security.cert.CertPath;
import java.security.cert.CertPathValidator;
import java.security.cert.CertPathValidatorException;
import java.security.cert.CertPathValidatorResult;
import java.security.cert.PKIXCertPathValidatorResult;
import java.security.cert.PKIXParameters;
import java.security.cert.TrustAnchor;
import java.security.cert.X509Certificate;
import java.util.HashSet;
import java.util.Set;

/**
 * Utility class about PKI validation.
 */
public class PKIValidator {

    /**
     * Validate Certificate Path using <a href=
     * "https://docs.oracle.com/javase/8/docs/technotes/guides/security/certpath/CertPathProgGuide.html#PKIXClasses">Java
     * PKIX algorithm</a> which implement <a href="https://tools.ietf.org/html/rfc3280">RFC3280</a>.
     *
     * @param certPath a certificate path to validate
     * @param trustedCertificates list of trusted certificates
     * @return the certpath from certificate to validate (end node) to first trusted anchor included.
     */
    public static CertPath applyPKIXValidation(CertPath certPath, X509Certificate[] trustedCertificates)
            throws GeneralSecurityException {
        // We first need to adapt certificate path to PKIX algorithm :
        // - trust anchor must not be in certificate path
        // See : https://tools.ietf.org/html/rfc3280#section-6
        CertPath adaptedCertPath = CertPathUtil.truncateToFirstTrustedCert(certPath, trustedCertificates);

        if (adaptedCertPath.getCertificates().isEmpty())
            throw new CertPathValidatorException(
                    "Invalid certificate path : certificate path is empty or end node certificate is directly trusted");

        // Create trustAnchor for PKIX algorithm
        Set<TrustAnchor> trustAnchors = new HashSet<TrustAnchor>();
        for (X509Certificate cert : trustedCertificates) {
            trustAnchors.add(new TrustAnchor(cert, null));
        }

        // Apply PKIX Validation agorithm
        String algorithm = CertPathValidator.getDefaultType();
        CertPathValidator validator = CertPathValidator.getInstance(algorithm);
        PKIXParameters params = new PKIXParameters(trustAnchors);
        // TODO: implement alternative means of revocation checking ?
        params.setRevocationEnabled(false);
        CertPathValidatorResult result = validator.validate(adaptedCertPath, params);

        // Create complete validated certification path
        X509Certificate trustedCertificate = ((PKIXCertPathValidatorResult) result).getTrustAnchor().getTrustedCert();
        return CertPathUtil.add(adaptedCertPath, trustedCertificate);
    }
}
