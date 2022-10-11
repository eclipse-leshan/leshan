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
 *     Achim Kraus (Bosch Software Innovations GmbH) - use ObserveRelationFilter
 *     Achim Kraus (Bosch Software Innovations GmbH) - use ServerIdentity
 *     Achim Kraus (Bosch Software Innovations GmbH) - implement POST "/oid/iid"
 *                                                     as UPDATE instance
 *     Michał Wadowski (Orange)                      - Add Observe-Composite feature.
 *     Michał Wadowski (Orange)                      - Improved compliance with rfc6690.
 *******************************************************************************/
package org.eclipse.leshan.client.californium.object;

import static org.eclipse.leshan.core.californium.ResponseCodeUtil.toCoapResponseCode;

import java.util.List;

import org.eclipse.californium.core.coap.CoAP.ResponseCode;
import org.eclipse.californium.core.coap.MediaTypeRegistry;
import org.eclipse.californium.core.coap.Request;
import org.eclipse.californium.core.server.resources.CoapExchange;
import org.eclipse.californium.core.server.resources.Resource;
import org.eclipse.leshan.client.californium.LwM2mClientCoapResource;
import org.eclipse.leshan.client.californium.endpoint.ServerIdentityExtractor;
import org.eclipse.leshan.client.endpoint.ClientEndpointToolbox;
import org.eclipse.leshan.client.request.DownlinkRequestReceiver;
import org.eclipse.leshan.client.resource.LwM2mObjectEnabler;
import org.eclipse.leshan.client.resource.listener.ObjectListener;
import org.eclipse.leshan.client.servers.ServerIdentity;
import org.eclipse.leshan.core.californium.identity.IdentityHandlerProvider;
import org.eclipse.leshan.core.link.attributes.InvalidAttributeException;
import org.eclipse.leshan.core.link.lwm2m.attributes.LwM2mAttributeSet;
import org.eclipse.leshan.core.node.InvalidLwM2mPathException;
import org.eclipse.leshan.core.node.LwM2mNode;
import org.eclipse.leshan.core.node.LwM2mObject;
import org.eclipse.leshan.core.node.LwM2mObjectInstance;
import org.eclipse.leshan.core.node.LwM2mPath;
import org.eclipse.leshan.core.node.LwM2mResource;
import org.eclipse.leshan.core.node.codec.CodecException;
import org.eclipse.leshan.core.request.BootstrapDeleteRequest;
import org.eclipse.leshan.core.request.BootstrapDiscoverRequest;
import org.eclipse.leshan.core.request.BootstrapReadRequest;
import org.eclipse.leshan.core.request.BootstrapWriteRequest;
import org.eclipse.leshan.core.request.ContentFormat;
import org.eclipse.leshan.core.request.CreateRequest;
import org.eclipse.leshan.core.request.DeleteRequest;
import org.eclipse.leshan.core.request.DiscoverRequest;
import org.eclipse.leshan.core.request.DownlinkRequest;
import org.eclipse.leshan.core.request.ExecuteRequest;
import org.eclipse.leshan.core.request.ObserveRequest;
import org.eclipse.leshan.core.request.ReadRequest;
import org.eclipse.leshan.core.request.WriteAttributesRequest;
import org.eclipse.leshan.core.request.WriteRequest;
import org.eclipse.leshan.core.request.WriteRequest.Mode;
import org.eclipse.leshan.core.request.exception.InvalidRequestException;
import org.eclipse.leshan.core.response.BootstrapDeleteResponse;
import org.eclipse.leshan.core.response.BootstrapDiscoverResponse;
import org.eclipse.leshan.core.response.BootstrapReadResponse;
import org.eclipse.leshan.core.response.BootstrapWriteResponse;
import org.eclipse.leshan.core.response.CreateResponse;
import org.eclipse.leshan.core.response.DeleteResponse;
import org.eclipse.leshan.core.response.DiscoverResponse;
import org.eclipse.leshan.core.response.ExecuteResponse;
import org.eclipse.leshan.core.response.ObserveResponse;
import org.eclipse.leshan.core.response.ReadResponse;
import org.eclipse.leshan.core.response.WriteAttributesResponse;
import org.eclipse.leshan.core.response.WriteResponse;

/**
 * A CoAP {@link Resource} in charge of handling requests targeting a lwM2M Object.
 */
public class ObjectResource extends LwM2mClientCoapResource implements ObjectListener {

    protected DownlinkRequestReceiver requestReceiver;
    protected ClientEndpointToolbox toolbox;

    public ObjectResource(int objectId, IdentityHandlerProvider identityHandlerProvider,
            ServerIdentityExtractor serverIdentityExtractor, DownlinkRequestReceiver requestReceiver,
            ClientEndpointToolbox toolbox) {
        super(Integer.toString(objectId), identityHandlerProvider, serverIdentityExtractor);
        this.requestReceiver = requestReceiver;
        this.toolbox = toolbox;
        setObservable(true);
    }

    @Override
    public void handleGET(CoapExchange exchange) {
        Request coapRequest = exchange.advanced().getRequest();
        ServerIdentity identity = getServerOrRejectRequest(exchange, coapRequest);
        if (identity == null)
            return;

        String URI = exchange.getRequestOptions().getUriPathString();

        if (exchange.getRequestOptions().getAccept() == MediaTypeRegistry.APPLICATION_LINK_FORMAT) {
            if (identity.isLwm2mBootstrapServer()) {
                // Manage Bootstrap Discover Request
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
            } else {
                // Manage Discover Request
                DiscoverResponse response = requestReceiver
                        .requestReceived(identity, new DiscoverRequest(URI, coapRequest)).getResponse();
                if (response.getCode().isError()) {
                    exchange.respond(toCoapResponseCode(response.getCode()), response.getErrorMessage());
                } else {
                    exchange.respond(toCoapResponseCode(response.getCode()),
                            toolbox.getLinkSerializer().serializeCoreLinkFormat(response.getObjectLinks()),
                            MediaTypeRegistry.APPLICATION_LINK_FORMAT);
                }
                return;
            }
        } else {
            // handle content format for Read and Observe Request
            ContentFormat requestedContentFormat = null;
            if (exchange.getRequestOptions().hasAccept()) {
                // If an request ask for a specific content format, use it (if we support it)
                requestedContentFormat = ContentFormat.fromCode(exchange.getRequestOptions().getAccept());
                if (!toolbox.getEncoder().isSupported(requestedContentFormat)) {
                    exchange.respond(ResponseCode.NOT_ACCEPTABLE);
                    return;
                }
            }

            // Manage Observe Request
            if (exchange.getRequestOptions().hasObserve()) {
                ObserveRequest observeRequest = new ObserveRequest(requestedContentFormat, URI, coapRequest);
                ObserveResponse response = requestReceiver.requestReceived(identity, observeRequest).getResponse();
                if (response.getCode() == org.eclipse.leshan.core.ResponseCode.CONTENT) {
                    LwM2mPath path = getPath(URI);
                    LwM2mNode content = response.getContent();
                    ContentFormat format = getContentFormat(observeRequest, requestedContentFormat);
                    exchange.respond(ResponseCode.CONTENT,
                            toolbox.getEncoder().encode(content, format, path, toolbox.getModel()), format.getCode());
                    return;
                } else {
                    exchange.respond(toCoapResponseCode(response.getCode()), response.getErrorMessage());
                    return;
                }
            } else {
                if (identity.isLwm2mBootstrapServer()) {
                    // Manage Bootstrap Read Request
                    BootstrapReadRequest readRequest = new BootstrapReadRequest(requestedContentFormat, URI,
                            coapRequest);
                    BootstrapReadResponse response = requestReceiver.requestReceived(identity, readRequest)
                            .getResponse();
                    if (response.getCode() == org.eclipse.leshan.core.ResponseCode.CONTENT) {
                        LwM2mPath path = getPath(URI);
                        LwM2mNode content = response.getContent();
                        ContentFormat format = getContentFormat(readRequest, requestedContentFormat);
                        exchange.respond(ResponseCode.CONTENT,
                                toolbox.getEncoder().encode(content, format, path, toolbox.getModel()),
                                format.getCode());
                        return;
                    } else {
                        exchange.respond(toCoapResponseCode(response.getCode()), response.getErrorMessage());
                        return;
                    }
                } else {
                    // Manage Read Request
                    ReadRequest readRequest = new ReadRequest(requestedContentFormat, URI, coapRequest);
                    ReadResponse response = requestReceiver.requestReceived(identity, readRequest).getResponse();
                    if (response.getCode() == org.eclipse.leshan.core.ResponseCode.CONTENT) {
                        LwM2mPath path = getPath(URI);
                        LwM2mNode content = response.getContent();
                        ContentFormat format = getContentFormat(readRequest, requestedContentFormat);
                        exchange.respond(ResponseCode.CONTENT,
                                toolbox.getEncoder().encode(content, format, path, toolbox.getModel()),
                                format.getCode());
                        return;
                    } else {
                        exchange.respond(toCoapResponseCode(response.getCode()), response.getErrorMessage());
                        return;
                    }
                }
            }
        }
    }

    protected ContentFormat getContentFormat(DownlinkRequest<?> request, ContentFormat requestedContentFormat) {
        if (requestedContentFormat != null) {
            // we already check before this content format is supported.
            return requestedContentFormat;
        }

        // TODO TL : should we keep this feature ?
        // ContentFormat format = nodeEnabler.getDefaultEncodingFormat(request);
        // return format == null ? ContentFormat.DEFAULT : format;

        return ContentFormat.DEFAULT;
    }

    @Override
    public void handlePUT(CoapExchange coapExchange) {
        Request coapRequest = coapExchange.advanced().getRequest();
        ServerIdentity identity = getServerOrRejectRequest(coapExchange, coapRequest);
        if (identity == null)
            return;

        String URI = coapExchange.getRequestOptions().getUriPathString();

        // get Observe Spec
        LwM2mAttributeSet attributes = null;
        if (coapRequest.getOptions().getURIQueryCount() != 0) {
            List<String> uriQueries = coapRequest.getOptions().getUriQuery();
            try {
                attributes = new LwM2mAttributeSet(toolbox.getAttributeParser().parseQueryParams(uriQueries));
            } catch (InvalidAttributeException e) {
                handleInvalidRequest(coapExchange.advanced(), "Unable to parse Attributes", e);
            }
        }

        // Manage Write Attributes Request
        if (attributes != null) {
            WriteAttributesResponse response = requestReceiver
                    .requestReceived(identity, new WriteAttributesRequest(URI, attributes, coapRequest)).getResponse();
            if (response.getCode().isError()) {
                coapExchange.respond(toCoapResponseCode(response.getCode()), response.getErrorMessage());
            } else {
                coapExchange.respond(toCoapResponseCode(response.getCode()));
            }
            return;
        }
        // Manage Write and Bootstrap Write Request (replace)
        else {
            LwM2mPath path = getPath(URI);

            if (!coapExchange.getRequestOptions().hasContentFormat()) {
                handleInvalidRequest(coapExchange, "Content Format is mandatory");
                return;
            }

            ContentFormat contentFormat = ContentFormat.fromCode(coapExchange.getRequestOptions().getContentFormat());
            if (!toolbox.getDecoder().isSupported(contentFormat)) {
                coapExchange.respond(ResponseCode.UNSUPPORTED_CONTENT_FORMAT);
                return;
            }
            LwM2mNode lwM2mNode;
            try {
                lwM2mNode = toolbox.getDecoder().decode(coapExchange.getRequestPayload(), contentFormat, path,
                        toolbox.getModel());
                if (identity.isLwm2mBootstrapServer()) {
                    BootstrapWriteResponse response = requestReceiver
                            .requestReceived(identity,
                                    new BootstrapWriteRequest(path, lwM2mNode, contentFormat, coapRequest))
                            .getResponse();
                    if (response.getCode().isError()) {
                        coapExchange.respond(toCoapResponseCode(response.getCode()), response.getErrorMessage());
                    } else {
                        coapExchange.respond(toCoapResponseCode(response.getCode()));
                    }
                } else {
                    WriteResponse response = requestReceiver
                            .requestReceived(identity,
                                    new WriteRequest(Mode.REPLACE, contentFormat, URI, lwM2mNode, coapRequest))
                            .getResponse();
                    if (response.getCode().isError()) {
                        coapExchange.respond(toCoapResponseCode(response.getCode()), response.getErrorMessage());
                    } else {
                        coapExchange.respond(toCoapResponseCode(response.getCode()));
                    }
                }

                return;
            } catch (CodecException e) {
                handleInvalidRequest(coapExchange.advanced(), "Unable to decode payload on WRITE", e);
                return;
            }

        }
    }

    @Override
    public void handlePOST(CoapExchange exchange) {
        Request coapRequest = exchange.advanced().getRequest();
        ServerIdentity identity = getServerOrRejectRequest(exchange, coapRequest);
        if (identity == null)
            return;

        String URI = exchange.getRequestOptions().getUriPathString();
        LwM2mPath path = getPath(URI);

        // Manage Execute Request
        if (path.isResource()) {
            // execute request has no content format at all or a TEXT concent format for parameters.
            if (!exchange.getRequestOptions().hasContentFormat()
                    || ContentFormat.fromCode(exchange.getRequestOptions().getContentFormat()) == ContentFormat.TEXT) {
                byte[] payload = exchange.getRequestPayload();

                ExecuteResponse response = requestReceiver
                        .requestReceived(identity,
                                new ExecuteRequest(URI, payload != null ? new String(payload) : null, coapRequest))
                        .getResponse();
                if (response.getCode().isError()) {
                    exchange.respond(toCoapResponseCode(response.getCode()), response.getErrorMessage());
                } else {
                    exchange.respond(toCoapResponseCode(response.getCode()));
                }
                return;
            }
        }

        // handle content format for Write (Update) and Create request
        if (!exchange.getRequestOptions().hasContentFormat()) {
            handleInvalidRequest(exchange, "Content Format is mandatory");
            return;
        }

        ContentFormat contentFormat = ContentFormat.fromCode(exchange.getRequestOptions().getContentFormat());
        if (!toolbox.getDecoder().isSupported(contentFormat)) {
            exchange.respond(ResponseCode.UNSUPPORTED_CONTENT_FORMAT);
            return;
        }

        // manage partial update of multi-instance resource
        if (path.isResource()) {
            try {
                LwM2mNode lwM2mNode = toolbox.getDecoder().decode(exchange.getRequestPayload(), contentFormat, path,
                        toolbox.getModel());
                WriteResponse response = requestReceiver
                        .requestReceived(identity,
                                new WriteRequest(Mode.UPDATE, contentFormat, URI, lwM2mNode, coapRequest))
                        .getResponse();
                if (response.getCode().isError()) {
                    exchange.respond(toCoapResponseCode(response.getCode()), response.getErrorMessage());
                } else {
                    exchange.respond(toCoapResponseCode(response.getCode()));
                }
            } catch (CodecException e) {
                handleInvalidRequest(exchange.advanced(), "Unable to decode payload on WRITE", e);
            }
            return;
        }
        // Manage Update Instance
        if (path.isObjectInstance()) {
            try {
                LwM2mNode lwM2mNode = toolbox.getDecoder().decode(exchange.getRequestPayload(), contentFormat, path,
                        toolbox.getModel());
                WriteResponse response = requestReceiver
                        .requestReceived(identity,
                                new WriteRequest(Mode.UPDATE, contentFormat, URI, lwM2mNode, coapRequest))
                        .getResponse();
                if (response.getCode().isError()) {
                    exchange.respond(toCoapResponseCode(response.getCode()), response.getErrorMessage());
                } else {
                    exchange.respond(toCoapResponseCode(response.getCode()));
                }
            } catch (CodecException e) {
                handleInvalidRequest(exchange.advanced(), "Unable to decode payload on WRITE", e);
            }
            return;
        }

        // Manage Create Request
        try {
            // decode the payload as an instance
            LwM2mObject object = toolbox.getDecoder().decode(exchange.getRequestPayload(), contentFormat,
                    new LwM2mPath(path.getObjectId()), toolbox.getModel(), LwM2mObject.class);

            CreateRequest createRequest;
            // check if this is the "special" case where instance ID is not defined ...
            LwM2mObjectInstance newInstance = object.getInstance(LwM2mObjectInstance.UNDEFINED);
            if (object.getInstances().isEmpty()) {
                // This is probably the pretty strange use case where
                // instance ID is not defined an no resources available.
                createRequest = new CreateRequest(contentFormat, coapRequest, URI, new LwM2mResource[0]);
            } else if (object.getInstances().size() == 1 && newInstance != null) {
                // the instance Id was not part of the create request payload.
                // will be assigned by the client.
                createRequest = new CreateRequest(contentFormat, coapRequest, URI, newInstance.getResources().values());
            } else {
                createRequest = new CreateRequest(contentFormat, coapRequest, URI, object.getInstances().values()
                        .toArray(new LwM2mObjectInstance[object.getInstances().values().size()]));
            }

            CreateResponse response = requestReceiver.requestReceived(identity, createRequest).getResponse();
            if (response.getCode() == org.eclipse.leshan.core.ResponseCode.CREATED) {
                if (response.getLocation() != null)
                    exchange.setLocationPath(response.getLocation());
                exchange.respond(toCoapResponseCode(response.getCode()));
                return;
            } else {
                exchange.respond(toCoapResponseCode(response.getCode()), response.getErrorMessage());
                return;
            }
        } catch (CodecException e) {
            handleInvalidRequest(exchange.advanced(), "Unable to decode payload on CREATE", e);
            return;
        }
    }

    @Override
    public void handleDELETE(CoapExchange coapExchange) {
        // Manage Delete Request
        String URI = coapExchange.getRequestOptions().getUriPathString();
        Request coapRequest = coapExchange.advanced().getRequest();
        ServerIdentity identity = getServerOrRejectRequest(coapExchange, coapRequest);
        if (identity == null)
            return;

        if (identity.isLwm2mBootstrapServer()) {
            BootstrapDeleteResponse response = requestReceiver
                    .requestReceived(identity, new BootstrapDeleteRequest(URI, coapRequest)).getResponse();
            if (response.getCode().isError()) {
                coapExchange.respond(toCoapResponseCode(response.getCode()), response.getErrorMessage());
            } else {
                coapExchange.respond(toCoapResponseCode(response.getCode()));
            }
        } else {
            DeleteResponse response = requestReceiver.requestReceived(identity, new DeleteRequest(URI, coapRequest))
                    .getResponse();
            if (response.getCode().isError()) {
                coapExchange.respond(toCoapResponseCode(response.getCode()), response.getErrorMessage());
            } else {
                coapExchange.respond(toCoapResponseCode(response.getCode()));
            }
        }
    }

    /*
     * Override the default behavior so that requests to sub resources (typically /ObjectId/*) are handled by this
     * resource.
     */
    @Override
    public Resource getChild(String name) {
        return this;
    }

    @Override
    public void resourceChanged(LwM2mPath... paths) {
        // notify CoAP layer than resources changes, this will send observe notification if an observe relationship
        // exits.
        changed(new ResourceObserveFilter(paths));
    }

    @Override
    public void objectInstancesAdded(LwM2mObjectEnabler object, int... instanceIds) {
    }

    @Override
    public void objectInstancesRemoved(LwM2mObjectEnabler object, int... instanceIds) {
    }

    protected LwM2mPath getPath(String URI) throws InvalidRequestException {
        try {
            return new LwM2mPath(URI);
        } catch (InvalidLwM2mPathException e) {
            throw new InvalidRequestException(e, "Invalid path : %s", e.getMessage());
        }
    }
}
