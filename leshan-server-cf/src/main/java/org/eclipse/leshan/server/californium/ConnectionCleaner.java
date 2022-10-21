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
package org.eclipse.leshan.server.californium;

import java.security.Principal;
import java.security.PublicKey;

import javax.security.auth.x500.X500Principal;

import org.eclipse.californium.elements.auth.PreSharedKeyIdentity;
import org.eclipse.californium.elements.auth.RawPublicKeyIdentity;
import org.eclipse.californium.elements.auth.X509CertPath;
import org.eclipse.californium.elements.util.LeastRecentlyUsedCache.Predicate;
import org.eclipse.californium.scandium.DTLSConnector;
import org.eclipse.leshan.core.util.X509CertUtil;
import org.eclipse.leshan.server.security.SecurityInfo;

/**
 * This class is responsible to remove DTLS connection for a given SecurityInfo.
 */
public class ConnectionCleaner {

    private final DTLSConnector connector;

    public ConnectionCleaner(DTLSConnector connector) {
        this.connector = connector;
    }

    public void cleanConnectionFor(final SecurityInfo... infos) {
        connector.startTerminateConnectionsForPrincipal(new Predicate<Principal>() {
            @Override
            public boolean accept(Principal principal) {
                if (principal != null) {
                    for (SecurityInfo info : infos) {
                        if (info != null) {
                            // PSK
                            if (info.usePSK() && principal instanceof PreSharedKeyIdentity) {
                                String identity = ((PreSharedKeyIdentity) principal).getIdentity();
                                if (info.getPskIdentity().equals(identity)) {
                                    return true;
                                }
                            }
                            // RPK
                            else if (info.useRPK() && principal instanceof RawPublicKeyIdentity) {
                                PublicKey publicKey = ((RawPublicKeyIdentity) principal).getKey();
                                if (info.getRawPublicKey().equals(publicKey)) {
                                    return true;
                                }
                            }
                            // x509
                            else if (info.useX509Cert() && principal instanceof X500Principal
                                    || principal instanceof X509CertPath) {
                                // Extract common name
                                String x509CommonName = X509CertUtil.extractCN(principal.getName());
                                if (x509CommonName.equals(info.getEndpoint())) {
                                    return true;
                                }
                            }
                        }
                    }
                }
                return false;
            }
        });
    }
}
