/*******************************************************************************
 * Copyright (c) 2022    Sierra Wireless and others.
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

import java.net.InetAddress;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateParsingException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

import javax.security.auth.x500.X500Principal;

import org.eclipse.leshan.core.util.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * X.509 Certificate Utilities.
 */
public class X509CertUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(X509CertUtil.class);

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

    /**
     * Creates a modifiable x509 certificates list from provided certificates list.
     *
     * @param certificates certificates list
     * @return created modifiable x509 certificates list
     * @throws NullPointerException if the certificate list is {@code null}.
     * @throws IllegalArgumentException if a certificate is provided, which is no x509 certificate.
     */
    public static List<X509Certificate> toX509CertificatesList(List<? extends Certificate> certificates) {
        if (certificates == null) {
            throw new NullPointerException("Certificates list must not be null!");
        }
        List<X509Certificate> chain = new ArrayList<>(certificates.size());
        for (Certificate cert : certificates) {
            if (!(cert instanceof X509Certificate)) {
                throw new IllegalArgumentException("Given certificate is not X.509!" + cert.getClass());
            }
            chain.add((X509Certificate) cert);
        }
        return chain;
    }

    /**
     * Convert array of {@link Certificate} to array of {@link X509Certificate}
     */
    public static X509Certificate[] asX509Certificates(Certificate... certificates) throws CertificateException {
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

    /**
     * <a href="https://tools.ietf.org/html/rfc3280#section-4.2.1.7">rfc3280#section-4.2.1.7</a> - GeneralName
     */
    public enum GeneralName {

        /**
         * otherName [0] OtherName
         */
        OTHER_NAME(0),

        /**
         * rfc822Name [1] IA5String
         */
        RFC822_NAME(1),

        /**
         * dNSName [2] IA5String
         */
        DNS_NAME(2),

        /**
         * x400Address [3] ORAddress
         */
        X400_ADDRESS(3),

        /**
         * directoryName [4] Name
         */
        DIRECTORY_NAME(4),

        /**
         * ediPartyName [5] EDIPartyName
         */
        EDI_PARTY_NAME(5),

        /**
         * uniformResourceIdentifier [6] IA5String
         */
        UNIFORM_RESOURCE_IDENTIFIER(6),

        /**
         * iPAddress [7] OCTET STRING
         */
        IP_ADDRESS(7),

        /**
         * registeredID [8] OBJECT IDENTIFIER
         */
        REGISTERED_ID(8);

        /**
         * The code value.
         */
        public final int value;

        /**
         * Instantiates a new code with the specified code value.
         *
         * @param value the integer value of the code
         */
        private GeneralName(final int value) {
            this.value = value;
        }

        /**
         * Converts the specified integer value to a general name code.
         *
         * @param value the integer value
         * @return the general name code
         * @throws IllegalArgumentException if the integer value does not represent a valid general name code.
         */
        public static GeneralName valueOf(final int value) {
            switch (value) {
            case 0:
                return OTHER_NAME;
            case 1:
                return RFC822_NAME;
            case 2:
                return DNS_NAME;
            case 3:
                return X400_ADDRESS;
            case 4:
                return DIRECTORY_NAME;
            case 5:
                return EDI_PARTY_NAME;
            case 6:
                return UNIFORM_RESOURCE_IDENTIFIER;
            case 7:
                return IP_ADDRESS;
            case 8:
                return REGISTERED_ID;
            default:
                throw new IllegalArgumentException(String.format("Unknown GeneralName class code: %d", value));
            }
        }
    }

    /**
     * Extract "common name" from "distinguished name" from {@link X509Certificate}
     *
     * @param certificate The certificate.
     * @return The extracted common name.
     * @throws IllegalStateException if the subject has no usable CN: none, more than one, empty, containing a control
     *         character or not encoded as a supported string type.
     */
    public static String extractCN(X509Certificate certificate) {
        return extractCN(certificate.getSubjectX500Principal());
    }

    /**
     * Extract "common name" from "distinguished name" from {@link X500Principal}
     *
     * @param principal The X500Principal.
     * @return The extracted common name.
     * @throws IllegalStateException if the subject has no usable CN: none, more than one, empty, containing a control
     *         character or not encoded as a supported string type.
     */
    public static String extractCN(X500Principal principal) {
        return DerCommonName.extractCN(principal);
    }

    /**
     * Create an {@link X500Principal} which only contains the given "common name".
     * <p>
     * The value is DER encoded, so it is kept as is (nothing to escape, nothing trimmed) and
     * {@link #extractCN(X500Principal)} gives it back. Do not build it with <code>new X500Principal("CN=" + cn)</code>:
     * that parses <code>cn</code> as a distinguished name.
     *
     * @param commonName The common name.
     * @return The principal.
     * @throws IllegalArgumentException if the common name can not be encoded.
     */
    public static X500Principal createX500Principal(String commonName) {
        Validate.notNull(commonName, "commonName must not be null");
        return new X500Principal(DerCommonName.encodeName(commonName));
    }

    /**
     * DNS name matcher
     * <p>
     * Supports matching with wildcard DNS names.
     *
     * @param matcher Matcher pattern
     * @param target Target DNS name to check
     * @param noWildcard set to true to skip wildcard matching
     * @return true if matches, false otherwise
     */
    public static boolean dnsNameMatch(String matcher, String target, boolean noWildcard) {
        // DNS matching is case-insensitive : https://www.rfc-editor.org/info/rfc6125/#section-6.4.1
        matcher = matcher.toLowerCase(Locale.ROOT);
        target = target.toLowerCase(Locale.ROOT);

        if (!noWildcard && matcher.startsWith("*.")) {
            // See wildcard matching constraint :
            // https://www.rfc-editor.org/info/rfc6125/#section-6.4.3
            // https://www.rfc-editor.org/info/rfc6125/#section-7.2
            // https://errata.rfc-editor.org/search/?rfc_number=6125&presentation=records

            // Wildcard pattern must have at least 3 labels (*.foo.tld)
            // to prevent over-broad wildcards like *.com
            if (matcher.indexOf('.', 2) == -1)
                return false;

            // Simple filtering out
            if (!target.endsWith(matcher.substring(1)))
                return false;

            // Wildcards only work in one sub level so no extra dots,
            // and the leftmost label must be non-empty (RFC 6125: wildcard
            // must match at least one character)
            String host = target.substring(0, target.length() - (matcher.length() - 1));
            return !host.isEmpty() && host.indexOf('.') == -1;
        }
        return matcher.equals(target);
    }

    /**
     * Match DNS name against X.509 Certificate's Subjects
     * <p>
     * If the certificate has no Subject Alternative Name, the common name of its subject is used instead. Nothing
     * matches if this common name is missing or ambiguous (see {@link #extractCN(X509Certificate)}).
     *
     * @param certificate Target X.509 certificate
     * @param dnsName DNS name to match
     * @return True if match, false otherwise
     */
    public static boolean matchSubjectDnsName(X509Certificate certificate, String dnsName) {
        try {
            // First one need to check SANs if they are present
            Collection<List<?>> sans = certificate.getSubjectAlternativeNames();

            if (sans != null) {
                for (List<?> san : sans) {
                    int generalName = (Integer) san.get(0);
                    if (generalName == GeneralName.DNS_NAME.value) {
                        String value = (String) san.get(1);
                        if (dnsNameMatch(value, dnsName, false /* wildcard allowed */)) {
                            return true;
                        }
                    }
                }

                // Strict Subject Alternative Name mode:
                // - Do not allow fallback to Subject DN matching
                return false;
            }

            // If subject alternative names are not present fallback to old ways at looking in Subject DN
            String cn = getCommonNameOrNull(certificate);

            if (cn != null && dnsNameMatch(cn, dnsName, true/* no wildcard matching */)) {
                // no wildcard matching, see : https://www.rfc-editor.org/info/rfc7252/#section-9.1.3.3
                return true;
            }
        } catch (CertificateParsingException e) {
            // Ignore exception and just return no match
        }
        return false;
    }

    /**
     * Match IP address against X.509 Certificate's Subjects
     * <p>
     * If the certificate has no Subject Alternative Name, the common name of its subject is used instead. Nothing
     * matches if this common name is missing or ambiguous (see {@link #extractCN(X509Certificate)}).
     *
     * @param certificate Target X.509 certificate
     * @param address IP address to match
     * @return True if match, false otherwise
     */
    public static boolean matchSubjectInetAddress(X509Certificate certificate, InetAddress address) {
        try {
            String hostAddress = address.getHostAddress();

            // First one need to check SANs if they are present
            Collection<List<?>> sans = certificate.getSubjectAlternativeNames();

            if (sans != null) {
                for (List<?> san : sans) {
                    int generalName = (Integer) san.get(0);
                    if (generalName == GeneralName.IP_ADDRESS.value) {
                        String value = (String) san.get(1);
                        if (hostAddress.equals(value)) {
                            return true;
                        }
                    }
                }

                // Strict Subject Alternative Name mode:
                // - Do not allow fallback to Subject DN matching
                return false;
            }

            // If subject alternative names are not present fallback to old ways at looking in Subject DN
            String cn = getCommonNameOrNull(certificate);

            if (hostAddress.equals(cn)) {
                return true;
            }
        } catch (CertificateParsingException e) {
            // Ignore exception and just return no match
        }
        return false;
    }

    /**
     * @return the common name of the subject of the certificate, or null if it has none or if it is ambiguous.
     */
    private static String getCommonNameOrNull(X509Certificate certificate) {
        try {
            return extractCN(certificate);
        } catch (IllegalStateException e) {
            return null;
        }
    }
}
