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
package org.eclipse.leshan.transport.californium.server.observation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.stream.Stream;

import javax.security.auth.x500.X500Principal;

import org.eclipse.californium.elements.AddressEndpointContext;
import org.eclipse.californium.elements.EndpointContext;
import org.eclipse.californium.elements.auth.PreSharedKeyIdentity;
import org.eclipse.californium.elements.auth.X509CertPath;
import org.eclipse.leshan.core.security.certificate.util.X509CertUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

class EndpointContextSerDesTest {

    private static final InetSocketAddress ADDRESS = new InetSocketAddress("127.0.0.1", 5684);

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
    private static final byte[] OID_OU = { 6, 3, 0x55, 4, 11 };

    /** Subjects, including some which the JDK can not parse back from its own RFC 2253 string. */
    static Stream<Arguments> subjects() {
        return Stream.of(//
                arguments("simple", name(rdn(OID_CN, "device-1"))), //
                arguments("comma in the CN", name(rdn(OID_CN, "dev,ice"))), //
                arguments("space, backslash and comma in another attribute",
                        name(rdn(OID_CN, "device-1"), rdn(OID_OU, " \\,"))), //
                arguments("quote, backslash and semicolon in another attribute",
                        name(rdn(OID_CN, "device-1"), rdn(OID_OU, "\"\\;"))));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("subjects")
    void x500_principal_is_restored_as_it_was(String label, X500Principal subject) throws Exception {
        EndpointContext restored = serializeAndDeserialize(new AddressEndpointContext(ADDRESS, subject));

        assertEquals(subject, restored.getPeerIdentity());
    }

    @Test
    void x509_cert_path_is_restored_as_its_subject() throws Exception {
        X509Certificate certificate = (X509Certificate) CertificateFactory.getInstance("X.509")
                .generateCertificate(new ByteArrayInputStream(CERT.getBytes(StandardCharsets.US_ASCII)));
        EndpointContext context = new AddressEndpointContext(ADDRESS, X509CertPath.fromCertificatesChain(certificate));

        EndpointContext restored = serializeAndDeserialize(context);

        assertEquals(certificate.getSubjectX500Principal(), restored.getPeerIdentity());
        assertEquals("urn:imei:123456789012345", X509CertUtil.extractCN((X500Principal) restored.getPeerIdentity()));
    }

    @Test
    void observation_stored_by_a_previous_version_can_still_be_read() throws Exception {
        // before, the subject was stored as a RFC 2253 string, under the key "dn"
        JsonNode stored = new ObjectMapper()
                .readTree("{\"address\":\"127.0.0.1\",\"port\":5684,\"dn\":\"CN=device-1,O=Acme\"}");

        EndpointContext restored = EndpointContextSerDes.deserialize(stored);

        assertEquals(new X500Principal("CN=device-1,O=Acme"), restored.getPeerIdentity());
    }

    @Test
    void the_subject_is_not_stored_as_a_string_any_more() {
        JsonNode stored = EndpointContextSerDes
                .serialize(new AddressEndpointContext(ADDRESS, name(rdn(OID_CN, "device-1"))));

        assertFalse(stored.has("dn"));
    }

    @Test
    void psk_identity_is_restored_as_it_was() throws Exception {
        EndpointContext restored = serializeAndDeserialize(
                new AddressEndpointContext(ADDRESS, new PreSharedKeyIdentity("identity")));

        assertEquals(new PreSharedKeyIdentity("identity"), restored.getPeerIdentity());
    }

    private static EndpointContext serializeAndDeserialize(EndpointContext context) throws Exception {
        String json = EndpointContextSerDes.serialize(context).toString();
        return EndpointContextSerDes.deserialize(new ObjectMapper().readTree(json));
    }

    // --- minimal DER encoder, to build subjects without going through the string form ---

    private static byte[] cat(byte[]... parts) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        for (byte[] part : parts) {
            out.write(part, 0, part.length);
        }
        return out.toByteArray();
    }

    private static byte[] tlv(int tag, byte[]... values) {
        byte[] content = cat(values);
        byte[] length = content.length < 0x80 ? new byte[] { (byte) content.length }
                : new byte[] { (byte) 0x81, (byte) content.length };
        return cat(new byte[] { (byte) tag }, length, content);
    }

    private static byte[] rdn(byte[] oid, String value) {
        return tlv(0x31, tlv(0x30, oid, tlv(0x0c, value.getBytes(StandardCharsets.UTF_8))));
    }

    private static X500Principal name(byte[]... rdns) {
        return new X500Principal(tlv(0x30, rdns));
    }
}
