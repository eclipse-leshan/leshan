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
package org.eclipse.leshan.server.californium.bootstrap.endpoint;

import java.net.URI;
import java.util.List;

import org.eclipse.californium.elements.config.Configuration;
import org.eclipse.californium.elements.config.Configuration.ModuleDefinitionsProvider;
import org.eclipse.leshan.core.endpoint.Protocol;

public interface BootstrapServerProtocolProvider {

    Protocol getProtocol();

    List<ModuleDefinitionsProvider> getModuleDefinitionsProviders();

    void applyDefaultValue(Configuration configuration);

    CaliforniumBootstrapServerEndpointFactory createDefaultEndpointFactory(URI uri);

    URI getDefaultUri(Configuration configuration);
}
