/*******************************************************************************
 * Copyright (c) 2014-2015 Sierra Wireless and others.
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

import java.util.Map;

import org.apache.commons.lang.ArrayUtils;
import org.eclipse.leshan.server.bootstrap.BootstrapConfig;

/**
 * Check it's a BoostrapConfig is correct. this is a complex process, we need to check if the different objects are in
 * coherence with each others.
 */
public class ConfigurationChecker {

    public static void verify(BootstrapConfig config) throws ConfigurationException {
        // check security configurations
        for (Map.Entry<Integer, BootstrapConfig.ServerSecurity> e : config.security.entrySet()) {
            BootstrapConfig.ServerSecurity sec = e.getValue();
            switch (sec.securityMode) {
            case NO_SEC:
                assertIf(!ArrayUtils.isEmpty(sec.secretKey), "NO-SEC mode, secret key must be empty");
                assertIf(!ArrayUtils.isEmpty(sec.publicKeyOrId), "NO-SEC mode, public key or ID must be empty");
                assertIf(!ArrayUtils.isEmpty(sec.serverPublicKeyOrId),
                        "NO-SEC mode, server public key or ID must be empty");
                break;
            case PSK:
                assertIf(ArrayUtils.isEmpty(sec.secretKey), "pre-shared-key mode, secret key must not be empty");
                assertIf(ArrayUtils.isEmpty(sec.publicKeyOrId),
                        "pre-shared-key mode, public key or id must not be empty");
                break;
            case RPK:
                assertIf(ArrayUtils.isEmpty(sec.secretKey), "pre-shared-key mode, secret key must not be empty");
                assertIf(ArrayUtils.isEmpty(sec.publicKeyOrId),
                        "pre-shared-key mode, public key or id must not be empty");
                assertIf(ArrayUtils.isEmpty(sec.serverPublicKeyOrId),
                        "pre-shared-key mode, server public key or ID must not be empty");
                break;
            case X509:
                assertIf(ArrayUtils.isEmpty(sec.secretKey), "pre-shared-key mode, secret key must not be empty");
                assertIf(ArrayUtils.isEmpty(sec.publicKeyOrId),
                        "pre-shared-key mode, public key or id must not be empty");
                assertIf(ArrayUtils.isEmpty(sec.serverPublicKeyOrId),
                        "pre-shared-key mode, server public key or ID must not be empty");
            default:
                break;
            }
        }

        // does each server have a corresponding security entry?
        for (Map.Entry<Integer, BootstrapConfig.ServerConfig> e : config.servers.entrySet()) {
            BootstrapConfig.ServerConfig srvCfg = e.getValue();

            // shortId checks
            if (srvCfg.shortId == 0) {
                throw new ConfigurationException("short ID must not be 0");
            }

            // look for security entry
            BootstrapConfig.ServerSecurity security = getSecurityEntry(config, srvCfg.shortId);

            if (security == null) {
                throw new ConfigurationException("no security entry for server instance: " + e.getKey());
            }

            if (security.bootstrapServer) {
                throw new ConfigurationException("the security entry for server  " + e.getKey()
                        + " should not be a boostrap server");
            }
        }
    }

    private static void assertIf(boolean condition, String message) throws ConfigurationException {
        if (condition) {
            throw new ConfigurationException(message);
        }

    }

    private static BootstrapConfig.ServerSecurity getSecurityEntry(BootstrapConfig config, int shortId) {
        for (Map.Entry<Integer, BootstrapConfig.ServerSecurity> es : config.security.entrySet()) {
            if (es.getValue().serverId == shortId) {
                return es.getValue();
            }
        }
        return null;
    }

    public static class ConfigurationException extends Exception {

        private static final long serialVersionUID = 1L;

        public ConfigurationException(String message) {
            super(message);
        }
    }
}
