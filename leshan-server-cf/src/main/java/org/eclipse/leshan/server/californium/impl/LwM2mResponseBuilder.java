/*******************************************************************************
 * Copyright (c) 2013-2015 Sierra Wireless and others.
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
 *******************************************************************************/
package org.eclipse.leshan.server.californium.impl;

import org.eclipse.californium.core.coap.MediaTypeRegistry;
import org.eclipse.californium.core.coap.Request;
import org.eclipse.californium.core.coap.Response;
import org.eclipse.leshan.Link;
import org.eclipse.leshan.ResponseCode;
import org.eclipse.leshan.core.model.LwM2mModel;
import org.eclipse.leshan.core.node.LwM2mNode;
import org.eclipse.leshan.core.node.LwM2mPath;
import org.eclipse.leshan.core.node.LwM2mSingleResource;
import org.eclipse.leshan.core.node.codec.CodecException;
import org.eclipse.leshan.core.node.codec.LwM2mNodeDecoder;
import org.eclipse.leshan.core.observation.Observation;
import org.eclipse.leshan.core.request.*;
import org.eclipse.leshan.core.request.exception.InvalidResponseException;
import org.eclipse.leshan.core.response.*;
import org.eclipse.leshan.server.californium.ObserveUtil;
import org.eclipse.leshan.server.registration.Registration;
import org.eclipse.leshan.util.Hex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.eclipse.leshan.core.californium.ResponseCodeUtil.toLwM2mResponseCode;

public class LwM2mResponseBuilder<T extends LwM2mResponse> implements DownlinkRequestVisitor {

    private static final Logger LOG = LoggerFactory.getLogger(LwM2mResponseBuilder.class);

    private LwM2mResponse lwM2mresponse;
    private final Request coapRequest;
    private final Response coapResponse;
    private final ObservationServiceImpl observationService;
    private final Registration registration;
    private final LwM2mModel model;
    private final LwM2mNodeDecoder decoder;

    public LwM2mResponseBuilder(Request coapRequest, Response coapResponse, Registration registration, LwM2mModel model,
            ObservationServiceImpl observationService, LwM2mNodeDecoder decoder) {
        this.coapRequest = coapRequest;
        this.coapResponse = coapResponse;
        this.observationService = observationService;
        this.registration = registration;
        this.model = model;
        this.decoder = decoder;
    }

    @Override
    public void visit(ReadRequest request) {
        if (coapResponse.isError()) {
            // handle error response:
            lwM2mresponse = new ReadResponse(toLwM2mResponseCode(coapResponse.getCode()), null,
                    coapResponse.getPayloadString(), coapResponse);
        } else if (coapResponse.getCode() == org.eclipse.californium.core.coap.CoAP.ResponseCode.CONTENT) {
            // handle success response:
            LwM2mNode content = decodeCoapResponse(request.getPath(), coapResponse, request,
                    registration.getEndpoint());
            lwM2mresponse = new ReadResponse(ResponseCode.CONTENT, content, null, coapResponse);
        } else {
            // handle unexpected response:
            handleUnexpectedResponseCode(registration.getEndpoint(), request, coapResponse);
        }
    }

    @Override
    public void visit(DiscoverRequest request) {
        if (coapResponse.isError()) {
            // handle error response:
            lwM2mresponse = new DiscoverResponse(toLwM2mResponseCode(coapResponse.getCode()), null,
                    coapResponse.getPayloadString(), coapResponse);
        } else if (coapResponse.getCode() == org.eclipse.californium.core.coap.CoAP.ResponseCode.CONTENT) {
            // handle success response:
            Link[] links;
            if (MediaTypeRegistry.APPLICATION_LINK_FORMAT != coapResponse.getOptions().getContentFormat()) {
                LOG.debug("Expected LWM2M Client [{}] to return application/link-format [{}] content but got [{}]",
                        registration.getEndpoint(), MediaTypeRegistry.APPLICATION_LINK_FORMAT,
                        coapResponse.getOptions().getContentFormat());
                links = new Link[] {}; // empty list
            } else {
                links = Link.parse(coapResponse.getPayload());
            }
            lwM2mresponse = new DiscoverResponse(ResponseCode.CONTENT, links, null, coapResponse);
        } else {
            // handle unexpected response:
            handleUnexpectedResponseCode(registration.getEndpoint(), request, coapResponse);
        }
    }

    @Override
    public void visit(WriteRequest request) {
        if (coapResponse.isError()) {
            // handle error response:
            lwM2mresponse = new WriteResponse(toLwM2mResponseCode(coapResponse.getCode()),
                    coapResponse.getPayloadString(), coapResponse);
        } else if (coapResponse.getCode() == org.eclipse.californium.core.coap.CoAP.ResponseCode.CHANGED) {
            // handle success response:
            lwM2mresponse = new WriteResponse(ResponseCode.CHANGED, null, coapResponse);
        } else {
            // handle unexpected response:
            handleUnexpectedResponseCode(registration.getEndpoint(), request, coapResponse);
        }
    }

    @Override
    public void visit(WriteAttributesRequest request) {
        if (coapResponse.isError()) {
            // handle error response:
            lwM2mresponse = new WriteAttributesResponse(toLwM2mResponseCode(coapResponse.getCode()),
                    coapResponse.getPayloadString(), coapResponse);
        } else if (coapResponse.getCode() == org.eclipse.californium.core.coap.CoAP.ResponseCode.CHANGED) {
            // handle success response:
            lwM2mresponse = new WriteAttributesResponse(ResponseCode.CHANGED, null, coapResponse);
        } else {
            // handle unexpected response:
            handleUnexpectedResponseCode(registration.getEndpoint(), request, coapResponse);
        }
    }

    @Override
    public void visit(ExecuteRequest request) {
        if (coapResponse.isError()) {
            // handle error response:
            lwM2mresponse = new ExecuteResponse(toLwM2mResponseCode(coapResponse.getCode()),
                    coapResponse.getPayloadString(), coapResponse);
        } else if (coapResponse.getCode() == org.eclipse.californium.core.coap.CoAP.ResponseCode.CHANGED) {
            // handle success response:
            lwM2mresponse = new ExecuteResponse(ResponseCode.CHANGED, null, coapResponse);
        } else {
            // handle unexpected response:
            handleUnexpectedResponseCode(registration.getEndpoint(), request, coapResponse);
        }
    }

    @Override
    public void visit(CreateRequest request) {
        if (coapResponse.isError()) {
            // handle error response:
            lwM2mresponse = new CreateResponse(toLwM2mResponseCode(coapResponse.getCode()), null,
                    coapResponse.getPayloadString(), coapResponse);
        } else if (coapResponse.getCode() == org.eclipse.californium.core.coap.CoAP.ResponseCode.CREATED) {
            // handle success response:
            lwM2mresponse = new CreateResponse(ResponseCode.CREATED, coapResponse.getOptions().getLocationPathString(),
                    null, coapResponse);
        } else {
            // handle unexpected response:
            handleUnexpectedResponseCode(registration.getEndpoint(), request, coapResponse);
        }
    }

    @Override
    public void visit(DeleteRequest request) {
        if (coapResponse.isError()) {
            // handle error response:
            lwM2mresponse = new DeleteResponse(toLwM2mResponseCode(coapResponse.getCode()),
                    coapResponse.getPayloadString(), coapResponse);
        } else if (coapResponse.getCode() == org.eclipse.californium.core.coap.CoAP.ResponseCode.DELETED) {
            // handle success response:
            lwM2mresponse = new DeleteResponse(ResponseCode.DELETED, null, coapResponse);
        } else {
            // handle unexpected response:
            handleUnexpectedResponseCode(registration.getEndpoint(), request, coapResponse);
        }
    }

    @Override
    public void visit(ObserveRequest request) {
        if (coapResponse.isError()) {
            // handle error response:
            lwM2mresponse = new ObserveResponse(toLwM2mResponseCode(coapResponse.getCode()), null, null, null,
                    coapResponse.getPayloadString(), coapResponse);
        } else if (coapResponse.getCode() == org.eclipse.californium.core.coap.CoAP.ResponseCode.CONTENT
                // This is for backward compatibility, when the spec say notification used CHANGED code
                || coapResponse.getCode() == org.eclipse.californium.core.coap.CoAP.ResponseCode.CHANGED) {
            // handle success response:
            LwM2mNode content = decodeOneNetResponse(request.getPath(), coapResponse, request,
                    registration.getEndpoint());
            if (coapResponse.getOptions().hasObserve()) {
                // observe request successful
                Observation observation = ObserveUtil.createLwM2mObservation(coapRequest);
                observationService.addObservation(registration, observation);
                // add the observation to an ObserveResponse instance
                lwM2mresponse = new ObserveResponse(toLwM2mResponseCode(coapResponse.getCode()), content, null,
                        observation, null, coapResponse);
            } else {
                lwM2mresponse = new ObserveResponse(toLwM2mResponseCode(coapResponse.getCode()), content, null, null,
                        null, coapResponse);
            }
        } else {
            // handle unexpected response:
            handleUnexpectedResponseCode(registration.getEndpoint(), request, coapResponse);
        }
    }

    @Override
    public void visit(BootstrapWriteRequest request) {
        if (coapResponse.isError()) {
            // handle error response:
            lwM2mresponse = new BootstrapWriteResponse(toLwM2mResponseCode(coapResponse.getCode()),
                    coapResponse.getPayloadString(), coapResponse);
        } else if (coapResponse.getCode() == org.eclipse.californium.core.coap.CoAP.ResponseCode.CHANGED) {
            // handle success response:
            lwM2mresponse = new BootstrapWriteResponse(ResponseCode.CHANGED, null, coapResponse);
        } else {
            // handle unexpected response:
            handleUnexpectedResponseCode(registration.getEndpoint(), request, coapResponse);
        }
    }

    @Override
    public void visit(BootstrapDeleteRequest request) {
        if (coapResponse.isError()) {
            // handle error response:
            lwM2mresponse = new BootstrapDeleteResponse(toLwM2mResponseCode(coapResponse.getCode()),
                    coapResponse.getPayloadString(), coapResponse);
        } else if (coapResponse.getCode() == org.eclipse.californium.core.coap.CoAP.ResponseCode.DELETED) {
            // handle success response:
            lwM2mresponse = new BootstrapDeleteResponse(ResponseCode.DELETED, null, coapResponse);
        } else {
            // handle unexpected response:
            handleUnexpectedResponseCode(registration.getEndpoint(), request, coapResponse);
        }
    }

    @Override
    public void visit(BootstrapFinishRequest request) {
        if (coapResponse.isError()) {
            // handle error response:
            lwM2mresponse = new BootstrapFinishResponse(toLwM2mResponseCode(coapResponse.getCode()),
                    coapResponse.getPayloadString(), coapResponse);
        } else if (coapResponse.getCode() == org.eclipse.californium.core.coap.CoAP.ResponseCode.CHANGED) {
            // handle success response:
            lwM2mresponse = new BootstrapFinishResponse(ResponseCode.CHANGED, null, coapResponse);
        } else {
            // handle unexpected response:
            handleUnexpectedResponseCode(registration.getEndpoint(), request, coapResponse);
        }
    }

    private LwM2mNode decodeCoapResponse(LwM2mPath path, Response coapResponse, LwM2mRequest<?> request,
            String endpoint) {

        // Get content format
        ContentFormat contentFormat = null;
        if (coapResponse.getOptions().hasContentFormat()) {
            contentFormat = ContentFormat.fromCode(coapResponse.getOptions().getContentFormat());
        }

        // Decode payload
        try {
            return decoder.decode(coapResponse.getPayload(), contentFormat, path, model);
        } catch (CodecException e) {
            if (LOG.isDebugEnabled()) {
                byte[] payload = coapResponse.getPayload() == null ? new byte[0] : coapResponse.getPayload();
                LOG.debug(
                        String.format("Unable to decode response payload of request [%s] from client [%s] [payload:%s]",
                                request, endpoint, Hex.encodeHexString(payload)));
            }
            throw new InvalidResponseException(e, "Unable to decode response payload of request [%s] from client [%s]",
                    request, endpoint);
        }
    }

    /**
     * hide the defect of oneNet protocol（violation of CoAP 2.05 content and LwM2M Observe）
     */
    private LwM2mNode decodeOneNetResponse(LwM2mPath path, Response coapResponse, LwM2mRequest<?> request,
                                           String endpoint) {

        // Get content format
        ContentFormat contentFormat = null;
        if (coapResponse.getOptions().hasContentFormat()) {
            contentFormat = ContentFormat.fromCode(coapResponse.getOptions().getContentFormat());
        }else {
            if (path != null) {
                return LwM2mSingleResource.newStringResource(path.getResourceId(), "");
            } else {
                return LwM2mSingleResource.newStringResource(2, "");
            }
        }

        // Decode payload
        try {
            return decoder.decode(coapResponse.getPayload(), contentFormat, path, model);
        } catch (CodecException e) {
            if (LOG.isDebugEnabled()) {
                byte[] payload = coapResponse.getPayload() == null ? new byte[0] : coapResponse.getPayload();
                LOG.debug(
                        String.format("Unable to decode response payload of request [%s] from client [%s] [payload:%s]",
                                request, endpoint, Hex.encodeHexString(payload)));
            }
            throw new InvalidResponseException(e, "Unable to decode response payload of request [%s] from client [%s]",
                    request, endpoint);
        }
    }

    @SuppressWarnings("unchecked")
    public T getResponse() {
        return (T) lwM2mresponse;
    }

    private void handleUnexpectedResponseCode(String clientEndpoint, LwM2mRequest<?> request, Response coapResponse) {
        throw new InvalidResponseException("Client [%s] returned unexpected response code [%s] for [%s]",
                clientEndpoint, coapResponse.getCode(), request);
    }
}
