/*******************************************************************************
 * Copyright (c) 2015 Sierra Wireless and others.
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
package org.eclipse.leshan.client.californium.impl;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.eclipse.californium.core.CoapResource;
import org.eclipse.californium.core.coap.CoAP.ResponseCode;
import org.eclipse.californium.core.coap.LinkFormat;
import org.eclipse.californium.core.coap.MediaTypeRegistry;
import org.eclipse.californium.core.coap.Response;
import org.eclipse.californium.core.network.Exchange;
import org.eclipse.californium.core.observe.ObserveRelation;
import org.eclipse.californium.core.observe.ObserveRelationContainer;
import org.eclipse.californium.core.server.resources.CoapExchange;
import org.eclipse.californium.core.server.resources.Resource;
import org.eclipse.leshan.LinkObject;
import org.eclipse.leshan.ObserveSpec;
import org.eclipse.leshan.client.resource.LinkFormattable;
import org.eclipse.leshan.client.resource.LwM2mObjectEnabler;
import org.eclipse.leshan.client.resource.NotifySender;
import org.eclipse.leshan.client.util.ObserveSpecParser;
import org.eclipse.leshan.core.model.LwM2mModel;
import org.eclipse.leshan.core.node.LwM2mNode;
import org.eclipse.leshan.core.node.LwM2mObject;
import org.eclipse.leshan.core.node.LwM2mObjectInstance;
import org.eclipse.leshan.core.node.LwM2mPath;
import org.eclipse.leshan.core.node.LwM2mResource;
import org.eclipse.leshan.core.node.codec.InvalidValueException;
import org.eclipse.leshan.core.node.codec.LwM2mNodeDecoder;
import org.eclipse.leshan.core.node.codec.LwM2mNodeEncoder;
import org.eclipse.leshan.core.request.ContentFormat;
import org.eclipse.leshan.core.request.ContentFormatHelper;
import org.eclipse.leshan.core.request.CreateRequest;
import org.eclipse.leshan.core.request.DeleteRequest;
import org.eclipse.leshan.core.request.DiscoverRequest;
import org.eclipse.leshan.core.request.ExecuteRequest;
import org.eclipse.leshan.core.request.ObserveRequest;
import org.eclipse.leshan.core.request.ReadRequest;
import org.eclipse.leshan.core.request.WriteAttributesRequest;
import org.eclipse.leshan.core.request.WriteRequest;
import org.eclipse.leshan.core.request.WriteRequest.Mode;
import org.eclipse.leshan.core.response.CreateResponse;
import org.eclipse.leshan.core.response.DeleteResponse;
import org.eclipse.leshan.core.response.DiscoverResponse;
import org.eclipse.leshan.core.response.ExecuteResponse;
import org.eclipse.leshan.core.response.ObserveResponse;
import org.eclipse.leshan.core.response.ReadResponse;
import org.eclipse.leshan.core.response.WriteAttributesResponse;
import org.eclipse.leshan.core.response.WriteResponse;
import org.eclipse.leshan.util.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A CoAP {@link Resource} in charge of handling requests for of a lwM2M Object.
 */
public class ObjectResource extends CoapResource implements LinkFormattable, NotifySender {

    private static final Logger LOG = LoggerFactory.getLogger(ObjectResource.class);

    private LwM2mObjectEnabler nodeEnabler;

    public ObjectResource(LwM2mObjectEnabler nodeEnabler) {
        super(Integer.toString(nodeEnabler.getId()));
        this.nodeEnabler = nodeEnabler;
        this.nodeEnabler.setNotifySender(this);
        setObservable(true);
    }

    @Override
    public void handleRequest(Exchange exchange) {
        try {
            super.handleRequest(exchange);
        } catch (Exception e) {
            LOG.error(String.format("Exception while handling a request on the %s resource", getURI()), e);
            exchange.sendResponse(new Response(ResponseCode.INTERNAL_SERVER_ERROR));
        }
    }

    @Override
    public void handleGET(CoapExchange exchange) {
        String URI = exchange.getRequestOptions().getUriPathString();

        // Manage Discover Request
        if (exchange.getRequestOptions().getAccept() == MediaTypeRegistry.APPLICATION_LINK_FORMAT) {
            DiscoverResponse response = nodeEnabler.discover(new DiscoverRequest(URI));
            if (response.getCode().isError()) {
                exchange.respond(fromLwM2mCode(response.getCode()), response.getErrorMessage());
            } else {
                exchange.respond(fromLwM2mCode(response.getCode()), LinkObject.serialyse(response.getObjectLinks()),
                        MediaTypeRegistry.APPLICATION_LINK_FORMAT);
            }
        }
        // Manage Observe Request
        else if (exchange.getRequestOptions().hasObserve()) {
            ObserveResponse response = nodeEnabler.observe(new ObserveRequest(URI));
            if (response.getCode() == org.eclipse.leshan.ResponseCode.CONTENT) {
                LwM2mPath path = new LwM2mPath(URI);
                LwM2mNode content = response.getContent();
                LwM2mModel model = new LwM2mModel(nodeEnabler.getObjectModel());
                ContentFormat contentFormat = ContentFormatHelper.compute(path, content, model);
                exchange.respond(ResponseCode.CONTENT, LwM2mNodeEncoder.encode(content, contentFormat, path, model));
                return;
            } else {
                exchange.respond(fromLwM2mCode(response.getCode()), response.getErrorMessage());
                return;
            }
        }
        // Manage Read Request
        else {
            ReadResponse response = nodeEnabler.read(new ReadRequest(URI));
            if (response.getCode() == org.eclipse.leshan.ResponseCode.CONTENT) {
                LwM2mPath path = new LwM2mPath(URI);
                LwM2mNode content = response.getContent();
                LwM2mModel model = new LwM2mModel(nodeEnabler.getObjectModel());
                ContentFormat contentFormat = ContentFormatHelper.compute(path, content, model);
                exchange.respond(ResponseCode.CONTENT, LwM2mNodeEncoder.encode(content, contentFormat, path, model));
                return;
            } else {
                exchange.respond(fromLwM2mCode(response.getCode()), response.getErrorMessage());
                return;
            }
        }
    }

    @Override
    public void handlePUT(final CoapExchange coapExchange) {
        String URI = coapExchange.getRequestOptions().getUriPathString();

        // get Observe Spec
        ObserveSpec spec = null;
        if (coapExchange.advanced().getRequest().getOptions().getURIQueryCount() != 0) {
            final List<String> uriQueries = coapExchange.advanced().getRequest().getOptions().getUriQuery();
            spec = ObserveSpecParser.parse(uriQueries);
        }

        // Manage Write Attributes Request
        if (spec != null) {
            WriteAttributesResponse response = nodeEnabler.writeAttributes(new WriteAttributesRequest(URI, spec));
            coapExchange.respond(fromLwM2mCode(response.getCode()), response.getErrorMessage());
            return;
        }
        // Manage Write Request (replace)
        else {
            LwM2mPath path = new LwM2mPath(URI);
            ContentFormat contentFormat = ContentFormat.fromCode(coapExchange.getRequestOptions().getContentFormat());
            LwM2mNode lwM2mNode;
            try {
                LwM2mModel model = new LwM2mModel(nodeEnabler.getObjectModel());
                lwM2mNode = LwM2mNodeDecoder.decode(coapExchange.getRequestPayload(), contentFormat, path, model);
                WriteResponse response = nodeEnabler
                        .write(new WriteRequest(Mode.REPLACE, contentFormat, URI, lwM2mNode));
                coapExchange.respond(fromLwM2mCode(response.getCode()), response.getErrorMessage());
                return;
            } catch (InvalidValueException e) {
                coapExchange.respond(ResponseCode.INTERNAL_SERVER_ERROR);
                return;
            }

        }
    }

    @Override
    public void handlePOST(final CoapExchange exchange) {
        String URI = exchange.getRequestOptions().getUriPathString();
        LwM2mPath path = new LwM2mPath(URI);

        // Manage Execute Request
        if (path.isResource()) {
            ExecuteResponse response = nodeEnabler.execute(new ExecuteRequest(URI, new String(exchange
                    .getRequestPayload())));
            exchange.respond(fromLwM2mCode(response.getCode()), response.getErrorMessage());
            return;
        }

        // Manage Create Request
        try {
            ContentFormat contentFormat = ContentFormat.fromCode(exchange.getRequestOptions().getContentFormat());
            LwM2mModel model = new LwM2mModel(nodeEnabler.getObjectModel());
            LwM2mNode lwM2mNode = LwM2mNodeDecoder.decode(exchange.getRequestPayload(), contentFormat, path, model);

            Collection<LwM2mResource> resources = null;
            if (lwM2mNode instanceof LwM2mObject) {
                LwM2mObject object = (LwM2mObject) lwM2mNode;
                if (object.getInstances().size() == 0) {
                    resources = Collections.emptyList();
                } else {
                    if (object.getInstances().size() == 1) {
                        LwM2mObjectInstance newInstance = object.getInstances().values().iterator().next();
                        resources = newInstance.getResources().values();
                    }
                }
            } else if (lwM2mNode instanceof LwM2mObjectInstance) {
                LwM2mObjectInstance newInstance = (LwM2mObjectInstance) lwM2mNode;
                resources = newInstance.getResources().values();
            }

            if (resources == null) {
                LOG.debug("Invalid create request payload: {}", lwM2mNode);
                exchange.respond(ResponseCode.BAD_REQUEST);
                return;
            }

            CreateResponse response = nodeEnabler.create(new CreateRequest(contentFormat, URI, resources));
            if (response.getCode() == org.eclipse.leshan.ResponseCode.CREATED) {
                exchange.setLocationPath(response.getLocation());
                exchange.respond(fromLwM2mCode(response.getCode()));
                return;
            } else {
                exchange.respond(fromLwM2mCode(response.getCode()), response.getErrorMessage());
                return;
            }
        } catch (InvalidValueException e) {
            exchange.respond(ResponseCode.BAD_REQUEST);
            return;
        }
    }

    @Override
    public void handleDELETE(final CoapExchange coapExchange) {
        // Manage Delete Request
        String URI = coapExchange.getRequestOptions().getUriPathString();
        DeleteResponse response = nodeEnabler.delete(new DeleteRequest(URI));
        coapExchange.respond(fromLwM2mCode(response.getCode()), response.getErrorMessage());
    }

    @Override
    public void sendNotify(String URI) {
        notifyObserverRelationsForResource(URI);
    }

    /*
     * Override the default behavior so that requests to sub resources (typically /ObjectId/*) are handled by this
     * resource.
     */
    @Override
    public Resource getChild(String name) {
        return this;
    }

    // TODO leshan-code-cf: this code should be factorize in a leshan-core-cf project.
    // duplicated from org.eclipse.leshan.server.californium.impl.RegisterResource
    public static ResponseCode fromLwM2mCode(final org.eclipse.leshan.ResponseCode code) {
        Validate.notNull(code);

        switch (code) {
        case CREATED:
            return ResponseCode.CREATED;
        case DELETED:
            return ResponseCode.DELETED;
        case CHANGED:
            return ResponseCode.CHANGED;
        case CONTENT:
            return ResponseCode.CONTENT;
        case BAD_REQUEST:
            return ResponseCode.BAD_REQUEST;
        case UNAUTHORIZED:
            return ResponseCode.UNAUTHORIZED;
        case NOT_FOUND:
            return ResponseCode.NOT_FOUND;
        case METHOD_NOT_ALLOWED:
            return ResponseCode.METHOD_NOT_ALLOWED;
        case FORBIDDEN:
            return ResponseCode.FORBIDDEN;
        case INTERNAL_SERVER_ERROR:
            return ResponseCode.INTERNAL_SERVER_ERROR;
        default:
            throw new IllegalArgumentException("Invalid CoAP code for LWM2M response: " + code);
        }
    }

    // TODO this code not be here, this is not its responsibility to do that.
    @Override
    public String asLinkFormat() {
        final StringBuilder linkFormat = LinkFormat.serializeResource(this).append(
                LinkFormat.serializeAttributes(getAttributes()));
        linkFormat.deleteCharAt(linkFormat.length() - 1);
        return linkFormat.toString();
    }

    /*
     * TODO: Observe HACK we should see if this could not be integrated in californium
     * http://dev.eclipse.org/mhonarc/lists/cf-dev/msg00181.html
     */
    private ObserveRelationContainer observeRelations = new ObserveRelationContainer();

    @Override
    public void addObserveRelation(ObserveRelation relation) {
        super.addObserveRelation(relation);
        observeRelations.add(relation);
    }

    @Override
    public void removeObserveRelation(ObserveRelation relation) {
        super.removeObserveRelation(relation);
        observeRelations.remove(relation);
    }

    protected void notifyObserverRelationsForResource(String URI) {
        for (ObserveRelation relation : observeRelations) {
            if (relation.getExchange().getRequest().getOptions().getUriPathString().equals(URI)) {
                relation.notifyObservers();
            }
        }
    }
}
