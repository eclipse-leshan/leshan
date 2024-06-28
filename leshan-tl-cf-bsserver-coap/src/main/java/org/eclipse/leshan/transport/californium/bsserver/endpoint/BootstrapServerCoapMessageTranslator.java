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
package org.eclipse.leshan.transport.californium.bsserver.endpoint;

import java.util.Arrays;
import java.util.List;

import org.eclipse.californium.core.coap.Request;
import org.eclipse.californium.core.coap.Response;
import org.eclipse.californium.core.server.resources.Resource;
import org.eclipse.leshan.bsserver.BootstrapSession;
import org.eclipse.leshan.bsserver.endpoint.BootstrapServerEndpointToolbox;
import org.eclipse.leshan.bsserver.request.BootstrapUplinkRequestReceiver;
import org.eclipse.leshan.core.request.BootstrapDownlinkRequest;
import org.eclipse.leshan.core.response.LwM2mResponse;
import org.eclipse.leshan.transport.californium.bsserver.BootstrapResource;
import org.eclipse.leshan.transport.californium.bsserver.request.CoapRequestBuilder;
import org.eclipse.leshan.transport.californium.bsserver.request.LwM2mResponseBuilder;
import org.eclipse.leshan.transport.californium.identity.IdentityHandler;
import org.eclipse.leshan.transport.californium.identity.IdentityHandlerProvider;

public class BootstrapServerCoapMessageTranslator {

    public Request createCoapRequest(BootstrapSession destination,
            BootstrapDownlinkRequest<? extends LwM2mResponse> lwm2mRequest, BootstrapServerEndpointToolbox toolbox,
            IdentityHandler identityHandler) {
        CoapRequestBuilder builder = new CoapRequestBuilder(destination.getClientTransportData(),
                destination.getModel(), toolbox.getEncoder(), identityHandler);
        lwm2mRequest.accept(builder);
        return builder.getRequest();
    }

    public <T extends LwM2mResponse> T createLwM2mResponse(BootstrapSession destination,
            BootstrapDownlinkRequest<T> lwm2mRequest, Response coapResponse, BootstrapServerEndpointToolbox toolbox) {
        LwM2mResponseBuilder<T> builder = new LwM2mResponseBuilder<T>(coapResponse, destination.getEndpoint(),
                destination.getModel(), toolbox.getDecoder(), toolbox.getLinkParser());
        lwm2mRequest.accept(builder);
        return builder.getResponse();
    }

    public List<Resource> createResources(BootstrapUplinkRequestReceiver receiver,
            BootstrapServerEndpointToolbox toolbox, IdentityHandlerProvider identityHandlerProvider) {
        return Arrays.asList((Resource) new BootstrapResource(receiver, identityHandlerProvider));
    }
}
