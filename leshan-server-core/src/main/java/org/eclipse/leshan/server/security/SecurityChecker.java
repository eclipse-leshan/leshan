/*******************************************************************************
 * Copyright (c) 2016 Sierra Wireless and others.
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
package org.eclipse.leshan.server.security;

import java.security.PublicKey;
import java.util.List;

import org.eclipse.leshan.core.request.Identity;
import org.eclipse.leshan.core.util.Hex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Ensure that client with given endpoint name and {@link Identity} authenticated itself in an expected way.
 */
public class SecurityChecker {

    private static final Logger LOG = LoggerFactory.getLogger(SecurityChecker.class);

    /**
     * Return true if client authenticated itself with any of the expected way.
     * 
     * @param endpoint the client endpoint name.
     * @param clientIdentity the client identity.
     * @param securityInfos the expected ways to authenticate.
     * 
     * @return true if client is correctly authenticated.
     * @see SecurityInfo
     */
    public boolean checkSecurityInfos(String endpoint, Identity clientIdentity, List<SecurityInfo> securityInfos) {
        // if this is a secure end-point, we must check that the registering client is using the right identity.
        if (clientIdentity.isSecure()) {
            if (securityInfos == null || securityInfos.isEmpty()) {
                LOG.debug("Client '{}' without security info try to connect through the secure endpoint", endpoint);
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
            LOG.debug("Client '{}' must connect using DTLS", endpoint);
            return false;
        }
        return true;
    }

    /**
     * Return true if client authenticated itself with the expected way.
     * 
     * @param endpoint the client endpoint name.
     * @param clientIdentity the client identity.
     * @param securityInfo the expected way to authenticate.
     * 
     * @return true if client is correctly authenticated.
     * @see SecurityInfo
     */
    public boolean checkSecurityInfo(String endpoint, Identity clientIdentity, SecurityInfo securityInfo) {

        // if this is a secure end-point, we must check that the registering client is using the right identity.
        if (clientIdentity.isSecure()) {
            if (securityInfo == null) {

                LOG.debug("Client '{}' without security info try to connect through the secure endpoint", endpoint);
                return false;

            } else if (clientIdentity.isPSK()) {

                return checkPskIdentity(endpoint, clientIdentity, securityInfo);

            } else if (clientIdentity.isRPK()) {

                return checkRpkIdentity(endpoint, clientIdentity, securityInfo);

            } else if (clientIdentity.isX509()) {

                return checkX509Identity(endpoint, clientIdentity, securityInfo);

            } else {
                LOG.debug("Unable to authenticate client '{}': unknown authentication mode", endpoint);
                return false;
            }
        } else {
            if (securityInfo != null) {
                LOG.debug("Client '{}' must connect using DTLS", endpoint);
                return false;
            }
        }
        return true;
    }

    protected boolean checkPskIdentity(String endpoint, Identity clientIdentity, SecurityInfo securityInfo) {
        // Manage PSK authentication
        // ----------------------------------------------------
        if (!securityInfo.usePSK()) {
            LOG.debug("Client '{}' is not supposed to use PSK to authenticate", endpoint);
            return false;
        }

        if (!matchPskIdentity(endpoint, clientIdentity.getPskIdentity(), securityInfo.getIdentity())) {
            return false;
        }

        LOG.trace("Authenticated client '{}' using DTLS PSK", endpoint);
        return true;
    }

    protected boolean matchPskIdentity(String endpoint, String receivedPskIdentity, String expectedPskIdentity) {
        if (!receivedPskIdentity.equals(expectedPskIdentity)) {
            LOG.debug("Invalid identity for client '{}': expected '{}' but was '{}'", endpoint, expectedPskIdentity,
                    receivedPskIdentity);
            return false;
        }
        return true;
    }

    protected boolean checkRpkIdentity(String endpoint, Identity clientIdentity, SecurityInfo securityInfo) {
        // Manage RPK authentication
        // ----------------------------------------------------
        if (!securityInfo.useRPK()) {
            LOG.debug("Client '{}' is not supposed to use RPK to authenticate", endpoint);
            return false;
        }

        if (!matchRpkIdenity(endpoint, clientIdentity.getRawPublicKey(), securityInfo.getRawPublicKey())) {
            return false;
        }

        LOG.trace("authenticated client '{}' using DTLS RPK", endpoint);
        return true;
    }

    protected boolean matchRpkIdenity(String endpoint, PublicKey receivedPublicKey, PublicKey expectedPublicKey) {
        if (!receivedPublicKey.equals(expectedPublicKey)) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Invalid rpk for client {}: expected \n'{}'\n but was \n'{}'", endpoint,
                        Hex.encodeHexString(expectedPublicKey.getEncoded()),
                        Hex.encodeHexString(receivedPublicKey.getEncoded()));
            }
            return false;
        }
        return true;
    }

    protected boolean checkX509Identity(String endpoint, Identity clientIdentity, SecurityInfo securityInfo) {
        // Manage X509 certificate authentication
        // ----------------------------------------------------
        if (!securityInfo.useX509Cert()) {
            LOG.debug("Client '{}' is not supposed to use X509 certificate to authenticate", endpoint);
            return false;
        }

        if (!matchX509Identity(endpoint, clientIdentity.getX509CommonName(), endpoint)) {
            return false;
        }

        LOG.trace("authenticated client '{}' using DTLS X509 certificates", endpoint);
        return true;
    }

    protected boolean matchX509Identity(String endpoint, String receivedX509CommonName, String expectedX509CommonName) {
        if (!receivedX509CommonName.equals(expectedX509CommonName)) {
            LOG.debug("Invalid certificate common name for client '{}': expected \n'{}'\n but was \n'{}'", endpoint,
                    expectedX509CommonName, receivedX509CommonName);
            return false;
        }
        return true;
    }
}
