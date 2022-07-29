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
package org.eclipse.leshan.server.californium.endpoint;

import java.util.List;

import org.eclipse.californium.core.coap.Request;
import org.eclipse.californium.core.coap.Response;
import org.eclipse.californium.core.server.resources.Resource;
import org.eclipse.leshan.core.observation.Observation;
import org.eclipse.leshan.core.request.LwM2mRequest;
import org.eclipse.leshan.core.response.AbstractLwM2mResponse;
import org.eclipse.leshan.core.response.LwM2mResponse;
import org.eclipse.leshan.server.endpoint.ClientProfile;
import org.eclipse.leshan.server.endpoint.LwM2mEndpointToolbox;
import org.eclipse.leshan.server.endpoint.LwM2mRequestReceiver;
import org.eclipse.leshan.server.endpoint.PeerProfile;

public interface CoapMessageTranslator {

    Request createCoapRequest(PeerProfile foreignPeerProfile, LwM2mRequest<? extends LwM2mResponse> request,
            LwM2mEndpointToolbox toolbox);

    <T extends LwM2mResponse> T createLwM2mResponse(PeerProfile foreignPeerProfile, LwM2mRequest<T> lwm2mRequest,
            Request coapRequest, Response coapResponse, LwM2mEndpointToolbox toolbox);

    List<Resource> createResources(LwM2mRequestReceiver receiver, LwM2mEndpointToolbox toolbox);

    AbstractLwM2mResponse createObservation(Observation observation, Response coapResponse,
            LwM2mEndpointToolbox toolbox, ClientProfile profile);
}
