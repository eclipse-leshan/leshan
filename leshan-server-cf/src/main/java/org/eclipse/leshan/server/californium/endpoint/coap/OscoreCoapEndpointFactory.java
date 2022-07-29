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
package org.eclipse.leshan.server.californium.endpoint.coap;

import java.net.InetSocketAddress;
import java.net.URI;

import org.eclipse.californium.core.network.CoapEndpoint;
import org.eclipse.californium.core.network.CoapEndpoint.Builder;
import org.eclipse.californium.elements.config.Configuration;
import org.eclipse.californium.oscore.OSCoreCoapStackFactory;
import org.eclipse.leshan.core.californium.oscore.cf.InMemoryOscoreContextDB;
import org.eclipse.leshan.server.californium.LwM2mOscoreStore;
import org.eclipse.leshan.server.californium.OscoreContextCleaner;
import org.eclipse.leshan.server.endpoint.LwM2mNotificationReceiver;
import org.eclipse.leshan.server.endpoint.LwM2mServer;
import org.eclipse.leshan.server.security.EditableSecurityStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OscoreCoapEndpointFactory extends CoapEndpointFactory {

    private static final Logger LOG = LoggerFactory.getLogger(OscoreCoapEndpointFactory.class);

    public OscoreCoapEndpointFactory(URI uri) {
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
            LwM2mServer server, LwM2mNotificationReceiver notificationReceiver) {
        CoapEndpoint.Builder builder = super.createUnsecuredEndpointBuilder(address, coapConfig, server,
                notificationReceiver);

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
}
