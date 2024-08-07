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
package org.eclipse.leshan.transport.californium.client.endpoint;

import java.net.InetAddress;
import java.security.cert.Certificate;
import java.util.List;

import org.eclipse.californium.core.network.CoapEndpoint;
import org.eclipse.californium.elements.config.Configuration;
import org.eclipse.leshan.client.endpoint.ClientEndpointToolbox;
import org.eclipse.leshan.client.servers.ServerInfo;
import org.eclipse.leshan.core.endpoint.Protocol;
import org.eclipse.leshan.transport.californium.ExceptionTranslator;
import org.eclipse.leshan.transport.californium.client.CaliforniumConnectionController;
import org.eclipse.leshan.transport.californium.identity.IdentityHandler;

public interface CaliforniumClientEndpointFactory {

    Protocol getProtocol();

    String getEndpointDescription();

    CoapEndpoint createCoapEndpoint(InetAddress clientAddress, Configuration defaultConfiguration,
            ServerInfo serverInfo, boolean clientInitiatedOnly, List<Certificate> trustStore,
            ClientEndpointToolbox toolbox);

    CaliforniumConnectionController createConnectionController();

    IdentityHandler createIdentityHandler();

    ExceptionTranslator createExceptionTranslator();
}
