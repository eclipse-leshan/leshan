/*******************************************************************************
 * Copyright (c) 2021 Sierra Wireless and others.
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
package org.eclipse.leshan.transport.javacoap.resource;

import static java.util.concurrent.CompletableFuture.completedFuture;

import java.net.URI;
import java.util.concurrent.CompletableFuture;

import org.eclipse.leshan.core.ResponseCode;
import org.eclipse.leshan.core.node.TimestampedLwM2mNodes;
import org.eclipse.leshan.core.node.codec.CodecException;
import org.eclipse.leshan.core.node.codec.LwM2mDecoder;
import org.eclipse.leshan.core.request.ContentFormat;
import org.eclipse.leshan.core.request.Identity;
import org.eclipse.leshan.core.request.SendRequest;
import org.eclipse.leshan.core.request.exception.InvalidRequestException;
import org.eclipse.leshan.core.response.SendResponse;
import org.eclipse.leshan.core.response.SendableResponse;
import org.eclipse.leshan.server.profile.ClientProfile;
import org.eclipse.leshan.server.profile.ClientProfileProvider;
import org.eclipse.leshan.server.request.UplinkRequestReceiver;
import org.eclipse.leshan.transport.javacoap.request.ResponseCodeUtil;

import com.mbed.coap.packet.CoapRequest;
import com.mbed.coap.packet.CoapResponse;

/**
 * A CoAP Resource used to handle "Send" request sent by LWM2M devices.
 *
 * @see SendRequest
 */
public class SendResource extends LwM2mCoapResource {

    public static final String RESOURCE_NAME = "dp";
    public static final String RESOURCE_URI = "/" + RESOURCE_NAME + "/*";

    private final LwM2mDecoder decoder;
    private final UplinkRequestReceiver receiver;
    private final ClientProfileProvider profileProvider;

    private final URI endpointUsed;

    public SendResource(UplinkRequestReceiver receiver, LwM2mDecoder decoder, ClientProfileProvider profileProvider,
            URI endpointUsed) {
        super(RESOURCE_URI);
        this.decoder = decoder;
        this.receiver = receiver;
        this.profileProvider = profileProvider;
        this.endpointUsed = endpointUsed;
    }

    @Override
    public CompletableFuture<CoapResponse> handlePOST(CoapRequest coapRequest) {
        Identity sender = getForeignPeerIdentity(coapRequest);
        ClientProfile clientProfile = profileProvider.getProfile(sender);

        // check we have a registration for this identity
        if (clientProfile == null) {
            return errorMessage(ResponseCode.NOT_FOUND, "no registration found");
        }

        try {
            // Decode payload
            byte[] payload = coapRequest.getPayload().getBytes();
            ContentFormat contentFormat = ContentFormat.fromCode(coapRequest.options().getContentFormat());
            if (!decoder.isSupported(contentFormat)) {
                // TODO receiver call should maybe called after we send the response...
                receiver.onError(sender, clientProfile,
                        new InvalidRequestException("Unsupported content format [%s] in [%s] from [%s]", contentFormat,
                                coapRequest, sender),
                        SendRequest.class, endpointUsed);
                return errorMessage(ResponseCode.BAD_REQUEST, "Unsupported content format");
            }

            TimestampedLwM2mNodes data = decoder.decodeTimestampedNodes(payload, contentFormat,
                    clientProfile.getModel());

            // Handle "send op request
            SendRequest sendRequest = new SendRequest(contentFormat, data, coapRequest);
            SendableResponse<SendResponse> sendableResponse = receiver.requestReceived(sender, clientProfile,
                    sendRequest, endpointUsed);
            SendResponse response = sendableResponse.getResponse();

            // send response
            // TODO sent should be called after the response is sent ?
            sendableResponse.sent();
            if (response.isSuccess()) {
                return completedFuture(CoapResponse.of(ResponseCodeUtil.toCoapResponseCode(response.getCode())));
            } else {
                return errorMessage(response.getCode(), response.getErrorMessage());
            }
        } catch (CodecException e) {
            // TODO receiver call should maybe called after we send the response...
            receiver.onError(sender, clientProfile,
                    new InvalidRequestException(e, "Invalid payload in [%s] from [%s]", coapRequest, sender),
                    SendRequest.class, endpointUsed);
            return errorMessage(ResponseCode.BAD_REQUEST, "Invalid Payload");
        } catch (RuntimeException e) {
            receiver.onError(sender, clientProfile, e, SendRequest.class, endpointUsed);
            throw e;
        }
    }
}
