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
 *     Michał Wadowski (Orange)                      - Add Observe-Composite feature.
 *     Michał Wadowski (Orange)                      - Add Cancel Composite-Observation feature.
 *******************************************************************************/
package org.eclipse.leshan.server.californium.request;

import org.eclipse.californium.core.coap.MediaTypeRegistry;
import org.eclipse.californium.core.coap.Request;
import org.eclipse.californium.elements.EndpointContext;
import org.eclipse.californium.elements.util.Bytes;
import org.eclipse.leshan.core.californium.ObserveUtil;
import org.eclipse.leshan.core.californium.identity.IdentityHandler;
import org.eclipse.leshan.core.model.LwM2mModel;
import org.eclipse.leshan.core.node.LwM2mNode;
import org.eclipse.leshan.core.node.LwM2mObject;
import org.eclipse.leshan.core.node.LwM2mObjectInstance;
import org.eclipse.leshan.core.node.LwM2mPath;
import org.eclipse.leshan.core.node.codec.LwM2mEncoder;
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
import org.eclipse.leshan.core.request.DownlinkRequest;
import org.eclipse.leshan.core.request.DownlinkRequestVisitor;
import org.eclipse.leshan.core.request.ExecuteRequest;
import org.eclipse.leshan.core.request.Identity;
import org.eclipse.leshan.core.request.ObserveCompositeRequest;
import org.eclipse.leshan.core.request.ObserveRequest;
import org.eclipse.leshan.core.request.ReadCompositeRequest;
import org.eclipse.leshan.core.request.ReadRequest;
import org.eclipse.leshan.core.request.WriteAttributesRequest;
import org.eclipse.leshan.core.request.WriteCompositeRequest;
import org.eclipse.leshan.core.request.WriteRequest;
import org.eclipse.leshan.core.util.StringUtils;
import org.eclipse.leshan.server.request.LowerLayerConfig;

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
    private final LwM2mEncoder encoder;

    private final LowerLayerConfig lowerLayerConfig;

    private final IdentityHandler identityHandler;

    public CoapRequestBuilder(Identity destination, String rootPath, String registrationId, String endpoint,
            LwM2mModel model, LwM2mEncoder encoder, boolean allowConnectionInitiation,
            LowerLayerConfig lowerLayerConfig, IdentityHandler identityHandler) {
        this.destination = destination;
        this.rootPath = rootPath;
        this.endpoint = endpoint;
        this.registrationId = registrationId;
        this.model = model;
        this.encoder = encoder;
        this.allowConnectionInitiation = allowConnectionInitiation;
        this.lowerLayerConfig = lowerLayerConfig;
        this.identityHandler = identityHandler;
    }

    @Override
    public void visit(ReadRequest request) {
        coapRequest = Request.newGet();
        if (request.getContentFormat() != null)
            coapRequest.getOptions().setAccept(request.getContentFormat().getCode());
        setURI(coapRequest, request.getPath());
        setSecurityContext(coapRequest);
        applyLowerLayerConfig(coapRequest);
    }

    @Override
    public void visit(DiscoverRequest request) {
        coapRequest = Request.newGet();
        setURI(coapRequest, request.getPath());
        setSecurityContext(coapRequest);
        coapRequest.getOptions().setAccept(MediaTypeRegistry.APPLICATION_LINK_FORMAT);
        applyLowerLayerConfig(coapRequest);
    }

    @Override
    public void visit(WriteRequest request) {
        coapRequest = request.isReplaceRequest() ? Request.newPut() : Request.newPost();
        ContentFormat format = request.getContentFormat();
        coapRequest.getOptions().setContentFormat(format.getCode());
        coapRequest.setPayload(encoder.encode(request.getNode(), format, request.getPath(), model));
        setURI(coapRequest, request.getPath());
        setSecurityContext(coapRequest);
        applyLowerLayerConfig(coapRequest);
    }

    @Override
    public void visit(WriteAttributesRequest request) {
        coapRequest = Request.newPut();
        setURI(coapRequest, request.getPath());
        setSecurityContext(coapRequest);
        for (String query : request.getAttributes().toQueryParams()) {
            coapRequest.getOptions().addUriQuery(query);
        }
        applyLowerLayerConfig(coapRequest);
    }

    @Override
    public void visit(ExecuteRequest request) {
        coapRequest = Request.newPost();
        setURI(coapRequest, request.getPath());
        setSecurityContext(coapRequest);
        String payload = request.getArguments().serialize();
        if (payload != null) {
            coapRequest.setPayload(payload);
            coapRequest.getOptions().setContentFormat(MediaTypeRegistry.TEXT_PLAIN);
        }
        applyLowerLayerConfig(coapRequest);
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
        setURI(coapRequest, request.getPath());
        setSecurityContext(coapRequest);
        applyLowerLayerConfig(coapRequest);
    }

    @Override
    public void visit(DeleteRequest request) {
        coapRequest = Request.newDelete();
        setURI(coapRequest, request.getPath());
        setSecurityContext(coapRequest);
        applyLowerLayerConfig(coapRequest);
    }

    @Override
    public void visit(ObserveRequest request) {
        coapRequest = Request.newGet();
        if (request.getContentFormat() != null)
            coapRequest.getOptions().setAccept(request.getContentFormat().getCode());
        coapRequest.setObserve();
        setURI(coapRequest, request.getPath());
        setSecurityContext(coapRequest);

        // add context info to the observe request
        coapRequest.setUserContext(ObserveUtil.createCoapObserveRequestContext(endpoint, registrationId, request));
        applyLowerLayerConfig(coapRequest);
    }

    @Override
    public void visit(CancelObservationRequest request) {
        coapRequest = Request.newGet();
        coapRequest.setObserveCancel();
        coapRequest.setToken(request.getObservation().getId().getBytes());
        if (request.getContentFormat() != null)
            coapRequest.getOptions().setAccept(request.getContentFormat().getCode());
        setURI(coapRequest, request.getPath());
        setSecurityContext(coapRequest);
        applyLowerLayerConfig(coapRequest);
    }

    @Override
    public void visit(ReadCompositeRequest request) {
        coapRequest = Request.newFetch();
        coapRequest.getOptions().setContentFormat(request.getRequestContentFormat().getCode());
        coapRequest.setPayload(encoder.encodePaths(request.getPaths(), request.getRequestContentFormat()));
        if (request.getResponseContentFormat() != null)
            coapRequest.getOptions().setAccept(request.getResponseContentFormat().getCode());
        setURI(coapRequest, LwM2mPath.ROOTPATH);
        setSecurityContext(coapRequest);
        applyLowerLayerConfig(coapRequest);
    }

    @Override
    public void visit(ObserveCompositeRequest request) {
        coapRequest = Request.newFetch();

        coapRequest.getOptions().setContentFormat(request.getRequestContentFormat().getCode());

        coapRequest.setPayload(encoder.encodePaths(request.getPaths(), request.getRequestContentFormat()));

        if (request.getResponseContentFormat() != null) {
            coapRequest.getOptions().setAccept(request.getResponseContentFormat().getCode());
        }

        coapRequest.setObserve();
        setURI(coapRequest, LwM2mPath.ROOTPATH);
        setSecurityContext(coapRequest);

        coapRequest.setUserContext(
                ObserveUtil.createCoapObserveCompositeRequestContext(endpoint, registrationId, request));
        applyLowerLayerConfig(coapRequest);
    }

    @Override
    public void visit(CancelCompositeObservationRequest request) {
        coapRequest = Request.newFetch();
        coapRequest.setObserveCancel();
        coapRequest.setToken(request.getObservation().getId().getBytes());

        coapRequest.getOptions().setContentFormat(request.getRequestContentFormat().getCode());
        coapRequest.setPayload(encoder.encodePaths(request.getPaths(), request.getRequestContentFormat()));
        if (request.getResponseContentFormat() != null) {
            coapRequest.getOptions().setAccept(request.getResponseContentFormat().getCode());
        }

        setURI(coapRequest, LwM2mPath.ROOTPATH);
        setSecurityContext(coapRequest);
        applyLowerLayerConfig(coapRequest);
    }

    @Override
    public void visit(WriteCompositeRequest request) {
        coapRequest = Request.newIPatch();
        coapRequest.getOptions().setContentFormat(request.getContentFormat().getCode());
        coapRequest.setPayload(encoder.encodeNodes(request.getNodes(), request.getContentFormat(), model));
        setURI(coapRequest, LwM2mPath.ROOTPATH);
        setSecurityContext(coapRequest);
        applyLowerLayerConfig(coapRequest);
    }

    @Override
    public void visit(BootstrapWriteRequest request) {
        coapRequest = Request.newPut();
        coapRequest.setConfirmable(true);
        ContentFormat format = request.getContentFormat();
        coapRequest.getOptions().setContentFormat(format.getCode());
        coapRequest.setPayload(encoder.encode(request.getNode(), format, request.getPath(), model));
        setURI(coapRequest, request.getPath());
        setSecurityContext(coapRequest);
        applyLowerLayerConfig(coapRequest);
    }

    @Override
    public void visit(BootstrapReadRequest request) {
        coapRequest = Request.newGet();
        if (request.getContentFormat() != null)
            coapRequest.getOptions().setAccept(request.getContentFormat().getCode());
        setURI(coapRequest, request.getPath());
        setSecurityContext(coapRequest);
        applyLowerLayerConfig(coapRequest);
    }

    @Override
    public void visit(BootstrapDiscoverRequest request) {
        coapRequest = Request.newGet();
        setURI(coapRequest, request.getPath());
        setSecurityContext(coapRequest);
        coapRequest.getOptions().setAccept(MediaTypeRegistry.APPLICATION_LINK_FORMAT);
        applyLowerLayerConfig(coapRequest);
    }

    @Override
    public void visit(BootstrapDeleteRequest request) {
        coapRequest = Request.newDelete();
        coapRequest.setConfirmable(true);
        setSecurityContext(coapRequest);
        setURI(coapRequest, request.getPath());
        applyLowerLayerConfig(coapRequest);
    }

    @Override
    public void visit(BootstrapFinishRequest request) {
        coapRequest = Request.newPost();
        coapRequest.setConfirmable(true);
        setSecurityContext(coapRequest);

        // root path
        if (rootPath != null) {
            for (String rootPathPart : rootPath.split("/")) {
                if (!StringUtils.isEmpty(rootPathPart)) {
                    coapRequest.getOptions().addUriPath(rootPathPart);
                }
            }
        }

        coapRequest.getOptions().addUriPath("bs");
        applyLowerLayerConfig(coapRequest);
    }

    protected void setURI(Request coapRequest, LwM2mPath path) {
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

        // resourceInstanceId
        if (path.getResourceInstanceId() != null) {
            coapRequest.getOptions().addUriPath(Integer.toString(path.getResourceInstanceId()));
        }
    }

    protected void setSecurityContext(Request coapRequest) {
        EndpointContext context = identityHandler.createEndpointContext(destination, allowConnectionInitiation);
        coapRequest.setDestinationContext(context);
        if (destination.isOSCORE()) {
            coapRequest.getOptions().setOscore(Bytes.EMPTY);
        }
    }

    protected void applyLowerLayerConfig(Request coapRequest) {
        if (lowerLayerConfig != null)
            lowerLayerConfig.apply(coapRequest);
    }

    public Request getRequest() {
        return coapRequest;
    }
}
