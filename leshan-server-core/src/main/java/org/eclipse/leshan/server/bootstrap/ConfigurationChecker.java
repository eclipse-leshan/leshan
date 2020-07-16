/*******************************************************************************
 * Copyright (c) 2014-2015 Sierra Wireless and others.
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

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.util.Arrays;
import java.util.Map;

import org.eclipse.leshan.core.util.SecurityUtil;
import org.eclipse.leshan.core.util.StringUtils;
import org.eclipse.leshan.server.bootstrap.BootstrapConfig.ServerSecurity;

/**
 * Check a BootstrapConfig is correct. This is a complex process, we need to check if the different objects are in
 * coherence with each other.
 */
public class ConfigurationChecker {

    /**
     * Verify if the {@link BootstrapConfig} is valid and consistent.
     * <p>
     * Raise a {@link InvalidConfigurationException} if config is not OK.
     * 
     * @param config the bootstrap configuration to check.
     * @throws InvalidConfigurationException if bootstrap configuration is not invalid.
     */
    public void verify(BootstrapConfig config) throws InvalidConfigurationException {
        // check security configurations
        for (Map.Entry<Integer, BootstrapConfig.ServerSecurity> e : config.security.entrySet()) {
            BootstrapConfig.ServerSecurity sec = e.getValue();

            // checks security config
            switch (sec.securityMode) {
            case NO_SEC:
                checkNoSec(sec);
                break;
            case PSK:
                checkPSK(sec);
                break;
            case RPK:
                checkRPK(sec);
                break;
            case X509:
                checkX509(sec);
                break;
            case EST:
                throw new InvalidConfigurationException("EST is not currently supported.", e);
            }

            validateMandatoryField(sec);
        }

        // does each server have a corresponding security entry?
        validateOneSecurityByServer(config);
    }

    protected void checkNoSec(ServerSecurity sec) throws InvalidConfigurationException {
        assertIf(!isEmpty(sec.secretKey), "NO-SEC mode, secret key must be empty");
        assertIf(!isEmpty(sec.publicKeyOrId), "NO-SEC mode, public key or ID must be empty");
        assertIf(!isEmpty(sec.serverPublicKey), "NO-SEC mode, server public key must be empty");
    }

    protected void checkPSK(ServerSecurity sec) throws InvalidConfigurationException {
        assertIf(isEmpty(sec.secretKey), "pre-shared-key mode, secret key must not be empty");
        assertIf(isEmpty(sec.publicKeyOrId), "pre-shared-key mode, public key or id must not be empty");

        String identity = new String(sec.publicKeyOrId, StandardCharsets.UTF_8);
        assertIf(!Arrays.equals(sec.publicKeyOrId, identity.getBytes()),
                "pre-shared-key mode, public key or id must not be an utf8 string");
    }

    protected void checkRPK(ServerSecurity sec) throws InvalidConfigurationException {
        assertIf(isEmpty(sec.secretKey), "raw-public-key mode, secret key must not be empty");
        assertIf(decodeRfc5958PrivateKey(sec.secretKey) == null,
                "raw-public-key mode, secret key must be RFC5958 encoded private key");
        assertIf(isEmpty(sec.publicKeyOrId), "raw-public-key mode, public key or id must not be empty");
        assertIf(decodeRfc7250PublicKey(sec.publicKeyOrId) == null,
                "raw-public-key mode, public key or id must be RFC7250 encoded public key");
        assertIf(isEmpty(sec.serverPublicKey), "raw-public-key mode, server public key must not be empty");
        assertIf(decodeRfc7250PublicKey(sec.serverPublicKey) == null,
                "raw-public-key mode, server public key must be RFC7250 encoded public key");
    }

    protected void checkX509(ServerSecurity sec) throws InvalidConfigurationException {
        assertIf(isEmpty(sec.secretKey), "x509 mode, secret key must not be empty");
        assertIf(decodeRfc5958PrivateKey(sec.secretKey) == null,
                "x509 mode, secret key must be RFC5958 encoded private key");
        assertIf(isEmpty(sec.publicKeyOrId), "x509 mode, public key or id must not be empty");
        assertIf(decodeCertificate(sec.publicKeyOrId) == null,
                "x509 mode, public key or id must be DER encoded X.509 certificate");
        assertIf(isEmpty(sec.serverPublicKey), "x509 mode, server public key must not be empty");
        assertIf(decodeCertificate(sec.serverPublicKey) == null,
                "x509 mode, server public key must be DER encoded X.509 certificate");
    }

    protected void validateMandatoryField(ServerSecurity sec) throws InvalidConfigurationException {
        // checks mandatory fields
        if (StringUtils.isEmpty(sec.uri))
            throw new InvalidConfigurationException("LwM2M Server URI is mandatory");
        if (sec.securityMode == null)
            throw new InvalidConfigurationException("Security Mode is mandatory");

    }

    /**
     * Each server entry must have 1 security entry.
     * 
     * @param config the bootstrap configuration to check.
     * @throws InvalidConfigurationException if bootstrap configuration is not invalid.
     */
    protected void validateOneSecurityByServer(BootstrapConfig config) throws InvalidConfigurationException {
        for (Map.Entry<Integer, BootstrapConfig.ServerConfig> e : config.servers.entrySet()) {
            BootstrapConfig.ServerConfig srvCfg = e.getValue();

            // shortId checks
            if (srvCfg.shortId == 0) {
                throw new InvalidConfigurationException("short ID must not be 0");
            }

            // look for security entry
            BootstrapConfig.ServerSecurity security = getSecurityEntry(config, srvCfg.shortId);

            if (security == null) {
                throw new InvalidConfigurationException("no security entry for server instance: " + e.getKey());
            }

            if (security.bootstrapServer) {
                throw new InvalidConfigurationException(
                        "the security entry for server  " + e.getKey() + " should not be a bootstrap server");
            }
        }
    }

    protected PrivateKey decodeRfc5958PrivateKey(byte[] encodedKey) throws InvalidConfigurationException {
        try {
            return SecurityUtil.privateKey.decode(encodedKey);
        } catch (IOException | GeneralSecurityException e) {
            throw new InvalidConfigurationException("Failed to decode RFC5958 private key", e);
        }
    }

    protected PublicKey decodeRfc7250PublicKey(byte[] encodedKey) throws InvalidConfigurationException {
        try {
            return SecurityUtil.publicKey.decode(encodedKey);
        } catch (IOException | GeneralSecurityException e) {
            throw new InvalidConfigurationException("Failed to decode RFC7250 public key", e);
        }
    }

    protected Certificate decodeCertificate(byte[] encodedCert) throws InvalidConfigurationException {
        try {
            return SecurityUtil.certificate.decode(encodedCert);
        } catch (IOException | GeneralSecurityException e) {
            throw new InvalidConfigurationException("Failed to decode X.509 certificate", e);
        }
    }

    protected static void assertIf(boolean condition, String message) throws InvalidConfigurationException {
        if (condition) {
            throw new InvalidConfigurationException(message);
        }
    }

    protected static boolean isEmpty(byte[] array) {
        return array == null || array.length == 0;
    }

    protected static BootstrapConfig.ServerSecurity getSecurityEntry(BootstrapConfig config, int shortId) {
        for (Map.Entry<Integer, BootstrapConfig.ServerSecurity> es : config.security.entrySet()) {
            if (es.getValue().serverId == shortId) {
                return es.getValue();
            }
        }
        return null;
    }
}
