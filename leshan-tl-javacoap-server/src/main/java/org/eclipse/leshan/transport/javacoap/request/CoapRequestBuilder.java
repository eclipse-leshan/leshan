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
package org.eclipse.leshan.transport.javacoap.request;

import java.net.InetSocketAddress;

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
import org.eclipse.leshan.server.request.LowerLayerConfig;

import com.mbed.coap.packet.CoapRequest;
import com.mbed.coap.packet.MediaTypes;
import com.mbed.coap.packet.Opaque;

/**
 * This class is able to create CoAP request from LWM2M {@link DownlinkRequest}.
 * <p>
 * Call <code>CoapRequestBuilder#visit(lwm2mRequest)</code>, then get the result using {@link #getRequest()}
 */
public class CoapRequestBuilder implements DownlinkRequestVisitor {

    private CoapRequest coapRequest;

    // client information
    private final Identity destination;
    private final String rootPath;
    // private final String registrationId;
    // private final String endpoint;
    // private final boolean allowConnectionInitiation;

    private final LwM2mModel model;
    private final LwM2mEncoder encoder;

    private final LowerLayerConfig lowerLayerConfig;

    private final RandomTokenGenerator tokenGenerator;

    // private final IdentityHandler identityHandler;

    public CoapRequestBuilder(Identity destination, String rootPath, String registrationId, String endpoint,
            LwM2mModel model, LwM2mEncoder encoder, boolean allowConnectionInitiation,
            LowerLayerConfig lowerLayerConfig /* ,IdentityHandler identityHandler */,
            RandomTokenGenerator tokenGenerator) {
        this.destination = destination;
        this.rootPath = rootPath;
        // this.endpoint = endpoint;
        // this.registrationId = registrationId;
        this.model = model;
        this.encoder = encoder;
        // this.allowConnectionInitiation = allowConnectionInitiation;
        this.tokenGenerator = tokenGenerator;
        this.lowerLayerConfig = lowerLayerConfig;
        // this.identityHandler = identityHandler;
    }

    @Override
    public void visit(ReadRequest request) {
        coapRequest = CoapRequest.get(getAddress(), getURI(request.getPath()));
        coapRequest = setToken(coapRequest);
        if (request.getContentFormat() != null)
            coapRequest.options().setAccept(request.getContentFormat().getCode());
        applyLowerLayerConfig(coapRequest);
    }

    @Override
    public void visit(DiscoverRequest request) {
        coapRequest = CoapRequest.get(getAddress(), getURI(request.getPath()));
        coapRequest = setToken(coapRequest);
        coapRequest.options().setAccept(MediaTypes.CT_APPLICATION_LINK__FORMAT);
        applyLowerLayerConfig(coapRequest);
    }

    @Override
    public void visit(WriteRequest request) {
        coapRequest = request.isReplaceRequest() ? CoapRequest.put(getAddress(), getURI(request.getPath()))
                : CoapRequest.post(getAddress(), getURI(request.getPath()));
        coapRequest = setToken(coapRequest);
        ContentFormat format = request.getContentFormat();
        coapRequest.options().setContentFormat((short) format.getCode());
        coapRequest = coapRequest
                .payload(Opaque.of(encoder.encode(request.getNode(), format, request.getPath(), model)));
        applyLowerLayerConfig(coapRequest);
    }

    @Override
    public void visit(WriteAttributesRequest request) {
        coapRequest = CoapRequest.put(getAddress(), getURI(request.getPath()));
        coapRequest = setToken(coapRequest);
        coapRequest.options().setUriQuery(request.getAttributes().toString());
        applyLowerLayerConfig(coapRequest);
    }

    @Override
    public void visit(ExecuteRequest request) {
        coapRequest = CoapRequest.post(getAddress(), getURI(request.getPath()));
        coapRequest = setToken(coapRequest);
        String payload = request.getArguments().serialize();
        if (payload != null) {
            coapRequest.payload(payload);
            coapRequest.options().setContentFormat(MediaTypes.CT_TEXT_PLAIN);
        }
        applyLowerLayerConfig(coapRequest);
    }

    @Override
    public void visit(CreateRequest request) {
        coapRequest = CoapRequest.post(getAddress(), getURI(request.getPath()));
        coapRequest = setToken(coapRequest);
        coapRequest.options().setContentFormat((short) request.getContentFormat().getCode());
        // if no instance id, the client will assign it.
        LwM2mNode node;
        if (request.unknownObjectInstanceId()) {
            node = new LwM2mObjectInstance(request.getResources());
        } else {
            node = new LwM2mObject(request.getPath().getObjectId(), request.getObjectInstances());
        }
        coapRequest = coapRequest
                .payload(Opaque.of(encoder.encode(node, request.getContentFormat(), request.getPath(), model)));
        applyLowerLayerConfig(coapRequest);
    }

    @Override
    public void visit(DeleteRequest request) {
        coapRequest = CoapRequest.delete(getAddress(), getURI(request.getPath()));
        coapRequest = setToken(coapRequest);
        applyLowerLayerConfig(coapRequest);
    }

    @Override
    public void visit(ObserveRequest request) {
        // TODO not implemented : need to investigate how observe work with java-coap

//        coapRequest = Request.newGet();
//        if (request.getContentFormat() != null)
//            coapRequest.getOptions().setAccept(request.getContentFormat().getCode());
//        coapRequest.setObserve();
//        setURI(coapRequest, request.getPath());
//        setSecurityContext(coapRequest);
//
//        // add context info to the observe request
//        coapRequest.setUserContext(ObserveUtil.createCoapObserveRequestContext(endpoint, registrationId, request));
//        applyLowerLayerConfig(coapRequest);
    }

    @Override
    public void visit(CancelObservationRequest request) {
        // TODO not implemented : need to investigate how observe work with java-coap

//        coapRequest = Request.newGet();
//        coapRequest.setObserveCancel();
//        coapRequest.setToken(request.getObservation().getId().getBytes());
//        if (request.getContentFormat() != null)
//            coapRequest.getOptions().setAccept(request.getContentFormat().getCode());
//        setURI(coapRequest, request.getPath());
//        setSecurityContext(coapRequest);
//        applyLowerLayerConfig(coapRequest);
    }

    @Override
    public void visit(ReadCompositeRequest request) {
        // TODO not implemented :no fetch in java-coap

//        coapRequest = Request.newFetch();
//        coapRequest.getOptions().setContentFormat(request.getRequestContentFormat().getCode());
//        coapRequest.setPayload(encoder.encodePaths(request.getPaths(), request.getRequestContentFormat()));
//        if (request.getResponseContentFormat() != null)
//            coapRequest.getOptions().setAccept(request.getResponseContentFormat().getCode());
//        setURI(coapRequest, LwM2mPath.ROOTPATH);
//        setSecurityContext(coapRequest);
//        applyLowerLayerConfig(coapRequest);
    }

    @Override
    public void visit(ObserveCompositeRequest request) {
        // TODO not implemented :no fetch in java-coap

//        coapRequest = Request.newFetch();
//
//        coapRequest.getOptions().setContentFormat(request.getRequestContentFormat().getCode());
//
//        coapRequest.setPayload(encoder.encodePaths(request.getPaths(), request.getRequestContentFormat()));
//
//        if (request.getResponseContentFormat() != null) {
//            coapRequest.getOptions().setAccept(request.getResponseContentFormat().getCode());
//        }
//
//        coapRequest.setObserve();
//        setURI(coapRequest, LwM2mPath.ROOTPATH);
//        setSecurityContext(coapRequest);
//
//        coapRequest.setUserContext(
//                ObserveUtil.createCoapObserveCompositeRequestContext(endpoint, registrationId, request));
//        applyLowerLayerConfig(coapRequest);
    }

    @Override
    public void visit(CancelCompositeObservationRequest request) {
        // TODO not implemented :no fetch in java-coap

//        coapRequest = Request.newFetch();
//        coapRequest.setObserveCancel();
//        coapRequest.setToken(request.getObservation().getId().getBytes());
//
//        coapRequest.getOptions().setContentFormat(request.getRequestContentFormat().getCode());
//        coapRequest.setPayload(encoder.encodePaths(request.getPaths(), request.getRequestContentFormat()));
//        if (request.getResponseContentFormat() != null) {
//            coapRequest.getOptions().setAccept(request.getResponseContentFormat().getCode());
//        }
//
//        setURI(coapRequest, LwM2mPath.ROOTPATH);
//        setSecurityContext(coapRequest);
//        applyLowerLayerConfig(coapRequest);
    }

    @Override
    public void visit(WriteCompositeRequest request) {
        // TODO not implemented :no ipatch in java-coap

//        coapRequest = Request.newIPatch();
//        coapRequest.getOptions().setContentFormat(request.getContentFormat().getCode());
//        coapRequest.setPayload(encoder.encodeNodes(request.getNodes(), request.getContentFormat(), model));
//        setURI(coapRequest, LwM2mPath.ROOTPATH);
//        setSecurityContext(coapRequest);
//        applyLowerLayerConfig(coapRequest);
    }

    @Override
    public void visit(BootstrapWriteRequest request) {
        coapRequest = CoapRequest.put(getAddress(), getURI(request.getPath()));
        coapRequest = setToken(coapRequest);
        // TODO how to set request as confirmable
        // coapRequest.setConfirmable(true);
        ContentFormat format = request.getContentFormat();
        coapRequest.options().setContentFormat((short) format.getCode());
        coapRequest = coapRequest
                .payload(Opaque.of(encoder.encode(request.getNode(), format, request.getPath(), model)));
        applyLowerLayerConfig(coapRequest);
    }

    @Override
    public void visit(BootstrapReadRequest request) {
        coapRequest = CoapRequest.get(getAddress(), getURI(request.getPath()));
        coapRequest = setToken(coapRequest);
        if (request.getContentFormat() != null)
            coapRequest.options().setAccept(request.getContentFormat().getCode());
        applyLowerLayerConfig(coapRequest);
    }

    @Override
    public void visit(BootstrapDiscoverRequest request) {
        coapRequest = CoapRequest.get(getAddress(), getURI(request.getPath()));
        coapRequest = setToken(coapRequest);
        coapRequest.options().setAccept(MediaTypes.CT_APPLICATION_LINK__FORMAT);
        applyLowerLayerConfig(coapRequest);
    }

    @Override
    public void visit(BootstrapDeleteRequest request) {
        coapRequest = CoapRequest.delete(getAddress(), getURI(request.getPath()));
        coapRequest = setToken(coapRequest);
        // TODO how to set request as confirmable
        // coapRequest.setConfirmable(true);
        applyLowerLayerConfig(coapRequest);
    }

    @Override
    public void visit(BootstrapFinishRequest request) {
        coapRequest = CoapRequest.post(getAddress(), "bs");
        coapRequest = setToken(coapRequest);
        // TODO how to set request as confirmable
        // coapRequest.setConfirmable(true);
        // setSecurityContext(coapRequest);

        applyLowerLayerConfig(coapRequest);
    }

    protected InetSocketAddress getAddress() {
        return destination.getPeerAddress();
    }

    protected String getURI(LwM2mPath path) {
        StringBuilder uri = new StringBuilder();

        // root path
        if (rootPath != null && !"/".equals(rootPath)) {
            uri.append(rootPath);
        }

        // TODO handle incomplete path ?
        uri.append(path.toString());

        return uri.toString();
    }

//    protected void setSecurityContext(Request coapRequest) {
//        EndpointContext context = identityHandler.createEndpointContext(destination, allowConnectionInitiation);
//        coapRequest.setDestinationContext(context);
//        if (destination.isOSCORE()) {
//            coapRequest.getOptions().setOscore(Bytes.EMPTY);
//        }
//    }

    protected CoapRequest setToken(CoapRequest coapRequest) {
        if (coapRequest.getToken().isEmpty())
            return coapRequest.token(tokenGenerator.createToken());
        return coapRequest;
    }

    protected void applyLowerLayerConfig(CoapRequest coapRequest) {
        if (lowerLayerConfig != null)
            lowerLayerConfig.apply(coapRequest);
    }

    public CoapRequest getRequest() {
        return coapRequest;
    }
}
