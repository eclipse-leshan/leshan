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

import java.security.cert.CertPath;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.security.auth.x500.X500Principal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Certificate Path Utility.
 */
public class CertPathUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(CertPathUtil.class);

    private static final String TYPE_X509 = "X.509";

    /**
     * Create certificate path from x509 certificates chain.
     *
     * @param certificateChain list with chain of x509 certificates. Maybe empty.
     * @return generated certificate path
     * @throws NullPointerException if provided certificateChain is {@code null}
     */
    public static CertPath generateCertPath(List<X509Certificate> certificateChain) {
        if (certificateChain == null) {
            throw new NullPointerException("Certificate chain must not be null!");
        }
        return generateCertPath(certificateChain, certificateChain.size());
    }

    /**
     * Create certificate path from x509 certificates chain up to the provided size.
     *
     * @param certificateChain list with chain of x509 certificates. Maybe empty.
     * @param size size of path to be included in the certificate path.
     * @return generated certificate path
     * @throws NullPointerException if provided certificateChain is {@code null}
     * @throws IllegalArgumentException if size is larger than certificate chain
     */
    public static CertPath generateCertPath(List<X509Certificate> certificateChain, int size) {
        if (certificateChain == null) {
            throw new NullPointerException("Certificate chain must not be null!");
        }
        if (size > certificateChain.size()) {
            throw new IllegalArgumentException("size must not be larger then certificate chain!");
        }
        try {
            if (!certificateChain.isEmpty()) {
                int last = certificateChain.size() - 1;
                X500Principal issuer = null;
                for (int index = 0; index <= last; ++index) {
                    X509Certificate cert = certificateChain.get(index);
                    LOGGER.debug("Current Subject DN: {}", cert.getSubjectX500Principal().getName());
                    if (issuer != null && !issuer.equals(cert.getSubjectX500Principal())) {
                        LOGGER.debug("Actual Issuer DN: {}", cert.getSubjectX500Principal().getName());
                        throw new IllegalArgumentException("Given certificates do not form a chain");
                    }
                    issuer = cert.getIssuerX500Principal();
                    LOGGER.debug("Expected Issuer DN: {}", issuer.getName());
                    if (issuer.equals(cert.getSubjectX500Principal()) && index != last) {
                        // a self-signed certificate, which is not the root
                        throw new IllegalArgumentException(
                                "Given certificates do not form a chain, root is not the last!");
                    }
                }
                if (size < certificateChain.size()) {
                    List<X509Certificate> temp = new ArrayList<>();
                    for (int index = 0; index < size; ++index) {
                        temp.add(certificateChain.get(index));
                    }
                    certificateChain = temp;
                }
            }
            CertificateFactory factory = CertificateFactory.getInstance(TYPE_X509);
            return factory.generateCertPath(certificateChain);
        } catch (CertificateException e) {
            // should not happen because all Java 7 implementation MUST
            // support X.509 certificates
            throw new IllegalArgumentException("could not create X.509 certificate factory", e);
        }
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
        List<X509Certificate> certificates = X509CertUtil.toX509CertificatesList(certPath.getCertificates());
        for (int index = 0; index < certificates.size(); ++index) {
            X509Certificate certificate = certificates.get(index);
            if (Arrays.asList(trustedCertificates).contains(certificate)) {
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
        List<X509Certificate> certificates = X509CertUtil.toX509CertificatesList(certPath.getCertificates());
        certificates.add(certificate);
        return CertPathUtil.generateCertPath(certificates);
    }
}
