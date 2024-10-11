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
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.eclipse.leshan.core.endpoint.EndpointUri;
import org.eclipse.leshan.core.endpoint.Protocol;
import org.eclipse.leshan.server.LeshanServer;
import org.eclipse.leshan.server.endpoint.EffectiveEndpointUriProvider;
import org.eclipse.leshan.server.endpoint.LwM2mServerEndpoint;
import org.eclipse.leshan.server.endpoint.LwM2mServerEndpointsProvider;
import org.eclipse.leshan.server.endpoint.ServerEndpointToolbox;
import org.eclipse.leshan.server.observation.LwM2mNotificationReceiver;
import org.eclipse.leshan.server.request.UplinkDeviceManagementRequestReceiver;
import org.eclipse.leshan.servers.security.SecurityStore;
import org.eclipse.leshan.servers.security.ServerSecurityInfo;
import org.eclipse.leshan.transport.javacoap.identity.IdentityHandler;
import org.eclipse.leshan.transport.javacoap.server.observation.CoapNotificationReceiver;
import org.eclipse.leshan.transport.javacoap.server.observation.LwM2mObservationsStore;
import org.eclipse.leshan.transport.javacoap.server.resource.RegistrationResource;
import org.eclipse.leshan.transport.javacoap.server.resource.SendResource;

import com.mbed.coap.packet.CoapRequest;
import com.mbed.coap.packet.CoapResponse;
import com.mbed.coap.server.CoapServer;
import com.mbed.coap.server.RouterService;
import com.mbed.coap.server.observe.NotificationsReceiver;
import com.mbed.coap.server.observe.ObservationsStore;
import com.mbed.coap.utils.Service;

public abstract class AbstractJavaCoapServerEndpointsProvider implements LwM2mServerEndpointsProvider {

    private final Protocol supportedProtocol;
    private final String endpointDescription;
    private final InetSocketAddress localAddress;
    private CoapServer coapServer;
    private JavaCoapServerEndpoint lwm2mEndpoint;
    private final IdentityHandler identityHandler;

    public AbstractJavaCoapServerEndpointsProvider(Protocol protocol, String endpointDescription,
            InetSocketAddress localAddress, IdentityHandler identityHandler) {
        this.supportedProtocol = protocol;
        this.endpointDescription = endpointDescription;
        this.localAddress = localAddress;
        this.identityHandler = identityHandler;
    }

    @Override
    public void createEndpoints(UplinkDeviceManagementRequestReceiver requestReceiver,
            final LwM2mNotificationReceiver notificationReceiver, final ServerEndpointToolbox toolbox,
            ServerSecurityInfo serverSecurityInfo, LeshanServer server) {

        EffectiveEndpointUriProvider endpointUriProvider = new EffectiveEndpointUriProvider();

        // Create Resources / Routes
        RegistrationResource registerResource = new RegistrationResource(requestReceiver, toolbox.getLinkParser(),
                endpointUriProvider, identityHandler);
        Service<CoapRequest, CoapResponse> resources = RouterService.builder() //
                .any("/rd/*", registerResource) //
                .any("/rd", registerResource)//
                .any("/dp",
                        new SendResource(requestReceiver, toolbox.getDecoder(), toolbox.getProfileProvider(),
                                endpointUriProvider, identityHandler))//
                .build();

        // Create CoAP Server
        coapServer = createCoapServer(localAddress, //
                serverSecurityInfo, //
                server.getSecurityStore(), //
                resources, //
                new CoapNotificationReceiver(coapServer, notificationReceiver, server.getRegistrationStore(),
                        server.getModelProvider(), toolbox.getDecoder(), endpointUriProvider), //
                new LwM2mObservationsStore(server.getRegistrationStore(), notificationReceiver, identityHandler,
                        endpointUriProvider) //
        );

        lwm2mEndpoint = new JavaCoapServerEndpoint(supportedProtocol, endpointDescription, coapServer,
                new ServerCoapMessageTranslator(identityHandler), toolbox);

        endpointUriProvider.setEndpoint(lwm2mEndpoint);
    }

    protected abstract CoapServer createCoapServer(InetSocketAddress localAddress,
            ServerSecurityInfo serverSecurityInfo, SecurityStore SecurityStore,
            Service<CoapRequest, CoapResponse> resources, NotificationsReceiver notificationReceiver,
            ObservationsStore observationsStore);

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
    public LwM2mServerEndpoint getEndpoint(EndpointUri uri) {
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
