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
package org.eclipse.leshan.client.engine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;

import org.eclipse.leshan.client.engine.DefaultClientEndpointNameProvider.Mode;
import org.eclipse.leshan.client.servers.ServerInfo;
import org.eclipse.leshan.core.SecurityMode;
import org.eclipse.leshan.core.request.RegisterRequest;
import org.junit.jupiter.api.Test;

class DefaultClientEndpointNameProviderTest {

    private static final String ENDPOINT = "urn:imei:123456789012345";

    /** keytool certificate, subject: CN=urn:imei:123456789012345, O=Acme. Issuer: CN=device-1, O=Acme CA. */
    private static final String CERT_CN_IS_URN = "-----BEGIN CERTIFICATE-----\n" //
            + "MIIBjjCCATWgAwIBAgIII1sfH8tXXeowCgYIKoZIzj0EAwMwJTEQMA4GA1UEChMH\n" //
            + "QWNtZSBDQTERMA8GA1UEAxMIZGV2aWNlLTEwHhcNMjYxMDIzMTYyNjU4WhcNMzYx\n" //
            + "MDIwMTYyNjU4WjAyMQ0wCwYDVQQKEwRBY21lMSEwHwYDVQQDExh1cm46aW1laTox\n" //
            + "MjM0NTY3ODkwMTIzNDUwWTATBgcqhkjOPQIBBggqhkjOPQMBBwNCAATStxW9XtU7\n" //
            + "kES0JDmIuJ2LivkgYcGI6XHNXK/dllPLQTdeF34Q1FifkCUB2Qj4/KjsRUfw5FVw\n" //
            + "x8OsXbZXc0jIo0IwQDAdBgNVHQ4EFgQU3ulvmrkMVaYTEjwS1wGym76+17cwHwYD\n" //
            + "VR0jBBgwFoAU8Jxk6Wb2pWxnL2ZsCDRIPzNA9TUwCgYIKoZIzj0EAwMDRwAwRAIg\n" //
            + "cBQPNfMrXniV36OSMEwSRuUx4UaPXOVey+YNR38pcH8CID28fhQbXqslkNuHYz91\n" //
            + "iACJ2epzMsUwJjGTZsHaNnWj\n" //
            + "-----END CERTIFICATE-----\n";

    /** Subject: CN=other-device, O=Acme. Issuer: CN=device-1, O=Acme CA, which is the endpoint name of some tests. */
    private static final String CERT_CN_IS_OTHER_DEVICE = "-----BEGIN CERTIFICATE-----\n" //
            + "MIIBgjCCASmgAwIBAgIISnQk/MirLdIwCgYIKoZIzj0EAwMwJTEQMA4GA1UEChMH\n" //
            + "QWNtZSBDQTERMA8GA1UEAxMIZGV2aWNlLTEwHhcNMjYxMDIzMTYyNzAxWhcNMzYx\n" //
            + "MDIwMTYyNzAxWjAmMQ0wCwYDVQQKEwRBY21lMRUwEwYDVQQDEwxvdGhlci1kZXZp\n" //
            + "Y2UwWTATBgcqhkjOPQIBBggqhkjOPQMBBwNCAASRlPS9ene4Gz626Z4Qc4GE8gvo\n" //
            + "8w2+Ah8KwUDeawQi366Q1IFr15KSvOwYcBwZ0i2vEZlbSJYdldF5s7UDA85Co0Iw\n" //
            + "QDAdBgNVHQ4EFgQUxZBs1AcLZLYJJGuOEjTw7QydhaQwHwYDVR0jBBgwFoAU8Jxk\n" //
            + "6Wb2pWxnL2ZsCDRIPzNA9TUwCgYIKoZIzj0EAwMDRwAwRAIgcx2f+qHJHw0w26DW\n" //
            + "C5Qp8iXM451wNIioGDkvhHafBYECIACMBECvhKtkSsm4oqzhabe7ehi8dAk+0sqr\n" //
            + "dxdtKVjH\n" //
            + "-----END CERTIFICATE-----\n";

    /** Self-signed, subject: CN=device-1, CN=device-2, O=Acme (two CN). */
    private static final String CERT_TWO_CN = "-----BEGIN CERTIFICATE-----\n" //
            + "MIIBgTCCASegAwIBAgIIRzTTBMNRA/4wCgYIKoZIzj0EAwMwNTENMAsGA1UEChME\n" //
            + "QWNtZTERMA8GA1UEAxMIZGV2aWNlLTIxETAPBgNVBAMTCGRldmljZS0xMB4XDTI2\n" //
            + "MTAyMzE2MjcwMVoXDTM2MTAyMDE2MjcwMVowNTENMAsGA1UEChMEQWNtZTERMA8G\n" //
            + "A1UEAxMIZGV2aWNlLTIxETAPBgNVBAMTCGRldmljZS0xMFkwEwYHKoZIzj0CAQYI\n" //
            + "KoZIzj0DAQcDQgAEKOw1O3HhheGR8WigdNJGIdx4KSeajAf/KDYblNWisnxnZl57\n" //
            + "PG8YRSntJKP/Yk5QlXhHixQ6USa2h/rJaFZcD6MhMB8wHQYDVR0OBBYEFEGK0Dbl\n" //
            + "y+E18e6FM5LfWG+3taOMMAoGCCqGSM49BAMDA0gAMEUCIQDyz+Ai9rjAs32ByNsg\n" //
            + "hSTHHp88grueBaWxNmm/raxyLAIgMK/ueLdMFuYjwMkiVeg5QpF6WNNYGyoC0zOD\n" //
            + "s8cgBxI=\n" //
            + "-----END CERTIFICATE-----\n";

    /** Self-signed, subject: OU=Acme (no CN). */
    private static final String CERT_NO_CN = "-----BEGIN CERTIFICATE-----\n" //
            + "MIIBMzCB26ADAgECAggx3jIouvEIOjAKBggqhkjOPQQDAzAPMQ0wCwYDVQQLEwRB\n" //
            + "Y21lMB4XDTI2MTAyMzE2MjcwM1oXDTM2MTAyMDE2MjcwM1owDzENMAsGA1UECxME\n" //
            + "QWNtZTBZMBMGByqGSM49AgEGCCqGSM49AwEHA0IABCdR+YJgOZj9KHbxS9umCT9+\n" //
            + "t6RQyXnMmxW020ZQaoWmBgvqrwMaHJSZIZJ4vR64htvnaqbVtp0IGBP9spaNXGSj\n" //
            + "ITAfMB0GA1UdDgQWBBSAvabXdzwuIwcBMP9Gp4D6UuMUHzAKBggqhkjOPQQDAwNH\n" //
            + "ADBEAiBFWKlPQWEJAAdofErs2WRD7hI9V0Cp+D98aac08ejtkQIgOv2iOrToBTjm\n" //
            + "DZSqX4aOvXyEXpa2pgGGKTYKWt4n104=\n" //
            + "-----END CERTIFICATE-----\n";

    /** Self-signed CA, subject: CN=device-1, O=Acme CA. It issued the first two certificates above. */
    private static final String CERT_CA = "-----BEGIN CERTIFICATE-----\n" //
            + "MIIBczCCARigAwIBAgIIT42u5DojQd4wCgYIKoZIzj0EAwMwJTEQMA4GA1UEChMH\n" //
            + "QWNtZSBDQTERMA8GA1UEAxMIZGV2aWNlLTEwHhcNMjYxMDIzMTYyNjU1WhcNMzYx\n" //
            + "MDIwMTYyNjU1WjAlMRAwDgYDVQQKEwdBY21lIENBMREwDwYDVQQDEwhkZXZpY2Ut\n" //
            + "MTBZMBMGByqGSM49AgEGCCqGSM49AwEHA0IABHz126dIyv9d8gFSlrCXRRvp/QHI\n" //
            + "JUkPaLWCCrrFvLm0S2cvGpiem3ZHuozuhpTggiIyOJ7Uoiv0hFQarDNfY1SjMjAw\n" //
            + "MB0GA1UdDgQWBBTwnGTpZvalbGcvZmwINEg/M0D1NTAPBgNVHRMBAf8EBTADAQH/\n" //
            + "MAoGCCqGSM49BAMDA0kAMEYCIQCDdagjfc6gnZmtjRLSTrKzsJyC9Ag9EC8LLen5\n" //
            + "ng3TswIhAOzZFK596fv1pxq7HYxDeeqPXuKe4s3hHK1hu+1cv/w3\n" //
            + "-----END CERTIFICATE-----\n";

    @Test
    void always_sends_the_endpoint_name() throws Exception {
        DefaultClientEndpointNameProvider provider = new DefaultClientEndpointNameProvider(ENDPOINT, Mode.ALWAYS);

        assertEquals(ENDPOINT, provider.getEndpointNameFor(x509(CERT_CN_IS_URN), RegisterRequest.class));
    }

    @Test
    void never_sends_the_endpoint_name() throws Exception {
        DefaultClientEndpointNameProvider provider = new DefaultClientEndpointNameProvider(ENDPOINT, Mode.NEVER);

        assertNull(provider.getEndpointNameFor(x509(CERT_CN_IS_OTHER_DEVICE), RegisterRequest.class));
    }

    @Test
    void if_necessary_does_not_send_the_endpoint_name_if_it_is_the_cn_of_the_certificate() throws Exception {
        DefaultClientEndpointNameProvider provider = new DefaultClientEndpointNameProvider(ENDPOINT, Mode.IF_NECESSARY);

        assertNull(provider.getEndpointNameFor(x509(CERT_CN_IS_URN), RegisterRequest.class));
    }

    @Test
    void if_necessary_sends_the_endpoint_name_if_the_cn_of_the_certificate_is_another_one() throws Exception {
        DefaultClientEndpointNameProvider provider = new DefaultClientEndpointNameProvider("device-2",
                Mode.IF_NECESSARY);

        assertEquals("device-2", provider.getEndpointNameFor(x509(CERT_CN_IS_URN), RegisterRequest.class));
    }

    @Test
    void if_necessary_uses_the_subject_of_the_certificate_not_its_issuer() throws Exception {
        // the issuer of this certificate is named "device-1" but its subject is "other-device"
        DefaultClientEndpointNameProvider provider = new DefaultClientEndpointNameProvider("device-1",
                Mode.IF_NECESSARY);

        assertEquals("device-1", provider.getEndpointNameFor(x509(CERT_CN_IS_OTHER_DEVICE), RegisterRequest.class));
    }

    @Test
    void if_necessary_uses_the_first_certificate_of_the_chain() throws Exception {
        // the chain is the device certificate followed by the one of its issuer, which is named "device-1"
        DefaultClientEndpointNameProvider provider = new DefaultClientEndpointNameProvider("other-device",
                Mode.IF_NECESSARY);

        assertNull(provider.getEndpointNameFor(x509(CERT_CN_IS_OTHER_DEVICE, CERT_CA), RegisterRequest.class));
    }

    @Test
    void if_necessary_sends_the_endpoint_name_if_the_certificate_has_several_cn() throws Exception {
        // The server can not guess the endpoint name from such a certificate. It must not make the request fail.
        DefaultClientEndpointNameProvider provider = new DefaultClientEndpointNameProvider("device-1",
                Mode.IF_NECESSARY);

        assertEquals("device-1", provider.getEndpointNameFor(x509(CERT_TWO_CN), RegisterRequest.class));
    }

    @Test
    void if_necessary_sends_the_endpoint_name_if_the_certificate_has_no_cn() throws Exception {
        DefaultClientEndpointNameProvider provider = new DefaultClientEndpointNameProvider(ENDPOINT, Mode.IF_NECESSARY);

        assertEquals(ENDPOINT, provider.getEndpointNameFor(x509(CERT_NO_CN), RegisterRequest.class));
    }

    @Test
    void if_necessary_does_not_send_the_endpoint_name_if_it_is_the_psk_identity() {
        DefaultClientEndpointNameProvider provider = new DefaultClientEndpointNameProvider(ENDPOINT, Mode.IF_NECESSARY);

        assertNull(provider.getEndpointNameFor(psk(ENDPOINT), RegisterRequest.class));
        assertEquals(ENDPOINT, provider.getEndpointNameFor(psk("another-identity"), RegisterRequest.class));
    }

    private static ServerInfo x509(String... pemCertificateChain) throws Exception {
        CertificateFactory factory = CertificateFactory.getInstance("X.509");
        Certificate[] chain = new Certificate[pemCertificateChain.length];
        for (int i = 0; i < chain.length; i++) {
            byte[] pem = pemCertificateChain[i].getBytes(StandardCharsets.US_ASCII);
            chain[i] = factory.generateCertificate(new ByteArrayInputStream(pem));
        }
        ServerInfo serverInfo = new ServerInfo();
        serverInfo.secureMode = SecurityMode.X509;
        serverInfo.clientCertificates = chain;
        return serverInfo;
    }

    private static ServerInfo psk(String identity) {
        ServerInfo serverInfo = new ServerInfo();
        serverInfo.secureMode = SecurityMode.PSK;
        serverInfo.pskId = identity;
        return serverInfo;
    }
}
