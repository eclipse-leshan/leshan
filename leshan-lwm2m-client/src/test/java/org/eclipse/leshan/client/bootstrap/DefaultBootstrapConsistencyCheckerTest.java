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
package org.eclipse.leshan.client.bootstrap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.leshan.core.util.TestCredentialsUtil.CLIENT_CERT_DER;
import static org.eclipse.leshan.core.util.TestCredentialsUtil.CLIENT_CERT_PEM;
import static org.eclipse.leshan.core.util.TestCredentialsUtil.CLIENT_PRIVATE_KEY_DER;
import static org.eclipse.leshan.core.util.TestCredentialsUtil.SERVER_CERT_DER;

import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.eclipse.leshan.client.object.Device;
import org.eclipse.leshan.client.object.Security;
import org.eclipse.leshan.client.object.Server;
import org.eclipse.leshan.client.resource.LwM2mObjectEnabler;
import org.eclipse.leshan.client.resource.ObjectsInitializer;
import org.eclipse.leshan.client.servers.DefaultServersInfoExtractor;
import org.eclipse.leshan.core.LwM2mId;
import org.eclipse.leshan.core.endpoint.Protocol;
import org.eclipse.leshan.core.request.BindingMode;
import org.eclipse.leshan.core.util.TestCredentialsUtil.Credential;
import org.junit.jupiter.api.Test;

class DefaultBootstrapConsistencyCheckerTest {

    public BootstrapConsistencyChecker configurationChecker = new DefaultBootstrapConsistencyChecker(
            new DefaultServersInfoExtractor());

    @Test
    void test_valid_configuration_using_certificate_with_der_endoding() {
        List<String> errors = configurationChecker.checkconfig(givenBootstrapConfigWith(CLIENT_CERT_DER));
        assertThat(errors).isNull();
    }

    @Test
    void test_invalid_configuration_using_certificate_with_pem_endoding() {
        List<String> errors = configurationChecker.checkconfig(givenBootstrapConfigWith(CLIENT_CERT_PEM));
        assertThat(errors).hasSize(1);
    }

    Map<Integer, LwM2mObjectEnabler> givenBootstrapConfigWith(Credential clientCertificate) {
        ObjectsInitializer initializer = new ObjectsInitializer();
        String defaultServerUri = "coap://leshan.eclipseprojects.io:5683";
        BindingMode serverBindingMode = BindingMode.fromProtocol(Protocol.fromUri(defaultServerUri));

        byte[] clientPrivateKey = CLIENT_PRIVATE_KEY_DER.asByteArray();
        byte[] serverCertificate = SERVER_CERT_DER.asByteArray();

        initializer.setInstancesForObject(LwM2mId.SECURITY, Security.x509(defaultServerUri, 12345,
                clientCertificate.asByteArray(), clientPrivateKey, serverCertificate));
        initializer.setInstancesForObject(LwM2mId.SERVER,
                new Server(12345, 5l * 60, EnumSet.of(serverBindingMode), false, serverBindingMode));
        initializer.setInstancesForObject(LwM2mId.DEVICE,
                new Device("Eclipse Leshan", "model12345", "12345", EnumSet.of(serverBindingMode)));

        List<LwM2mObjectEnabler> objs = initializer.createAll();

        return objs.stream().collect(Collectors.toMap(obj -> obj.getId(), obj -> obj));
    }
}
