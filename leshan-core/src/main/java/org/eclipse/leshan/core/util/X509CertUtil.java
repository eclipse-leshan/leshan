package org.eclipse.leshan.core.util;

import javax.security.auth.x500.X500Principal;
import java.net.InetAddress;
import java.security.Principal;
import java.security.cert.CertificateParsingException;
import java.security.cert.X509Certificate;
import java.util.*;

/**
 * X.509 Certificate Utilities for accessing certificate details.
 */
public class X509CertUtil {
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
     * Helper for checking if given character is HEX character.
     *
     * @param ch Char to test
     * @return true if HEX char, false otherwise.
     */
    private static boolean isHex(char ch) {
        return (ch >= '0' && ch <= '9' || ch >= 'A' && ch <= 'F' || ch >= 'a' && ch <= 'f');
    }

    /**
     * Parses <a href="https://tools.ietf.org/html/rfc2253#section-3">RFC 2253 name string</a>.
     * <p>
     * Extracts field keys and their values in form as they are given in name string.
     *
     * @param name RFC 2253 name string
     * @return Map with field keys and their values
     */
    public static Map<String, String> parseRfc2253Name(String name) {
        Map<String, String> map = new HashMap<String, String>();

        // Parse RFC 2253 string
        boolean inValue = false;

        StringBuilder key = new StringBuilder();
        StringBuilder value = new StringBuilder();

        for (int i = 0; i < name.length(); i++) {
            char ch = name.charAt(i);

            if (!inValue) {
                if (ch == '=') {
                    inValue = true;
                    if (key.length() == 0)
                        throw new IllegalArgumentException("Key in RFC 2253 name cannot be empty");
                } else {
                    key.append(ch);
                }
            } else {
                if (ch == '\\') {
                    char nextCh = name.charAt(i + 1);
                    // Check for HEX encoding
                    if (isHex(nextCh) && isHex(name.charAt(i + 2))) {
                        int val = Integer.parseInt(name.substring(i + 1, i + 2), 16);
                        value.append((char) val);
                        i += 2;
                    } else {
                        value.append(nextCh);
                        i++;
                    }
                } else if (ch == ',' || ch == '+') {
                    inValue = false;

                    map.put(key.toString(), value.toString());

                    key = new StringBuilder();
                    value = new StringBuilder();
                } else {
                    value.append(ch);
                }
            }
        }

        if (key.length() > 0) {
            map.put(key.toString(), value.toString());
        }

        return map;
    }

    /**
     * Extracts field from given principal's name.
     * <p>
     * Notes: Can parse most name strings but HEX encoded DER values are returned back in DER HEX form. Notes: Only
     * understands X500Principal.
     *
     * @param principal Source principal.
     * @param field Field key as defined in RFC 2253.
     * @return null or value as string.
     */
    public static String getPrincipalField(Principal principal, String field) {
        if (principal instanceof X500Principal) {
            X500Principal x500Principal = (X500Principal) principal;

            // Extra practical OID that Java implementation hides for "not being standard" even thou
            // list in RFC is only example OIDs list (see JDK class AVAKeyword). These extra OID's are
            // actually defined in Sun packages but we should not access them as they are not standard
            // Java classes.
            HashMap<String, String> extraOids = new HashMap<String, String>();
            extraOids.put("2.5.4.5", "SERIALNUMBER");

            String dn = x500Principal.getName(X500Principal.RFC2253, extraOids);

            Map<String, String> fields = parseRfc2253Name(dn);

            if (fields.containsKey(field)) {
                return fields.get(field);
            }
        }

        return null;
    }

    /**
     * DNS name matcher
     * <p>
     * Supports matching with wildcard DNS names.
     *
     * @param matcher Matcher pattern
     * @param target Target DNS name to check
     * @return true if matches, false otherwise
     */
    private static boolean dnsNameMatch(String matcher, String target) {
        if (matcher.startsWith("*.")) {
            // Simple filtering out
            if (!target.endsWith(matcher.substring(1)))
                return false;

            // Wildcards only work in one sub level so no extra dots
            String host = target.substring(0, target.length() - (matcher.length() - 1));
            return host.indexOf('.') == -1;
        }
        return matcher.equals(target);
    }

    /**
     * Match DNS name against X.509 Certificate's Subjects
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
                        if (dnsNameMatch(value, dnsName)) {
                            return true;
                        }
                    }
                }

                // Strict Subject Alternative Name mode:
                // - Do not allow fallback to Subject DN matching
                return false;
            }

            // If subject alternative names are not present fallback to old ways at looking in Subject DN
            String cn = getPrincipalField(certificate.getSubjectX500Principal(), "CN");

            if (dnsNameMatch(cn, dnsName)) {
                return true;
            }
        } catch (CertificateParsingException e) {
            // Ignore exception and just return no match
        }
        return false;
    }

    /**
     * Match IP address against X.509 Certificate's Subjects
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
            String cn = getPrincipalField(certificate.getSubjectX500Principal(), "CN");

            if (hostAddress.equals(cn)) {
                return true;
            }
        } catch (CertificateParsingException e) {
            // Ignore exception and just return no match
        }
        return false;
    }
}
