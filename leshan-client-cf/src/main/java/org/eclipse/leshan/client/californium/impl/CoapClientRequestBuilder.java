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

import org.eclipse.californium.core.coap.Request;
import org.eclipse.leshan.LinkObject;
import org.eclipse.leshan.client.LwM2mClient;
import org.eclipse.leshan.client.util.LinkFormatHelper;
import org.eclipse.leshan.core.request.BindingMode;
import org.eclipse.leshan.core.request.BootstrapRequest;
import org.eclipse.leshan.core.request.DeregisterRequest;
import org.eclipse.leshan.core.request.RegisterRequest;
import org.eclipse.leshan.core.request.UpdateRequest;
import org.eclipse.leshan.core.request.UplinkRequestVisitor;

public class CoapClientRequestBuilder implements UplinkRequestVisitor {

    private Request coapRequest;
    private final InetSocketAddress serverAddress;
    private final LwM2mClient client;

    public CoapClientRequestBuilder(final InetSocketAddress serverAddress, final LwM2mClient client) {
        this.serverAddress = serverAddress;
        this.client = client;
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

        coapRequest.getOptions().addUriPath("rd");
        coapRequest.getOptions().addUriQuery("ep=" + request.getEndpointName());

        Long lifetime = request.getLifetime();
        if (lifetime != null)
            coapRequest.getOptions().addUriQuery("lt=" + lifetime);

        String smsNumber = request.getSmsNumber();
        if (smsNumber != null)
            coapRequest.getOptions().addUriQuery("sms=" + smsNumber);

        String lwVersion = request.getLwVersion();
        if (lwVersion != null)
            coapRequest.getOptions().addUriQuery("lwm2m=" + lwVersion);

        BindingMode bindingMode = request.getBindingMode();
        if (bindingMode != null)
            coapRequest.getOptions().addUriQuery("b=" + bindingMode.toString());

        LinkObject[] linkObjects = request.getObjectLinks();
        String payload;
        if (linkObjects == null)
            payload = LinkObject.serialyse(LinkFormatHelper.getClientDescription(client.getObjectEnablers(), null));
        else
            payload = LinkObject.serialyse(linkObjects);
        coapRequest.setPayload(payload);
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
        if (linkObjects != null)
            coapRequest.setPayload(LinkObject.serialyse(linkObjects));
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
