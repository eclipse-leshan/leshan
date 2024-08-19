/*******************************************************************************
 * Copyright (c) 2013-2015 Sierra Wireless and others.
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
 *     Rikard Höglund (RISE) - additions to support OSCORE
 *******************************************************************************/
package org.eclipse.leshan.bsserver;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.leshan.bsserver.BootstrapConfig.ServerSecurity;
import org.eclipse.leshan.core.SecurityMode;

/**
 * Simple bootstrap store implementation storing bootstrap configuration information in memory.
 *
 */
public class InMemoryBootstrapConfigStore implements EditableBootstrapConfigStore {

    protected final ConfigurationChecker configChecker = new ConfigurationChecker();

    protected final Map<String /* endpoint */, BootstrapConfig> bootstrapByEndpoint = new ConcurrentHashMap<>();
    protected final ConcurrentHashMap<PskByServer, BootstrapConfig> bootstrapByPskId = new ConcurrentHashMap<>();

    @Override
    public BootstrapConfig get(BootstrapSession session) {
        return bootstrapByEndpoint.get(session.getEndpoint());
    }

    @Override
    public synchronized void add(String endpoint, BootstrapConfig config) throws InvalidConfigurationException {
        checkConfig(endpoint, config);

        // Check PSK identity uniqueness for bootstrap server:
        PskByServer pskToAdd = getBootstrapPskIdentity(config);
        if (pskToAdd != null) {
            BootstrapConfig existingConfig = bootstrapByPskId.get(pskToAdd);
            if (existingConfig != null) {
                // check if this config will be replace by the new one.
                BootstrapConfig previousConfig = bootstrapByEndpoint.get(endpoint);
                if (previousConfig != existingConfig) {
                    throw new InvalidConfigurationException(
                            "Psk identity [%s] already used for this bootstrap server [%s]", pskToAdd.identity,
                            pskToAdd.serverUrl);
                }
            }
        }
        // TODO we should probably also check lwm2m server
        // TODO OSCORE we should also check recipient ID uniqueness.

        // About this uniqueness check above,
        // This was introduced because of : https://github.com/eclipse/leshan/issues/322
        // but with https://github.com/eclipse/leshan/issues/1112
        // I'm not sure this still makes sense...

        bootstrapByEndpoint.put(endpoint, config);
        if (pskToAdd != null) {
            bootstrapByPskId.put(pskToAdd, config);
        }
    }

    protected void checkConfig(String endpoint, BootstrapConfig config) throws InvalidConfigurationException {
        configChecker.verify(config);
    }

    @Override
    public synchronized BootstrapConfig remove(String enpoint) {
        BootstrapConfig bootstrapConfig = bootstrapByEndpoint.remove(enpoint);
        if (bootstrapConfig != null) {
            PskByServer pskIdentity = getBootstrapPskIdentity(bootstrapConfig);
            if (pskIdentity != null) {
                bootstrapByPskId.remove(pskIdentity, bootstrapConfig);
            }
        }
        return bootstrapConfig;
    }

    protected PskByServer getBootstrapPskIdentity(BootstrapConfig config) {
        for (ServerSecurity security : config.security.values()) {
            if (security.bootstrapServer) {
                if (security.securityMode == SecurityMode.PSK) {
                    return new PskByServer(security.uri, new String(security.publicKeyOrId));
                }
            }
        }
        return null;
    }

    @Override
    public Map<String, BootstrapConfig> getAll() {
        return Collections.unmodifiableMap(bootstrapByEndpoint);
    }

    protected static class PskByServer {
        public final String serverUrl;
        public final String identity;

        public PskByServer(String serverUrl, String identity) {
            this.serverUrl = serverUrl;
            this.identity = identity;
        }

        @Override
        public final boolean equals(Object o) {
            if (this == o)
                return true;
            if (!(o instanceof PskByServer))
                return false;
            PskByServer that = (PskByServer) o;
            return Objects.equals(serverUrl, that.serverUrl) && Objects.equals(identity, that.identity);
        }

        @Override
        public final int hashCode() {
            return Objects.hash(serverUrl, identity);
        }
    }
}
