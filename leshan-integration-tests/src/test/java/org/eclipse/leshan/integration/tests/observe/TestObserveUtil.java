/*******************************************************************************
 * Copyright (c) 2020 Sierra Wireless and others.
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
package org.eclipse.leshan.integration.tests.observe;

import java.net.URI;

import org.eclipse.californium.core.coap.CoAP.Type;
import org.eclipse.californium.core.coap.OptionSet;
import org.eclipse.californium.core.coap.Response;
import org.eclipse.californium.core.network.serialization.UdpDataSerializer;
import org.eclipse.californium.elements.AddressEndpointContext;
import org.eclipse.californium.elements.Connector;
import org.eclipse.californium.elements.EndpointContext;
import org.eclipse.californium.elements.RawData;
import org.eclipse.leshan.core.request.ContentFormat;

public class TestObserveUtil {
    public static void sendNotification(Connector connector, URI destination, byte[] payload,
            Response firstCoapResponse, ContentFormat contentFormat) {

        // create observe response
        Response response = new Response(org.eclipse.californium.core.coap.CoAP.ResponseCode.CONTENT);
        response.setType(Type.NON);
        response.setPayload(payload);
        response.setMID(firstCoapResponse.getMID() + 1);
        response.setToken(firstCoapResponse.getToken());
        OptionSet options = new OptionSet().setContentFormat(contentFormat.getCode())
                .setObserve(firstCoapResponse.getOptions().getObserve() + 1);
        response.setOptions(options);
        EndpointContext context = new AddressEndpointContext(destination.getHost(), destination.getPort());
        response.setDestinationContext(context);

        // serialize response
        UdpDataSerializer serializer = new UdpDataSerializer();
        RawData data = serializer.serializeResponse(response);

        // send it
        connector.send(data);
    }
}
