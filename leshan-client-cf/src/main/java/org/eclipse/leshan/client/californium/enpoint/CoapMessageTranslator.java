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

import java.util.List;

import org.eclipse.californium.core.CoapServer;
import org.eclipse.californium.core.coap.Request;
import org.eclipse.californium.core.coap.Response;
import org.eclipse.californium.core.server.resources.Resource;
import org.eclipse.leshan.client.endpoint.ClientEndpointToolbox;
import org.eclipse.leshan.client.endpoint.LwM2mRequestReceiver;
import org.eclipse.leshan.client.resource.LwM2mObjectTree;
import org.eclipse.leshan.client.servers.ServerIdentity;
import org.eclipse.leshan.core.model.LwM2mModel;
import org.eclipse.leshan.core.request.UplinkRequest;
import org.eclipse.leshan.core.response.LwM2mResponse;

public interface CoapMessageTranslator {

    Request createCoapRequest(ServerIdentity serverIdentity, UplinkRequest<? extends LwM2mResponse> request,
            ClientEndpointToolbox toolbox, LwM2mModel model);

    <T extends LwM2mResponse> T createLwM2mResponse(ServerIdentity serverIdentity, UplinkRequest<T> lwm2mRequest,
            Request coapRequest, Response coapResponse, ClientEndpointToolbox toolbox);

    List<Resource> createResources(CoapServer coapServer, ServerIdentityExtractor identityExtractor,
            LwM2mRequestReceiver receiver, ClientEndpointToolbox toolbox, LwM2mObjectTree objectTree);

}
