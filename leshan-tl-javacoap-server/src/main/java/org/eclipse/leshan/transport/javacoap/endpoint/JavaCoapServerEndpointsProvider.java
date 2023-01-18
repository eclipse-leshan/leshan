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
import java.net.URI;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.eclipse.leshan.core.endpoint.EndpointUriUtil;
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

public class JavaCoapServerEndpointsProvider implements LwM2mServerEndpointsProvider {

    private CoapServer coapServer;
    private final int coapPort;

    private JavaCoapServerEndpoint lwm2mEndpoint;

    public JavaCoapServerEndpointsProvider(int coapPort) {
        this.coapPort = coapPort;
    }

    @Override
    public void createEndpoints(UplinkRequestReceiver requestReceiver, LwM2mNotificationReceiver observationService,
            ServerEndpointToolbox toolbox, ServerSecurityInfo serverSecurityInfo, LeshanServer server) {

        // TODO we should get endpoint URI dynamically in Resources
        URI endpointURI = EndpointUriUtil.createUri("coap", "0.0.0.0", coapPort);

        // create Resources / Routes
        RegistrationResource registerResource = new RegistrationResource(requestReceiver, toolbox.getLinkParser(),
                endpointURI);
        ResourcesService resources = ResourcesService.builder() //
                .add("/rd/*", registerResource) //
                .add("/rd", registerResource)//
                .add("/dp",
                        new SendResource(requestReceiver, toolbox.getDecoder(), toolbox.getProfileProvider(),
                                endpointURI))//
                .build();
        coapServer = CoapServer.builder().transport(coapPort).route(resources).build();

        lwm2mEndpoint = new JavaCoapServerEndpoint(endpointURI, coapServer, new ServerCoapMessageTranslator(), toolbox);
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
