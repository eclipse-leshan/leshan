/*******************************************************************************
 * Copyright (c) 2024 Sierra Wireless and others.
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
package org.eclipse.leshan.transport.javacoap.identity;

import java.net.InetSocketAddress;
import java.security.Principal;

import javax.security.auth.x500.X500Principal;

import org.eclipse.leshan.core.peer.IpPeer;
import org.eclipse.leshan.core.peer.LwM2mPeer;
import org.eclipse.leshan.core.peer.X509Identity;
import org.eclipse.leshan.core.security.certificate.util.X509CertUtil;

import com.mbed.coap.transport.TransportContext;

public class DefaultTlsIdentityHandler extends DefaultCoapIdentityHandler {

    @Override
    protected LwM2mPeer getIdentity(InetSocketAddress address, TransportContext context) {
        Principal principal = context.get(TlsTransportContextKeys.PRINCIPAL);
        if (principal != null) {
            if (principal instanceof X500Principal) {
                // Extract common name
                String x509CommonName = X509CertUtil.extractCN(principal.getName());
                return new IpPeer(address, new X509Identity(x509CommonName));
            }
            throw new IllegalStateException(
                    String.format("Unable to extract sender identity : unexpected type of Principal %s [%s]",
                            principal.getClass(), principal.toString()));
        } else {
            return new IpPeer(address);
        }
    }

    @Override
    public TransportContext createTransportContext(LwM2mPeer client, boolean allowConnectionInitiation) {
        Principal peerIdentity = null;
        if (client.getIdentity() instanceof X509Identity) {
            /* simplify distinguished name to CN= part */
            peerIdentity = new X500Principal("CN=" + ((X509Identity) client.getIdentity()).getX509CommonName());
            return TransportContext.of(TlsTransportContextKeys.PRINCIPAL, peerIdentity);
        } else {
            throw new IllegalStateException(String.format("Unsupported Identity : %s", client.getIdentity()));
        }
    }

}
