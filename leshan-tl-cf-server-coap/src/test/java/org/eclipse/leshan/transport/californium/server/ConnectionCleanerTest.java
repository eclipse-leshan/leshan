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
package org.eclipse.leshan.transport.californium.server;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PublicKey;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

import javax.security.auth.x500.X500Principal;

import org.eclipse.californium.elements.auth.PreSharedKeyIdentity;
import org.eclipse.californium.elements.auth.RawPublicKeyIdentity;
import org.eclipse.californium.elements.auth.X509CertPath;
import org.eclipse.leshan.servers.security.SecurityInfo;
import org.junit.jupiter.api.Test;

class ConnectionCleanerTest {

    private static final String CERT = "-----BEGIN CERTIFICATE-----\n" //
            + "MIIBfDCCASGgAwIBAgIIRYjI0zuc1xwwCgYIKoZIzj0EAwMwMjENMAsGA1UEChME\n" //
            + "QWNtZTEhMB8GA1UEAxMYdXJuOmltZWk6MTIzNDU2Nzg5MDEyMzQ1MB4XDTI2MTAx\n" //
            + "OTE0MDIyM1oXDTI2MTAyMTE0MDIyM1owMjENMAsGA1UEChMEQWNtZTEhMB8GA1UE\n" //
            + "AxMYdXJuOmltZWk6MTIzNDU2Nzg5MDEyMzQ1MFkwEwYHKoZIzj0CAQYIKoZIzj0D\n" //
            + "AQcDQgAEbp4g0bxgjjT/r7bszJ2FmC75r3OUyPWOx8EmGqxxyNl9NM469y8G4qz9\n" //
            + "WM0zI1zmzyoz2ASeWxCb3IcdbuVQeaMhMB8wHQYDVR0OBBYEFB6Ch4xZ0EPORVpL\n" //
            + "06kqK4KNdA+jMAoGCCqGSM49BAMDA0kAMEYCIQC40Zsdkj07W14OLHTg6oz+yP9D\n" //
            + "YMehvjhzzDIkoW1DmgIhAOsIwnDvXsFBS6uLFRqzKCrxSu9UEiuWc9mdNoFgW0JD\n" //
            + "-----END CERTIFICATE-----\n";

    private static final byte[] OID_CN = { 6, 3, 0x55, 4, 3 };

    private final SecurityInfo psk = SecurityInfo.newPreSharedKeyInfo("device-1", "identity-1", "key".getBytes());
    private final SecurityInfo x509 = SecurityInfo.newX509CertInfo("device-1");

    @Test
    void psk_connection_is_the_one_of_the_psk_info_with_the_same_identity() {
        assertTrue(ConnectionCleaner.isConnectionOfAny(new PreSharedKeyIdentity("identity-1"), psk));
        assertFalse(ConnectionCleaner.isConnectionOfAny(new PreSharedKeyIdentity("identity-2"), psk));
    }

    @Test
    void rpk_connection_is_the_one_of_the_rpk_info_with_the_same_key() throws Exception {
        PublicKey key = newPublicKey();
        SecurityInfo rpk = SecurityInfo.newRawPublicKeyInfo("device-1", key);

        assertTrue(ConnectionCleaner.isConnectionOfAny(new RawPublicKeyIdentity(key), rpk));
        assertFalse(ConnectionCleaner.isConnectionOfAny(new RawPublicKeyIdentity(newPublicKey()), rpk));
    }

    @Test
    void x509_connection_is_the_one_of_the_x509_info_with_the_same_common_name() {
        assertTrue(ConnectionCleaner.isConnectionOfAny(commonName("device-1"), x509));
        assertFalse(ConnectionCleaner.isConnectionOfAny(commonName("device-2"), x509));
    }

    @Test
    void x509_connection_can_be_a_certificate_path() throws Exception {
        X509Certificate certificate = (X509Certificate) CertificateFactory.getInstance("X.509")
                .generateCertificate(new ByteArrayInputStream(CERT.getBytes(StandardCharsets.US_ASCII)));

        assertTrue(ConnectionCleaner.isConnectionOfAny(X509CertPath.fromCertificatesChain(certificate),
                SecurityInfo.newX509CertInfo("urn:imei:123456789012345")));
        assertFalse(ConnectionCleaner.isConnectionOfAny(X509CertPath.fromCertificatesChain(certificate), x509));
    }

    @Test
    void connection_with_an_ambiguous_certificate_is_not_a_match_and_does_not_throw() {
        // Scandium stops visiting the connections if this throws, then the next connections would not be cleaned.
        X500Principal severalCommonNames = new X500Principal(
                tlv(0x30, rdn(OID_CN, "device-1"), rdn(OID_CN, "device-2")));
        X500Principal noCommonName = new X500Principal(tlv(0x30));

        assertFalse(ConnectionCleaner.isConnectionOfAny(severalCommonNames, x509));
        assertFalse(ConnectionCleaner.isConnectionOfAny(noCommonName, x509));
    }

    @Test
    void connection_is_never_the_one_of_a_security_info_using_another_kind_of_credentials() {
        assertFalse(ConnectionCleaner.isConnectionOfAny(commonName("device-1"), psk));
        assertFalse(ConnectionCleaner.isConnectionOfAny(new PreSharedKeyIdentity("identity-1"), x509));
    }

    @Test
    void connection_is_a_match_if_any_of_the_security_infos_is_the_one_of_the_connection() {
        assertTrue(ConnectionCleaner.isConnectionOfAny(commonName("device-1"), psk, null, x509));
    }

    @Test
    void nothing_is_a_match_without_principal_or_security_info() {
        assertFalse(ConnectionCleaner.isConnectionOfAny(null, x509));
        assertFalse(ConnectionCleaner.isConnectionOfAny(commonName("device-1")));
        assertFalse(ConnectionCleaner.isConnectionOfAny(commonName("device-1"), (SecurityInfo) null));
    }

    private static PublicKey newPublicKey() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("EC");
        generator.initialize(256);
        KeyPair pair = generator.generateKeyPair();
        return pair.getPublic();
    }

    private static X500Principal commonName(String commonName) {
        return new X500Principal(tlv(0x30, rdn(OID_CN, commonName)));
    }

    private static byte[] cat(byte[]... parts) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        for (byte[] part : parts) {
            out.write(part, 0, part.length);
        }
        return out.toByteArray();
    }

    private static byte[] tlv(int tag, byte[]... values) {
        byte[] content = cat(values);
        return cat(new byte[] { (byte) tag, (byte) content.length }, content);
    }

    private static byte[] rdn(byte[] oid, String value) {
        return tlv(0x31, tlv(0x30, oid, tlv(0x0c, value.getBytes(StandardCharsets.UTF_8))));
    }
}
