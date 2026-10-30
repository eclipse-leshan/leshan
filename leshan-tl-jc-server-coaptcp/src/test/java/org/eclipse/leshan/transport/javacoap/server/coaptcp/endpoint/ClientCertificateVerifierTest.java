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
package org.eclipse.leshan.transport.javacoap.server.coaptcp.endpoint;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Arrays;

import org.eclipse.leshan.core.security.certificate.util.CertPathUtil;
import org.eclipse.leshan.core.security.certificate.verifier.X509CertificateVerifier.Role;
import org.junit.jupiter.api.Test;

class ClientCertificateVerifierTest {

    private static final InetSocketAddress CLIENT_ADDRESS = new InetSocketAddress("127.0.0.1", 40000);

    /** Trusted CA, subject: CN=Test CA, O=Acme. */
    private static final String CA = "-----BEGIN CERTIFICATE-----\n" //
            + "MIIBbjCCAROgAwIBAgIJAMeU875hwSW/MAoGCCqGSM49BAMDMCExDTALBgNVBAoT\n" //
            + "BEFjbWUxEDAOBgNVBAMTB1Rlc3QgQ0EwIBcNMjYxMDI4MTY0MDMyWhgPMjEyNjEw\n" //
            + "MDQxNjQwMzJaMCExDTALBgNVBAoTBEFjbWUxEDAOBgNVBAMTB1Rlc3QgQ0EwWTAT\n" //
            + "BgcqhkjOPQIBBggqhkjOPQMBBwNCAATQeZJK9HCsZEJiBTWVyUCGpH8GPdI8IvaX\n" //
            + "b6S+Th6PztJTeHFW+r890mlAM9ZOpZ0ux9VAdfztjS3yrg4OHH0uozIwMDAdBgNV\n" //
            + "HQ4EFgQUWb7cBdu8OQu0EBlxMUi7d3fffM4wDwYDVR0TAQH/BAUwAwEB/zAKBggq\n" //
            + "hkjOPQQDAwNJADBGAiEAjo9UqGZ0H45cSJ69cgqQYMlLMsGnm8HI3mtKRmPqmlsC\n" //
            + "IQCoT/XoQ/qJ9OIBF3Q1flCEtATkYiNeGxDN1AfJiK31KQ==\n" //
            + "-----END CERTIFICATE-----\n";

    /** Signed by CA, subject: CN=device-1, O=Acme. */
    private static final String CERT_ONE_CN = "-----BEGIN CERTIFICATE-----\n" //
            + "MIIBfTCCASOgAwIBAgIIVhMcdV6H7zUwCgYIKoZIzj0EAwMwITENMAsGA1UEChME\n" //
            + "QWNtZTEQMA4GA1UEAxMHVGVzdCBDQTAgFw0yNjEwMjgxNjQwMzdaGA8yMTI2MTAw\n" //
            + "NDE2NDAzN1owIjENMAsGA1UEChMEQWNtZTERMA8GA1UEAxMIZGV2aWNlLTEwWTAT\n" //
            + "BgcqhkjOPQIBBggqhkjOPQMBBwNCAAR8avR9fVx9bjmrg+YiQBXIinkKBPJBCdM5\n" //
            + "sJ6xWs2nnzkuSE45/s+mUUBvNDuaPUGsyDesAeGsIHmrfav3TucFo0IwQDAdBgNV\n" //
            + "HQ4EFgQUcg/SpwNsd0y4nppt1B9a/94VLQEwHwYDVR0jBBgwFoAUWb7cBdu8OQu0\n" //
            + "EBlxMUi7d3fffM4wCgYIKoZIzj0EAwMDSAAwRQIgFqM1UnVVuYxDKbEDp9UJwfeW\n" //
            + "3GPbCOEl8D2lMQwq5zoCIQCX4cC0ndxPk2LDx5fJJI9+GvQadoQMdWEG46wOfPve\n" //
            + "TA==\n" //
            + "-----END CERTIFICATE-----\n";

    /** Signed by CA, subject: CN=device-1, CN=device-2, O=Acme. */
    private static final String CERT_TWO_CN = "-----BEGIN CERTIFICATE-----\n" //
            + "MIIBkDCCATagAwIBAgIIckQi9PNbptkwCgYIKoZIzj0EAwMwITENMAsGA1UEChME\n" //
            + "QWNtZTEQMA4GA1UEAxMHVGVzdCBDQTAgFw0yNjEwMjgxNjQwNDBaGA8yMTI2MTAw\n" //
            + "NDE2NDA0MFowNTENMAsGA1UEChMEQWNtZTERMA8GA1UEAxMIZGV2aWNlLTIxETAP\n" //
            + "BgNVBAMTCGRldmljZS0xMFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEXUCbUmG4\n" //
            + "e7/W4JWWaKQCAx74Igf0stav0FT21r4gLgDVjzcoB/iTFT0AeWr0vpHhawiQ/0A9\n" //
            + "C5fViALrPk5fNaNCMEAwHQYDVR0OBBYEFCf+s2zWnrJpBqdJyf0v+Lv/keALMB8G\n" //
            + "A1UdIwQYMBaAFFm+3AXbvDkLtBAZcTFIu3d333zOMAoGCCqGSM49BAMDA0gAMEUC\n" //
            + "IQCqAE3GTTVj3MmRzLzAxGT+lZeVcJK3Px7GCr6rCh6uXwIgCqHGQi+VKh0kcjXr\n" //
            + "E/PXhrrvjeg0O8O8VhK7LRTxZek=\n" //
            + "-----END CERTIFICATE-----\n";

    /** Signed by CA, subject: OU=Acme (no CN). */
    private static final String CERT_NO_CN = "-----BEGIN CERTIFICATE-----\n" //
            + "MIIBajCCARCgAwIBAgIIGl4d9UOKYaUwCgYIKoZIzj0EAwMwITENMAsGA1UEChME\n" //
            + "QWNtZTEQMA4GA1UEAxMHVGVzdCBDQTAgFw0yNjEwMjgxNjQwNDNaGA8yMTI2MTAw\n" //
            + "NDE2NDA0M1owDzENMAsGA1UECxMEQWNtZTBZMBMGByqGSM49AgEGCCqGSM49AwEH\n" //
            + "A0IABHJwOp7LnHiPZAFI9CNnfF+nFHUDBSGu/w3DQFW/OyY/Ldx+FzQROCSAGuNA\n" //
            + "TRhmaA9gReIRloOGOZzMUyGlq9mjQjBAMB0GA1UdDgQWBBSwZf0MFQSm/ZTA8IW8\n" //
            + "V1zmscyFejAfBgNVHSMEGDAWgBRZvtwF27w5C7QQGXExSLt3d998zjAKBggqhkjO\n" //
            + "PQQDAwNIADBFAiBeWjVNN3XkuQMuhCA815QW/2UFNICWg2VDBn8atWL2WQIhAKYA\n" //
            + "DLmSTwGownrSLMArUF0fF8mbCAXEtyeOobT0d6Yl\n" //
            + "-----END CERTIFICATE-----\n";

    /** Signed by another CA, subject: CN=device-9, O=Acme. */
    private static final String CERT_UNTRUSTED = "-----BEGIN CERTIFICATE-----\n" //
            + "MIIBfjCCASSgAwIBAgIIG8yAkWYEK08wCgYIKoZIzj0EAwMwIjENMAsGA1UEChME\n" //
            + "QWNtZTERMA8GA1UEAxMIT3RoZXIgQ0EwIBcNMjYxMDI4MTY0MDMxWhgPMjEyNjEw\n" //
            + "MDQxNjQwMzFaMCIxDTALBgNVBAoTBEFjbWUxETAPBgNVBAMTCGRldmljZS05MFkw\n" //
            + "EwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAE20EavJsllXoZOaS2hGwMyXNx4P440FEt\n" //
            + "tYusehaIqllA9JzQmNW7x/TLb5k0FH3OsvV6Vj25RGAWKIgkRrZYSKNCMEAwHQYD\n" //
            + "VR0OBBYEFEA6oFb3euYRasc4ksBwOnQbMYC/MB8GA1UdIwQYMBaAFP5wYCuo9I4F\n" //
            + "AtW8NW8oC77uWLTvMAoGCCqGSM49BAMDA0gAMEUCIDT63YVLtGZSgaUnrxT1bwht\n" //
            + "U+psC2pVE8h3DXFDgEFJAiEAmRGkFfCGrMCvXg/ZY3AHNF7YO9OG92MhNwQnig6L\n" //
            + "7jg=\n" //
            + "-----END CERTIFICATE-----\n";

    private final ClientCertificateVerifier verifier;

    ClientCertificateVerifierTest() throws CertificateException {
        verifier = new ClientCertificateVerifier(Arrays.asList(parse(CA)));
    }

    @Test
    void certificate_with_one_common_name_is_accepted() {
        assertDoesNotThrow(() -> verify(CERT_ONE_CN));
    }

    @Test
    void certificate_with_several_common_names_is_rejected() {
        CertificateException exception = assertThrows(CertificateException.class, () -> verify(CERT_TWO_CN));

        assertTrue(exception.getMessage().contains("more than one CN"), exception.getMessage());
    }

    @Test
    void certificate_without_common_name_is_rejected() {
        CertificateException exception = assertThrows(CertificateException.class, () -> verify(CERT_NO_CN));

        assertTrue(exception.getMessage().contains("no CN"), exception.getMessage());
    }

    @Test
    void certificate_of_an_untrusted_issuer_is_still_rejected() {
        // the check of the common name is added to the validation of the chain, it does not replace it
        assertThrows(CertificateException.class, () -> verify(CERT_UNTRUSTED));
    }

    private void verify(String pem) throws CertificateException {
        verifier.verifyCertificate(CertPathUtil.generateCertPath(Arrays.asList(parse(pem))), CLIENT_ADDRESS,
                Role.CLIENT);
    }

    private static X509Certificate parse(String pem) throws CertificateException {
        return (X509Certificate) CertificateFactory.getInstance("X.509")
                .generateCertificate(new ByteArrayInputStream(pem.getBytes(StandardCharsets.US_ASCII)));
    }
}
