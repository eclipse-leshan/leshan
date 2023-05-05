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
 *     Rikard Höglund (RISE SICS) - Additions to support OSCORE
 *******************************************************************************/
package org.eclipse.leshan.server.security;

import java.security.PublicKey;
import java.util.Iterator;

import org.eclipse.leshan.core.oscore.OscoreIdentity;
import org.eclipse.leshan.core.peer.LwM2mIdentity;
import org.eclipse.leshan.core.peer.LwM2mPeer;
import org.eclipse.leshan.core.peer.PskIdentity;
import org.eclipse.leshan.core.peer.RpkIdentity;
import org.eclipse.leshan.core.peer.X509Identity;
import org.eclipse.leshan.core.util.Hex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Ensure that client with given endpoint name and {@link LwM2mPeer} authenticated itself in an expected way.
 */
public class SecurityChecker {

    private static final Logger LOG = LoggerFactory.getLogger(SecurityChecker.class);

    /**
     * Return true if client authenticated itself with any of the expected way.
     *
     * @param endpoint the client endpoint name.
     * @param client the transport information about client.
     * @param securityInfos the expected ways to authenticate.
     *
     * @return true if client is correctly authenticated.
     * @see SecurityInfo
     */
    public boolean checkSecurityInfos(String endpoint, LwM2mPeer client, Iterator<SecurityInfo> securityInfos) {
        // if this is a secure end-point, we must check that the registering client is using the right identity.
        LwM2mIdentity clientIdentity = client.getIdentity();
        if (clientIdentity.isSecure()) {
            if (securityInfos == null || !securityInfos.hasNext()) {
                LOG.debug("Client '{}' without security info try to connect through the secure endpoint", endpoint);
                return false;
            } else {
                // check of one expected security info matches client identity
                do {
                    SecurityInfo securityInfo = securityInfos.next();
                    if (checkSecurityInfo(endpoint, client, securityInfo)) {
                        return true;
                    }
                } while (securityInfos.hasNext());
                return false;
            }
        } else if (clientIdentity instanceof org.eclipse.leshan.core.peer.OscoreIdentity) {
            if (securityInfos == null || !securityInfos.hasNext()) {
                LOG.debug("Client '{}' without security info trying to connect using OSCORE", endpoint);
                return false;
            } else {
                // check if one expected security info matches OSCORE client identity
                do {
                    SecurityInfo securityInfo = securityInfos.next();
                    if (checkSecurityInfo(endpoint, client, securityInfo)) {
                        return true;
                    }
                } while (securityInfos.hasNext());
            }
        } else if (securityInfos != null && securityInfos.hasNext()) {
            LOG.debug("Client '{}' must connect using DTLS or/and OSCORE", endpoint);
            return false;
        }
        return true;
    }

    /**
     * Return true if client authenticated itself with the expected way.
     *
     * @param endpoint the client endpoint name.
     * @param client the transport information about client.
     * @param securityInfo the expected way to authenticate.
     *
     * @return true if client is correctly authenticated.
     * @see SecurityInfo
     */
    public boolean checkSecurityInfo(String endpoint, LwM2mPeer client, SecurityInfo securityInfo) {
        // if this is a secure end-point, we must check that the registering client is using the right identity.
        LwM2mIdentity clientIdentity = client.getIdentity();
        if (clientIdentity.isSecure()) {
            if (securityInfo == null) {

                LOG.debug("Client '{}' without security info try to connect through the secure endpoint", endpoint);
                return false;

            } else if (clientIdentity instanceof PskIdentity) {

                return checkPskIdentity(endpoint, (PskIdentity) clientIdentity, securityInfo);

            } else if (clientIdentity instanceof RpkIdentity) {

                return checkRpkIdentity(endpoint, (RpkIdentity) clientIdentity, securityInfo);

            } else if (clientIdentity instanceof X509Identity) {

                return checkX509Identity(endpoint, (X509Identity) clientIdentity, securityInfo);

            } else {
                LOG.debug("Unable to authenticate client '{}': unknown authentication mode", endpoint);
                return false;
            }
        } else if (clientIdentity instanceof org.eclipse.leshan.core.peer.OscoreIdentity) {
            if (securityInfo == null) {
                LOG.debug("Client '{}' without security info trying to connect using OSCORE", endpoint);
                return false;
            } else {
                return checkOscoreIdentity(endpoint, (org.eclipse.leshan.core.peer.OscoreIdentity) clientIdentity,
                        securityInfo);
            }
        } else {
            if (securityInfo != null) {
                LOG.debug("Client '{}' must connect using DTLS or/and OSCORE", endpoint);
                return false;
            }
        }
        return true;
    }

    protected boolean checkPskIdentity(String endpoint, PskIdentity clientIdentity, SecurityInfo securityInfo) {
        // Manage PSK authentication
        // ----------------------------------------------------
        if (!securityInfo.usePSK()) {
            LOG.debug("Client '{}' is not supposed to use PSK to authenticate", endpoint);
            return false;
        }

        if (!matchPskIdentity(endpoint, clientIdentity.getPskIdentity(), securityInfo.getPskIdentity())) {
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

    protected boolean checkRpkIdentity(String endpoint, RpkIdentity clientIdentity, SecurityInfo securityInfo) {
        // Manage RPK authentication
        // ----------------------------------------------------
        if (!securityInfo.useRPK()) {
            LOG.debug("Client '{}' is not supposed to use RPK to authenticate", endpoint);
            return false;
        }

        if (!matchRpkIdenity(endpoint, clientIdentity.getPublicKey(), securityInfo.getRawPublicKey())) {
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

    protected boolean checkX509Identity(String endpoint, X509Identity clientIdentity, SecurityInfo securityInfo) {
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

    protected boolean checkOscoreIdentity(String endpoint, org.eclipse.leshan.core.peer.OscoreIdentity clientIdentity,
            SecurityInfo securityInfo) {
        // Manage OSCORE authentication
        // ----------------------------------------------------
        if (!securityInfo.useOSCORE()) {
            LOG.debug("Client '{}' is not supposed to use OSCORE to authenticate", endpoint);
            return false;
        }

        if (!matchOscoreIdentity(endpoint, new OscoreIdentity(clientIdentity.getRecipientId()),
                securityInfo.getOscoreSetting().getOscoreIdentity())) {
            return false;
        }

        LOG.trace("Authenticated client '{}' using OSCORE", endpoint);
        return true;
    }

    protected boolean matchOscoreIdentity(String endpoint, OscoreIdentity receivedOscoreIdentity,
            OscoreIdentity expectedOscoreIdentity) {
        if (!receivedOscoreIdentity.equals(expectedOscoreIdentity)) {
            LOG.debug("Invalid identity for client '{}': expected '{}' but was '{}'", endpoint, expectedOscoreIdentity,
                    receivedOscoreIdentity);
            return false;
        }
        return true;
    }
}
