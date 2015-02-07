/*******************************************************************************
 * Copyright (c) 2015 Sierra Wireless and others.
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
package org.eclipse.leshan.bootstrap;

import java.util.Arrays;
import java.util.Map;

import org.apache.commons.io.Charsets;
import org.eclipse.leshan.server.bootstrap.BootstrapConfig;
import org.eclipse.leshan.server.bootstrap.SecurityMode;
import org.eclipse.leshan.server.bootstrap.BootstrapConfig.ServerSecurity;
import org.eclipse.leshan.server.security.SecurityInfo;
import org.eclipse.leshan.server.security.SecurityStore;

/**
 * A DTLS security store using the provisioned bootstrap information for finding the DTLS/PSK credentials.
 */
public class BootstrapSecurityStore implements SecurityStore {

    private final BootstrapStoreImpl bsStore;

    public BootstrapSecurityStore(BootstrapStoreImpl bsStore) {
        this.bsStore = bsStore;
    }

    @Override
    public SecurityInfo getByIdentity(String identity) {
        byte[] identityBytes = identity.getBytes(Charsets.UTF_8);
        for (Map.Entry<String, BootstrapConfig> e : bsStore.getBootstrapConfigs().entrySet()) {
            for (Map.Entry<Integer, BootstrapConfig.ServerSecurity> ec : e.getValue().security.entrySet()) {
                if (ec.getValue().bootstrapServer && ec.getValue().securityMode == SecurityMode.PSK
                        && Arrays.equals(ec.getValue().publicKeyOrId, identityBytes)) {
                    return SecurityInfo.newPreSharedKeyInfo(e.getKey(), identity, ec.getValue().secretKey);
                }
            }
        }
        return null;
    }

    @Override
    public SecurityInfo getByEndpoint(String endpoint) {
        BootstrapConfig bootstrap = bsStore.getBootstrap(endpoint);

        for (Map.Entry<Integer, BootstrapConfig.ServerSecurity> e : bootstrap.security.entrySet()) {
            ServerSecurity value = e.getValue();
            if (value.bootstrapServer && value.securityMode == SecurityMode.PSK) {
                // got it!
                return SecurityInfo.newPreSharedKeyInfo(endpoint, new String(value.publicKeyOrId, Charsets.UTF_8),
                        value.secretKey);
            }
        }
        return null;
    }
}
