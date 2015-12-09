/*******************************************************************************
 * Copyright (c) 2015 Sierra Wireless and others.
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

import java.net.InetSocketAddress;
import java.security.Principal;
import java.security.PublicKey;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.security.auth.x500.X500Principal;

import org.eclipse.californium.core.coap.CoAP.ResponseCode;
import org.eclipse.californium.core.server.resources.CoapExchange;
import org.eclipse.californium.scandium.auth.PreSharedKeyIdentity;
import org.eclipse.californium.scandium.auth.RawPublicKeyIdentity;
import org.eclipse.leshan.core.request.Identity;
import org.eclipse.leshan.util.Validate;

public class ResourceUtil {

    // TODO leshan-core-cf: this code should be factorized in a leshan-core-cf project.
    // duplicated from org.eclipse.leshan.server.californium.impl.RegisterResource
    public static Identity extractIdentity(CoapExchange exchange) {
        InetSocketAddress peerAddress = new InetSocketAddress(exchange.getSourceAddress(), exchange.getSourcePort());

        Principal senderIdentity = exchange.advanced().getRequest().getSenderIdentity();
        if (senderIdentity != null) {
            if (senderIdentity instanceof PreSharedKeyIdentity) {
                return Identity.psk(peerAddress, senderIdentity.getName());
            } else if (senderIdentity instanceof RawPublicKeyIdentity) {
                PublicKey publicKey = ((RawPublicKeyIdentity) senderIdentity).getKey();
                return Identity.rpk(peerAddress, publicKey);
            } else if (senderIdentity instanceof X500Principal) {
                // Extract common name
                Matcher endpointMatcher = Pattern.compile("CN=.*?,").matcher(senderIdentity.getName());
                if (endpointMatcher.find()) {
                    String x509CommonName = endpointMatcher.group().substring(3, endpointMatcher.group().length() - 1);
                    return Identity.x509(peerAddress, x509CommonName);
                } else {
                    return null;
                }
            }
        }
        return Identity.unsecure(peerAddress);
    }

    // TODO leshan-core-cf: this code should be factorize in a leshan-core-cf project.
    // duplicated from org.eclipse.leshan.server.californium.impl.RegisterResource
    public static ResponseCode fromLwM2mCode(final org.eclipse.leshan.ResponseCode code) {
        Validate.notNull(code);

        switch (code) {
        case CREATED:
            return ResponseCode.CREATED;
        case DELETED:
            return ResponseCode.DELETED;
        case CHANGED:
            return ResponseCode.CHANGED;
        case CONTENT:
            return ResponseCode.CONTENT;
        case BAD_REQUEST:
            return ResponseCode.BAD_REQUEST;
        case UNAUTHORIZED:
            return ResponseCode.UNAUTHORIZED;
        case NOT_FOUND:
            return ResponseCode.NOT_FOUND;
        case METHOD_NOT_ALLOWED:
            return ResponseCode.METHOD_NOT_ALLOWED;
        case FORBIDDEN:
            return ResponseCode.FORBIDDEN;
        case INTERNAL_SERVER_ERROR:
            return ResponseCode.INTERNAL_SERVER_ERROR;
        default:
            throw new IllegalArgumentException("Invalid CoAP code for LWM2M response: " + code);
        }
    }

}
