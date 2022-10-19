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
package org.eclipse.leshan.server.californium.bootstrap.endpoint.coap;

import java.net.InetSocketAddress;
import java.net.URI;
import java.security.Principal;

import org.eclipse.californium.core.coap.Message;
import org.eclipse.californium.core.network.CoapEndpoint;
import org.eclipse.californium.core.network.CoapEndpoint.Builder;
import org.eclipse.californium.elements.AddressEndpointContext;
import org.eclipse.californium.elements.EndpointContext;
import org.eclipse.californium.elements.config.Configuration;
import org.eclipse.californium.oscore.OSCoreCoapStackFactory;
import org.eclipse.californium.oscore.OSCoreCtxDB;
import org.eclipse.californium.oscore.OSCoreEndpointContextInfo;
import org.eclipse.leshan.core.californium.identity.IdentityHandler;
import org.eclipse.leshan.core.californium.oscore.cf.InMemoryOscoreContextDB;
import org.eclipse.leshan.core.oscore.OscoreIdentity;
import org.eclipse.leshan.core.request.Identity;
import org.eclipse.leshan.core.util.Hex;
import org.eclipse.leshan.server.bootstrap.LeshanBootstrapServer;
import org.eclipse.leshan.server.californium.bootstrap.BootstrapOscoreContextCleaner;
import org.eclipse.leshan.server.californium.bootstrap.LwM2mBootstrapOscoreStore;
import org.eclipse.leshan.server.californium.bootstrap.OscoreBootstrapListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CoapOscoreBootstrapServerEndpointFactory extends CoapBootstrapServerEndpointFactory {

    private static final Logger LOG = LoggerFactory.getLogger(CoapOscoreBootstrapServerEndpointFactory.class);

    public CoapOscoreBootstrapServerEndpointFactory(URI uri) {
        super(uri);
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
    protected CoapEndpoint.Builder createUnsecuredEndpointBuilder(InetSocketAddress address, Configuration coapConfig,
            LeshanBootstrapServer server) {
        CoapEndpoint.Builder builder = super.createUnsecuredEndpointBuilder(address, coapConfig, server);

        // handle oscore
        if (server.getSecurityStore() != null) {
            // Handle OSCORE support.
            OSCoreCtxDB oscoreCtxDB = null;
            OscoreBootstrapListener sessionHolder = null;
            BootstrapOscoreContextCleaner oscoreContextCleaner = null;
            if (server.getSecurityStore() != null) {
                sessionHolder = new OscoreBootstrapListener();
                server.addListener(sessionHolder);

                oscoreCtxDB = new InMemoryOscoreContextDB(
                        new LwM2mBootstrapOscoreStore(server.getSecurityStore(), sessionHolder));
                oscoreContextCleaner = new BootstrapOscoreContextCleaner(oscoreCtxDB);
                server.addListener(oscoreContextCleaner);

                builder.setCustomCoapStackArgument(oscoreCtxDB).setCoapStackFactory(new OSCoreCoapStackFactory());
                LOG.warn("Experimental OSCORE feature is enabled.");
            }
        }
        return builder;
    }

    @Override
    public IdentityHandler createIdentityHandler() {
        return new IdentityHandler() {

            @Override
            public Identity getIdentity(Message receivedMessage) {
                EndpointContext context = receivedMessage.getSourceContext();
                InetSocketAddress peerAddress = context.getPeerAddress();
                Principal senderIdentity = context.getPeerIdentity();
                if (senderIdentity == null) {
                    // Build identity for OSCORE if it is used
                    if (context.get(OSCoreEndpointContextInfo.OSCORE_RECIPIENT_ID) != null) {
                        String recipient = context.get(OSCoreEndpointContextInfo.OSCORE_RECIPIENT_ID);
                        return Identity.oscoreOnly(peerAddress,
                                new OscoreIdentity(Hex.decodeHex(recipient.toCharArray())));
                    }
                    return Identity.unsecure(peerAddress);
                } else {
                    return null;
                }
            }

            @Override
            public EndpointContext createEndpointContext(Identity identity, boolean allowConnectionInitiation) {
                // TODO OSCORE : should we add properties to endpoint context ?
                return new AddressEndpointContext(identity.getPeerAddress());
            }
        };
    }
}
