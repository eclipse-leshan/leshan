/*******************************************************************************
 * Copyright (c) 2017 Sierra Wireless and others.
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
import org.eclipse.californium.elements.DtlsEndpointContext;
import org.eclipse.californium.elements.EndpointContext;
import org.eclipse.californium.elements.MapBasedEndpointContext;
import org.eclipse.californium.elements.MapBasedEndpointContext.Attributes;
import org.eclipse.californium.elements.auth.PreSharedKeyIdentity;
import org.eclipse.californium.elements.auth.RawPublicKeyIdentity;
import org.eclipse.californium.elements.auth.X509CertPath;
import org.eclipse.californium.oscore.OSCoreEndpointContextInfo;
import org.eclipse.leshan.core.oscore.OscoreIdentity;
import org.eclipse.leshan.core.request.Identity;
import org.eclipse.leshan.core.util.Hex;

//TODO TL : to be delete when no more class use it (at the end of the refactoring)

/**
 * Utility class used to handle Californium {@link EndpointContext} in Leshan.
 * <p>
 * Able to translate Californium {@link EndpointContext} to Leshan {@link Identity} and vice-versa.
 */
public class EndpointContextUtil {

    /**
     * Create Leshan {@link Identity} from Californium {@link EndpointContext}.
     *
     * @param context The Californium {@link EndpointContext} to convert.
     * @return The corresponding Leshan {@link Identity}.
     * @throws IllegalStateException if we are not able to extract {@link Identity}.
     */
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
            throw new IllegalStateException(
                    String.format("Unable to extract sender identity : unexpected type of Principal %s [%s]",
                            senderIdentity.getClass(), senderIdentity.toString()));
        } else {
            // Build identity for OSCORE if it is used
            if (context.get(OSCoreEndpointContextInfo.OSCORE_RECIPIENT_ID) != null) {
                String recipient = context.get(OSCoreEndpointContextInfo.OSCORE_RECIPIENT_ID);
                return Identity.oscoreOnly(peerAddress, new OscoreIdentity(Hex.decodeHex(recipient.toCharArray())));
            }
        }
        return Identity.unsecure(peerAddress);
    }

    /**
     * Create Californium {@link EndpointContext} from Leshan {@link Identity}.
     * <p>
     * OSCORE does not use a Principal but automatically sets properties in the endpoint context at message
     * transmission/reception.
     *
     * @param identity The Leshan {@link Identity} to convert.
     * @param allowConnectionInitiation This request can initiate a Handshake if there is no DTLS connection.
     *
     * @return The corresponding Californium {@link EndpointContext}.
     */
    public static EndpointContext extractContext(Identity identity, boolean allowConnectionInitiation) {
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

        // TODO OSCORE : should we add properties to endpoint context ?

        if (peerIdentity != null && allowConnectionInitiation) {
            return new MapBasedEndpointContext(identity.getPeerAddress(), peerIdentity, new Attributes()
                    .add(DtlsEndpointContext.KEY_HANDSHAKE_MODE, DtlsEndpointContext.HANDSHAKE_MODE_AUTO));
        }
        return new AddressEndpointContext(identity.getPeerAddress(), peerIdentity);
    }

    /**
     * Extract "common name" from "distinguished name".
     *
     * @param dn The distinguished name.
     * @return The extracted common name.
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
