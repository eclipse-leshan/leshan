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
package org.eclipse.leshan.transport.javacoap.endpoint;

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
import org.eclipse.leshan.transport.javacoap.resource.RegistrationResource;
import org.eclipse.leshan.transport.javacoap.resource.ResourcesService;
import org.eclipse.leshan.transport.javacoap.resource.SendResource;

import com.mbed.coap.server.CoapServer;
import com.mbed.coap.transport.udp.DatagramSocketTransport;

public class JavaCoapServerEndpointsProvider implements LwM2mServerEndpointsProvider {

    private CoapServer coapServer;
    private final InetSocketAddress localAddress;
    private JavaCoapServerEndpoint lwm2mEndpoint;

    public JavaCoapServerEndpointsProvider(InetSocketAddress localAddress) {
        this.localAddress = localAddress;
    }

    @Override
    public void createEndpoints(UplinkRequestReceiver requestReceiver, LwM2mNotificationReceiver notificationReceiver,
            ServerEndpointToolbox toolbox, ServerSecurityInfo serverSecurityInfo, LeshanServer server) {

        // HACK to be able to get local URI in resource, need to discuss about it with java-coap.
        EndpointUriProvider endpointUriProvider = new EndpointUriProvider(Protocol.COAP);

        // create Resources / Routes
        RegistrationResource registerResource = new RegistrationResource(requestReceiver, toolbox.getLinkParser(),
                endpointUriProvider);
        ResourcesService resources = ResourcesService.builder() //
                .add("/rd/*", registerResource) //
                .add("/rd", registerResource)//
                .add("/dp",
                        new SendResource(requestReceiver, toolbox.getDecoder(), toolbox.getProfileProvider(),
                                endpointUriProvider))//
                .build();
        coapServer = CoapServer.builder().transport(new DatagramSocketTransport(localAddress)).route(resources).build();
        endpointUriProvider.setCoapServer(coapServer);

        lwm2mEndpoint = new JavaCoapServerEndpoint(coapServer, new ServerCoapMessageTranslator(), toolbox,
                notificationReceiver, server.getRegistrationStore());

    }

    @Override
    public List<LwM2mServerEndpoint> getEndpoints() {
        // We support only one endpoint for now
        // TODO I don't know if 1 java-coap server can support several endpoints ?
        if (lwm2mEndpoint == null) {
            return Collections.emptyList();
        } else {
            return Arrays.asList(lwm2mEndpoint);
        }
    }

    @Override
    public LwM2mServerEndpoint getEndpoint(URI uri) {
        // We support only one endpoint for now
        // TODO I don't know if 1 java-coap server can support several endpoints ?
        if (lwm2mEndpoint != null && lwm2mEndpoint.getURI().equals(uri))
            return lwm2mEndpoint;
        else
            return null;
    }

    @Override
    public void start() {
        try {
            coapServer.start();
        } catch (IllegalStateException | IOException e) {
            // TODO handle this correctly
            e.printStackTrace();
        }
    }

    @Override
    public void stop() {
        // TODO in Leshan stop means "we can restart after a stop", so we should check what means stop for java-coap
        coapServer.stop();
    }

    @Override
    public void destroy() {
        // TODO there is no destroy, so we just stop ?
        coapServer.stop();
    }
}
