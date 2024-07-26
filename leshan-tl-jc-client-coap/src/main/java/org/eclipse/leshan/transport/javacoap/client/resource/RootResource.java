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

import static java.util.concurrent.CompletableFuture.completedFuture;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.eclipse.leshan.client.endpoint.ClientEndpointToolbox;
import org.eclipse.leshan.client.request.DownlinkRequestReceiver;
import org.eclipse.leshan.client.servers.LwM2mServer;
import org.eclipse.leshan.core.ResponseCode;
import org.eclipse.leshan.core.node.LwM2mNode;
import org.eclipse.leshan.core.node.LwM2mPath;
import org.eclipse.leshan.core.request.BootstrapDeleteRequest;
import org.eclipse.leshan.core.request.BootstrapDiscoverRequest;
import org.eclipse.leshan.core.request.ContentFormat;
import org.eclipse.leshan.core.request.ObserveCompositeRequest;
import org.eclipse.leshan.core.request.ReadCompositeRequest;
import org.eclipse.leshan.core.request.WriteCompositeRequest;
import org.eclipse.leshan.core.response.BootstrapDeleteResponse;
import org.eclipse.leshan.core.response.BootstrapDiscoverResponse;
import org.eclipse.leshan.core.response.ObserveCompositeResponse;
import org.eclipse.leshan.core.response.ReadCompositeResponse;
import org.eclipse.leshan.core.response.WriteCompositeResponse;
import org.eclipse.leshan.transport.javacoap.identity.IdentityHandler;
import org.eclipse.leshan.transport.javacoap.request.ResponseCodeUtil;

import com.mbed.coap.packet.CoapRequest;
import com.mbed.coap.packet.CoapResponse;
import com.mbed.coap.packet.MediaTypes;
import com.mbed.coap.packet.Opaque;

public class RootResource extends LwM2mClientCoapResource {

    protected DownlinkRequestReceiver requestReceiver;
    protected ClientEndpointToolbox toolbox;

    public RootResource(DownlinkRequestReceiver requestReceiver, ClientEndpointToolbox toolbox,
            IdentityHandler identityHandler, ServerIdentityExtractor serverIdentityExtractor) {
        super("", identityHandler, serverIdentityExtractor);
        this.requestReceiver = requestReceiver;
        this.toolbox = toolbox;
    }

    @Override
    public CompletableFuture<CoapResponse> handleGET(CoapRequest coapRequest) {
        // Get server identity
        LwM2mServer identity = extractIdentity(coapRequest);
        if (identity == null) {
            return unknownServer();
        }

        // Manage Bootstrap Discover Request
        BootstrapDiscoverResponse response = requestReceiver
                .requestReceived(identity, new BootstrapDiscoverRequest("/", coapRequest)).getResponse();
        if (response.getCode().isError()) {
            return errorMessage(response.getCode(), response.getErrorMessage());
        } else {
            return completedFuture(CoapResponse //
                    .coapResponse(ResponseCodeUtil.toCoapResponseCode(response.getCode())) //
                    .payload(Opaque.of(toolbox.getLinkSerializer().serializeCoreLinkFormat(response.getObjectLinks()))) //
                    .contentFormat(MediaTypes.CT_APPLICATION_LINK__FORMAT) //
                    .build());
        }
    }

    @Override
    public CompletableFuture<CoapResponse> handleFETCH(CoapRequest coapRequest) {
        // Get server identity
        LwM2mServer identity = extractIdentity(coapRequest);
        if (identity == null) {
            return unknownServer();
        }

        // Handle content format for the response
        ContentFormat responseContentFormat = ContentFormat.SENML_CBOR; // use CBOR as default
        if (coapRequest.options().getAccept() != null) {
            // If an request ask for a specific content format, use it (if we support it)
            responseContentFormat = ContentFormat.fromCode(coapRequest.options().getAccept());
            if (!toolbox.getEncoder().isSupported(responseContentFormat)) {
                return emptyResponse(ResponseCode.NOT_ACCEPTABLE);
            }
        }

        // Decode Path to read
        if (coapRequest.options().getContentFormat() == null) {
            return emptyResponse(ResponseCode.BAD_REQUEST);
        }
        ContentFormat requestContentFormat = ContentFormat.fromCode(coapRequest.options().getContentFormat());
        List<LwM2mPath> paths = toolbox.getDecoder().decodePaths(coapRequest.getPayload().getBytes(),
                requestContentFormat, null);

        if (coapRequest.options().getObserve() != null) {
            // TODO ideally we would like to to attach paths to the coapRequest to avoid to decode it twice :/

            // Manage Observe Composite request
            ObserveCompositeRequest observeRequest = new ObserveCompositeRequest(requestContentFormat,
                    responseContentFormat, paths, coapRequest);
            ObserveCompositeResponse response = requestReceiver.requestReceived(identity, observeRequest).getResponse();

            if (response.getCode() == ResponseCode.CONTENT) {
                return responseWithPayload( //
                        response.getCode(), //
                        responseContentFormat, //
                        toolbox.getEncoder().encodeNodes(response.getContent(), responseContentFormat, null,
                                toolbox.getModel()));
            } else {
                return errorMessage(response.getCode(), response.getErrorMessage());
            }
        } else {
            // Manage Read Composite request
            ReadCompositeResponse response = requestReceiver
                    .requestReceived(identity,
                            new ReadCompositeRequest(paths, requestContentFormat, responseContentFormat, coapRequest))
                    .getResponse();
            if (response.getCode() == ResponseCode.CONTENT) {
                return responseWithPayload( //
                        response.getCode(), //
                        responseContentFormat, //
                        toolbox.getEncoder().encodeNodes(response.getContent(), responseContentFormat, null,
                                toolbox.getModel()));
            } else {
                return errorMessage(response.getCode(), response.getErrorMessage());
            }
        }
    }

    @Override
    public CompletableFuture<CoapResponse> handleIPATCH(CoapRequest coapRequest) {
        // Get server identity
        LwM2mServer identity = extractIdentity(coapRequest);
        if (identity == null) {
            return unknownServer();
        }

        // Manage Write Composite request
        // Handle content format
        ContentFormat contentFormat = ContentFormat.fromCode(coapRequest.options().getContentFormat());
        if (!toolbox.getDecoder().isSupported(contentFormat)) {
            return emptyResponse(ResponseCode.UNSUPPORTED_CONTENT_FORMAT);
        }

        Map<LwM2mPath, LwM2mNode> nodes = toolbox.getDecoder().decodeNodes(coapRequest.getPayload().getBytes(),
                contentFormat, null, null, toolbox.getModel());
        WriteCompositeResponse response = requestReceiver
                .requestReceived(identity, new WriteCompositeRequest(contentFormat, nodes, coapRequest)).getResponse();
        if (response.getCode().isError()) {
            return errorMessage(response.getCode(), response.getErrorMessage());
        } else {
            return emptyResponse(response.getCode());
        }
    }

    @Override
    public CompletableFuture<CoapResponse> handleDELETE(CoapRequest coapRequest) {
        // Get server identity
        LwM2mServer identity = extractIdentity(coapRequest);
        if (identity == null) {
            return unknownServer();
        }

        BootstrapDeleteResponse response = requestReceiver
                .requestReceived(identity, new BootstrapDeleteRequest("/", coapRequest)).getResponse();
        if (response.getCode().isError()) {
            return errorMessage(response.getCode(), response.getErrorMessage());
        } else {
            return emptyResponse(response.getCode());
        }
    }
}
