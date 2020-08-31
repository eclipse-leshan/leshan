/*******************************************************************************
 * Copyright (c) 2020 Sierra Wireless and others.
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
package org.eclipse.leshan.server.bootstrap;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.leshan.core.request.Identity;

/**
 * Simple bootstrap store implementation storing bootstrap configuration information in memory.
 */
public class InMemoryBootstrapConfigurationStore implements EditableBootstrapConfigurationStore {
    protected final ConfigurationChecker configChecker = new ConfigurationChecker();

    protected final Map<String /* endpoint */, BootstrapConfiguration> bootstrapByEndpoint = new ConcurrentHashMap<>();

    @Override
    public BootstrapConfiguration get(String endpoint, Identity deviceIdentity, BootstrapSession session) {
        return bootstrapByEndpoint.get(endpoint);
    }

    @Override
    public synchronized void add(String endpoint, BootstrapConfiguration config) throws InvalidConfigurationException {
        bootstrapByEndpoint.put(endpoint, config);
    }

    @Override
    public synchronized BootstrapConfiguration remove(String enpoint) {
        BootstrapConfiguration bootstrapConfig = bootstrapByEndpoint.remove(enpoint);
        return bootstrapConfig;
    }
}