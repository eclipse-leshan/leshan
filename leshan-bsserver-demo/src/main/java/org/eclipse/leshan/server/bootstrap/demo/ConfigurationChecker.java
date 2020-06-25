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
 *     Rikard Höglund (RISE) - additions to support OSCORE
 *******************************************************************************/
package org.eclipse.leshan.server.bootstrap.demo;

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

import org.apache.commons.lang.ArrayUtils;
import org.eclipse.leshan.server.bootstrap.BootstrapConfig;
import org.eclipse.leshan.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Check a BootstrapConfig is correct. This is a complex process, we need to check if the different objects are in
 * coherence with each other.
 */
public class ConfigurationChecker {

    private static final Logger LOG = LoggerFactory.getLogger(ConfigurationChecker.class);

    private static final String[] KEY_ALGORITHMS = new String[] { "EC", "DiffieHellman", "RSA", "DSA" };

    public static void verify(BootstrapConfig config) throws ConfigurationException {
        // check security configurations
        for (Map.Entry<Integer, BootstrapConfig.ServerSecurity> e : config.security.entrySet()) {
            BootstrapConfig.ServerSecurity sec = e.getValue();

            // Check OSCORE object for the bootstrap server security object
            boolean usingOscore = false;
            if (sec.bootstrapServer) {

                BootstrapConfig.OscoreObject osc = null;
                for (Map.Entry<Integer, BootstrapConfig.OscoreObject> o : config.oscore.entrySet()) {
                    osc = o.getValue();
                    if (osc.objectInstanceId == sec.oscoreSecurityMode) {
                        usingOscore = true;
                        break;
                    }
                }
                if (usingOscore) {
                    LOG.trace("Bootstrapping information contains OSCORE security object.");
                    assertIf(usingOscore == false, "no oscore object found for bootstrap server security object");
                    assertIf(ArrayUtils.isEmpty(osc.oscoreMasterSecret), "master secret must not be empty");
                    assertIf(ArrayUtils.isEmpty(osc.oscoreSenderId) && ArrayUtils.isEmpty(osc.oscoreRecipientId),
                            "either sender ID or recipient ID must be filled");
                }

            }

            // checks mandatory fields
            if (StringUtils.isEmpty(sec.uri))
                throw new ConfigurationException("LwM2M Server URI is mandatory");
            if (sec.securityMode == null)
                throw new ConfigurationException("Security Mode is mandatory");

            // End loop here (since OSCORE is not a proper securityMode)
            if (usingOscore) {
                continue;
            }

            // checks security config
            switch (sec.securityMode) {
            case NO_SEC:
                assertIf(!ArrayUtils.isEmpty(sec.secretKey), "NO-SEC mode, secret key must be empty");
                assertIf(!ArrayUtils.isEmpty(sec.publicKeyOrId), "NO-SEC mode, public key or ID must be empty");
                assertIf(!ArrayUtils.isEmpty(sec.serverPublicKey), "NO-SEC mode, server public key must be empty");
                break;
            case PSK:
                assertIf(ArrayUtils.isEmpty(sec.secretKey), "pre-shared-key mode, secret key must not be empty");
                assertIf(ArrayUtils.isEmpty(sec.publicKeyOrId),
                        "pre-shared-key mode, public key or id must not be empty");
                break;
            case RPK:
                assertIf(ArrayUtils.isEmpty(sec.secretKey), "raw-public-key mode, secret key must not be empty");
                assertIf(decodeRfc5958PrivateKey(sec.secretKey) == null,
                        "raw-public-key mode, secret key must be RFC5958 encoded private key");
                assertIf(ArrayUtils.isEmpty(sec.publicKeyOrId),
                        "raw-public-key mode, public key or id must not be empty");
                assertIf(decodeRfc7250PublicKey(sec.publicKeyOrId) == null,
                        "raw-public-key mode, public key or id must be RFC7250 encoded public key");
                assertIf(ArrayUtils.isEmpty(sec.serverPublicKey),
                        "raw-public-key mode, server public key must not be empty");
                assertIf(decodeRfc7250PublicKey(sec.serverPublicKey) == null,
                        "raw-public-key mode, server public key must be RFC7250 encoded public key");
                break;
            case X509:
                assertIf(ArrayUtils.isEmpty(sec.secretKey), "x509 mode, secret key must not be empty");
                assertIf(decodeRfc5958PrivateKey(sec.secretKey) == null,
                        "x509 mode, secret key must be RFC5958 encoded private key");
                assertIf(ArrayUtils.isEmpty(sec.publicKeyOrId), "x509 mode, public key or id must not be empty");
                assertIf(decodeCertificate(sec.publicKeyOrId) == null,
                        "x509 mode, public key or id must be DER encoded X.509 certificate");
                assertIf(ArrayUtils.isEmpty(sec.serverPublicKey),
                        "x509 mode, server public key must not be empty");
                assertIf(decodeCertificate(sec.serverPublicKey) == null,
                        "x509 mode, server public key must be DER encoded X.509 certificate");
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
                        + " should not be a bootstrap server");
            }
        }
    }

    private static PrivateKey decodeRfc5958PrivateKey(byte[] encodedKey) {
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(encodedKey);
        for (String algorithm : KEY_ALGORITHMS) {
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

    private static PublicKey decodeRfc7250PublicKey(byte[] encodedKey) {
        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(encodedKey);
        for (String algorithm : KEY_ALGORITHMS) {
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

    private static Certificate decodeCertificate(byte[] encodedCert) {
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
