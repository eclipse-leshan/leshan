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

import static org.eclipse.leshan.core.californium.ResponseCodeUtil.toLwM2mResponseCode;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.eclipse.californium.core.coap.CoAP;
import org.eclipse.californium.core.coap.Request;
import org.eclipse.californium.core.coap.Response;
import org.eclipse.californium.core.server.resources.Resource;
import org.eclipse.leshan.core.ResponseCode;
import org.eclipse.leshan.core.californium.identity.IdentityHandler;
import org.eclipse.leshan.core.californium.identity.IdentityHandlerProvider;
import org.eclipse.leshan.core.node.LwM2mNode;
import org.eclipse.leshan.core.node.LwM2mPath;
import org.eclipse.leshan.core.node.TimestampedLwM2mNode;
import org.eclipse.leshan.core.node.codec.CodecException;
import org.eclipse.leshan.core.observation.CompositeObservation;
import org.eclipse.leshan.core.observation.Observation;
import org.eclipse.leshan.core.observation.SingleObservation;
import org.eclipse.leshan.core.request.ContentFormat;
import org.eclipse.leshan.core.request.DownlinkRequest;
import org.eclipse.leshan.core.request.exception.InvalidResponseException;
import org.eclipse.leshan.core.response.AbstractLwM2mResponse;
import org.eclipse.leshan.core.response.LwM2mResponse;
import org.eclipse.leshan.core.response.ObserveCompositeResponse;
import org.eclipse.leshan.core.response.ObserveResponse;
import org.eclipse.leshan.core.util.Hex;
import org.eclipse.leshan.server.californium.registration.RegisterResource;
import org.eclipse.leshan.server.californium.request.CoapRequestBuilder;
import org.eclipse.leshan.server.californium.request.LwM2mResponseBuilder;
import org.eclipse.leshan.server.californium.send.SendResource;
import org.eclipse.leshan.server.endpoint.ServerEndpointToolbox;
import org.eclipse.leshan.server.profile.ClientProfile;
import org.eclipse.leshan.server.request.UplinkRequestReceiver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ServerCoapMessageTranslator {

    private final Logger LOG = LoggerFactory.getLogger(ServerCoapMessageTranslator.class);

    public Request createCoapRequest(ClientProfile clientProfile, DownlinkRequest<? extends LwM2mResponse> lwm2mRequest,
            ServerEndpointToolbox toolbox, IdentityHandler identityHandler) {
        CoapRequestBuilder builder = new CoapRequestBuilder(clientProfile.getIdentity(), clientProfile.getRootPath(),
                clientProfile.getRegistrationId(), clientProfile.getEndpoint(), clientProfile.getModel(),
                toolbox.getEncoder(), clientProfile.canInitiateConnection(), null, identityHandler);
        lwm2mRequest.accept(builder);
        return builder.getRequest();
    }

    public <T extends LwM2mResponse> T createLwM2mResponse(ClientProfile clientProfile, DownlinkRequest<T> lwm2mRequest,
            Request coapRequest, Response coapResponse, ServerEndpointToolbox toolbox) {

        LwM2mResponseBuilder<T> builder = new LwM2mResponseBuilder<T>(coapRequest, coapResponse,
                clientProfile.getEndpoint(), clientProfile.getModel(), toolbox.getDecoder(), toolbox.getLinkParser());
        lwm2mRequest.accept(builder);
        return builder.getResponse();
    }

    public List<Resource> createResources(UplinkRequestReceiver receiver, ServerEndpointToolbox toolbox,
            IdentityHandlerProvider identityHandlerProvider) {
        return Arrays.asList( //
                (Resource) new RegisterResource(receiver, toolbox.getLinkParser(), identityHandlerProvider), //
                (Resource) new SendResource(receiver, toolbox.getDecoder(), toolbox.getProfileProvider(),
                        identityHandlerProvider));
    }

    public AbstractLwM2mResponse createObservation(Observation observation, Response coapResponse,
            ServerEndpointToolbox toolbox, ClientProfile profile) {
        // CHANGED response is supported for backward compatibility with old spec.
        if (coapResponse.getCode() != CoAP.ResponseCode.CHANGED
                && coapResponse.getCode() != CoAP.ResponseCode.CONTENT) {
            throw new InvalidResponseException("Unexpected response code [%s] for %s", coapResponse.getCode(),
                    observation);
        }

        // get content format
        ContentFormat contentFormat = null;
        if (coapResponse.getOptions().hasContentFormat()) {
            contentFormat = ContentFormat.fromCode(coapResponse.getOptions().getContentFormat());
        }

        // decode response
        try {
            ResponseCode responseCode = toLwM2mResponseCode(coapResponse.getCode());

            if (observation instanceof SingleObservation) {
                SingleObservation singleObservation = (SingleObservation) observation;

                List<TimestampedLwM2mNode> timestampedNodes = toolbox.getDecoder().decodeTimestampedData(
                        coapResponse.getPayload(), contentFormat, singleObservation.getPath(), profile.getModel());

                // create lwm2m response
                if (timestampedNodes.size() == 1 && !timestampedNodes.get(0).isTimestamped()) {
                    return new ObserveResponse(responseCode, timestampedNodes.get(0).getNode(), null, singleObservation,
                            null, coapResponse);
                } else {
                    return new ObserveResponse(responseCode, null, timestampedNodes, singleObservation, null,
                            coapResponse);
                }
            } else if (observation instanceof CompositeObservation) {

                CompositeObservation compositeObservation = (CompositeObservation) observation;

                Map<LwM2mPath, LwM2mNode> nodes = toolbox.getDecoder().decodeNodes(coapResponse.getPayload(),
                        contentFormat, compositeObservation.getPaths(), profile.getModel());

                return new ObserveCompositeResponse(responseCode, nodes, null, coapResponse, compositeObservation);
            }

            throw new IllegalStateException(
                    "observation must be a CompositeObservation or a SingleObservation but was " + observation == null
                            ? null
                            : observation.getClass().getSimpleName());
        } catch (CodecException e) {
            if (LOG.isDebugEnabled()) {
                byte[] payload = coapResponse.getPayload() == null ? new byte[0] : coapResponse.getPayload();
                LOG.debug(String.format("Unable to decode notification payload [%s] of observation [%s] ",
                        Hex.encodeHexString(payload), observation), e);
            }
            throw new InvalidResponseException(e, "Unable to decode notification payload  of observation [%s] ",
                    observation);
        }
    }
}
