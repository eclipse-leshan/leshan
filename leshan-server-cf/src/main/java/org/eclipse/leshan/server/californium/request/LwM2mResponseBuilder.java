/*******************************************************************************
 * Copyright (c) 2013-2015 Sierra Wireless and others.
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
 *     Michał Wadowski (Orange) - Add Observe-Composite feature.
 *     Michał Wadowski (Orange) - Add Cancel Composite-Observation feature.
 *     Michał Wadowski (Orange) - Improved compliance with rfc6690.
 *     Rikard Höglund (RISE SICS) - Additions to support OSCORE
 *******************************************************************************/
package org.eclipse.leshan.server.californium.request;

import static org.eclipse.leshan.core.californium.ResponseCodeUtil.toLwM2mResponseCode;

import java.util.List;
import java.util.Map;

import org.eclipse.californium.core.coap.MediaTypeRegistry;
import org.eclipse.californium.core.coap.Request;
import org.eclipse.californium.core.coap.Response;
import org.eclipse.leshan.core.ResponseCode;
import org.eclipse.leshan.core.californium.ObserveUtil;
import org.eclipse.leshan.core.link.LinkParseException;
import org.eclipse.leshan.core.link.lwm2m.LwM2mLink;
import org.eclipse.leshan.core.link.lwm2m.LwM2mLinkParser;
import org.eclipse.leshan.core.model.LwM2mModel;
import org.eclipse.leshan.core.node.LwM2mNode;
import org.eclipse.leshan.core.node.LwM2mPath;
import org.eclipse.leshan.core.node.codec.CodecException;
import org.eclipse.leshan.core.node.codec.LwM2mDecoder;
import org.eclipse.leshan.core.observation.CompositeObservation;
import org.eclipse.leshan.core.observation.SingleObservation;
import org.eclipse.leshan.core.request.BootstrapDeleteRequest;
import org.eclipse.leshan.core.request.BootstrapDiscoverRequest;
import org.eclipse.leshan.core.request.BootstrapFinishRequest;
import org.eclipse.leshan.core.request.BootstrapReadRequest;
import org.eclipse.leshan.core.request.BootstrapWriteRequest;
import org.eclipse.leshan.core.request.CancelCompositeObservationRequest;
import org.eclipse.leshan.core.request.CancelObservationRequest;
import org.eclipse.leshan.core.request.ContentFormat;
import org.eclipse.leshan.core.request.CreateRequest;
import org.eclipse.leshan.core.request.DeleteRequest;
import org.eclipse.leshan.core.request.DiscoverRequest;
import org.eclipse.leshan.core.request.DownlinkRequestVisitor;
import org.eclipse.leshan.core.request.ExecuteRequest;
import org.eclipse.leshan.core.request.LwM2mRequest;
import org.eclipse.leshan.core.request.ObserveCompositeRequest;
import org.eclipse.leshan.core.request.ObserveRequest;
import org.eclipse.leshan.core.request.ReadCompositeRequest;
import org.eclipse.leshan.core.request.ReadRequest;
import org.eclipse.leshan.core.request.WriteAttributesRequest;
import org.eclipse.leshan.core.request.WriteCompositeRequest;
import org.eclipse.leshan.core.request.WriteRequest;
import org.eclipse.leshan.core.request.exception.InvalidResponseException;
import org.eclipse.leshan.core.response.BootstrapDeleteResponse;
import org.eclipse.leshan.core.response.BootstrapDiscoverResponse;
import org.eclipse.leshan.core.response.BootstrapFinishResponse;
import org.eclipse.leshan.core.response.BootstrapReadResponse;
import org.eclipse.leshan.core.response.BootstrapWriteResponse;
import org.eclipse.leshan.core.response.CancelCompositeObservationResponse;
import org.eclipse.leshan.core.response.CancelObservationResponse;
import org.eclipse.leshan.core.response.CreateResponse;
import org.eclipse.leshan.core.response.DeleteResponse;
import org.eclipse.leshan.core.response.DiscoverResponse;
import org.eclipse.leshan.core.response.ExecuteResponse;
import org.eclipse.leshan.core.response.LwM2mResponse;
import org.eclipse.leshan.core.response.ObserveCompositeResponse;
import org.eclipse.leshan.core.response.ObserveResponse;
import org.eclipse.leshan.core.response.ReadCompositeResponse;
import org.eclipse.leshan.core.response.ReadResponse;
import org.eclipse.leshan.core.response.WriteAttributesResponse;
import org.eclipse.leshan.core.response.WriteCompositeResponse;
import org.eclipse.leshan.core.response.WriteResponse;
import org.eclipse.leshan.core.util.Hex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is able to create a {@link LwM2mResponse} from a CoAP {@link Response}.
 * <p>
 * Call <code>LwM2mResponseBuilder#visit(coapResponse)</code>, then get the result using {@link #getResponse()}
 *
 * @param <T> the type of the response to build.
 */
public class LwM2mResponseBuilder<T extends LwM2mResponse> implements DownlinkRequestVisitor {

    private static final Logger LOG = LoggerFactory.getLogger(LwM2mResponseBuilder.class);

    private LwM2mResponse lwM2mresponse;
    private final Request coapRequest;
    private final Response coapResponse;
    private final String clientEndpoint;
    private final LwM2mModel model;
    private final LwM2mDecoder decoder;
    private final LwM2mLinkParser linkParser;

    public LwM2mResponseBuilder(Request coapRequest, Response coapResponse, String clientEndpoint, LwM2mModel model,
            LwM2mDecoder decoder, LwM2mLinkParser linkParser) {
        this.coapRequest = coapRequest;
        this.coapResponse = coapResponse;
        this.clientEndpoint = clientEndpoint;
        this.model = model;
        this.decoder = decoder;
        this.linkParser = linkParser;
    }

    @Override
    public void visit(ReadRequest request) {
        if (coapResponse.isError()) {
            // handle error response:
            lwM2mresponse = new ReadResponse(toLwM2mResponseCode(coapResponse.getCode()), null,
                    coapResponse.getPayloadString(), coapResponse);
        } else if (isResponseCodeContent()) {
            // handle success response:
            LwM2mNode content = decodeCoapResponse(request.getPath(), coapResponse, request, clientEndpoint);
            lwM2mresponse = new ReadResponse(ResponseCode.CONTENT, content, null, coapResponse);
        } else {
            // handle unexpected response:
            handleUnexpectedResponseCode(clientEndpoint, request, coapResponse);
        }
    }

    @Override
    public void visit(DiscoverRequest request) {
        if (coapResponse.isError()) {
            // handle error response:
            lwM2mresponse = new DiscoverResponse(toLwM2mResponseCode(coapResponse.getCode()), null,
                    coapResponse.getPayloadString(), coapResponse);
        } else if (isResponseCodeContent()) {
            // handle success response:
            LwM2mLink[] links;
            if (MediaTypeRegistry.APPLICATION_LINK_FORMAT != coapResponse.getOptions().getContentFormat()) {
                throw new InvalidResponseException("Client [%s] returned unexpected content format [%s] for [%s]",
                        clientEndpoint, coapResponse.getOptions().getContentFormat(), request);
            } else {
                try {
                    // We don't know if root path should be present in discover response.
                    // See : https://github.com/OpenMobileAlliance/OMA_LwM2M_for_Developers/issues/534
                    String rootpath = null;
                    links = linkParser.parseLwM2mLinkFromCoreLinkFormat(coapResponse.getPayload(), rootpath);
                } catch (LinkParseException e) {
                    throw new InvalidResponseException(e,
                            "Unable to decode response payload of request [%s] from client [%s]", request,
                            clientEndpoint);
                }
            }
            lwM2mresponse = new DiscoverResponse(ResponseCode.CONTENT, links, null, coapResponse);
        } else {
            // handle unexpected response:
            handleUnexpectedResponseCode(clientEndpoint, request, coapResponse);
        }
    }

    @Override
    public void visit(WriteRequest request) {
        if (coapResponse.isError()) {
            // handle error response:
            lwM2mresponse = new WriteResponse(toLwM2mResponseCode(coapResponse.getCode()),
                    coapResponse.getPayloadString(), coapResponse);
        } else if (isResponseCodeChanged()) {
            // handle success response:
            lwM2mresponse = new WriteResponse(ResponseCode.CHANGED, null, coapResponse);
        } else {
            // handle unexpected response:
            handleUnexpectedResponseCode(clientEndpoint, request, coapResponse);
        }
    }

    @Override
    public void visit(WriteAttributesRequest request) {
        if (coapResponse.isError()) {
            // handle error response:
            lwM2mresponse = new WriteAttributesResponse(toLwM2mResponseCode(coapResponse.getCode()),
                    coapResponse.getPayloadString(), coapResponse);
        } else if (isResponseCodeChanged()) {
            // handle success response:
            lwM2mresponse = new WriteAttributesResponse(ResponseCode.CHANGED, null, coapResponse);
        } else {
            // handle unexpected response:
            handleUnexpectedResponseCode(clientEndpoint, request, coapResponse);
        }
    }

    @Override
    public void visit(ExecuteRequest request) {
        if (coapResponse.isError()) {
            // handle error response:
            lwM2mresponse = new ExecuteResponse(toLwM2mResponseCode(coapResponse.getCode()),
                    coapResponse.getPayloadString(), coapResponse);
        } else if (isResponseCodeChanged()) {
            // handle success response:
            lwM2mresponse = new ExecuteResponse(ResponseCode.CHANGED, null, coapResponse);
        } else {
            // handle unexpected response:
            handleUnexpectedResponseCode(clientEndpoint, request, coapResponse);
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
            lwM2mresponse = new CreateResponse(ResponseCode.CREATED,
                    coapResponse.getOptions().getLocationPathCount() == 0 ? null
                            : coapResponse.getOptions().getLocationPathString(),
                    null, coapResponse);
        } else {
            // handle unexpected response:
            handleUnexpectedResponseCode(clientEndpoint, request, coapResponse);
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
            handleUnexpectedResponseCode(clientEndpoint, request, coapResponse);
        }
    }

    @Override
    public void visit(ObserveRequest request) {
        if (coapResponse.isError()) {
            // handle error response:
            lwM2mresponse = new ObserveResponse(toLwM2mResponseCode(coapResponse.getCode()), null, null, null,
                    coapResponse.getPayloadString(), coapResponse);
        } else if (isResponseCodeContent()
                // This is for backward compatibility, when the spec say notification used CHANGED code
                || isResponseCodeChanged()) {
            // handle success response:
            LwM2mNode content = decodeCoapResponse(request.getPath(), coapResponse, request, clientEndpoint);
            SingleObservation observation = null;
            if (coapResponse.getOptions().hasObserve()) {

                /*
                 * Note: When using OSCORE and Observe the first coapRequest sent to register an observation can have
                 * its Token missing here. Is this because OSCORE re-creates the request before sending? When looking in
                 * Wireshark all messages have a Token as they should. The lines below fixes this by taking the Token
                 * from the response that came to the request (since the request actually has a Token when going out the
                 * response will have the same correct Token.
                 *
                 * TODO OSCORE : This should probably not be done here. should we fix this ? should we check if oscore
                 * is used ?
                 */
                if (coapRequest.getTokenBytes() == null) {
                    coapRequest.setToken(coapResponse.getTokenBytes());
                }

                // observe request successful
                observation = ObserveUtil.createLwM2mObservation(coapRequest);
            }
            lwM2mresponse = new ObserveResponse(toLwM2mResponseCode(coapResponse.getCode()), content, null, observation,
                    null, coapResponse);
        } else {
            // handle unexpected response:
            handleUnexpectedResponseCode(clientEndpoint, request, coapResponse);
        }
    }

    @Override
    public void visit(CancelObservationRequest request) {
        if (coapResponse.isError()) {
            // handle error response:
            lwM2mresponse = new CancelObservationResponse(toLwM2mResponseCode(coapResponse.getCode()), null, null, null,
                    coapResponse.getPayloadString(), coapResponse);
        } else if (isResponseCodeContent()
                // This is for backward compatibility, when the spec say notification used CHANGED code
                || isResponseCodeChanged()) {
            // handle success response:
            LwM2mNode content = decodeCoapResponse(request.getPath(), coapResponse, request, clientEndpoint);
            lwM2mresponse = new CancelObservationResponse(toLwM2mResponseCode(coapResponse.getCode()), content, null,
                    null, null, coapResponse);
        } else {
            // handle unexpected response:
            handleUnexpectedResponseCode(clientEndpoint, request, coapResponse);
        }
    }

    @Override
    public void visit(ReadCompositeRequest request) {
        if (coapResponse.isError()) {
            // handle error response:
            lwM2mresponse = new ReadCompositeResponse(toLwM2mResponseCode(coapResponse.getCode()), null,
                    coapResponse.getPayloadString(), coapResponse);
        } else if (isResponseCodeContent()) {
            // handle success response:
            Map<LwM2mPath, LwM2mNode> content = decodeCompositeCoapResponse(request.getPaths(), coapResponse, request,
                    clientEndpoint);
            lwM2mresponse = new ReadCompositeResponse(ResponseCode.CONTENT, content, null, coapResponse);
        } else {
            // handle unexpected response:
            handleUnexpectedResponseCode(clientEndpoint, request, coapResponse);
        }
    }

    @Override
    public void visit(ObserveCompositeRequest request) {
        if (coapResponse.isError()) {
            // handle error response:
            lwM2mresponse = new ObserveCompositeResponse(toLwM2mResponseCode(coapResponse.getCode()), null,
                    coapResponse.getPayloadString(), coapResponse, null);

        } else if (isResponseCodeContent()) {
            // handle success response:
            Map<LwM2mPath, LwM2mNode> content = decodeCompositeCoapResponse(request.getPaths(), coapResponse, request,
                    clientEndpoint);

            CompositeObservation observation = null;
            if (coapResponse.getOptions().hasObserve()) {
                // observe request successful
                observation = ObserveUtil.createLwM2mCompositeObservation(coapRequest);
            }
            lwM2mresponse = new ObserveCompositeResponse(toLwM2mResponseCode(coapResponse.getCode()), content, null,
                    coapResponse, observation);
        } else {
            // handle unexpected response:
            handleUnexpectedResponseCode(clientEndpoint, request, coapResponse);
        }
    }

    @Override
    public void visit(CancelCompositeObservationRequest request) {
        if (coapResponse.isError()) {
            // handle error response:
            lwM2mresponse = new CancelCompositeObservationResponse(toLwM2mResponseCode(coapResponse.getCode()), null,
                    coapResponse.getPayloadString(), coapResponse, null);
        } else if (isResponseCodeContent() || isResponseCodeChanged()) {
            // handle success response:
            Map<LwM2mPath, LwM2mNode> content = decodeCompositeCoapResponse(request.getPaths(), coapResponse, request,
                    clientEndpoint);
            lwM2mresponse = new CancelCompositeObservationResponse(toLwM2mResponseCode(coapResponse.getCode()), content,
                    null, coapResponse, null);
        } else {
            // handle unexpected response:
            handleUnexpectedResponseCode(clientEndpoint, request, coapResponse);
        }
    }

    @Override
    public void visit(WriteCompositeRequest request) {
        if (coapResponse.isError()) {
            // handle error response:
            lwM2mresponse = new WriteCompositeResponse(toLwM2mResponseCode(coapResponse.getCode()),
                    coapResponse.getPayloadString(), coapResponse);
        } else if (isResponseCodeChanged()) {
            // handle success response:
            lwM2mresponse = new WriteCompositeResponse(ResponseCode.CHANGED, null, coapResponse);
        } else {
            // handle unexpected response:
            handleUnexpectedResponseCode(clientEndpoint, request, coapResponse);
        }
    }

    @Override
    public void visit(BootstrapDiscoverRequest request) {
        if (coapResponse.isError()) {
            // handle error response:
            lwM2mresponse = new BootstrapDiscoverResponse(toLwM2mResponseCode(coapResponse.getCode()), null,
                    coapResponse.getPayloadString(), coapResponse);
        } else if (isResponseCodeContent()) {
            // handle success response:
            LwM2mLink[] links;
            if (MediaTypeRegistry.APPLICATION_LINK_FORMAT != coapResponse.getOptions().getContentFormat()) {
                throw new InvalidResponseException("Client [%s] returned unexpected content format [%s] for [%s]",
                        clientEndpoint, coapResponse.getOptions().getContentFormat(), request);
            } else {
                try {
                    links = linkParser.parseLwM2mLinkFromCoreLinkFormat(coapResponse.getPayload(), null);
                } catch (LinkParseException e) {
                    throw new InvalidResponseException(e,
                            "Unable to decode response payload of request [%s] from client [%s]", request,
                            clientEndpoint);
                }
            }
            lwM2mresponse = new BootstrapDiscoverResponse(ResponseCode.CONTENT, links, null, coapResponse);
        } else {
            // handle unexpected response:
            handleUnexpectedResponseCode(clientEndpoint, request, coapResponse);
        }
    }

    @Override
    public void visit(BootstrapWriteRequest request) {
        if (coapResponse.isError()) {
            // handle error response:
            lwM2mresponse = new BootstrapWriteResponse(toLwM2mResponseCode(coapResponse.getCode()),
                    coapResponse.getPayloadString(), coapResponse);
        } else if (isResponseCodeChanged()) {
            // handle success response:
            lwM2mresponse = new BootstrapWriteResponse(ResponseCode.CHANGED, null, coapResponse);
        } else {
            // handle unexpected response:
            handleUnexpectedResponseCode(clientEndpoint, request, coapResponse);
        }
    }

    @Override
    public void visit(BootstrapReadRequest request) {
        if (coapResponse.isError()) {
            // handle error response:
            lwM2mresponse = new BootstrapReadResponse(toLwM2mResponseCode(coapResponse.getCode()), null,
                    coapResponse.getPayloadString(), coapResponse);
        } else if (isResponseCodeContent()) {
            // handle success response:
            LwM2mNode content = decodeCoapResponse(request.getPath(), coapResponse, request, clientEndpoint);
            lwM2mresponse = new BootstrapReadResponse(ResponseCode.CONTENT, content, null, coapResponse);
        } else {
            // handle unexpected response:
            handleUnexpectedResponseCode(clientEndpoint, request, coapResponse);
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
            handleUnexpectedResponseCode(clientEndpoint, request, coapResponse);
        }
    }

    @Override
    public void visit(BootstrapFinishRequest request) {
        if (coapResponse.isError()) {
            // handle error response:
            lwM2mresponse = new BootstrapFinishResponse(toLwM2mResponseCode(coapResponse.getCode()),
                    coapResponse.getPayloadString(), coapResponse);
        } else if (isResponseCodeChanged()) {
            // handle success response:
            lwM2mresponse = new BootstrapFinishResponse(ResponseCode.CHANGED, null, coapResponse);
        } else {
            // handle unexpected response:
            handleUnexpectedResponseCode(clientEndpoint, request, coapResponse);
        }
    }

    private boolean isResponseCodeContent() {
        return coapResponse.getCode() == org.eclipse.californium.core.coap.CoAP.ResponseCode.CONTENT;
    }

    private boolean isResponseCodeChanged() {
        return coapResponse.getCode() == org.eclipse.californium.core.coap.CoAP.ResponseCode.CHANGED;
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

    private Map<LwM2mPath, LwM2mNode> decodeCompositeCoapResponse(List<LwM2mPath> paths, Response coapResponse,
            LwM2mRequest<?> request, String endpoint) {

        // Get content format
        ContentFormat contentFormat = null;
        if (coapResponse.getOptions().hasContentFormat()) {
            contentFormat = ContentFormat.fromCode(coapResponse.getOptions().getContentFormat());
        }

        // Decode payload
        try {
            return decoder.decodeNodes(coapResponse.getPayload(), contentFormat, paths, model);
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
