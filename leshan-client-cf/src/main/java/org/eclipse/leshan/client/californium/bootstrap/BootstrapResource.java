/*******************************************************************************
 * Copyright (c) 2015 Sierra Wireless and others.
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
 *     Achim Kraus (Bosch Software Innovations GmbH) - use ServerIdentity
 *******************************************************************************/
package org.eclipse.leshan.client.californium.bootstrap;

import static org.eclipse.leshan.core.californium.ResponseCodeUtil.toCoapResponseCode;

import org.eclipse.californium.core.coap.MediaTypeRegistry;
import org.eclipse.californium.core.coap.MessageObserverAdapter;
import org.eclipse.californium.core.coap.Request;
import org.eclipse.californium.core.coap.Response;
import org.eclipse.californium.core.server.resources.CoapExchange;
import org.eclipse.californium.core.server.resources.Resource;
import org.eclipse.leshan.client.californium.LwM2mClientCoapResource;
import org.eclipse.leshan.client.californium.endpoint.ServerIdentityExtractor;
import org.eclipse.leshan.client.request.DownlinkRequestReceiver;
import org.eclipse.leshan.client.servers.ServerIdentity;
import org.eclipse.leshan.core.californium.identity.IdentityHandlerProvider;
import org.eclipse.leshan.core.request.BootstrapFinishRequest;
import org.eclipse.leshan.core.response.BootstrapFinishResponse;
import org.eclipse.leshan.core.response.SendableResponse;

/**
 * A CoAP {@link Resource} in charge of handling the Bootstrap Finish indication from the bootstrap server.
 */
public class BootstrapResource extends LwM2mClientCoapResource {

    protected DownlinkRequestReceiver requestReceiver;

    public BootstrapResource(IdentityHandlerProvider identityHandlerProvider,
            ServerIdentityExtractor serverIdentityExtractor, DownlinkRequestReceiver requestReceiver) {
        super("bs", identityHandlerProvider, serverIdentityExtractor);
        this.requestReceiver = requestReceiver;
        this.setVisible(false);
    }

    @Override
    public void handlePOST(CoapExchange exchange) {
        // Handle bootstrap request
        Request coapRequest = exchange.advanced().getRequest();
        ServerIdentity identity = getServerOrRejectRequest(exchange, coapRequest);
        if (identity == null)
            return;

        // Acknowledge bootstrap finished request
        exchange.accept();
        final SendableResponse<BootstrapFinishResponse> sendableResponse = requestReceiver.requestReceived(identity,
                new BootstrapFinishRequest(coapRequest));

        // Create CoAP response
        Response coapResponse = new Response(toCoapResponseCode(sendableResponse.getResponse().getCode()));
        if (sendableResponse.getResponse().getCode().isError()) {
            // Use confirmable response to be ensure answer is well received before the end of the bootstrap session.
            // (after bootstrap session will not be able to handle retransmission correctly)
            coapResponse.setConfirmable(true);
            coapResponse.setPayload(sendableResponse.getResponse().getErrorMessage());
            coapResponse.getOptions().setContentFormat(MediaTypeRegistry.TEXT_PLAIN);
        }

        // Send response
        coapResponse.addMessageObserver(new MessageObserverAdapter() {
            @Override
            public void onAcknowledgement() {
                // we wait the response is acknowledged.
                // TODO should we modify SendableResponse by adding an acknowledged method ?
                sendableResponse.sent();
            }
        });
        exchange.respond(coapResponse);
    }
}
