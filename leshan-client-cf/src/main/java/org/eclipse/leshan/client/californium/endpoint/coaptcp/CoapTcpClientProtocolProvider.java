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
package org.eclipse.leshan.client.californium.endpoint.coaptcp;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.eclipse.californium.core.config.CoapConfig;
import org.eclipse.californium.elements.config.Configuration;
import org.eclipse.californium.elements.config.Configuration.ModuleDefinitionsProvider;
import org.eclipse.californium.elements.config.SystemConfig;
import org.eclipse.californium.elements.config.TcpConfig;
import org.eclipse.leshan.client.californium.endpoint.CaliforniumClientEndpointFactory;
import org.eclipse.leshan.client.californium.endpoint.ClientProtocolProvider;
import org.eclipse.leshan.core.endpoint.Protocol;

public class CoapTcpClientProtocolProvider implements ClientProtocolProvider {

    @Override
    public Protocol getProtocol() {
        return Protocol.COAP_TCP;
    }

    @Override
    public void applyDefaultValue(Configuration configuration) {
        configuration.set(CoapConfig.MAX_ACTIVE_PEERS, 10);
        configuration.set(CoapConfig.PROTOCOL_STAGE_THREAD_COUNT, 1);
        configuration.set(TcpConfig.TCP_CONNECTION_IDLE_TIMEOUT, 0, TimeUnit.SECONDS);
    }

    @Override
    public List<ModuleDefinitionsProvider> getModuleDefinitionsProviders() {
        return Arrays.asList(SystemConfig.DEFINITIONS, CoapConfig.DEFINITIONS, TcpConfig.DEFINITIONS);
    }

    @Override
    public CaliforniumClientEndpointFactory createDefaultEndpointFactory() {
        return new CoapTcpClientEndpointFactory();
    }
}
