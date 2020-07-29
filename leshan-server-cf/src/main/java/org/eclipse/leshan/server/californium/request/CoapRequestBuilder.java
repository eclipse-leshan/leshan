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
 *     Achim Kraus (Bosch Software Innovations GmbH) - use Identity as destination
 *                                                     and transform them to 
 *                                                     EndpointContext for requests
 *******************************************************************************/
package org.eclipse.leshan.server.californium.request;

import org.eclipse.californium.core.coap.MediaTypeRegistry;
import org.eclipse.californium.core.coap.Request;
import org.eclipse.californium.elements.EndpointContext;
import org.eclipse.leshan.core.californium.EndpointContextUtil;
import org.eclipse.leshan.core.model.LwM2mModel;
import org.eclipse.leshan.core.node.LwM2mNode;
import org.eclipse.leshan.core.node.LwM2mObject;
import org.eclipse.leshan.core.node.LwM2mObjectInstance;
import org.eclipse.leshan.core.node.LwM2mPath;
import org.eclipse.leshan.core.node.codec.LwM2mNodeEncoder;
import org.eclipse.leshan.core.request.BootstrapDeleteRequest;
import org.eclipse.leshan.core.request.BootstrapDiscoverRequest;
import org.eclipse.leshan.core.request.BootstrapFinishRequest;
import org.eclipse.leshan.core.request.BootstrapWriteRequest;
import org.eclipse.leshan.core.request.CancelObservationRequest;
import org.eclipse.leshan.core.request.ContentFormat;
import org.eclipse.leshan.core.request.CreateRequest;
import org.eclipse.leshan.core.request.DeleteRequest;
import org.eclipse.leshan.core.request.DiscoverRequest;
import org.eclipse.leshan.core.request.DownlinkRequest;
import org.eclipse.leshan.core.request.DownlinkRequestVisitor;
import org.eclipse.leshan.core.request.ExecuteRequest;
import org.eclipse.leshan.core.request.Identity;
import org.eclipse.leshan.core.request.ObserveRequest;
import org.eclipse.leshan.core.request.ReadRequest;
import org.eclipse.leshan.core.request.WriteAttributesRequest;
import org.eclipse.leshan.core.request.WriteRequest;
import org.eclipse.leshan.core.util.StringUtils;
import org.eclipse.leshan.server.californium.observation.ObserveUtil;

/**
 * This class is able to create CoAP request from LWM2M {@link DownlinkRequest}.
 * <p>
 * Call <code>CoapRequestBuilder#visit(lwm2mRequest)</code>, then get the result using {@link #getRequest()}
 */
public class CoapRequestBuilder implements DownlinkRequestVisitor {

    private Request coapRequest;

    // client information
    private final Identity destination;
    private final String rootPath;
    private final String registrationId;
    private final String endpoint;
    private final boolean allowConnectionInitiation;

    private final LwM2mModel model;
    private final LwM2mNodeEncoder encoder;

    public CoapRequestBuilder(Identity destination, String rootPath, String registrationId, String endpoint,
            LwM2mModel model, LwM2mNodeEncoder encoder, boolean allowConnectionInitiation) {
        this.destination = destination;
        this.rootPath = rootPath;
        this.endpoint = endpoint;
        this.registrationId = registrationId;
        this.model = model;
        this.encoder = encoder;
        this.allowConnectionInitiation = allowConnectionInitiation;
    }

    @Override
    public void visit(ReadRequest request) {
        coapRequest = Request.newGet();
        if (request.getContentFormat() != null)
            coapRequest.getOptions().setAccept(request.getContentFormat().getCode());
        setTarget(coapRequest, request.getPath());
    }

    @Override
    public void visit(DiscoverRequest request) {
        coapRequest = Request.newGet();
        setTarget(coapRequest, request.getPath());
        coapRequest.getOptions().setAccept(MediaTypeRegistry.APPLICATION_LINK_FORMAT);
    }

    @Override
    public void visit(WriteRequest request) {
        coapRequest = request.isReplaceRequest() ? Request.newPut() : Request.newPost();
        ContentFormat format = request.getContentFormat();
        coapRequest.getOptions().setContentFormat(format.getCode());
        coapRequest.setPayload(encoder.encode(request.getNode(), format, request.getPath(), model));
        setTarget(coapRequest, request.getPath());
    }

    @Override
    public void visit(WriteAttributesRequest request) {
        coapRequest = Request.newPut();
        setTarget(coapRequest, request.getPath());
        for (String query : request.getAttributes().toQueryParams()) {
            coapRequest.getOptions().addUriQuery(query);
        }
    }

    @Override
    public void visit(ExecuteRequest request) {
        coapRequest = Request.newPost();
        setTarget(coapRequest, request.getPath());
        if (request.getParameters() != null) {
            coapRequest.setPayload(request.getParameters());
            coapRequest.getOptions().setContentFormat(MediaTypeRegistry.TEXT_PLAIN);
        }
    }

    @Override
    public void visit(CreateRequest request) {
        coapRequest = Request.newPost();
        coapRequest.getOptions().setContentFormat(request.getContentFormat().getCode());
        // if no instance id, the client will assign it.
        LwM2mNode node;
        if (request.unknownObjectInstanceId()) {
            node = new LwM2mObjectInstance(request.getResources());
        } else {
            node = new LwM2mObject(request.getPath().getObjectId(), request.getObjectInstances());
        }
        coapRequest.setPayload(encoder.encode(node, request.getContentFormat(), request.getPath(), model));
        setTarget(coapRequest, request.getPath());
    }

    @Override
    public void visit(DeleteRequest request) {
        coapRequest = Request.newDelete();
        setTarget(coapRequest, request.getPath());
    }

    @Override
    public void visit(ObserveRequest request) {
        coapRequest = Request.newGet();
        if (request.getContentFormat() != null)
            coapRequest.getOptions().setAccept(request.getContentFormat().getCode());
        coapRequest.setObserve();
        setTarget(coapRequest, request.getPath());

        // add context info to the observe request
        coapRequest.setUserContext(ObserveUtil.createCoapObserveRequestContext(endpoint, registrationId, request));
    }

    @Override
    public void visit(CancelObservationRequest request) {
        coapRequest = Request.newGet();
        coapRequest.setObserveCancel();
        coapRequest.setToken(request.getObservation().getId());
        if (request.getObservation().getContentFormat() != null)
            coapRequest.getOptions().setAccept(request.getObservation().getContentFormat().getCode());
        setTarget(coapRequest, request.getPath());
    }

    @Override
    public void visit(BootstrapWriteRequest request) {
        coapRequest = Request.newPut();
        coapRequest.setConfirmable(true);
        ContentFormat format = request.getContentFormat();
        coapRequest.getOptions().setContentFormat(format.getCode());
        coapRequest.setPayload(encoder.encode(request.getNode(), format, request.getPath(), model));
        setTarget(coapRequest, request.getPath());
    }

    @Override
    public void visit(BootstrapDiscoverRequest request) {
        coapRequest = Request.newGet();
        setTarget(coapRequest, request.getPath());
        coapRequest.getOptions().setAccept(MediaTypeRegistry.APPLICATION_LINK_FORMAT);
    }

    @Override
    public void visit(BootstrapDeleteRequest request) {
        coapRequest = Request.newDelete();
        coapRequest.setConfirmable(true);
        EndpointContext context = EndpointContextUtil.extractContext(destination, allowConnectionInitiation);
        coapRequest.setDestinationContext(context);
        setTarget(coapRequest, request.getPath());
    }

    @Override
    public void visit(BootstrapFinishRequest request) {
        coapRequest = Request.newPost();
        coapRequest.setConfirmable(true);
        EndpointContext context = EndpointContextUtil.extractContext(destination, allowConnectionInitiation);
        coapRequest.setDestinationContext(context);

        // root path
        if (rootPath != null) {
            for (String rootPathPart : rootPath.split("/")) {
                if (!StringUtils.isEmpty(rootPathPart)) {
                    coapRequest.getOptions().addUriPath(rootPathPart);
                }
            }
        }

        coapRequest.getOptions().addUriPath("bs");
    }

    protected void setTarget(Request coapRequest, LwM2mPath path) {
        EndpointContext context = EndpointContextUtil.extractContext(destination, allowConnectionInitiation);
        coapRequest.setDestinationContext(context);

        // root path
        if (rootPath != null) {
            for (String rootPathPart : rootPath.split("/")) {
                if (!StringUtils.isEmpty(rootPathPart)) {
                    coapRequest.getOptions().addUriPath(rootPathPart);
                }
            }
        }

        // objectId
        if (path.getObjectId() != null) {
            coapRequest.getOptions().addUriPath(Integer.toString(path.getObjectId()));
        }

        // objectInstanceId
        if (path.getObjectInstanceId() == null) {
            if (path.getResourceId() != null) {
                coapRequest.getOptions().addUriPath("0"); // default instanceId
            }
        } else {
            coapRequest.getOptions().addUriPath(Integer.toString(path.getObjectInstanceId()));
        }

        // resourceId
        if (path.getResourceId() != null) {
            coapRequest.getOptions().addUriPath(Integer.toString(path.getResourceId()));
        }
    }

    public Request getRequest() {
        return coapRequest;
    }
}
