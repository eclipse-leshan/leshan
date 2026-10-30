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
package org.eclipse.leshan.transport.californium.server.endpoint.coaps;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.eclipse.californium.scandium.dtls.x509.CertificateVerifier;
import org.eclipse.californium.scandium.dtls.x509.StaticCertificateVerifier;
import org.eclipse.leshan.core.endpoint.EndpointUri;
import org.eclipse.leshan.transport.californium.security.ClientCommonNameCertificateVerifier;
import org.junit.jupiter.api.Test;

class CoapsServerEndpointFactoryTest {

    private final EndpointUri uri = new EndpointUri("coaps", "localhost", 5684);
    private final CertificateVerifier chainVerifier = StaticCertificateVerifier.builder().setTrustAllRPKs().build();

    @Test
    void certificates_of_the_clients_must_have_a_common_name_by_default() {
        CoapsServerEndpointFactory factory = new CoapsServerEndpointFactory(uri);
        CertificateVerifier verifier = factory.createClientCertificateVerifier(chainVerifier);

        assertTrue(verifier instanceof ClientCommonNameCertificateVerifier);
    }

    @Test
    void factory_which_identifies_the_clients_in_another_way_can_skip_the_check() {
        CoapsServerEndpointFactory factory = new CoapsServerEndpointFactory(uri) {
            @Override
            protected CertificateVerifier createClientCertificateVerifier(CertificateVerifier chainVerifier) {
                return chainVerifier;
            }
        };

        assertSame(chainVerifier, factory.createClientCertificateVerifier(chainVerifier));
    }
}
