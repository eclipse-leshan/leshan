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

import java.security.GeneralSecurityException;
import java.security.cert.CertPath;
import java.security.cert.CertPathValidator;
import java.security.cert.CertPathValidatorResult;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.PKIXCertPathValidatorResult;
import java.security.cert.PKIXParameters;
import java.security.cert.TrustAnchor;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.californium.elements.util.CertPathUtil;

public class X509Util {

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
        CertPath adaptedCertPath = truncateToFirstTrustedCert(certPath, trustedCertificates);

        if (adaptedCertPath.getCertificates().isEmpty())
            throw new IllegalArgumentException(
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
        return add(adaptedCertPath, trustedCertificate);
    }

    /**
     * Truncate certificate path just before the first trusted certificates.
     * 
     * @param certPath a certificate path to eventually truncate
     * @param trustedCertificates list of trusted certificates
     * @return a certPath without trusted any trusted certificates
     */
    public static CertPath truncateToFirstTrustedCert(CertPath certPath, X509Certificate[] trustedCertificates)
            throws CertificateEncodingException {
        List<X509Certificate> certificates = CertPathUtil.toX509CertificatesList(certPath.getCertificates());
        for (int index = 0; index < certificates.size(); ++index) {
            X509Certificate certificate = certificates.get(index);
            if (contains(certificate, trustedCertificates)) {
                return CertPathUtil.generateCertPath(certificates, index);
            }
        }
        return certPath;
    }

    /**
     * Add certificate at the end of the given certificate path.
     * 
     * @param certPath a certificate path to extend
     * @param certificate to add at the end
     * @return a certPath ending with given certificate
     */
    public static CertPath add(CertPath certPath, X509Certificate certificate) {
        List<X509Certificate> certificates = CertPathUtil.toX509CertificatesList(certPath.getCertificates());
        certificates.add(certificate);
        return CertPathUtil.generateCertPath(certificates);
    }

    /**
     * Search certificate in trusts.
     * 
     * @param certificate certificate to search
     * @param certificates to search
     * @return {@code true}, if certificate is contained, {@code false}, otherwise.
     */
    public static boolean contains(X509Certificate certificate, X509Certificate[] certificates) {
        for (X509Certificate trust : certificates) {
            if (certificate.equals(trust)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Convert array of {@link Certificate} to array of {@link X509Certificate}
     */
    public static X509Certificate[] asX509Certificates(Certificate[] certificates) throws CertificateException {
        ArrayList<X509Certificate> x509Certificates = new ArrayList<>();

        for (Certificate cert : certificates) {
            if (!(cert instanceof X509Certificate)) {
                throw new CertificateException(String.format(
                        "%s certificate format is not supported, Only X.509 certificate is supported", cert.getType()));
            }
            x509Certificates.add((X509Certificate) cert);
        }

        return x509Certificates.toArray(new X509Certificate[0]);
    }
}
