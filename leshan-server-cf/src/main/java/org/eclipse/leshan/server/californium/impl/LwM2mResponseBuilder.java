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

import static org.eclipse.leshan.core.californium.ResponseCodeUtil.fromCoapCode;

import org.eclipse.californium.core.coap.MediaTypeRegistry;
import org.eclipse.californium.core.coap.Request;
import org.eclipse.californium.core.coap.Response;
import org.eclipse.leshan.Link;
import org.eclipse.leshan.ResponseCode;
import org.eclipse.leshan.core.model.LwM2mModel;
import org.eclipse.leshan.core.node.LwM2mNode;
import org.eclipse.leshan.core.node.LwM2mPath;
import org.eclipse.leshan.core.node.codec.CodecException;
import org.eclipse.leshan.core.node.codec.LwM2mNodeDecoder;
import org.eclipse.leshan.core.observation.Observation;
import org.eclipse.leshan.core.request.BootstrapDeleteRequest;
import org.eclipse.leshan.core.request.BootstrapFinishRequest;
import org.eclipse.leshan.core.request.BootstrapWriteRequest;
import org.eclipse.leshan.core.request.ContentFormat;
import org.eclipse.leshan.core.request.CreateRequest;
import org.eclipse.leshan.core.request.DeleteRequest;
import org.eclipse.leshan.core.request.DiscoverRequest;
import org.eclipse.leshan.core.request.DownlinkRequestVisitor;
import org.eclipse.leshan.core.request.ExecuteRequest;
import org.eclipse.leshan.core.request.LwM2mRequest;
import org.eclipse.leshan.core.request.ObserveRequest;
import org.eclipse.leshan.core.request.ReadRequest;
import org.eclipse.leshan.core.request.WriteAttributesRequest;
import org.eclipse.leshan.core.request.WriteRequest;
import org.eclipse.leshan.core.request.exception.InvalidResponseException;
import org.eclipse.leshan.core.response.BootstrapDeleteResponse;
import org.eclipse.leshan.core.response.BootstrapFinishResponse;
import org.eclipse.leshan.core.response.BootstrapWriteResponse;
import org.eclipse.leshan.core.response.CreateResponse;
import org.eclipse.leshan.core.response.DeleteResponse;
import org.eclipse.leshan.core.response.DiscoverResponse;
import org.eclipse.leshan.core.response.ExecuteResponse;
import org.eclipse.leshan.core.response.LwM2mResponse;
import org.eclipse.leshan.core.response.ObserveResponse;
import org.eclipse.leshan.core.response.ReadResponse;
import org.eclipse.leshan.core.response.WriteAttributesResponse;
import org.eclipse.leshan.core.response.WriteResponse;
import org.eclipse.leshan.server.registration.Registration;
import org.eclipse.leshan.util.Hex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
        switch (coapResponse.getCode()) {
        case CONTENT:
            LwM2mNode content = decodeCoapResponse(request.getPath(), coapResponse, request,
                    registration.getEndpoint());
            lwM2mresponse = new ReadResponse(ResponseCode.CONTENT, content, null, coapResponse);
            break;
        case BAD_REQUEST:
        case UNAUTHORIZED:
        case NOT_FOUND:
        case METHOD_NOT_ALLOWED:
        case NOT_ACCEPTABLE:
        case INTERNAL_SERVER_ERROR:
            lwM2mresponse = new ReadResponse(fromCoapCode(coapResponse.getCode().value), null,
                    coapResponse.getPayloadString(), coapResponse);
            break;
        default:
            handleUnexpectedResponseCode(registration.getEndpoint(), request, coapResponse);
        }
    }

    @Override
    public void visit(DiscoverRequest request) {
        switch (coapResponse.getCode()) {
        case CONTENT:
            Link[] links = null;
            if (MediaTypeRegistry.APPLICATION_LINK_FORMAT != coapResponse.getOptions().getContentFormat()) {
                LOG.debug("Expected LWM2M Client [{}] to return application/link-format [{}] content but got [{}]",
                        registration.getEndpoint(), MediaTypeRegistry.APPLICATION_LINK_FORMAT,
                        coapResponse.getOptions().getContentFormat());
                links = new Link[] {}; // empty list
            } else {
                links = Link.parse(coapResponse.getPayload());
            }
            lwM2mresponse = new DiscoverResponse(ResponseCode.CONTENT, links, null, coapResponse);
            break;
        case BAD_REQUEST:
        case NOT_FOUND:
        case UNAUTHORIZED:
        case METHOD_NOT_ALLOWED:
        case INTERNAL_SERVER_ERROR:
            lwM2mresponse = new DiscoverResponse(fromCoapCode(coapResponse.getCode().value), null,
                    coapResponse.getPayloadString(), coapResponse);
            break;
        default:
            handleUnexpectedResponseCode(registration.getEndpoint(), request, coapResponse);
        }
    }

    @Override
    public void visit(WriteRequest request) {
        switch (coapResponse.getCode()) {
        case CHANGED:
            lwM2mresponse = new WriteResponse(ResponseCode.CHANGED, null, coapResponse);
            break;
        case BAD_REQUEST:
        case NOT_FOUND:
        case UNAUTHORIZED:
        case METHOD_NOT_ALLOWED:
        case UNSUPPORTED_CONTENT_FORMAT:
        case INTERNAL_SERVER_ERROR:
            lwM2mresponse = new WriteResponse(fromCoapCode(coapResponse.getCode().value),
                    coapResponse.getPayloadString(), coapResponse);
            break;
        default:
            handleUnexpectedResponseCode(registration.getEndpoint(), request, coapResponse);
        }
    }

    @Override
    public void visit(WriteAttributesRequest request) {
        switch (coapResponse.getCode()) {
        case CHANGED:
            lwM2mresponse = new WriteAttributesResponse(ResponseCode.CHANGED, null, coapResponse);
            break;
        case BAD_REQUEST:
        case NOT_FOUND:
        case UNAUTHORIZED:
        case METHOD_NOT_ALLOWED:
        case INTERNAL_SERVER_ERROR:
            lwM2mresponse = new WriteAttributesResponse(fromCoapCode(coapResponse.getCode().value),
                    coapResponse.getPayloadString(), coapResponse);
            break;
        default:
            handleUnexpectedResponseCode(registration.getEndpoint(), request, coapResponse);
        }
    }

    @Override
    public void visit(ExecuteRequest request) {
        switch (coapResponse.getCode()) {
        case CHANGED:
            lwM2mresponse = new ExecuteResponse(ResponseCode.CHANGED, null, coapResponse);
            break;
        case BAD_REQUEST:
        case UNAUTHORIZED:
        case NOT_FOUND:
        case METHOD_NOT_ALLOWED:
        case INTERNAL_SERVER_ERROR:
            lwM2mresponse = new ExecuteResponse(fromCoapCode(coapResponse.getCode().value),
                    coapResponse.getPayloadString(), coapResponse);
            break;
        default:
            handleUnexpectedResponseCode(registration.getEndpoint(), request, coapResponse);
        }

    }

    @Override
    public void visit(CreateRequest request) {
        switch (coapResponse.getCode()) {
        case CREATED:
            lwM2mresponse = new CreateResponse(ResponseCode.CREATED, coapResponse.getOptions().getLocationPathString(),
                    null, coapResponse);
            break;
        case BAD_REQUEST:
        case UNAUTHORIZED:
        case NOT_FOUND:
        case METHOD_NOT_ALLOWED:
        case UNSUPPORTED_CONTENT_FORMAT:
        case INTERNAL_SERVER_ERROR:
            lwM2mresponse = new CreateResponse(fromCoapCode(coapResponse.getCode().value), null,
                    coapResponse.getPayloadString(), coapResponse);
            break;
        default:
            handleUnexpectedResponseCode(registration.getEndpoint(), request, coapResponse);
        }
    }

    @Override
    public void visit(DeleteRequest request) {
        switch (coapResponse.getCode()) {
        case DELETED:
            lwM2mresponse = new DeleteResponse(ResponseCode.DELETED, null, coapResponse);
            break;
        case UNAUTHORIZED:
        case NOT_FOUND:
        case METHOD_NOT_ALLOWED:
        case BAD_REQUEST:
        case INTERNAL_SERVER_ERROR:
            lwM2mresponse = new DeleteResponse(fromCoapCode(coapResponse.getCode().value),
                    coapResponse.getPayloadString(), coapResponse);
            break;
        default:
            handleUnexpectedResponseCode(registration.getEndpoint(), request, coapResponse);
        }
    }

    @Override
    public void visit(ObserveRequest request) {
        switch (coapResponse.getCode()) {
        case CHANGED:
            // TODO now the spec say that NOTIFY should use 2.05 content so we should remove this.
            // ignore changed response (this is probably a NOTIFY)
            lwM2mresponse = null;
            break;
        case CONTENT:
            LwM2mNode content = decodeCoapResponse(request.getPath(), coapResponse, request,
                    registration.getEndpoint());
            if (coapResponse.getOptions().hasObserve()) {
                // observe request successful
                Observation observation = new Observation(coapRequest.getToken(), registration.getId(),
                        request.getPath(), request.getContext());
                observationService.addObservation(registration, observation);
                // add the observation to an ObserveResponse instance
                lwM2mresponse = new ObserveResponse(ResponseCode.CONTENT, content, null, observation, null,
                        coapResponse);
            } else {
                lwM2mresponse = new ObserveResponse(ResponseCode.CONTENT, content, null, null, null, coapResponse);
            }
            break;
        case BAD_REQUEST:
        case UNAUTHORIZED:
        case NOT_FOUND:
        case METHOD_NOT_ALLOWED:
        case NOT_ACCEPTABLE:
        case INTERNAL_SERVER_ERROR:
            lwM2mresponse = new ObserveResponse(fromCoapCode(coapResponse.getCode().value), null, null, null,
                    coapResponse.getPayloadString(), coapResponse);
            break;
        default:
            handleUnexpectedResponseCode(registration.getEndpoint(), request, coapResponse);
        }
    }

    @Override
    public void visit(BootstrapWriteRequest request) {
        switch (coapResponse.getCode()) {
        case CHANGED:
            lwM2mresponse = new BootstrapWriteResponse(ResponseCode.CHANGED, null, coapResponse);
            break;
        case UNSUPPORTED_CONTENT_FORMAT:
        case BAD_REQUEST:
        case INTERNAL_SERVER_ERROR:
            lwM2mresponse = new BootstrapWriteResponse(fromCoapCode(coapResponse.getCode().value),
                    coapResponse.getPayloadString(), coapResponse);
            break;
        default:
            handleUnexpectedResponseCode(registration.getEndpoint(), request, coapResponse);
        }
    }

    @Override
    public void visit(BootstrapDeleteRequest request) {
        switch (coapResponse.getCode()) {
        case DELETED:
            lwM2mresponse = new BootstrapDeleteResponse(ResponseCode.DELETED, null, coapResponse);
            break;
        case BAD_REQUEST:
        case INTERNAL_SERVER_ERROR:
            lwM2mresponse = new BootstrapDeleteResponse(fromCoapCode(coapResponse.getCode().value),
                    coapResponse.getPayloadString(), coapResponse);
            break;
        default:
            handleUnexpectedResponseCode(registration.getEndpoint(), request, coapResponse);
        }
    }

    @Override
    public void visit(BootstrapFinishRequest request) {
        switch (coapResponse.getCode()) {
        case CHANGED:
            lwM2mresponse = new BootstrapFinishResponse(ResponseCode.CHANGED, null, coapResponse);
            break;
        case BAD_REQUEST:
        case INTERNAL_SERVER_ERROR:
            lwM2mresponse = new BootstrapFinishResponse(fromCoapCode(coapResponse.getCode().value),
                    coapResponse.getPayloadString(), coapResponse);
            break;
        default:
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

    @SuppressWarnings("unchecked")
    public T getResponse() {
        return (T) lwM2mresponse;
    }

    private void handleUnexpectedResponseCode(String clientEndpoint, LwM2mRequest<?> request, Response coapResponse) {
        throw new InvalidResponseException("Client [%s] returned unexpected response code [%s] for [%s]",
                clientEndpoint, coapResponse.getCode(), request);
    }
}
