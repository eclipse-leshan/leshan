/*******************************************************************************
 * Copyright (c) 2019 Bosch Software Innovations GmbH and others.
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
 *    Bosch Software Innovations - initial creation
 ******************************************************************************/
package org.eclipse.leshan.client.bootstrap;

import java.security.cert.CertificateParsingException;
import java.security.cert.X509Certificate;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// TODO TL : this is copy / paste from californium need to see if there is a better way to handle this.

/**
 * Certificate Path Utility.
 * <p>
 * Generates certificate path, check intended certificates usage and verify certificate paths.
 *
 * This implementation considers the below listed RFC's by:
 * <dl>
 * <dt>self-signed top-level certificate</dt>
 * <dd>Self-signed top-level certificate are removed before validation. This is done before sending such a certificate
 * path and before validating a received certificate path in order to support peers, which doesn't remove it.</dd>
 * <dt>intermediate authorities certificate</dt>
 * <dd>Intermediate authorities certificate are removed before validation. This is done before sending such a
 * certificate path, when a certificate authorities list was received before, and before validating a received
 * certificate path in order to support peers, which doesn't remove them.</dd>
 * </dl>
 *
 * References: <a href="https://tools.ietf.org/html/rfc5246#section-7.4.2" target= "_blank">RFC5246, Section 7.4.2,
 * Server Certificate</a>
 * <p>
 * "Because certificate validation requires that root keys be distributed independently, the self-signed certificate
 * that specifies the root certificate authority MAY be omitted from the chain, under the assumption that the remote end
 * must already possess it in order to validate it in any case."
 * </p>
 *
 * <a href="https://tools.ietf.org/html/rfc5246#section-7.4.6" target= "_blank">RFC5246, Section 7.4.6, Client
 * Certificate </a>
 * <p>
 * "If the certificate_authorities list in the certificate request message was non-empty, one of the certificates in the
 * certificate chain SHOULD be issued by one of the listed CAs."
 * </p>
 *
 * <a href="https://tools.ietf.org/html/rfc5280#section-6" target= "_blank">RFC5280, Section 6, Certification Path
 * Validation</a>
 * <p>
 * "Valid paths begin with certificates issued by a trust anchor." ... "The procedure performed to obtain this sequence
 * of certificates is outside the scope of this specification".
 * </p>
 *
 * @since 2.1
 */
public class CertPathUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(CertPathUtil.class);

    /**
     * OID for server authentication in extended key.
     */
    private static final String SERVER_AUTHENTICATION = "1.3.6.1.5.5.7.3.1";

    /**
     * OID for client authentication in extended key.
     */
    private static final String CLIENT_AUTHENTICATION = "1.3.6.1.5.5.7.3.2";

    /**
     * Bit for digital signature in key usage.
     */
    private static final int KEY_USAGE_SIGNATURE = 0;

    /**
     * Bit for certificate signing in key usage.
     */
    private static final int KEY_USAGE_CERTIFICATE_SIGNING = 5;

    /**
     * Check, if certificate is intended to be used to verify a signature of an other certificate.
     *
     * @param cert certificate to check.
     * @return {@code true}, if certificate is intended to be used to verify a signature of an other certificate,
     *         {@code false}, otherwise.
     */
    public static boolean canBeUsedToVerifySignature(X509Certificate cert) {

        if (cert.getBasicConstraints() < 0) {
            LOGGER.debug("certificate: {}, not for CA!", cert.getSubjectX500Principal());
            return false;
        }
        if ((cert.getKeyUsage() != null && !cert.getKeyUsage()[KEY_USAGE_CERTIFICATE_SIGNING])) {
            LOGGER.debug("certificate: {}, not for certificate signing!", cert.getSubjectX500Principal());
            return false;
        }
        return true;
    }

    /**
     * Check, if certificate is intended to be used for client or server authentication.
     *
     * @param cert certificate to check.
     * @param client {@code true} for client authentication, {@code false} for server authentication.
     * @return {@code true}, if certificate is intended to be used for client or server authentication, {@code false},
     *         otherwise.
     */
    public static boolean canBeUsedForAuthentication(X509Certificate cert, boolean client) {

        // KeyUsage is an optional extension which may be used to restrict
        // the way the key can be used.
        // https://tools.ietf.org/html/rfc5280#section-4.2.1.3
        // If this extension is used, we check if digitalsignature usage is
        // present.
        // (For more details see:
        // https://github.com/eclipse/californium/issues/748)
        if ((cert.getKeyUsage() != null && !cert.getKeyUsage()[KEY_USAGE_SIGNATURE])) {
            LOGGER.debug("certificate: {}, not for signing!", cert.getSubjectX500Principal());
            return false;
        }
        try {
            List<String> list = cert.getExtendedKeyUsage();
            if (list != null && !list.isEmpty()) {
                LOGGER.trace("certificate: {}", cert.getSubjectX500Principal());
                final String authentication = client ? CLIENT_AUTHENTICATION : SERVER_AUTHENTICATION;
                boolean foundUsage = false;
                for (String extension : list) {
                    LOGGER.trace("   extkeyusage {}", extension);
                    if (authentication.equals(extension)) {
                        foundUsage = true;
                    }
                }
                if (!foundUsage) {
                    LOGGER.debug("certificate: {}, not for {}!", cert.getSubjectX500Principal(),
                            client ? "client" : "server");
                    return false;
                }
            } else {
                LOGGER.debug("certificate: {}, no extkeyusage!", cert.getSubjectX500Principal());
            }
        } catch (CertificateParsingException e) {
            LOGGER.warn("x509 certificate:", e);
        }
        return true;
    }
}
