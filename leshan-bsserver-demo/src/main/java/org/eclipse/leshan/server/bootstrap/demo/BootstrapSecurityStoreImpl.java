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

import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.eclipse.leshan.SecurityMode;
import org.eclipse.leshan.server.bootstrap.BootstrapConfig;
import org.eclipse.leshan.server.bootstrap.BootstrapConfig.ServerSecurity;
import org.eclipse.leshan.server.security.BootstrapSecurityStore;
import org.eclipse.leshan.server.security.SecurityInfo;
import org.eclipse.leshan.util.SecurityUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A DTLS security store using the provisioned bootstrap information for finding the DTLS/PSK credentials.
 */
public class BootstrapSecurityStoreImpl implements BootstrapSecurityStore {

    private static final Logger LOG = LoggerFactory.getLogger(BootstrapSecurityStoreImpl.class);

    private final BootstrapStoreImpl bsStore;

    public BootstrapSecurityStoreImpl(BootstrapStoreImpl bsStore) {
        this.bsStore = bsStore;
    }

    @Override
    public SecurityInfo getByIdentity(String identity) {
        byte[] identityBytes = identity.getBytes(StandardCharsets.UTF_8);
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

        BootstrapConfig bsConfig = bsStore.getBootstrap(endpoint, null);

        if (bsConfig == null || bsConfig.security == null)
            return null;

        for (Map.Entry<Integer, BootstrapConfig.ServerSecurity> bsEntry : bsConfig.security.entrySet()) {
            ServerSecurity value = bsEntry.getValue();

            // Extract PSK identity
            if (value.bootstrapServer && value.securityMode == SecurityMode.PSK) {
                SecurityInfo securityInfo = SecurityInfo.newPreSharedKeyInfo(endpoint,
                        new String(value.publicKeyOrId, StandardCharsets.UTF_8), value.secretKey);
                return Arrays.asList(securityInfo);
            }
            // Extract RPK identity
            else if (value.bootstrapServer && value.securityMode == SecurityMode.RPK) {
                try {
                    SecurityInfo securityInfo = SecurityInfo.newRawPublicKeyInfo(endpoint,
                            SecurityUtil.extractPublicKey(value.publicKeyOrId));
                    return Arrays.asList(securityInfo);
                } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
                    LOG.error("Unable to decode Client public key for {}", endpoint, e);
                    return null;
                }
            }
        }
        return null;

    }
}
