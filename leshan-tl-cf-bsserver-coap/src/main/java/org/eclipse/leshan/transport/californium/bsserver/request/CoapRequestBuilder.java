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
package org.eclipse.leshan.transport.californium.bsserver.request;

import org.eclipse.californium.core.coap.MediaTypeRegistry;
import org.eclipse.californium.core.coap.Request;
import org.eclipse.californium.elements.EndpointContext;
import org.eclipse.californium.elements.util.Bytes;
import org.eclipse.leshan.core.model.LwM2mModel;
import org.eclipse.leshan.core.node.LwM2mPath;
import org.eclipse.leshan.core.node.codec.LwM2mEncoder;
import org.eclipse.leshan.core.peer.LwM2mPeer;
import org.eclipse.leshan.core.peer.OscoreIdentity;
import org.eclipse.leshan.core.request.BootstrapDeleteRequest;
import org.eclipse.leshan.core.request.BootstrapDiscoverRequest;
import org.eclipse.leshan.core.request.BootstrapFinishRequest;
import org.eclipse.leshan.core.request.BootstrapReadRequest;
import org.eclipse.leshan.core.request.BootstrapWriteRequest;
import org.eclipse.leshan.core.request.ContentFormat;
import org.eclipse.leshan.core.request.DownlinkBootstrapRequestVisitor;
import org.eclipse.leshan.core.request.DownlinkRequest;
import org.eclipse.leshan.transport.californium.identity.IdentityHandler;

/**
 * This class is able to create CoAP request from LWM2M {@link DownlinkRequest}.
 * <p>
 * Call <code>CoapRequestBuilder#visit(lwm2mRequest)</code>, then get the result using {@link #getRequest()}
 */
public class CoapRequestBuilder implements DownlinkBootstrapRequestVisitor {

    private Request coapRequest;

    // client information
    private final LwM2mPeer destination;
    private final LwM2mModel model;
    private final LwM2mEncoder encoder;
    private final IdentityHandler identityHandler;

    public CoapRequestBuilder(LwM2mPeer destination, LwM2mModel model, LwM2mEncoder encoder,
            IdentityHandler identityHandler) {
        this.destination = destination;
        this.model = model;
        this.encoder = encoder;
        this.identityHandler = identityHandler;
    }

    @Override
    public void visit(BootstrapWriteRequest request) {
        coapRequest = Request.newPut();
        coapRequest.setConfirmable(true);
        ContentFormat format = request.getContentFormat();
        coapRequest.getOptions().setContentFormat(format.getCode());
        coapRequest.setPayload(encoder.encode(request.getNode(), format, null, request.getPath(), model));
        setURI(coapRequest, request.getPath());
        setSecurityContext(coapRequest);
    }

    @Override
    public void visit(BootstrapReadRequest request) {
        coapRequest = Request.newGet();
        if (request.getContentFormat() != null)
            coapRequest.getOptions().setAccept(request.getContentFormat().getCode());
        setURI(coapRequest, request.getPath());
        setSecurityContext(coapRequest);
    }

    @Override
    public void visit(BootstrapDiscoverRequest request) {
        coapRequest = Request.newGet();
        setURI(coapRequest, request.getPath());
        setSecurityContext(coapRequest);
        coapRequest.getOptions().setAccept(MediaTypeRegistry.APPLICATION_LINK_FORMAT);
    }

    @Override
    public void visit(BootstrapDeleteRequest request) {
        coapRequest = Request.newDelete();
        coapRequest.setConfirmable(true);
        setSecurityContext(coapRequest);
        setURI(coapRequest, request.getPath());
    }

    @Override
    public void visit(BootstrapFinishRequest request) {
        coapRequest = Request.newPost();
        coapRequest.setConfirmable(true);
        setSecurityContext(coapRequest);
        coapRequest.getOptions().addUriPath("bs");
    }

    protected void setURI(Request coapRequest, LwM2mPath path) {
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
        if (identityHandler != null) {
            EndpointContext context = identityHandler.createEndpointContext(destination, false);
            coapRequest.setDestinationContext(context);
        }
        if (destination.getIdentity() instanceof OscoreIdentity) {
            coapRequest.getOptions().setOscore(Bytes.EMPTY);
        }
    }

    public Request getRequest() {
        return coapRequest;
    }
}
