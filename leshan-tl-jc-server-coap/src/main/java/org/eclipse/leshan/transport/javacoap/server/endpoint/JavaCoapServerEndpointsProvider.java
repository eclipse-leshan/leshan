/*******************************************************************************
 * Copyright (c) 2023 Sierra Wireless and others.
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
package org.eclipse.leshan.transport.javacoap.server.endpoint;

import java.net.InetSocketAddress;

import org.eclipse.leshan.core.endpoint.Protocol;
import org.eclipse.leshan.servers.security.SecurityStore;
import org.eclipse.leshan.servers.security.ServerSecurityInfo;
import org.eclipse.leshan.transport.javacoap.identity.DefaultCoapIdentityHandler;

import com.mbed.coap.packet.CoapRequest;
import com.mbed.coap.packet.CoapResponse;
import com.mbed.coap.server.CoapServer;
import com.mbed.coap.server.CoapServerBuilder;
import com.mbed.coap.server.filter.TokenGeneratorFilter;
import com.mbed.coap.server.observe.NotificationsReceiver;
import com.mbed.coap.server.observe.ObservationsStore;
import com.mbed.coap.transport.CoapTransport;
import com.mbed.coap.transport.udp.DatagramSocketTransport;
import com.mbed.coap.utils.Service;

public class JavaCoapServerEndpointsProvider extends AbstractJavaCoapServerEndpointsProvider {

    public JavaCoapServerEndpointsProvider(InetSocketAddress localAddress) {
        super(Protocol.COAP, "CoAP over UDP endpoint based on java-coap library", localAddress,
                new DefaultCoapIdentityHandler());
    }

    @Override
    protected CoapServer createCoapServer(CoapTransport transport, Service<CoapRequest, CoapResponse> resources,
            NotificationsReceiver notificationReceiver, ObservationsStore observationsStore) {
        return createCoapServer() //
                .transport(transport) //
                .route(resources) //
                .notificationsReceiver(notificationReceiver) //
                .observationsStore(observationsStore) //
                .build();
    }

    protected CoapServerBuilder createCoapServer() {
        return CoapServer.builder().outboundFilter(TokenGeneratorFilter.RANDOM);
    }

    @Override
    protected CoapTransport createCoapTransport(InetSocketAddress localAddress, ServerSecurityInfo serverSecurityInfo,
            SecurityStore securityStore) {
        return new DatagramSocketTransport(localAddress);
    }

    @Override
    protected ConnectionsManager createConnectionManager(CoapTransport transport) {
        return () -> {
            // no connection to manage with CoAP
        };
    }
}
