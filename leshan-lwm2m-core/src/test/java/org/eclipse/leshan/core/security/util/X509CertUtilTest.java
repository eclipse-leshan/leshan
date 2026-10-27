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
package org.eclipse.leshan.core.security.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import javax.security.auth.x500.X500Principal;

import org.eclipse.leshan.core.security.certificate.util.X509CertUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

class X509CertUtilTest {

    private final KeyStore keyStore;

    public X509CertUtilTest() {
        // prepare key store for access
        try {
            // Get certificates from key store
            char[] keyStorePwd = "secret".toCharArray();
            keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
            try (FileInputStream keyStoreFile = new FileInputStream("./certificates/certificates.jks")) {
                keyStore.load(keyStoreFile, keyStorePwd);
            }
        } catch (GeneralSecurityException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Loads X.509 PEM Certificate
     *
     * @param filename File name to load
     * @return Valid X.509 Certificate object or null
     */
    private X509Certificate loadX509PemCertificate(String filename) {
        X509Certificate certificate = null;

        try {
            CertificateFactory fact = CertificateFactory.getInstance("X.509");
            FileInputStream is = new FileInputStream(filename);
            certificate = (X509Certificate) fact.generateCertificate(is);
        } catch (CertificateException | FileNotFoundException e) {
            e.printStackTrace();
        }

        return certificate;
    }

    @Test
    void x509_simple_dns_name_test() throws KeyStoreException {
        X509Certificate certificate = (X509Certificate) keyStore.getCertificate("server");

        assertTrue(X509CertUtil.matchSubjectDnsName(certificate, "server.mydomain.com"));
        assertFalse(X509CertUtil.matchSubjectDnsName(certificate, "another.domain.com"));
        assertFalse(X509CertUtil.matchSubjectDnsName(certificate, "localhost"));
    }

    @Test
    void x509_san_dns_name_test() throws KeyStoreException {
        X509Certificate certificate = (X509Certificate) keyStore.getCertificate("server_with_san");

        assertTrue(X509CertUtil.matchSubjectDnsName(certificate, "server.mydomain.com"));
        assertTrue(X509CertUtil.matchSubjectDnsName(certificate, "SERVER.MyDomain.com"));
        assertFalse(X509CertUtil.matchSubjectDnsName(certificate, "another.domain.com"));
        assertFalse(X509CertUtil.matchSubjectDnsName(certificate, "localhost"));
    }

    @Test
    void x509_san_ipv4_test() throws KeyStoreException, UnknownHostException {
        X509Certificate certificate = (X509Certificate) keyStore.getCertificate("server_with_san");

        assertTrue(X509CertUtil.matchSubjectInetAddress(certificate, InetAddress.getByName("192.168.1.42")));
        assertFalse(X509CertUtil.matchSubjectInetAddress(certificate, InetAddress.getByName("127.0.0.1")));
        assertFalse(X509CertUtil.matchSubjectInetAddress(certificate,
                InetAddress.getByName("2001:0db8:85a3:0000:0000:8a2e:0370:7334")));
        assertFalse(X509CertUtil.matchSubjectInetAddress(certificate, InetAddress.getByName("2001:db8:1234::")));
        assertFalse(X509CertUtil.matchSubjectInetAddress(certificate, InetAddress.getByName("::1")));
    }

    @Test
    void x509_san_ipv6_test() throws KeyStoreException, UnknownHostException {
        X509Certificate certificate = (X509Certificate) keyStore.getCertificate("server_with_ipv6");

        assertFalse(X509CertUtil.matchSubjectInetAddress(certificate, InetAddress.getByName("192.168.1.42")));
        assertFalse(X509CertUtil.matchSubjectInetAddress(certificate, InetAddress.getByName("127.0.0.1")));
        assertTrue(X509CertUtil.matchSubjectInetAddress(certificate,
                InetAddress.getByName("2001:0db8:85a3:0000:0000:8a2e:0370:7334")));
        assertFalse(X509CertUtil.matchSubjectInetAddress(certificate, InetAddress.getByName("2001:db8:1234::")));
        assertFalse(X509CertUtil.matchSubjectInetAddress(certificate, InetAddress.getByName("::1")));
    }

    @Test
    void x509_san_combo_test() throws KeyStoreException, UnknownHostException {
        X509Certificate certificate = (X509Certificate) keyStore.getCertificate("localhost");

        assertFalse(X509CertUtil.matchSubjectDnsName(certificate, "server.mydomain.com"));
        assertFalse(X509CertUtil.matchSubjectDnsName(certificate, "another.domain.com"));
        assertTrue(X509CertUtil.matchSubjectDnsName(certificate, "localhost"));

        assertFalse(X509CertUtil.matchSubjectInetAddress(certificate, InetAddress.getByName("192.168.1.42")));
        assertTrue(X509CertUtil.matchSubjectInetAddress(certificate, InetAddress.getByName("127.0.0.1")));
        assertFalse(X509CertUtil.matchSubjectInetAddress(certificate,
                InetAddress.getByName("2001:0db8:85a3:0000:0000:8a2e:0370:7334")));
        assertFalse(X509CertUtil.matchSubjectInetAddress(certificate, InetAddress.getByName("2001:db8:1234::")));
        assertTrue(X509CertUtil.matchSubjectInetAddress(certificate, InetAddress.getByName("::1")));
    }

    @Test
    void x509_san_wildcard_test() {
        X509Certificate certificate = loadX509PemCertificate("./certificates/eclipse.org.pem");

        assertTrue(X509CertUtil.matchSubjectDnsName(certificate, "eclipse.org"));
        assertTrue(X509CertUtil.matchSubjectDnsName(certificate, "server.eclipse.org"));
        assertFalse(X509CertUtil.matchSubjectDnsName(certificate, "server.subdomain.eclipse.org"));
        assertFalse(X509CertUtil.matchSubjectDnsName(certificate, "sub.server.eclipse.org"));
    }

    @Test
    void dns_wildcard_matching_test() {
        // match
        assertTrue(X509CertUtil.dnsNameMatch("*.eclipse.org", "server.eclipse.org", false));
        assertFalse(X509CertUtil.dnsNameMatch("*.eclipse.org", "server.eclipse.org", true));

        // doesn't match
        assertFalse(X509CertUtil.dnsNameMatch("*.eclipse.org", "eclipse.org", false));
        assertFalse(X509CertUtil.dnsNameMatch("*.eclipse.org", "server.subdomain.eclipse.org", false));
        assertFalse(X509CertUtil.dnsNameMatch("*.eclipse.org", "sub.server.eclipse.org", false));
        assertFalse(X509CertUtil.dnsNameMatch("*.eclipse.org", ".eclipse.org", false));

        // invalid matcher
        assertFalse(X509CertUtil.dnsNameMatch("*", "eclipse.org", false));
        assertFalse(X509CertUtil.dnsNameMatch("*.", "eclipse.org", false));
        assertFalse(X509CertUtil.dnsNameMatch("s*r.eclipse.org", "server.eclipse.org", false));
        assertFalse(X509CertUtil.dnsNameMatch("*ver.eclipse.org", "server.eclipse.org", false));
        assertFalse(X509CertUtil.dnsNameMatch("server.*.org", "server.eclipse.org", false));
        assertFalse(X509CertUtil.dnsNameMatch("*.org", "eclipse.org", false));
        assertFalse(X509CertUtil.dnsNameMatch("*.*.eclipse.org", "sub.server.eclipse.org", false));
    }

    /** Self-signed, no Subject Alternative Name, subject: CN=server.example.com, O=Acme. */
    private static final String CERT_NO_SAN_ONE_CN = "-----BEGIN CERTIFICATE-----\n" //
            + "MIIBcDCCARegAwIBAgIIO7PeiMpblrswCgYIKoZIzj0EAwMwLDENMAsGA1UEChME\n" //
            + "QWNtZTEbMBkGA1UEAxMSc2VydmVyLmV4YW1wbGUuY29tMCAXDTI2MTAyNjE2NDgy\n" //
            + "NVoYDzIxMjYxMDAyMTY0ODI1WjAsMQ0wCwYDVQQKEwRBY21lMRswGQYDVQQDExJz\n" //
            + "ZXJ2ZXIuZXhhbXBsZS5jb20wWTATBgcqhkjOPQIBBggqhkjOPQMBBwNCAAT4Gl2/\n" //
            + "Z358LXsbdC5AFVWbf0/ZMIkxmt9BCyWaIy/rrTy+SRDetIUPG5Fma7sy8qN3rHoS\n" //
            + "Uy3NWaGIsXocStBdoyEwHzAdBgNVHQ4EFgQUzrZPoweBDOTKruH3y+yD4kTzQdsw\n" //
            + "CgYIKoZIzj0EAwMDRwAwRAIgKmtbFk/aPhh4YLmWPAuZv77hj18Yq8ZjObBVIHuD\n" //
            + "OsUCIHmm3t52tvRR318RcdwbAN7b6QROrL3hy44CRq1c8z6M\n" //
            + "-----END CERTIFICATE-----\n";

    /** Self-signed, no Subject Alternative Name, subject: CN=server.example.com, CN=other.example.com, O=Acme. */
    private static final String CERT_NO_SAN_TWO_CN = "-----BEGIN CERTIFICATE-----\n" //
            + "MIIBqDCCAU+gAwIBAgIIUmiut62BSTAwCgYIKoZIzj0EAwMwSDENMAsGA1UEChME\n" //
            + "QWNtZTEaMBgGA1UEAxMRb3RoZXIuZXhhbXBsZS5jb20xGzAZBgNVBAMTEnNlcnZl\n" //
            + "ci5leGFtcGxlLmNvbTAgFw0yNjEwMjYxNjQ4MjdaGA8yMTI2MTAwMjE2NDgyN1ow\n" //
            + "SDENMAsGA1UEChMEQWNtZTEaMBgGA1UEAxMRb3RoZXIuZXhhbXBsZS5jb20xGzAZ\n" //
            + "BgNVBAMTEnNlcnZlci5leGFtcGxlLmNvbTBZMBMGByqGSM49AgEGCCqGSM49AwEH\n" //
            + "A0IABG2UVIInRK8+KwPQ7A6YKxrlVkY6HJUvB8tK7KCXnBGU6JNmlWpUIS+I+8O+\n" //
            + "f1MPgnCLLpVfX44QJzJr7E2cT5ajITAfMB0GA1UdDgQWBBTAS2dvLb0/eEg1JREV\n" //
            + "zFOKAbN2TzAKBggqhkjOPQQDAwNHADBEAiBEcVv4AS44ufASEj0IPIOwIjhIjaTA\n" //
            + "c+zoFJvpT0WWFQIgZIP1RjR0Cpl8AT1S563ICe0Alpw1wuziFspbAJ0QOwY=\n" //
            + "-----END CERTIFICATE-----\n";

    /** Self-signed, no Subject Alternative Name, subject: CN=*.example.com, O=Acme. */
    private static final String CERT_NO_SAN_WILDCARD_CN = "-----BEGIN CERTIFICATE-----\n" //
            + "MIIBZzCCAQ2gAwIBAgIILAwiZi6sCCMwCgYIKoZIzj0EAwMwJzENMAsGA1UEChME\n" //
            + "QWNtZTEWMBQGA1UEAwwNKi5leGFtcGxlLmNvbTAgFw0yNjEwMjYxNjUwNDBaGA8y\n" //
            + "MTI2MTAwMjE2NTA0MFowJzENMAsGA1UEChMEQWNtZTEWMBQGA1UEAwwNKi5leGFt\n" //
            + "cGxlLmNvbTBZMBMGByqGSM49AgEGCCqGSM49AwEHA0IABBqGDO7p3gecD7gUEeg8\n" //
            + "MeNfk2Bd6lUXyd1kTsqReTJFi9y4scFPxU7s+SZnf/ZAnn7gn6MTzSy/IV8fVxii\n" //
            + "itCjITAfMB0GA1UdDgQWBBRD1nkYtWDiP0wTbKFlHOE7wt6gvDAKBggqhkjOPQQD\n" //
            + "AwNIADBFAiEAt51fEITbvkjRrc7AaJ/F1Ignt6oke5WyQ/P7MaUDC30CIEe9Ty4m\n" //
            + "N4iVwlZg4ldNIj4dPYEERZVPx2Ur/i7XuWA2\n" //
            + "-----END CERTIFICATE-----\n";

    /** Self-signed, no Subject Alternative Name, subject: OU=Acme (no CN). */
    private static final String CERT_NO_SAN_NO_CN = "-----BEGIN CERTIFICATE-----\n" //
            + "MIIBNzCB3qADAgECAgkA7YiULQB3uGgwCgYIKoZIzj0EAwMwDzENMAsGA1UECxME\n" //
            + "QWNtZTAgFw0yNjEwMjYxNjQ4MjlaGA8yMTI2MTAwMjE2NDgyOVowDzENMAsGA1UE\n" //
            + "CxMEQWNtZTBZMBMGByqGSM49AgEGCCqGSM49AwEHA0IABIsI6O272QF6gbkm5k6H\n" //
            + "I8x/+58Pwrk66vql1a/YCYYf6MRfGPdDDSr4tjR0kgzlB2RZ4210RY/K1IXE9Jjg\n" //
            + "ONWjITAfMB0GA1UdDgQWBBRReGIpJIcnTv5t+kFuDmvIjF3tmTAKBggqhkjOPQQD\n" //
            + "AwNIADBFAiEAnbYMH6MNAEZW3Gh3iQjl/J6rA9sjOCC1gTU4D9Z3zUICIBsEUibj\n" //
            + "I5eGS3gTlBrVdFMHt92uTIjzlLBwBuUSIPTr\n" //
            + "-----END CERTIFICATE-----\n";

    /** Self-signed, no Subject Alternative Name, subject: CN=127.0.0.1, O=Acme. */
    private static final String CERT_NO_SAN_IP_CN = "-----BEGIN CERTIFICATE-----\n" //
            + "MIIBYDCCAQagAwIBAgIJAPbJCYkceG9PMAoGCCqGSM49BAMDMCMxDTALBgNVBAoT\n" //
            + "BEFjbWUxEjAQBgNVBAMTCTEyNy4wLjAuMTAgFw0yNjEwMjYxNjQ4MzFaGA8yMTI2\n" //
            + "MTAwMjE2NDgzMVowIzENMAsGA1UEChMEQWNtZTESMBAGA1UEAxMJMTI3LjAuMC4x\n" //
            + "MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEfHTqzAPAVH4z61U5UApsmCw9sQly\n" //
            + "1T5hhqhw5BZsNTgL4PO9p7wBhfPersogWI2QfAFhbJyWkr9jpdYqNaYYTKMhMB8w\n" //
            + "HQYDVR0OBBYEFBeMxLkF8pDwBQzyXX0umnBDPe3zMAoGCCqGSM49BAMDA0gAMEUC\n" //
            + "IQC+SFdxTlu5iEAok/8Sgk/9ctnVJC95G6n5+AKVQJKYIAIga6fhq1PZKEuXdBsu\n" //
            + "lPhkFFat9lOTPicU1fqCgzDeMD0=\n" //
            + "-----END CERTIFICATE-----\n";

    /** Self-signed, no Subject Alternative Name, subject: CN=127.0.0.1, CN=127.0.0.2, O=Acme. */
    private static final String CERT_NO_SAN_TWO_IP_CN = "-----BEGIN CERTIFICATE-----\n" //
            + "MIIBiDCCAS6gAwIBAgIJAKr5Ir8uxaa1MAoGCCqGSM49BAMDMDcxDTALBgNVBAoT\n" //
            + "BEFjbWUxEjAQBgNVBAMTCTEyNy4wLjAuMjESMBAGA1UEAxMJMTI3LjAuMC4xMCAX\n" //
            + "DTI2MTAyNjE2NDgzM1oYDzIxMjYxMDAyMTY0ODMzWjA3MQ0wCwYDVQQKEwRBY21l\n" //
            + "MRIwEAYDVQQDEwkxMjcuMC4wLjIxEjAQBgNVBAMTCTEyNy4wLjAuMTBZMBMGByqG\n" //
            + "SM49AgEGCCqGSM49AwEHA0IABMLiIakiSB5ufh19Rg84mSsxQRJp+YrRh2jOAhlk\n" //
            + "OrrgQ2BnxV84sww39vFOrfULHR+XySkeQ47F/6OgFWb2eVKjITAfMB0GA1UdDgQW\n" //
            + "BBQZJHVQ5QutcgUUrrSdeNzHqOb1jTAKBggqhkjOPQQDAwNIADBFAiBnzdlKjbNr\n" //
            + "CFOO/szKxtEp7dxRleDDuAwW+LruMiTTBwIhANzpvZFJcc4QEHCDB4Mxf8E6nu2V\n" //
            + "vcMCtJ4i0sDIGR9m\n" //
            + "-----END CERTIFICATE-----\n";

    // Without Subject Alternative Name, the common name of the subject is used, if there is exactly one.

    @Test
    void x509_dns_name_falls_back_to_the_cn_if_there_is_no_san() throws Exception {
        X509Certificate certificate = parsePem(CERT_NO_SAN_ONE_CN);

        assertTrue(X509CertUtil.matchSubjectDnsName(certificate, "server.example.com"));
        assertFalse(X509CertUtil.matchSubjectDnsName(certificate, "other.example.com"));
    }

    @Test
    void x509_dns_name_does_not_fall_back_to_a_cn_if_there_are_several() throws Exception {
        X509Certificate certificate = parsePem(CERT_NO_SAN_TWO_CN);

        assertFalse(X509CertUtil.matchSubjectDnsName(certificate, "server.example.com"));
        assertFalse(X509CertUtil.matchSubjectDnsName(certificate, "other.example.com"));
    }

    @Test
    void x509_dns_name_does_not_match_a_certificate_without_san_and_cn() throws Exception {
        assertFalse(X509CertUtil.matchSubjectDnsName(parsePem(CERT_NO_SAN_NO_CN), "server.example.com"));
    }

    @Test
    void x509_dns_name_does_not_use_a_wildcard_in_the_cn_if_there_is_no_san() throws Exception {
        // wildcard matching is only for the SAN, see https://www.rfc-editor.org/info/rfc7252/#section-9.1.3.3
        assertFalse(X509CertUtil.matchSubjectDnsName(parsePem(CERT_NO_SAN_WILDCARD_CN), "server.example.com"));
    }

    @Test
    void x509_ip_address_falls_back_to_the_cn_if_there_is_no_san() throws Exception {
        X509Certificate certificate = parsePem(CERT_NO_SAN_IP_CN);

        assertTrue(X509CertUtil.matchSubjectInetAddress(certificate, InetAddress.getByName("127.0.0.1")));
        assertFalse(X509CertUtil.matchSubjectInetAddress(certificate, InetAddress.getByName("127.0.0.2")));
    }

    @Test
    void x509_ip_address_does_not_fall_back_to_a_cn_if_there_are_several() throws Exception {
        X509Certificate certificate = parsePem(CERT_NO_SAN_TWO_IP_CN);

        assertFalse(X509CertUtil.matchSubjectInetAddress(certificate, InetAddress.getByName("127.0.0.1")));
        assertFalse(X509CertUtil.matchSubjectInetAddress(certificate, InetAddress.getByName("127.0.0.2")));
    }

    @Test
    void x509_ip_address_does_not_match_a_certificate_without_san_and_cn() throws Exception {
        assertFalse(
                X509CertUtil.matchSubjectInetAddress(parsePem(CERT_NO_SAN_NO_CN), InetAddress.getByName("127.0.0.1")));
    }

    static Stream<Arguments> commonNamesToKeepAsIs() {
        return Stream.of(//
                arguments("simple", "device-1"), //
                arguments("urn", "urn:imei:123456789012345"), //
                // characters which are special in a distinguished name
                arguments("comma", "dev,ice"), //
                arguments("plus", "a+b"), //
                arguments("equals", "a=b"), //
                arguments("semicolon", "a;b"), //
                arguments("angle brackets", "<a>"), //
                arguments("quotes", "say \"hi\""), //
                arguments("backslash", "back\\slash"), //
                arguments("leading #", "#hash"), //
                arguments("leading space", " lead"), //
                arguments("trailing space", "trail "), //
                // a CN which looks like several attributes
                arguments("looks like 2 attributes", "x,CN=victim"), //
                // non ASCII, including a character outside the BMP
                arguments("non ASCII", "\u010dlovek-\u4e2d"), //
                arguments("outside BMP", "smile-\uD83D\uDE00"), //
                // DER length encodings: 1 octet, 2 octets, 3 octets
                arguments("200 characters", repeat('a', 200)), //
                arguments("300 characters", repeat('a', 300)), //
                arguments("70000 characters", repeat('a', 70_000)));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("commonNamesToKeepAsIs")
    void create_x500principal_keeps_the_common_name_as_is(String label, String commonName) {
        X500Principal principal = X509CertUtil.createX500Principal(commonName);

        assertEquals(commonName, X509CertUtil.extractCN(principal));
    }

    @Test
    void create_x500principal_around_the_boundaries_of_the_der_length_encoding() {
        // The length of a DER element switches from 1 octet to 2 octets at 128, and to 3 octets at 65536. The length
        // of the elements around the common name also grow with its length, so check a range around each switch.
        for (int length : intRanges(100, 140, 250, 260, 65_530, 65_540)) {
            String commonName = repeat('a', length);

            assertEquals(commonName, X509CertUtil.extractCN(X509CertUtil.createX500Principal(commonName)),
                    "common name of " + length + " characters");
        }
    }

    @ParameterizedTest
    @ValueSource(strings = { "a\uD800b", "\uDC00", "x\uD83D" })
    void create_x500principal_rejects_unpaired_surrogate(String commonName) {
        // a malformed string must not be silently replaced by another value
        assertThrows(IllegalArgumentException.class, () -> X509CertUtil.createX500Principal(commonName));
    }

    @Test
    void create_x500principal_rejects_null() {
        assertThrows(IllegalArgumentException.class, () -> X509CertUtil.createX500Principal(null));
    }

    @ParameterizedTest
    @ValueSource(strings = { "", "a\u0000b", "a\nb" })
    void create_x500principal_leaves_to_extractCN_the_decision_to_use_the_common_name(String commonName) {
        X500Principal principal = X509CertUtil.createX500Principal(commonName);

        assertThrows(IllegalStateException.class, () -> X509CertUtil.extractCN(principal));
    }

    private static String repeat(char c, int count) {
        char[] chars = new char[count];
        Arrays.fill(chars, c);
        return new String(chars);
    }

    private static int[] intRanges(int... fromTo) {
        return IntStream.range(0, fromTo.length / 2)
                .flatMap(i -> IntStream.rangeClosed(fromTo[2 * i], fromTo[2 * i + 1])).toArray();
    }

    private static X509Certificate parsePem(String pem) throws CertificateException {
        return (X509Certificate) CertificateFactory.getInstance("X.509")
                .generateCertificate(new ByteArrayInputStream(pem.getBytes(StandardCharsets.US_ASCII)));
    }
}
