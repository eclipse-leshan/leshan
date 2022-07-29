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
import org.eclipse.leshan.core.node.LwM2mNode;
import org.eclipse.leshan.core.node.LwM2mPath;
import org.eclipse.leshan.core.node.TimestampedLwM2mNode;
import org.eclipse.leshan.core.node.codec.CodecException;
import org.eclipse.leshan.core.observation.CompositeObservation;
import org.eclipse.leshan.core.observation.Observation;
import org.eclipse.leshan.core.observation.SingleObservation;
import org.eclipse.leshan.core.request.ContentFormat;
import org.eclipse.leshan.core.request.DownlinkRequest;
import org.eclipse.leshan.core.request.LwM2mRequest;
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
import org.eclipse.leshan.server.endpoint.ClientProfile;
import org.eclipse.leshan.server.endpoint.LwM2mEndpointToolbox;
import org.eclipse.leshan.server.endpoint.LwM2mRequestReceiver;
import org.eclipse.leshan.server.endpoint.PeerProfile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ServerCoapMessageTranslator implements CoapMessageTranslator {

    private final Logger LOG = LoggerFactory.getLogger(ServerCoapMessageTranslator.class);

    @Override
    public Request createCoapRequest(PeerProfile foreignPeerProfile, LwM2mRequest<? extends LwM2mResponse> lwm2mRequest,
            LwM2mEndpointToolbox toolbox) {
        // check we get expected inputs
        ClientProfile clientProfile = assertIsClientProfile(foreignPeerProfile);
        DownlinkRequest<? extends LwM2mResponse> downlinkRequest = assertIsDownlinkRequest(lwm2mRequest);

        // create CoAP Request
        CoapRequestBuilder builder = new org.eclipse.leshan.server.californium.request.CoapRequestBuilder(
                clientProfile.getIdentity(), clientProfile.getRootPath(), clientProfile.getSessionID(),
                clientProfile.getEndpoint(), clientProfile.getModel(), toolbox.getEncoder(),
                clientProfile.canInitiateConnection(), null);
        downlinkRequest.accept(builder);
        return builder.getRequest();
    }

    @Override
    public <T extends LwM2mResponse> T createLwM2mResponse(PeerProfile foreignPeerProfile, LwM2mRequest<T> lwm2mRequest,
            Request coapRequest, Response coapResponse, LwM2mEndpointToolbox toolbox) {
        // check we get expected inputs
        ClientProfile clientProfile = assertIsClientProfile(foreignPeerProfile);
        DownlinkRequest<? extends LwM2mResponse> downlinkRequest = assertIsDownlinkRequest(lwm2mRequest);

        // create LWM2M Response
        LwM2mResponseBuilder<T> builder = new LwM2mResponseBuilder<T>(coapRequest, coapResponse,
                clientProfile.getEndpoint(), clientProfile.getModel(), toolbox.getDecoder(), toolbox.getLinkParser());
        downlinkRequest.accept(builder);
        return builder.getResponse();
    }

    private ClientProfile assertIsClientProfile(PeerProfile foreignPeerProfile) {
        if (!(foreignPeerProfile instanceof ClientProfile)) {
            throw new IllegalStateException(
                    String.format("Unable to handle %s, LWM2M server only support ClientProfile",
                            foreignPeerProfile.getClass().getSimpleName()));
        }

        return (ClientProfile) foreignPeerProfile;
    }

    private DownlinkRequest<? extends LwM2mResponse> assertIsDownlinkRequest(
            LwM2mRequest<? extends LwM2mResponse> lwm2mRequest) {
        if (!(lwm2mRequest instanceof DownlinkRequest<?>)) {
            throw new IllegalStateException(
                    String.format("Unable to handle %s, LWM2M server only support DownlinkRequest",
                            lwm2mRequest.getClass().getSimpleName()));
        }

        return (DownlinkRequest<? extends LwM2mResponse>) lwm2mRequest;
    }

    @Override
    public List<Resource> createResources(LwM2mRequestReceiver receiver, LwM2mEndpointToolbox toolbox) {
        return Arrays.asList( //
                (Resource) new RegisterResource(receiver, toolbox.getLinkParser()), //
                (Resource) new SendResource(receiver, toolbox.getDecoder(), toolbox.getProfileProvider()));
    }

    @Override
    public AbstractLwM2mResponse createObservation(Observation observation, Response coapResponse,
            LwM2mEndpointToolbox toolbox, ClientProfile profile) {
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
