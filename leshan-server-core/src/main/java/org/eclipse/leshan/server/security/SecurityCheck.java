/*******************************************************************************
 * Copyright (c) 2016 Sierra Wireless and others.
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
package org.eclipse.leshan.server.security;

import java.security.PublicKey;
import java.util.List;

import org.eclipse.leshan.core.request.Identity;
import org.eclipse.leshan.util.Hex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SecurityCheck {

    private static final Logger LOG = LoggerFactory.getLogger(SecurityCheck.class);

    /**
     * Return true if any of the securityInfos is valid for the given endpoint and client identity.
     * 
     * @see #checkSecurityInfo(String, Identity, SecurityInfo)
     * 
     * @param endpoint
     * @param clientIdentity
     * @param securityInfos
     * 
     */
    public static boolean checkSecurityInfos(String endpoint, Identity clientIdentity,
            List<SecurityInfo> securityInfos) {
        // if this is a secure end-point, we must check that the registering client is using the right identity.
        if (clientIdentity.isSecure()) {
            if (securityInfos == null || securityInfos.isEmpty()) {
                LOG.warn("Client '{}' without security info try to connect through the secure endpoint", endpoint);
                return false;
            } else {
                for (SecurityInfo securityInfo : securityInfos) {
                    if (checkSecurityInfo(endpoint, clientIdentity, securityInfo)) {
                        return true;
                    }
                }
                return false;
            }
        } else if (securityInfos != null && !securityInfos.isEmpty()) {
            LOG.warn("Client '{}' must connect using DTLS", endpoint);
            return false;
        }
        return true;
    }

    /**
     * Validates security info against a known endpoint and identity.
     * 
     * @param endpoint
     * @param clientIdentity
     * @param securityInfo
     * @return true if the security info are valid.
     */
    public static boolean checkSecurityInfo(String endpoint, Identity clientIdentity, SecurityInfo securityInfo) {

        // if this is a secure end-point, we must check that the registering client is using the right identity.
        if (clientIdentity.isSecure()) {
            if (securityInfo == null) {

                LOG.warn("Client '{}' without security info try to connect through the secure endpoint", endpoint);
                return false;

            } else if (clientIdentity.isPSK()) {

                return checkPskIdentity(endpoint, clientIdentity, securityInfo);

            } else if (clientIdentity.isRPK()) {

                return checkRpkIdentity(endpoint, clientIdentity, securityInfo);

            } else if (clientIdentity.isX509()) {

                return checkX509Identity(endpoint, clientIdentity, securityInfo);

            } else {
                LOG.warn("Unable to authenticate client '{}': unknown authentication mode", endpoint);
                return false;
            }
        } else {
            if (securityInfo != null) {
                LOG.warn("Client '{}' must connect using DTLS", endpoint);
                return false;
            }
        }
        return true;
    }

    private static boolean checkPskIdentity(String endpoint, Identity clientIdentity, SecurityInfo securityInfo) {
        // Manage PSK authentication
        // ----------------------------------------------------
        String pskIdentity = clientIdentity.getPskIdentity();
        LOG.debug("Registration request received using the secure endpoint with identity {}", pskIdentity);

        if (pskIdentity == null || !pskIdentity.equals(securityInfo.getIdentity())) {
            LOG.warn("Invalid identity for client '{}': expected '{}' but was '{}'", endpoint,
                    securityInfo.getIdentity(), pskIdentity);
            return false;
        } else {
            LOG.debug("Authenticated client '{}' using DTLS PSK", endpoint);
            return true;
        }
    }

    private static boolean checkRpkIdentity(String endpoint, Identity clientIdentity, SecurityInfo securityInfo) {
        // Manage RPK authentication
        // ----------------------------------------------------
        PublicKey publicKey = clientIdentity.getRawPublicKey();
        if (LOG.isDebugEnabled()) {
            LOG.debug("Registration request received using the secure endpoint with rpk '{}'",
                    Hex.encodeHexString(publicKey.getEncoded()));
        }

        if (publicKey == null || !publicKey.equals(securityInfo.getRawPublicKey())) {
            if (LOG.isWarnEnabled()) {
                LOG.warn("Invalid rpk for client {}: expected \n'{}'\n but was \n'{}'", endpoint,
                        Hex.encodeHexString(securityInfo.getRawPublicKey().getEncoded()),
                        Hex.encodeHexString(publicKey.getEncoded()));
            }
            return false;
        } else {
            LOG.debug("authenticated client '{}' using DTLS RPK", endpoint);
            return true;
        }
    }

    private static boolean checkX509Identity(String endpoint, Identity clientIdentity, SecurityInfo securityInfo) {
        // Manage X509 certificate authentication
        // ----------------------------------------------------
        String x509CommonName = clientIdentity.getX509CommonName();
        LOG.debug("Registration request received using the secure endpoint with X509 identity {}", x509CommonName);

        if (!securityInfo.useX509Cert()) {
            LOG.warn("Client '{}' is not supposed to use X509 certificate to authenticate", endpoint);
            return false;
        }

        if (!x509CommonName.equals(endpoint)) {
            LOG.warn("Invalid certificate common name for client '{}': expected \n'{}'\n but was \n'{}'", endpoint,
                    endpoint, x509CommonName);
            return false;
        } else {
            LOG.debug("authenticated client '{}' using DTLS X509 certificates", endpoint);
            return true;
        }
    }
}
