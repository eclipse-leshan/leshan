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
package org.eclipse.leshan.server.bootstrap.demo;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.Charsets;
import org.eclipse.leshan.server.bootstrap.BootstrapConfig;
import org.eclipse.leshan.server.bootstrap.BootstrapConfig.ServerSecurity;
import org.eclipse.leshan.server.bootstrap.SecurityMode;
import org.eclipse.leshan.server.security.BootstrapSecurityStore;
import org.eclipse.leshan.server.security.SecurityInfo;

/**
 * A DTLS security store using the provisioned bootstrap information for finding the DTLS/PSK credentials.
 */
public class BootstrapSecurityStoreImpl implements BootstrapSecurityStore {

    private final BootstrapStoreImpl bsStore;

    public BootstrapSecurityStoreImpl(BootstrapStoreImpl bsStore) {
        this.bsStore = bsStore;
    }

    @Override
    public SecurityInfo getByIdentity(String identity) {
        byte[] identityBytes = identity.getBytes(Charsets.UTF_8);
        for (Map.Entry<String, BootstrapConfig> e : bsStore.getBootstrapConfigs().entrySet()) {
            BootstrapConfig bsConfig = e.getValue();
            if (bsConfig.security != null) {
                for (Map.Entry<Integer, BootstrapConfig.ServerSecurity> ec : bsConfig.security.entrySet()) {
                    ServerSecurity serverSecurity = ec.getValue();
                    if (serverSecurity.bootstrapServer && serverSecurity.securityMode == SecurityMode.PSK
                            && Arrays.equals(serverSecurity.publicKeyOrId, identityBytes)) {
                        return SecurityInfo.newPreSharedKeyInfo(e.getKey(), identity, serverSecurity.secretKey);
                    }
                }
            }
        }
        return null;
    }

    @Override
    public List<SecurityInfo> getAllByEndpoint(String endpoint) {

        BootstrapConfig bsConfig = bsStore.getBootstrap(endpoint);

        if (bsConfig == null || bsConfig.security == null)
            return null;

        for (Map.Entry<Integer, BootstrapConfig.ServerSecurity> e : bsConfig.security.entrySet()) {
            ServerSecurity value = e.getValue();
            if (value.bootstrapServer && value.securityMode == SecurityMode.PSK) {
                // got it!
                SecurityInfo securityInfo = SecurityInfo.newPreSharedKeyInfo(endpoint,
                        new String(value.publicKeyOrId, Charsets.UTF_8), value.secretKey);
                return Arrays.asList(securityInfo);
            }
        }
        return null;

    }
}
