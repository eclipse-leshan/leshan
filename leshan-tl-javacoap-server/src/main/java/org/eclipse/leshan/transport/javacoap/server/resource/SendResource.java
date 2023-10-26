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
package org.eclipse.leshan.transport.javacoap.server.resource;

import static java.util.concurrent.CompletableFuture.completedFuture;

import java.util.concurrent.CompletableFuture;

import org.eclipse.leshan.core.ResponseCode;
import org.eclipse.leshan.core.node.TimestampedLwM2mNodes;
import org.eclipse.leshan.core.node.codec.CodecException;
import org.eclipse.leshan.core.node.codec.LwM2mDecoder;
import org.eclipse.leshan.core.peer.LwM2mPeer;
import org.eclipse.leshan.core.request.ContentFormat;
import org.eclipse.leshan.core.request.SendRequest;
import org.eclipse.leshan.core.request.exception.InvalidRequestException;
import org.eclipse.leshan.core.response.SendResponse;
import org.eclipse.leshan.core.response.SendableResponse;
import org.eclipse.leshan.server.profile.ClientProfile;
import org.eclipse.leshan.server.profile.ClientProfileProvider;
import org.eclipse.leshan.server.request.UplinkRequestReceiver;
import org.eclipse.leshan.transport.javacoap.identity.IdentityHandler;
import org.eclipse.leshan.transport.javacoap.request.ResponseCodeUtil;
import org.eclipse.leshan.transport.javacoap.resource.LwM2mCoapResource;
import org.eclipse.leshan.transport.javacoap.server.endpoint.EndpointUriProvider;

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

    private final EndpointUriProvider endpointUriProvider;

    public SendResource(UplinkRequestReceiver receiver, LwM2mDecoder decoder, ClientProfileProvider profileProvider,
            EndpointUriProvider endpointUriProvider, IdentityHandler identityHandler) {
        super(RESOURCE_URI, identityHandler);
        this.decoder = decoder;
        this.receiver = receiver;
        this.profileProvider = profileProvider;
        this.endpointUriProvider = endpointUriProvider;
    }

    @Override
    public CompletableFuture<CoapResponse> handlePOST(CoapRequest coapRequest) {
        LwM2mPeer sender = getForeignPeerIdentity(coapRequest);
        ClientProfile clientProfile = profileProvider.getProfile(sender.getIdentity());

        // check we have a registration for this identity
        if (clientProfile == null) {
            return errorMessage(ResponseCode.BAD_REQUEST, "no registration found");
        }

        try {
            // Decode payload
            byte[] payload = coapRequest.getPayload().getBytes();
            ContentFormat contentFormat = ContentFormat.fromCode(coapRequest.options().getContentFormat());
            if (!decoder.isSupported(contentFormat)) {
                // TODO receiver call should maybe called after we send the response...
                // (not sure there is a way to do that)
                receiver.onError(sender, clientProfile,
                        new InvalidRequestException("Unsupported content format [%s] in [%s] from [%s]", contentFormat,
                                coapRequest, sender),
                        SendRequest.class, endpointUriProvider.getEndpointUri());
                return errorMessage(ResponseCode.BAD_REQUEST, "Unsupported content format");
            }

            TimestampedLwM2mNodes data = decoder.decodeTimestampedNodes(payload, contentFormat, null,
                    clientProfile.getModel());

            // Handle "send op request
            SendRequest sendRequest = new SendRequest(contentFormat, data, coapRequest);
            SendableResponse<SendResponse> sendableResponse = receiver.requestReceived(sender, clientProfile,
                    sendRequest, endpointUriProvider.getEndpointUri());
            SendResponse response = sendableResponse.getResponse();

            // send response
            // TODO this should be called once request is sent. (No java-coap API for this)
            sendableResponse.sent();
            if (response.isSuccess()) {
                return completedFuture(CoapResponse.of(ResponseCodeUtil.toCoapResponseCode(response.getCode())));
            } else {
                return errorMessage(response.getCode(), response.getErrorMessage());
            }
        } catch (CodecException e) {
            // TODO receiver call should maybe called after we send the response...
            // (not sure there is a way to do that)
            receiver.onError(sender, clientProfile,
                    new InvalidRequestException(e, "Invalid payload in [%s] from [%s]", coapRequest, sender),
                    SendRequest.class, endpointUriProvider.getEndpointUri());
            return errorMessage(ResponseCode.BAD_REQUEST, "Invalid Payload");
        } catch (RuntimeException e) {
            receiver.onError(sender, clientProfile, e, SendRequest.class, endpointUriProvider.getEndpointUri());
            throw e;
        }
    }
}
