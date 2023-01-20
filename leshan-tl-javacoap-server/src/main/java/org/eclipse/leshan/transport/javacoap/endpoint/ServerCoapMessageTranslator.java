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
package org.eclipse.leshan.transport.javacoap.endpoint;

import java.util.List;

import org.eclipse.leshan.core.observation.Observation;
import org.eclipse.leshan.core.request.DownlinkRequest;
import org.eclipse.leshan.core.response.AbstractLwM2mResponse;
import org.eclipse.leshan.core.response.LwM2mResponse;
import org.eclipse.leshan.server.endpoint.ServerEndpointToolbox;
import org.eclipse.leshan.server.profile.ClientProfile;
import org.eclipse.leshan.server.request.UplinkRequestReceiver;
import org.eclipse.leshan.transport.javacoap.request.CoapRequestBuilder;
import org.eclipse.leshan.transport.javacoap.request.LwM2mResponseBuilder;
import org.eclipse.leshan.transport.javacoap.request.RandomTokenGenerator;
import org.eclipse.leshan.transport.javacoap.resource.LwM2mCoapResource;

import com.mbed.coap.packet.CoapRequest;
import com.mbed.coap.packet.CoapResponse;
import com.mbed.coap.packet.Opaque;

public class ServerCoapMessageTranslator {

    // TODO : this should be configurable
    private final RandomTokenGenerator tokenGenerator = new RandomTokenGenerator(8);

    public CoapRequest createCoapRequest(ClientProfile clientProfile,
            DownlinkRequest<? extends LwM2mResponse> lwm2mRequest,
            ServerEndpointToolbox toolbox /* , IdentityHandler identityHandler */) {

        CoapRequestBuilder builder = new CoapRequestBuilder(clientProfile.getIdentity(), clientProfile.getRootPath(),
                clientProfile.getRegistrationId(), clientProfile.getEndpoint(), clientProfile.getModel(),
                toolbox.getEncoder(), clientProfile.canInitiateConnection(), null, tokenGenerator);
        lwm2mRequest.accept(builder);
        return builder.getRequest();
    }

    public <T extends LwM2mResponse> T createLwM2mResponse(ClientProfile clientProfile, DownlinkRequest<T> lwm2mRequest,
            CoapRequest coapRequest, CoapResponse coapResponse, ServerEndpointToolbox toolbox,
            /* TODO HACK */ Opaque token) {

        LwM2mResponseBuilder<T> builder = new LwM2mResponseBuilder<T>(coapRequest, coapResponse,
                clientProfile.getEndpoint(), clientProfile.getModel(), toolbox.getDecoder(), toolbox.getLinkParser(),
                clientProfile.getRegistrationId(), token);
        lwm2mRequest.accept(builder);
        return builder.getResponse();
    }

    public List<LwM2mCoapResource> createResources(UplinkRequestReceiver receiver, ServerEndpointToolbox toolbox
    /* , IdentityHandlerProvider identityHandlerProvider */) {
        // TODO implement it ?
//        return Arrays.asList( //
//                (Resource) new RegisterResource(receiver, toolbox.getLinkParser(), identityHandlerProvider), //
//                (Resource) new SendResource(receiver, toolbox.getDecoder(), toolbox.getProfileProvider(),
//                        identityHandlerProvider));
        return null;
    }

    public AbstractLwM2mResponse createObservation(Observation observation, CoapResponse coapResponse,
            ServerEndpointToolbox toolbox, ClientProfile profile) {
// TODO implement it ?
        return null;
    }

    // TODO HACK remove this public access
    public RandomTokenGenerator getTokenGenerator() {
        return tokenGenerator;
    }
}
