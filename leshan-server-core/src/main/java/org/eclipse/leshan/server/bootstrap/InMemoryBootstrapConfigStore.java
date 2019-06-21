/*******************************************************************************
 * Copyright (c) 2013-2015 Sierra Wireless and others.
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v1.0 which accompany this distribution.
 * 
 * The Eclipse Public License is available at
 *    http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 *    http://www.eclipse.org/org/documents/edl-v10.html.
 * 
 * Contributors:
 *     Sierra Wireless - initial API and implementation
 *******************************************************************************/
package org.eclipse.leshan.server.bootstrap;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.leshan.core.request.Identity;

/**
 * Simple bootstrap store implementation storing bootstrap configuration information in memory.
 */
public class InMemoryBootstrapConfigStore implements EditableBootstrapConfigStore {

    protected final ConfigurationChecker configChecker = new ConfigurationChecker();
    protected final Map<String, BootstrapConfig> bootstrapByEndpoint = new ConcurrentHashMap<>();

    @Override
    public BootstrapConfig getBootstrap(String endpoint, Identity deviceIdentity) {
        return bootstrapByEndpoint.get(endpoint);
    }

    @Override
    public void addConfig(String endpoint, BootstrapConfig config) throws InvalidConfigurationException {
        checkConfig(endpoint, config);
        bootstrapByEndpoint.put(endpoint, config);
    }

    protected void checkConfig(String endpoint, BootstrapConfig config) throws InvalidConfigurationException {
        configChecker.verify(config);
    }

    @Override
    public BootstrapConfig removeConfig(String enpoint) {
        return bootstrapByEndpoint.remove(enpoint);

    }

    @Override
    public Map<String, BootstrapConfig> getBootstrapConfigs() {
        return Collections.unmodifiableMap(bootstrapByEndpoint);
    }
}
