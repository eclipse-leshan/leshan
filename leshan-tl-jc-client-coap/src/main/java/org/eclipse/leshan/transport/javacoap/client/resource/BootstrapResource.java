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
package org.eclipse.leshan.transport.javacoap.client.resource;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CompletableFuture;

import org.eclipse.leshan.client.request.DownlinkRequestReceiver;
import org.eclipse.leshan.client.servers.LwM2mServer;
import org.eclipse.leshan.core.request.BootstrapFinishRequest;
import org.eclipse.leshan.core.response.BootstrapFinishResponse;
import org.eclipse.leshan.core.response.SendableResponse;
import org.eclipse.leshan.transport.javacoap.identity.IdentityHandler;
import org.eclipse.leshan.transport.javacoap.resource.LwM2mCoapResource;

import com.mbed.coap.packet.CoapRequest;
import com.mbed.coap.packet.CoapResponse;

/**
 * A CoAP {@link LwM2mCoapResource} in charge of handling the Bootstrap Finish indication from the bootstrap server.
 */
public class BootstrapResource extends LwM2mClientCoapResource {

    protected DownlinkRequestReceiver requestReceiver;

    public BootstrapResource(DownlinkRequestReceiver requestReceiver, IdentityHandler identityHandler,
            ServerIdentityExtractor serverIdentityExtractor) {
        super("bs", identityHandler, serverIdentityExtractor);
        this.requestReceiver = requestReceiver;
    }

    @Override
    public CompletableFuture<CoapResponse> handlePOST(CoapRequest coapRequest) {
        // Get server identity
        LwM2mServer identity = extractIdentity(coapRequest);
        if (identity == null) {
            return unknownServer();
        }

        // Acknowledge bootstrap finished request
        // TODO how to ack then send separated response
        // exchange.accept();

        final SendableResponse<BootstrapFinishResponse> sendableResponse = requestReceiver.requestReceived(identity,
                new BootstrapFinishRequest(coapRequest));
        BootstrapFinishResponse response = sendableResponse.getResponse();

        // TODO ACK to delay response.
        new Timer().schedule(new TimerTask() {

            @Override
            public void run() {
                sendableResponse.sent();
            }
        }, 500);

        if (response.getCode().isError()) {
            return errorMessage(response.getCode(), response.getErrorMessage());
        } else {
            return emptyResponse(response.getCode());
        }

        // TODO how to now if the response is acknowledge

//        // Create CoAP response
//        Response coapResponse = new Response(toCoapResponseCode(sendableResponse.getResponse().getCode()));
//        if (sendableResponse.getResponse().getCode().isError()) {
//            // Use confirmable response to be ensure answer is well received before the end of the bootstrap session.
//            // (after bootstrap session will not be able to handle retransmission correctly)
//            coapResponse.setConfirmable(true);
//            coapResponse.setPayload(sendableResponse.getResponse().getErrorMessage());
//            coapResponse.getOptions().setContentFormat(MediaTypeRegistry.TEXT_PLAIN);
//        }
//
//        // Send response
//        coapResponse.addMessageObserver(new MessageObserverAdapter() {
//            @Override
//            public void onAcknowledgement() {
//                // we wait the response is acknowledged.
//                // TODO should we modify SendableResponse by adding an acknowledged method ?
//                sendableResponse.sent();
//            }
//        });
//        exchange.respond(coapResponse);

    }
}
