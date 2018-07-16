/*******************************************************************************
 * Copyright (c) 2018 Sierra Wireless and others.
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
package org.eclipse.leshan.client.californium.impl;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.californium.elements.auth.RawPublicKeyIdentity;
import org.eclipse.californium.scandium.dtls.cipher.CipherSuite;
import org.eclipse.californium.scandium.dtls.credentialsstore.CredentialsConfiguration;
import org.eclipse.californium.scandium.dtls.pskstore.PskStore;
import org.eclipse.californium.scandium.dtls.pskstore.StaticPskStore;
import org.eclipse.californium.scandium.dtls.rpkstore.TrustAllRpks;
import org.eclipse.californium.scandium.dtls.rpkstore.TrustedRpkStore;
import org.eclipse.californium.scandium.dtls.x509.CertificateVerifier;
import org.eclipse.leshan.SecurityMode;
import org.eclipse.leshan.client.object.SecurityObjectUtil;
import org.eclipse.leshan.core.node.LwM2mObjectInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SecurityInstanceCredentialsConfig implements CredentialsConfiguration {

    private static final Logger LOG = LoggerFactory.getLogger(SecurityInstanceCredentialsConfig.class);

    private LwM2mObjectInstance securityInstance;

    public SecurityInstanceCredentialsConfig(LwM2mObjectInstance securityInstance) {
        this.securityInstance = securityInstance;
    }

    @Override
    public CipherSuite[] getSupportedCipherSuites() {
        List<CipherSuite> ciphers = new ArrayList<>();
        switch (SecurityObjectUtil.getSecurityMode(securityInstance)) {
        case PSK:
            ciphers.add(CipherSuite.TLS_PSK_WITH_AES_128_CCM_8);
            ciphers.add(CipherSuite.TLS_PSK_WITH_AES_128_CBC_SHA256);
            break;
        case RPK:
        case X509:
            // TODO implement RPK/x509 support
            ciphers.add(CipherSuite.TLS_ECDHE_ECDSA_WITH_AES_128_CCM_8);
            ciphers.add(CipherSuite.TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA256);
            LOG.warn("RPK/x509 support not yet implemented.");
            break;
        case NO_SEC:
        default:
            LOG.warn("A credentialsConfig was created for a NO_SEC secutiry instance.");
            break;
        }
        return ciphers.toArray(new CipherSuite[ciphers.size()]);
    }

    @Override
    public PskStore getPskStore() {
        if (SecurityObjectUtil.getSecurityMode(securityInstance) == SecurityMode.PSK) {
            // Get Identity
            String identity = SecurityObjectUtil.getPskIdentity(securityInstance);
            if (identity == null || identity.isEmpty()) {
                LOG.warn("Empty PSK identity for PSK security instance");
                return null;
            }

            // Get Key
            byte[] key = SecurityObjectUtil.getPskKey(securityInstance);
            if (key == null || key.length == 0) {
                LOG.warn("Empty PSK key for PSK security instance");
                return null;
            }

            return new StaticPskStore(identity, key);
        } else {
            LOG.warn("Try to access to PSKStore for a none PSK security instance.");
        }
        return null;
    }

    @Override
    public PrivateKey getPrivateKey() {
        if (SecurityObjectUtil.getSecurityMode(securityInstance) == SecurityMode.RPK) {
            return SecurityObjectUtil.getPrivateKey(securityInstance);
        }
        LOG.warn("RPK support not yet implemented.");
        return null;
    }

    @Override
    public PublicKey getPublicKey() {
        if (SecurityObjectUtil.getSecurityMode(securityInstance) == SecurityMode.RPK) {
            return SecurityObjectUtil.getPublicKey(securityInstance);
        }
        LOG.warn("RPK support not yet implemented.");
        return null;
    }

    @Override
    public TrustedRpkStore getRpkTrustStore() {
        if (SecurityObjectUtil.getSecurityMode(securityInstance) == SecurityMode.RPK) {
            return new TrustedRpkStore() {
                @Override
                public boolean isTrusted(RawPublicKeyIdentity id) {
                    PublicKey receivedKey = id.getKey();
                    if (receivedKey == null) {
                        LOG.warn("The server public key is null {}", id);
                        return false;
                    }
                    PublicKey expectedKey = SecurityObjectUtil.getServerPublicKey(securityInstance);
                    if (!receivedKey.equals(expectedKey)) {
                        LOG.debug(
                                "Server public key received does match with the expected one.\nReceived: {}\nExpected: {}",
                                receivedKey, expectedKey);
                        return false;
                    }
                    return true;
                }
            };
        }
        return new TrustAllRpks();
    }

    @Override
    public X509Certificate[] getCertificateChain() {
        // TODO implement X509 support
        LOG.warn("x509 support not yet implemented.");
        return null;
    }

    @Override
    public CertificateVerifier getCertificateVerifier() {
        // TODO implement X509 support
        LOG.warn("x509 support not yet implemented.");
        return null;
    }

    @Override
    public Boolean isSendRawKey() {
        if (SecurityObjectUtil.getSecurityMode(securityInstance) == SecurityMode.RPK) {
            return true;
        }
        return false;
    }
}
