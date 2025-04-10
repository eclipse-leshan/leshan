/*******************************************************************************
 * Copyright (c) 2025 Sierra Wireless and others.
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
package org.eclipse.leshan.bsserver;

import static org.eclipse.leshan.core.util.TestCredentialsUtil.CLIENT_CERT_DER;
import static org.eclipse.leshan.core.util.TestCredentialsUtil.CLIENT_CERT_PEM;
import static org.eclipse.leshan.core.util.TestCredentialsUtil.CLIENT_PRIVATE_KEY_DER;
import static org.eclipse.leshan.core.util.TestCredentialsUtil.SERVER_CERT_DER;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;

import org.eclipse.leshan.bsserver.BootstrapConfig.ServerConfig;
import org.eclipse.leshan.bsserver.BootstrapConfig.ServerSecurity;
import org.eclipse.leshan.core.SecurityMode;
import org.eclipse.leshan.core.util.TestCredentialsUtil.Credential;
import org.junit.jupiter.api.Test;

class DefaultConfigurationCheckerTest {

    ConfigurationChecker configurationChecker = new DefaultConfigurationChecker();

    @Test
    void test_valid_configuration_using_certificate_with_der_endoding() {
        assertDoesNotThrow(() -> {
            configurationChecker.verify(givenBootstrapConfigWith(CLIENT_CERT_DER));
        });
    }

    @Test
    void test_invalid_configuration_using_certificate_with_pem_endoding() {
        assertThrowsExactly(InvalidConfigurationException.class, () -> {
            configurationChecker.verify(givenBootstrapConfigWith(CLIENT_CERT_PEM));
        });
    }

    BootstrapConfig givenBootstrapConfigWith(Credential clientCertificate) {
        BootstrapConfig config = new BootstrapConfig();

        // security for DM server
        ServerSecurity dmSecurity = new ServerSecurity();
        dmSecurity.uri = "coaps://localhost:5684";
        dmSecurity.serverId = 2222;
        dmSecurity.securityMode = SecurityMode.X509;
        dmSecurity.publicKeyOrId = clientCertificate.asByteArray();
        dmSecurity.secretKey = CLIENT_PRIVATE_KEY_DER.asByteArray();
        dmSecurity.serverPublicKey = SERVER_CERT_DER.asByteArray();
        config.security.put(1, dmSecurity);

        // DM server
        ServerConfig dmConfig = new ServerConfig();
        dmConfig.shortId = 2222;
        config.servers.put(0, dmConfig);

        return config;
    }
}
