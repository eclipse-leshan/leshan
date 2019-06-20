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
package org.eclipse.leshan.server.bootstrap;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Map;

import org.eclipse.leshan.server.bootstrap.BootstrapConfig.ServerSecurity;
import org.eclipse.leshan.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Check a BootstrapConfig is correct. This is a complex process, we need to check if the different objects are in
 * coherence with each other.
 */
public class ConfigurationChecker {

    private static final Logger LOG = LoggerFactory.getLogger(ConfigurationChecker.class);

    protected final String[] algorithms;

    /**
     * Create a new configuration checker supporting "EC", "DiffieHellman", "RSA", "DSA" algorithm for Public and
     * Private keys
     */
    public ConfigurationChecker() {
        this(new String[] { "EC", "DiffieHellman", "RSA", "DSA" });
    }

    /**
     * Create a new configuration checker supporting given algorithms.
     * 
     * @param algorithms an array of supported algorithm name. (see {@link KeyFactory#getInstance(String))}
     */
    public ConfigurationChecker(String[] algorithms) {
        this.algorithms = algorithms;
    }

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

    // TODO should we reuse org.eclipse.leshan.util.SecurityUtil ?
    protected PrivateKey decodeRfc5958PrivateKey(byte[] encodedKey) {
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(encodedKey);
        for (String algorithm : algorithms) {
            try {
                KeyFactory kf = KeyFactory.getInstance(algorithm);
                return kf.generatePrivate(keySpec);
            } catch (NoSuchAlgorithmException e) {
                LOG.debug("Failed to instantiate key factory for algorithm " + algorithm, e);
                continue;
            } catch (InvalidKeySpecException e) {
                LOG.debug("Failed to decode RFC5958 private key with algorithm " + algorithm, e);
                continue;
            }
        }
        return null;
    }

    // TODO should we reuse org.eclipse.leshan.util.SecurityUtil ?
    protected PublicKey decodeRfc7250PublicKey(byte[] encodedKey) {
        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(encodedKey);
        for (String algorithm : algorithms) {
            try {
                KeyFactory kf = KeyFactory.getInstance(algorithm);
                return kf.generatePublic(keySpec);
            } catch (NoSuchAlgorithmException e) {
                LOG.debug("Failed to instantiate key factory for algorithm " + algorithm, e);
                continue;
            } catch (InvalidKeySpecException e) {
                LOG.debug("Failed to decode RFC7250 public key with algorithm " + algorithm, e);
                continue;
            }
        }
        return null;
    }

    // TODO should we reuse org.eclipse.leshan.util.SecurityUtil ?
    protected Certificate decodeCertificate(byte[] encodedCert) {
        try {
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            try (ByteArrayInputStream in = new ByteArrayInputStream(encodedCert)) {
                return cf.generateCertificate(in);
            }
        } catch (CertificateException | IOException e) {
            LOG.debug("Failed to decode X.509 certificate", e);
            return null;
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
