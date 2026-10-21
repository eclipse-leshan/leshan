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
package org.eclipse.leshan.core.security.certificate.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.stream.Stream;

import javax.naming.NamingException;
import javax.naming.ldap.LdapName;
import javax.naming.ldap.Rdn;
import javax.security.auth.x500.X500Principal;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class DerCommonNameTest {

    // ASN.1 universal tags
    private static final int INTEGER = 0x02;
    private static final int OID = 0x06;
    private static final int UTF8_STRING = 0x0c;
    private static final int PRINTABLE_STRING = 0x13;
    private static final int TELETEX_STRING = 0x14;
    private static final int IA5_STRING = 0x16;
    private static final int UNIVERSAL_STRING = 0x1c;
    private static final int BMP_STRING = 0x1e;
    private static final int CONSTRUCTED_UTF8_STRING = 0x2c;
    private static final int SEQUENCE = 0x30;
    private static final int SET = 0x31;

    // attribute types
    private static final byte[] CN = oid(0x55, 0x04, 0x03);
    private static final byte[] OU = oid(0x55, 0x04, 0x0b);
    private static final byte[] O = oid(0x55, 0x04, 0x0a);
    private static final byte[] L = oid(0x55, 0x04, 0x07);

    private static final long SEED = 20261020L;

    // ------------------------------------------------------------------------------------------------------------
    // names that must give a common name
    // ------------------------------------------------------------------------------------------------------------

    static Stream<Arguments> validNames() {
        return Stream.of(//
                arguments("single CN", name(rdn(utf8(CN, "device-1"))), "device-1"), //
                arguments("CN with a URN", name(rdn(utf8(CN, "urn:imei:123456789012345"))), //
                        "urn:imei:123456789012345"), //
                arguments("text 'CN=' in an OU, encoded after the CN",
                        name(rdn(utf8(CN, "real")), rdn(utf8(OU, "CN=victim,x"))), "real"), //
                arguments("text 'CN=' in an OU, encoded before the CN",
                        name(rdn(utf8(OU, "CN=victim,x")), rdn(utf8(CN, "real"))), "real"), //
                arguments("text 'CN=' in O and L",
                        name(rdn(utf8(O, ",CN=victim,")), rdn(utf8(CN, "real")), rdn(utf8(L, "CN=victim"))), "real"), //
                arguments("CN in a multi-valued RDN with an OU", name(rdn(utf8(CN, "real"), utf8(OU, "CN=victim"))),
                        "real"), //
                arguments("PrintableString", name(rdn(ava(CN, PRINTABLE_STRING, ascii("dev-1")))), "dev-1"), //
                arguments("BMPString", name(rdn(ava(CN, BMP_STRING, "d\u00e9v".getBytes(StandardCharsets.UTF_16BE)))),
                        "d\u00e9v"), //
                arguments("UniversalString",
                        name(rdn(ava(CN, UNIVERSAL_STRING, "d\u00e9v".getBytes(Charset.forName("UTF-32BE"))))),
                        "d\u00e9v"), //
                arguments("UTF8String with non-ASCII characters", name(rdn(utf8(CN, "\u010dlovek-\u4e2d"))),
                        "\u010dlovek-\u4e2d"), //
                arguments("DN special characters are kept verbatim", name(rdn(utf8(CN, "dev,ice=x\"y\\z"))),
                        "dev,ice=x\"y\\z"), //
                arguments("attribute of a non-string type is ignored",
                        name(rdn(ava(OU, INTEGER, new byte[] { 7 })), rdn(utf8(CN, "real"))), "real"), //

                // documented as "deliberately not checked": these pin that behavior
                arguments("PrintableString with '*' (repertoire not enforced)",
                        name(rdn(ava(CN, PRINTABLE_STRING, ascii("*.example.com")))), "*.example.com"), //
                arguments("SET OF not sorted, order 1 (not enforced)", name(rdn(utf8(OU, "x"), utf8(CN, "real"))),
                        "real"), //
                arguments("SET OF not sorted, order 2 (not enforced)", name(rdn(utf8(CN, "real"), utf8(OU, "x"))),
                        "real"), //
                arguments("200 characters (RFC 5280 limit of 64 not enforced)", name(rdn(utf8(CN, repeat('a', 200)))),
                        repeat('a', 200)));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("validNames")
    void extract_common_name_of_valid_name(String label, byte[] der, String expectedCommonName) {
        assertEquals(expectedCommonName, DerCommonName.extractCN(der));
    }

    // ------------------------------------------------------------------------------------------------------------
    // names that must be rejected, and for the right reason
    // ------------------------------------------------------------------------------------------------------------

    static Stream<Arguments> invalidNames() {
        byte[] valid = name(rdn(utf8(CN, "real")));

        return Stream.of(//
                // number of CN
                arguments("two CN in two RDNs", name(rdn(utf8(CN, "attacker")), rdn(utf8(CN, "victim"))),
                        "more than one CN"), //
                arguments("two CN in one multi-valued RDN", name(rdn(utf8(CN, "a"), utf8(CN, "b"))),
                        "more than one CN"), //
                arguments("two identical CN in one multi-valued RDN", name(rdn(utf8(CN, "a"), utf8(CN, "a"))),
                        "more than one CN"), //
                arguments("only an OU", name(rdn(utf8(OU, "x"))), "no CN in name"), //
                arguments("only an OU containing the text 'CN=victim-device'", name(rdn(utf8(OU, "CN=victim-device"))),
                        "no CN in name"), //
                arguments("empty name", name(), "no CN in name"), //
                arguments("empty CN", name(rdn(utf8(CN, ""))), "empty CN"), //

                // string types
                arguments("TeletexString", name(rdn(ava(CN, TELETEX_STRING, ascii("dev")))),
                        "unsupported CN string type 0x14"), //
                arguments("IA5String (not an RFC 5280 DirectoryString choice)",
                        name(rdn(ava(CN, IA5_STRING, ascii("dev")))), "unsupported CN string type 0x16"), //
                arguments("constructed UTF8String", name(rdn(ava(CN, CONSTRUCTED_UTF8_STRING, ascii("dev")))),
                        "unsupported CN string type 0x2c"), //
                arguments("CN is an INTEGER", name(rdn(ava(CN, INTEGER, new byte[] { 5 }))),
                        "unsupported CN string type 0x02"), //
                arguments("invalid UTF-8", name(rdn(ava(CN, UTF8_STRING, new byte[] { 'a', (byte) 0xc3, 0x28 }))),
                        "not a valid UTF-8"), //
                arguments("BMPString of odd length", name(rdn(ava(CN, BMP_STRING, new byte[] { 0, 'a', 0 }))),
                        "BMPString with odd length"), //
                arguments("BMPString with a surrogate pair",
                        name(rdn(ava(CN, BMP_STRING, "a\uD83D\uDE00b".getBytes(StandardCharsets.UTF_16BE)))),
                        "surrogate code unit"), //
                arguments("BMPString with an unpaired high surrogate",
                        name(rdn(ava(CN, BMP_STRING, new byte[] { (byte) 0xD8, 0x00, 0, 'a' }))),
                        "surrogate code unit"), //
                arguments("BMPString with a lone low surrogate",
                        name(rdn(ava(CN, BMP_STRING, new byte[] { 0, 'a', (byte) 0xDC, 0x00 }))),
                        "surrogate code unit"), //
                arguments("UniversalString of bad length", name(rdn(ava(CN, UNIVERSAL_STRING, new byte[] { 0, 0, 0 }))),
                        "not a multiple of 4"), //
                arguments("UniversalString with a surrogate value",
                        name(rdn(ava(CN, UNIVERSAL_STRING, new byte[] { 0, 0, (byte) 0xD8, 0x00 }))),
                        "invalid code point"), //
                arguments("UniversalString above U+10FFFF",
                        name(rdn(ava(CN, UNIVERSAL_STRING, new byte[] { 0, 0x11, 0, 0 }))), "invalid code point"), //

                // control characters
                arguments("NUL in the CN (null-prefix trick)", name(rdn(utf8(CN, "victim\u0000.evil"))),
                        "control character"), //
                arguments("LF in the CN", name(rdn(utf8(CN, "a\nb"))), "control character"), //
                arguments("DEL in the CN", name(rdn(utf8(CN, "a\u007fb"))), "control character"), //
                arguments("C1 control character in the CN", name(rdn(utf8(CN, "a\u0085b"))), "control character"), //

                // DER framing
                arguments("null input", (byte[]) null, "name is null"), //
                arguments("empty input", new byte[0], "truncated Name"), //
                arguments("not a SEQUENCE", new byte[] { SET, 0x00 }, "unexpected tag 0x31 for Name"), //
                arguments("trailing byte after the name", cat(valid, new byte[] { 0 }), "trailing data after Name"), //
                arguments("truncated name", Arrays.copyOf(valid, valid.length - 1), "exceeds available data"), //
                arguments("length larger than the data", new byte[] { SEQUENCE, 0x7f, SET, 0x00 },
                        "exceeds available data"), //
                arguments("indefinite length", new byte[] { SEQUENCE, (byte) 0x80, 0, 0 }, "indefinite length"), //
                arguments("long form used for a short length",
                        cat(new byte[] { SEQUENCE, (byte) 0x81, (byte) (valid.length - 2) },
                                Arrays.copyOfRange(valid, 2, valid.length)),
                        "non-minimal length encoding of Name"), //
                arguments("more than 3 length octets", new byte[] { SEQUENCE, (byte) 0x84, 0, 0, 0, 1, 0 },
                        "invalid length encoding"), //
                arguments("length octets cut short", new byte[] { SEQUENCE, (byte) 0x82, 0x01 },
                        "invalid length encoding"), //
                arguments("high-tag-number form", new byte[] { 0x3f, 0x01, 0x00 }, "high-tag-number form"), //
                arguments("empty RDN", name(tlv(SET)), "empty RDN"), //
                arguments("data after the value of an attribute",
                        name(rdn(tlv(SEQUENCE, CN, tlv(UTF8_STRING, ascii("a")), new byte[] { 5, 0 }))),
                        "trailing data in AttributeTypeAndValue"));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("invalidNames")
    void reject_invalid_name(String label, byte[] der, String expectedReason) {
        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> DerCommonName.extractCN(der));

        assertTrue(exception.getMessage().contains(expectedReason), () -> "expected a message containing ["
                + expectedReason + "] but was [" + exception.getMessage() + "]");
    }

    // ------------------------------------------------------------------------------------------------------------
    // the X500Principal entry point
    // ------------------------------------------------------------------------------------------------------------

    @Test
    void extract_common_name_of_x500principal() {
        assertEquals("device-1", DerCommonName.extractCN(new X500Principal("CN=device-1,O=Acme,C=FR")));
    }

    @Test
    void common_name_text_in_another_attribute_does_not_give_a_common_name() {
        // Subject is only "OU = CN=victim-device": a regex on the rendered DN could take that for the identity
        X500Principal subject = new X500Principal(name(rdn(utf8(OU, "CN=victim-device"))));

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> DerCommonName.extractCN(subject));

        assertTrue(exception.getMessage().contains("no CN in name"));
    }

    @Test
    void common_name_text_in_another_attribute_does_not_hide_the_real_common_name() {
        X500Principal subject = new X500Principal(name(rdn(utf8(OU, "CN=victim,x")), rdn(utf8(CN, "real"))));

        assertEquals("real", DerCommonName.extractCN(subject));
    }

    @Test
    void reject_null_principal() {
        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> DerCommonName.extractCN((X500Principal) null));

        assertTrue(exception.getMessage().contains("principal is null"));
    }

    // ------------------------------------------------------------------------------------------------------------
    // real certificates, parsed by the JDK
    // ------------------------------------------------------------------------------------------------------------

    /** Self-signed EC P-256 certificate made with keytool, subject: CN=urn:imei:123456789012345, O=Acme. */
    private static final String CERT_SINGLE_CN = "-----BEGIN CERTIFICATE-----\n" //
            + "MIIBfDCCASGgAwIBAgIIRYjI0zuc1xwwCgYIKoZIzj0EAwMwMjENMAsGA1UEChME\n" //
            + "QWNtZTEhMB8GA1UEAxMYdXJuOmltZWk6MTIzNDU2Nzg5MDEyMzQ1MB4XDTI2MTAx\n" //
            + "OTE0MDIyM1oXDTI2MTAyMTE0MDIyM1owMjENMAsGA1UEChMEQWNtZTEhMB8GA1UE\n" //
            + "AxMYdXJuOmltZWk6MTIzNDU2Nzg5MDEyMzQ1MFkwEwYHKoZIzj0CAQYIKoZIzj0D\n" //
            + "AQcDQgAEbp4g0bxgjjT/r7bszJ2FmC75r3OUyPWOx8EmGqxxyNl9NM469y8G4qz9\n" //
            + "WM0zI1zmzyoz2ASeWxCb3IcdbuVQeaMhMB8wHQYDVR0OBBYEFB6Ch4xZ0EPORVpL\n" //
            + "06kqK4KNdA+jMAoGCCqGSM49BAMDA0kAMEYCIQC40Zsdkj07W14OLHTg6oz+yP9D\n" //
            + "YMehvjhzzDIkoW1DmgIhAOsIwnDvXsFBS6uLFRqzKCrxSu9UEiuWc9mdNoFgW0JD\n" //
            + "-----END CERTIFICATE-----\n";

    /**
     * keytool -dname 'OU=CN\=victim\,x, CN=attacker-device, O=Acme': one real CN, plus an OU whose value is the text
     * CN=victim,x.
     */
    private static final String CERT_CN_AND_INJECTED_OU = "-----BEGIN CERTIFICATE-----\n" //
            + "MIIBlTCCATygAwIBAgIJAMHAdUia/7NOMAoGCCqGSM49BAMDMD8xDTALBgNVBAoT\n" //
            + "BEFjbWUxGDAWBgNVBAMTD2F0dGFja2VyLWRldmljZTEUMBIGA1UECxMLQ049dmlj\n" //
            + "dGltLHgwHhcNMjYxMDE5MTQwMzIyWhcNMjYxMDIxMTQwMzIyWjA/MQ0wCwYDVQQK\n" //
            + "EwRBY21lMRgwFgYDVQQDEw9hdHRhY2tlci1kZXZpY2UxFDASBgNVBAsTC0NOPXZp\n" //
            + "Y3RpbSx4MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAE4auelJYKJvlcidsW+mMC\n" //
            + "TF09Ybbu7u4B6BRndgP8q/VYz84iGOGemHNNR8RbvATK95NI8Ib11HntAvLmeXrr\n" //
            + "rKMhMB8wHQYDVR0OBBYEFJ+c7X/Hjs2zKKZ4YVuzfe1ii2TzMAoGCCqGSM49BAMD\n" //
            + "A0cAMEQCIFBzK9V/QcOuMMEMDXuU+iSjPigZbpy7UhlUtPcoiAVqAiBZkilsKM4H\n" //
            + "sLX0xJvaN1SX55VJmZpOdbc9foiqHa6ZJw==\n" //
            + "-----END CERTIFICATE-----\n";

    /** keytool -dname 'CN=attacker, CN=victim, O=Acme': two CN attributes. */
    private static final String CERT_TWO_CN = "-----BEGIN CERTIFICATE-----\n" //
            + "MIIBfTCCASOgAwIBAgIIS60r1/nwvJgwCgYIKoZIzj0EAwMwMzENMAsGA1UEChME\n" //
            + "QWNtZTEPMA0GA1UEAxMGdmljdGltMREwDwYDVQQDEwhhdHRhY2tlcjAeFw0yNjEw\n" //
            + "MTkxNDAyMjFaFw0yNjEwMjExNDAyMjFaMDMxDTALBgNVBAoTBEFjbWUxDzANBgNV\n" //
            + "BAMTBnZpY3RpbTERMA8GA1UEAxMIYXR0YWNrZXIwWTATBgcqhkjOPQIBBggqhkjO\n" //
            + "PQMBBwNCAAQo1W5+OqqhMQ2Y+reMQC450LjTKGTZuE/xZSTkVS9RUeScezGT8s2L\n" //
            + "SapNQJlj8hIExbXmVJUkgW33MTdSf4OVoyEwHzAdBgNVHQ4EFgQUG+0yw3hN5P3N\n" //
            + "x3idfCmgDllE3yswCgYIKoZIzj0EAwMDSAAwRQIgUNsxv9Cd+0FJUgQABM9wyT4v\n" //
            + "WMW49yYkqBtKsPmI/jMCIQDNZ67awV6HLZA2SDbM2MOxQVzVucNCDDDtDmeux4g3\n" //
            + "xQ==\n" //
            + "-----END CERTIFICATE-----\n";

    /**
     * keytool -dname 'OU=CN\=victim\,x, O=Acme': no CN at all, only an OU whose value is the text CN=victim,x.
     */
    private static final String CERT_NO_CN = "-----BEGIN CERTIFICATE-----\n" //
            + "MIIBYTCCAQegAwIBAgIIVE+h3DYHd/AwCgYIKoZIzj0EAwMwJTENMAsGA1UEChME\n" //
            + "QWNtZTEUMBIGA1UECxMLQ049dmljdGltLHgwHhcNMjYxMDE5MTQwMzIzWhcNMjYx\n" //
            + "MDIxMTQwMzIzWjAlMQ0wCwYDVQQKEwRBY21lMRQwEgYDVQQLEwtDTj12aWN0aW0s\n" //
            + "eDBZMBMGByqGSM49AgEGCCqGSM49AwEHA0IABAIpLzWHE8wIbLtuuDLD8MaHfiIp\n" //
            + "D/VUfFoVBOEfv2xZQLGLhoob02SGYtDRAO/PYUpx0REsCjgzAP8uacNpXrCjITAf\n" //
            + "MB0GA1UdDgQWBBQTThgpMlX6xebCiQDcyFVtJhuZ3DAKBggqhkjOPQQDAwNIADBF\n" //
            + "AiEA0QVFLYZURfmN5S2Rlgj2w7K64+AdH5jJ3lkVzwdg5XsCIGYBLoWOfl8yhQH1\n" //
            + "3hBD3GlTwRMLI1HUlNSBAqKNB4Fl\n" //
            + "-----END CERTIFICATE-----\n";

    @Test
    void extract_common_name_of_certificate_with_a_single_cn() throws Exception {
        assertEquals("urn:imei:123456789012345", DerCommonName.extractCN(subjectOf(CERT_SINGLE_CN)));
    }

    @Test
    void injected_text_in_another_attribute_does_not_change_the_common_name_of_a_certificate() throws Exception {
        assertEquals("attacker-device", DerCommonName.extractCN(subjectOf(CERT_CN_AND_INJECTED_OU)));
    }

    @Test
    void reject_certificate_with_two_cn() throws Exception {
        X500Principal subject = subjectOf(CERT_TWO_CN);

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> DerCommonName.extractCN(subject));

        assertTrue(exception.getMessage().contains("more than one CN"));
    }

    @Test
    void reject_certificate_without_cn() throws Exception {
        X500Principal subject = subjectOf(CERT_NO_CN);

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> DerCommonName.extractCN(subject));

        assertTrue(exception.getMessage().contains("no CN in name"));
    }

    // ------------------------------------------------------------------------------------------------------------
    // randomized checks, with a fixed seed so a failure can be replayed
    // ------------------------------------------------------------------------------------------------------------

    private static final String NASTY_CHARACTERS = "\"\\,+=<>#; abCNvictim01\u00e9\u4e2d";

    @Test
    void attacker_controlled_attributes_never_change_the_common_name() {
        Random random = new Random(SEED);

        for (int i = 0; i < 20_000; i++) {
            byte[] der = randomNameWithOneCommonName(random, "real-device");

            assertEquals("real-device", DerCommonName.extractCN(der), () -> "input " + toHex(der));
            // same name, but after the JDK parsed and re-encoded it
            assertEquals("real-device", DerCommonName.extractCN(new X500Principal(der)), () -> "input " + toHex(der));
        }
    }

    @Test
    @SuppressWarnings("java:S8714")
    void random_corruption_is_accepted_or_rejected_cleanly() {
        Random random = new Random(SEED);
        List<byte[]> bases = Arrays.asList(//
                name(rdn(utf8(CN, "device-1"))), //
                name(rdn(utf8(O, "Acme"), utf8(OU, "a,b")), rdn(utf8(CN, "device-1"))), //
                name(rdn(utf8(CN, repeat('a', 200)))), // length in long form
                name(rdn(utf8(CN, repeat('b', 300)))), // length in long form, 2 octets
                name(rdn(ava(CN, PRINTABLE_STRING, ascii("dev-1")))), //
                name(rdn(ava(CN, BMP_STRING, "d\u00e9v".getBytes(StandardCharsets.UTF_16BE)))), //
                name(rdn(ava(CN, UNIVERSAL_STRING, "d\u00e9v".getBytes(Charset.forName("UTF-32BE"))))), //
                name(rdn(utf8(CN, "a")), rdn(utf8(CN, "b"))), //
                name(rdn(utf8(CN, "x"), utf8(OU, "CN=victim,y"))));

        for (int i = 0; i < 50_000; i++) {
            byte[] corrupted = corrupt(random, bases.get(random.nextInt(bases.size())));
            try {
                DerCommonName.extractCN(corrupted);
            } catch (IllegalStateException expected) {
                // fine: rejected
            } catch (Throwable unexpected) {
                fail("unexpected " + unexpected + " for input " + toHex(corrupted), unexpected);
            }
        }
    }

    /** The JDK is an independent second opinion on how each string type is decoded. */
    @Test
    void string_types_are_decoded_like_the_jdk() throws Exception {
        Random random = new Random(SEED);
        int[] stringTypes = { UTF8_STRING, PRINTABLE_STRING, BMP_STRING };

        for (int i = 0; i < 10_000; i++) {
            int stringType = stringTypes[random.nextInt(stringTypes.length)];
            byte[] der = name(rdn(ava(CN, stringType, encode(randomValue(random, stringType), stringType))));

            assertEquals(jdkCommonName(der), DerCommonName.extractCN(der), () -> "input " + toHex(der));
        }
    }

    // ------------------------------------------------------------------------------------------------------------
    // helpers
    // ------------------------------------------------------------------------------------------------------------

    private static X500Principal subjectOf(String pem) throws Exception {
        X509Certificate certificate = (X509Certificate) CertificateFactory.getInstance("X.509")
                .generateCertificate(new ByteArrayInputStream(pem.getBytes(StandardCharsets.US_ASCII)));
        return certificate.getSubjectX500Principal();
    }

    private static String jdkCommonName(byte[] der) throws NamingException {
        LdapName ldapName = new LdapName(new X500Principal(der).getName(X500Principal.RFC2253));
        for (Rdn rdn : ldapName.getRdns()) {
            if ("CN".equalsIgnoreCase(rdn.getType())) {
                return (String) rdn.getValue();
            }
        }
        throw new AssertionError("no CN in " + toHex(der));
    }

    /** A name with exactly one CN, surrounded by attributes whose values are chosen by an "attacker". */
    private static byte[] randomNameWithOneCommonName(Random random, String commonName) {
        byte[][] attributeTypes = { OU, O, L };
        int otherRdns = random.nextInt(5);
        int commonNameAt = random.nextInt(otherRdns + 1);
        List<byte[]> rdns = new ArrayList<>();
        for (int i = 0; i <= otherRdns; i++) {
            byte[] attacker = utf8(attributeTypes[random.nextInt(attributeTypes.length)],
                    randomValue(random, UTF8_STRING));
            if (i == commonNameAt) {
                byte[] common = utf8(CN, commonName);
                if (random.nextInt(5) != 0) {
                    rdns.add(rdn(common));
                } else if (random.nextBoolean()) {
                    // the CN shares a multi-valued RDN with an attacker controlled attribute
                    rdns.add(rdn(attacker, common));
                } else {
                    rdns.add(rdn(common, attacker));
                }
            } else {
                rdns.add(rdn(attacker));
            }
        }
        return name(rdns.toArray(new byte[0][]));
    }

    private static String randomValue(Random random, int stringType) {
        // PrintableString has a small repertoire, UTF8String and BMPString take everything we generate
        String alphabet = stringType == PRINTABLE_STRING ? "ABCabc012 '()+,-./:=?" : NASTY_CHARACTERS;
        int length = 1 + random.nextInt(12);
        StringBuilder value = new StringBuilder();
        for (int i = 0; i < length; i++) {
            value.append(alphabet.charAt(random.nextInt(alphabet.length())));
        }
        return value.toString();
    }

    private static byte[] encode(String value, int stringType) {
        switch (stringType) {
        case PRINTABLE_STRING:
            return value.getBytes(StandardCharsets.US_ASCII);
        case BMP_STRING:
            return value.getBytes(StandardCharsets.UTF_16BE);
        default:
            return value.getBytes(StandardCharsets.UTF_8);
        }
    }

    /** Changes, inserts, removes or truncates a few bytes. */
    private static byte[] corrupt(Random random, byte[] original) {
        byte[] bytes = original.clone();
        int changes = 1 + random.nextInt(4);
        for (int i = 0; i < changes && bytes.length > 0; i++) {
            int at = random.nextInt(bytes.length);
            switch (random.nextInt(6)) {
            case 0:
                bytes[at] = (byte) random.nextInt(256);
                break;
            case 1:
                bytes = Arrays.copyOf(bytes, random.nextInt(bytes.length + 1));
                break;
            case 2:
                bytes = cat(Arrays.copyOf(bytes, at), new byte[] { (byte) random.nextInt(256) },
                        Arrays.copyOfRange(bytes, at, bytes.length));
                break;
            case 3:
                bytes = cat(Arrays.copyOf(bytes, at), Arrays.copyOfRange(bytes, at + 1, bytes.length));
                break;
            case 4:
                bytes[at] ^= (byte) (1 << random.nextInt(8));
                break;
            default: // something that looks like a length octet
                bytes[at] = (byte) (random.nextBoolean() ? 0x80 + random.nextInt(4) : random.nextInt(8));
            }
        }
        return bytes;
    }

    // --- a minimal DER encoder, enough to build the names above ---

    private static byte[] cat(byte[]... parts) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        for (byte[] part : parts) {
            out.write(part, 0, part.length);
        }
        return out.toByteArray();
    }

    private static byte[] tlv(int tag, byte[]... values) {
        byte[] content = cat(values);
        int length = content.length;
        byte[] lengthOctets;
        if (length < 0x80) {
            lengthOctets = new byte[] { (byte) length };
        } else if (length < 0x100) {
            lengthOctets = new byte[] { (byte) 0x81, (byte) length };
        } else {
            lengthOctets = new byte[] { (byte) 0x82, (byte) (length >> 8), (byte) length };
        }
        return cat(new byte[] { (byte) tag }, lengthOctets, content);
    }

    private static byte[] oid(int... contentOctets) {
        byte[] content = new byte[contentOctets.length];
        for (int i = 0; i < content.length; i++) {
            content[i] = (byte) contentOctets[i];
        }
        return tlv(OID, content);
    }

    /** AttributeTypeAndValue. */
    private static byte[] ava(byte[] type, int valueTag, byte[] value) {
        return tlv(SEQUENCE, type, tlv(valueTag, value));
    }

    private static byte[] utf8(byte[] type, String value) {
        return ava(type, UTF8_STRING, value.getBytes(StandardCharsets.UTF_8));
    }

    /** RelativeDistinguishedName. */
    private static byte[] rdn(byte[]... attributes) {
        return tlv(SET, attributes);
    }

    private static byte[] name(byte[]... rdns) {
        return tlv(SEQUENCE, rdns);
    }

    private static byte[] ascii(String value) {
        return value.getBytes(StandardCharsets.US_ASCII);
    }

    private static String repeat(char c, int count) {
        char[] chars = new char[count];
        Arrays.fill(chars, c);
        return new String(chars);
    }

    private static String toHex(byte[] bytes) {
        StringBuilder hex = new StringBuilder();
        for (byte b : bytes) {
            hex.append(String.format("%02x", b));
        }
        return hex.toString();
    }
}
