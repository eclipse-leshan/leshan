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
 *     Zebra Technologies - initial API and implementation
 *     Michał Wadowski (Orange) - Improved compliance with rfc6690
 *******************************************************************************/
package org.eclipse.leshan.transport.javacoap.client.request;

import java.net.InetSocketAddress;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.stream.Collectors;

import org.eclipse.leshan.core.link.Link;
import org.eclipse.leshan.core.link.LinkSerializer;
import org.eclipse.leshan.core.model.LwM2mModel;
import org.eclipse.leshan.core.node.codec.LwM2mEncoder;
import org.eclipse.leshan.core.peer.IpPeer;
import org.eclipse.leshan.core.request.BindingMode;
import org.eclipse.leshan.core.request.BootstrapRequest;
import org.eclipse.leshan.core.request.ContentFormat;
import org.eclipse.leshan.core.request.DeregisterRequest;
import org.eclipse.leshan.core.request.RegisterRequest;
import org.eclipse.leshan.core.request.SendRequest;
import org.eclipse.leshan.core.request.UpdateRequest;
import org.eclipse.leshan.core.request.UplinkRequest;
import org.eclipse.leshan.core.request.UplinkRequestVisitor;
import org.eclipse.leshan.transport.javacoap.identity.IdentityHandler;

import com.mbed.coap.packet.CoapRequest;
import com.mbed.coap.packet.Opaque;

/**
 * This class is able to create CoAP request from LWM2M {@link UplinkRequest}.
 * <p>
 * Call <code>CoapRequestBuilder#visit(lwm2mRequest)</code>, then get the result using {@link #getRequest()}
 */
public class CoapRequestBuilder implements UplinkRequestVisitor {

    protected CoapRequest.Builder coapRequestBuilder;
    protected final IpPeer server;
    protected final LwM2mEncoder encoder;
    protected final LwM2mModel model;
    protected final LinkSerializer linkSerializer;
    protected final IdentityHandler identityHandler;

    public CoapRequestBuilder(IpPeer server, LwM2mEncoder encoder, LwM2mModel model, LinkSerializer linkSerializer,
            IdentityHandler identityHandler) {
        this.server = server;
        this.encoder = encoder;
        this.model = model;
        this.linkSerializer = linkSerializer;
        this.identityHandler = identityHandler;
    }

    @Override
    public void visit(BootstrapRequest request) {
        coapRequestBuilder = CoapRequest.post("/bs");

        // Create map of attributes
        HashMap<String, String> attributes = new HashMap<>();
        attributes.putAll(request.getAdditionalAttributes());

        String endpoint = request.getEndpointName();
        if (endpoint != null) {
            attributes.put("ep", endpoint);
        }
        if (request.getPreferredContentFormat() != null) {
            attributes.put("pct", Integer.toString(request.getPreferredContentFormat().getCode()));
        }

        // Convert map of attributes in URI Query as String
        String uriQuery = attributes.entrySet().stream() //
                .map(e -> e.getKey() + "=" + e.getValue()) //
                .collect(Collectors.joining("&"));
        coapRequestBuilder.query(uriQuery.toString());
    }

    @Override
    public void visit(RegisterRequest request) {
        coapRequestBuilder = CoapRequest.post("/rd");

        // Create map of attributes
        HashMap<String, String> attributes = new HashMap<>();
        attributes.putAll(request.getAdditionalAttributes());

        String endpoint = request.getEndpointName();
        if (endpoint != null) {
            attributes.put("ep", endpoint);
        }

        Long lifetime = request.getLifetime();
        if (lifetime != null)
            attributes.put("lt", lifetime.toString());

        String smsNumber = request.getSmsNumber();
        if (smsNumber != null)
            attributes.put("sms", smsNumber);

        String lwVersion = request.getLwVersion();
        if (lwVersion != null)
            attributes.put("lwm2m", lwVersion);

        EnumSet<BindingMode> bindingMode = request.getBindingMode();
        if (bindingMode != null)
            attributes.put("b", BindingMode.toString(bindingMode));

        Boolean queueMode = request.getQueueMode();
        if (queueMode != null && queueMode)
            attributes.put("Q", null);

        // Convert map of attributes in URI Query as String
        String uriQuery = attributes.entrySet().stream() //
                .map(e -> e.getValue() == null ? e.getKey() : e.getKey() + "=" + e.getValue()) //
                .collect(Collectors.joining("&"));
        coapRequestBuilder.query(uriQuery.toString());

        // Add Object links as Payload
        Link[] objectLinks = request.getObjectLinks();
        if (objectLinks != null) {
            String payload = linkSerializer.serializeCoreLinkFormat(objectLinks);
            coapRequestBuilder.payload(Opaque.of(payload), (short) ContentFormat.LINK.getCode());
        }
    }

    @Override
    public void visit(UpdateRequest request) {
        coapRequestBuilder = CoapRequest.post(request.getRegistrationId());

        // Create map of attributes
        HashMap<String, String> attributes = new HashMap<>();

        Long lifetime = request.getLifeTimeInSec();
        if (lifetime != null)
            attributes.put("lt", lifetime.toString());

        String smsNumber = request.getSmsNumber();
        if (smsNumber != null)
            attributes.put("sms", smsNumber);

        EnumSet<BindingMode> bindingMode = request.getBindingMode();
        if (bindingMode != null)
            attributes.put("b", BindingMode.toString(bindingMode));

        // Convert map of attributes in URI Query as String
        String uriQuery = attributes.entrySet().stream() //
                .map(e -> e.getValue() == null ? e.getKey() : e.getKey() + "=" + e.getValue()) //
                .collect(Collectors.joining("&"));
        coapRequestBuilder.query(uriQuery.toString());

        // Add Object links as Payload
        Link[] linkObjects = request.getObjectLinks();
        if (linkObjects != null) {
            coapRequestBuilder.payload(Opaque.of(linkSerializer.serializeCoreLinkFormat(linkObjects)),
                    (short) ContentFormat.LINK.getCode());
        }

    }

    @Override
    public void visit(DeregisterRequest request) {
        coapRequestBuilder = CoapRequest.delete(request.getRegistrationId());
    }

    @Override
    public void visit(SendRequest request) {
        ContentFormat format = request.getFormat();
        Opaque payload = Opaque.of(encoder.encodeTimestampedNodes(request.getTimestampedNodes(), format, null, model));

        coapRequestBuilder = CoapRequest.post("/dp") //
                .payload(payload) //
                .contentFormat((short) format.getCode());
    }

    public CoapRequest getRequest() {
        return coapRequestBuilder.address(getAddress()).context(identityHandler.createTransportContext(server, true))
                .build();
    }

    protected InetSocketAddress getAddress() {
        return server.getSocketAddress();
    }

}
