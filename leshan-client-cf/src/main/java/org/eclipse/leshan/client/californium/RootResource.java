/*******************************************************************************
 * Copyright (c) 2015 Sierra Wireless and others.
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
 *     Achim Kraus (Bosch Software Innovations GmbH) - use ServerIdentity
 *     Michał Wadowski (Orange) - Add Observe-Composite feature.
 *     Michał Wadowski (Orange) - Improved compliance with rfc6690.
 *******************************************************************************/
package org.eclipse.leshan.client.californium;

import static org.eclipse.leshan.core.californium.ResponseCodeUtil.toCoapResponseCode;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.californium.core.CoapResource;
import org.eclipse.californium.core.CoapServer;
import org.eclipse.californium.core.coap.CoAP.ResponseCode;
import org.eclipse.californium.core.coap.MediaTypeRegistry;
import org.eclipse.californium.core.coap.Request;
import org.eclipse.californium.core.server.resources.CoapExchange;
import org.eclipse.leshan.client.californium.endpoint.ServerIdentityExtractor;
import org.eclipse.leshan.client.endpoint.ClientEndpointToolbox;
import org.eclipse.leshan.client.request.DownlinkRequestReceiver;
import org.eclipse.leshan.client.servers.ServerIdentity;
import org.eclipse.leshan.core.californium.ObserveUtil;
import org.eclipse.leshan.core.californium.identity.IdentityHandlerProvider;
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
import org.eclipse.leshan.core.util.StringUtils;

/**
 * A root {@link CoapResource} resource in charge of handling Bootstrap Delete requests targeting the "/" URI.
 */
public class RootResource extends LwM2mClientCoapResource {

    protected DownlinkRequestReceiver requestReceiver;
    protected ClientEndpointToolbox toolbox;

    public RootResource(IdentityHandlerProvider identityHandlerProvider,
            ServerIdentityExtractor serverIdentityExtractor, CoapServer coapServer,
            DownlinkRequestReceiver requestReceiver, ClientEndpointToolbox toolbox) {
        super("", identityHandlerProvider, serverIdentityExtractor);
        setVisible(false);
        setObservable(true);
        this.requestReceiver = requestReceiver;
        this.toolbox = toolbox;
    }

    @Override
    public void handleGET(CoapExchange exchange) {
        // Manage Bootstrap Discover Request
        Request coapRequest = exchange.advanced().getRequest();
        ServerIdentity identity = getServerOrRejectRequest(exchange, coapRequest);
        if (identity == null)
            return;

        String URI = exchange.getRequestOptions().getUriPathString();

        BootstrapDiscoverResponse response = requestReceiver
                .requestReceived(identity, new BootstrapDiscoverRequest(URI, coapRequest)).getResponse();
        if (response.getCode().isError()) {
            exchange.respond(toCoapResponseCode(response.getCode()), response.getErrorMessage());
        } else {
            exchange.respond(toCoapResponseCode(response.getCode()),
                    toolbox.getLinkSerializer().serializeCoreLinkFormat(response.getObjectLinks()),
                    MediaTypeRegistry.APPLICATION_LINK_FORMAT);
        }
        return;
    }

    @Override
    public void handleFETCH(CoapExchange exchange) {
        Request coapRequest = exchange.advanced().getRequest();
        ServerIdentity identity = getServerOrRejectRequest(exchange, coapRequest);
        if (identity == null)
            return;

        // Handle content format for the response
        ContentFormat responseContentFormat = ContentFormat.SENML_CBOR; // use CBOR as default
        if (exchange.getRequestOptions().hasAccept()) {
            // If an request ask for a specific content format, use it (if we support it)
            responseContentFormat = ContentFormat.fromCode(exchange.getRequestOptions().getAccept());
            if (!toolbox.getEncoder().isSupported(responseContentFormat)) {
                exchange.respond(ResponseCode.NOT_ACCEPTABLE);
                return;
            }
        }

        // Decode Path to read
        if (!exchange.getRequestOptions().hasContentFormat()) {
            exchange.respond(ResponseCode.BAD_REQUEST);
            return;
        }
        ContentFormat requestContentFormat = ContentFormat.fromCode(exchange.getRequestOptions().getContentFormat());
        List<LwM2mPath> paths = toolbox.getDecoder().decodePaths(coapRequest.getPayload(), requestContentFormat);

        if (exchange.getRequestOptions().hasObserve()) {
            // Manage Observe Composite request
            ObserveCompositeRequest observeRequest = new ObserveCompositeRequest(requestContentFormat,
                    responseContentFormat, paths, coapRequest);
            ObserveCompositeResponse response = requestReceiver.requestReceived(identity, observeRequest).getResponse();

            updateUserContextWithPaths(coapRequest, paths);

            if (response.getCode().isError()) {
                exchange.respond(toCoapResponseCode(response.getCode()), response.getErrorMessage());
                return;
            } else {
                exchange.respond(toCoapResponseCode(response.getCode()), toolbox.getEncoder()
                        .encodeNodes(response.getContent(), responseContentFormat, toolbox.getModel()),
                        responseContentFormat.getCode());
                return;
            }
        } else {
            // Manage Read Composite request
            ReadCompositeResponse response = requestReceiver
                    .requestReceived(identity,
                            new ReadCompositeRequest(paths, requestContentFormat, responseContentFormat, coapRequest))
                    .getResponse();
            if (response.getCode().isError()) {
                exchange.respond(toCoapResponseCode(response.getCode()), response.getErrorMessage());
            } else {
                // TODO we could maybe face some race condition if an objectEnabler is removed from LwM2mObjectTree
                // between rootEnabler.read() and rootEnabler.getModel()
                exchange.respond(toCoapResponseCode(response.getCode()), toolbox.getEncoder()
                        .encodeNodes(response.getContent(), responseContentFormat, toolbox.getModel()),
                        responseContentFormat.getCode());
            }
            return;
        }
    }

    private void updateUserContextWithPaths(Request coapRequest, List<LwM2mPath> paths) {
        HashMap<String, String> userContext = new HashMap<>();
        if (coapRequest.getUserContext() != null) {
            userContext.putAll(coapRequest.getUserContext());
        }
        ObserveUtil.addPathsIntoContext(userContext, paths);
        coapRequest.setUserContext(userContext);
    }

    @Override
    public void handleIPATCH(CoapExchange exchange) {
        // Manage Read Composite request
        Request coapRequest = exchange.advanced().getRequest();
        ServerIdentity identity = getServerOrRejectRequest(exchange, coapRequest);
        if (identity == null)
            return;

        // Handle content format
        ContentFormat contentFormat = ContentFormat.fromCode(exchange.getRequestOptions().getContentFormat());
        if (!toolbox.getDecoder().isSupported(contentFormat)) {
            exchange.respond(ResponseCode.UNSUPPORTED_CONTENT_FORMAT);
            return;
        }

        Map<LwM2mPath, LwM2mNode> nodes = toolbox.getDecoder().decodeNodes(coapRequest.getPayload(), contentFormat,
                null, toolbox.getModel());

        WriteCompositeResponse response = requestReceiver
                .requestReceived(identity, new WriteCompositeRequest(contentFormat, nodes, coapRequest)).getResponse();
        if (response.getCode().isError()) {
            exchange.respond(toCoapResponseCode(response.getCode()), response.getErrorMessage());
        } else {
            exchange.respond(toCoapResponseCode(response.getCode()));
        }
        return;
    }

    @Override
    public void handleDELETE(CoapExchange exchange) {
        String URI = exchange.getRequestOptions().getUriPathString();
        if (!StringUtils.isEmpty(URI)) {
            exchange.respond(ResponseCode.METHOD_NOT_ALLOWED);
            return;
        }

        Request coapRequest = exchange.advanced().getRequest();
        ServerIdentity identity = getServerOrRejectRequest(exchange, coapRequest);
        if (identity == null)
            return;

        BootstrapDeleteResponse response = requestReceiver
                .requestReceived(identity, new BootstrapDeleteRequest(URI, coapRequest)).getResponse();
        exchange.respond(toCoapResponseCode(response.getCode()), response.getErrorMessage());
    }
}
