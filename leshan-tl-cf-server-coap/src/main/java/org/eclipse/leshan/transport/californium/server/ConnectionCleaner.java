/*******************************************************************************
 * Copyright (c) 2020 Sierra Wireless and others.
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
package org.eclipse.leshan.transport.californium.server;

import java.security.Principal;

import org.eclipse.californium.elements.auth.PreSharedKeyIdentity;
import org.eclipse.californium.elements.auth.RawPublicKeyIdentity;
import org.eclipse.californium.scandium.DTLSConnector;
import org.eclipse.leshan.core.security.certificate.util.X509CertUtil;
import org.eclipse.leshan.servers.security.SecurityInfo;
import org.eclipse.leshan.transport.californium.identity.CaliforniumPrincipalUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is responsible to remove DTLS connection for a given SecurityInfo.
 */
public class ConnectionCleaner {

    private static final Logger LOG = LoggerFactory.getLogger(ConnectionCleaner.class);

    private final DTLSConnector connector;

    public ConnectionCleaner(DTLSConnector connector) {
        this.connector = connector;
    }

    public void cleanConnectionFor(final SecurityInfo... infos) {
        connector.startTerminateConnectionsForPrincipal(principal -> isConnectionOfAny(principal, infos));
    }

    /**
     * @return true if the connection of this principal is one of the connections to remove for the given
     *         {@link SecurityInfo}.
     */
    static boolean isConnectionOfAny(Principal principal, SecurityInfo... infos) {
        if (principal == null) {
            return false;
        }
        for (SecurityInfo info : infos) {
            if (info != null && matches(principal, info)) {
                return true;
            }
        }
        return false;
    }

    private static boolean matches(Principal principal, SecurityInfo info) {
        // PSK
        if (info.usePSK() && principal instanceof PreSharedKeyIdentity) {
            return info.getPskIdentity().equals(((PreSharedKeyIdentity) principal).getIdentity());
        }
        // RPK
        if (info.useRPK() && principal instanceof RawPublicKeyIdentity) {
            return info.getRawPublicKey().equals(((RawPublicKeyIdentity) principal).getKey());
        }
        // x509
        return info.useX509Cert() && matchesX509(principal, info);
    }

    private static boolean matchesX509(Principal principal, SecurityInfo info) {
        if (!CaliforniumPrincipalUtil.canBeConvertedToX500Principal(principal)) {
            return false;
        }
        try {
            // Extract common name
            String x509CommonName = X509CertUtil.extractCN(CaliforniumPrincipalUtil.convertToX500Principal(principal));
            return x509CommonName.equals(info.getEndpoint());
        } catch (IllegalStateException e) {
            // The identity of this connection can not be read (no CN, several CN, ...) so this is not the connection we
            // are looking for. The exception must not escape from the predicate: it would stop Scandium from visiting
            // the remaining connections, which would then not be cleaned.
            LOG.debug("Ignore connection with unreadable X.509 identity while cleaning connections for '{}': {}",
                    info.getEndpoint(), e.getMessage());
            return false;
        }
    }
}
