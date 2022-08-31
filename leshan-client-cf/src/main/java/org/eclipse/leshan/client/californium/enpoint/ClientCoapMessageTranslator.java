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
package org.eclipse.leshan.client.californium.enpoint;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.californium.core.CoapResource;
import org.eclipse.californium.core.CoapServer;
import org.eclipse.californium.core.coap.Request;
import org.eclipse.californium.core.coap.Response;
import org.eclipse.californium.core.server.resources.Resource;
import org.eclipse.leshan.client.californium.bootstrap.BootstrapResource;
import org.eclipse.leshan.client.californium.object.ObjectResource;
import org.eclipse.leshan.client.californium.request.CoapRequestBuilder;
import org.eclipse.leshan.client.californium.request.LwM2mClientResponseBuilder;
import org.eclipse.leshan.client.endpoint.ClientEndpointToolbox;
import org.eclipse.leshan.client.endpoint.LwM2mRequestReceiver;
import org.eclipse.leshan.client.resource.LwM2mObjectEnabler;
import org.eclipse.leshan.client.resource.LwM2mObjectTree;
import org.eclipse.leshan.client.resource.listener.ObjectListener;
import org.eclipse.leshan.client.resource.listener.ObjectsListenerAdapter;
import org.eclipse.leshan.client.servers.ServerIdentity;
import org.eclipse.leshan.core.model.LwM2mModel;
import org.eclipse.leshan.core.request.UplinkRequest;
import org.eclipse.leshan.core.response.LwM2mResponse;

public class ClientCoapMessageTranslator implements CoapMessageTranslator {

    @Override
    public Request createCoapRequest(ServerIdentity serverIdentity, UplinkRequest<? extends LwM2mResponse> lwm2mRequest,
            ClientEndpointToolbox toolbox, LwM2mModel model) {

        // create CoAP Request
        CoapRequestBuilder builder = new CoapRequestBuilder(serverIdentity.getIdentity(), toolbox.getEncoder(), model,
                toolbox.getLinkSerializer());
        lwm2mRequest.accept(builder);
        return builder.getRequest();
    }

    @Override
    public <T extends LwM2mResponse> T createLwM2mResponse(ServerIdentity serverIdentity, UplinkRequest<T> lwm2mRequest,
            Request coapRequest, Response coapResponse, ClientEndpointToolbox toolbox) {

        // create LWM2M Response
        LwM2mClientResponseBuilder<T> builder = new LwM2mClientResponseBuilder<T>(coapResponse);
        lwm2mRequest.accept(builder);
        return builder.getResponse();
    }

    @Override
    public List<Resource> createResources(CoapServer coapServer, ServerIdentityExtractor extractor,
            LwM2mRequestReceiver receiver, ClientEndpointToolbox toolbox, LwM2mObjectTree objectTree) {
        ArrayList<Resource> resources = new ArrayList<>();

        // create bootstrap resource
        resources.add(new BootstrapResource(extractor, receiver));

        // create object resources
        for (LwM2mObjectEnabler enabler : objectTree.getObjectEnablers().values()) {
            resources.add(createObjectResource(enabler, extractor, receiver, toolbox));
        }

        // link resource to object tree
        objectTree.addListener(new ObjectsListenerAdapter() {
            @Override
            public void objectAdded(LwM2mObjectEnabler object) {
                CoapResource clientObject = createObjectResource(object, extractor, receiver, toolbox);
                coapServer.add(clientObject);
            }

            @Override
            public void objectRemoved(LwM2mObjectEnabler object) {
                Resource resource = coapServer.getRoot().getChild(Integer.toString(object.getId()));
                if (resource instanceof ObjectListener) {
                    object.removeListener((ObjectListener) (resource));
                }
                coapServer.remove(resource);
            }
        });
        return resources;
    }

    public CoapResource createObjectResource(LwM2mObjectEnabler objectEnabler,
            ServerIdentityExtractor identityExtractor, LwM2mRequestReceiver requestReceiver,
            ClientEndpointToolbox toolbox) {
        ObjectResource objectResource = new ObjectResource(objectEnabler.getId(), identityExtractor, requestReceiver,
                toolbox);
        objectEnabler.addListener(objectResource);
        return objectResource;
    }
}
