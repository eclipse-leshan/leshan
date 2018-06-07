/*******************************************************************************
 * Copyright (c) 2017 Sierra Wireless and others.
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
 *     Achim Kraus (Bosch Software Innovations GmbH) - add support for californium
 *                                                     endpoint context
 *******************************************************************************/
package org.eclipse.leshan.core.californium;

import java.net.InetSocketAddress;
import java.security.Principal;
import java.security.PublicKey;

import javax.security.auth.x500.X500Principal;

import org.eclipse.californium.elements.AddressEndpointContext;
import org.eclipse.californium.elements.EndpointContext;
import org.eclipse.californium.elements.auth.PreSharedKeyIdentity;
import org.eclipse.californium.elements.auth.RawPublicKeyIdentity;
import org.eclipse.californium.elements.auth.X509CertPath;
import org.eclipse.leshan.core.request.Identity;
import org.eclipse.leshan.util.X509Util;

public class EndpointContextUtil {

    public static Identity extractIdentity(EndpointContext context) {
        InetSocketAddress peerAddress = context.getPeerAddress();
        Principal senderIdentity = context.getPeerIdentity();
        if (senderIdentity != null) {
            if (senderIdentity instanceof PreSharedKeyIdentity) {
                return Identity.psk(peerAddress, ((PreSharedKeyIdentity) senderIdentity).getIdentity());
            } else if (senderIdentity instanceof RawPublicKeyIdentity) {
                PublicKey publicKey = ((RawPublicKeyIdentity) senderIdentity).getKey();
                return Identity.rpk(peerAddress, publicKey);
            } else if (senderIdentity instanceof X500Principal) {
                String x509CommonName = X509Util.extractCN(senderIdentity.getName());
                return Identity.x509(peerAddress, x509CommonName);
            } else if (senderIdentity instanceof X509CertPath) {
                return Identity.x509(peerAddress, ((X509CertPath) senderIdentity).getPath());
            }
            throw new IllegalStateException("Unable to extract sender identity : unexpected type of Principal");
        }
        return Identity.unsecure(peerAddress);
    }

    /**
     * Create californium endpoint context from leshan identity.
     * 
     * @param identity leshan identity received on last registration.
     * @return californium endpoint context for leshan identity
     */
    public static EndpointContext extractContext(Identity identity) {
        Principal peerIdentity = null;
        if (identity != null) {
            if (identity.isPSK()) {
                peerIdentity = new PreSharedKeyIdentity(identity.getPskIdentity());
            } else if (identity.isRPK()) {
                peerIdentity = new RawPublicKeyIdentity(identity.getRawPublicKey());
            } else if (identity.isX509()) {
                if (identity.getCertificates() != null) {
                    peerIdentity = new X509CertPath(identity.getCertificates());
                } else {
                    /* simplify distinguished name to CN= part */
                    peerIdentity = new X500Principal(X509Util.createDN(identity.getX509CommonName()));
                }
            }
        }
        return new AddressEndpointContext(identity.getPeerAddress(), peerIdentity);
    }

}