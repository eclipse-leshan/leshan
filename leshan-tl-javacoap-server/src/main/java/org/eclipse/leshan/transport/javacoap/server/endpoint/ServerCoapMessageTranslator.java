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

import org.eclipse.leshan.core.request.DownlinkRequest;
import org.eclipse.leshan.core.response.LwM2mResponse;
import org.eclipse.leshan.server.endpoint.ServerEndpointToolbox;
import org.eclipse.leshan.server.profile.ClientProfile;
import org.eclipse.leshan.transport.javacoap.server.request.CoapRequestBuilder;
import org.eclipse.leshan.transport.javacoap.server.request.LwM2mResponseBuilder;

import com.mbed.coap.packet.CoapRequest;
import com.mbed.coap.packet.CoapResponse;

public class ServerCoapMessageTranslator {

    public CoapRequest createCoapRequest(ClientProfile clientProfile,
            DownlinkRequest<? extends LwM2mResponse> lwm2mRequest,
            ServerEndpointToolbox toolbox /* , IdentityHandler identityHandler */) {

        CoapRequestBuilder builder = new CoapRequestBuilder(clientProfile.getRegistration(),
                clientProfile.getTransportData(), clientProfile.getRootPath(), clientProfile.getModel(),
                toolbox.getEncoder());
        lwm2mRequest.accept(builder);
        return builder.getRequest();
    }

    public <T extends LwM2mResponse> T createLwM2mResponse(ClientProfile clientProfile, DownlinkRequest<T> lwm2mRequest,
            CoapResponse coapResponse, CoapRequest coapRequest, ServerEndpointToolbox toolbox) {

        LwM2mResponseBuilder<T> builder = new LwM2mResponseBuilder<T>(coapResponse, coapRequest,
                clientProfile.getEndpoint(), clientProfile.getModel(), toolbox.getDecoder(), toolbox.getLinkParser());
        lwm2mRequest.accept(builder);
        return builder.getResponse();
    }
}
