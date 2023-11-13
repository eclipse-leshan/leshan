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
package org.eclipse.leshan.transport.javacoap.server.request;

import java.net.InetSocketAddress;
import java.util.Collections;

import org.eclipse.leshan.core.model.LwM2mModel;
import org.eclipse.leshan.core.node.LwM2mIncompletePath;
import org.eclipse.leshan.core.node.LwM2mNode;
import org.eclipse.leshan.core.node.LwM2mObject;
import org.eclipse.leshan.core.node.LwM2mObjectInstance;
import org.eclipse.leshan.core.node.LwM2mPath;
import org.eclipse.leshan.core.node.codec.LwM2mEncoder;
import org.eclipse.leshan.core.observation.CompositeObservation;
import org.eclipse.leshan.core.observation.ObservationIdentifier;
import org.eclipse.leshan.core.observation.SingleObservation;
import org.eclipse.leshan.core.peer.IpPeer;
import org.eclipse.leshan.core.peer.LwM2mPeer;
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
import org.eclipse.leshan.core.request.ObserveCompositeRequest;
import org.eclipse.leshan.core.request.ObserveRequest;
import org.eclipse.leshan.core.request.ReadCompositeRequest;
import org.eclipse.leshan.core.request.ReadRequest;
import org.eclipse.leshan.core.request.WriteAttributesRequest;
import org.eclipse.leshan.core.request.WriteCompositeRequest;
import org.eclipse.leshan.core.request.WriteRequest;
import org.eclipse.leshan.server.registration.Registration;
import org.eclipse.leshan.transport.javacoap.request.RandomTokenGenerator;
import org.eclipse.leshan.transport.javacoap.server.observation.LwM2mKeys;

import com.mbed.coap.packet.CoapRequest;
import com.mbed.coap.packet.MediaTypes;
import com.mbed.coap.packet.Opaque;
import com.mbed.coap.transport.TransportContext;

/**
 * This class is able to create CoAP request from LWM2M {@link DownlinkRequest}.
 * <p>
 * Call <code>CoapRequestBuilder#visit(lwm2mRequest)</code>, then get the result using {@link #getRequest()}
 */
public class CoapRequestBuilder implements DownlinkRequestVisitor {

    private CoapRequest.Builder coapRequestBuilder;

    // client information
    private final LwM2mPeer destination;
    private final Registration registration;
    private final String rootPath;
    private final LwM2mEncoder encoder;
    private final LwM2mModel model;
    // TODO we should better manage this and especially better handle token conflict
    private final RandomTokenGenerator tokenGenerator = new RandomTokenGenerator(8);

    public CoapRequestBuilder(Registration registration, LwM2mPeer destination, String rootPath, LwM2mModel model,
            LwM2mEncoder encoder) {
        this.registration = registration;
        this.destination = destination;
        this.rootPath = rootPath;
        this.model = model;
        this.encoder = encoder;
    }

    @Override
    public void visit(ReadRequest request) {
        coapRequestBuilder = CoapRequest.get(getURI(request.getPath()));
        if (request.getContentFormat() != null)
            coapRequestBuilder.accept((short) request.getContentFormat().getCode());
    }

    @Override
    public void visit(DiscoverRequest request) {
        coapRequestBuilder = CoapRequest.get(getURI(request.getPath())) //
                .accept(MediaTypes.CT_APPLICATION_LINK__FORMAT);
    }

    @Override
    public void visit(WriteRequest request) {
        coapRequestBuilder = request.isReplaceRequest() ? //
                CoapRequest.put(getURI(request.getPath())) : //
                CoapRequest.post(getURI(request.getPath()));

        ContentFormat format = request.getContentFormat();
        coapRequestBuilder //
                .contentFormat((short) format.getCode()) //
                .payload(Opaque.of(encoder.encode(request.getNode(), format, request.getPath(), model)));
    }

    @Override
    public void visit(WriteAttributesRequest request) {
        coapRequestBuilder = CoapRequest.put(getURI(request.getPath())) //
                .query(request.getAttributes().toString());
    }

    @Override
    public void visit(ExecuteRequest request) {
        coapRequestBuilder = CoapRequest.post(getURI(request.getPath()));
        String payload = request.getArguments().serialize();
        if (payload != null) {
            coapRequestBuilder.payload(payload) //
                    .contentFormat(MediaTypes.CT_TEXT_PLAIN);
        }
    }

    @Override
    public void visit(CreateRequest request) {
        // if no instance id, the client will assign it.
        LwM2mNode node;
        if (request.unknownObjectInstanceId()) {
            node = new LwM2mObjectInstance(request.getResources());
        } else {
            node = new LwM2mObject(request.getPath().getObjectId(), request.getObjectInstances());
        }

        coapRequestBuilder = CoapRequest.post(getURI(request.getPath())) //
                .contentFormat((short) request.getContentFormat().getCode()) //
                .payload(Opaque.of(encoder.encode(node, request.getContentFormat(), request.getPath(), model)));
    }

    @Override
    public void visit(DeleteRequest request) {
        coapRequestBuilder = CoapRequest.delete(getURI(request.getPath()));
    }

    @Override
    public void visit(ObserveRequest request) {
        coapRequestBuilder = CoapRequest.observe(getURI(request.getPath()));

        if (request.getContentFormat() != null)
            coapRequestBuilder.accept((short) request.getContentFormat().getCode());

        // Create Observation
        // TODO the token generation is probably an issue :
        // What happens in case of conflict but also how could we follow :
        // https://www.rfc-editor.org/rfc/rfc9175#section-4.2
        Opaque token = tokenGenerator.createToken();
        SingleObservation observation = new SingleObservation(new ObservationIdentifier(token.getBytes()),
                registration.getId(), request.getPath(), request.getContentFormat(), request.getContext(),
                Collections.emptyMap());

        // Add Observation to request context
        TransportContext extendedContext = TransportContext.EMPTY //
                .with(LwM2mKeys.LESHAN_OBSERVATION, observation) //
                .with(LwM2mKeys.LESHAN_REGISTRATION, registration);
        coapRequestBuilder //
                .context(extendedContext) //
                .token(token);
    }

    @Override
    public void visit(CancelObservationRequest request) {
        coapRequestBuilder = CoapRequest.observe(getURI(request.getPath())) //
                .token(Opaque.of(request.getObservation().getId().getBytes())) //
                .deregisterObserve();

        if (request.getContentFormat() != null)
            coapRequestBuilder.accept((short) request.getContentFormat().getCode());
    }

    @Override
    public void visit(ReadCompositeRequest request) {
        coapRequestBuilder = CoapRequest.fetch(getURI(LwM2mPath.ROOTPATH)) //
                .contentFormat((short) request.getRequestContentFormat().getCode()) //
                .payload(Opaque.of(encoder.encodePaths(request.getPaths(), request.getRequestContentFormat())));

        if (request.getResponseContentFormat() != null) {
            coapRequestBuilder.accept((short) request.getResponseContentFormat().getCode());
        }
    }

    @Override
    public void visit(ObserveCompositeRequest request) {
        coapRequestBuilder = CoapRequest.fetch(getURI(LwM2mPath.ROOTPATH)) //
                .contentFormat((short) request.getRequestContentFormat().getCode()) //
                .payload(Opaque.of(encoder.encodePaths(request.getPaths(), request.getRequestContentFormat()))) //
                .observe();

        if (request.getResponseContentFormat() != null) {
            coapRequestBuilder.accept((short) request.getResponseContentFormat().getCode());
        }

        // Create Observation
        Opaque token = tokenGenerator.createToken();
        CompositeObservation observation = new CompositeObservation(new ObservationIdentifier(token.getBytes()),
                registration.getId(), request.getPaths(), request.getRequestContentFormat(),
                request.getResponseContentFormat(), request.getContext(), Collections.emptyMap());

        // Add Observation to request context
        TransportContext extendedContext = TransportContext.EMPTY //
                .with(LwM2mKeys.LESHAN_OBSERVATION, observation) //
                .with(LwM2mKeys.LESHAN_REGISTRATION, registration);
        coapRequestBuilder //
                .context(extendedContext) //
                .token(token);
    }

    @Override
    public void visit(CancelCompositeObservationRequest request) {
        coapRequestBuilder = CoapRequest.fetch(getURI(LwM2mPath.ROOTPATH)) //
                .token(Opaque.of(request.getObservation().getId().getBytes())) //
                .contentFormat((short) request.getRequestContentFormat().getCode()) //
                .payload(Opaque.of(encoder.encodePaths(request.getPaths(), request.getRequestContentFormat()))) //
                .deregisterObserve(); //

        if (request.getResponseContentFormat() != null) {
            coapRequestBuilder.accept((short) request.getResponseContentFormat().getCode());
        }
    }

    @Override
    public void visit(WriteCompositeRequest request) {
        coapRequestBuilder = CoapRequest.iPatch(getURI(LwM2mPath.ROOTPATH)) //
                .contentFormat((short) request.getContentFormat().getCode()) //
                .payload(Opaque.of(encoder.encodeNodes(request.getNodes(), request.getContentFormat(), model)));

    }

    @Override
    public void visit(BootstrapWriteRequest request) {
        coapRequestBuilder = CoapRequest.put(getURI(request.getPath()));

        ContentFormat format = request.getContentFormat();
        coapRequestBuilder //
                .contentFormat((short) format.getCode()) //
                .payload(Opaque.of(encoder.encode(request.getNode(), format, request.getPath(), model)));
    }

    @Override
    public void visit(BootstrapReadRequest request) {
        coapRequestBuilder = CoapRequest.get(getURI(request.getPath()));
        if (request.getContentFormat() != null)
            coapRequestBuilder.accept((short) request.getContentFormat().getCode());
    }

    @Override
    public void visit(BootstrapDiscoverRequest request) {
        coapRequestBuilder = CoapRequest.get(getURI(request.getPath())) //
                .accept(MediaTypes.CT_APPLICATION_LINK__FORMAT);
    }

    @Override
    public void visit(BootstrapDeleteRequest request) {
        coapRequestBuilder = CoapRequest.delete(getURI(request.getPath()));
    }

    @Override
    public void visit(BootstrapFinishRequest request) {
        coapRequestBuilder = CoapRequest.post("bs");
    }

    protected InetSocketAddress getAddress() {
        if (destination instanceof IpPeer) {
            return ((IpPeer) destination).getSocketAddress();
        } else {
            throw new IllegalStateException(String.format("Unsupported Peer : %s", destination));
        }
    }

    protected String getURI(LwM2mPath path) {
        if (path instanceof LwM2mIncompletePath) {
            throw new IllegalStateException("Incomplete path can not be used to create request");
        }

        StringBuilder uri = new StringBuilder();

        // handle root/alternate path
        if (rootPath != null && !"/".equals(rootPath)) {
            uri.append(rootPath);
        }
        // add LWM2M request path
        uri.append(path.toString());
        return uri.toString();
    }

    public CoapRequest getRequest() {
        coapRequestBuilder.address(getAddress());
        return coapRequestBuilder.build();
    }
}
