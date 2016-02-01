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
 *     Zebra Technologies - initial API and implementation
 *******************************************************************************/
package org.eclipse.leshan.client.californium.impl;

import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map.Entry;

import org.eclipse.californium.core.coap.Request;
import org.eclipse.leshan.LinkObject;
import org.eclipse.leshan.core.request.BindingMode;
import org.eclipse.leshan.core.request.BootstrapRequest;
import org.eclipse.leshan.core.request.ContentFormat;
import org.eclipse.leshan.core.request.DeregisterRequest;
import org.eclipse.leshan.core.request.RegisterRequest;
import org.eclipse.leshan.core.request.UpdateRequest;
import org.eclipse.leshan.core.request.UplinkRequestVisitor;

public class CoapClientRequestBuilder implements UplinkRequestVisitor {

    private Request coapRequest;
    private final InetSocketAddress serverAddress;

    public CoapClientRequestBuilder(final InetSocketAddress serverAddress) {
        this.serverAddress = serverAddress;
    }

    @Override
    public void visit(final BootstrapRequest request) {
        coapRequest = Request.newPost();
        buildRequestSettings();
        coapRequest.getOptions().addUriPath("bs");
        coapRequest.getOptions().addUriQuery("ep=" + request.getEndpointName());
    }

    @Override
    public void visit(final RegisterRequest request) {
        coapRequest = Request.newPost();
        buildRequestSettings();
        coapRequest.getOptions().setContentFormat(ContentFormat.LINK.getCode());
        coapRequest.getOptions().addUriPath("rd");

        HashMap<String, String> attributes = new HashMap<>();
        attributes.putAll(request.getAdditionalAttributes());

        attributes.put("ep", request.getEndpointName());

        Long lifetime = request.getLifetime();
        if (lifetime != null)
            attributes.put("lt", lifetime.toString());

        String smsNumber = request.getSmsNumber();
        if (smsNumber != null)
            attributes.put("sms", smsNumber);

        String lwVersion = request.getLwVersion();
        if (lwVersion != null)
            attributes.put("lwm2m", lwVersion);

        BindingMode bindingMode = request.getBindingMode();
        if (bindingMode != null)
            attributes.put("b", bindingMode.toString());

        for (Entry<String, String> attr : attributes.entrySet()) {
            coapRequest.getOptions().addUriQuery(attr.getKey() + "=" + attr.getValue());
        }

        LinkObject[] linkObjects = request.getObjectLinks();
        if (linkObjects != null)
            coapRequest.setPayload(LinkObject.serialize(linkObjects));

    }

    @Override
    public void visit(final UpdateRequest request) {
        coapRequest = Request.newPost();
        buildRequestSettings();
        coapRequest.getOptions().setUriPath(request.getRegistrationId());

        Long lifetime = request.getLifeTimeInSec();
        if (lifetime != null)
            coapRequest.getOptions().addUriQuery("lt=" + lifetime);

        String smsNumber = request.getSmsNumber();
        if (smsNumber != null)
            coapRequest.getOptions().addUriQuery("sms=" + smsNumber);

        BindingMode bindingMode = request.getBindingMode();
        if (bindingMode != null)
            coapRequest.getOptions().addUriQuery("b=" + bindingMode.toString());

        LinkObject[] linkObjects = request.getObjectLinks();
        if (linkObjects != null) {
            coapRequest.getOptions().setContentFormat(ContentFormat.LINK.getCode());
            coapRequest.setPayload(LinkObject.serialize(linkObjects));
        }
    }

    @Override
    public void visit(final DeregisterRequest request) {
        coapRequest = Request.newDelete();
        buildRequestSettings();
        coapRequest.getOptions().setUriPath(request.getRegistrationID());
    }

    public Request getRequest() {
        return coapRequest;
    }

    private void buildRequestSettings() {
        coapRequest.setDestination(serverAddress.getAddress());
        coapRequest.setDestinationPort(serverAddress.getPort());
    }
}
