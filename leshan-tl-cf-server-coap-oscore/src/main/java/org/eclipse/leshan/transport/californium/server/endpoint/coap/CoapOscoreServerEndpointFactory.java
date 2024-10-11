/*******************************************************************************
 * Copyright (c) 2022 Sierra Wireless and others.
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
package org.eclipse.leshan.transport.californium.server.endpoint.coap;

import java.net.InetSocketAddress;
import java.security.Principal;

import org.eclipse.californium.core.coap.Message;
import org.eclipse.californium.core.network.CoapEndpoint;
import org.eclipse.californium.core.network.CoapEndpoint.Builder;
import org.eclipse.californium.elements.AddressEndpointContext;
import org.eclipse.californium.elements.EndpointContext;
import org.eclipse.californium.elements.config.Configuration;
import org.eclipse.californium.oscore.OSCoreCoapStackFactory;
import org.eclipse.californium.oscore.OSCoreEndpointContextInfo;
import org.eclipse.leshan.core.endpoint.EndPointUriHandler;
import org.eclipse.leshan.core.endpoint.EndpointUri;
import org.eclipse.leshan.core.peer.IpPeer;
import org.eclipse.leshan.core.peer.LwM2mPeer;
import org.eclipse.leshan.core.peer.OscoreIdentity;
import org.eclipse.leshan.core.util.Hex;
import org.eclipse.leshan.server.LeshanServer;
import org.eclipse.leshan.server.endpoint.EffectiveEndpointUriProvider;
import org.eclipse.leshan.server.observation.LwM2mNotificationReceiver;
import org.eclipse.leshan.servers.security.EditableSecurityStore;
import org.eclipse.leshan.transport.californium.identity.IdentityHandler;
import org.eclipse.leshan.transport.californium.oscore.cf.InMemoryOscoreContextDB;
import org.eclipse.leshan.transport.californium.server.LwM2mOscoreStore;
import org.eclipse.leshan.transport.californium.server.OscoreContextCleaner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CoapOscoreServerEndpointFactory extends CoapServerEndpointFactory {

    private static final Logger LOG = LoggerFactory.getLogger(CoapOscoreServerEndpointFactory.class);

    public CoapOscoreServerEndpointFactory(EndpointUri uri) {
        super(uri);
    }

    public CoapOscoreServerEndpointFactory(EndpointUri uri, EndPointUriHandler uriHandler) {
        super(uri, null, null, null, uriHandler);
    }

    @Override
    public String getEndpointDescription() {
        return super.getEndpointDescription() + " with very experimental support of OSCORE";
    }

    /**
     * This method is intended to be overridden.
     *
     * @param address the IP address and port, if null the connector is bound to an ephemeral port on the wildcard
     *        address.
     * @param coapConfig the CoAP config used to create this endpoint.
     * @return the {@link Builder} used for unsecured communication.
     */
    @Override
    protected CoapEndpoint.Builder createEndpointBuilder(InetSocketAddress address, Configuration coapConfig,
            LwM2mNotificationReceiver notificationReceiver, LeshanServer server,
            EffectiveEndpointUriProvider endpointUriProvider) {
        CoapEndpoint.Builder builder = super.createEndpointBuilder(address, coapConfig, notificationReceiver, server,
                endpointUriProvider);

        // handle oscore
        if (server.getSecurityStore() != null) {
            InMemoryOscoreContextDB oscoreCtxDB = new InMemoryOscoreContextDB(
                    new LwM2mOscoreStore(server.getSecurityStore(), server.getRegistrationStore()));
            builder.setCustomCoapStackArgument(oscoreCtxDB).setCoapStackFactory(new OSCoreCoapStackFactory());

            OscoreContextCleaner oscoreCtxCleaner = new OscoreContextCleaner(oscoreCtxDB);
            server.getRegistrationService().addListener(oscoreCtxCleaner);

            if (server.getSecurityStore() instanceof EditableSecurityStore) {
                ((EditableSecurityStore) server.getSecurityStore()).addListener(oscoreCtxCleaner);
            }
        }

        LOG.warn("Experimental OSCORE feature is enabled.");

        return builder;
    }

    @Override
    public IdentityHandler createIdentityHandler() {
        return new IdentityHandler() {

            @Override
            public LwM2mPeer getIdentity(Message receivedMessage) {
                EndpointContext context = receivedMessage.getSourceContext();
                InetSocketAddress peerAddress = context.getPeerAddress();
                Principal senderIdentity = context.getPeerIdentity();
                if (senderIdentity == null) {
                    // Build identity for OSCORE if it is used
                    if (context.get(OSCoreEndpointContextInfo.OSCORE_RECIPIENT_ID) != null) {
                        String recipient = context.get(OSCoreEndpointContextInfo.OSCORE_RECIPIENT_ID);
                        return new IpPeer(peerAddress, new OscoreIdentity(Hex.decodeHex(recipient.toCharArray())));
                    }
                    return new IpPeer(peerAddress);
                } else {
                    return null;
                }
            }

            @Override
            public EndpointContext createEndpointContext(LwM2mPeer client, boolean allowConnectionInitiation) {
                // TODO OSCORE : should we add properties to endpoint context ?
                if (client instanceof IpPeer) {
                    return new AddressEndpointContext(((IpPeer) client).getSocketAddress());
                } else {
                    throw new IllegalStateException(String.format("Unsupported Peer : %s", client));
                }
            }
        };
    }
}
