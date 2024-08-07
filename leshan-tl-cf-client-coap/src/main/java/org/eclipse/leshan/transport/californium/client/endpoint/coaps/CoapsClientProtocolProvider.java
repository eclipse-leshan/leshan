/*******************************************************************************
 * Copyright (c) 2022 Sierra Wireless and others.
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
package org.eclipse.leshan.transport.californium.client.endpoint.coaps;

import java.util.Arrays;
import java.util.List;

import org.eclipse.californium.core.config.CoapConfig;
import org.eclipse.californium.core.config.CoapConfig.TrackerMode;
import org.eclipse.californium.elements.config.Configuration;
import org.eclipse.californium.elements.config.Configuration.ModuleDefinitionsProvider;
import org.eclipse.californium.elements.config.SystemConfig;
import org.eclipse.californium.elements.config.UdpConfig;
import org.eclipse.californium.scandium.config.DtlsConfig;
import org.eclipse.leshan.core.endpoint.Protocol;
import org.eclipse.leshan.transport.californium.client.endpoint.CaliforniumClientEndpointFactory;
import org.eclipse.leshan.transport.californium.client.endpoint.ClientProtocolProvider;

public class CoapsClientProtocolProvider implements ClientProtocolProvider {

    @Override
    public Protocol getProtocol() {
        return Protocol.COAPS;
    }

    @Override
    public void applyDefaultValue(Configuration configuration) {
        configuration.set(CoapConfig.MID_TRACKER, TrackerMode.NULL);
        configuration.set(CoapConfig.MAX_ACTIVE_PEERS, 10);
        configuration.set(CoapConfig.PROTOCOL_STAGE_THREAD_COUNT, 1);
        configuration.set(DtlsConfig.DTLS_MAX_CONNECTIONS, 10);
        configuration.set(DtlsConfig.DTLS_RECEIVER_THREAD_COUNT, 1);
        configuration.set(DtlsConfig.DTLS_CONNECTOR_THREAD_COUNT, 1);
        // currently not supported by leshan's CertificateVerifier
        configuration.setTransient(DtlsConfig.DTLS_VERIFY_SERVER_CERTIFICATES_SUBJECT);
        // Set it to null to allow automatic mode
        // See org.eclipse.leshan.transport.californium.client.endpoint.CoapsEndpointFactory
        configuration.set(DtlsConfig.DTLS_ROLE, null);
    }

    @Override
    public List<ModuleDefinitionsProvider> getModuleDefinitionsProviders() {
        return Arrays.asList(SystemConfig.DEFINITIONS, CoapConfig.DEFINITIONS, UdpConfig.DEFINITIONS,
                DtlsConfig.DEFINITIONS);
    }

    @Override
    public CaliforniumClientEndpointFactory createDefaultEndpointFactory() {
        return new CoapsClientEndpointFactory();
    }
}
