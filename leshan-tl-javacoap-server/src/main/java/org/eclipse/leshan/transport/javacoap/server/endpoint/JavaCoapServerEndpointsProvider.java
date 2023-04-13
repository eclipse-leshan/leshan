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

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.eclipse.leshan.core.endpoint.Protocol;
import org.eclipse.leshan.server.LeshanServer;
import org.eclipse.leshan.server.endpoint.LwM2mServerEndpoint;
import org.eclipse.leshan.server.endpoint.LwM2mServerEndpointsProvider;
import org.eclipse.leshan.server.endpoint.ServerEndpointToolbox;
import org.eclipse.leshan.server.observation.LwM2mNotificationReceiver;
import org.eclipse.leshan.server.request.UplinkRequestReceiver;
import org.eclipse.leshan.server.security.ServerSecurityInfo;
import org.eclipse.leshan.transport.javacoap.server.observation.CoapNotificationReceiver;
import org.eclipse.leshan.transport.javacoap.server.observation.LwM2mObservationsStore;
import org.eclipse.leshan.transport.javacoap.server.resource.RegistrationResource;
import org.eclipse.leshan.transport.javacoap.server.resource.SendResource;

import com.mbed.coap.packet.CoapRequest;
import com.mbed.coap.packet.CoapResponse;
import com.mbed.coap.server.CoapServer;
import com.mbed.coap.server.CoapServerBuilder;
import com.mbed.coap.server.RouterService;
import com.mbed.coap.server.filter.TokenGeneratorFilter;
import com.mbed.coap.transport.udp.DatagramSocketTransport;
import com.mbed.coap.utils.Service;

public class JavaCoapServerEndpointsProvider implements LwM2mServerEndpointsProvider {

    private CoapServer coapServer;
    private final InetSocketAddress localAddress;
    private JavaCoapServerEndpoint lwm2mEndpoint;

    public JavaCoapServerEndpointsProvider(InetSocketAddress localAddress) {
        this.localAddress = localAddress;
    }

    @Override
    public void createEndpoints(UplinkRequestReceiver requestReceiver,
            final LwM2mNotificationReceiver notificationReceiver, final ServerEndpointToolbox toolbox,
            ServerSecurityInfo serverSecurityInfo, LeshanServer server) {

        // TODO: HACK to be able to get local URI in resource, need to discuss about it with java-coap.
        EndpointUriProvider endpointUriProvider = new EndpointUriProvider(Protocol.COAP);

        // Create Resources / Routes
        RegistrationResource registerResource = new RegistrationResource(requestReceiver, toolbox.getLinkParser(),
                endpointUriProvider);
        Service<CoapRequest, CoapResponse> resources = RouterService.builder() //
                .any("/rd/*", registerResource) //
                .any("/rd", registerResource)//
                .any("/dp",
                        new SendResource(requestReceiver, toolbox.getDecoder(), toolbox.getProfileProvider(),
                                endpointUriProvider))//
                .build();

        // Create CoAP Server
        coapServer = createCoapServer() //
                .transport(new DatagramSocketTransport(localAddress)) //
                .route(resources) //
                .notificationsReceiver(new CoapNotificationReceiver(coapServer, notificationReceiver,
                        server.getRegistrationStore(), server.getModelProvider(), toolbox.getDecoder())) //
                .observationsStore(new LwM2mObservationsStore(server.getRegistrationStore(), notificationReceiver)) //
                .build();
        endpointUriProvider.setCoapServer(coapServer);

        lwm2mEndpoint = new JavaCoapServerEndpoint(coapServer, new ServerCoapMessageTranslator(), toolbox);

    }

    protected CoapServerBuilder createCoapServer() {
        return CoapServer.builder().outboundFilter(TokenGeneratorFilter.RANDOM);
    }

    @Override
    public List<LwM2mServerEndpoint> getEndpoints() {
        // java-coap CoapServer support only 1 socket/endpoint by server.
        // So for now this endpoint provider support only 1 endpoint.
        // If we want to support more, we need to :
        // - either create serveral coap server by provider
        // - or create a kind or custom transport proxy with several transport.
        if (lwm2mEndpoint == null) {
            return Collections.emptyList();
        } else {
            return Arrays.asList(lwm2mEndpoint);
        }
    }

    @Override
    public LwM2mServerEndpoint getEndpoint(URI uri) {
        if (lwm2mEndpoint != null && lwm2mEndpoint.getURI().equals(uri))
            return lwm2mEndpoint;
        else
            return null;
    }

    @Override
    public void start() {
        try {
            coapServer.start();
        } catch (IOException e) {
            throw new IllegalStateException("Unable to start java-coap endpoint", e);
        }
    }

    @Override
    public void stop() {
        // TODO in Leshan stop means "we can restart after a stop"
        // but in java-coap : There is no restart after stop, need to create new instance to start again.
        // I don't know if we should remove stop from Leshan API ?
        coapServer.stop();
    }

    @Override
    public void destroy() {
        // TODO there is no destroy, so we just stop ?
        coapServer.stop();
    }
}
