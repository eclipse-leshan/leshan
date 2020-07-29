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
 *******************************************************************************/
package org.eclipse.leshan.client.californium.object;

import static org.eclipse.leshan.core.californium.ResponseCodeUtil.toCoapResponseCode;

import java.util.List;

import org.eclipse.californium.core.coap.CoAP.ResponseCode;
import org.eclipse.californium.core.coap.MediaTypeRegistry;
import org.eclipse.californium.core.server.resources.CoapExchange;
import org.eclipse.californium.core.server.resources.Resource;
import org.eclipse.leshan.client.californium.LwM2mClientCoapResource;
import org.eclipse.leshan.client.engine.RegistrationEngine;
import org.eclipse.leshan.client.resource.LwM2mObjectEnabler;
import org.eclipse.leshan.client.resource.listener.ObjectListener;
import org.eclipse.leshan.client.servers.ServerIdentity;
import org.eclipse.leshan.core.Link;
import org.eclipse.leshan.core.attributes.AttributeSet;
import org.eclipse.leshan.core.model.LwM2mModel;
import org.eclipse.leshan.core.model.StaticModel;
import org.eclipse.leshan.core.node.LwM2mNode;
import org.eclipse.leshan.core.node.LwM2mObject;
import org.eclipse.leshan.core.node.LwM2mObjectInstance;
import org.eclipse.leshan.core.node.LwM2mPath;
import org.eclipse.leshan.core.node.LwM2mResource;
import org.eclipse.leshan.core.node.codec.CodecException;
import org.eclipse.leshan.core.node.codec.LwM2mNodeDecoder;
import org.eclipse.leshan.core.node.codec.LwM2mNodeEncoder;
import org.eclipse.leshan.core.request.BootstrapDeleteRequest;
import org.eclipse.leshan.core.request.BootstrapDiscoverRequest;
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
import org.eclipse.leshan.core.response.BootstrapDeleteResponse;
import org.eclipse.leshan.core.response.BootstrapDiscoverResponse;
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

    protected final LwM2mObjectEnabler nodeEnabler;
    protected final LwM2mNodeEncoder encoder;
    protected final LwM2mNodeDecoder decoder;

    public ObjectResource(LwM2mObjectEnabler nodeEnabler, RegistrationEngine registrationEngine,
            LwM2mNodeEncoder encoder, LwM2mNodeDecoder decoder) {
        super(Integer.toString(nodeEnabler.getId()), registrationEngine);
        this.nodeEnabler = nodeEnabler;
        this.nodeEnabler.addListener(this);
        this.encoder = encoder;
        this.decoder = decoder;
        setObservable(true);
    }

    @Override
    public void handleGET(CoapExchange exchange) {
        ServerIdentity identity = getServerOrRejectRequest(exchange);
        if (identity == null)
            return;

        String URI = exchange.getRequestOptions().getUriPathString();

        if (exchange.getRequestOptions().getAccept() == MediaTypeRegistry.APPLICATION_LINK_FORMAT) {
            if (identity.isLwm2mBootstrapServer()) {
                // Manage Bootstrap Discover Request
                BootstrapDiscoverResponse response = nodeEnabler.discover(identity, new BootstrapDiscoverRequest(URI));
                if (response.getCode().isError()) {
                    exchange.respond(toCoapResponseCode(response.getCode()), response.getErrorMessage());
                } else {
                    exchange.respond(toCoapResponseCode(response.getCode()), Link.serialize(response.getObjectLinks()),
                            MediaTypeRegistry.APPLICATION_LINK_FORMAT);
                }
                return;
            } else {
                // Manage Discover Request
                DiscoverResponse response = nodeEnabler.discover(identity, new DiscoverRequest(URI));
                if (response.getCode().isError()) {
                    exchange.respond(toCoapResponseCode(response.getCode()), response.getErrorMessage());
                } else {
                    exchange.respond(toCoapResponseCode(response.getCode()), Link.serialize(response.getObjectLinks()),
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
                if (!encoder.isSupported(requestedContentFormat)) {
                    exchange.respond(ResponseCode.NOT_ACCEPTABLE);
                    return;
                }
            }

            // Manage Observe Request
            if (exchange.getRequestOptions().hasObserve()) {
                ObserveRequest observeRequest = new ObserveRequest(URI);
                ObserveResponse response = nodeEnabler.observe(identity, observeRequest);
                if (response.getCode() == org.eclipse.leshan.core.ResponseCode.CONTENT) {
                    LwM2mPath path = new LwM2mPath(URI);
                    LwM2mNode content = response.getContent();
                    LwM2mModel model = new StaticModel(nodeEnabler.getObjectModel());
                    ContentFormat format = getContentFormat(observeRequest, requestedContentFormat);
                    exchange.respond(ResponseCode.CONTENT, encoder.encode(content, format, path, model),
                            format.getCode());
                    return;
                } else {
                    exchange.respond(toCoapResponseCode(response.getCode()), response.getErrorMessage());
                    return;
                }
            }
            // Manage Read Request
            else {
                ReadRequest readRequest = new ReadRequest(URI);
                ReadResponse response = nodeEnabler.read(identity, readRequest);
                if (response.getCode() == org.eclipse.leshan.core.ResponseCode.CONTENT) {
                    LwM2mPath path = new LwM2mPath(URI);
                    LwM2mNode content = response.getContent();
                    LwM2mModel model = new StaticModel(nodeEnabler.getObjectModel());
                    ContentFormat format = getContentFormat(readRequest, requestedContentFormat);
                    exchange.respond(ResponseCode.CONTENT, encoder.encode(content, format, path, model),
                            format.getCode());
                    return;
                } else {
                    exchange.respond(toCoapResponseCode(response.getCode()), response.getErrorMessage());
                    return;
                }
            }
        }
    }

    protected ContentFormat getContentFormat(DownlinkRequest<?> request, ContentFormat requestedContentFormat) {
        if (requestedContentFormat != null) {
            // we already check before this content format is supported.
            return requestedContentFormat;
        }

        ContentFormat format = nodeEnabler.getDefaultEncodingFormat(request);
        return format == null ? ContentFormat.DEFAULT : format;
    }

    @Override
    public void handlePUT(CoapExchange coapExchange) {
        ServerIdentity identity = getServerOrRejectRequest(coapExchange);
        if (identity == null)
            return;

        String URI = coapExchange.getRequestOptions().getUriPathString();

        // get Observe Spec
        AttributeSet attributes = null;
        if (coapExchange.advanced().getRequest().getOptions().getURIQueryCount() != 0) {
            List<String> uriQueries = coapExchange.advanced().getRequest().getOptions().getUriQuery();
            attributes = AttributeSet.parse(uriQueries);
        }

        // Manage Write Attributes Request
        if (attributes != null) {
            WriteAttributesResponse response = nodeEnabler.writeAttributes(identity,
                    new WriteAttributesRequest(URI, attributes));
            if (response.getCode().isError()) {
                coapExchange.respond(toCoapResponseCode(response.getCode()), response.getErrorMessage());
            } else {
                coapExchange.respond(toCoapResponseCode(response.getCode()));
            }
            return;
        }
        // Manage Write and Bootstrap Write Request (replace)
        else {
            LwM2mPath path = new LwM2mPath(URI);

            if (!coapExchange.getRequestOptions().hasContentFormat()) {
                handleInvalidRequest(coapExchange, "Content Format is mandatory");
                return;
            }

            ContentFormat contentFormat = ContentFormat.fromCode(coapExchange.getRequestOptions().getContentFormat());
            if (!decoder.isSupported(contentFormat)) {
                coapExchange.respond(ResponseCode.UNSUPPORTED_CONTENT_FORMAT);
                return;
            }
            LwM2mNode lwM2mNode;
            try {
                LwM2mModel model = new StaticModel(nodeEnabler.getObjectModel());
                lwM2mNode = decoder.decode(coapExchange.getRequestPayload(), contentFormat, path, model);
                if (identity.isLwm2mBootstrapServer()) {
                    BootstrapWriteResponse response = nodeEnabler.write(identity,
                            new BootstrapWriteRequest(path, lwM2mNode, contentFormat));
                    if (response.getCode().isError()) {
                        coapExchange.respond(toCoapResponseCode(response.getCode()), response.getErrorMessage());
                    } else {
                        coapExchange.respond(toCoapResponseCode(response.getCode()));
                    }
                } else {
                    WriteResponse response = nodeEnabler.write(identity,
                            new WriteRequest(Mode.REPLACE, contentFormat, URI, lwM2mNode));
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
        ServerIdentity identity = getServerOrRejectRequest(exchange);
        if (identity == null)
            return;

        String URI = exchange.getRequestOptions().getUriPathString();

        LwM2mPath path = new LwM2mPath(URI);

        // Manage Execute Request
        if (path.isResource()) {
            byte[] payload = exchange.getRequestPayload();
            ExecuteResponse response = nodeEnabler.execute(identity,
                    new ExecuteRequest(URI, payload != null ? new String(payload) : null));
            if (response.getCode().isError()) {
                exchange.respond(toCoapResponseCode(response.getCode()), response.getErrorMessage());
            } else {
                exchange.respond(toCoapResponseCode(response.getCode()));
            }
            return;
        }

        // handle content format for Write (Update) and Create request
        if (!exchange.getRequestOptions().hasContentFormat()) {
            handleInvalidRequest(exchange, "Content Format is mandatory");
            return;
        }

        ContentFormat contentFormat = ContentFormat.fromCode(exchange.getRequestOptions().getContentFormat());
        if (!decoder.isSupported(contentFormat)) {
            exchange.respond(ResponseCode.UNSUPPORTED_CONTENT_FORMAT);
            return;
        }
        LwM2mModel model = new StaticModel(nodeEnabler.getObjectModel());

        // Manage Update Instance
        if (path.isObjectInstance()) {
            try {
                LwM2mNode lwM2mNode = decoder.decode(exchange.getRequestPayload(), contentFormat, path, model);
                WriteResponse response = nodeEnabler.write(identity,
                        new WriteRequest(Mode.UPDATE, contentFormat, URI, lwM2mNode));
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
            LwM2mObject object = decoder.decode(exchange.getRequestPayload(), contentFormat,
                    new LwM2mPath(path.getObjectId()), model, LwM2mObject.class);

            CreateRequest createRequest;
            // check if this is the "special" case where instance ID is not defined ...
            LwM2mObjectInstance newInstance = object.getInstance(LwM2mObjectInstance.UNDEFINED);
            if (object.getInstances().isEmpty()) {
                // This is probably the pretty strange use case where
                // instance ID is not defined an no resources available.
                createRequest = new CreateRequest(contentFormat, path.getObjectId(), new LwM2mResource[0]);
            } else if (object.getInstances().size() == 1 && newInstance != null) {
                // the instance Id was not part of the create request payload.
                // will be assigned by the client.
                createRequest = new CreateRequest(contentFormat, path.getObjectId(),
                        newInstance.getResources().values());
            } else {
                createRequest = new CreateRequest(contentFormat, path.getObjectId(), object.getInstances().values()
                        .toArray(new LwM2mObjectInstance[object.getInstances().values().size()]));
            }

            CreateResponse response = nodeEnabler.create(identity, createRequest);
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
        ServerIdentity identity = getServerOrRejectRequest(coapExchange);
        if (identity == null)
            return;

        if (identity.isLwm2mBootstrapServer()) {
            BootstrapDeleteResponse response = nodeEnabler.delete(identity, new BootstrapDeleteRequest(URI));
            if (response.getCode().isError()) {
                coapExchange.respond(toCoapResponseCode(response.getCode()), response.getErrorMessage());
            } else {
                coapExchange.respond(toCoapResponseCode(response.getCode()));
            }
        } else {
            DeleteResponse response = nodeEnabler.delete(identity, new DeleteRequest(URI));
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
    public void resourceChanged(LwM2mObjectEnabler object, int instanceId, int... resourceIds) {
        // notify CoAP layer than resources changes, this will send observe notification if an observe relationship
        // exits.
        changed(new ResourceObserveFilter(object.getId() + ""));
        changed(new ResourceObserveFilter(object.getId() + "/" + instanceId));
        for (int resourceId : resourceIds) {
            changed(new ResourceObserveFilter(object.getId() + "/" + instanceId + "/" + resourceId));
        }
    }

    @Override
    public void objectInstancesAdded(LwM2mObjectEnabler object, int... instanceIds) {
    }

    @Override
    public void objectInstancesRemoved(LwM2mObjectEnabler object, int... instanceIds) {
    }
}
