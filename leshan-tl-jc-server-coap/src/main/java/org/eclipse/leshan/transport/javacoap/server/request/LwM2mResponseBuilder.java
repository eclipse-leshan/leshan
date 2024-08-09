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
package org.eclipse.leshan.transport.javacoap.server.request;

import java.util.List;
import java.util.Map;

import org.eclipse.leshan.core.ResponseCode;
import org.eclipse.leshan.core.link.LinkParseException;
import org.eclipse.leshan.core.link.lwm2m.LwM2mLink;
import org.eclipse.leshan.core.link.lwm2m.LwM2mLinkParser;
import org.eclipse.leshan.core.model.LwM2mModel;
import org.eclipse.leshan.core.node.LwM2mNode;
import org.eclipse.leshan.core.node.LwM2mPath;
import org.eclipse.leshan.core.node.TimestampedLwM2mNode;
import org.eclipse.leshan.core.node.TimestampedLwM2mNodes;
import org.eclipse.leshan.core.node.codec.CodecException;
import org.eclipse.leshan.core.node.codec.LwM2mDecoder;
import org.eclipse.leshan.core.observation.CompositeObservation;
import org.eclipse.leshan.core.observation.Observation;
import org.eclipse.leshan.core.observation.SingleObservation;
import org.eclipse.leshan.core.request.CancelCompositeObservationRequest;
import org.eclipse.leshan.core.request.CancelObservationRequest;
import org.eclipse.leshan.core.request.ContentFormat;
import org.eclipse.leshan.core.request.CreateRequest;
import org.eclipse.leshan.core.request.DeleteRequest;
import org.eclipse.leshan.core.request.DiscoverRequest;
import org.eclipse.leshan.core.request.DownlinkDeviceManagementRequestVisitor;
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
import org.eclipse.leshan.transport.javacoap.request.ResponseCodeUtil;
import org.eclipse.leshan.transport.javacoap.server.observation.LwM2mKeys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mbed.coap.packet.CoapRequest;
import com.mbed.coap.packet.CoapResponse;
import com.mbed.coap.packet.Code;
import com.mbed.coap.packet.MediaTypes;

/**
 * This class is able to create a {@link LwM2mResponse} from a CoAP {@link CoapResponse}.
 * <p>
 * Call <code>LwM2mResponseBuilder#visit(coapResponse)</code>, then get the result using {@link #getResponse()}
 *
 * @param <T> the type of the response to build.
 */
public class LwM2mResponseBuilder<T extends LwM2mResponse> implements DownlinkDeviceManagementRequestVisitor {

    private static final Logger LOG = LoggerFactory.getLogger(LwM2mResponseBuilder.class);

    private LwM2mResponse lwM2mresponse;

    private final CoapResponse coapResponse;
    private final CoapRequest coapRequest;

    private final String clientEndpoint;
    private final LwM2mModel model;
    private final LwM2mDecoder decoder;
    private final LwM2mLinkParser linkParser;

    public LwM2mResponseBuilder(CoapResponse coapResponse, CoapRequest coapRequest, String clientEndpoint,
            LwM2mModel model, LwM2mDecoder decoder, LwM2mLinkParser linkParser) {
        this.coapResponse = coapResponse;
        this.coapRequest = coapRequest;

        this.clientEndpoint = clientEndpoint;

        this.model = model;
        this.decoder = decoder;
        this.linkParser = linkParser;
    }

    @Override
    public void visit(ReadRequest request) {
        if (coapResponse.getCode().getHttpCode() >= 400) {
            // handle error response:
            lwM2mresponse = new ReadResponse(toLwM2mResponseCode(coapResponse.getCode()), null, null,
                    coapResponse.getPayloadString(), coapResponse);
        } else if (isResponseCodeContent()) {
            // handle success response with timestamped node
            TimestampedLwM2mNode timestampedNode = decodeCoapTimestampedResponse(request.getPath(), coapResponse,
                    request, clientEndpoint);
            lwM2mresponse = new ReadResponse(ResponseCode.CONTENT, null, timestampedNode, null, coapResponse);
        } else {
            // handle unexpected response:
            handleUnexpectedResponseCode(clientEndpoint, request, coapResponse);
        }
    }

    @Override
    public void visit(DiscoverRequest request) {
        if (coapResponse.getCode().getHttpCode() >= 400) {
            // handle error response:
            lwM2mresponse = new DiscoverResponse(toLwM2mResponseCode(coapResponse.getCode()), null,
                    coapResponse.getPayloadString(), coapResponse);
        } else if (isResponseCodeContent()) {
            // handle success response:
            LwM2mLink[] links;
            if (MediaTypes.CT_APPLICATION_LINK__FORMAT != coapResponse.options().getContentFormat()) {
                throw new InvalidResponseException("Client [%s] returned unexpected content format [%s] for [%s]",
                        clientEndpoint, coapResponse.options().getContentFormat(), request);
            } else {
                try {
                    // We don't know if root path should be present in discover response.
                    // See : https://github.com/OpenMobileAlliance/OMA_LwM2M_for_Developers/issues/534
                    String rootpath = null;
                    links = linkParser.parseLwM2mLinkFromCoreLinkFormat(coapResponse.getPayload().getBytes(), rootpath);
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
        if (coapResponse.getCode().getHttpCode() >= 400) {
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
        if (coapResponse.getCode().getHttpCode() >= 400) {
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
        if (coapResponse.getCode().getHttpCode() >= 400) {
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
        if (coapResponse.getCode().getHttpCode() >= 400) {
            // handle error response:
            lwM2mresponse = new CreateResponse(toLwM2mResponseCode(coapResponse.getCode()), null,
                    coapResponse.getPayloadString(), coapResponse);
        } else if (coapResponse.getCode() == Code.C201_CREATED) {
            // handle success response:
            String locationPath = coapResponse.options().getLocationPath();
            if (locationPath == null || locationPath.equals("/")) {
                locationPath = null;
            } else if (locationPath.startsWith("/")) {
                locationPath = locationPath.substring(1);
            }
            lwM2mresponse = new CreateResponse(ResponseCode.CREATED, locationPath, null, coapResponse);
        } else {
            // handle unexpected response:
            handleUnexpectedResponseCode(clientEndpoint, request, coapResponse);
        }
    }

    @Override
    public void visit(DeleteRequest request) {
        if (coapResponse.getCode().getHttpCode() >= 400) {
            // handle error response:
            lwM2mresponse = new DeleteResponse(toLwM2mResponseCode(coapResponse.getCode()),
                    coapResponse.getPayloadString(), coapResponse);
        } else if (coapResponse.getCode() == Code.C202_DELETED) {
            // handle success response:
            lwM2mresponse = new DeleteResponse(ResponseCode.DELETED, null, coapResponse);
        } else {
            // handle unexpected response:
            handleUnexpectedResponseCode(clientEndpoint, request, coapResponse);
        }
    }

    @Override
    public void visit(ObserveRequest request) {
        // TODO implement observe
        if (coapResponse.getCode().getHttpCode() >= 400) {
            // handle error response:
            lwM2mresponse = new ObserveResponse(toLwM2mResponseCode(coapResponse.getCode()), null, null, null, null,
                    coapResponse.getPayloadString(), coapResponse);
        } else if (isResponseCodeContent()
                // This is for backward compatibility, when the spec say notification used CHANGED code
                || isResponseCodeChanged()) {
            // handle success response:
            TimestampedLwM2mNode timestampedNode = decodeCoapTimestampedResponse(request.getPath(), coapResponse,
                    request, clientEndpoint);

            if (coapResponse.options().getObserve() != null) {
                // Observe relation established
                Observation observation = coapRequest.getTransContext().get(LwM2mKeys.LESHAN_OBSERVATION);
                if (observation instanceof SingleObservation) {
                    lwM2mresponse = new ObserveResponse(toLwM2mResponseCode(coapResponse.getCode()), null,
                            timestampedNode, null, (SingleObservation) observation, null, coapResponse);
                } else {
                    throw new IllegalStateException(String.format(
                            "A Single Observation is expected in coapRequest transport Context, but was %s",
                            observation == null ? "null" : observation.getClass().getSimpleName()));
                }
            } else {
                // Observe relation NOT established
                lwM2mresponse = new ObserveResponse(toLwM2mResponseCode(coapResponse.getCode()), null, timestampedNode,
                        null, null, null, coapResponse);
            }
        } else {
            // handle unexpected response:
            handleUnexpectedResponseCode(clientEndpoint, request, coapResponse);
        }
    }

    @Override
    public void visit(CancelObservationRequest request) {
        if (coapResponse.getCode().getHttpCode() >= 400) {
            // handle error response:
            lwM2mresponse = new CancelObservationResponse(toLwM2mResponseCode(coapResponse.getCode()), null, null, null,
                    null, coapResponse.getPayloadString(), coapResponse);
        } else if (isResponseCodeContent()
                // This is for backward compatibility, when the spec say notification used CHANGED code
                || isResponseCodeChanged()) {
            // handle success response:
            TimestampedLwM2mNode timestampedNode = decodeCoapTimestampedResponse(request.getPath(), coapResponse,
                    request, clientEndpoint);
            lwM2mresponse = new CancelObservationResponse(toLwM2mResponseCode(coapResponse.getCode()), null,
                    timestampedNode, null, null, null, coapResponse);
        } else {
            // handle unexpected response:
            handleUnexpectedResponseCode(clientEndpoint, request, coapResponse);
        }
    }

    @Override
    public void visit(ReadCompositeRequest request) {
        if (coapResponse.getCode().getHttpCode() >= 400) {
            // handle error response:
            lwM2mresponse = new ReadCompositeResponse(toLwM2mResponseCode(coapResponse.getCode()), null, null,
                    coapResponse.getPayloadString(), coapResponse);
        } else if (isResponseCodeContent()) {
            // handle success response:
            TimestampedLwM2mNodes timestampedNodes = decodeTimestampedCompositeCoapResponse(request.getPaths(),
                    coapResponse, request, clientEndpoint);
            lwM2mresponse = new ReadCompositeResponse(ResponseCode.CONTENT, null, timestampedNodes, null, coapResponse);
        } else {
            // handle unexpected response:
            handleUnexpectedResponseCode(clientEndpoint, request, coapResponse);
        }
    }

    @Override
    public void visit(ObserveCompositeRequest request) {
        if (coapResponse.getCode().getHttpCode() >= 400) {
            // handle error response:
            lwM2mresponse = new ObserveCompositeResponse(toLwM2mResponseCode(coapResponse.getCode()), null, null, null,
                    coapResponse.getPayloadString(), coapResponse);

        } else if (isResponseCodeContent()) {
            // handle success response:
            Map<LwM2mPath, LwM2mNode> content = decodeCompositeCoapResponse(request.getPaths(), coapResponse, request,
                    clientEndpoint);

            if (coapResponse.options().getObserve() != null) {
                // Observe relation established
                Observation observation = coapRequest.getTransContext().get(LwM2mKeys.LESHAN_OBSERVATION);
                if (observation instanceof CompositeObservation) {
                    lwM2mresponse = new ObserveCompositeResponse(toLwM2mResponseCode(coapResponse.getCode()), content,
                            null, (CompositeObservation) observation, null, coapResponse);
                } else {
                    throw new IllegalStateException(String.format(
                            "A Composite Observation is expected in coapRequest transport Context, but was %s",
                            observation == null ? "null" : observation.getClass().getSimpleName()));
                }
            } else {
                // Observe relation NOTestablished
                lwM2mresponse = new ObserveCompositeResponse(toLwM2mResponseCode(coapResponse.getCode()), content, null,
                        null, null, coapResponse);
            }
        } else {
            // handle unexpected response:
            handleUnexpectedResponseCode(clientEndpoint, request, coapResponse);
        }
    }

    @Override
    public void visit(CancelCompositeObservationRequest request) {
        if (coapResponse.getCode().getHttpCode() >= 400) {
            // handle error response:
            lwM2mresponse = new CancelCompositeObservationResponse(toLwM2mResponseCode(coapResponse.getCode()), null,
                    null, null, coapResponse.getPayloadString(), coapResponse);
        } else if (isResponseCodeContent() || isResponseCodeChanged()) {
            // handle success response:
            Map<LwM2mPath, LwM2mNode> content = decodeCompositeCoapResponse(request.getPaths(), coapResponse, request,
                    clientEndpoint);
            lwM2mresponse = new CancelCompositeObservationResponse(toLwM2mResponseCode(coapResponse.getCode()), content,
                    null, null, null, coapResponse);
        } else {
            // handle unexpected response:
            handleUnexpectedResponseCode(clientEndpoint, request, coapResponse);
        }
    }

    @Override
    public void visit(WriteCompositeRequest request) {
        if (coapResponse.getCode().getHttpCode() >= 400) {
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

    // TODO to be moved when leshan-tl-jc-bsserver-coap will be created

//    @Override
//    public void visit(BootstrapDiscoverRequest request) {
//        if (coapResponse.getCode().getHttpCode() >= 400) {
//            // handle error response:
//            lwM2mresponse = new BootstrapDiscoverResponse(toLwM2mResponseCode(coapResponse.getCode()), null,
//                    coapResponse.getPayloadString(), coapResponse);
//        } else if (isResponseCodeContent()) {
//            // handle success response:
//            LwM2mLink[] links;
//            if (MediaTypes.CT_APPLICATION_LINK__FORMAT != coapResponse.options().getContentFormat()) {
//                throw new InvalidResponseException("Client [%s] returned unexpected content format [%s] for [%s]",
//                        clientEndpoint, coapResponse.options().getContentFormat(), request);
//            } else {
//                try {
//                    links = linkParser.parseLwM2mLinkFromCoreLinkFormat(coapResponse.getPayload().getBytes(), null);
//                } catch (LinkParseException e) {
//                    throw new InvalidResponseException(e,
//                            "Unable to decode response payload of request [%s] from client [%s]", request,
//                            clientEndpoint);
//                }
//            }
//            lwM2mresponse = new BootstrapDiscoverResponse(ResponseCode.CONTENT, links, null, coapResponse);
//        } else {
//            // handle unexpected response:
//            handleUnexpectedResponseCode(clientEndpoint, request, coapResponse);
//        }
//    }
//
//    @Override
//    public void visit(BootstrapWriteRequest request) {
//        if (coapResponse.getCode().getHttpCode() >= 400) {
//            // handle error response:
//            lwM2mresponse = new BootstrapWriteResponse(toLwM2mResponseCode(coapResponse.getCode()),
//                    coapResponse.getPayloadString(), coapResponse);
//        } else if (isResponseCodeChanged()) {
//            // handle success response:
//            lwM2mresponse = new BootstrapWriteResponse(ResponseCode.CHANGED, null, coapResponse);
//        } else {
//            // handle unexpected response:
//            handleUnexpectedResponseCode(clientEndpoint, request, coapResponse);
//        }
//    }
//
//    @Override
//    public void visit(BootstrapReadRequest request) {
//        if (coapResponse.getCode().getHttpCode() >= 400) {
//            // handle error response:
//            lwM2mresponse = new BootstrapReadResponse(toLwM2mResponseCode(coapResponse.getCode()), null,
//                    coapResponse.getPayloadString(), coapResponse);
//        } else if (isResponseCodeContent()) {
//            // handle success response:
//            LwM2mNode content = decodeCoapResponse(request.getPath(), coapResponse, request, clientEndpoint);
//            lwM2mresponse = new BootstrapReadResponse(ResponseCode.CONTENT, content, null, coapResponse);
//        } else {
//            // handle unexpected response:
//            handleUnexpectedResponseCode(clientEndpoint, request, coapResponse);
//        }
//    }
//
//    @Override
//    public void visit(BootstrapDeleteRequest request) {
//        if (coapResponse.getCode().getHttpCode() >= 400) {
//            // handle error response:
//            lwM2mresponse = new BootstrapDeleteResponse(toLwM2mResponseCode(coapResponse.getCode()),
//                    coapResponse.getPayloadString(), coapResponse);
//        } else if (coapResponse.getCode() == Code.C202_DELETED) {
//            // handle success response:
//            lwM2mresponse = new BootstrapDeleteResponse(ResponseCode.DELETED, null, coapResponse);
//        } else {
//            // handle unexpected response:
//            handleUnexpectedResponseCode(clientEndpoint, request, coapResponse);
//        }
//    }
//
//    @Override
//    public void visit(BootstrapFinishRequest request) {
//        if (coapResponse.getCode().getHttpCode() >= 400) {
//            // handle error response:
//            lwM2mresponse = new BootstrapFinishResponse(toLwM2mResponseCode(coapResponse.getCode()),
//                    coapResponse.getPayloadString(), coapResponse);
//        } else if (isResponseCodeChanged()) {
//            // handle success response:
//            lwM2mresponse = new BootstrapFinishResponse(ResponseCode.CHANGED, null, coapResponse);
//        } else {
//            // handle unexpected response:
//            handleUnexpectedResponseCode(clientEndpoint, request, coapResponse);
//        }
//    }

    private boolean isResponseCodeContent() {
        return coapResponse.getCode() == Code.C205_CONTENT;
    }

    private boolean isResponseCodeChanged() {
        return coapResponse.getCode() == Code.C204_CHANGED;
    }

    public static ResponseCode toLwM2mResponseCode(Code coapResponseCode) {
        return ResponseCodeUtil.toLwM2mResponseCode(coapResponseCode);
    }

//    private LwM2mNode decodeCoapResponse(LwM2mPath path, CoapResponse coapResponse, LwM2mRequest<?> request,
//            String endpoint) {
//        try {
//            return decoder.decode(coapResponse.getPayload().getBytes(), getContentFormat(coapResponse), path, model);
//        } catch (CodecException e) {
//            handleCodecException(e, request, coapResponse, endpoint);
//            return null; // should not happen as handleCodecException raise exception
//        }
//    }

    private Map<LwM2mPath, LwM2mNode> decodeCompositeCoapResponse(List<LwM2mPath> paths, CoapResponse coapResponse,
            LwM2mRequest<?> request, String endpoint) {
        try {
            return decoder.decodeNodes(coapResponse.getPayload().getBytes(), getContentFormat(coapResponse), paths,
                    model);
        } catch (CodecException e) {
            handleCodecException(e, request, coapResponse, endpoint);
            return null; // should not happen as handleCodecException raise exception
        }
    }

    private TimestampedLwM2mNodes decodeTimestampedCompositeCoapResponse(List<LwM2mPath> paths,
            CoapResponse coapResponse, LwM2mRequest<?> request, String endpoint) {
        try {
            return decoder.decodeTimestampedNodes(coapResponse.getPayload().getBytes(), getContentFormat(coapResponse),
                    paths, model);
        } catch (CodecException e) {
            handleCodecException(e, request, coapResponse, endpoint);
            return null; // should not happen as handleCodecException raise exception
        }
    }

    private TimestampedLwM2mNode decodeCoapTimestampedResponse(LwM2mPath path, CoapResponse coapResponse,
            LwM2mRequest<?> request, String endpoint) {
        List<TimestampedLwM2mNode> timestampedNodes = null;
        try {

            timestampedNodes = decoder.decodeTimestampedData(coapResponse.getPayload().getBytes(),
                    getContentFormat(coapResponse), path, model);
            if (timestampedNodes.size() != 1) {
                throw new InvalidResponseException(
                        "Unable to decode response payload of request [%s] from client [%s] : should receive only 1 timestamped node but received %s",
                        request, endpoint, timestampedNodes.size());
            }
            return timestampedNodes.get(0);
        } catch (CodecException e) {
            handleCodecException(e, request, coapResponse, endpoint);
            return null; // should not happen as handleCodecException raise exception
        }

    }

    private ContentFormat getContentFormat(CoapResponse coapResponse) {
        ContentFormat contentFormat = null;
        if (coapResponse.options().getContentFormat() != null) {
            contentFormat = ContentFormat.fromCode(coapResponse.options().getContentFormat());
        }
        return contentFormat;
    }

    private void handleCodecException(CodecException e, LwM2mRequest<?> request, CoapResponse coapResponse,
            String endpoint) {
        if (LOG.isDebugEnabled()) {
            byte[] payload = coapResponse.getPayload() == null ? new byte[0] : coapResponse.getPayload().getBytes();
            LOG.debug(String.format("Unable to decode response payload of request [%s] from client [%s] [payload:%s]",
                    request, endpoint, Hex.encodeHexString(payload)));
        }
        throw new InvalidResponseException(e, "Unable to decode response payload of request [%s] from client [%s]",
                request, endpoint);
    }

    @SuppressWarnings("unchecked")
    public T getResponse() {
        return (T) lwM2mresponse;
    }

    private void handleUnexpectedResponseCode(String clientEndpoint, LwM2mRequest<?> request,
            CoapResponse coapResponse) {
        throw new InvalidResponseException("Client [%s] returned unexpected response code [%s] for [%s]",
                clientEndpoint, coapResponse.getCode(), request);
    }
}
