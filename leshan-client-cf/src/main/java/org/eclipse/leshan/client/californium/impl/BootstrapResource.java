/*******************************************************************************
 * Copyright (c) 2015 Sierra Wireless and others.
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v1.0 which accompany this distribution.
 * 
 * The Eclipse Public License is available at
 *    http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 *    http://www.eclipse.org/org/documents/edl-v10.html.
 * 
 * Contributors:
 *     Sierra Wireless - initial API and implementation
 *     Achim Kraus (Bosch Software Innovations GmbH) - use ServerIdentity
 *******************************************************************************/
package org.eclipse.leshan.client.californium.impl;

import static org.eclipse.leshan.core.californium.ResponseCodeUtil.toCoapResponseCode;

import org.eclipse.californium.core.CoapResource;
import org.eclipse.californium.core.coap.MediaTypeRegistry;
import org.eclipse.californium.core.coap.MessageObserverAdapter;
import org.eclipse.californium.core.coap.Response;
import org.eclipse.californium.core.server.resources.CoapExchange;
import org.eclipse.californium.core.server.resources.Resource;
import org.eclipse.leshan.client.request.ServerIdentity;
import org.eclipse.leshan.client.servers.BootstrapHandler;
import org.eclipse.leshan.core.request.BootstrapFinishRequest;
import org.eclipse.leshan.core.response.BootstrapFinishResponse;
import org.eclipse.leshan.core.response.SendableResponse;

/**
 * A CoAP {@link Resource} in charge of handling the Bootstrap Finish indication from the bootstrap server.
 */
public class BootstrapResource extends CoapResource {

    private final BootstrapHandler bootstrapHandler;

    public BootstrapResource(BootstrapHandler bootstrapHandler) {
        super("bs", false);
        this.bootstrapHandler = bootstrapHandler;
    }

    @Override
    public void handlePOST(CoapExchange exchange) {
        // Handle bootstrap request
        ServerIdentity identity = ResourceUtil.extractServerIdentity(exchange, bootstrapHandler);
        final SendableResponse<BootstrapFinishResponse> sendableResponse = bootstrapHandler.finished(identity,
                new BootstrapFinishRequest());

        // Create CoAP response
        Response coapResponse = new Response(toCoapResponseCode(sendableResponse.getResponse().getCode()));
        if (sendableResponse.getResponse().getCode().isError()) {
            coapResponse.setPayload(sendableResponse.getResponse().getErrorMessage());
            coapResponse.getOptions().setContentFormat(MediaTypeRegistry.TEXT_PLAIN);
        }

        // Send response
        coapResponse.addMessageObserver(new MessageObserverAdapter() {
            @Override
            public void onSent() {
                sendableResponse.sent();
            }
        });
        exchange.respond(coapResponse);
    }
}
