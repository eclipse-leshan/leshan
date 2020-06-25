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
 *     Rikard Höglund (RISE) - additions to support OSCORE
 *******************************************************************************/
package org.eclipse.leshan.server.bootstrap.demo;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.eclipse.californium.oscore.HashMapCtxDB;
import org.eclipse.californium.oscore.OSCoreCtx;
import org.eclipse.leshan.SecurityMode;
import org.eclipse.leshan.server.OscoreHandler;
import org.eclipse.leshan.server.bootstrap.BootstrapConfig;
import org.eclipse.leshan.server.bootstrap.BootstrapConfig.OscoreObject;
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

        // Extract OSCORE security info
        if (bsConfig != null && bsConfig.oscore != null && !bsConfig.oscore.isEmpty()) {
            LOG.trace("Extracting OSCORE security info for endpoint {}", endpoint);

            // First find the context for this endpoint
            for (Map.Entry<Integer, BootstrapConfig.OscoreObject> oscoreEntry : bsConfig.oscore.entrySet()) {
                OscoreObject value = oscoreEntry.getValue();

                HashMapCtxDB db = OscoreHandler.getContextDB();
                OSCoreCtx ctx = db.getContext(value.oscoreRecipientId);

                // Create the security info (will re-add the context to the db)
                SecurityInfo securityInfo = SecurityInfo.newOSCoreInfo(endpoint, ctx);

                return Arrays.asList(securityInfo);
            }
        }

        if (bsConfig == null || bsConfig.security == null)
            return null;

        for (Map.Entry<Integer, BootstrapConfig.ServerSecurity> bsEntry : bsConfig.security.entrySet()) {
            ServerSecurity value = bsEntry.getValue();

            // Extract PSK security info
            if (value.bootstrapServer && value.securityMode == SecurityMode.PSK) {
                SecurityInfo securityInfo = SecurityInfo.newPreSharedKeyInfo(endpoint,
                        new String(value.publicKeyOrId, StandardCharsets.UTF_8), value.secretKey);
                return Arrays.asList(securityInfo);
            }
            // Extract RPK security info
            else if (value.bootstrapServer && value.securityMode == SecurityMode.RPK) {
                try {
                    SecurityInfo securityInfo = SecurityInfo.newRawPublicKeyInfo(endpoint,
                            SecurityUtil.publicKey.decode(value.publicKeyOrId));
                    return Arrays.asList(securityInfo);
                } catch (IOException | GeneralSecurityException e) {
                    LOG.error("Unable to decode Client public key for {}", endpoint, e);
                    return null;
                }
            }
            // Extract X509 security info
            else if (value.bootstrapServer && value.securityMode == SecurityMode.X509) {
                SecurityInfo securityInfo = SecurityInfo.newX509CertInfo(endpoint);
                return Arrays.asList(securityInfo);
            }
        }
        return null;

    }
}
