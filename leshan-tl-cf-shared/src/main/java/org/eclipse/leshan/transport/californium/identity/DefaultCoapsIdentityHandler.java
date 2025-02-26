/*******************************************************************************
 * Copyright (c) 2025 Sierra Wireless and others.
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
package org.eclipse.leshan.transport.californium.identity;

import java.net.InetSocketAddress;
import java.security.Principal;
import java.security.PublicKey;

import javax.security.auth.x500.X500Principal;

import org.eclipse.californium.core.coap.Message;
import org.eclipse.californium.elements.AddressEndpointContext;
import org.eclipse.californium.elements.DtlsEndpointContext;
import org.eclipse.californium.elements.EndpointContext;
import org.eclipse.californium.elements.MapBasedEndpointContext;
import org.eclipse.californium.elements.MapBasedEndpointContext.Attributes;
import org.eclipse.californium.elements.auth.PreSharedKeyIdentity;
import org.eclipse.californium.elements.auth.RawPublicKeyIdentity;
import org.eclipse.californium.elements.auth.X509CertPath;
import org.eclipse.leshan.core.peer.IpPeer;
import org.eclipse.leshan.core.peer.LwM2mPeer;
import org.eclipse.leshan.core.peer.PskIdentity;
import org.eclipse.leshan.core.peer.RpkIdentity;
import org.eclipse.leshan.core.peer.X509Identity;
import org.eclipse.leshan.core.security.certificate.util.X509CertUtil;

public class DefaultCoapsIdentityHandler implements IdentityHandler {

    private final boolean supportVirtualHost;

    public DefaultCoapsIdentityHandler() {
        this(false);
    }

    public DefaultCoapsIdentityHandler(boolean supportVirtualHost) {
        this.supportVirtualHost = supportVirtualHost;
    }

    @Override
    public LwM2mPeer getIdentity(Message receivedMessage) {
        EndpointContext context = receivedMessage.getSourceContext();
        InetSocketAddress peerAddress = context.getPeerAddress();
        Principal senderIdentity = context.getPeerIdentity();
        if (senderIdentity != null) {
            if (senderIdentity instanceof PreSharedKeyIdentity) {
                return new IpPeer(peerAddress, new PskIdentity(((PreSharedKeyIdentity) senderIdentity).getIdentity()));
            } else if (senderIdentity instanceof RawPublicKeyIdentity) {
                PublicKey publicKey = ((RawPublicKeyIdentity) senderIdentity).getKey();
                return new IpPeer(peerAddress, new RpkIdentity(publicKey));
            } else if (senderIdentity instanceof X500Principal || senderIdentity instanceof X509CertPath) {
                // Extract common name
                String x509CommonName = X509CertUtil.extractCN(senderIdentity.getName());
                return new IpPeer(peerAddress, new X509Identity(x509CommonName));
            }
            throw new IllegalStateException(
                    String.format("Unable to extract sender identity : unexpected type of Principal %s [%s]",
                            senderIdentity.getClass(), senderIdentity.toString()));
        }
        return null;
    }

    @Override
    public EndpointContext createEndpointContext(LwM2mPeer client, boolean allowConnectionInitiation) {
        Principal peerIdentity = null;
        if (client.getIdentity() instanceof PskIdentity) {
            peerIdentity = new PreSharedKeyIdentity(((PskIdentity) client.getIdentity()).getPskIdentity());
        } else if (client.getIdentity() instanceof RpkIdentity) {
            peerIdentity = new RawPublicKeyIdentity(((RpkIdentity) client.getIdentity()).getPublicKey());
        } else if (client.getIdentity() instanceof X509Identity) {
            /* simplify distinguished name to CN= part */
            peerIdentity = new X500Principal("CN=" + ((X509Identity) client.getIdentity()).getX509CommonName());
        } else {
            throw new IllegalStateException(String.format("Unsupported Identity : %s", client.getIdentity()));
        }
        if (client instanceof IpPeer) {
            IpPeer ipClient = (IpPeer) client;
            String virtualHost = supportVirtualHost ? ipClient.getVirtualHost() : null;

            if (allowConnectionInitiation) {
                return new MapBasedEndpointContext(ipClient.getSocketAddress(), virtualHost, peerIdentity,
                        new Attributes().add(DtlsEndpointContext.KEY_HANDSHAKE_MODE,
                                DtlsEndpointContext.HANDSHAKE_MODE_AUTO));
            }
            return new AddressEndpointContext(ipClient.getSocketAddress(), virtualHost, peerIdentity);

        } else {
            throw new IllegalStateException(String.format("Unsupported peer : %s", client));
        }
    }
}
