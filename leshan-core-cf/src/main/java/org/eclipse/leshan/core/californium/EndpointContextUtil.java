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
 *     Rikard Höglund (RISE SICS) - Additions to support OSCORE
 *******************************************************************************/
package org.eclipse.leshan.core.californium;

import java.net.InetSocketAddress;
import java.security.Principal;
import java.security.PublicKey;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.security.auth.x500.X500Principal;

import org.eclipse.californium.elements.AddressEndpointContext;
import org.eclipse.californium.elements.EndpointContext;
import org.eclipse.californium.elements.auth.PreSharedKeyIdentity;
import org.eclipse.californium.elements.auth.RawPublicKeyIdentity;
import org.eclipse.californium.elements.auth.X509CertPath;
import org.eclipse.californium.oscore.OSCoreEndpointContextInfo;
import org.eclipse.leshan.core.request.Identity;

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
            } else if (senderIdentity instanceof X500Principal || senderIdentity instanceof X509CertPath) {
                // Extract common name
                String x509CommonName = extractCN(senderIdentity.getName());
                return Identity.x509(peerAddress, x509CommonName);
            }
            throw new IllegalStateException("Unable to extract sender identity : unexpected type of Principal");
        } else {
            // Build identity for OSCORE if it is used
            if (context.get(OSCoreEndpointContextInfo.OSCORE_SENDER_ID) != null) {
                String oscoreIdentity = "sid=" + context.get(OSCoreEndpointContextInfo.OSCORE_SENDER_ID) + ",rid="
                        + context.get(OSCoreEndpointContextInfo.OSCORE_RECIPIENT_ID);
                return Identity.oscoreOnly(peerAddress, oscoreIdentity.toLowerCase());
            }
        }
        return Identity.unsecure(peerAddress);
    }

    /**
     * Create californium endpoint context from leshan identity. OSCORE does not use a Principal but automatically sets
     * properties in the endpoint context at message transmission/reception.
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
                /* simplify distinguished name to CN= part */
                peerIdentity = new X500Principal("CN=" + identity.getX509CommonName());
            }
        }
        return new AddressEndpointContext(identity.getPeerAddress(), peerIdentity);
    }

    /**
     * Extract "common name" from "distinguished name".
     * 
     * @param dn distinguished name
     * @return common name
     * @throws IllegalStateException if no CN is contained in DN.
     */
    public static String extractCN(String dn) {
        // Extract common name
        Matcher endpointMatcher = Pattern.compile("CN=(.*?)(,|$)").matcher(dn);
        if (endpointMatcher.find()) {
            return endpointMatcher.group(1);
        } else {
            throw new IllegalStateException(
                    "Unable to extract sender identity : can not get common name in certificate");
        }
    }
}