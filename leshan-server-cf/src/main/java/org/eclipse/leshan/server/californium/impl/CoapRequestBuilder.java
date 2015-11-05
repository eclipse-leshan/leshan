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
import org.eclipse.leshan.core.model.LwM2mModel;
import org.eclipse.leshan.core.node.LwM2mObjectInstance;
import org.eclipse.leshan.core.node.LwM2mPath;
import org.eclipse.leshan.core.node.codec.LwM2mNodeEncoder;
import org.eclipse.leshan.core.request.ContentFormat;
import org.eclipse.leshan.core.request.ContentFormatHelper;
import org.eclipse.leshan.core.request.CreateRequest;
import org.eclipse.leshan.core.request.DeleteRequest;
import org.eclipse.leshan.core.request.DiscoverRequest;
import org.eclipse.leshan.core.request.DownlinkRequestVisitor;
import org.eclipse.leshan.core.request.ExecuteRequest;
import org.eclipse.leshan.core.request.ObserveRequest;
import org.eclipse.leshan.core.request.ReadRequest;
import org.eclipse.leshan.core.request.WriteAttributesRequest;
import org.eclipse.leshan.core.request.WriteRequest;
import org.eclipse.leshan.server.client.Client;
import org.eclipse.leshan.util.StringUtils;

public class CoapRequestBuilder implements DownlinkRequestVisitor {

    private Request coapRequest;
    private final Client destination;
    private final LwM2mModel model;

    public CoapRequestBuilder(Client destination, LwM2mModel model) {
        this.destination = destination;
        this.model = model;
    }

    @Override
    public void visit(ReadRequest request) {
        coapRequest = Request.newGet();
        setTarget(coapRequest, destination, request.getPath());
    }

    @Override
    public void visit(DiscoverRequest request) {
        coapRequest = Request.newGet();
        setTarget(coapRequest, destination, request.getPath());
        coapRequest.getOptions().setAccept(MediaTypeRegistry.APPLICATION_LINK_FORMAT);
    }

    @Override
    public void visit(WriteRequest request) {
        coapRequest = request.isReplaceRequest() ? Request.newPut() : Request.newPost();
        ContentFormat format = request.getContentFormat();
        if (format == null) {
            format = ContentFormatHelper.compute(request.getPath(), request.getNode(), model);
        }
        coapRequest.getOptions().setContentFormat(format.getCode());
        coapRequest.setPayload(LwM2mNodeEncoder.encode(request.getNode(), format, request.getPath(), model));
        setTarget(coapRequest, destination, request.getPath());
    }

    @Override
    public void visit(WriteAttributesRequest request) {
        coapRequest = Request.newPut();
        setTarget(coapRequest, destination, request.getPath());
        for (String query : request.getObserveSpec().toQueryParams()) {
            coapRequest.getOptions().addUriQuery(query);
        }
    }

    @Override
    public void visit(ExecuteRequest request) {
        coapRequest = Request.newPost();
        setTarget(coapRequest, destination, request.getPath());
        coapRequest.setPayload(request.getParameters());
    }

    @Override
    public void visit(CreateRequest request) {
        coapRequest = Request.newPost();
        coapRequest.getOptions().setContentFormat(request.getContentFormat().getCode());
        // wrap the resources into an object instance layer (with a fake instance id).
        coapRequest.setPayload(LwM2mNodeEncoder.encode(new LwM2mObjectInstance(-1, request.getResources()),
                request.getContentFormat(), request.getPath(), model));
        setTarget(coapRequest, destination, request.getPath());
    }

    @Override
    public void visit(DeleteRequest request) {
        coapRequest = Request.newDelete();
        setTarget(coapRequest, destination, request.getPath());
    }

    @Override
    public void visit(ObserveRequest request) {
        coapRequest = Request.newGet();
        coapRequest.setObserve();
        setTarget(coapRequest, destination, request.getPath());
    }

    private final void setTarget(Request coapRequest, Client client, LwM2mPath path) {
        coapRequest.setDestination(client.getAddress());
        coapRequest.setDestinationPort(client.getPort());

        // root path
        if (client.getRootPath() != null) {
            for (String rootPath : client.getRootPath().split("/")) {
                if (!StringUtils.isEmpty(rootPath)) {
                    coapRequest.getOptions().addUriPath(rootPath);
                }
            }
        }

        // objectId
        coapRequest.getOptions().addUriPath(Integer.toString(path.getObjectId()));

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
    };
}
